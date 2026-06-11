# i18n Bug Report — Translation Keys Displayed Instead of Text

**Date:** 2026-06-10  
**Status:** Audit only (no fixes applied)  
**Symptom:** UI shows raw keys such as `nav.dashboard`, `auth.username`, `view.dashboard.title` instead of human-readable strings.

---

## Executive Summary

The primary cause is **missing property entries**: **81 translation keys** referenced in Java code do not exist in any bundle file. When Spring `MessageSource` cannot resolve a key, `TranslationService` returns the key itself as the default message — so the key is rendered verbatim in the UI.

This is **not** a broken i18n architecture. The wiring (`MessageSource`, `TranslationService`, `CrmI18nProvider`, `LocaleService`) is correct. The failure is **incomplete migration data**: several views were updated to call `i18n.translate(...)` but their keys were never added to the property files.

Secondary issues: **missing enum keys** for `AlertState`, **inconsistent `page.*` vs `pageTitle.*` namespaces**, **`LocaleCookieFilter` scope inversion**, and **no JSON translation files** (project uses `.properties` only).

---

## Root Cause Mechanism

```23:25:src/main/java/com/crm/service/TranslationService.java
    public String translate(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
```

The third argument to `getMessage` is the **default message**. When the key is absent from all bundles, Spring returns that default — the key string itself.

`CrmI18nProvider` uses the same pattern:

```26:28:src/main/java/com/crm/config/CrmI18nProvider.java
    public String getTranslation(String key, Locale locale, Object... params) {
        return messageSource.getMessage(key, params, key, locale);
    }
```

`TranslationServiceTest` explicitly expects missing enum keys to echo back (`enum.TestEnum.NEW`), confirming this is intentional fallback behavior, not a lookup bug.

---

## Translation Files — Present vs Expected

| File | Path | Status |
|------|------|--------|
| Default (English) | `src/main/resources/i18n/messages.properties` | Present — **665 keys**, **81 referenced keys missing** |
| English mirror | `src/main/resources/i18n/messages_en.properties` | Present — mirrors default |
| Hebrew | `src/main/resources/i18n/messages_he.properties` | Present — **same 81 keys missing** |
| `en.json` | — | **Does not exist** (not part of current stack) |
| `he.json` | — | **Does not exist** (not part of current stack) |

**Bundle configuration is correct:**

```16:21:src/main/java/com/crm/config/I18nConfig.java
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
```

Classpath basename `i18n/messages` correctly resolves to `src/main/resources/i18n/messages*.properties`.

---

## Missing Translation Keys (81)

Keys used in Java via `i18n.translate("...")` but **absent** from `messages.properties`, `messages_en.properties`, and `messages_he.properties`.

### Auth — `LoginView.java`, `OtpVerificationView.java` (18 keys)

| Key | File | Approx. line |
|-----|------|--------------|
| `auth.username` | `src/main/java/com/crm/ui/LoginView.java` | 97 |
| `auth.password` | `src/main/java/com/crm/ui/LoginView.java` | 101 |
| `auth.login` | `src/main/java/com/crm/ui/LoginView.java` | 104 |
| `auth.usernamePasswordRequired` | `src/main/java/com/crm/ui/LoginView.java` | 124 |
| `auth.incorrectCredentials` | `src/main/java/com/crm/ui/LoginView.java` | ~133 |
| `auth.loginFailed` | `src/main/java/com/crm/ui/LoginView.java` | ~137 |
| `auth.noEmail` | `src/main/java/com/crm/ui/LoginView.java` | ~145 |
| `page.login` | `src/main/java/com/crm/ui/LoginView.java` | 118 (`getPageTitle`) |
| `auth.verifyIdentity` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 107 |
| `auth.otpSent` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 117 |
| `auth.verificationCode` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 123 |
| `auth.rememberDevice` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 130 |
| `auth.verify` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 132 |
| `auth.resendCode` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 140 |
| `auth.backToLogin` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 143 |
| `auth.codeRequired` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 158 |
| `auth.invalidCode` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 163 |
| `auth.newCodeSent` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 194 |
| `auth.yourEmail` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 219 |
| `page.verifyOtp` | `src/main/java/com/crm/ui/OtpVerificationView.java` | 76 (`getPageTitle`) |

