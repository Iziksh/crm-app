# i18n System Design — English + Hebrew RTL

Architecture and decisions for multi-language support in the CRM application.

**Source of truth:** [`docs/i18n-analysis.md`](i18n-analysis.md)  
**Status:** Design only — no implementation or refactoring.

---

## 1. Selected i18n Library

### Decision: Spring `MessageSource` + Vaadin `I18NProvider`

No third-party i18n library (e.g. ICU4J wrappers, react-intl, i18next) is introduced.

| Layer | Mechanism |
|-------|-----------|
| **Backend** (REST errors, validation, emails, services) | Spring Boot built-in `MessageSource` (`ResourceBundleMessageSource`) |
| **Vaadin UI** (~28 server-rendered Java views) | Vaadin 24 `I18NProvider` interface, implemented as a Spring `@Component` delegating to the same `MessageSource` |

### Justification

1. **Stack fit** — The frontend is Vaadin 24 server-rendered Java (`com.crm.ui.*`), not a separate SPA. Vaadin's native `I18NProvider` is the intended integration point; a client-side library would not cover grid headers, dialogs, or `@PageTitle` values defined in Java.
2. **Single source of truth** — One set of `.properties` files serves both Vaadin views and Spring backend messages, avoiding duplicate translation catalogs.
3. **Zero new dependencies** — Spring `MessageSource` and Vaadin `I18NProvider` are already on the classpath via Spring Boot 3.3.5 and Vaadin 24.5.6.
4. **Enum and error unification** — The analysis identifies 42 enums and mixed English/Hebrew backend exceptions; a shared key namespace (`enum.*`, `error.*`) resolves both through one lookup API.

### Explicitly not selected

| Alternative | Reason rejected |
|-------------|-----------------|
| Client-only i18n (localStorage + JS bundles) | Does not apply to server-rendered Vaadin components |
| Separate backend/frontend translation files | Duplicates maintenance across ~28 views + API layer |
| Hardcoded bilingual maps in Java | Current problem in `AttendanceReportType.hebrewLabel`; does not scale |

---

## 2. Translation Structure

### Folder layout

```
src/main/resources/
  i18n/
    messages.properties           # Default fallback (English)
    messages_en.properties        # Explicit English (optional mirror of default)
    messages_he.properties        # Hebrew
    email/
      otp_en.html                 # OTP email body template (English)
      otp_he.html                 # OTP email body template (Hebrew)
      activity-assigned_en.html
      activity-assigned_he.html
      monthly-report_en.html
      monthly-report_he.html
```

Email templates are separated from `.properties` because the analysis identifies multi-line HTML in `EmailService` and plain-text in `ReportEmailService` — property files are unsuitable for large HTML blocks.

### Supported locales

| Code | Locale | Direction | Status |
|------|--------|-----------|--------|
| `en` | `Locale.ENGLISH` | LTR | Default |
| `he` | `Locale.forLanguageTag("he")` | RTL | Second language |

Only these two locales are registered in `I18NProvider.getProvidedLocales()`. No locale variants (e.g. `en-US`, `he-IL`) in v1.

### Key naming convention

Dot-separated, lowercase segments. Pattern: `{domain}.{context}.{element}`.

| Prefix | Scope | Examples |
|--------|-------|----------|
| `app.` | Global / branding | `app.name=CRM` |
| `nav.` | `MainLayout` drawer and sections | `nav.dashboard`, `nav.contacts`, `nav.hr.timeClock` |
| `header.` | `MainLayout` navbar | `header.loggedInAs`, `header.logout`, `header.alerts` |
| `view.{viewName}.` | Per-view strings | `view.accounts.title`, `view.accounts.column.name` |
| `dialog.` | Shared dialog labels | `dialog.save`, `dialog.cancel`, `dialog.close`, `dialog.delete` |
| `notification.` | Toast messages | `notification.saved`, `notification.deleted` |
| `enum.{EnumSimpleName}.{CONSTANT}` | All 42 enums | `enum.LeadStatus.NEW`, `enum.AttendanceReportType.PRESENCE` |
| `error.` | Exceptions and validation | `error.notFound`, `error.attendance.overlap` |
| `validation.` | Jakarta Bean Validation `message=` keys | `validation.required`, `validation.email` |
| `email.` | Email subject lines (bodies in `email/*.html`) | `email.otp.subject` |
| `pageTitle.` | Browser tab titles | `pageTitle.accounts=Accounts \| CRM` |

