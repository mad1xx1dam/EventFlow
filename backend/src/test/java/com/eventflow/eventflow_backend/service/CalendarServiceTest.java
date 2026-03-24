package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.CalendarResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.mapper.DashboardMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventGuestRepository eventGuestRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private CalendarService calendarService;

    @Test
    void getCalendar_Success() {
        Long userId = 1L;
        Integer year = 2026;
        Integer month = 4;

        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(userId);
        when(eventRepository.findAllByCreatorIdAndStartsAtBetweenOrderByStartsAtAsc(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(new Event()));
        when(eventGuestRepository.findAllByRegisteredUserIdAndEvent_StartsAtBetweenOrderByEvent_StartsAtAsc(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(new EventGuest()));

        CalendarResponse response = calendarService.getCalendar(year, month);

        assertNotNull(response);
        assertEquals(year, response.getYear());
        assertEquals(month, response.getMonth());
        verify(eventRepository, times(1))
                .findAllByCreatorIdAndStartsAtBetweenOrderByStartsAtAsc(anyLong(), any(), any());
        verify(eventGuestRepository, times(1))
                .findAllByRegisteredUserIdAndEvent_StartsAtBetweenOrderByEvent_StartsAtAsc(anyLong(), any(), any());
    }

    @Test
    void getCalendar_InvalidMonth_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> calendarService.getCalendar(2026, 13));
        assertThrows(IllegalArgumentException.class, () -> calendarService.getCalendar(2026, 0));
    }

    @Test
    void getCalendar_InvalidYear_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> calendarService.getCalendar(1999, 5));
    }
}