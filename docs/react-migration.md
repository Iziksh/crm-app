# Converting the Frontend from Vaadin to React

This document is a practical checklist for replacing the current Vaadin Flow UI
(`src/main/java/com/crm/ui/**`, 33 views) with a React single-page app, while keeping the
Spring Boot backend. It reflects the actual state of this codebase, not a generic Vaadin→React
guide — read section 1 before starting, because it changes how much work this really is.

## Status: migration complete, feature-parity gaps closed (as of 2026-07-16)

All 33 Vaadin views now have a React equivalent, built in `crm-web/` (Vite + React Router +
TanStack Query, monday.com-style design system per section 4). A few pragmatic simplifications
were made along the way, documented inline in the corresponding page/component:

- **CSV import/export, file attachments, the Accounts "Addons" tab, and login 2FA** were
  initially left as known gaps (see section 2's original punch-list), then closed in a follow-up
  pass once the rest of the app was live on production:
  - **Import/export** was already 100% backed by REST (`/contacts/import`+`/export`,
    `/accounts/import`+`/export`) — just needed `ListToolbar` wired up with an `onImport`/
    `onExport` pair (file input + `apiUpload`/`apiDownload` helpers in `api/client.ts`).
  - **File attachments** were likewise already a complete generic REST API
    (`/api/v1/attachments/**`) — added a reusable `AttachmentPanel.tsx` component and wired it
    into the Contact/Account/Activity edit modals (the same 3 entity types the Vaadin
    `AttachmentPanel` was used for).
  - **Accounts "Addons" tab** needed a real backend addition: `AddonController` (thin CRUD wrapper
    over the pre-existing `AddonService`), `@PreAuthorize("hasRole('ADMIN')")`-gated for
    create/update/delete, list open to all. The React `AddonsPanel.tsx` lives behind a "Details /
    Addons" tab in the Account edit modal, add/edit/delete only shown to admin-level roles
    (`hasEffectiveRole(user?.roles, "ROLE_ADMIN")`, same hierarchy check used elsewhere).
  - **Login 2FA was a genuine backend gap**, not just missing UI — `/api/v1/auth/login` used to
    skip the OTP step entirely that the Vaadin session login always enforced. Ported the full
    flow: `/login` now checks a client-supplied `deviceToken` against `DeviceTrustService`, and if
    absent/invalid emails an OTP and returns `{otpRequired: true, maskedEmail}` instead of a token;
    `/login/verify-otp` validates the code and returns the JWT (plus a new device-trust token if
    the client asked to be remembered); `/login/resend-otp` re-sends. The device token is stored
    client-side in `localStorage` (not a cookie, since the SPA and API are different origins) via
    `lib/deviceTrust.ts`. **Deliberate parity decision:** if a user has no deliverable email at
    all, login is blocked outright — exactly like the Vaadin behavior — rather than silently
    skipping 2FA, so this is a hard requirement going forward, not just a soft warning.
- **`PaymentRequestDocumentView`** *was* ported faithfully as its own route
  (`/payment-request-document/:id`, `PaymentRequestDocumentPage.tsx`) — the overview card,
  tax-status warning banner, action panel (create invoice/receipt/proforma, cancel), and the
  itemized document preview with signature lines all match the original view. It also gained a
  color-coded share panel (Email/WhatsApp/Print/Download) that didn't exist as a distinct visual
  treatment in Vaadin — see the note on the monday.com palette below.
- **`RootRedirectView`** has no dedicated page — the same behavior (redirect to `/login` when
  unauthenticated, otherwise show the app) is handled by the `RequireAuth` wrapper in `App.tsx`.
- A number of backend gaps and bugs were found and fixed along the way (not anticipated by this
  doc originally, several only surfaced once the React app was pointed at the **real production
  Postgres database**, not the local H2 dev DB):
  - A reserved-word (`year`) H2 schema bug blocking the whole Billing & Documents feature in dev
    only (Postgres/Flyway in prod was unaffected).
  - A missing `User.workspaceId` self-heal (`DataInitializer.backfillMissingPrimaryWorkspace()`)
    for users who never got a primary workspace assigned.
  - **JWT requests never applied Spring Security's `RoleHierarchy`** — only session-based Vaadin
    login did. This meant an `ROLE_ADMIN`-level JWT user failed `hasRole('ADMIN')`-style checks
    that succeeded for the same user via Vaadin. Fixed in `JwtAuthenticationFilter` by expanding
    authorities through the hierarchy bean before building the auth token; this is what caused the
    production "Users page only shows one user" bug.
  - `DuplicateEmailException` now takes an entity label (`"Contact"`, `"Account"`) so duplicate-
    email errors read as e.g. *"Contact with this email already exists"* instead of the generic,
    misleading *"Email already registered"* (that message is still used for real user-registration
    duplicates).
  - Several REST endpoints that only existed as Vaadin-view-to-service calls (signup/OTP flow,
    invitation acceptance, a `/auth/me` endpoint) needed thin controller wrappers added.
  - Contacts gained a free-text **Company** field (`ContactRequest`/`ContactResponse`, migration
    `V2.15.0`), distinct from the `accountId` link — captures an employer name even when no
    matching `Account` record exists yet. The New/Edit Contact form's account dropdown is now
    labeled **"Belongs to Account"** to make that distinction explicit to users.
- CSV import/export, file attachments, and the Accounts "Addons" sub-tab were not ported —
  same reasoning as originally scoped in section 2: real functional parity for core CRUD, not
  pixel-for-pixel feature parity everywhere.

The Vaadin app (`com.crm.ui.**`) has **not** been deleted — both UIs currently coexist per
section 6's guidance, and cutover/cleanup is still a separate decision for the team to make.

## 1. The important discovery: the backend is already API-first

This app does **not** need a "wrap the backend in a REST API" phase. It already has one, built
in parallel with the Vaadin UI:

- **32 REST controllers** under `com/crm/controller/**`, `com/crm/billing/controller/**`, and
  `com/crm/timetracking/controller/**`, all under `/api/v1/**`.
- **Stateless JWT auth is already wired up and isolated from Vaadin's session auth.** See
  `com/crm/config/SecurityConfig.java`:
  - An `@Order(1)` `SecurityFilterChain` matches `/api/**` only, disables CSRF, sets
    `SessionCreationPolicy.STATELESS`, and runs `JwtAuthenticationFilter` before the standard
    username/password filter.
  - A second chain (`VaadinWebSecurity`, the default/lower-priority one) handles everything else
    session-based, for the Vaadin views.
  - These two chains don't interfere with each other. **You can build and test the React app
    against the existing `/api/v1/**` endpoints today, in parallel with the live Vaadin UI,
    without touching backend security code.**
- `POST /api/v1/auth/login` and `/register` (`AuthController`) already return a JWT
  (`AuthResponse`). A React app authenticates exactly the way a mobile client would: store the
  token (memory or `httpOnly` cookie — avoid `localStorage` for XSS resistance) and send
  `Authorization: Bearer <token>` on every call.

This means the migration is mostly **frontend build-out + a short backend punch-list**, not a
rewrite of the domain/service layer. Do not re-architect the service or repository layers —
they're already UI-agnostic (Vaadin views call the same `@Service` classes the controllers do).

## 2. Backend punch-list (small, do this first)

Even though the API exists, a few things were never needed because Vaadin runs same-origin and
server-rendered. A React app served separately (dev server on its own port, or a static build)
needs:

1. **CORS.** There is currently no `CorsConfiguration`/`@CrossOrigin`/`CorsFilter` anywhere in
   the codebase — grep confirms it. Add a `CorsConfigurationSource` bean (allowed origins,
   `Authorization` header, credentials as needed) and wire it into the `/api/**` filter chain in
   `SecurityConfig`. Restrict allowed origins per environment (dev: `localhost:5173`/`3000`;
   prod: the real static-asset origin) rather than `*`.
2. **REST parity audit.** Every Vaadin view needs to be checked against the controller list —
   most already have a matching controller, but confirm each one before porting it. Views to
   double check because they look thin or aggregate other data (no obviously-dedicated
   controller found during this pass): `DashboardView`, `CalendarView`, `MyProfileView`,
   `OtpVerificationView`, `AttendanceCorrectionView`, `TimeClockView`,
   `PaymentRequestDocumentView`/`TaxDocumentEditorView`/`PaymentRequestEditorView`,
   `ScheduledTasksView`, `SavedSearchesView`, `WorkspacesView`. For each, either find the backing
   endpoint (it may already exist under a controller with a different name) or add one — do not
   invent new service logic, only a thin controller over the existing service.
3. **File upload/download parity.** `AttachmentController` exists; confirm every attachment/
   document flow (quotes, payment requests, tax documents, WhatsApp document sharing via
   `PublicDocumentShareController`) is reachable over plain HTTP multipart, since Vaadin's
   `Upload`/`StreamResource` components won't have a React equivalent to fall back on.
4. **Refresh/expiry story for JWTs.** Check `JwtService` for the current token TTL. Vaadin's
   session-based UI never needed a refresh flow; a long-lived SPA session will. Decide now:
   short-lived access token + refresh endpoint, or a longer-lived token accepted as a tradeoff —
   don't retrofit this after the React app is built.
5. **Error response shape.** Confirm `com/crm/exception/**` produces a consistent JSON error body
   (status, message, field errors) across all controllers — Vaadin views may currently rely on
   exceptions being caught ad hoc in Java rather than a uniform API contract a frontend can parse.

## 3. New React app — scaffolding

Keep it a separate deployable artifact from `crm-app`; don't try to make Vaadin's Flow/frontend
tooling (`src/main/frontend/**`, which is Vaadin's generated Hilla/Flow assets, not a real app
shell) serve React.

1. Scaffold with Vite (`npm create vite@latest crm-web -- --template react-ts`) as a sibling
   directory, e.g. `C:\crm\crm-web`, not nested inside `crm-app/src/main/frontend` — that folder
   is Vaadin-generated (`generated/flow`, `generated/jar-resources`) and will be deleted wholesale
   once Vaadin is removed.
2. Pick a router (React Router) and mirror the current navigation structure. `MainLayout.java`
   (referenced in memory as heavily modified) defines the current nav/menu — read it once to get
   the full route list and role-based visibility rules (the `RoleHierarchy` bean in
   `SecurityConfig` — `SUPER_ADMIN > COMPANY_ADMIN > ADMIN > SALES/SUPPORT/USER` — needs a
   client-side equivalent for hiding nav items/routes).
3. Pick a data-fetching layer (TanStack Query is the natural fit) so you get caching, retries,
   and loading/error states per-endpoint instead of re-deriving that per view like Vaadin's
   `Grid`/`Binder` did implicitly.
4. Pick a form library matching Vaadin's `Binder` role (React Hook Form + zod/yup) — every view
   with a form (leads, opportunities, quotes, contracts, users, workspaces, invitations) has
   validation currently expressed via Vaadin binders and/or Jakarta `@Valid` DTOs; the latter is
   what you should mirror client-side (same field names/constraints), not re-derive from the UI.
5. Component library: pick one now. Given the monday.com-style direction (section 4 below),
   default to **headless components (Radix or Ark UI) + Tailwind**, or **Mantine** if you want
   more out of the box — both are unopinionated about visual style. Skip MUI/Ant Design here:
   their Material/Ant look fights a monday.com aesthetic hard enough that you'd spend more effort
   overriding their defaults than building from a headless base. Whichever you pick, the Vaadin
   views use `Grid`, `ComboBox`, `DatePicker`, dialogs, and tabs heavily; you need equivalents for
   all of those or you'll be building them by hand for 33 views.

## 4. Visual design direction: monday.com style

The ask here is a specific, recognizable SaaS look — not a generic component-library default.
monday.com's UI is built on a handful of consistent patterns; treat these as the target, not the
migration-order list in section 5, which is about sequencing, not aesthetics.

- **Color-coded status as the primary visual language.** Every record (lead, opportunity, quote,
  task) gets a status rendered as a solid-color rounded pill/chip (e.g. green "Won", orange
  "Pending", red "Overdue", blue "In Progress") — not a plain text label. This is the single most
  recognizable monday.com trait; apply it everywhere `*View.java` currently renders an enum
  (`LeadStatus`, `OpportunityStage`, `ContractStatus`, etc. in `com/crm/domain/enums`) as plain
  text in a Vaadin `Grid` column.
- **Board/table hybrid, not a plain data grid.** monday.com's core view is a colorful table with
  grouped rows (colored group headers you can collapse), inline-editable cells, and a
  circular "+" to add a row — a step up from Vaadin's plain `Grid`. For list views (`LeadsView`,
  `OpportunitiesView`, `AccountsView`, etc.) aim for grouped, inline-editable rows rather than a
  static table with an "Edit" button per row.
