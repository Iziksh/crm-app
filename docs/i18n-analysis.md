# Multi-Language Support Analysis (English + Hebrew RTL)

Phase 1 analysis for adding full language switching between **English (LTR)** and **Hebrew (RTL)** to the CRM application.

**Status:** Analysis only — no implementation changes have been made.

---

## 1. Current Localization State

### Technology stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.3.5, JPA, REST API (`/api/v1/**`) with JWT |
| Frontend | Vaadin 24 server-rendered Java views (`com.crm.ui.*`) |
| Routing | `@Route` / `@RouteAlias` per view; shared shell via `MainLayout` (`AppLayout` + `SideNav`) |
| State | Vaadin session + Spring Security (`SecurityService`); no global locale/language state |
| User model | `User` entity has no `locale` / `language` field |

### i18n infrastructure: none

| Check | Result |
|-------|--------|
| `messages*.properties` | Not present |
| Spring `MessageSource` | Not configured |
| Vaadin `I18NProvider` | Not implemented |
| `spring.web.locale` / locale resolver | Not configured |
| `index.html` `lang` / `dir` attributes | Not set |

### How text is handled today

#### CRM module (~22 views) — English, hardcoded

All labels, headers, dialogs, notifications, and `@PageTitle` values are inline English strings. Example pattern in views such as `AccountsView`:

- `@PageTitle("Accounts | CRM")`
- Grid headers: `"Name"`, `"Industry"`, `"Type"`, etc.
- Buttons, dialogs, and `Notification.show(...)` messages in English

#### HR / Time-tracking module — Hebrew, hardcoded + forced RTL

Partial Hebrew localization exists only in attendance-related views:

| File | Language | RTL |
|------|----------|-----|
| `TimeClockView.java` | Hebrew UI strings, Hebrew date formatter | Inline `direction: rtl` on cards |
| `AttendanceCalendarView.java` | Hebrew labels | `dir="rtl"` on root + CSS |
| `AttendanceReportEditor.java` | Hebrew dialog | `dir="rtl"` |
| `attendance-calendar.css` | — | `direction: rtl` throughout |
| `AttendanceReportType.java` | Embedded `hebrewLabel` per enum | — |
| `AttendanceReportService.java` | Hebrew validation errors | — |

Related HR views remain **English LTR**:

- `AttendanceCorrectionView.java`
- `MainLayout` nav labels: `"Time Clock"`, `"Attendance Calendar"`, `"Corrections"`

#### Enums (42 files) — raw English `enum.name()`

Displayed directly in grids and badges (e.g. `LeadStatus.NEW` → `"NEW"`), with no translation layer.

#### Backend / API user-facing text — English (with Hebrew exceptions)

- `GlobalExceptionHandler`: `"Not Found"`, `"Validation failed"`, `"Invalid username or password"`, etc.
- `ResourceNotFoundException`: `"%s not found with %s : '%s'"`
- Service exceptions: mixed English (`AttendanceService`) and Hebrew (`AttendanceReportService`)
- Jakarta validation DTOs: `@NotBlank` without custom `message=` → default English messages
- `EmailService`: English HTML templates (`lang="en"`)
- `ReportEmailService`: English plain-text email

#### User/business data — locale-neutral

CRM entity fields (account names, activity titles, notes) are stored as user-entered content and are not part of the UI i18n layer.

---

## 2. Files / Components That Will Be Affected

### Tier 1 — Core i18n infrastructure (new)

| Item | Purpose |
|------|---------|
| `I18nProvider` (Vaadin `I18NProvider` bean) | Central translation lookup for Vaadin UI |
| `LocaleService` / `TranslationService` | Read/write active locale (session + optional user preference) |
| `messages_en.properties` | English UI strings |
| `messages_he.properties` | Hebrew UI strings |
| `I18nConfig` (Spring `MessageSource`) | Backend message resolution |
| Language switcher in `MainLayout` | User-facing toggle EN ↔ HE |

### Tier 2 — Layout and global direction

| File | Changes |
|------|---------|
| `MainLayout.java` | Translate nav/header/dialogs; set `document.documentElement.dir` + `lang`; language switcher |
| `index.html` | Default `lang` attribute (optional; Vaadin can set at runtime) |
| New global CSS (or Lumo theme) | Logical properties (`margin-inline-*`, `padding-inline-*`) for RTL-safe layout |

