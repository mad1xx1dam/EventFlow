package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollOptionResponse {

    private Long id;
    private String optionText;
    private Integer position;
    private Long votesCount;
}