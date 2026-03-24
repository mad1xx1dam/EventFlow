package com.eventflow.eventflow_backend.dto.response;

import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuestInvitationDetailsResponse {

    private Long eventId;
    private String title;
    private String description;
    private OffsetDateTime startsAt;
    private String address;
    private Double lat;
    private Double lon;
    private String posterUrl;

    private String guestEmail;
    private RsvpStatus rsvpStatus;
}