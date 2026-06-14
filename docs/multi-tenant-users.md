# Multi-Tenant User Management

## Overview

The CRM supports multiple companies (tenants) on a single deployment. Each company owns a **Workspace**. Users belong to exactly one workspace and can only see data — contacts, accounts, leads, etc. — that belongs to that same workspace. A platform-level `SUPER_ADMIN` can see everything across all workspaces.

---

## Role Hierarchy

```
ROLE_SUPER_ADMIN
    └── ROLE_COMPANY_ADMIN
            └── ROLE_ADMIN
                    ├── ROLE_SALES
                    ├── ROLE_SUPPORT
                    └── ROLE_USER
```

A role higher in the tree inherits all view-access permissions of the roles below it. **Data scoping is NOT driven by the hierarchy** — it is always driven by the stored database role.

| Role | Can invite users | Sees all workspaces | Manages users |
|---|---|---|---|
| `ROLE_SUPER_ADMIN` | ✅ any workspace | ✅ | ✅ any workspace |
| `ROLE_ADMIN` (legacy global) | ✅ any workspace | ✅ | ✅ any workspace |
| `ROLE_COMPANY_ADMIN` | ✅ own workspace only | ❌ own workspace only | ✅ own workspace only |
| `ROLE_SALES` | ❌ | ❌ | ❌ |
| `ROLE_SUPPORT` | ❌ | ❌ | ❌ |
| `ROLE_USER` | ❌ | ❌ | ❌ |

### Why `isAdmin()` checks the database, not Spring Security authorities

Because the role hierarchy gives `ROLE_COMPANY_ADMIN` the `ROLE_ADMIN` Spring Security authority at login time, a naive `hasRole("ADMIN")` check would make a company admin appear as a global admin and bypass workspace filtering. `WorkspaceContext.isAdmin()` always re-reads the stored roles from the database.

---

## Invitation Flow

### OTP-based invite (primary flow — used by the UI)

```
Admin (COMPANY_ADMIN / SUPER_ADMIN)
  │
  ├─ Opens Settings → Users → "Invite User"
  │    Enters email + role
  │
  ▼
AdminUserManagementService.inviteUser()
  ├─ Validates role is in {ROLE_COMPANY_ADMIN, ROLE_USER, ROLE_SALES, ROLE_SUPPORT}
  ├─ Checks acting user is allowed to invite into target workspace
  ├─ Creates User with status = INVITED (random placeholder password)
  ├─ Adds user to workspace_members
  ├─ Generates a 6-digit OTP (stored in OtpService / Redis or in-memory)
  └─ Sends invite email via EmailService.sendInvite()
       • Subject / body rendered from invitation.html template
       • Contains a clickable link: {APP_BASE_URL}/verify-invite?email=...
       • OTP code shown in the email body

Invited user clicks the link
  │
  ▼
VerifyInviteView  (/verify-invite?email=...)
  ├─ User enters: OTP code, new password, confirm password
  └─ Calls AdminUserManagementService.verifyInviteOtp(email, otp, rawPassword)
       ├─ Validates OTP (throws InvitationInvalidException on any failure)
       ├─ BCrypt-encodes and saves the new password
       ├─ Sets status = ACTIVE
       └─ Logs the user in and redirects to the dashboard
```

### Token-based invite (InvitationService — alternative / API path)

```
POST /api/v1/invitations  →  InvitationService.createInvitation()
  ├─ Generates a UUID raw token
  ├─ Stores SHA-256(token) in the invitations table
  ├─ Token expires after 72 hours
  └─ Link: {APP_BASE_URL}/accept-invite/{rawToken}

GET /accept-invite/{token}  →  AcceptInviteView
POST (form submit)           →  InvitationService.acceptInvitation()
  ├─ Looks up invitation by SHA-256(token)
  ├─ Checks not expired, not already accepted
  ├─ Creates user with chosen username + password
  ├─ Adds user to workspace_members
  └─ Marks invitation as accepted
```

Expired invitations are purged nightly at **04:00** by `InvitationService.cleanupExpired()`.

---

## User Lifecycle

```
[INVITED] ──── verifyInviteOtp() ──── [ACTIVE] ──── disableUser() ──── [DISABLED]
                                          │                                   │
                                          └───────── enableUser() ────────────┘
```

