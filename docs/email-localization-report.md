# Email Localization Report

**Date:** 2026-06-11  
**Status:** Complete  
**Audit reference:** [`docs/email-localization-audit.md`](email-localization-audit.md)

---

## Emails Audited

| # | Email Type | SMTP? | Localized? |
|---|------------|-------|------------|
| 1 | Login OTP / email verification | Yes | **Yes** |
| 2 | Activity assignment (internal) | Yes | **Yes** |
| 3 | EMAIL-type activity (to contact) | Yes | N/A (user-authored content) |
| 4 | Monthly attendance report (HR) | Yes | **Yes** |
| 5 | Password reset | No | Not implemented |
| 6 | Welcome / invitation | No | Not implemented |
| 7 | Alert/notification emails | No | Not implemented (`sendMailEnabled` unused) |

**Total SMTP types:** 4  
**System-generated types localized:** 3 (OTP, activity assignment, monthly report)

---

## Changes Made

### Language Resolution

Added `EmailLocaleResolver` with priority chain:

1. **Recipient saved preference** — `users.locale` column on `User` entity (`en` / `he`, nullable)
2. **Current request locale** — `LocaleService.getCurrentLocale()` (cookie → session → Accept-Language)
3. **Fallback** — English (`SupportedLocale.DEFAULT`)

### User Locale Persistence

- Added `User.locale` column (nullable `VARCHAR(5)`)
- `LocaleService.setLocale()` persists locale to DB for authenticated users
- `LocaleService.persistLocaleForAuthenticatedUser()` called after successful OTP verification
- Login/OTP flows pass explicit `localeService.getCurrentLocale()` so pre-login Hebrew selection is honored immediately

### Email Services Refactored

| Service | Change |
|---------|--------|
| `EmailService` | Injects `TranslationService`, `EmailTemplateService`, `EmailLocaleResolver`; all OTP and activity-assignment content resolved via translation keys |
| `ReportEmailService` | Same i18n stack for monthly report subject and plain-text body |

### Template Structure

Single template per email type (no per-locale duplicate files). Locale-specific `dir`/`lang` injected at render time; text from translation keys.

| Template | Path | Format |
|----------|------|--------|
| OTP | `src/main/resources/i18n/email/otp.html` | HTML with `{{dir}}`, `{{lang}}`, content placeholders |
| Activity assigned | `src/main/resources/i18n/email/activity-assigned.html` | HTML with logical CSS (`border-inline-start`, `text-align:start`) |
| Monthly report | `src/main/resources/i18n/email/monthly-report.txt` | Plain text with placeholders |

### RTL Support

Hebrew emails receive:

- `<html lang="he" dir="rtl">` (OTP template)
- `dir="rtl"` on activity-assigned wrapper
- `text-align:start` and logical CSS properties for correct mirroring in email clients

English emails receive `lang="en" dir="ltr"`.

### Callers Updated

| Caller | Change |
|--------|--------|
| `LoginView` | `sendOtp(email, otp, localeService.getCurrentLocale())` |
| `OtpVerificationView` | Same for resend; persists locale to user after verification |
| `ActivityService` | Passes `emailLocaleResolver.resolveForUser(assignee)` |
| `MonthlyReportJob` | Uses resolver via `ReportEmailService` (looks up accountant by email) |

---

## Translation Keys Added

All keys added to `messages.properties`, `messages_en.properties`, and `messages_he.properties`:

### OTP (`email.otp.*`)

| Key | English | Hebrew |
|-----|---------|--------|
| `email.otp.subject` | Your CRM login code | קוד ההתחברות שלך ל-CRM |
| `email.otp.heading` | CRM Login Verification | אימות התחברות ל-CRM |
| `email.otp.greeting` | Hello, | שלום, |
| `email.otp.instructions` | Use the code below… expires in {0} minutes. | השתמש בקוד שלהלן… תוקף הקוד הוא {0} דקות. |
| `email.otp.disclaimer` | If you didn't request this code… | אם לא ביקשת קוד זה… |
| `email.otp.footer` | This is an automated message… Do not reply. | זוהי הודעה אוטומטית… אין להשיב. |

### Activity Assignment (`email.activityAssigned.*`)

