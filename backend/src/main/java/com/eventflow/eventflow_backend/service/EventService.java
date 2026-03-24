package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.request.EventRequest;
import com.eventflow.eventflow_backend.dto.response.EventResponse;
import com.eventflow.eventflow_backend.dto.response.FileUploadResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.EventStatus;
import com.eventflow.eventflow_backend.exception.ResourceNotFoundException;
import com.eventflow.eventflow_backend.mapper.EventMapper;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final MinioService minioService;
    private final EventMapper eventMapper;

    @Transactional
    public EventResponse createEvent(EventRequest request, MultipartFile poster) {
        User currentUser = currentUserService.getCurrentUserOrThrow();

        String posterPath = null;

        if (poster != null && !poster.isEmpty()) {
            FileUploadResponse uploadResponse = minioService.uploadPoster(poster);
            posterPath = uploadResponse.getObjectPath();
        }

        Event event = eventMapper.toEvent(request, currentUser, posterPath, OffsetDateTime.now());
        Event savedEvent = eventRepository.save(event);

        return buildEventResponse(savedEvent);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validateAccess(event);
        return buildEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents() {
        Long currentUserId = currentUserService.getCurrentUserIdOrThrow();

        return eventRepository.findAllByCreatorIdOrderByStartsAtAsc(currentUserId).stream()
                .map(this::buildEventResponse)
                .toList();
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, MultipartFile poster) {
        Event event = getEventOrThrow(eventId);
        validateAccess(event);

        eventMapper.updateEventFromRequest(request, event);

        if (poster != null && !poster.isEmpty()) {
            String oldPosterPath = event.getPosterPath();

            FileUploadResponse uploadResponse = minioService.uploadPoster(poster);
            event.setPosterPath(uploadResponse.getObjectPath());

            if (oldPosterPath != null && !oldPosterPath.isBlank()) {
                minioService.deleteObject(oldPosterPath);
            }
        }

        event.setUpdatedAt(OffsetDateTime.now());

        Event savedEvent = eventRepository.save(event);
        return buildEventResponse(savedEvent);
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId) {
        Event event = getEventOrThrow(eventId);
        validateAccess(event);

        event.setStatus(EventStatus.CANCELLED);
        event.setUpdatedAt(OffsetDateTime.now());

        Event savedEvent = eventRepository.save(event);
        return buildEventResponse(savedEvent);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие не найдено"));
    }

    private void validateAccess(Event event) {
        Long currentUserId = currentUserService.getCurrentUserIdOrThrow();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isCreator = event.getCreator().getId().equals(currentUserId);

        if (!isAdmin && !isCreator) {
            throw new AccessDeniedException("У вас нет прав на управление этим мероприятием");
        }
    }

    private EventResponse buildEventResponse(Event event) {
        String posterUrl = minioService.buildPublicUrl(event.getPosterPath());
        return eventMapper.toResponse(event, posterUrl);
    }
}