### Parameterized messages

Use `{0}`, `{1}` placeholders (Java `MessageFormat` style) for dynamic segments:

```properties
# messages.properties
error.notFound={0} not found with {1} : ''{2}''
header.loggedInAs=Logged in as: {0}
dialog.deleteConfirm=Delete "{0}"?

# messages_he.properties
error.notFound={0} לא נמצא עם {1} : ''{2}''
header.loggedInAs=מחובר כ: {0}
dialog.deleteConfirm=למחוק את "{0}"?
```

### Migration mapping (Hebrew hardcoded → keys)

Strings currently embedded in HR views and services move to keyed entries. Examples from the analysis:

| Current location | Proposed key |
|----------------|--------------|
| `TimeClockView` — `"שעון נוכחות"` | `view.timeClock.title` |
| `AttendanceReportType.hebrewLabel` — `"נוכחות"` | `enum.AttendanceReportType.PRESENCE` |
| `AttendanceReportService` — `"קיימת חפיפה עם דיווח נוכחות אחר באותו יום"` | `error.attendance.overlap` |

English equivalents are added to `messages.properties` / `messages_en.properties` for every Hebrew string migrated.

### Key ownership by tier (from analysis)

| Tier | Key prefixes used |
|------|---------------------|
| Tier 1 — Infrastructure | (none — infrastructure reads keys, does not define them) |
| Tier 2 — `MainLayout` | `nav.*`, `header.*`, `dialog.*` |
| Tier 3 — Views (~28) | `view.{viewName}.*`, `pageTitle.*`, `notification.*` |
| Tier 4 — Enums (42) | `enum.*` |
| Tier 5 — Backend | `error.*`, `validation.*`, `email.*` |
| Tier 7 — CSS | No translation keys; direction handled separately (Section 5) |

---

## 3. Language State Management

### Runtime locale holder: `LocaleService`

A single Spring `@Service` owns the active locale for the current request/UI session.

**Responsibilities:**

- Resolve locale on each request using the priority chain (Section 4)
- Expose `getCurrentLocale()` for views, services, and exception handlers
- Expose `setLocale(Locale)` to apply a user-initiated switch
- Notify listeners when locale changes (for re-rendering)
- Delegate lookups to `MessageSource` via `getMessage(String key, Object... args)`

### Resolution priority chain

Applied on every Vaadin UI attach and every REST API request:

```
1. Authenticated user's DB preference   (User.locale — once Tier 6 is implemented)
2. HTTP cookie                          (CRM_LOCALE — survives browser restart, works pre-login)
3. VaadinSession attribute              (runtime cache, synced from cookie/DB on attach)
4. Accept-Language request header       (browser default, first matching en or he)
5. Fallback                             (en)
```

### Vaadin UI lifecycle integration

| Event | Action |
|-------|--------|
| `MainLayout.onAttach` | Resolve locale via priority chain; call `UI.setLocale(locale)`; apply `dir`/`lang` on `<html>` |
| `LoginView.onAttach` | Same resolution (no DB preference yet — cookie/header/fallback apply) |
| Language switcher click | `LocaleService.setLocale(newLocale)` → persist → re-apply direction → refresh UI |
| Post-login | If `User.locale` is set, override cookie/session to DB value |

### REST API lifecycle integration

