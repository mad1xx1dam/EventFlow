package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.DashboardSummaryResponse;
import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import com.eventflow.eventflow_backend.mapper.DashboardMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;
    private final CurrentUserService currentUserService;
    private final DashboardMapper dashboardMapper;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Long currentUserId = currentUserService.getCurrentUserIdOrThrow();

        long createdEventsCount = eventRepository.countByCreatorId(currentUserId);
        long acceptedInvitationsCount = eventGuestRepository.countByRegisteredUserIdAndRsvpStatus(
                currentUserId,
                RsvpStatus.GOING
        );

        return dashboardMapper.toDashboardSummaryResponse(createdEventsCount, acceptedInvitationsCount);
    }
}
