package com.eventflow.eventflow_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VotePollRequest {

    @NotNull(message = "Идентификатор варианта ответа обязателен")
    private Long pollOptionId;
}