| Component | Action |
|-----------|--------|
| `AcceptHeaderLocaleResolver` (or custom resolver) | Reads `Accept-Language` for `/api/**` requests |
| `LocaleContextHolder` | Holds locale for `GlobalExceptionHandler` and validation message resolution |
| Authenticated API calls | Optionally override with `User.locale` from security principal |

### Language switcher placement

Located in `MainLayout` header (Tier 2 from analysis), between the user span and the logout button.

- Control type: `ComboBox` or two-option toggle (`EN` / `עב`)
- Visible on all authenticated views
- On `LoginView` and `OtpVerificationView`: a compact switcher in the login card (pre-auth persistence via cookie only)

### State diagram

```
┌─────────────┐     attach / login      ┌──────────────────┐
│  Browser    │ ──────────────────────► │  LocaleService   │
│  (cookie,   │                         │  (resolve chain) │
│   header)   │ ◄────────────────────── │                  │
└─────────────┘     set cookie/DB       └────────┬─────────┘
                                                 │
                    ┌────────────────────────────┼────────────────────────────┐
                    ▼                            ▼                            ▼
            UI.setLocale()              documentElement            MessageSource
            VaadinSession               dir + lang                  lookups
```

---

## 4. Persistence Strategy

### Three-tier persistence model

| Tier | Store | Scope | When written | When read |
|------|-------|-------|--------------|-----------|
| **1 — Database** | `users.locale` column (`VARCHAR`, values `en` / `he`) | Per authenticated user, cross-device | Language switch while logged in; admin sets in `UsersView` | Priority 1 on login and every authenticated request |
| **2 — HTTP cookie** | `CRM_LOCALE` (`en` or `he`; `Path=/`; `Max-Age=365d`; `SameSite=Lax`) | Per browser, survives restart | Every language switch (logged in or not) | Priority 2; bridges anonymous and authenticated sessions |
| **3 — VaadinSession** | Attribute `crm.locale` | Current server session | On resolve and on switch | Priority 3; avoids re-parsing cookie on every component render |

### localStorage — supplementary, not authoritative

| Aspect | Decision |
|--------|----------|
| Key | `crm.locale` |
| Purpose | Client-side mirror of cookie for instant `dir`/`lang` on first paint before Vaadin round-trip |
| Written by | `executeJs` in `LocaleService.applyDirection()` alongside cookie write |
| Read by | Optional boot script in `index.html` to set `dir`/`lang` before Vaadin hydrates (reduces LTR flash on Hebrew users) |
| Authority | Cookie and DB override localStorage on server attach; localStorage is a cache only |

localStorage alone is insufficient for this architecture because Vaadin is server-rendered and locale must be available in Java for `MessageSource` lookups on the server.

### Persistence on language switch

```
User selects Hebrew in switcher
  │
  ├─► LocaleService.setLocale(he)
  │
  ├─► VaadinSession attribute = he
  ├─► HTTP cookie CRM_LOCALE = he
  ├─► localStorage crm.locale = he          (client mirror)
  ├─► If authenticated: UPDATE users.locale = he
  │
  ├─► UI.setLocale(he)
  ├─► documentElement dir=rtl, lang=he
  │
  └─► UI refresh (re-navigate to current route)
```

### Pre-login behavior

- Anonymous users on `LoginView` / `OtpVerificationView`: cookie + localStorage + session only
- On successful login: if `User.locale` is null, seed DB from cookie; if `User.locale` is set, cookie/session are updated to match DB

### Data model addition (Tier 6 — recommended)

| Column | Type | Default | Notes |
|--------|------|---------|-------|
| `users.locale` | `VARCHAR(5)` | `'en'` | Nullable during migration; existing users default to `en` |

Exposed in `UserRequest` / `UserResponse` and editable in `UsersView` by admins (and optionally by the user via the language switcher).

---

## 5. Global RTL / LTR Handling Strategy

### Principle: one global direction, no per-view overrides

