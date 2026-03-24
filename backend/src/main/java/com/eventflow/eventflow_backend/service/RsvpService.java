package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.RsvpCountersResponse;
import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final EventGuestRepository eventGuestRepository;

    @Transactional(readOnly = true)
    public RsvpCountersResponse getCounters(Long eventId) {
        RsvpCountersResponse response = new RsvpCountersResponse();
        response.setEventId(eventId);
        response.setGoingCount(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.GOING));
        response.setMaybeCount(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE));
        response.setDeclinedCount(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.DECLINED));
        response.setPendingCount(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.PENDING));
        return response;
    }
}