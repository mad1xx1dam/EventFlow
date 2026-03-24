package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.config.properties.MailProperties;
import com.eventflow.eventflow_backend.dto.request.CreateInvitationsRequest;
import com.eventflow.eventflow_backend.dto.request.UpdateRsvpRequest;
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

        List<InvitationResponse> responses = normalizedEmails.stream()
                .map(email -> createOrReuseInvitation(event, email, now))
                .toList();

        return responses;
    }

    @Transactional
    public List<InvitationResponse> resendInvitations(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validateEventManagementAccess(event);

        List<EventGuest> guests = eventGuestRepository.findAllByEventIdOrderByInvitedAtAsc(eventId);

        if (guests.isEmpty()) {
            throw new IllegalArgumentException("Для этого мероприятия нет приглашённых гостей");
        }

        guests.forEach(eventGuest -> mailService.sendInvitationEmail(
                eventGuest.getGuestEmail(),
                event.getTitle(),
                buildInvitationUrl(event.getId(), eventGuest.getGuestToken())
        ));

        return guests.stream()
                .map(eventGuest -> invitationMapper.toInvitationResponse(
                        eventGuest,
                        buildInvitationUrl(event.getId(), eventGuest.getGuestToken())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GuestInvitationDetailsResponse getGuestInvitation(Long eventId, UUID guestToken) {
        EventGuest eventGuest = getActiveInvitationOrThrow(eventId, guestToken);
        String posterUrl = minioService.buildPublicUrl(eventGuest.getEvent().getPosterPath());

        return invitationMapper.toGuestInvitationDetailsResponse(eventGuest, posterUrl);
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

        RsvpCountersResponse counters = rsvpService.getCounters(savedEventGuest.getEvent().getId());
        webSocketEventService.sendRsvpCounters(savedEventGuest.getEvent().getId(), counters);

        String posterUrl = minioService.buildPublicUrl(savedEventGuest.getEvent().getPosterPath());
        return invitationMapper.toGuestInvitationDetailsResponse(savedEventGuest, posterUrl);
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

    private User findRegisteredUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие не найдено"));
    }

    private EventGuest getActiveInvitationOrThrow(Long eventId, UUID guestToken) {
        EventGuest eventGuest = eventGuestRepository.findByGuestToken(guestToken)
                .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено"));

        if (!eventGuest.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Токен приглашения не принадлежит этому мероприятию");
        }

        if (!Boolean.TRUE.equals(eventGuest.getTokenActive())) {
            throw new IllegalArgumentException("Токен приглашения недействителен");
        }

        return eventGuest;
    }

    private void validateEventManagementAccess(Event event) {
        Long currentUserId = currentUserService.getCurrentUserIdOrThrow();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isCreator = event.getCreator().getId().equals(currentUserId);

        if (!isAdmin && !isCreator) {
            throw new AccessDeniedException("У вас нет прав на управление приглашениями этого мероприятия");
        }
    }

    private Set<String> normalizeEmails(List<String> emails) {
        Set<String> normalizedEmails = new LinkedHashSet<>();

        for (String email : emails) {
            if (email == null || email.isBlank()) {
                continue;
            }

            normalizedEmails.add(email.trim().toLowerCase());
        }

        if (normalizedEmails.isEmpty()) {
            throw new IllegalArgumentException("Список email не должен быть пустым");
        }

        return normalizedEmails;
    }

    private String buildInvitationUrl(Long eventId, UUID guestToken) {
        return mailProperties.getInvitationBaseUrl() + "/" + eventId + "/invite/" + guestToken;
    }
}