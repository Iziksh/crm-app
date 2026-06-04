package com.crm.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService = new EmailService(mailSender, "from@example.com");
    }

    @Test
    void sendOtp_sendsMimeMessage() {
        emailService.sendOtp("to@example.com", "123456");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendActivityAssigned_sendsMimeMessage() {
        emailService.sendActivityAssigned("to@example.com", "Follow up with client");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailActivity_sendsMimeMessage() {
        emailService.sendEmailActivity("to@example.com", "Hello", "Body text");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOtp_doesNotThrow_whenMailSenderFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP unavailable"));

        // Should not propagate — @Async wraps in try-catch
        emailService.sendOtp("to@example.com", "123456");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendActivityAssigned_doesNotThrow_whenMailSenderFails() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        emailService.sendActivityAssigned("to@example.com", "Task");

        // exception swallowed — no propagation
    }
}