The analysis documents inconsistent RTL today (inline styles in `TimeClockView`, `dir="rtl"` on attendance views, hardcoded CSS in `attendance-calendar.css`). The design replaces all of this with a **single document-level direction** driven by locale.

| Locale | `dir` | `lang` |
|--------|-------|--------|
| `en` | `ltr` | `en` |
| `he` | `rtl` | `he` |

### Layered direction model

| Layer | Mechanism | Owner |
|-------|-----------|-------|
| **Document** | `document.documentElement` attributes `dir` and `lang` | `LocaleService.applyDirection()` |
| **Vaadin Lumo** | Automatic RTL layout when `dir="rtl"` on `<html>` (Vaadin 24) | Framework |
| **Global CSS** | Logical properties in new theme/global stylesheet | Tier 2 CSS |
| **Module CSS** | `attendance-calendar.css` scoped under `[dir="rtl"]` selectors | Tier 7 |
| **Per-component** | **Prohibited** in new code; existing inline `direction: rtl` removed during HR migration | Tier 3/5 |

### CSS migration rules (from analysis findings)

| Current pattern | Replacement |
|-----------------|-------------|
| `direction: rtl` in `attendance-calendar.css` | `[dir="rtl"] .attendance-calendar-page { ... }` or rely on inherited `dir` |
| Inline `card.getStyle().set("direction", "rtl")` in `TimeClockView` | Remove; inherit from document |
| `getElement().setAttribute("dir", "rtl")` in attendance views | Remove; inherit from document |
| `margin-right` in `MainLayout`, `ScheduledTasksView` | `margin-inline-end` |
| `text-align: right` in attendance CSS | `text-align: start` (auto-flips with `dir`) |
| `justify-content: flex-end` in profile section | `justify-content: start` (auto-flips with `dir`) |

### Direction-sensitive UI areas (from analysis)

These areas are verified during implementation, not given special per-locale code:

- `MainLayout` drawer (`SideNav`) — icon and label alignment
- Header bar — bell badge position (migrate to logical properties)
- Grids — column order, sort indicators, inline editors
- Dialogs and confirm dialogs — button order
- Attendance calendar — month nav button DOM order (may need locale-aware ordering if icons do not auto-mirror)
- Directional icons (`ANGLE_LEFT`, `ANGLE_RIGHT`, `ARROW_FORWARD`) — Vaadin icon mirroring or explicit swap by `dir`
- Date/number formatting — active `Locale` passed to `DateTimeFormatter`, not hardcoded `he` or `ENGLISH`

### Email HTML direction

Email templates in `i18n/email/` set `<html lang="..." dir="...">` per template file. Hebrew templates use `lang="he" dir="rtl"`.

---

## 6. Direction Switching at Runtime

### Apply sequence (every locale change and initial attach)

```
Step 1 — Server
  LocaleService resolves or receives new locale
  VaadinSession attribute updated
  Cookie written (response header)
  If authenticated: User.locale persisted to DB

Step 2 — Vaadin UI
  UI.getCurrent().setLocale(locale)
  UI.getCurrent().getPage().executeJs(
      "document.documentElement.setAttribute('dir', $0);
       document.documentElement.setAttribute('lang', $1);
       localStorage.setItem('crm.locale', $1);",
      isRtl(locale) ? "rtl" : "ltr",
      locale.toLanguageTag()
  )

Step 3 — UI refresh
  Re-navigate to current route (preserves URL, rebuilds view tree with new locale)
  All getTranslation() calls return new language
  Vaadin components re-render with correct direction
```

### Initial page load (before Vaadin attaches)

Optional static boot snippet in `index.html` (Tier 2):

```html
<script>
  (function () {
    var loc = localStorage.getItem('crm.locale')
           || (document.cookie.match(/CRM_LOCALE=([^;]+)/) || [])[1]
           || 'en';
    var rtl = loc === 'he';
    document.documentElement.setAttribute('dir', rtl ? 'rtl' : 'ltr');
    document.documentElement.setAttribute('lang', loc);
  })();
</script>
```

