# Email Localization Audit

**Date:** 2026-06-11  
**Scope:** All outgoing communication mechanisms in the CRM  
**Languages supported in UI:** English (`en`), Hebrew (`he`)

---

## Executive Summary

The CRM has **4 SMTP email types** and a separate **in-app alert system** that does not send email. Prior to localization, all system-generated email content was hardcoded English inline in Java. No external templates, no `email.*` translation keys, and no locale resolution for recipients existed.

**Root cause of reported bug:** `EmailService.sendOtp()` built HTML with `lang="en"` and English strings regardless of the user's UI language selection on the login page.

---

## Outgoing SMTP Emails

### 1. Login OTP / Email Verification

| Attribute | Detail |
|-----------|--------|
| **Class / method** | `EmailService.sendOtp(String toEmail, String otpCode)` |
| **Triggered by** | `LoginView.handleLogin()`, `OtpVerificationView.handleResend()` |
| **Template location (before)** | Inline HTML in `EmailService.buildHtml()` |
| **Subject (before)** | Hardcoded: `"Your CRM login code"` |
| **Body (before)** | Hardcoded English HTML (`lang="en"`) |
| **Locale determination (before)** | None — always English |
| **Recipient language respected?** | **No** |
| **Missing translations** | Entire subject, heading, greeting, instructions, disclaimer, footer |
| **Hardcoded English** | All email content |

### 2. Activity Assignment (Internal User)

| Attribute | Detail |
|-----------|--------|
| **Class / method** | `EmailService.sendActivityAssigned(String toEmail, String activityTitle)` |
| **Triggered by** | `ActivityService.create()` when assignee has email |
| **Template location (before)** | Inline HTML text block in `EmailService` |
| **Subject (before)** | `"CRM: Activity assigned to you — " + activityTitle` |
| **Body (before)** | Hardcoded English: heading, body, CTA |
| **Locale determination (before)** | None |
| **Recipient language respected?** | **No** |
| **Missing translations** | Subject prefix, heading, body, CTA |
| **Hardcoded English** | All wrapper content (activity title is dynamic) |

### 3. EMAIL-Type Activity (Outbound to Contact)

| Attribute | Detail |
|-----------|--------|
| **Class / method** | `EmailService.sendEmailActivity(String toEmail, String subject, String body)` |
| **Triggered by** | `ActivityService.create()` when `type == EMAIL` and contact has email |
| **Template location** | None — raw user-entered content |
| **Subject** | Activity title (user-entered) |
| **Body** | Activity description (user-entered) |
| **Locale determination** | N/A — content authored by CRM user in their language |
| **Recipient language respected?** | **N/A** (user-authored content, no system wrapper) |
| **Missing translations** | None for system strings |
| **Hardcoded English** | None (system does not add wrapper text) |

### 4. Monthly Attendance Report (HR)

| Attribute | Detail |
|-----------|--------|
| **Class / method** | `ReportEmailService.sendMonthlyReport(byte[] excelBytes, String monthLabel)` |
| **Triggered by** | `MonthlyReportJob` — cron `0 0 6 1 * *` Asia/Jerusalem |
| **Template location (before)** | Inline plain text in `ReportEmailService` |
| **Subject (before)** | `"Monthly Attendance Report — " + monthLabel` |
| **Body (before)** | Hardcoded English plain text |
| **Locale determination (before)** | None |
| **Recipient language respected?** | **No** |
| **Missing translations** | Subject, greeting, body, notice, signature |
| **Hardcoded English** | Entire message |

---

## Email Types NOT Found

Searched `src/main/java`, `src/main/resources`, services, controllers, schedulers:

| Type | Status |
|------|--------|
| Password reset email | Not implemented |
| Welcome / onboarding email | Not implemented (`UserService.register()` creates user only) |
| User invitation email | Not implemented |
| Registration email verification | Not implemented (only login OTP exists) |
| Alert/notification emails | Not implemented (`Topic.sendMailEnabled` flag exists but unused) |
| Lead/opportunity assignment emails | In-app only |
| Contract/quote/sales-order emails | Not found |
| Thymeleaf / FreeMarker templates | Not found |
| `templates/` folder | Does not exist |

---

## In-App Notifications (Not Email)

These mechanisms deliver **in-app alerts only** (`Alert` table, `NotificationChannel.IN_APP`). Documented for completeness; not in SMTP scope but contain hardcoded English.

