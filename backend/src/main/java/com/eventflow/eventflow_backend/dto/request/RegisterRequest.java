package com.eventflow.eventflow_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Имя обязательно для заполнения")
    @Size(max = 150, message = "Имя не должно превышать 150 символов")
    private String name;

    @NotBlank(message = "Email обязателен для заполнения")
    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    private String email;

    @NotBlank(message = "Пароль обязателен для заполнения")
    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов") // Увеличил до 8
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-zа-я])(?=.*[A-ZА-Я]).*$",
            message = "Пароль должен содержать цифры, заглавные и строчные буквы"
    )
    private String password;

    @NotBlank(message = "Подтверждение пароля обязательно для заполнения")
    @Size(min = 6, max = 100, message = "Подтверждение пароля должно содержать от 6 до 100 символов")
    private String confirmPassword;
}