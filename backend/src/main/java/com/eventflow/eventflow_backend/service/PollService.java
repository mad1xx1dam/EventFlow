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
import java.util.stream.Collectors;
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

        PollResponse response = buildPollResponse(
                savedPoll,
                savedOptions,
                pollRedisService.getCounts(savedPoll.getId())
        );

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

    @Transactional(readOnly = true)
    public List<PollResponse> getEventPolls(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validatePollManagementAccess(event);
        return buildPollResponsesForEvent(eventId);
    }

    @Transactional(readOnly = true)
    public List<PollResponse> getGuestPolls(Long eventId, UUID guestToken) {
        EventGuest eventGuest = eventGuestRepository.findByEventIdAndGuestToken(eventId, guestToken)
                .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено"));

        if (!Boolean.TRUE.equals(eventGuest.getTokenActive())) {
            throw new IllegalArgumentException("Ссылка-приглашение больше не активна");
        }

        return buildPollResponsesForGuest(eventId, eventGuest.getId());
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
        Map<Long, Long> counts = pollRedisService.getCounts(pollId);
        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(pollId);

        // Общий WS-ответ без персональных полей конкретного гостя
        PollResponse wsResponse = buildPollResponse(poll, options, counts);
        webSocketEventService.sendPollUpdated(pollId, wsResponse);

        // Персональный HTTP-ответ для текущего гостя
        PollResponse response = buildPollResponse(poll, options, counts);
        response.setVotedByCurrentGuest(true);
        response.setSelectedOptionId(selectedOption.getId());

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

        List<PollVote> existingVotes = pollVoteRepository.findAllByPollId(pollId);
        Map<Long, PollVote> existingVotesMap = existingVotes.stream()
                .collect(Collectors.toMap(v -> v.getEventGuest().getId(), v -> v));

        List<PollVote> votesToSave = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : choices.entrySet()) {
            Long eventGuestId = entry.getKey();
            Long pollOptionId = entry.getValue();

            EventGuest eventGuest = eventGuestRepository.findById(eventGuestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено"));
            PollOption pollOption = getPollOptionOrThrow(pollOptionId);

            PollVote pollVote = existingVotesMap.getOrDefault(eventGuestId, new PollVote());
            pollVote.setPoll(poll);
            pollVote.setEventGuest(eventGuest);
            pollVote.setPollOption(pollOption);

            if (pollVote.getId() == null) {
                pollVote.setCreatedAt(now);
            }
            pollVote.setUpdatedAt(now);

            votesToSave.add(pollVote);
        }

        pollVoteRepository.saveAll(votesToSave);

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

    private List<PollResponse> buildPollResponsesForEvent(Long eventId) {
        return pollRepository.findAllByEventIdOrderByStartedAtDesc(eventId).stream()
                .map(poll -> {
                    List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(poll.getId());
                    Map<Long, Long> counts = poll.getStatus() == PollStatus.ACTIVE && pollRedisService.isPollActive(poll.getId())
                            ? pollRedisService.getCounts(poll.getId())
                            : buildCountsFromDatabase(poll.getId());

                    return buildPollResponse(poll, options, counts);
                })
                .toList();
    }

    private List<PollResponse> buildPollResponsesForGuest(Long eventId, Long eventGuestId) {
        return pollRepository.findAllByEventIdOrderByStartedAtDesc(eventId).stream()
                .map(poll -> buildPollResponseForGuest(poll, eventGuestId))
                .toList();
    }

    private PollResponse buildPollResponseForGuest(Poll poll, Long eventGuestId) {
        List<PollOption> options = pollOptionRepository.findAllByPollIdOrderByPositionAsc(poll.getId());

        Map<Long, Long> counts = poll.getStatus() == PollStatus.ACTIVE && pollRedisService.isPollActive(poll.getId())
                ? pollRedisService.getCounts(poll.getId())
                : buildCountsFromDatabase(poll.getId());

        PollResponse response = buildPollResponse(poll, options, counts);

        if (poll.getStatus() == PollStatus.ACTIVE && pollRedisService.isPollActive(poll.getId())) {
            Map<Long, Long> choices = pollRedisService.getChoices(poll.getId());
            Long selectedOptionId = choices.get(eventGuestId);

            response.setVotedByCurrentGuest(selectedOptionId != null);
            response.setSelectedOptionId(selectedOptionId);
        } else {
            response.setVotedByCurrentGuest(false);
            response.setSelectedOptionId(null);
        }

        return response;
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
            throw new AccessDeniedException("У вас нет прав для управления опросами этого мероприятия");
        }
    }

    private List<String> normalizeOptions(List<String> options) {
        Set<String> normalizedOptions = new LinkedHashSet<>();

        for (String option : options) {
            if (option != null) {
                String trimmed = option.trim();
                if (!trimmed.isBlank()) {
                    normalizedOptions.add(trimmed);
                }
            }
        }

        if (normalizedOptions.size() < 2) {
            throw new IllegalArgumentException("Нужно указать минимум два варианта ответа");
        }

        return new ArrayList<>(normalizedOptions);
    }

    private Map<Long, Long> buildCountsFromDatabase(Long pollId) {
        return pollVoteRepository.findAllByPollId(pollId).stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getPollOption().getId(),
                        Collectors.counting()
                ));
    }

    private PollResponse buildPollResponse(Poll poll, List<PollOption> options, Map<Long, Long> counts) {
        List<PollOptionResponse> optionResponses = options.stream()
                .map(option -> pollMapper.toPollOptionResponse(option, counts.getOrDefault(option.getId(), 0L)))
                .toList();

        PollResponse response = pollMapper.toPollResponse(poll, optionResponses);
        response.setVotedByCurrentGuest(false);
        response.setSelectedOptionId(null);
        return response;
    }
}