This prevents a visible LTR flash for returning Hebrew users. Server-side resolution on attach remains authoritative and may correct the value.

### What is not done at runtime

- No per-component `setAttribute("dir", ...)` calls
- No CSS class toggle on individual views (`rtl-mode` on body is unnecessary if `dir` is on `<html>`)
- No full page reload (`window.location.reload()`); Vaadin re-navigation is sufficient

### HR module transition note

During migration (analysis implementation order step 5), existing Hebrew hardcoded strings and per-view RTL hacks are removed in the same pass. Until migration completes for a given view, that view must not set its own `dir` — it inherits global direction even if some labels are still hardcoded in Hebrew.

---

## 7. How Components Access Translations

### Primary API: `TranslationService` (facade over `MessageSource`)

All Java code — Vaadin views, services, exception handlers — uses one service:

```
TranslationService.translate(String key)
TranslationService.translate(String key, Object... params)
TranslationService.translateEnum(Enum<?> value)
TranslationService.getCurrentLocale()
TranslationService.isRtl()
```

### Access patterns by component type

#### Vaadin views (`com.crm.ui.*`)

| Pattern | Usage |
|---------|-------|
| Constructor injection | `TranslationService` injected via Spring; views are Spring beans |
| Labels and headers | `new H2(i18n.translate("view.accounts.title"))` |
| Grid columns | `.setHeader(i18n.translate("view.accounts.column.name"))` |
| Notifications | `Notification.show(i18n.translate("notification.saved"))` |
| `@PageTitle` | Dynamic via `HasDynamicTitle` interface, or static key resolved in constructor |
| Confirm dialogs | `new ConfirmDialog(i18n.translate("dialog.delete"), ...)` |

Views do **not** call `MessageSource` directly. Views do **not** hardcode strings.

#### `MainLayout`

Injects `TranslationService` + `LocaleService`. Builds `SideNav` labels from `nav.*` keys. Hosts language switcher bound to `LocaleService.setLocale()`.

#### Enums (42 files)

| Pattern | Usage |
|---------|-------|
| Display in grids/badges | `i18n.translateEnum(lead.status())` |
| ComboBox items | `.setItemLabelGenerator(i18n::translateEnum)` |
| Key format | `enum.{SimpleClassName}.{CONSTANT}` e.g. `enum.LeadStatus.NEW` |
| `AttendanceReportType` | `hebrewLabel` field removed; uses same `enum.AttendanceReportType.*` keys |

`EnumLabels` helper (from analysis) is a static or service method:

```
translateEnum(Enum<?> e) →
  messageSource.getMessage("enum." + e.getClass().getSimpleName() + "." + e.name(), ...)
```

#### Backend — REST and services

| Component | Access |
|-----------|--------|
| `GlobalExceptionHandler` | `TranslationService` + `LocaleContextHolder.getLocale()` |
| `ResourceNotFoundException` | Carries message **key** + args, not resolved text; handler resolves at render time |
| `AttendanceReportService` | Validation throws exceptions with keys (`error.attendance.overlap`), not Hebrew/English strings |
| Jakarta validation | `@NotBlank(message = "{validation.required}")` with `MessageSource` configured as validation message source |
| `EmailService` / `ReportEmailService` | Subject from `email.*` key; body loaded from `i18n/email/{template}_{locale}.html` |

#### Date and number formatting

| Pattern | Usage |
|---------|-------|
| Dates in views | `DateTimeFormatter.ofPattern(pattern, i18n.getCurrentLocale())` |
| Currency/numbers | `NumberFormat` / `String.format` with locale from `TranslationService` |

Hardcoded `Locale.forLanguageTag("he")` and `Locale.ENGLISH` are replaced with `getCurrentLocale()` (per analysis RTL section).

