package com.eventflow.eventflow_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateInvitationsRequest {

    @NotEmpty(message = "Список email не должен быть пустым")
    private List<
            @Email(message = "Некорректный формат email")
            @Size(max = 255, message = "Email не должен превышать 255 символов")
                    String
            > guestEmails;
}