### Tier 3 — All Vaadin views (~28 files)

Every file under `src/main/java/com/crm/ui/`:

**CRM (English today):**

- `LoginView.java`
- `OtpVerificationView.java`
- `DashboardView.java`
- `AccountsView.java`
- `ContactsView.java`
- `AccountGroupsView.java`
- `AddressesView.java`
- `ActivitiesView.java`
- `CalendarView.java`
- `LeadsView.java`
- `OpportunitiesView.java`
- `QuotesView.java`
- `SalesOrdersView.java`
- `ContractsView.java`
- `ForecastView.java`
- `ProductsView.java`
- `WorkspacesView.java`
- `SavedSearchesView.java`
- `SubscriptionsView.java`
- `UsersView.java`
- `ScheduledTasksView.java`
- `AttachmentPanel.java`

**HR (Hebrew today — refactor to translation keys):**

- `TimeClockView.java`
- `attendance/AttendanceCalendarView.java`
- `attendance/AttendanceReportEditor.java`
- `AttendanceCorrectionView.java`

### Tier 4 — Enums and display labels

All 42 enums in `domain/enums/` and `timetracking/enums/`, especially:

- `AttendanceReportType` (replace embedded `hebrewLabel` with i18n keys)
- Status/stage enums shown in grids: `LeadStatus`, `OpportunityStage`, `ActivityStatus`, `QuoteStatus`, `SalesOrderStatus`, `ContractStatus`, `ContactStatus`, `AlertState`, etc.

### Tier 5 — Backend messages

| File | Changes |
|------|---------|
| `GlobalExceptionHandler.java` | Resolve messages by locale |
| `ResourceNotFoundException.java`, `BadRequestException.java`, etc. | Message keys instead of raw strings |
| `AttendanceReportService.java`, `AttendanceService.java` | Unify to message keys |
| `EmailService.java`, `ReportEmailService.java` | Locale-aware templates |
| `dto/request/*` | Custom validation `message` keys |
| `timetracking/controller/*` | `AccessDeniedException` messages |

### Tier 6 — Data model (optional but recommended)

| File | Changes |
|------|---------|
| `User.java` | Add `locale` field (`en` / `he`) |
| `UserRequest` / `UserResponse` | Expose locale in admin UI |
| DB migration | `users.locale` column |

### Tier 7 — CSS

| File | Changes |
|------|---------|
| `attendance-calendar.css` | Replace hardcoded `direction: rtl` with locale-driven class (e.g. `[dir="rtl"] .attendance-calendar-page`) |

---

## 3. Recommended i18n Approach

### Translation storage — Spring `MessageSource` + property files

```
src/main/resources/
  i18n/
    messages.properties          # default (en)
    messages_he.properties
    messages_en.properties       # explicit en (optional)
```

**Key naming convention:**

```
nav.dashboard=Dashboard
nav.contacts=Contacts
view.accounts.title=Accounts
view.accounts.column.name=Name
enum.leadStatus.NEW=New
enum.leadStatus.CONTACTED=Contacted
error.validation.required=This field is required
error.attendance.overlap=Overlapping presence report on same day
```

### Vaadin integration — `I18NProvider`

Implement Vaadin's `I18NProvider` backed by Spring `MessageSource`:

```java
@Component
public class CrmI18nProvider implements I18NProvider {
    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(Locale.ENGLISH, Locale.forLanguageTag("he"));
    }
    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        return messageSource.getMessage(key, params, key, locale);
    }
}
```

Views call `getTranslation("nav.dashboard")` (via injected service or helper) instead of hardcoded strings.

### Locale resolution and persistence

**Priority order:**

1. User DB preference (if logged in)
2. Session/cookie (VaadinSession or HTTP cookie)
3. Browser `Accept-Language`
4. Default: `en`

**On locale change:**

- Set `UI.getCurrent().setLocale(locale)`
- Set `dir` and `lang` on `document.documentElement`
- Refresh or re-navigate views so labels update

Vaadin 24 Lumo supports RTL when `dir="rtl"` is set on `<html>`.

### Enum localization pattern

Replace `.name()` display with a small helper:

