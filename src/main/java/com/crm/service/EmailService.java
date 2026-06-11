package com.crm.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final TranslationService translationService;
    private final EmailTemplateService emailTemplateService;
    private final EmailLocaleResolver emailLocaleResolver;
    private final int otpExpiryMinutes;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress,
                        TranslationService translationService,
                        EmailTemplateService emailTemplateService,
                        EmailLocaleResolver emailLocaleResolver,
                        @Value("${otp.expiry-minutes:5}") int otpExpiryMinutes) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.translationService = translationService;
        this.emailTemplateService = emailTemplateService;
        this.emailLocaleResolver = emailLocaleResolver;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    @Async
    public void sendActivityAssigned(String toEmail, String activityTitle) {
        sendActivityAssigned(toEmail, activityTitle, emailLocaleResolver.resolveForEmail(toEmail));
    }

    @Async
    public void sendActivityAssigned(String toEmail, String activityTitle, Locale locale) {
        try {
            Locale resolved = locale != null ? locale : emailLocaleResolver.resolveForEmail(toEmail);
            String subject = translationService.translate(resolved, "email.activityAssigned.subject", activityTitle);
            String html = emailTemplateService.renderHtml("activity-assigned", resolved, Map.of(
                    "heading", translationService.translate(resolved, "email.activityAssigned.heading"),
                    "body", translationService.translate(resolved, "email.activityAssigned.body"),
                    "activityTitle", activityTitle,
                    "cta", translationService.translate(resolved, "email.activityAssigned.cta")
            ));

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Activity assigned email sent to {} (locale={})", toEmail, resolved.getLanguage());
        } catch (Exception e) {
            log.error("Failed to send activity assigned email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendEmailActivity(String toEmail, String subject, String body) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(mime);
            log.info("EMAIL activity sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send EMAIL activity to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOtp(String toEmail, String otpCode) {
        sendOtp(toEmail, otpCode, emailLocaleResolver.resolveForEmail(toEmail));
    }

    @Async
    public void sendOtp(String toEmail, String otpCode, Locale locale) {
        try {
            Locale resolved = locale != null ? locale : emailLocaleResolver.resolveForEmail(toEmail);
            String subject = translationService.translate(resolved, "email.otp.subject");
            String html = emailTemplateService.renderHtml("otp", resolved, Map.of(
                    "heading", translationService.translate(resolved, "email.otp.heading"),
                    "greeting", translationService.translate(resolved, "email.otp.greeting"),
                    "instructions", translationService.translate(resolved, "email.otp.instructions", otpExpiryMinutes),
                    "otpCode", otpCode,
                    "disclaimer", translationService.translate(resolved, "email.otp.disclaimer"),
                    "footer", translationService.translate(resolved, "email.otp.footer")
            ));

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("OTP email sent to {} (locale={})", toEmail, resolved.getLanguage());
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
