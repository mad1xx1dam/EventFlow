package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.DashboardSummaryResponse;
import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import com.eventflow.eventflow_backend.mapper.DashboardMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventGuestRepository eventGuestRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummary_Success() {
        Long userId = 1L;
        long expectedCreatedCount = 5L;
        long expectedGoingCount = 3L;

        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(userId);
        when(eventRepository.countByCreatorId(userId)).thenReturn(expectedCreatedCount);
        when(eventGuestRepository.countByRegisteredUserIdAndRsvpStatus(userId, RsvpStatus.GOING))
                .thenReturn(expectedGoingCount);
        when(dashboardMapper.toDashboardSummaryResponse(expectedCreatedCount, expectedGoingCount))
                .thenReturn(new DashboardSummaryResponse());

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertNotNull(response);
        verify(eventRepository, times(1)).countByCreatorId(userId);
        verify(eventGuestRepository, times(1))
                .countByRegisteredUserIdAndRsvpStatus(userId, RsvpStatus.GOING);
        verify(dashboardMapper, times(1))
                .toDashboardSummaryResponse(expectedCreatedCount, expectedGoingCount);
    }
}