- **Bright, saturated accent palette on a mostly white/light-gray canvas.** Chrome (sidebar,
  headers) stays neutral; color is reserved for status pills, avatars, and action buttons. Don't
  spread color evenly — it should read as "mostly white, with color used to mean something."
  Follow the `dataviz` skill's palette/contrast guidance if you also add charts (`ForecastView`,
  `DashboardView`) so chart colors stay consistent with the status-pill palette.
- **Rounded corners and soft shadows throughout** (cards, pills, buttons, modals) — no sharp
  Vaadin-Lumo-style rectangles.
- **Avatars everywhere a person is referenced** (assigned rep on a lead, owner on an
  opportunity, uploader on an attachment) — small circular avatar + name, not just a name string.
- **Left sidebar navigation, grouped and expandable** (240px, `Sidebar.tsx`) — mirrors
  `MainLayout.java`'s actual structure rather than collapsing it to icons: a standalone Dashboard
  link plus six collapsible groups (Contacts, Support, Sales, Settings, HR, Billing & Documents),
  each with its own icon, label, and chevron toggle. Admin-only items (Users, Task Queue,
  Corrections) are filtered client-side via `lib/roles.ts`, which mirrors the backend's
  `RoleHierarchy` (`SUPER_ADMIN > COMPANY_ADMIN > ADMIN > SALES/SUPPORT/USER`) so any
  admin-level role — not just `SUPER_ADMIN` — sees them.
