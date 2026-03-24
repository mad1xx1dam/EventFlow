package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.request.EventRequest;
import com.eventflow.eventflow_backend.dto.response.EventResponse;
import com.eventflow.eventflow_backend.dto.response.FileUploadResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.EventStatus;
import com.eventflow.eventflow_backend.mapper.EventMapper;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private MinioService minioService;
    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEvent_Success_WithPoster() {
        EventRequest request = new EventRequest();
        MultipartFile poster = mock(MultipartFile.class);
        User currentUser = new User();
        Event event = new Event();
        Event savedEvent = new Event();
        FileUploadResponse uploadResponse = new FileUploadResponse();
        uploadResponse.setObjectPath("posters/test.jpg");

        when(currentUserService.getCurrentUserOrThrow()).thenReturn(currentUser);
        when(poster.isEmpty()).thenReturn(false);
        when(minioService.uploadPoster(poster)).thenReturn(uploadResponse);
        when(eventMapper.toEvent(any(), any(), anyString(), any())).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(savedEvent);
        when(eventMapper.toResponse(any(), any())).thenReturn(new EventResponse());

        EventResponse response = eventService.createEvent(request, poster);

        assertNotNull(response);
        verify(minioService).uploadPoster(poster);
        verify(eventRepository).save(event);
    }

    @Test
    void getEventById_AccessDenied_ThrowsException() {
        Long eventId = 1L;
        Event event = new Event();
        User creator = new User();
        creator.setId(2L);
        event.setCreator(creator);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(1L);
        when(currentUserService.isAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> eventService.getEventById(eventId));
    }

    @Test
    void cancelEvent_Success() {
        Long eventId = 1L;
        Long currentUserId = 1L;

        Event event = new Event();
        User creator = new User();
        creator.setId(currentUserId);
        event.setCreator(creator);
        event.setStatus(EventStatus.ACTIVE);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);
        when(eventMapper.toResponse(any(), any())).thenReturn(new EventResponse());

        eventService.cancelEvent(eventId);

        assertEquals(EventStatus.CANCELLED, event.getStatus());
        verify(eventRepository).save(event);
    }
}