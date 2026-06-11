package com.crm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;
    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        LocaleService localeService = mock(LocaleService.class);
        translationService = new TranslationService(messageSource, localeService);
        emailTemplateService = new EmailTemplateService();
    }

    @Test
    void renderOtp_english_hasLtrAttributes() {
        String html = emailTemplateService.renderHtml("otp", Locale.ENGLISH, otpVars(Locale.ENGLISH));

        assertTrue(html.contains("lang=\"en\""));
        assertTrue(html.contains("dir=\"ltr\""));
        assertTrue(html.contains("CRM Login Verification"));
        assertTrue(html.contains("123456"));
        assertFalse(html.contains("{{"));
    }

    @Test
    void renderOtp_hebrew_hasRtlAttributesAndHebrewText() {
        Locale hebrew = Locale.forLanguageTag("he");
        String html = emailTemplateService.renderHtml("otp", hebrew, otpVars(hebrew));

        assertTrue(html.contains("lang=\"he\""));
        assertTrue(html.contains("dir=\"rtl\""));
        assertTrue(html.contains("אימות התחברות ל-CRM"));
        assertTrue(html.contains("123456"));
        assertFalse(html.contains("{{"));
    }

    @Test
    void renderActivityAssigned_hebrew_hasRtlAttributes() {
        Locale hebrew = Locale.forLanguageTag("he");
        String html = emailTemplateService.renderHtml("activity-assigned", hebrew, Map.of(
                "heading", translationService.translate(hebrew, "email.activityAssigned.heading"),
                "body", translationService.translate(hebrew, "email.activityAssigned.body"),
                "activityTitle", "פגישה עם לקוח",
                "cta", translationService.translate(hebrew, "email.activityAssigned.cta")
        ));

        assertTrue(html.contains("dir=\"rtl\""));
        assertTrue(html.contains("lang=\"he\""));
        assertTrue(html.contains("פעילות חדשה הוקצתה לך"));
        assertTrue(html.contains("פגישה עם לקוח"));
    }

    @Test
    void renderMonthlyReport_english_hasNoPlaceholders() {
        String text = emailTemplateService.renderPlainText("monthly-report", Locale.ENGLISH, Map.of(
                "greeting", translationService.translate(Locale.ENGLISH, "email.monthlyReport.greeting"),
                "body", translationService.translate(Locale.ENGLISH, "email.monthlyReport.body", "2025-05"),
                "notice", translationService.translate(Locale.ENGLISH, "email.monthlyReport.notice"),
                "regards", translationService.translate(Locale.ENGLISH, "email.monthlyReport.regards"),
                "signature", translationService.translate(Locale.ENGLISH, "email.monthlyReport.signature")
        ));

        assertTrue(text.contains("Hi,"));
        assertTrue(text.contains("2025-05"));
        assertTrue(text.contains("CRM Attendance System"));
        assertFalse(text.contains("{{"));
    }

    @Test
    void renderMonthlyReport_hebrew_hasHebrewText() {
        Locale hebrew = Locale.forLanguageTag("he");
        String text = emailTemplateService.renderPlainText("monthly-report", hebrew, Map.of(
                "greeting", translationService.translate(hebrew, "email.monthlyReport.greeting"),
                "body", translationService.translate(hebrew, "email.monthlyReport.body", "2025-05"),
                "notice", translationService.translate(hebrew, "email.monthlyReport.notice"),
                "regards", translationService.translate(hebrew, "email.monthlyReport.regards"),
                "signature", translationService.translate(hebrew, "email.monthlyReport.signature")
        ));

        assertTrue(text.contains("שלום,"));
        assertTrue(text.contains("דוח הנוכחות"));
        assertTrue(text.contains("מערכת נוכחות CRM"));
        assertFalse(text.contains("{{"));
    }

    private Map<String, String> otpVars(Locale locale) {
        return Map.of(
                "heading", translationService.translate(locale, "email.otp.heading"),
                "greeting", translationService.translate(locale, "email.otp.greeting"),
                "instructions", translationService.translate(locale, "email.otp.instructions", 5),
                "otpCode", "123456",
                "disclaimer", translationService.translate(locale, "email.otp.disclaimer"),
                "footer", translationService.translate(locale, "email.otp.footer")
        );
    }
}
