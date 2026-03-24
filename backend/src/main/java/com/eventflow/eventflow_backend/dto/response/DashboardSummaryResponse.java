package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DashboardSummaryResponse {

    private Long createdEventsCount;
    private Long acceptedInvitationsCount;
}