### Direct Notifications (`NotificationService`)

| Caller | Message pattern | Email? |
|--------|-----------------|--------|
| `ActivityService.create()` | `"New activity assigned: " + title` | No |
| `ActivityService.resolve()` | `"Activity resolved: " + title` | No |
| `LeadService.create()` | `"New lead assigned: " + title` | No |
| `OpportunityService.update()` (WON) | `"Opportunity WON: " + name` | No |
| `MainLayout.openSendNotificationDialog()` | Admin free text | No |

### Subscription Alerts (`SubscriptionHandlerService`)

- Title: topic name (seeded English)
- Body: `"{entityType} #{id} — {CREATION|REPLACEMENT|REMOVAL}"`
- `Topic.sendMailEnabled` is never read at runtime

### Scheduled Workflow Alerts (`NotificationTriggerService` → `ScheduledTaskProcessor`)

12 workflow types (L-02, L-03, L-08, O-05, O-07, O-08, A-01, A-02, A-03, T-02, T-03, T-05) with hardcoded English titles. No email delivery.

---

## Infrastructure Inspected

| Component | Path | Role |
|-----------|------|------|
| `EmailService` | `src/main/java/com/crm/service/EmailService.java` | Primary SMTP sender |
| `ReportEmailService` | `src/main/java/com/crm/timetracking/service/ReportEmailService.java` | HR monthly report |
| `JavaMailSender` | Spring Boot `spring-boot-starter-mail` | SMTP transport |
| `OtpService` | `src/main/java/com/crm/service/OtpService.java` | OTP generation (not mail) |
| `AsyncConfig` | `src/main/java/com/crm/config/AsyncConfig.java` | `@EnableAsync` for email |
| SMTP config | `application.properties` | Host, port, credentials |
| Report config | `application-postgres.properties` | `app.reporting.accountant-email`, `sender-email` |

### i18n Infrastructure (Available for Reuse)

| Component | Path | Reusable for Email? |
|-----------|------|---------------------|
| `TranslationService` | `src/main/java/com/crm/service/TranslationService.java` | Yes — `translate(Locale, key, args)` |
| `MessageSource` | `src/main/java/com/crm/config/I18nConfig.java` | Yes — `i18n/messages*.properties` |
| `SupportedLocale` | `src/main/java/com/crm/i18n/SupportedLocale.java` | Yes — en/he, RTL detection |
| `LocaleService` | `src/main/java/com/crm/service/LocaleService.java` | Partial — request-bound; emails need explicit locale |
| `User.locale` | Not present before audit | Needed for async/offline recipient resolution |

### Template Engines

None. All email HTML was inline Java strings.

---

## Locale Determination (Before Fix)

| Context | How locale was determined | Problem |
|---------|---------------------------|---------|
| Login OTP | Not considered | UI could be Hebrew; email always English |
| Activity assignment | Not considered | Assignee language ignored |
| Monthly report (scheduled) | Not considered | No request context; always English |
| EMAIL activity to contact | N/A | User-authored content |

**User language storage (before):** Cookie (`CRM_LOCALE`), Vaadin session, `localStorage` — no `users.locale` DB column.

---

## Planned but Unimplemented (Pre-Audit)

| Feature | Evidence | Status |
|---------|----------|--------|
| `email.*` translation keys | `docs/i18n-design.md` Section 2 | Designed, not in properties |
| `i18n/email/*.html` templates | `docs/i18n-design.md` Section 2 | Designed, not on disk |
| `EmailNotificationService` | `README.md` | Class does not exist |
| Alert email on `sendMailEnabled` | `Topic.sendMailEnabled` | Flag stored, never read |
| `NotificationChannel.EMAIL` | Enum defined | Always `IN_APP` |

---

## Summary Table

| # | Email Type | Template (before) | i18n (before) | Recipient Locale (before) |
|---|------------|-------------------|---------------|---------------------------|
| 1 | Login OTP | Inline Java HTML | No | No |
| 2 | Activity assigned | Inline Java HTML | No | No |
| 3 | EMAIL activity to contact | None (user content) | N/A | N/A |
| 4 | Monthly attendance report | Inline Java plain text | No | No |

**Total SMTP email types:** 4  
**Localized before audit:** 0  
**In-app-only notification paths:** 15+ (English hardcoded, no email)
