package com.eventflow.eventflow_backend.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventRequest {

    @NotBlank(message = "Название мероприятия обязательно для заполнения")
    @Size(max = 255, message = "Название мероприятия не должно превышать 255 символов")
    private String title;

    @Size(max = 5000, message = "Описание мероприятия не должно превышать 5000 символов")
    private String description;

    @NotNull(message = "Дата и время начала обязательны для заполнения")
    @Future(message = "Дата и время начала должны быть в будущем")
    private OffsetDateTime startsAt;

    @NotBlank(message = "Адрес обязателен для заполнения")
    @Size(max = 500, message = "Адрес не должен превышать 500 символов")
    private String address;

    private Double lat;

    private Double lon;
}