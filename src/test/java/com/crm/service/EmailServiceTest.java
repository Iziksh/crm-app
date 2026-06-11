package com.crm.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;
    @Mock TranslationService translationService;
    @Mock EmailTemplateService emailTemplateService;
    @Mock EmailLocaleResolver emailLocaleResolver;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLocaleResolver.resolveForEmail(anyString())).thenReturn(Locale.ENGLISH);
        when(translationService.translate(any(Locale.class), anyString())).thenReturn("Subject");
        when(translationService.translate(any(Locale.class), anyString(), any())).thenReturn("Instructions");
        when(emailTemplateService.renderHtml(anyString(), any(Locale.class), anyMap())).thenReturn("<html>body</html>");

        emailService = new EmailService(mailSender, "from@example.com",
                translationService, emailTemplateService, emailLocaleResolver, 5);
    }

    @Test
    void sendOtp_sendsMimeMessage() {
        emailService.sendOtp("to@example.com", "123456");

        verify(mailSender).send(mimeMessage);
        verify(translationService).translate(Locale.ENGLISH, "email.otp.subject");
        verify(emailTemplateService).renderHtml(eq("otp"), eq(Locale.ENGLISH), anyMap());
    }

    @Test
    void sendOtp_usesExplicitLocale() {
        Locale hebrew = Locale.forLanguageTag("he");
        emailService.sendOtp("to@example.com", "123456", hebrew);

        verify(translationService).translate(hebrew, "email.otp.subject");
        verify(emailTemplateService).renderHtml(eq("otp"), eq(hebrew), anyMap());
    }

    @Test
    void sendActivityAssigned_sendsMimeMessage() {
        when(translationService.translate(any(Locale.class), eq("email.activityAssigned.subject"), anyString()))
                .thenReturn("Assigned");
        when(emailTemplateService.renderHtml(eq("activity-assigned"), any(Locale.class), anyMap()))
                .thenReturn("<div>assigned</div>");

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

        emailService.sendOtp("to@example.com", "123456");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendActivityAssigned_doesNotThrow_whenMailSenderFails() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        emailService.sendActivityAssigned("to@example.com", "Task");
    }
}
