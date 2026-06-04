package com.crm.timetracking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ReportEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.reporting.accountant-email:accountant@yourcompany.co.il}")
    private String accountantEmail;

    @Value("${app.reporting.sender-email:crm-reports@yourcompany.co.il}")
    private String senderEmail;

    public ReportEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMonthlyReport(byte[] excelBytes, String monthLabel) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(accountantEmail);
        helper.setSubject("Monthly Attendance Report — " + monthLabel);
        helper.setText(
                "Hi,\n\n" +
                "Please find attached the attendance report for " + monthLabel + ".\n\n" +
                "This report was generated automatically by the CRM system.\n\n" +
                "Regards,\nCRM Attendance System");
        helper.addAttachment(
                "attendance-report-" + monthLabel + ".xlsx",
                new ByteArrayResource(excelBytes),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mailSender.send(message);
    }
}
