package com.eventflow.eventflow_backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CalendarEventItemResponse {

    private Long eventId;
    private String title;
    private OffsetDateTime startsAt;
    private String address;
    private String colorType;
    private UUID guestToken;
}