| Key | English | Hebrew |
|-----|---------|--------|
| `email.activityAssigned.subject` | CRM: Activity assigned to you — {0} | CRM: הוקצתה לך פעילות — {0} |
| `email.activityAssigned.heading` | New Activity Assigned | פעילות חדשה הוקצתה לך |
| `email.activityAssigned.body` | You have been assigned a new activity: | הוקצתה לך פעילות חדשה: |
| `email.activityAssigned.cta` | Log in to the CRM to view details. | התחבר ל-CRM כדי לצפות בפרטים. |

### Monthly Report (`email.monthlyReport.*`)

| Key | English | Hebrew |
|-----|---------|--------|
| `email.monthlyReport.subject` | Monthly Attendance Report — {0} | דוח נוכחות חודשי — {0} |
| `email.monthlyReport.greeting` | Hi, | שלום, |
| `email.monthlyReport.body` | Please find attached the attendance report for {0}. | מצורף דוח הנוכחות עבור {0}. |
| `email.monthlyReport.notice` | This report was generated automatically… | דוח זה נוצר אוטומטית… |
| `email.monthlyReport.regards` | Regards, | בברכה, |
| `email.monthlyReport.signature` | CRM Attendance System | מערכת נוכחות CRM |

---

## Validation

Unit tests verify rendering for English and Hebrew:

| Test Class | Coverage |
|------------|----------|
| `EmailTemplateServiceTest` | OTP HTML: `lang`/`dir`, Hebrew text, no `{{` placeholders left; activity-assigned RTL; monthly report en/he |
| `EmailLocaleResolverTest` | Priority chain: DB preference → current locale → English default |
| `EmailServiceTest` | Sends with resolved/explicit locale; error handling preserved |
| `ActivityServiceTest` | Passes assignee locale to email service |

**Verified per email type:**

| Check | OTP | Activity | Monthly Report |
|-------|-----|----------|----------------|
| Correct language (en/he) | ✅ | ✅ | ✅ |
| Correct subject | ✅ | ✅ | ✅ |
| RTL/LTR (`dir`/`lang`) | ✅ | ✅ | N/A (plain text) |
| No untranslated keys | ✅ | ✅ | ✅ |
| No hardcoded English strings | ✅ | ✅ | ✅ |

---

## Remaining Localization Risks

| Risk | Severity | Notes |
|------|----------|-------|
| **In-app alerts still English** | Medium | 15+ notification paths (`NotificationService`, `ScheduledTaskService`, `SubscriptionHandlerService`) use hardcoded English; not email but visible to users |
| **EMAIL activity to contacts** | Low | Subject/body are user-entered; no system wrapper to localize |
| **Accountant email may not match a User** | Low | Monthly report resolves locale by `app.reporting.accountant-email`; if no matching user, falls back to English |
| **No `app.reporting.locale` config** | Low | Could add explicit locale override for external accountant addresses |
| **Alert email channel unimplemented** | Info | `Topic.sendMailEnabled` and `NotificationChannel.EMAIL` exist but send no mail |
| **Password reset / welcome / invitation** | Info | Not implemented; will need localization when added |
| **Async email locale context** | Low | `@Async` methods capture locale at call time via explicit parameter; callers must continue passing locale where session context may be lost |
| **User.locale not editable in UsersView** | Low | DB field exists; admin UI for per-user locale not yet added (language switcher persists for authenticated users) |
| **Email client RTL rendering** | Low | Uses logical CSS and `dir="rtl"`; some legacy clients may render imperfectly |

---

## Confirmation

All **system-generated outgoing SMTP emails** (OTP login verification, activity assignment notification, monthly attendance report) now respect recipient language preferences through the shared i18n infrastructure (`TranslationService` + `messages*.properties` + `SupportedLocale`).

The reported bug — Hebrew UI users receiving English OTP emails — is fixed by:

1. Passing the active UI locale from `LoginView` / `OtpVerificationView` to `sendOtp()`
2. Resolving all email content through `email.*` translation keys
3. Rendering HTML with `lang="he" dir="rtl"` for Hebrew recipients
4. Persisting locale to `users.locale` after login for future async emails

No second translation system was introduced. Existing email functionality (async sending, error swallowing, SMTP config) is preserved.
