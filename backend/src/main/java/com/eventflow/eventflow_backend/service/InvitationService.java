package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.config.properties.MailProperties;
import com.eventflow.eventflow_backend.dto.request.CreateInvitationsRequest;
import com.eventflow.eventflow_backend.dto.request.UpdateRsvpRequest;
import com.eventflow.eventflow_backend.dto.response.EventRsvpSnapshotResponse;
import com.eventflow.eventflow_backend.dto.response.GuestInvitationDetailsResponse;
import com.eventflow.eventflow_backend.dto.response.InvitationResponse;
import com.eventflow.eventflow_backend.dto.response.RsvpCountersResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import com.eventflow.eventflow_backend.exception.ResourceNotFoundException;
import com.eventflow.eventflow_backend.mapper.InvitationMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.repository.UserRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final InvitationMapper invitationMapper;
    private final MailService mailService;
    private final MailProperties mailProperties;
    private final MinioService minioService;
    private final RsvpService rsvpService;
    private final WebSocketEventService webSocketEventService;

    @Transactional
    public List<InvitationResponse> createInvitations(Long eventId, CreateInvitationsRequest request) {
        Event event = getEventOrThrow(eventId);
        validateEventManagementAccess(event);

        Set<String> normalizedEmails = normalizeEmails(request.getGuestEmails());
        OffsetDateTime now = OffsetDateTime.now();

        return normalizedEmails.stream()
                .map(email -> createOrReuseInvitation(event, email, now))
                .toList();
    }

    @Transactional
    public List<InvitationResponse> resendInvitations(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validateEventManagementAccess(event);

        List<EventGuest> guests = eventGuestRepository.findAllByEventIdOrderByInvitedAtAsc(eventId).stream()
                .filter(eventGuest -> eventGuest.getRsvpStatus() == RsvpStatus.PENDING)
                .toList();

        if (guests.isEmpty()) {
            throw new IllegalArgumentException("Для этого мероприятия нет гостей, ожидающих ответа");
        }

        guests.forEach(eventGuest -> mailService.sendInvitationEmail(
                eventGuest.getGuestEmail(),
                event.getTitle(),
                buildInvitationUrl(event.getId(), eventGuest.getGuestToken())
        ));

        return guests.stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuestInvitationDetailsResponse getGuestInvitation(Long eventId, UUID guestToken) {
        EventGuest eventGuest = getActiveInvitationOrThrow(eventId, guestToken);
        String posterUrl = minioService.buildPublicUrl(eventGuest.getEvent().getPosterPath());

        GuestInvitationDetailsResponse response =
                invitationMapper.toGuestInvitationDetailsResponse(eventGuest, posterUrl);

        RsvpCountersResponse counters = rsvpService.getCounters(eventId);
        response.setGoingCount(counters.getGoingCount());
        response.setMaybeCount(counters.getMaybeCount());
        response.setDeclinedCount(counters.getDeclinedCount());
        response.setPendingCount(counters.getPendingCount());

        return response;
    }

    @Transactional
    public GuestInvitationDetailsResponse updateRsvp(Long eventId, UUID guestToken, UpdateRsvpRequest request) {
        EventGuest eventGuest = getActiveInvitationOrThrow(eventId, guestToken);

        if (request.getRsvpStatus() == RsvpStatus.PENDING) {
            throw new IllegalArgumentException("Нельзя установить RSVP-статус PENDING вручную");
        }

        eventGuest.setRsvpStatus(request.getRsvpStatus());
        eventGuest.setRespondedAt(OffsetDateTime.now());

        EventGuest savedEventGuest = eventGuestRepository.save(eventGuest);

        EventRsvpSnapshotResponse snapshot = buildEventRsvpSnapshot(savedEventGuest.getEvent().getId());
        webSocketEventService.sendRsvpSnapshot(savedEventGuest.getEvent().getId(), snapshot);

        String posterUrl = minioService.buildPublicUrl(savedEventGuest.getEvent().getPosterPath());
        GuestInvitationDetailsResponse response =
                invitationMapper.toGuestInvitationDetailsResponse(savedEventGuest, posterUrl);

        response.setGoingCount(snapshot.getGoingCount());
        response.setMaybeCount(snapshot.getMaybeCount());
        response.setDeclinedCount(snapshot.getDeclinedCount());
        response.setPendingCount(snapshot.getPendingCount());

        return response;
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getEventGuests(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validateEventManagementAccess(event);

        return eventGuestRepository.findAllByEventIdOrderByInvitedAtAsc(eventId).stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventRsvpSnapshotResponse buildEventRsvpSnapshot(Long eventId) {
        List<InvitationResponse> guests = eventGuestRepository.findAllByEventIdOrderByInvitedAtAsc(eventId).stream()
                .map(this::toInvitationResponse)
                .toList();

        RsvpCountersResponse counters = rsvpService.getCounters(eventId);

        EventRsvpSnapshotResponse response = new EventRsvpSnapshotResponse();
        response.setEventId(eventId);
        response.setGoingCount(counters.getGoingCount());
        response.setMaybeCount(counters.getMaybeCount());
        response.setDeclinedCount(counters.getDeclinedCount());
        response.setPendingCount(counters.getPendingCount());
        response.setGuests(guests);

        return response;
    }

    private InvitationResponse createOrReuseInvitation(Event event, String email, OffsetDateTime now) {
        EventGuest eventGuest = eventGuestRepository.findByEventIdAndGuestEmail(event.getId(), email)
                .orElseGet(() -> buildNewInvitation(event, email, now));

        EventGuest savedEventGuest = eventGuestRepository.save(eventGuest);

        String invitationUrl = buildInvitationUrl(event.getId(), savedEventGuest.getGuestToken());
        mailService.sendInvitationEmail(savedEventGuest.getGuestEmail(), event.getTitle(), invitationUrl);

        return invitationMapper.toInvitationResponse(savedEventGuest, invitationUrl);
    }

    private EventGuest buildNewInvitation(Event event, String email, OffsetDateTime now) {
        EventGuest eventGuest = new EventGuest();
        eventGuest.setEvent(event);
        eventGuest.setRegisteredUser(findRegisteredUserByEmail(email));
        eventGuest.setGuestEmail(email);
        eventGuest.setGuestToken(UUID.randomUUID());
        eventGuest.setRsvpStatus(RsvpStatus.PENDING);
        eventGuest.setTokenActive(true);
        eventGuest.setInvitedAt(now);
        eventGuest.setRespondedAt(null);
        return eventGuest;
    }

    private InvitationResponse toInvitationResponse(EventGuest eventGuest) {
        return invitationMapper.toInvitationResponse(
                eventGuest,
                buildInvitationUrl(eventGuest.getEvent().getId(), eventGuest.getGuestToken())
        );
    }

    private EventGuest getActiveInvitationOrThrow(Long eventId, UUID guestToken) {
        return eventGuestRepository.findByEventIdAndGuestToken(eventId, guestToken)
                .filter(eventGuest -> Boolean.TRUE.equals(eventGuest.getTokenActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено или больше не активно"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие не найдено"));
    }

    private void validateEventManagementAccess(Event event) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        boolean isAdmin = "ADMIN".equals(currentUser.getRole().getName());

        if (isAdmin) {
            return;
        }

        if (!event.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("У вас нет прав для управления этим мероприятием");
        }
    }

    private Set<String> normalizeEmails(List<String> emails) {
        Set<String> normalizedEmails = new LinkedHashSet<>();

        for (String email : emails) {
            if (email != null && !email.isBlank()) {
                normalizedEmails.add(email.trim().toLowerCase());
            }
        }

        return normalizedEmails;
    }

    private User findRegisteredUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private String buildInvitationUrl(Long eventId, UUID guestToken) {
        return mailProperties.getInvitationBaseUrl()
                + eventId
                + "/invite/"
                + guestToken;
    }
}