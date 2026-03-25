package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.config.properties.MailProperties;
import com.eventflow.eventflow_backend.dto.response.GuestInvitationDetailsResponse;
import com.eventflow.eventflow_backend.dto.response.InvitationResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.mapper.InvitationMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventGuestRepository eventGuestRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private InvitationMapper invitationMapper;
    @Mock
    private MailService mailService;
    @Mock
    private MailProperties mailProperties;
    @Mock
    private MinioService minioService;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void resendInvitations_Success() {
        Long eventId = 1L;
        Long currentUserId = 1L;

        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Test Event");
        User creator = new User();
        creator.setId(currentUserId);
        event.setCreator(creator);

        EventGuest guest = new EventGuest();
        guest.setGuestEmail("guest@test.com");
        guest.setGuestToken(UUID.randomUUID());

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(eventGuestRepository.findAllByEventIdOrderByInvitedAtAsc(eventId))
                .thenReturn(List.of(guest));
        when(mailProperties.getInvitationBaseUrl()).thenReturn("http://localhost");
        when(invitationMapper.toInvitationResponse(any(), anyString()))
                .thenReturn(new InvitationResponse());

        List<InvitationResponse> responses = invitationService.resendInvitations(eventId);

        assertEquals(1, responses.size());
        verify(mailService, times(1)).sendInvitationEmail(
                eq("guest@test.com"),
                eq("Test Event"),
                anyString()
        );
    }

    @Test
    void resendInvitations_EmptyList_ThrowsException() {
        Long eventId = 1L;
        Long currentUserId = 1L;

        Event event = new Event();
        event.setId(eventId);
        User creator = new User();
        creator.setId(currentUserId);
        event.setCreator(creator);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(eventGuestRepository.findAllByEventIdOrderByInvitedAtAsc(eventId))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> invitationService.resendInvitations(eventId));
    }

    @Test
    void getGuestInvitation_Success() {
        Long eventId = 1L;
        UUID token = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);
        event.setPosterPath("poster.png");

        EventGuest guest = new EventGuest();
        guest.setEvent(event);
        guest.setTokenActive(true);

        when(eventGuestRepository.findByGuestToken(token)).thenReturn(Optional.of(guest));
        when(minioService.buildPublicUrl(anyString())).thenReturn("http://minio/poster.png");
        when(invitationMapper.toGuestInvitationDetailsResponse(any(), anyString()))
                .thenReturn(new GuestInvitationDetailsResponse());

        GuestInvitationDetailsResponse response = invitationService.getGuestInvitation(eventId, token);

        assertNotNull(response);
        verify(eventGuestRepository).findByGuestToken(token);
        verify(minioService).buildPublicUrl("poster.png");
    }
}