- `INVITED` — account exists, password is a random placeholder, cannot log in.
- `ACTIVE` — normal login allowed.
- `DISABLED` — account blocked, cannot log in. Can be re-enabled by a COMPANY_ADMIN.

---

## Users Panel (Settings → Users)

Accessible by `ROLE_ADMIN`, `ROLE_COMPANY_ADMIN`, and `ROLE_SUPER_ADMIN`.

| Action | Button | Who can use it |
|---|---|---|
| Invite a new user | ✉ Invite | COMPANY_ADMIN, SUPER_ADMIN |
| Resend invite email | ✉ (row) | COMPANY_ADMIN, SUPER_ADMIN — only on INVITED rows |
| Edit name / email | ✏ | Any admin, including on own account |
| Disable / enable | 🚫 / ✓ | COMPANY_ADMIN, SUPER_ADMIN — not on own account |
| Change role | 🖉 | COMPANY_ADMIN, SUPER_ADMIN — not on own account |
| Delete user | 🗑 | COMPANY_ADMIN, SUPER_ADMIN — not on own account |

**Last-admin protection** — disabling, deleting, or demoting the last active `ROLE_COMPANY_ADMIN` in a workspace is blocked with a user-visible error.

**Workspace scoping in the panel:**

- `SUPER_ADMIN` / `ROLE_ADMIN` → sees all users across all workspaces.
- `COMPANY_ADMIN` → sees only users whose `workspace_id` matches their own.
- Legacy accounts with `workspace_id = NULL` are resolved on first visit by looking up membership in the `workspace_members` join table and persisting the found ID.

---

## Data Isolation

Every service that fetches tenant-scoped data calls `WorkspaceContext`:

```java
// Non-admin users see only their workspace
List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
return repository.findByWorkspace_IdIn(wsIds, pageable);

// Global admins (ROLE_ADMIN / ROLE_SUPER_ADMIN) see everything
if (workspaceContext.isAdmin()) {
    return repository.findAll(pageable);
}
```

Services that apply this pattern: `ContactService`, `AccountService`, and all data-bearing services.

---

## Database Schema

### `users` table (key columns)

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `username` | VARCHAR | unique |
| `email` | VARCHAR | unique |
| `password` | VARCHAR | BCrypt, cost 12 |
| `roles` | text[] / JSON | e.g. `["ROLE_COMPANY_ADMIN"]` |
| `status` | VARCHAR | `INVITED` / `ACTIVE` / `DISABLED` |
| `workspace_id` | BIGINT FK | tenant scope |

### `workspace_members` join table

Many-to-many between `workspaces` and `users`. A user must be removed from this table before deletion to avoid FK constraint errors. `AdminUserManagementService.removeUser()` handles this automatically.

### `invitations` table

| Column | Notes |
|---|---|
| `token_hash` | SHA-256 of the raw UUID token, unique |
| `email` | Invited email address |
| `role` | Role to assign on acceptance |
| `workspace_id` | Target workspace (CASCADE DELETE) |
| `expires_at` | 72 hours after creation |
| `accepted_at` | NULL until invitation is used |

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `app.base-url` | `http://localhost:9080` | Base URL prepended to invite links in emails |

**Production (Railway):** set the `APP_BASE_URL` environment variable to the deployed URL (e.g. `https://crm-app.up.railway.app`). This is read by `application-prod.properties`:

```properties
app.base-url=${APP_BASE_URL:https://crm-app.up.railway.app}
```

---

## Key Source Files

| File | Purpose |
|---|---|
| `service/AdminUserManagementService.java` | Invite, verify OTP, disable/enable, change role, delete — all with workspace guards |
| `service/InvitationService.java` | Token-based invite flow (API path) |
| `service/WorkspaceContext.java` | `isAdmin()` and `currentUserWorkspaceIds()` — tenant resolution |
| `config/SecurityConfig.java` | Role hierarchy definition and `DaoAuthenticationProvider` setup |
| `ui/UsersView.java` | Users panel UI — invite dialog, role change, edit, delete |
| `ui/VerifyInviteView.java` | OTP + password form for invited users |
| `ui/AcceptInviteView.java` | Token-based acceptance form |
| `domain/entity/Invitation.java` | Invitation entity with `isExpired()` / `isAccepted()` helpers |
| `resources/db/migration/V2.5.0__create_invitations_table.sql` | Flyway migration for the invitations table |