```java
public final class EnumLabels {
    public static String of(Enum<?> e, Locale locale) {
        return msg.get("enum." + e.getClass().getSimpleName() + "." + e.name(), locale);
    }
}
```

Refactor `AttendanceReportType` from `hebrewLabel` field to standard enum key lookup.

### Backend API localization (Phase 2 consideration)

For REST clients:

- Add `Accept-Language` header support via Spring `LocaleResolver`
- Return message keys and resolved text in `ErrorResponse`
- Keep CRM data fields unchanged; only system messages are translated

### Email templates

Store HTML bodies as message keys or separate template files per locale (`email/otp_en.html`, `email/otp_he.html`), selected by recipient's `User.locale`.

### Suggested implementation order

1. Infrastructure: `MessageSource`, `I18NProvider`, `LocaleService`, language switcher in `MainLayout`
2. Global RTL: `dir`/`lang` on document + CSS logical properties
3. `MainLayout` + auth views (`LoginView`, `OtpVerificationView`)
4. CRM views (batch by nav section: Contacts → Sales → Settings)
5. HR module: migrate Hebrew hardcoded strings to keys; remove duplicate RTL
6. Enums + backend errors + validation messages
7. User locale persistence + email templates

---

## 4. RTL / LTR Considerations

### Current RTL handling (inconsistent)

- **Page-level RTL** only on attendance calendar/editor (`dir="rtl"`)
- **Component-level RTL** via inline styles in `TimeClockView`
- **CSS-level RTL** in `attendance-calendar.css` (`text-align: right`, `justify-content: flex-end`)
- **No app-wide RTL** — `MainLayout`, drawer, header, and all CRM views default to LTR
- **Physical CSS properties** in a few places (`margin-right` in `MainLayout`, `ScheduledTasksView`) — will not flip automatically in RTL
- **Date formatting** hardcoded: Hebrew in `TimeClockView`, English in `CalendarView`

### Recommended RTL strategy

| Layer | Approach |
|-------|----------|
| **Global** | Set `dir` and `lang` on `<html>` from `MainLayout` based on active locale |
| **Per-view hacks** | Remove inline `direction: rtl` from `TimeClockView`; rely on global `dir` |
| **CSS** | Use logical properties (`margin-inline-end` instead of `margin-right`); scope attendance CSS under `[dir="rtl"]` only if needed |
| **Icons** | Mirror directional icons (`ANGLE_LEFT`/`RIGHT`, `ARROW_FORWARD`) via Vaadin RTL icon mirroring or swap icon by locale |
| **Grids** | Vaadin Grid generally respects `dir`; verify column editor alignment |
| **Dates/numbers** | `DateTimeFormatter` with active `Locale` (not hardcoded `he` or `ENGLISH`) |

### Areas where direction matters

- **Navigation drawer** (`SideNav` in `MainLayout`) — item order and icon alignment
- **Header bar** — logout button, bell badge position (`margin-right` today)
- **Grids** — column order, sort indicators, inline editors
- **Dialogs and forms** — label alignment, required-field indicators
- **Attendance calendar** — day-of-week order (currently RTL-specific DOM order for prev/next buttons)
- **Confirm dialogs** — button order (primary/cancel)
- **Email HTML** — `dir` and `lang` on `<html>` for Hebrew templates

### Hebrew-specific notes

- `AttendanceReportType` already has Hebrew labels embedded; these should move to `messages_he.properties` with English equivalents in `messages_en.properties`
- `AttendanceReportService` validation messages are already in Hebrew and should become keyed messages usable in both locales
- Hebrew month/day formatting in `TimeClockView` and `AttendanceCalendarView` should use the active locale rather than a hardcoded `Locale.forLanguageTag("he")`

---

## Summary

| Aspect | Current state |
|--------|---------------|
| **Overall** | No real i18n; English CRM + Hebrew-only HR module with ad-hoc RTL |
| **Primary work surface** | ~28 Vaadin Java views + `MainLayout` |
| **Biggest inconsistency** | Nav in English, Time Clock content in Hebrew, no user-controlled switch |
| **Recommended approach** | Spring `MessageSource` + Vaadin `I18NProvider` + global `dir`/`lang` + enum/error key catalog |
| **Data model gap** | No `User.locale` for persistence |

---

*Generated from Phase 1 codebase analysis. Implementation has not started.*
