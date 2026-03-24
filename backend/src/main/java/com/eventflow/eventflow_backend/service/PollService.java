package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.request.CreatePollRequest;
import com.eventflow.eventflow_backend.dto.request.VotePollRequest;
import com.eventflow.eventflow_backend.dto.response.PollOptionResponse;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.entity.Poll;
import com.eventflow.eventflow_backend.entity.PollOption;
import com.eventflow.eventflow_backend.entity.PollVote;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.PollStatus;
import com.eventflow.eventflow_backend.exception.ResourceNotFoundException;
import com.eventflow.eventflow_backend.mapper.PollMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.repository.PollOptionRepository;
import com.eventflow.eventflow_backend.repository.PollRepository;
import com.eventflow.eventflow_backend.repository.PollVoteRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;
    private final CurrentUserService currentUserService;
    private final PollRedisService pollRedisService;
    private final PollMapper pollMapper;
    private final WebSocketEventService webSocketEventService;

    @Transactional
    public PollResponse createPoll(Long eventId, CreatePollRequest request) {
        Event event = getEventOrThrow(eventId);
        validatePollManagementAccess(event);

        if (pollRepository.existsByEventIdAndStatus(eventId, PollStatus.ACTIVE)) {
            throw new IllegalArgumentException("У мероприятия уже есть активный опрос");
        }

        List<String> normalizedOptions = normalizeOptions(request.getOptions());
        OffsetDateTime now = OffsetDateTime.now();
        User currentUser = currentUserService.getCurrentUserOrThrow();

        Poll poll = pollMapper.toPoll(event, currentUser, request.getQuestion(), now);
        Poll savedPoll = pollRepository.save(poll);

        List<PollOption> savedOptions = new ArrayList<>();
        for (int i = 0; i < normalizedOptions.size(); i++) {
            PollOption option = pollMapper.toPollOption(savedPoll, normalizedOptions.get(i), i + 1, now);
            savedOptions.add(pollOptionRepository.save(option));
        }

        List<Long> optionIds = savedOptions.stream().map(PollOption::getId).toList();
        pollRedisService.initializeActivePoll(savedPoll.getId(), optionIds);

        PollResponse response = buildPollResponse(savedPoll, savedOptions, pollRedisService.getCounts(savedPoll.getId()));
        webSocketEventService.sendPollStarted(eventId, response);
        return response;
    }

    @Transactional(readOnly = true)
    public PollResponse getActivePollByEventId(Long eventId) {
        Poll poll = pollRepository.findByEventIdAndStatus(eventId, PollStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Активный опрос не найден"));

        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(poll.getId());
        Map<Long, Long> counts = pollRedisService.isPollActive(poll.getId())
                ? pollRedisService.getCounts(poll.getId())
                : buildCountsFromDatabase(poll.getId());

        return buildPollResponse(poll, options, counts);
    }

    @Transactional
    public PollResponse vote(Long pollId, UUID guestToken, VotePollRequest request) {
        Poll poll = getPollOrThrow(pollId);

        if (!pollRedisService.isPollActive(pollId) || poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalArgumentException("Опрос уже закрыт");
        }

        EventGuest eventGuest = eventGuestRepository.findByEventIdAndGuestToken(poll.getEvent().getId(), guestToken)
                .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено"));

        if (!Boolean.TRUE.equals(eventGuest.getTokenActive())) {
            throw new IllegalArgumentException("Токен приглашения недействителен");
        }

        PollOption selectedOption = getPollOptionOrThrow(request.getPollOptionId());

        if (!selectedOption.getPoll().getId().equals(pollId)) {
            throw new IllegalArgumentException("Выбранный вариант ответа не принадлежит этому опросу");
        }

        pollRedisService.saveVote(pollId, eventGuest.getId(), selectedOption.getId());

        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(pollId);
        Map<Long, Long> counts = pollRedisService.getCounts(pollId);

        PollResponse response = buildPollResponse(poll, options, counts);
        webSocketEventService.sendPollUpdated(pollId, response);
        return response;
    }

    @Transactional
    public PollResponse closePoll(Long pollId) {
        Poll poll = getPollOrThrow(pollId);
        validatePollManagementAccess(poll.getEvent());

        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new IllegalArgumentException("Опрос уже закрыт");
        }

        Map<Long, Long> choices = pollRedisService.getChoices(pollId);
        OffsetDateTime now = OffsetDateTime.now();

        for (Map.Entry<Long, Long> entry : choices.entrySet()) {
            Long eventGuestId = entry.getKey();
            Long pollOptionId = entry.getValue();

            EventGuest eventGuest = eventGuestRepository.findById(eventGuestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено"));

            PollOption pollOption = getPollOptionOrThrow(pollOptionId);

            PollVote pollVote = pollVoteRepository.findByPollIdAndEventGuestId(pollId, eventGuestId)
                    .orElseGet(PollVote::new);

            pollVote.setPoll(poll);
            pollVote.setEventGuest(eventGuest);
            pollVote.setPollOption(pollOption);

            if (pollVote.getId() == null) {
                pollVote.setCreatedAt(now);
            }
            pollVote.setUpdatedAt(now);

            pollVoteRepository.save(pollVote);
        }

        poll.setStatus(PollStatus.CLOSED);
        poll.setClosedAt(now);
        poll.setUpdatedAt(now);
        Poll savedPoll = pollRepository.save(poll);

        Map<Long, Long> finalCounts = pollRedisService.getCounts(pollId);
        pollRedisService.closePoll(pollId);
        pollRedisService.cleanup(pollId);

        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(pollId);

        PollResponse response = buildPollResponse(savedPoll, options, finalCounts);
        webSocketEventService.sendPollClosed(savedPoll.getEvent().getId(), pollId, response);
        return response;
    }

    @Transactional(readOnly = true)
    public PollResponse getPollResults(Long pollId) {
        Poll poll = getPollOrThrow(pollId);
        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(pollId);

        Map<Long, Long> counts = poll.getStatus() == PollStatus.ACTIVE && pollRedisService.isPollActive(pollId)
                ? pollRedisService.getCounts(pollId)
                : buildCountsFromDatabase(pollId);

        return buildPollResponse(poll, options, counts);
    }

    private Poll getPollOrThrow(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Опрос не найден"));
    }

    private PollOption getPollOptionOrThrow(Long pollOptionId) {
        return pollOptionRepository.findById(pollOptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Вариант ответа не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие не найдено"));
    }

    private void validatePollManagementAccess(Event event) {
        Long currentUserId = currentUserService.getCurrentUserIdOrThrow();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isCreator = event.getCreator().getId().equals(currentUserId);

        if (!isAdmin && !isCreator) {
            throw new AccessDeniedException("У вас нет прав на управление опросами этого мероприятия");
        }
    }

    private List<String> normalizeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов ответа не должен быть пустым");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String option : options) {
            if (option == null) {
                continue;
            }

            String trimmed = option.trim();
            if (!trimmed.isBlank()) {
                normalized.add(trimmed);
            }
        }

        if (normalized.size() < 2) {
            throw new IllegalArgumentException("Опрос должен содержать минимум два варианта ответа");
        }

        return new ArrayList<>(normalized);
    }

    private Map<Long, Long> buildCountsFromDatabase(Long pollId) {
        List<PollVote> votes = pollVoteRepository.findAllByPollId(pollId);
        java.util.LinkedHashMap<Long, Long> counts = new java.util.LinkedHashMap<>();

        for (PollVote vote : votes) {
            Long optionId = vote.getPollOption().getId();
            counts.put(optionId, counts.getOrDefault(optionId, 0L) + 1);
        }

        return counts;
    }

    private PollResponse buildPollResponse(Poll poll, List<PollOption> options, Map<Long, Long> counts) {
        List<PollOptionResponse> optionResponses = options.stream()
                .map(option -> pollMapper.toPollOptionResponse(
                        option,
                        counts.getOrDefault(option.getId(), 0L)
                ))
                .toList();

        return pollMapper.toPollResponse(poll, optionResponses);
    }
}