### Vaadin `I18NProvider` role

`CrmI18nProvider` implements Vaadin's `I18NProvider` and delegates to the same `MessageSource`. This enables:

- Vaadin built-in component translations (if any keys are added)
- `Component.getTranslation(key)` in views that extend Vaadin base classes with i18n support

Views may use either `TranslationService` (preferred, explicit) or `getTranslation(key)` (Vaadin convention); both hit the same backing store. **Decision: standardize on `TranslationService`** to keep backend and frontend access identical.

### What is not translated

Per analysis: user/business data (account names, activity titles, notes, uploaded content) remains locale-neutral and is displayed as stored.

---

## 8. New Components Summary (implementation reference)

These are planned artifacts from the analysis tiers. Listed here for traceability; not built in this design phase.

| Component | Package (proposed) | Role |
|-----------|-------------------|------|
| `I18nConfig` | `com.crm.config` | `MessageSource` bean, basename `i18n/messages` |
| `CrmI18nProvider` | `com.crm.config` | Vaadin `I18NProvider` → `MessageSource` |
| `LocaleService` | `com.crm.service` | Resolve, set, persist locale; apply direction |
| `TranslationService` | `com.crm.service` | `translate()`, `translateEnum()`, locale helpers |
| `EnumLabels` | `com.crm.util` | Enum key builder (may be folded into `TranslationService`) |
| `LocaleCookieFilter` or resolver | `com.crm.config` | Read/write `CRM_LOCALE` cookie |
| Language switcher | `MainLayout` (+ auth views) | User-facing EN ↔ HE toggle |
| Global RTL CSS | `src/main/frontend/styles/i18n.css` | Logical properties, `[dir="rtl"]` overrides |
| `User.locale` | `com.crm.domain.entity` | DB persistence (Tier 6) |

---

## 9. Implementation Order (from analysis — for downstream phases)

1. Infrastructure: `MessageSource`, `I18NProvider`, `LocaleService`, `TranslationService`, language switcher in `MainLayout`
2. Global RTL: `dir`/`lang` on document + CSS logical properties
3. `MainLayout` + auth views (`LoginView`, `OtpVerificationView`)
4. CRM views (batch by nav section: Contacts → Sales → Settings)
5. HR module: migrate Hebrew hardcoded strings to keys; remove per-view RTL
6. Enums + backend errors + validation messages
7. User locale DB persistence + email templates

---

## 10. Decision Log

| # | Decision | Rationale (from analysis) |
|---|----------|---------------------------|
| D1 | Spring `MessageSource` + Vaadin `I18NProvider` | Server-rendered Vaadin; no i18n infra exists; single catalog for UI + backend |
| D2 | Property files under `src/main/resources/i18n/` | Analysis recommended structure; supports UTF-8 Hebrew |
| D3 | `TranslationService` as single access API | Avoids split patterns between views, services, and exception handler |
| D4 | DB + cookie + session persistence; localStorage as mirror | Vaadin needs server-side locale; cookie works pre-login; DB for cross-device; localStorage prevents flash |
| D5 | Global `dir`/`lang` on `<html>` only | Eliminates inconsistent per-view RTL in HR module |
| D6 | CSS logical properties over physical left/right | Analysis found `margin-right` in `MainLayout` that won't flip in RTL |
| D7 | Enum keys replace `.name()` and `hebrewLabel` | 42 enums displayed raw; `AttendanceReportType` already has embedded Hebrew |
| D8 | Email bodies in `i18n/email/*.html` per locale | Analysis: multi-line HTML unsuitable for `.properties` |
| D9 | Two locales only (`en`, `he`) | Stated project goal; no variant locales in v1 |
| D10 | Re-navigate on locale switch | Analysis: "refresh or re-navigate views so labels update" |

---

*Architecture design derived from [`docs/i18n-analysis.md`](i18n-analysis.md). No code has been written or modified.*
