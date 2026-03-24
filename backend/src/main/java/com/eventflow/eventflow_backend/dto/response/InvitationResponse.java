package com.eventflow.eventflow_backend.dto.response;

import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvitationResponse {

    private Long id;
    private Long eventId;
    private Long registeredUserId;
    private String guestEmail;
    private UUID guestToken;
    private RsvpStatus rsvpStatus;
    private Boolean tokenActive;
    private OffsetDateTime invitedAt;
    private OffsetDateTime respondedAt;
    private String invitationUrl;
}