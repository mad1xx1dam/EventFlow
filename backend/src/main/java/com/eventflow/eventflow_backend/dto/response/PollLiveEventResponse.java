package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollLiveEventResponse {

    private String type;
    private PollResponse poll;
}
