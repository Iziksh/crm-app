package com.crm.timetracking.service;

import com.crm.service.EmailLocaleResolver;
import com.crm.service.EmailTemplateService;
import com.crm.service.TranslationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class ReportEmailService {

    private final JavaMailSender mailSender;
    private final TranslationService translationService;
    private final EmailTemplateService emailTemplateService;
    private final EmailLocaleResolver emailLocaleResolver;

    @Value("${app.reporting.accountant-email:accountant@yourcompany.co.il}")
    private String accountantEmail;

    @Value("${app.reporting.sender-email:crm-reports@yourcompany.co.il}")
    private String senderEmail;

    public ReportEmailService(JavaMailSender mailSender,
                              TranslationService translationService,
                              EmailTemplateService emailTemplateService,
                              EmailLocaleResolver emailLocaleResolver) {
        this.mailSender = mailSender;
        this.translationService = translationService;
        this.emailTemplateService = emailTemplateService;
        this.emailLocaleResolver = emailLocaleResolver;
    }

    public void sendMonthlyReport(byte[] excelBytes, String monthLabel) throws MessagingException {
        sendMonthlyReport(excelBytes, monthLabel, emailLocaleResolver.resolveForEmail(accountantEmail));
    }

    public void sendMonthlyReport(byte[] excelBytes, String monthLabel, Locale locale) throws MessagingException {
        Locale resolved = locale != null ? locale : emailLocaleResolver.resolveForEmail(accountantEmail);
        String subject = translationService.translate(resolved, "email.monthlyReport.subject", monthLabel);
        String body = emailTemplateService.renderPlainText("monthly-report", resolved, Map.of(
                "greeting", translationService.translate(resolved, "email.monthlyReport.greeting"),
                "body", translationService.translate(resolved, "email.monthlyReport.body", monthLabel),
                "notice", translationService.translate(resolved, "email.monthlyReport.notice"),
                "regards", translationService.translate(resolved, "email.monthlyReport.regards"),
                "signature", translationService.translate(resolved, "email.monthlyReport.signature")
        ));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(accountantEmail);
        helper.setSubject(subject);
        helper.setText(body);
        helper.addAttachment(
                "attendance-report-" + monthLabel + ".xlsx",
                new ByteArrayResource(excelBytes),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mailSender.send(message);
    }
}
