package com.crm.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendActivityAssigned(String toEmail, String activityTitle) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("CRM: Activity assigned to you — " + activityTitle);
            helper.setText("""
                <div style="font-family:'Segoe UI',Arial,sans-serif;background:#f4f6fb;padding:32px;">
                  <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:10px;
                              box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden;">
                    <div style="background:#1565c0;padding:24px 32px;">
                      <h2 style="margin:0;color:#fff;font-size:18px;">New Activity Assigned</h2>
                    </div>
                    <div style="padding:28px 32px;">
                      <p style="margin:0 0 16px;color:#333;">You have been assigned a new activity:</p>
                      <div style="background:#f0f4ff;border-left:4px solid #1565c0;padding:12px 16px;
                                  border-radius:4px;font-weight:600;color:#1565c0;font-size:15px;">
                        %s
                      </div>
                      <p style="margin:16px 0 0;color:#888;font-size:12px;">Log in to the CRM to view details.</p>
                    </div>
                  </div>
                </div>""".formatted(activityTitle), true);
            mailSender.send(mime);
            log.info("Activity assigned email sent to {}", toEmail);
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
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your CRM login code");
            helper.setText(buildHtml(otpCode), true);
            mailSender.send(mime);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildHtml(String otpCode) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#f4f6fb;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:40px 0;">
                <tr><td align="center">
                  <table width="480" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;box-shadow:0 2px 16px rgba(0,0,0,0.08);overflow:hidden;">

                    <!-- Header -->
                    <tr>
                      <td style="background:#1565c0;padding:32px 40px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">
                          CRM Login Verification
                        </h1>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:40px 40px 24px;">
                        <p style="margin:0 0 8px;color:#333;font-size:15px;">Hello,</p>
                        <p style="margin:0 0 28px;color:#555;font-size:14px;line-height:1.6;">
                          Use the code below to complete your sign-in. It expires in
                          <strong>5 minutes</strong>.
                        </p>

                        <!-- OTP box -->
                        <div style="background:#f0f4ff;border:2px dashed #1565c0;border-radius:10px;
                                    text-align:center;padding:24px 16px;margin-bottom:28px;">
                          <span style="font-size:42px;font-weight:800;letter-spacing:14px;
                                       color:#1565c0;font-family:'Courier New',monospace;">
                            %s
                          </span>
                        </div>

                        <p style="margin:0;color:#888;font-size:12px;line-height:1.6;">
                          If you didn't request this code, you can safely ignore this email.
                          Someone may have typed your address by mistake.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8f9fc;padding:20px 40px;border-top:1px solid #e8eaf0;text-align:center;">
                        <p style="margin:0;color:#aaa;font-size:11px;">
                          This is an automated message from your CRM system. Do not reply.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(otpCode);
    }
}
