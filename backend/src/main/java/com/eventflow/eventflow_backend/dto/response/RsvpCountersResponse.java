package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RsvpCountersResponse {

    private Long eventId;
    private Long goingCount;
    private Long maybeCount;
    private Long declinedCount;
    private Long pendingCount;
}
