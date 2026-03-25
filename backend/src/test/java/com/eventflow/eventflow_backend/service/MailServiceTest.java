package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.config.properties.MailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private MailService mailService;

    @Test
    void sendVerificationEmail_Success() {
        UUID token = UUID.randomUUID();
        String recipientEmail = "user@test.com";

        when(mailProperties.getFrom()).thenReturn("noreply@eventflow.com");
        when(mailProperties.getFrontendBaseUrl()).thenReturn("http://localhost:3000");

        mailService.sendVerificationEmail(recipientEmail, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(recipientEmail, sentMessage.getTo()[0]);
        assertEquals("noreply@eventflow.com", sentMessage.getFrom());
        assertTrue(sentMessage.getText().contains(token.toString()));
    }

    @Test
    void sendVerificationEmail_MailSenderThrowsException() {
        UUID token = UUID.randomUUID();

        when(mailProperties.getFrom()).thenReturn("noreply@eventflow.com");
        when(mailProperties.getFrontendBaseUrl()).thenReturn("http://localhost:3000");
        doThrow(new RuntimeException("SMTP connection failed")).when(mailSender).send(any(SimpleMailMessage.class));

        // проверяем, что сервис оборачивает ошибку отправки в свой IllegalStateException
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> mailService.sendVerificationEmail("user@test.com", token)
        );
        assertTrue(exception.getMessage().contains("Не удалось отправить"));
    }

    @Test
    void sendInvitationEmail_Success() {
        String recipientEmail = "guest@test.com";
        String eventTitle = "Project Demo";
        String invitationUrl = "http://localhost:3000/events/1/invite/abc";

        when(mailProperties.getFrom()).thenReturn("noreply@eventflow.com");

        mailService.sendInvitationEmail(recipientEmail, eventTitle, invitationUrl);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(recipientEmail, sentMessage.getTo()[0]);
        assertEquals("noreply@eventflow.com", sentMessage.getFrom());
        assertTrue(sentMessage.getText().contains(eventTitle));
        assertTrue(sentMessage.getText().contains(invitationUrl));
    }

    @Test
    void sendInvitationEmail_MailSenderThrowsException() {
        when(mailProperties.getFrom()).thenReturn("noreply@eventflow.com");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(
                IllegalStateException.class,
                () -> mailService.sendInvitationEmail("guest@test.com", "Title", "http://url")
        );
    }
}