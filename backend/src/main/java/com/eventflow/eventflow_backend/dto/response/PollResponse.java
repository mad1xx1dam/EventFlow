package com.eventflow.eventflow_backend.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollResponse {

    private Long id;
    private Long eventId;
    private Long createdByUserId;
    private String question;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime closedAt;
    private List<PollOptionResponse> options;
}