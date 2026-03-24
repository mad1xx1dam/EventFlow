package com.eventflow.eventflow_backend.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CalendarResponse {

    private Integer year;
    private Integer month;
    private List<CalendarEventItemResponse> creatorEvents;
    private List<CalendarEventItemResponse> guestEvents;
}
