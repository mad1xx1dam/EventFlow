package com.eventflow.eventflow_backend.dto.response;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventResponse {

    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private OffsetDateTime startsAt;
    private String address;
    private Double lat;
    private Double lon;
    private String posterPath;
    private String posterUrl;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}