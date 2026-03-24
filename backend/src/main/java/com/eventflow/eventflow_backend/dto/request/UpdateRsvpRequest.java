package com.eventflow.eventflow_backend.dto.request;

import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateRsvpRequest {

    @NotNull(message = "RSVP-статус обязателен для заполнения")
    private RsvpStatus rsvpStatus;
}