### Shell / navigation — `MainLayout.java` (38 keys)

| Key | Usage |
|-----|-------|
| `header.loggedInAs` | User span in navbar (~165) |
| `header.logout` | Logout button (~196) |
| `dialog.alerts` | Alerts dialog title (~227) |
| `dialog.send` | Send button in alerts dialog (~237) |
| `dialog.markAllRead` | Mark-all button (~249) |
| `dialog.noAlerts` | Empty alerts list (~275) |
| `dialog.alertDetail` | Alert row detail (~323) |
| `dialog.read` | Read action button (~337) |
| `dialog.accept` | Accept action button (~353) |
| `dialog.sendNotification` | Send-notification dialog title (~405) |
| `dialog.to` | Recipient combo label (~415) |
| `dialog.message` | Message field label (~425) |
| `dialog.selectRecipient` | Validation error (~447) |
| `dialog.messageRequired` | Validation error (~457) |
| `notification.sent` | Success toast (~467) |
| `nav.dashboard` | Side nav (~489) |
| `nav.contacts` | Side nav section (~493) |
| `nav.accounts` | Side nav item (~497) |
| `nav.accountGroups` | Side nav item (~499) |
| `nav.contactsItem` | Side nav item (~501) |
| `nav.addresses` | Side nav item (~503) |
| `nav.support` | Side nav section (~509) |
| `nav.activities` | Side nav item (~513) |
| `nav.calendar` | Side nav item (~515) |
| `nav.sales` | Side nav section (~521) |
| `nav.leads` | Side nav item (~525) |
| `nav.opportunities` | Side nav item (~527) |
| `nav.quotes` | Side nav item (~529) |
| `nav.salesOrders` | Side nav item (~531) |
| `nav.contracts` | Side nav item (~533) |
| `nav.forecast` | Side nav item (~535) |
| `nav.products` | Side nav item (~537) |
| `nav.settings` | Side nav section (~543) |
| `nav.workspaces` | Side nav item (~547) |
| `nav.savedSearches` | Side nav item (~549) |
| `nav.subscriptions` | Side nav item (~551) |
| `nav.users` | Side nav item (~555) |
| `nav.taskQueue` | Side nav item (~557) |
| `nav.hr` | Side nav section (~563) |
| `nav.timeClock` | Side nav item (~567) |
| `nav.attendanceCalendar` | Side nav item (~569) |
| `nav.corrections` | Side nav item (~573) |

**File:** `src/main/java/com/crm/ui/MainLayout.java`

### Language switcher — `LanguageSwitcher.java` (3 keys)

| Key | Line |
|-----|------|
| `language.label` | 13–15 |
| `language.en` | 19 |
| `language.he` | 20 |

**File:** `src/main/java/com/crm/ui/LanguageSwitcher.java`

### Dashboard — `DashboardView.java` (15 keys)

| Key | Line |
|-----|------|
| `page.dashboard` | 159 (`getPageTitle`) |
| `view.dashboard.title` | 52 |
| `view.dashboard.pipelineCurrency` | 60 |
| `view.dashboard.accounts` | 75 |
| `view.dashboard.contacts` | 76 |
| `view.dashboard.openActivities` | 77 |
| `view.dashboard.newLeads` | 78 |
| `view.dashboard.pipelineValue` | 79 |
| `view.dashboard.contractsExpiring` | 80 |
| `view.dashboard.winRate` | 81 |
| `view.dashboard.salesOverview` | 86 |
| `view.dashboard.pipelineByStage` | 98 |
| `view.dashboard.leadFunnel` | 115 |
| `view.dashboard.activityBreakdown` | 133 |
| `view.dashboard.contractHealth` | 147 |

**File:** `src/main/java/com/crm/ui/DashboardView.java`