- **Row hover reveals actions** (edit/delete/duplicate icons appear on hover instead of being
  always-visible buttons) — keeps tables visually quiet until you interact with them.
- **Color-coded action buttons, not just status pills.** The `PaymentRequestDocumentPage` share
  panel applies the same "color means something" rule to actions: Email = sky blue (matches the
  status-pill "new" tone), WhatsApp = brand green (`#25d366`), Print = cool teal (`#00b8a9`),
  Download = indigo (`#6c5ce7`). Reuse this palette for any future action-button row rather than
  inventing new colors per view.

This is a design *direction*, not a pixel spec — don't chase monday.com's exact CSS. Establish
the pattern (status-pill component, grouped/inline-editable table, icon rail) once during the
first couple of migrated views (section 5's step 2, the read-heavy list views), then reuse it for
the rest so all 33 views end up visually consistent rather than each view inventing its own look.

## 5. Migration order (incremental, not big-bang)

Given 33 views, do not attempt a flag-day cutover. Suggested order, easiest/highest-value first:

1. **Auth screens** (`LoginView`, `RegisterView`, `OtpVerificationView`,
   `VerifyRegistrationView`, `AcceptInviteView`, `VerifyInviteView`) — self-contained, exercises
   the JWT flow end-to-end, and unblocks everything else.
2. **Read-heavy list/detail views** with existing REST parity (`AccountsView`, `ContactsView`,
   `LeadsView`, `OpportunitiesView`, `ProductsView`) — straightforward CRUD against existing
   controllers, good place to establish the Grid/table + form patterns you'll reuse everywhere
   else.
3. **Workflow-heavy views** (`QuotesView`, `SalesOrdersView`, `ContractsView`,
   `PaymentRequestEditorView`, `TaxDocumentEditorView`) — these likely have multi-step or
   line-item editing logic currently embedded in the Vaadin view class; that logic needs to be
   read out of Java and re-implemented in React/JS, since none of it lives in the service layer.
4. **Admin/ops views** (`UsersView`, `WorkspacesView`, `ScheduledTasksView`,
   `SubscriptionsView`, `SavedSearchesView`) — lower traffic, fine to do last.
5. **Time-tracking module** (`TimeClockView`, `AttendanceCalendarView`,
   `AttendanceCorrectionView`) — self-contained package (`com/crm/timetracking/**`), can be
   migrated independently of the rest.

Run both frontends side by side during the transition: keep Vaadin serving its views while React
takes over one route at a time (e.g. reverse-proxy `/app/*` to React, leave everything else on
Vaadin, and cut Vaadin's route over to a redirect once its React replacement ships). Don't try to
embed React inside a Vaadin view or vice versa — treat it as two independent frontends sharing
one backend during the transition, not one hybrid app.

## 6. What to delete, and when

Only after every view above has a working React replacement and the team has signed off:

- `src/main/java/com/crm/ui/**` (all 33 `*View.java` classes and `MainLayout.java`).
- The `vaadin-spring-boot-starter` / `vaadin-bom` dependencies and `vaadin.version` property in
  `pom.xml`.
- `src/main/frontend/**` (Vaadin's generated frontend assets — `generated/flow`,
  `generated/jar-resources`, theme CSS).
- `VaadinWebSecurity` usage in `SecurityConfig` — once there's no Vaadin UI to protect, the
  `configure(HttpSecurity)` override and `setLoginView(...)` call go away, and the API filter
  chain becomes the only chain.

Don't delete any of this before the React app has full parity — the two UIs are meant to coexist
during the migration, not race each other.
