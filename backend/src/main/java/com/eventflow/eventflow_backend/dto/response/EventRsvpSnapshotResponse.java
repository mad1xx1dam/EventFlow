package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class EventRsvpSnapshotResponse {

    private Long eventId;
    private Long goingCount;
    private Long maybeCount;
    private Long declinedCount;
    private Long pendingCount;
    private List<InvitationResponse> guests;
}