---

## Additional Missing Keys (enum — not in the 81 count)

These are built dynamically by `translateEnum()` and are also missing:

| Key | Built from | Used in |
|-----|------------|---------|
| `enum.AlertState.NEW` | `AlertState.NEW` | `MainLayout.java` ~293 |
| `enum.AlertState.READ` | `AlertState.READ` | `MainLayout.java` ~293 |
| `enum.AlertState.ACCEPTED` | `AlertState.ACCEPTED` | `MainLayout.java` ~293 |
| `enum.AlertState.EXPIRED` | `AlertState.EXPIRED` | `MainLayout.java` ~293 |

**Enum definition:** `src/main/java/com/crm/domain/enums/AlertState.java`

No `enum.AlertState.*` entries exist anywhere in `messages*.properties` (grep confirms zero matches).

---

## Namespace Mismatches (`page.*` vs `pageTitle.*`)

Views use **two different conventions** for browser tab titles. Both work **only if the key exists** in properties.

| Convention | Views using it | Keys in properties? |
|------------|----------------|---------------------|
| `pageTitle.*` | `AccountsView`, `ContactsView`, `AccountGroupsView`, `AddressesView`, `ActivitiesView`, `CalendarView` | **Yes** — lines 25–30 of `messages.properties` |
| `page.*` | `LeadsView`, `OpportunitiesView`, `QuotesView`, `SalesOrdersView`, `ContractsView`, `ForecastView`, `ProductsView`, `WorkspacesView`, `SavedSearchesView`, `SubscriptionsView`, `UsersView`, `ScheduledTasksView`, `TimeClockView`, `AttendanceCorrectionView`, `AttendanceCalendarView` | **Yes** — lines 257–263, 465–472 |
| `page.*` (missing) | `DashboardView` (`page.dashboard`), `LoginView` (`page.login`), `OtpVerificationView` (`page.verifyOtp`) | **No** |

This is inconsistent naming, not a runtime bug by itself. The three `page.*` entries above are simply **missing from the bundle**.

**Example — working:**

```77:78:src/main/java/com/crm/ui/AccountsView.java
    public String getPageTitle() {
        return i18n.translate("pageTitle.accounts");
```

**Example — broken:**

```83:84:src/main/java/com/crm/ui/LeadsView.java
    public String getPageTitle() {
        return i18n.translate("page.leads");
```
(`page.leads` exists; `page.dashboard` does not.)

---

## Dynamic Key Paths (conditional failures)

### Role badges — `UsersView.java`

```80:81:src/main/java/com/crm/ui/UsersView.java
                    String roleKey = role.replace("ROLE_", "");
                    Span badge = new Span(i18n.translate("common.role." + roleKey));
```

**Defined in properties:** `common.role.USER`, `ADMIN`, `SALES`, `SUPPORT` (lines 434–437).

**Risk:** Any role outside that set (e.g. `ROLE_MANAGER`) resolves to a missing key and displays `common.role.MANAGER` literally.

---

## Initialization / Locale Issues

### Vaadin locale — OK

```19:22:src/main/java/com/crm/config/LocaleUiInitListener.java
        event.getSource().addUIInitListener(uiEvent ->
                localeService.resolveAndApply(uiEvent.getUI()));
```

On each UI init, `LocaleService` reads cookie → session → `Accept-Language`, stores in `VaadinSession`, calls `UI.setLocale()`, and sets `dir`/`lang` on `<html>`. **Locale resolution is not the cause of key display.**

### `LocaleCookieFilter` — inverted scope (secondary)

```23:26:src/main/java/com/crm/config/LocaleCookieFilter.java
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
```

`shouldNotFilter` returns `true` for **non-API** paths, so the filter **only runs on `/api/**`** and **does not run for Vaadin UI requests**. Therefore `LocaleContextHolder` is **not** set during normal page rendering.

**Impact today:** Low for translations, because `TranslationService` reads locale from `VaadinSession` first (`LocaleService.getCurrentLocale()`). **Impact for REST:** Filter works as intended for API.

