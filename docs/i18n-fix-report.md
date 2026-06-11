# i18n Fix Report

**Date:** 2026-06-11  
**Scope:** Add missing translation keys identified in `docs/i18n-bug-report.md`

---

## Summary

Added **85 translation keys** (81 static UI keys + 4 `enum.AlertState.*` keys) to all three property bundles. A full-project scan of `i18n.translate("...")` literals confirms every static key now resolves in `messages.properties`, `messages_en.properties`, and `messages_he.properties`.

---

## Files Modified

| File | Change |
|------|--------|
| `src/main/resources/i18n/messages.properties` | Added 85 keys (English default) |
| `src/main/resources/i18n/messages_en.properties` | Added 85 keys (English mirror) |
| `src/main/resources/i18n/messages_he.properties` | Added 85 keys (Hebrew translations) |

---

## Keys Added

### Auth (18 keys)

| Key | English |
|-----|---------|
| `auth.username` | Username |
| `auth.password` | Password |
| `auth.login` | Log In |
| `auth.usernamePasswordRequired` | Username and password are required |
| `auth.incorrectCredentials` | Incorrect username or password |
| `auth.loginFailed` | Login failed. Please try again. |
| `auth.noEmail` | No email address on file for this account |
| `auth.verifyIdentity` | Verify Your Identity |
| `auth.otpSent` | A verification code was sent to {0} |
| `auth.verificationCode` | Verification Code |
| `auth.rememberDevice` | Remember this device for {0} days |
| `auth.verify` | Verify |
| `auth.resendCode` | Resend Code |
| `auth.backToLogin` | Back to Login |
| `auth.codeRequired` | Verification code is required |
| `auth.invalidCode` | Invalid verification code |
| `auth.newCodeSent` | A new verification code has been sent |
| `auth.yourEmail` | your email |

### Page titles (3 keys)

| Key | English |
|-----|---------|
| `page.login` | Login \| CRM |
| `page.verifyOtp` | Verify Identity \| CRM |
| `page.dashboard` | Dashboard \| CRM |

### Header (2 keys)

| Key | English |
|-----|---------|
| `header.loggedInAs` | Logged in as {0} |
| `header.logout` | Log Out |

### Language switcher (3 keys)

| Key | English |
|-----|---------|
| `language.label` | Language |
| `language.en` | English |
| `language.he` | Hebrew |

### Navigation (27 keys)

| Key | English |
|-----|---------|
| `nav.dashboard` | Dashboard |
| `nav.contacts` | Contacts |
| `nav.accounts` | Accounts |
| `nav.accountGroups` | Account Groups |
| `nav.contactsItem` | Contacts |
| `nav.addresses` | Addresses |
| `nav.support` | Support |
| `nav.activities` | Activities |
| `nav.calendar` | Calendar |
| `nav.sales` | Sales |
| `nav.leads` | Leads |
| `nav.opportunities` | Opportunities |
| `nav.quotes` | Quotes |
| `nav.salesOrders` | Sales Orders |
| `nav.contracts` | Contracts |
| `nav.forecast` | Forecast |
| `nav.products` | Products |
| `nav.settings` | Settings |
| `nav.workspaces` | Workspaces |
| `nav.savedSearches` | Saved Searches |
| `nav.subscriptions` | Subscriptions |
| `nav.users` | Users |
| `nav.taskQueue` | Task Queue |
| `nav.hr` | HR |
| `nav.timeClock` | Time Clock |
| `nav.attendanceCalendar` | Attendance Calendar |
| `nav.corrections` | Corrections |

### Dialogs / notifications (13 keys)

| Key | English |
|-----|---------|
| `dialog.alerts` | Alerts |
| `dialog.send` | Send |
| `dialog.markAllRead` | Mark All Read |
| `dialog.noAlerts` | No alerts |
| `dialog.alertDetail` | {0} — {1} |
| `dialog.read` | Read |
| `dialog.accept` | Accept |
| `dialog.sendNotification` | Send Notification |
| `dialog.to` | To |
| `dialog.message` | Message |
| `dialog.selectRecipient` | Please select a recipient |
| `dialog.messageRequired` | Message is required |
| `notification.sent` | Notification sent |

### Dashboard (14 keys)

| Key | English |
|-----|---------|
| `view.dashboard.title` | Dashboard |
| `view.dashboard.pipelineCurrency` | {0} ILS |
| `view.dashboard.accounts` | Accounts |
| `view.dashboard.contacts` | Contacts |
| `view.dashboard.openActivities` | Open Activities |
| `view.dashboard.newLeads` | New Leads |
| `view.dashboard.pipelineValue` | Pipeline Value |
| `view.dashboard.contractsExpiring` | Contracts Expiring |
| `view.dashboard.winRate` | Win Rate |
| `view.dashboard.salesOverview` | Sales Overview |
| `view.dashboard.pipelineByStage` | Pipeline by Stage |
| `view.dashboard.leadFunnel` | Lead Funnel |
| `view.dashboard.activityBreakdown` | Activity Breakdown |
| `view.dashboard.contractHealth` | Contract Health |

### Enums — AlertState (4 keys)

| Key | English |
|-----|---------|
| `enum.AlertState.NEW` | New |
| `enum.AlertState.READ` | Read |
| `enum.AlertState.ACCEPTED` | Accepted |
| `enum.AlertState.EXPIRED` | Expired |

---

## Validation

**Method:** Scanned all `i18n.translate("...")` string literals under `src/main/java/**/*.java` and cross-referenced against keys in each bundle.

| Metric | Result |
|--------|--------|
| Static keys referenced in Java | 535 |
| Missing from `messages.properties` | 0 |
| Missing from `messages_en.properties` | 0 |
| Missing from `messages_he.properties` | 0 |

**Views verified (per requirements):**

- `MainLayout.java` — all `header.*`, `nav.*`, `dialog.*`, and `notification.sent` keys present
- `LoginView.java` — all `auth.*` and `page.login` keys present
- `OtpVerificationView.java` — all `auth.*` and `page.verifyOtp` keys present
- `DashboardView.java` — all `view.dashboard.*` and `page.dashboard` keys present
- `LanguageSwitcher.java` — all `language.*` keys present
- `MainLayout.java` alert badges — all `enum.AlertState.*` keys present

---

## Remaining Missing Keys

**None** for static `i18n.translate("...")` literals or the four `enum.AlertState.*` values added in this fix.

**Known dynamic-key caveat (out of scope, documented in bug report):**

- `UsersView.java` builds keys as `"common.role." + roleKey`. Only `common.role.USER`, `ADMIN`, `SALES`, and `SUPPORT` are defined. Any other Spring Security role (e.g. `ROLE_MANAGER`) would still display the raw key at runtime. This was not part of the 81-key fix list and was intentionally left unchanged.

---

## Confirmation

All 85 keys from the bug report are present in all three bundles with Hebrew translations in `messages_he.properties`. No untranslated static keys remain for the audited `i18n.translate(...)` calls.
