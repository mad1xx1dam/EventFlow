package com.eventflow.eventflow_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePollRequest {

    @NotBlank(message = "Вопрос обязателен для заполнения")
    @Size(max = 500, message = "Вопрос не должен превышать 500 символов")
    private String question;

    private List<String> options;
}