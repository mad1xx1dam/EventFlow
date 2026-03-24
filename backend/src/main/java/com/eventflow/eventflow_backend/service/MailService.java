package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.config.properties.MailProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String recipientEmail, UUID token) {
        String verifyUrl = buildVerifyUrl(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(recipientEmail);
        message.setSubject("Подтверждение email");
        message.setText(buildVerificationMessage(verifyUrl));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}", recipientEmail, ex);
            throw new IllegalStateException("Не удалось отправить письмо с подтверждением email");
        }
    }

    @Async("mailTaskExecutor")
    public void sendInvitationEmail(String recipientEmail, String eventTitle, String invitationUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(recipientEmail);
        message.setSubject("Приглашение на мероприятие");
        message.setText(buildInvitationMessage(eventTitle, invitationUrl));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send invitation email to {}", recipientEmail, ex);
            throw new IllegalStateException("Не удалось отправить письмо с приглашением");
        }
    }

    private String buildVerifyUrl(UUID token) {
        return mailProperties.getFrontendBaseUrl() + "/verify-email?token=" + token;
    }

    private String buildVerificationMessage(String verifyUrl) {
        return """
                Здравствуйте!

                Для подтверждения email перейдите по ссылке:
                %s

                Если вы не регистрировались в EventFlow, просто проигнорируйте это письмо.
                """.formatted(verifyUrl);
    }

    private String buildInvitationMessage(String eventTitle, String invitationUrl) {
        return """
            Здравствуйте!

            Вас пригласили на мероприятие: %s

            Перейдите по ссылке, чтобы посмотреть детали и ответить на приглашение:
            %s

            """.formatted(eventTitle, invitationUrl);
    }
}
