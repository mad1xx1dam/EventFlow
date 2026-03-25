package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.RsvpCountersResponse;
import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RsvpServiceTest {

    @Mock
    private EventGuestRepository eventGuestRepository;

    @InjectMocks
    private RsvpService rsvpService;

    @Test
    void getCounters_Success() {
        Long eventId = 1L;

        when(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.GOING)).thenReturn(10L);
        when(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE)).thenReturn(5L);
        when(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.DECLINED)).thenReturn(2L);
        when(eventGuestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.PENDING)).thenReturn(8L);

        RsvpCountersResponse response = rsvpService.getCounters(eventId);

        assertEquals(eventId, response.getEventId());
        assertEquals(10L, response.getGoingCount());
        assertEquals(5L, response.getMaybeCount());
        assertEquals(2L, response.getDeclinedCount());
        assertEquals(8L, response.getPendingCount());
    }
}