**Impact if code relied on `LocaleContextHolder` in Vaadin views:** Would fall through to default `en` when session attribute is unset.

### Hebrew bundle — OK when keys exist

`messages_he.properties` exists and mirrors structure. Missing keys in the default bundle are also missing in Hebrew — switching to Hebrew does not fix the 81 gaps.

---

## Components With Highest Visible Impact

Because `MainLayout` wraps every authenticated route, these missing keys appear on **every page**:

1. **Entire side navigation** — all `nav.*` keys (~27 items)
2. **Header** — `header.loggedInAs`, `header.logout`
3. **Language switcher** — `language.label`, `language.en`, `language.he`
4. **Alerts / notifications UI** — all `dialog.*` keys in alert flows

**Login / OTP** (pre-auth): all `auth.*` keys + `page.login` / `page.verifyOtp`.

**Dashboard** (home route): all `view.dashboard.*` keys + `page.dashboard`.

**Sales/CRM entity views** (Leads, Accounts, Contacts, etc.): largely **OK** — keys were added during migration. Users may only notice missing text on shell, auth, and dashboard.

---

## Keys Present in Properties but Using Different Paths in Java (aliases)

These are **not broken** (both paths often exist), but increase maintenance risk:

| Java uses | Properties also has |
|-----------|---------------------|
| `common.exportCsv` | `common.button.exportCsv` (both exist) |
| `common.button.exportCsv` | `common.exportCsv` (both exist) |

No current breakage, but duplicate definitions at lines 16–17 and 213 of `messages.properties`.

---

## What Is NOT Wrong

| Check | Result |
|-------|--------|
| `MessageSource` bean | Configured correctly |
| Bundle path `i18n/messages` | Matches file location |
| UTF-8 encoding | Set on `MessageSource` |
| `TranslationService` injection | Present in all UI views |
| `HasDynamicTitle` migration | Applied across views |
| Enum keys for CRM entities | Present (LeadStatus, OpportunityStage, etc.) |
| HR / attendance keys | Present in properties |
| Vaadin `UI.setLocale()` | Applied on UI init |

---

## Recommended Fix Priority (for future work — not applied here)

1. **P0 — Add 81 missing keys** to `messages.properties`, `messages_en.properties`, `messages_he.properties` (auth, nav, header, language, dashboard, MainLayout dialogs).
2. **P0 — Add `enum.AlertState.*`** (4 values).
3. **P1 — Normalize `page.*` vs `pageTitle.*`** to one convention.
4. **P2 — Fix `LocaleCookieFilter` scope** so Vaadin requests also populate `LocaleContextHolder`, or document that only `VaadinSession` is authoritative.
5. **P2 — Audit dynamic `common.role.*`** for roles beyond the four defined keys.

---

## Audit Method

Automated cross-reference of all `i18n.translate("...")` literals in `src/main/java/com/crm/ui/**/*.java` against keys in `src/main/resources/i18n/messages.properties`.

- **537** distinct static keys referenced in UI code  
- **456** found in properties  
- **81** missing (listed above)  
- **4** additional missing `enum.AlertState.*` keys from `translateEnum()` usage  

---

## Files Referenced

| Role | Path |
|------|------|
| Translation API | `src/main/java/com/crm/service/TranslationService.java` |
| Locale holder | `src/main/java/com/crm/service/LocaleService.java` |
| Spring config | `src/main/java/com/crm/config/I18nConfig.java` |
| Vaadin provider | `src/main/java/com/crm/config/CrmI18nProvider.java` |
| UI init | `src/main/java/com/crm/config/LocaleUiInitListener.java` |
| Cookie filter | `src/main/java/com/crm/config/LocaleCookieFilter.java` |
| Bundles | `src/main/resources/i18n/messages.properties` (+ `_en`, `_he`) |
| Primary broken views | `MainLayout.java`, `LoginView.java`, `OtpVerificationView.java`, `DashboardView.java`, `LanguageSwitcher.java` |
