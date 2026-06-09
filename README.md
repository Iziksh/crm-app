# Spring Boot CRM — OpenCRX Feature Parity Implementation

A full-featured CRM built with Spring Boot 3, Vaadin 24, and Spring Security, replicating OpenCRX (`localhost:9090`) as a single executable JAR.

---

## Table of Contents

1. [Project Goal](#1-project-goal)
2. [OpenCRX → Spring Boot Feature Map](#2-opencrx--spring-boot-feature-map)
3. [Technology Stack](#3-technology-stack)
4. [Project Structure](#4-project-structure)
5. [Domain Model and Database Schema](#5-domain-model-and-database-schema)
6. [Sales Pipeline Flow](#6-sales-pipeline-flow)
7. [REST API Reference](#7-rest-api-reference)
8. [Vaadin UI Views](#8-vaadin-ui-views)
9. [Step-by-Step Implementation Guide](#9-step-by-step-implementation-guide)
10. [Running the Application](#10-running-the-application)
11. [Configuration Reference](#11-configuration-reference)
12. [Testing](#12-testing)
13. [Default Credentials](#13-default-credentials)

---

## 1. Project Goal

OpenCRX ships as a JEE application on Apache TomEE. This Spring Boot project replaces it with:

- A single `crm-app.jar` on port `9080`
- Vaadin 24 server-rendered UI (no separate front-end build)
- Statele
- REST API secured with JWT
- H2 in dev, PostgreSQL in prod
    
---

## 2. OpenCRX → Spring Boot Feature Map

| OpenCRX Section | OpenCRX Item | Spring Boot Implementation |
|---|---|---|
| **Contacts** | Manage Accounts | `AccountsView` + `GET /api/v1/accounts` |
| **Contacts** | New Contact | Dialog in `ContactsView` + `POST /api/v1/contacts` |
| **Contacts** | All Accounts | `AccountsView` grid with type filter |
| **Contacts** | Account Groups | `AccountGroup` entity + `AccountGroupsView` |
| **Contacts** | Accounts (disabled) | `Account.enabled` flag + filter toggle |
| **Contacts** | Saved Searches — Accounts | `SavedSearch` scoped to `ACCOUNT` |
| **Contacts** | Addresses | `Address` entity (one-to-many on Account and Contact) |
| **Contacts** | Addresses (disabled) | `Address.enabled` flag |
| **Contacts** | Saved Searches — Addresses | `SavedSearch` scoped to `ADDRESS` |
| **Support** | Bugs + Features | `Activity` with `type = BUG / FEATURE` |
| **Support** | New Activity | Dialog in `ActivitiesView` + `POST /api/v1/activities` |
| **Support** | Activities list | `ActivitiesView` grid with status/type filters |
| **Sales** | Sales Dashboard | Enhanced `DashboardView` with pipeline KPIs |
| **Sales** | New Lead | `LeadsView` + `POST /api/v1/leads` |
| **Sales** | New Opportunity | `OpportunitiesView` + `POST /api/v1/opportunities` |
| **Sales** | New Quote | `QuotesView` + `POST /api/v1/quotes` |
| **Sales** | New Sales Order | `SalesOrdersView` + `POST /api/v1/sales-orders` |
| **Sales** | Won Opportunities | `OpportunitiesView` filtered `stage = WON` |
| **Sales** | Won Quotes | `QuotesView` filtered `status = WON` |
| **Sales** | Won Leads | `LeadsView` filtered `status = WON` |
| **Sales** | Contracts | `ContractsView` + `GET /api/v1/contracts` |
| **Sales** | Quote / Lead / Opportunity Forecast | `ForecastView` tabs |
| **Workspaces** | Default Workspace | `Workspace` entity, all records scoped |
| **Activities** | Follow-up Notes / Add Note | `ActivityNote` entity + notes timeline (Phase 11) |
| **Activities** | 11 Activity Types | `EMAIL, SALES_VISIT, MAILING, SMS, ABSENCE` added (Phase 11) |
| **Products** | Product Catalog / Price List | `Product` entity + `ProductsView` (Phase 12) |
| **Admin** | Managing Users | `UsersView` (admin only) + `UserController` (Phase 13) |
| **Data** | Import / Export | CSV import (Contacts, Accounts) + CSV export all grids (Phase 14) |
| **Notify** | Subscribe / Notify Alerts | `Alert` + `Subscription` + `Topic` entities; `SubscriptionHandlerService` audit-trail scanner; `AlertService.sendAlert()`; bell badge + 3-state mark (NEW/READ/ACCEPTED/EXPIRED); 11 standard Topics; `SubscriptionsView` (Phase 15) |
| **Documents** | File Attachments | `Attachment` entity, upload on Account/Contact/Activity (Phase 16) |
| **Calendar** | Meeting / Task Calendar | `CalendarView` month grid + iCalendar export (Phase 17) |
| **Email** | E-Mail Services | Spring Mail + EMAIL activity type (Phase 18) |
| **Security** | Email OTP 2-Factor Auth | Caffeine-cached OTPs, HTML email, trusted-device cookie (14 days), `OtpVerificationView` (Phase 19) |
| **Dashboard** | Sales Charts | CSS horizontal bar charts: Pipeline by Stage, Lead Funnel, Activity Breakdown, Contract Health + Win Rate KPI (Phase 20) |
| **Time Clock** | Clock In / Clock Out | `AttendanceService.punchIn/punchOut()` + partial unique index dedup + `GET/POST /api/v1/attendance` (Phase 21–23) |
| **Time Clock** | Israeli Public Holidays | `IsraeliHolidayService` using KosherJava (`com.kosherjava:zmanim`), annual `HolidayRecalculationJob` cron (Phase 24) |
| **Time Clock** | Monthly Accountant Report | `ExcelReportService` (Apache POI `.xlsx`), `MonthlyReportJob` cron, emailed to accountant on 1st of month (Phase 25) |

---

## 3. Technology Stack

| Component | Choice | Version |
|---|---|---|
| Framework | Spring Boot | 3.3.5 |
| Language | Java | 17 |
| Build | Maven | 3.9.x |
| UI | Vaadin Flow | 24.5.6 |
| Security | Spring Security + JJWT | 6.x / 0.12.6 |
| Persistence | Spring Data JPA + Hibernate | 6.x |
| Database (dev) | H2 in-memory | runtime |
| Database (prod) | PostgreSQL | 15+ |
| API Docs | SpringDoc OpenAPI | 2.6.0 |
| Testing | JUnit 5, Mockito, Spring Test | included |

---

## 4. Project Structure

```
crm-app/src/main/java/com/crm/
├── CrmApplication.java
├── config/
│   ├── AsyncConfig.java              # @EnableAsync for email sending
│   ├── DataInitializer.java          # Seeds admin user (email synced from app.admin.email) + Default workspace + Topics
│   ├── JwtConfig.java
│   └── SecurityConfig.java
├── domain/
│   ├── entity/
│   │   ├── User.java                 # BUILT
│   │   ├── Account.java              # BUILT
│   │   ├── Contact.java              # BUILT
│   │   ├── AccountGroup.java         # BUILT
│   │   ├── Address.java              # BUILT
│   │   ├── Activity.java             # BUILT
│   │   ├── Lead.java                 # BUILT
│   │   ├── Opportunity.java          # BUILT
│   │   ├── Quote.java                # BUILT
│   │   ├── QuoteLineItem.java        # BUILT
│   │   ├── SalesOrder.java           # BUILT
│   │   ├── SalesOrderLineItem.java   # BUILT
│   │   ├── Contract.java             # BUILT
│   │   ├── SavedSearch.java          # BUILT
│   │   ├── Workspace.java            # BUILT
│   │   ├── Alert.java                # BUILT (Phase 15)
│   │   ├── Subscription.java         # BUILT (Phase 15)
│   │   ├── Topic.java                # BUILT (Phase 15)
│   │   ├── AuditLog.java             # BUILT (Phase 15)
│   │   └── TrustedDevice.java        # BUILT (Phase 19) — hashed device-trust tokens
│   └── enums/
│       ├── AccountType.java          # BUILT
│       ├── ContactStatus.java        # BUILT
│       ├── AddressType.java          # BUILT
│       ├── SavedSearchScope.java     # BUILT
│       ├── ActivityType.java         # BUILT
│       ├── ActivityStatus.java       # BUILT
│       ├── LeadStatus.java           # BUILT
│       ├── LeadSource.java           # BUILT
│       ├── OpportunityStage.java     # BUILT
│       ├── QuoteStatus.java          # BUILT
│       ├── SalesOrderStatus.java     # BUILT
│       ├── ContractStatus.java       # BUILT
│       ├── AlertState.java           # BUILT (Phase 15)
│       ├── AlertImportance.java      # BUILT (Phase 15)
│       └── SubscriptionEventType.java # BUILT (Phase 15)
├── dto/
│   ├── request/                      # BUILT: Login, Register, Account, Contact
│   └── response/                     # BUILT: Auth, Account, Contact
├── repository/                       # BUILT: User, Account, Contact, TrustedDevice
├── service/
│   ├── AlertService.java             # BUILT — sendAlertFull uses REQUIRES_NEW transaction
│   ├── DeviceTrustService.java       # BUILT (Phase 19) — SHA-256 hashed cookie tokens, 14-day expiry, nightly cleanup
│   ├── EmailService.java             # BUILT (Phase 19) — async HTML OTP emails via JavaMailSender
│   ├── OtpService.java               # BUILT (Phase 19) — Caffeine-cached 6-digit codes, 5-min TTL, single-use
│   └── ...                           # User, Account, Contact, Jwt, etc.
├── controller/                       # BUILT: Auth, Account, Contact
├── security/                         # BUILT: JwtFilter, UserDetailsServiceImpl
├── exception/                        # BUILT: GlobalHandler, ErrorResponse, exceptions
└── ui/
    ├── LoginView.java                # BUILT (Phase 19) — Vaadin form, checks trusted-device cookie before OTP
    ├── OtpVerificationView.java      # BUILT (Phase 19) — @AnonymousAllowed, resend, trust-device checkbox
    ├── MainLayout.java               # BUILT — extend nav each phase
    ├── DashboardView.java            # BUILT — extend KPI cards each phase
    ├── AccountsView.java             # BUILT
    ├── ContactsView.java             # BUILT
    ├── SecurityService.java          # BUILT
    ├── AccountGroupsView.java        # BUILT
    ├── AddressesView.java            # BUILT
    ├── ActivitiesView.java           # BUILT
    ├── LeadsView.java                # BUILT
    ├── OpportunitiesView.java        # BUILT
    ├── QuotesView.java               # BUILT
    ├── SalesOrdersView.java          # BUILT
    ├── ContractsView.java            # BUILT
    ├── ForecastView.java             # BUILT
    ├── WorkspacesView.java           # BUILT
    ├── SavedSearchesView.java        # BUILT
    ├── SubscriptionsView.java        # BUILT (Phase 15)
    └── ScheduledTasksView.java       # BUILT (Phase 15) — admin task queue with detail dialog + failure reason
├── timetracking/                     # Time Clock module (Phases 21–25)
│   ├── entity/
│   │   ├── Attendance.java           # Phase 22 — clock-in/clock-out sessions
│   │   └── Holiday.java              # Phase 22 — Israeli public holidays lookup table
│   ├── repository/
│   │   ├── AttendanceRepository.java # Phase 22
│   │   └── HolidayRepository.java    # Phase 22
│   ├── service/
│   │   ├── AttendanceService.java    # Phase 23 — punchIn, punchOut, editSession, getMonthlyRecords
│   │   ├── IsraeliHolidayService.java # Phase 24 — KosherJava Hebrew→Gregorian, generateHolidaysForYear
│   │   ├── ExcelReportService.java   # Phase 25 — Apache POI .xlsx generation
│   │   └── ReportEmailService.java   # Phase 25 — MimeMessage attachment send
│   ├── scheduler/
│   │   ├── HolidayRecalculationJob.java # Phase 24 — cron Oct 1 02:00
│   │   └── MonthlyReportJob.java     # Phase 25 — cron 1st of month 06:00 Asia/Jerusalem
│   └── controller/
│       ├── AttendanceController.java # Phase 23 — punch-in/out REST endpoints
│       └── ReportAdminController.java # Phase 25 — manual trigger endpoint
```

---

## 5. Domain Model and Database Schema

### 5.1 Built Entities

#### `users` — `user_roles (user_id, role)`
`id`, `username` (UNIQUE), `email` (UNIQUE), `password` (BCrypt), `enabled`, `created_at`, `updated_at`

#### `trusted_devices`
`id`, `user_email` (indexed), `token_hash` VARCHAR(64) UNIQUE (SHA-256 of raw cookie token), `expires_at` (indexed), `created_at`
— Raw token stored only in `DEVICE_TRUST` HttpOnly cookie; only the hash is persisted. Cleaned up nightly at 03:00.

#### `accounts`
`id`, `name` (NOT NULL), `industry`, `website`, `phone`, `email` (UNIQUE), `address`, `type` (AccountType), `notes` (TEXT), `created_at`, `updated_at`

#### `contacts`
`id`, `first_name` (NOT NULL), `last_name` (NOT NULL), `email` (UNIQUE NOT NULL), `phone`, `job_title`, `department`, `status` (ContactStatus), `notes` (TEXT), `account_id` FK→accounts (nullable), `created_at`, `updated_at`

---

### 5.2 Entities To Build

#### `account_groups`
`id`, `name` (NOT NULL), `description` (TEXT), `parent_group_id` FK→account_groups (nullable)
Join table: `account_group_members (group_id, account_id)`

#### `addresses`
`id`, `type` (AddressType), `street`, `city`, `state`, `postal_code`, `country`, `enabled` (default true), `account_id` FK nullable, `contact_id` FK nullable

#### `activities`
`id`, `title` (NOT NULL), `description` (TEXT), `type` (ActivityType), `status` (ActivityStatus), `priority`, `due_date`, `resolved_at`, `assigned_to_id` FK→users, `account_id` FK nullable, `contact_id` FK nullable, `created_by_id` FK→users, `created_at`, `updated_at`

#### `leads`
`id`, `title` (NOT NULL), `first_name`, `last_name`, `email`, `phone`, `company`, `status` (LeadStatus), `source` (LeadSource), `estimated_value` DECIMAL(15,2), `currency` (3 chars), `close_date`, `contact_id` FK nullable, `account_id` FK nullable, `assigned_to_id` FK→users, `created_by_id` FK→users, `notes` (TEXT), `created_at`, `updated_at`

#### `opportunities`
`id`, `name` (NOT NULL), `stage` (OpportunityStage), `amount` DECIMAL(15,2), `currency`, `probability` INT (0–100), `close_date`, `lead_id` FK nullable, `account_id` FK→accounts, `contact_id` FK→contacts, `assigned_to_id` FK→users, `created_by_id` FK→users, `notes` (TEXT), `created_at`, `updated_at`

#### `quotes`
`id`, `quote_number` (UNIQUE, auto QT-YYYY-NNN), `title` (NOT NULL), `status` (QuoteStatus), `valid_until`, `total_amount` DECIMAL(15,2), `currency`, `opportunity_id` FK nullable, `account_id` FK→accounts, `contact_id` FK→contacts, `assigned_to_id` FK→users, `created_by_id` FK→users, `notes` (TEXT), `created_at`, `updated_at`

#### `quote_line_items`
`id`, `quote_id` FK→quotes, `product_name` (NOT NULL), `quantity` DECIMAL(10,2), `unit_price` DECIMAL(15,2), `discount_pct` DECIMAL(5,2), `line_total` DECIMAL(15,2), `sort_order` INT

#### `sales_orders`
`id`, `order_number` (UNIQUE, auto SO-YYYY-NNN), `status` (SalesOrderStatus), `order_date`, `delivery_date`, `total_amount` DECIMAL(15,2), `currency`, `quote_id` FK nullable, `account_id` FK→accounts, `contact_id` FK→contacts, `assigned_to_id` FK→users, `created_by_id` FK→users, `notes` (TEXT), `created_at`, `updated_at`

#### `sales_order_line_items` — same columns as quote_line_items with FK→sales_orders

#### `contracts`
`id`, `contract_number` (UNIQUE, auto CNT-YYYY-NNN), `title` (NOT NULL), `status` (ContractStatus), `start_date`, `end_date`, `total_value` DECIMAL(15,2), `currency`, `description` (TEXT), `terms` (TEXT), `sales_order_id` FK nullable, `account_id` FK→accounts, `contact_id` FK→contacts, `assigned_to_id` FK→users, `created_by_id` FK→users, `created_at`, `updated_at`

#### `saved_searches`
`id`, `name` (NOT NULL), `scope` (SavedSearchScope), `filter_json` (TEXT), `owner_id` FK→users (null = shared), `created_at`, `updated_at`

#### `workspaces`
`id`, `name` (NOT NULL), `description` (TEXT), `created_by_id` FK→users
Join table: `workspace_members (workspace_id, user_id)`

---

### 5.3 Relationships

```
Account 1──* Contact, Address, Lead, Opportunity, Quote, SalesOrder, Contract, Activity
Account *──* AccountGroup

Contact 1──* Address, Activity

Lead ──────→ Opportunity  (convert action)
Opportunity → Quote
Quote ──────→ SalesOrder  (convert when WON)
SalesOrder ─→ Contract    (convert when DELIVERED)

Quote 1──* QuoteLineItem
SalesOrder 1──* SalesOrderLineItem
```

---

## 6. Sales Pipeline Flow

```
[Lead]  NEW → CONTACTED → QUALIFIED → WON/LOST
         ↓ convert
[Opportunity]  PROSPECTING → QUALIFICATION → PROPOSAL → NEGOTIATION → WON/LOST
                ↓ generate quote
[Quote]  DRAFT → SENT → WON/LOST/EXPIRED  (has QuoteLineItems)
          ↓ convert when WON
[SalesOrder]  PENDING → CONFIRMED → DELIVERED/CANCELLED  (has SalesOrderLineItems)
               ↓ generate when DELIVERED
[Contract]  DRAFT → ACTIVE → EXPIRED/TERMINATED
```

**Business rules:**
- Lead convert → auto-creates Contact + Account if not already linked
- Opportunity WON requires at least one WON Quote
- SalesOrder line items cloned from Quote on conversion
- Contract inherits total_value from SalesOrder
- Forecast = `amount × (probability / 100)` for Opportunities

---

## 7. REST API Reference

All endpoints except `/api/v1/auth/**` require `Authorization: Bearer <JWT>`.

### Built

| Module | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| Accounts | `POST/GET /api/v1/accounts`, `GET/PUT/DELETE /api/v1/accounts/{id}`, `GET /api/v1/accounts/{id}/contacts` |
| Contacts | `POST/GET /api/v1/contacts`, `GET/PUT/DELETE /api/v1/contacts/{id}` |

### To Build

| Module | Key Endpoints |
|---|---|
| Account Groups | `POST/GET /api/v1/account-groups`, `GET/PUT/DELETE /{id}`, `POST/DELETE /{id}/members/{accountId}` |
| Addresses | `POST/GET /api/v1/addresses`, `GET/PUT/DELETE /{id}`, `GET /api/v1/accounts/{id}/addresses` |
| Activities | `POST/GET /api/v1/activities` (`?type=BUG&status=OPEN`), `PATCH /{id}/resolve`, `PATCH /{id}/close` |
| Leads | `POST/GET /api/v1/leads` (`?status=WON`), `POST /{id}/convert` |
| Opportunities | `POST/GET /api/v1/opportunities` (`?stage=WON`), `GET /{id}/quotes` |
| Quotes | `POST/GET /api/v1/quotes` (`?status=WON`), `POST/PUT/DELETE /{id}/line-items`, `POST /{id}/convert-to-order` |
| Sales Orders | `POST/GET /api/v1/sales-orders`, `POST/PUT/DELETE /{id}/line-items`, `POST /{id}/convert-to-contract` |
| Contracts | `POST/GET /api/v1/contracts`, `GET/PUT/DELETE /{id}` |
| Forecast | `GET /api/v1/forecast/leads`, `/opportunities`, `/quotes` |
| Workspaces | `POST/GET /api/v1/workspaces`, `POST/DELETE /{id}/members/{userId}` |

---

## 8. Vaadin UI Views

Extend `MainLayout.createDrawer()` with grouped `SideNavItem` sections:

```
Dashboard
Contacts  ── Accounts | Account Groups | Contacts | Addresses
Support   ── Activities (Bugs + Features)
Sales     ── Leads | Opportunities | Quotes | Sales Orders | Contracts | Forecast
Settings  ── Workspaces | Saved Searches | Users (ADMIN)
```

---

## 9. Step-by-Step Implementation Guide

Each phase follows this order: **Enums → Entity → Repository → DTOs → Service → Controller → Vaadin View → Navigation**.

All new code must follow the existing patterns:
- **Entity**: `@Entity @Table @EntityListeners(AuditingEntityListener.class)`, manual getters/setters (no Lombok), `@Enumerated(EnumType.STRING)`, `@ManyToOne(fetch = LAZY)`, `@CreatedDate @Column(updatable=false)`
- **DTO**: Java `record` for Request and Response; Response has `static XxxResponse from(Xxx e)`
- **Repository**: `extends JpaRepository<T, Long>, JpaSpecificationExecutor<T>`
- **Service**: `@Service @Transactional`, constructor injection, `private getOrThrow(Long id)`, `private mapToEntity()`
- **Controller**: `@RestController @RequestMapping("/api/v1/...")`, `ResponseEntity<T>`
- **View**: `@Route(value="...", layout=MainLayout.class) @PageTitle("... | CRM") @PermitAll extends VerticalLayout`

---

### Phase 1 — Foundation ✅ COMPLETE (built)

- User authentication (JWT + Vaadin session)
- Account CRUD: entity, repo, DTOs, service, controller, `AccountsView`
- Contact CRUD: entity, repo, DTOs, service, controller, `ContactsView`
- Global exception handler, H2 dev / PostgreSQL prod profiles
- Swagger at `/swagger-ui.html`, admin user seeded by `DataInitializer`

---

### Phase 2 — Contacts Module Completion ✅ COMPLETE (built)

Adds: AccountGroup (with member management), Address (structured), SavedSearch. Enables the Contacts nav section.

#### BE Step 2.1 — Enums

**`AddressType.java`** — `HOME, WORK, BILLING, SHIPPING, OTHER`

**`SavedSearchScope.java`** — `ACCOUNT, CONTACT, ADDRESS, LEAD, OPPORTUNITY, ACTIVITY`

#### BE Step 2.2 — AccountGroup Entity

**`domain/entity/AccountGroup.java`**
```
@Entity @Table("account_groups")
Fields:
  Long id
  @Column(nullable=false) String name
  @Column(columnDefinition="TEXT") String description
  @ManyToOne(fetch=LAZY) @JoinColumn("parent_group_id") AccountGroup parent  // nullable
  @ManyToMany @JoinTable(name="account_group_members",
    joinColumns="group_id", inverseJoinColumns="account_id")
  List<Account> members = new ArrayList<>()
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 2.3 — Address Entity

**`domain/entity/Address.java`**
```
@Entity @Table("addresses")
Fields:
  Long id
  @Enumerated(EnumType.STRING) AddressType type
  String street, city, state, postalCode, country
  boolean enabled = true
  @ManyToOne(fetch=LAZY) @JoinColumn("account_id") Account account  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("contact_id") Contact contact  // nullable
  @CreatedDate LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

Also add to **`Account.java`**:
```java
@OneToMany(mappedBy="account", cascade=ALL, fetch=LAZY, orphanRemoval=true)
List<Address> addresses = new ArrayList<>();
```

Also add to **`Contact.java`**:
```java
@OneToMany(mappedBy="contact", cascade=ALL, fetch=LAZY, orphanRemoval=true)
List<Address> addresses = new ArrayList<>();
```

#### BE Step 2.4 — SavedSearch Entity

**`domain/entity/SavedSearch.java`**
```
@Entity @Table("saved_searches")
Fields:
  Long id
  @Column(nullable=false) String name
  @Enumerated(EnumType.STRING) SavedSearchScope scope
  @Column(columnDefinition="TEXT") String filterJson
  @ManyToOne(fetch=LAZY) @JoinColumn("owner_id") User owner  // nullable = shared
  @CreatedDate LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 2.5 — Repositories

```java
// AccountGroupRepository extends JpaRepository<AccountGroup, Long>
// Methods: findByNameContainingIgnoreCase(String name)

// AddressRepository extends JpaRepository<Address, Long>
// Methods: findByAccount_Id(Long accountId), findByContact_Id(Long contactId), findByEnabled(boolean)

// SavedSearchRepository extends JpaRepository<SavedSearch, Long>
// Methods: findByScope(SavedSearchScope scope), findByOwner_IdOrOwnerIsNull(Long userId)
```

#### BE Step 2.6 — Request/Response DTOs

**`AccountGroupRequest`** record: `@NotBlank String name`, `String description`, `Long parentId`

**`AccountGroupResponse`** record: `Long id`, `String name`, `String description`, `Long parentId`, `int memberCount`, `LocalDateTime createdAt`
— `from(AccountGroup g)`: `new AccountGroupResponse(g.getId(), g.getName(), g.getDescription(), g.getParent() != null ? g.getParent().getId() : null, g.getMembers().size(), g.getCreatedAt())`

**`AddressRequest`** record: `AddressType type`, `String street`, `String city`, `String state`, `String postalCode`, `String country`, `Long accountId`, `Long contactId`

**`AddressResponse`** record: `Long id`, `AddressType type`, `String street`, `String city`, `String state`, `String postalCode`, `String country`, `boolean enabled`
— `from(Address a)`

**`SavedSearchRequest`** record: `@NotBlank String name`, `SavedSearchScope scope`, `String filterJson`

**`SavedSearchResponse`** record: `Long id`, `String name`, `SavedSearchScope scope`, `String filterJson`

#### BE Step 2.7 — Services

**`AccountGroupService`** `@Service @Transactional`:
- `AccountGroupResponse create(AccountGroupRequest)`
- `AccountGroupResponse findById(Long id)` `@Transactional(readOnly=true)`
- `Page<AccountGroupResponse> findAll(Pageable)` `@Transactional(readOnly=true)`
- `AccountGroupResponse update(Long id, AccountGroupRequest)`
- `void delete(Long id)`
- `AccountGroupResponse addMember(Long groupId, Long accountId)`
- `AccountGroupResponse removeMember(Long groupId, Long accountId)`

**`AddressService`** `@Service @Transactional`:
- `AddressResponse create(AddressRequest)`
- `AddressResponse findById(Long)` `@Transactional(readOnly=true)`
- `List<AddressResponse> findByAccount(Long accountId)` `@Transactional(readOnly=true)`
- `List<AddressResponse> findByContact(Long contactId)` `@Transactional(readOnly=true)`
- `AddressResponse update(Long id, AddressRequest)`
- `void delete(Long id)`
- `AddressResponse toggleEnabled(Long id)`

#### BE Step 2.8 — Controllers

**`AccountGroupController`** `@RestController @RequestMapping("/api/v1/account-groups")`:
```
POST   /                           → create
GET    /                           → list (paginated)
GET    /{id}                       → findById
PUT    /{id}                       → update
DELETE /{id}                       → delete (204)
POST   /{id}/members/{accountId}   → addMember
DELETE /{id}/members/{accountId}   → removeMember
```

**`AddressController`** `@RestController @RequestMapping("/api/v1/addresses")`:
```
POST   /              → create
GET    /              → list (?accountId= or ?contactId=)
GET    /{id}          → findById
PUT    /{id}          → update
DELETE /{id}          → delete (204)
PATCH  /{id}/toggle   → toggleEnabled
```

Also add to **`AccountController`**: `GET /{id}/addresses → addressService.findByAccount(id)`
Also add to **`ContactController`**: `GET /{id}/addresses → addressService.findByContact(id)`

#### FE Step 2.9 — AccountGroupsView

**`ui/AccountGroupsView.java`**
```
@Route("account-groups") @PageTitle("Account Groups | CRM") @PermitAll
extends VerticalLayout
Grid<AccountGroupResponse> grid = new Grid<>(AccountGroupResponse.class, false)
Columns: name (sortable, flex 2), memberCount ("Members"), createdAt ("Created"), Actions
Toolbar: TextField searchField (ValueChangeMode.LAZY) + "New Group" Button
Dialog fields: TextField name, TextField description, ComboBox<AccountGroupResponse> parentGroup
Action buttons: Edit (LUMO_TERTIARY), Delete (LUMO_ERROR)
```

#### FE Step 2.10 — AddressesView

**`ui/AddressesView.java`**
```
@Route("addresses") @PageTitle("Addresses | CRM") @PermitAll
extends VerticalLayout
Grid<AddressResponse> grid = new Grid<>(AddressResponse.class, false)
Columns: type, street, city, state, country, enabled (badge), Actions
Toolbar: ComboBox<AccountResponse> filterByAccount + ComboBox<ContactResponse> filterByContact + "New Address" Button
Dialog fields: ComboBox<AddressType> type, TextField street/city/state/postalCode/country
               ComboBox<AccountResponse> account, ComboBox<ContactResponse> contact (mutually exclusive)
```

#### FE Step 2.11 — Navigation

Edit **`MainLayout.createDrawer()`** — replace flat list with grouped sections:

```java
SideNav nav = new SideNav();

// Dashboard (keep existing)
nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));

// Contacts section
SideNavItem contacts = new SideNavItem("Contacts");
contacts.setPrefixComponent(VaadinIcon.USERS.create());
contacts.addItem(new SideNavItem("Accounts", AccountsView.class, VaadinIcon.BUILDING.create()));
contacts.addItem(new SideNavItem("Account Groups", AccountGroupsView.class, VaadinIcon.FOLDER.create()));
contacts.addItem(new SideNavItem("Contacts", ContactsView.class, VaadinIcon.USER.create()));
contacts.addItem(new SideNavItem("Addresses", AddressesView.class, VaadinIcon.MAP_MARKER.create()));
nav.addItem(contacts);

addToDrawer(nav);
```

#### Verification Phase 2

1. `./mvnw spring-boot:run`
2. Open http://localhost:9080, log in as `admin / admin123`
3. Navigate to Contacts → Account Groups: create a group, add an account as member
4. Navigate to Addresses: create an address linked to an account
5. Check H2 console: tables `account_groups`, `account_group_members`, `addresses` exist with data

---

### Phase 3 — Support / Activity Module ✅ COMPLETE (built)

Adds Activity entity (Bug/Feature/Task/Meeting/Call tracking). Enables the Support nav section.

#### BE Step 3.1 — Enums

**`ActivityType.java`** — `BUG, FEATURE, TASK, MEETING, CALL`

**`ActivityStatus.java`** — `OPEN, IN_PROGRESS, RESOLVED, CLOSED`

**`ActivityPriority.java`** — `LOW, MEDIUM, HIGH, CRITICAL`

#### BE Step 3.2 — Activity Entity

**`domain/entity/Activity.java`**
```
@Entity @Table("activities")
Fields:
  Long id
  @Column(nullable=false) String title
  @Column(columnDefinition="TEXT") String description
  @Enumerated(EnumType.STRING) ActivityType type
  @Enumerated(EnumType.STRING) ActivityStatus status = ActivityStatus.OPEN
  @Enumerated(EnumType.STRING) ActivityPriority priority = ActivityPriority.MEDIUM
  LocalDate dueDate
  LocalDateTime resolvedAt  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("assigned_to_id") User assignedTo
  @ManyToOne(fetch=LAZY) @JoinColumn("account_id") Account account  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("contact_id") Contact contact  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("created_by_id") User createdBy
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 3.3 — Repository

```java
// ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity>
// Methods:
//   findByType(ActivityType type)
//   findByStatus(ActivityStatus status)
//   findByAssignedTo_Id(Long userId)
//   findByAccount_Id(Long accountId)
//   findByContact_Id(Long contactId)
//   countByStatus(ActivityStatus status)
```

#### BE Step 3.4 — DTOs

**`ActivityRequest`** record: `@NotBlank String title`, `String description`, `ActivityType type`, `ActivityStatus status`, `ActivityPriority priority`, `LocalDate dueDate`, `Long assignedToId`, `Long accountId`, `Long contactId`

**`ActivityResponse`** record: `Long id`, `String title`, `String description`, `ActivityType type`, `ActivityStatus status`, `ActivityPriority priority`, `LocalDate dueDate`, `LocalDateTime resolvedAt`, `String assignedToName`, `Long accountId`, `String accountName`, `Long contactId`, `String contactName`, `LocalDateTime createdAt`
— `from(Activity a)`: map all fields, use `a.getAssignedTo() != null ? a.getAssignedTo().getUsername() : null`

#### BE Step 3.5 — Service

**`ActivityService`** `@Service @Transactional`:
- `ActivityResponse create(ActivityRequest, String createdByUsername)`
- `ActivityResponse findById(Long)` `@Transactional(readOnly=true)`
- `Page<ActivityResponse> findAll(Pageable, ActivityType type, ActivityStatus status)` `@Transactional(readOnly=true)`
- `ActivityResponse update(Long id, ActivityRequest)`
- `void delete(Long id)`
- `ActivityResponse resolve(Long id)` — sets `status = RESOLVED`, `resolvedAt = now()`
- `ActivityResponse close(Long id)` — sets `status = CLOSED`

#### BE Step 3.6 — Controller

**`ActivityController`** `@RestController @RequestMapping("/api/v1/activities")`:
```
POST   /              → create (get authenticated username from SecurityContext)
GET    /              → list (?type=BUG&status=OPEN&accountId=)
GET    /{id}          → findById
PUT    /{id}          → update
DELETE /{id}          → delete (204)
PATCH  /{id}/resolve  → resolve
PATCH  /{id}/close    → close
```

#### FE Step 3.7 — ActivitiesView

**`ui/ActivitiesView.java`**
```
@Route("activities") @PageTitle("Activities | CRM") @PermitAll
extends VerticalLayout
Grid<ActivityResponse> grid = new Grid<>(ActivityResponse.class, false)
Columns: title (flex 2, sortable), type (badge), status (badge), priority, assignedToName, dueDate, Actions
Toolbar:
  ComboBox<ActivityType> typeFilter ("All Types")
  ComboBox<ActivityStatus> statusFilter ("All Statuses")
  TextField searchField (ValueChangeMode.LAZY, search title)
  "New Activity" Button (LUMO_PRIMARY)
Action buttons per row:
  Resolve (VaadinIcon.CHECK, LUMO_SUCCESS) — hidden if status is RESOLVED/CLOSED
  Edit (VaadinIcon.EDIT, LUMO_TERTIARY)
  Delete (VaadinIcon.TRASH, LUMO_ERROR)
Dialog fields: TextField title, TextArea description,
               ComboBox<ActivityType> type, ComboBox<ActivityStatus> status,
               ComboBox<ActivityPriority> priority, DatePicker dueDate,
               ComboBox<String> assignedTo (load usernames from UserService),
               ComboBox<AccountResponse> account, ComboBox<ContactResponse> contact
```

#### FE Step 3.8 — DashboardView update

Add stat card: `countByStatus(ActivityStatus.OPEN)` → "Open Activities"

#### FE Step 3.9 — Navigation

Add Support section to `MainLayout.createDrawer()`:
```java
SideNavItem support = new SideNavItem("Support");
support.setPrefixComponent(VaadinIcon.LIFEBUOY.create());
support.addItem(new SideNavItem("Activities", ActivitiesView.class, VaadinIcon.BUG.create()));
nav.addItem(support);
```

#### Verification Phase 3

1. Open http://localhost:9080/activities
2. Create a BUG activity, assign to admin, link to an account
3. Click Resolve — confirm `resolvedAt` is set and status changes to RESOLVED
4. Filter by `type = BUG` and `status = OPEN` — confirm only open bugs show

---

### Phase 4 — Sales: Leads ✅ COMPLETE (built)

Adds Lead entity with conversion to Opportunity. Enables Sales → Leads nav item.

#### BE Step 4.1 — Enums

**`LeadStatus.java`** — `NEW, CONTACTED, QUALIFIED, WON, LOST`

**`LeadSource.java`** — `WEB, REFERRAL, COLD_CALL, EVENT, TRADE_SHOW, OTHER`

#### BE Step 4.2 — Lead Entity

**`domain/entity/Lead.java`**
```
@Entity @Table("leads")
Fields:
  Long id
  @Column(nullable=false) String title
  String firstName, lastName, email, phone, company
  @Enumerated(EnumType.STRING) LeadStatus status = LeadStatus.NEW
  @Enumerated(EnumType.STRING) LeadSource source
  @Column(precision=15, scale=2) BigDecimal estimatedValue
  @Column(length=3) String currency = "USD"
  LocalDate closeDate
  @Column(columnDefinition="TEXT") String notes
  @ManyToOne(fetch=LAZY) @JoinColumn("contact_id") Contact contact  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("account_id") Account account  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("assigned_to_id") User assignedTo
  @ManyToOne(fetch=LAZY) @JoinColumn("created_by_id") User createdBy
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 4.3 — Repository

```java
// LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead>
// Methods:
//   findByStatus(LeadStatus status)
//   findByAssignedTo_Id(Long userId)
//   findByAccount_Id(Long accountId)
//   countByStatus(LeadStatus status)
//   List<Lead> findByStatusNot(LeadStatus status) // for forecast excluding LOST
```

#### BE Step 4.4 — DTOs

**`LeadRequest`** record: `@NotBlank String title`, `String firstName`, `String lastName`, `@Email String email`, `String phone`, `String company`, `LeadStatus status`, `LeadSource source`, `BigDecimal estimatedValue`, `String currency`, `LocalDate closeDate`, `String notes`, `Long assignedToId`, `Long accountId`, `Long contactId`

**`LeadResponse`** record: `Long id`, `String title`, `String firstName`, `String lastName`, `String email`, `String phone`, `String company`, `LeadStatus status`, `LeadSource source`, `BigDecimal estimatedValue`, `String currency`, `LocalDate closeDate`, `String assignedToName`, `Long accountId`, `String accountName`, `LocalDateTime createdAt`

#### BE Step 4.5 — Service

**`LeadService`** `@Service @Transactional`:
- `LeadResponse create(LeadRequest, String createdByUsername)`
- `LeadResponse findById(Long)` `@Transactional(readOnly=true)`
- `Page<LeadResponse> findAll(Pageable, LeadStatus status)` `@Transactional(readOnly=true)`
- `LeadResponse update(Long, LeadRequest)`
- `void delete(Long)`
- `OpportunityResponse convert(Long leadId)` — creates Opportunity (stage=PROSPECTING), links Lead, creates Contact + Account if not set

#### BE Step 4.6 — Controller

**`LeadController`** `@RestController @RequestMapping("/api/v1/leads")`:
```
POST   /              → create
GET    /              → list (?status=WON for won leads)
GET    /{id}          → findById
PUT    /{id}          → update
DELETE /{id}          → delete (204)
POST   /{id}/convert  → convert to Opportunity (returns OpportunityResponse)
```

#### FE Step 4.7 — LeadsView

**`ui/LeadsView.java`**
```
@Route("leads") @PageTitle("Leads | CRM") @PermitAll
extends VerticalLayout
Grid<LeadResponse> grid = new Grid<>(LeadResponse.class, false)
Columns: title (flex 2), company, status (badge with color), estimatedValue, source, closeDate, assignedToName, Actions
Toolbar:
  ComboBox<LeadStatus> statusFilter (empty = all)
  TextField searchField (search title/company, ValueChangeMode.LAZY)
  "New Lead" Button (LUMO_PRIMARY)
Action buttons per row:
  Convert (VaadinIcon.ARROW_FORWARD) — shown only when status = QUALIFIED
  Edit (VaadinIcon.EDIT)
  Delete (VaadinIcon.TRASH, LUMO_ERROR)
Convert action: calls leadService.convert(id), shows "Lead converted to Opportunity" notification, refreshes grid
Dialog fields: TextField title, firstName, lastName, email, phone, company,
               ComboBox<LeadStatus> status, ComboBox<LeadSource> source,
               NumberField estimatedValue, TextField currency, DatePicker closeDate,
               TextArea notes, ComboBox<String> assignedTo
Status badge colors: NEW=contrast, CONTACTED=primary, QUALIFIED=success, WON=success, LOST=error
```

#### FE Step 4.8 — DashboardView update

Add stat card: `leadService.countByStatus(LeadStatus.NEW)` → "New Leads"

#### FE Step 4.9 — Navigation

Add Leads under Sales section in `MainLayout.createDrawer()`:
```java
SideNavItem sales = new SideNavItem("Sales");
sales.setPrefixComponent(VaadinIcon.TRENDING_UP.create());
sales.addItem(new SideNavItem("Leads", LeadsView.class, VaadinIcon.CONNECT.create()));
nav.addItem(sales);
```

#### Verification Phase 4

1. Create a Lead with status NEW
2. Change status to QUALIFIED, click Convert — confirm Opportunity is created
3. Filter `?status=WON` — confirm won leads list shows correctly
4. Check Swagger at `/swagger-ui.html` for `POST /api/v1/leads/{id}/convert`

---

### Phase 5 — Sales: Opportunities ✅ COMPLETE (built)

Adds Opportunity entity with stage tracking. Extends Sales nav section.

#### BE Step 5.1 — Enum

**`OpportunityStage.java`** — `PROSPECTING, QUALIFICATION, PROPOSAL, NEGOTIATION, WON, LOST`

#### BE Step 5.2 — Opportunity Entity

**`domain/entity/Opportunity.java`**
```
@Entity @Table("opportunities")
Fields:
  Long id
  @Column(nullable=false) String name
  @Enumerated(EnumType.STRING) OpportunityStage stage = OpportunityStage.PROSPECTING
  @Column(precision=15, scale=2) BigDecimal amount
  @Column(length=3) String currency = "USD"
  Integer probability = 10  // percent
  LocalDate closeDate
  @Column(columnDefinition="TEXT") String notes
  @ManyToOne(fetch=LAZY) @JoinColumn("lead_id") Lead lead  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("account_id") Account account
  @ManyToOne(fetch=LAZY) @JoinColumn("contact_id") Contact contact
  @ManyToOne(fetch=LAZY) @JoinColumn("assigned_to_id") User assignedTo
  @ManyToOne(fetch=LAZY) @JoinColumn("created_by_id") User createdBy
  @OneToMany(mappedBy="opportunity", cascade=ALL, fetch=LAZY, orphanRemoval=true)
  List<Quote> quotes = new ArrayList<>()
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

Also add on **`Lead.java`**:
```java
@OneToOne(mappedBy="lead", fetch=FetchType.LAZY)
private Opportunity opportunity;
```

#### BE Step 5.3 — Repository

```java
// OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity>
// Methods:
//   findByStage(OpportunityStage stage)
//   findByAssignedTo_Id(Long userId)
//   findByAccount_Id(Long accountId)
//   findByStageNot(OpportunityStage stage)  // for forecast excluding LOST
//   countByStage(OpportunityStage stage)
//   @Query("SELECT SUM(o.amount) FROM Opportunity o WHERE o.stage != 'LOST'")
//   BigDecimal sumPipelineAmount()
```

#### BE Step 5.4 — DTOs

**`OpportunityRequest`** record: `@NotBlank String name`, `OpportunityStage stage`, `BigDecimal amount`, `String currency`, `Integer probability`, `LocalDate closeDate`, `String notes`, `Long leadId`, `Long accountId`, `Long contactId`, `Long assignedToId`

**`OpportunityResponse`** record: `Long id`, `String name`, `OpportunityStage stage`, `BigDecimal amount`, `String currency`, `Integer probability`, `LocalDate closeDate`, `String assignedToName`, `Long accountId`, `String accountName`, `Long contactId`, `String contactName`, `BigDecimal weightedAmount`, `LocalDateTime createdAt`
— `weightedAmount`: `amount != null ? amount.multiply(BigDecimal.valueOf(probability / 100.0)) : null`

#### BE Step 5.5 — Service

**`OpportunityService`** `@Service @Transactional`:
- `OpportunityResponse create(OpportunityRequest, String createdByUsername)`
- `OpportunityResponse findById(Long)` `@Transactional(readOnly=true)`
- `Page<OpportunityResponse> findAll(Pageable, OpportunityStage stage)` `@Transactional(readOnly=true)`
- `List<QuoteResponse> findQuotes(Long opportunityId)` `@Transactional(readOnly=true)`
- `OpportunityResponse update(Long, OpportunityRequest)`
- `void delete(Long)`

#### BE Step 5.6 — Controller

**`OpportunityController`** `@RestController @RequestMapping("/api/v1/opportunities")`:
```
POST   /          → create
GET    /          → list (?stage=WON)
GET    /{id}      → findById
PUT    /{id}      → update
DELETE /{id}      → delete (204)
GET    /{id}/quotes → findQuotes
```

#### FE Step 5.7 — OpportunitiesView

**`ui/OpportunitiesView.java`**
```
@Route("opportunities") @PageTitle("Opportunities | CRM") @PermitAll
extends VerticalLayout
Grid<OpportunityResponse> grid = new Grid<>(OpportunityResponse.class, false)
Columns: name (flex 2), accountName, stage (badge), amount (formatted currency), probability ("+%"),
         weightedAmount, closeDate, assignedToName, Actions
Toolbar:
  ComboBox<OpportunityStage> stageFilter
  TextField searchField (ValueChangeMode.LAZY)
  "New Opportunity" Button (LUMO_PRIMARY)
Stage badge colors: WON=success, LOST=error, NEGOTIATION=primary, others=contrast
Dialog fields: TextField name, ComboBox<OpportunityStage> stage,
               NumberField amount, TextField currency, IntegerField probability (0–100),
               DatePicker closeDate, ComboBox<AccountResponse> account,
               ComboBox<ContactResponse> contact, ComboBox<String> assignedTo, TextArea notes
```

#### FE Step 5.8 — DashboardView update

Add stat card: `opportunityService.sumPipelineAmount()` → "Pipeline Value"

#### FE Step 5.9 — Navigation

Add Opportunities to Sales section:
```java
sales.addItem(new SideNavItem("Opportunities", OpportunitiesView.class, VaadinIcon.DOLLAR.create()));
```

#### Verification Phase 5

1. Create an Opportunity linked to an existing Account
2. Set stage to PROPOSAL, confirm probability updates in grid
3. Convert a Lead (Phase 4) — confirm Opportunity is created and linked
4. Filter by `stage=WON` — confirm won opportunities show

---

### Phase 6 — Sales: Quotes ✅ COMPLETE (built)

Adds Quote with line items. Quote number auto-generation. Convert to Sales Order. Extends Sales nav.

#### BE Step 6.1 — Enum

**`QuoteStatus.java`** — `DRAFT, SENT, WON, LOST, EXPIRED`

#### BE Step 6.2 — Quote + QuoteLineItem Entities

**`domain/entity/Quote.java`**
```
@Entity @Table("quotes")
Fields:
  Long id
  @Column(unique=true, nullable=false) String quoteNumber  // set in @PrePersist
  @Column(nullable=false) String title
  @Enumerated(EnumType.STRING) QuoteStatus status = QuoteStatus.DRAFT
  LocalDate validUntil
  @Column(precision=15, scale=2) BigDecimal totalAmount = BigDecimal.ZERO
  @Column(length=3) String currency = "USD"
  @Column(columnDefinition="TEXT") String notes
  @ManyToOne(fetch=LAZY) @JoinColumn("opportunity_id") Opportunity opportunity  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("account_id") Account account
  @ManyToOne(fetch=LAZY) @JoinColumn("contact_id") Contact contact
  @ManyToOne(fetch=LAZY) @JoinColumn("assigned_to_id") User assignedTo
  @ManyToOne(fetch=LAZY) @JoinColumn("created_by_id") User createdBy
  @OneToMany(mappedBy="quote", cascade=ALL, fetch=LAZY, orphanRemoval=true)
  List<QuoteLineItem> lineItems = new ArrayList<>()
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt

  @PrePersist
  void generateNumber() {
    if (quoteNumber == null) {
      quoteNumber = "QT-" + LocalDate.now().getYear() + "-" +
          String.format("%03d", new Random().nextInt(900) + 100);
    }
  }

  void recalculateTotal() {
    totalAmount = lineItems.stream()
        .map(QuoteLineItem::getLineTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
```

**`domain/entity/QuoteLineItem.java`**
```
@Entity @Table("quote_line_items")
Fields:
  Long id
  @ManyToOne(fetch=LAZY) @JoinColumn("quote_id") Quote quote
  @Column(nullable=false) String productName
  @Column(precision=10, scale=2) BigDecimal quantity
  @Column(precision=15, scale=2) BigDecimal unitPrice
  @Column(precision=5, scale=2) BigDecimal discountPct = BigDecimal.ZERO
  @Column(precision=15, scale=2) BigDecimal lineTotal
  Integer sortOrder = 0

  @PrePersist @PreUpdate
  void calcLineTotal() {
    if (quantity != null && unitPrice != null) {
      BigDecimal disc = discountPct != null ? discountPct : BigDecimal.ZERO;
      lineTotal = quantity.multiply(unitPrice)
          .multiply(BigDecimal.ONE.subtract(disc.divide(BigDecimal.valueOf(100))));
    }
  }
```

#### BE Step 6.3 — Repository

```java
// QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote>
// Methods:
//   findByStatus(QuoteStatus status)
//   findByOpportunity_Id(Long opportunityId)
//   findByAccount_Id(Long accountId)
//   findByQuoteNumber(String number)
//   countByStatus(QuoteStatus status)
```

#### BE Step 6.4 — DTOs

**`QuoteLineItemRequest`** record: `@NotBlank String productName`, `BigDecimal quantity`, `BigDecimal unitPrice`, `BigDecimal discountPct`, `Integer sortOrder`

**`QuoteLineItemResponse`** record: `Long id`, `String productName`, `BigDecimal quantity`, `BigDecimal unitPrice`, `BigDecimal discountPct`, `BigDecimal lineTotal`, `Integer sortOrder`

**`QuoteRequest`** record: `@NotBlank String title`, `QuoteStatus status`, `LocalDate validUntil`, `String currency`, `String notes`, `Long opportunityId`, `Long accountId`, `Long contactId`, `Long assignedToId`, `List<QuoteLineItemRequest> lineItems`

**`QuoteResponse`** record: `Long id`, `String quoteNumber`, `String title`, `QuoteStatus status`, `LocalDate validUntil`, `BigDecimal totalAmount`, `String currency`, `String accountName`, `Long accountId`, `List<QuoteLineItemResponse> lineItems`, `LocalDateTime createdAt`

#### BE Step 6.5 — Service

**`QuoteService`** `@Service @Transactional`:
- `QuoteResponse create(QuoteRequest, String createdByUsername)` — saves Quote with line items, calls `recalculateTotal()`
- `QuoteResponse findById(Long)` `@Transactional(readOnly=true)`
- `Page<QuoteResponse> findAll(Pageable, QuoteStatus status)` `@Transactional(readOnly=true)`
- `QuoteResponse update(Long, QuoteRequest)` — clears and replaces line items, recalculates
- `void delete(Long)`
- `QuoteResponse addLineItem(Long quoteId, QuoteLineItemRequest)` — appends line item, recalculates
- `QuoteResponse removeLineItem(Long quoteId, Long lineItemId)` — removes item, recalculates
- `SalesOrderResponse convertToOrder(Long quoteId)` — requires `status = WON`, creates SalesOrder, clones line items

#### BE Step 6.6 — Controller

**`QuoteController`** `@RestController @RequestMapping("/api/v1/quotes")`:
```
POST   /                            → create
GET    /                            → list (?status=WON)
GET    /{id}                        → findById (includes lineItems)
PUT    /{id}                        → update
DELETE /{id}                        → delete (204)
POST   /{id}/line-items             → addLineItem
DELETE /{id}/line-items/{lineId}    → removeLineItem
POST   /{id}/convert-to-order       → convertToOrder (returns SalesOrderResponse)
```

#### FE Step 6.7 — QuotesView

**`ui/QuotesView.java`**
```
@Route("quotes") @PageTitle("Quotes | CRM") @PermitAll
extends VerticalLayout — two-panel layout (master/detail)
Top panel: Grid<QuoteResponse>
  Columns: quoteNumber, title, accountName, status (badge), totalAmount, validUntil, Actions
Toolbar: ComboBox<QuoteStatus> statusFilter, "New Quote" Button
Bottom panel (shown when row selected): H4 "Line Items" + Grid<QuoteLineItemResponse>
  Line item columns: productName, quantity, unitPrice, discountPct (%), lineTotal, Delete button
  "Add Line Item" button → inline dialog: TextField productName, NumberField qty/unitPrice/discountPct
Action buttons per quote row:
  Convert to Order (VaadinIcon.CART, LUMO_SUCCESS) — shown only when status = WON
  Edit (VaadinIcon.EDIT)
  Delete (VaadinIcon.TRASH, LUMO_ERROR)
Dialog fields: TextField title, ComboBox<QuoteStatus> status, DatePicker validUntil,
               TextField currency, ComboBox<AccountResponse> account, ComboBox<ContactResponse> contact,
               ComboBox<OpportunityResponse> opportunity (optional), TextArea notes
```

#### FE Step 6.8 — Navigation

```java
sales.addItem(new SideNavItem("Quotes", QuotesView.class, VaadinIcon.INVOICE.create()));
```

#### Verification Phase 6

1. Create a Quote with 2 line items — confirm `totalAmount` auto-calculated
2. Mark quote WON, click Convert to Order — confirm SalesOrder is created
3. Test `/api/v1/quotes/{id}/convert-to-order` returns 400 if status != WON

---

### Phase 7 — Sales: Orders + Contracts ✅ COMPLETE (built)

Adds SalesOrder (with line items) and Contract. Extends Sales nav.

#### BE Step 7.1 — Enums

**`SalesOrderStatus.java`** — `PENDING, CONFIRMED, DELIVERED, CANCELLED`

**`ContractStatus.java`** — `DRAFT, ACTIVE, EXPIRED, TERMINATED`

#### BE Step 7.2 — SalesOrder + SalesOrderLineItem Entities

**`domain/entity/SalesOrder.java`** — mirrors Quote.java pattern:
```
quoteNumber → orderNumber ("SO-YYYY-NNN" via @PrePersist)
QuoteStatus → SalesOrderStatus (default PENDING)
opportunity_id → quote_id FK nullable
Has: List<SalesOrderLineItem> lineItems, same recalculateTotal() method
```

**`domain/entity/SalesOrderLineItem.java`** — identical to QuoteLineItem but FK→SalesOrder

Add on **`Quote.java`**:
```java
@OneToOne(mappedBy="quote", fetch=FetchType.LAZY)
private SalesOrder salesOrder;
```

#### BE Step 7.3 — Contract Entity

**`domain/entity/Contract.java`**
```
@Entity @Table("contracts")
Fields:
  Long id
  @Column(unique=true, nullable=false) String contractNumber  // "CNT-YYYY-NNN" @PrePersist
  @Column(nullable=false) String title
  @Enumerated(EnumType.STRING) ContractStatus status = ContractStatus.DRAFT
  LocalDate startDate, endDate
  @Column(precision=15, scale=2) BigDecimal totalValue
  @Column(length=3) String currency = "USD"
  @Column(columnDefinition="TEXT") String description
  @Column(columnDefinition="TEXT") String terms
  @ManyToOne(fetch=LAZY) @JoinColumn("sales_order_id") SalesOrder salesOrder  // nullable
  @ManyToOne(fetch=LAZY) @JoinColumn("account_id") Account account
  @ManyToOne(fetch=LAZY) @JoinColumn("contact_id") Contact contact
  @ManyToOne(fetch=LAZY) @JoinColumn("assigned_to_id") User assignedTo
  @ManyToOne(fetch=LAZY) @JoinColumn("created_by_id") User createdBy
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 7.4 — Repositories

```java
// SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder>
// Methods: findByStatus, findByQuote_Id, findByAccount_Id

// ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract>
// Methods: findByStatus, findBySalesOrder_Id, findByAccount_Id,
//          findByEndDateBefore(LocalDate date)  // for expiry alerts
```

#### BE Step 7.5 — DTOs

Follow same pattern as QuoteRequest/Response for SalesOrder (swap opportunityId → quoteId, QuoteLineItemRequest → SalesOrderLineItemRequest).

**`ContractRequest`** record: `@NotBlank String title`, `ContractStatus status`, `LocalDate startDate`, `LocalDate endDate`, `BigDecimal totalValue`, `String currency`, `String description`, `String terms`, `Long salesOrderId`, `Long accountId`, `Long contactId`, `Long assignedToId`

**`ContractResponse`** record: `Long id`, `String contractNumber`, `String title`, `ContractStatus status`, `LocalDate startDate`, `LocalDate endDate`, `BigDecimal totalValue`, `String currency`, `String accountName`, `Long accountId`, `LocalDateTime createdAt`

#### BE Step 7.6 — Services

**`SalesOrderService`** — mirror `QuoteService`, add:
- `ContractResponse convertToContract(Long orderId)` — requires `status = DELIVERED`, creates Contract, inherits `totalValue`

**`ContractService`** — standard CRUD:
- `ContractResponse create(ContractRequest, String createdByUsername)`
- `Page<ContractResponse> findAll(Pageable, ContractStatus status)` `@Transactional(readOnly=true)`
- `List<ContractResponse> findExpiringWithin(int days)` — uses `findByEndDateBefore(LocalDate.now().plusDays(days))`
- `ContractResponse update(Long, ContractRequest)`, `void delete(Long)`

#### BE Step 7.7 — Controllers

**`SalesOrderController`** `@RestController @RequestMapping("/api/v1/sales-orders")`:
```
POST   /                             → create
GET    /                             → list
GET    /{id}                         → findById (with line items)
PUT    /{id}                         → update
DELETE /{id}                         → delete (204)
POST   /{id}/line-items              → addLineItem
DELETE /{id}/line-items/{lineId}     → removeLineItem
POST   /{id}/convert-to-contract     → convertToContract
```

**`ContractController`** `@RestController @RequestMapping("/api/v1/contracts")`:
```
POST   /     → create
GET    /     → list (?status=ACTIVE)
GET    /{id} → findById
PUT    /{id} → update
DELETE /{id} → delete (204)
```

#### FE Step 7.8 — SalesOrdersView

**`ui/SalesOrdersView.java`** — mirrors `QuotesView.java`:
```
Grid columns: orderNumber, accountName, status (badge), totalAmount, orderDate, deliveryDate, Actions
Action buttons: Convert to Contract (shown only when status = DELIVERED), Edit, Delete
Line items sub-panel below selected row (same as QuotesView)
Dialog: TextField title, ComboBox<SalesOrderStatus>, DatePicker orderDate/deliveryDate,
        ComboBox<QuoteResponse> quote (optional), ComboBox<AccountResponse>, TextArea notes
```

#### FE Step 7.9 — ContractsView

**`ui/ContractsView.java`**
```
@Route("contracts") @PageTitle("Contracts | CRM") @PermitAll
Grid<ContractResponse> grid = new Grid<>(ContractResponse.class, false)
Columns: contractNumber, title, accountName, status (badge), totalValue, startDate, endDate, Actions
Toolbar: ComboBox<ContractStatus> statusFilter, "New Contract" Button
Dialog: TextField title, ComboBox<ContractStatus>, DatePicker start/end, NumberField totalValue,
        TextField currency, ComboBox<AccountResponse>, ComboBox<ContactResponse>,
        ComboBox<SalesOrderResponse> salesOrder (optional), TextArea description/terms
```

#### FE Step 7.10 — DashboardView update

Add stat card: `contractService.findExpiringWithin(30).size()` → "Contracts Expiring (30d)"

#### FE Step 7.11 — Navigation

```java
sales.addItem(new SideNavItem("Sales Orders", SalesOrdersView.class, VaadinIcon.PACKAGE.create()));
sales.addItem(new SideNavItem("Contracts", ContractsView.class, VaadinIcon.FILE_TEXT.create()));
```

#### Verification Phase 7

1. Convert a WON Quote → Sales Order, mark DELIVERED, convert → Contract
2. Verify `contractNumber` = `CNT-YYYY-NNN` format
3. Check `findExpiringWithin(30)` returns contracts whose `endDate < today + 30 days`

---

### Phase 8 — Forecast Dashboard ✅ COMPLETE (built)

Adds ForecastService and ForecastView. No new entities — aggregates existing Lead, Opportunity, Quote data.

#### BE Step 8.1 — ForecastSummaryResponse DTO

**`dto/response/ForecastSummaryResponse`** record:
```java
String period,              // e.g. "2024-Q4"
BigDecimal totalAmount,
BigDecimal weightedAmount,  // for opportunities only
long count,
List<ForecastByStageResponse> byStage
```

**`ForecastByStageResponse`** record: `String stage`, `long count`, `BigDecimal amount`, `BigDecimal weighted`

#### BE Step 8.2 — ForecastService

**`ForecastService`** `@Service @Transactional(readOnly=true)`:

```java
ForecastSummaryResponse leadForecast() {
  // Group leads by status, sum estimatedValue, exclude LOST
  List<Lead> leads = leadRepository.findByStatusNot(LeadStatus.LOST);
  // group by status, sum estimatedValue per group
  // return summary with total and byStage list
}

ForecastSummaryResponse opportunityForecast() {
  // Group opportunities by stage, compute weighted = amount * (probability/100)
  List<Opportunity> opps = opportunityRepository.findByStageNot(OpportunityStage.LOST);
  // group by stage, sum amount and weighted per group
  // total = sum of all amounts, weightedAmount = sum of weighted amounts
}

ForecastSummaryResponse quoteForecast() {
  // Group quotes by status, sum totalAmount, exclude LOST/EXPIRED
  List<Quote> quotes = quoteRepository.findByStatusNot(QuoteStatus.LOST);
  // group by status, sum totalAmount
}
```

#### BE Step 8.3 — ForecastController

**`ForecastController`** `@RestController @RequestMapping("/api/v1/forecast")`:
```
GET /leads          → forecastService.leadForecast()
GET /opportunities  → forecastService.opportunityForecast()
GET /quotes         → forecastService.quoteForecast()
```

#### FE Step 8.4 — ForecastView

**`ui/ForecastView.java`**
```
@Route("forecast") @PageTitle("Forecast | CRM") @PermitAll
extends VerticalLayout
TabSheet tabs: Leads | Opportunities | Quotes
Each tab renders:
  H4 "Total: $X" + H4 "Weighted: $Y" (for opportunities)
  Grid<ForecastByStageResponse>:
    Columns: stage, count, amount (formatted), weighted (formatted, only for opportunities)
On tab change: call corresponding forecast endpoint, refresh grid
Use Span with LumoUtility.FontSize.XLARGE for summary numbers
```

#### FE Step 8.5 — DashboardView — full KPI row

Replace existing 2 stat cards with 5 cards:
1. Accounts count
2. Contacts count
3. Open Activities count (`activityService.countByStatus(ActivityStatus.OPEN)`)
4. Open Leads count (`leadService.countByStatus(LeadStatus.NEW)`)
5. Pipeline value (`opportunityService.sumPipelineAmount()` formatted as currency)

#### FE Step 8.6 — Navigation

```java
sales.addItem(new SideNavItem("Forecast", ForecastView.class, VaadinIcon.CHART.create()));
```

#### Verification Phase 8

1. Open http://localhost:9080/forecast
2. Create Opportunities at different stages — confirm weighted totals match `amount × probability/100`
3. Check Dashboard shows 5 KPI cards

---

### Phase 9 — Workspaces ✅ COMPLETE (built)

Multi-team isolation. All records scoped to a workspace via workspace_id FK.

#### BE Step 9.1 — Workspace Entity

**`domain/entity/Workspace.java`**
```
@Entity @Table("workspaces")
Fields:
  Long id
  @Column(nullable=false) String name
  @Column(columnDefinition="TEXT") String description
  @ManyToOne(fetch=LAZY) @JoinColumn("created_by_id") User createdBy
  @ManyToMany @JoinTable(name="workspace_members",
    joinColumns="workspace_id", inverseJoinColumns="user_id")
  List<User> members = new ArrayList<>()
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 9.2 — Add workspace_id to all entities

Add to: `Account`, `Contact`, `AccountGroup`, `Activity`, `Lead`, `Opportunity`, `Quote`, `SalesOrder`, `Contract`, `SavedSearch`:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "workspace_id")
private Workspace workspace;
```

#### BE Step 9.3 — Repository + Service

```java
// WorkspaceRepository extends JpaRepository<Workspace, Long>
// Methods: findByMembers_Id(Long userId), existsByName(String name)
```

**`WorkspaceService`** `@Service @Transactional`:
- `WorkspaceResponse create(WorkspaceRequest, String createdByUsername)` — also adds creator as member
- `List<WorkspaceResponse> findForUser(Long userId)` `@Transactional(readOnly=true)`
- `WorkspaceResponse addMember(Long workspaceId, Long userId)`
- `WorkspaceResponse removeMember(Long workspaceId, Long userId)`

#### BE Step 9.4 — Controller

**`WorkspaceController`** `@RestController @RequestMapping("/api/v1/workspaces")`:
```
POST   /                          → create (ADMIN only)
GET    /                          → list for current user
GET    /{id}                      → findById
POST   /{id}/members/{userId}     → addMember
DELETE /{id}/members/{userId}     → removeMember
```

#### BE Step 9.5 — DataInitializer update

In `DataInitializer.java`, after creating the admin user, seed the Default workspace:
```java
if (workspaceRepository.count() == 0) {
    Workspace defaultWs = new Workspace();
    defaultWs.setName("Default");
    defaultWs.setDescription("Default workspace");
    defaultWs.setCreatedBy(adminUser);
    defaultWs.getMembers().add(adminUser);
    workspaceRepository.save(defaultWs);
}
```

#### FE Step 9.6 — WorkspacesView (ADMIN only)

**`ui/WorkspacesView.java`**
```
@Route("workspaces") @PageTitle("Workspaces | CRM") @RolesAllowed("ADMIN")
Grid<WorkspaceResponse> columns: name, memberCount, createdAt, Actions
Dialog: TextField name, TextArea description
Member management sub-panel: list current members, ComboBox to add new member
```

#### FE Step 9.7 — Navigation

Add Settings section:
```java
SideNavItem settings = new SideNavItem("Settings");
settings.setPrefixComponent(VaadinIcon.COG.create());
settings.addItem(new SideNavItem("Workspaces", WorkspacesView.class, VaadinIcon.GROUP.create()));
nav.addItem(settings);
```

#### Verification Phase 9

1. Check `DataInitializer` created "Default" workspace on first boot
2. Create a second workspace, add admin as member
3. Verify `GET /api/v1/workspaces` returns only workspaces where current user is a member

---

### Phase 10 — Polish ✅ COMPLETE (built)

Final hardening — no new entities.

#### BE Step 10.1 — SavedSearch execution engine

In `SavedSearchService`, implement `List<?> execute(Long searchId)`:
- Parse `filterJson` as a map of field→value pairs
- Build JPA `Specification<T>` dynamically based on `scope`
- Execute via the relevant repository's `findAll(spec)`

#### FE Step 10.2 — Pagination in all grids

All `Grid` views currently load page 0 of 200 records. Update each view:
- Replace `refreshGrid()` with a `DataProvider` using `CallbackDataProvider`
- Wire `grid.setDataProvider(DataProvider.fromFilteringCallbacks(query -> ..., query -> ...))` to the paginated service call

#### BE Step 10.3 — Roles

Add `ROLE_SALES` and `ROLE_SUPPORT` to `SecurityConfig.java`:
```java
// In securityFilterChain:
.requestMatchers("/api/v1/leads/**", "/api/v1/opportunities/**",
                 "/api/v1/quotes/**", "/api/v1/sales-orders/**",
                 "/api/v1/contracts/**").hasAnyRole("SALES","ADMIN")
.requestMatchers("/api/v1/activities/**").hasAnyRole("SUPPORT","ADMIN")
```

Add `@RolesAllowed` to Vaadin views:
- `LeadsView`, `OpportunitiesView`, `QuotesView`, `SalesOrdersView`, `ContractsView`, `ForecastView` → `@RolesAllowed({"SALES","ADMIN"})`
- `ActivitiesView` → `@RolesAllowed({"SUPPORT","ADMIN"})`

#### FE Step 10.4 — Saved Searches UI

Add `SavedSearchesView` (`@Route("saved-searches")`):
- Grid: name, scope, Run button
- Run button → calls `/api/v1/saved-searches/{id}/execute`, shows result count in notification
- "New Search" dialog: TextField name, ComboBox scope, TextArea filterJson (JSON editor)

---

### Phase 11 — Activity Notes & Follow-ups ✅ COMPLETE (built)

OpenCRX tracks every state change and note on an activity as a timestamped follow-up entry. Adds `ActivityNote` entity, timeline UI inside the activity row, and proper status-transition actions (Assign → In Progress, Complete, Reopen, Close).

#### BE Step 11.1 — ActivityNote Entity

**`domain/entity/ActivityNote.java`**
```
@Entity @Table("activity_notes")
Fields:
  Long id
  @Column(columnDefinition="TEXT", nullable=false) String text
  @ManyToOne(fetch=LAZY) @JoinColumn("activity_id") Activity activity
  @ManyToOne(fetch=LAZY) @JoinColumn("author_id") User author
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
```

Add to **`Activity.java`**:
```java
@OneToMany(mappedBy="activity", cascade=ALL, fetch=LAZY, orphanRemoval=true)
@OrderBy("createdAt ASC")
List<ActivityNote> notes = new ArrayList<>();
```

#### BE Step 11.2 — Expand ActivityType enum

Add to `ActivityType.java`: `EMAIL, SALES_VISIT, MAILING, SMS, ABSENCE`
(keeps backward-compatible — existing BUG/FEATURE/TASK/MEETING/CALL values unchanged)

#### BE Step 11.3 — Repository

```java
// ActivityNoteRepository extends JpaRepository<ActivityNote, Long>
// findByActivity_IdOrderByCreatedAtAsc(Long activityId)
```

#### BE Step 11.4 — DTOs

**`ActivityNoteRequest`** record: `@NotBlank String text`

**`ActivityNoteResponse`** record: `Long id`, `String text`, `String authorName`, `LocalDateTime createdAt`
— `from(ActivityNote n)`: map all fields

Update **`ActivityResponse`** to add: `List<ActivityNoteResponse> notes`

#### BE Step 11.5 — Service updates

Add to **`ActivityService`**:
- `ActivityNoteResponse addNote(Long activityId, ActivityNoteRequest, String authorUsername)`
- `void deleteNote(Long noteId)`
- `ActivityResponse assign(Long activityId, String assignedToUsername)` — sets `status = IN_PROGRESS`
- `ActivityResponse reopen(Long activityId)` — sets `status = IN_PROGRESS`, clears `resolvedAt`

#### BE Step 11.6 — Controller updates

Add to **`ActivityController`**:
```
POST   /{id}/notes          → addNote (returns ActivityNoteResponse)
DELETE /{id}/notes/{noteId} → deleteNote (204)
PATCH  /{id}/assign         → assign (?username=)
PATCH  /{id}/reopen         → reopen
```

#### FE Step 11.7 — Activity detail panel

In **`ActivitiesView`**, add a detail row expansion (or side panel) per activity:
- Show notes timeline: each note as a card with author, timestamp, text
- "Add Note" text area + Submit button at the bottom
- Transition buttons: Assign (shows only if NEW), Reopen (shows only if RESOLVED/CLOSED)

#### Verification Phase 11

1. Create a MEETING activity
2. Add a note — confirm note appears in timeline with timestamp
3. Click Assign — confirm status changes to IN_PROGRESS and a system note is added
4. Click Resolve then Reopen — confirm status goes back to IN_PROGRESS

---

### Phase 12 — Product Catalog ✅ COMPLETE (built)

OpenCRX has a full product/price list model. Adding a product catalog lets users select products when building quote and sales-order line items instead of typing free-text product names.

#### BE Step 12.1 — Enums

**`ProductCategory.java`** — `SOFTWARE, HARDWARE, SERVICE, SUBSCRIPTION, CONSULTING, OTHER`

#### BE Step 12.2 — Product Entity

**`domain/entity/Product.java`**
```
@Entity @Table("products")
Fields:
  Long id
  @Column(nullable=false, unique=true) String sku
  @Column(nullable=false) String name
  @Column(columnDefinition="TEXT") String description
  @Enumerated(EnumType.STRING) ProductCategory category
  @Column(precision=15, scale=2, nullable=false) BigDecimal unitPrice
  @Column(length=3) String currency = "USD"
  boolean active = true
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

#### BE Step 12.3 — Repository

```java
// ProductRepository extends JpaRepository<Product, Long>
// Page<Product> findByNameContainingIgnoreCaseAndActive(String name, boolean active, Pageable pageable)
// long countByNameContainingIgnoreCaseAndActive(String name, boolean active)
// Optional<Product> findBySku(String sku)
```

#### BE Step 12.4 — DTOs

**`ProductRequest`** record: `@NotBlank String sku`, `@NotBlank String name`, `String description`, `ProductCategory category`, `@NotNull BigDecimal unitPrice`, `String currency`

**`ProductResponse`** record: `Long id`, `String sku`, `String name`, `String description`, `ProductCategory category`, `BigDecimal unitPrice`, `String currency`, `boolean active`
— `from(Product p)`

#### BE Step 12.5 — Service

**`ProductService`** `@Service @Transactional`:
- `ProductResponse create(ProductRequest)`
- `ProductResponse findById(Long)` `@Transactional(readOnly=true)`
- `Page<ProductResponse> findAll(Pageable, String search)` `@Transactional(readOnly=true)`
- `long count(String search)` `@Transactional(readOnly=true)`
- `ProductResponse update(Long, ProductRequest)`
- `ProductResponse toggleActive(Long)`
- `void delete(Long)`

#### BE Step 12.6 — Controller

**`ProductController`** `@RestController @RequestMapping("/api/v1/products")`:
```
POST   /              → create
GET    /              → list (?search=)
GET    /{id}          → findById
PUT    /{id}          → update
PATCH  /{id}/toggle   → toggleActive
DELETE /{id}          → delete (204)
```

#### FE Step 12.7 — ProductsView

**`ui/ProductsView.java`**
```
@Route("products") @PageTitle("Products | CRM") @PermitAll
extends VerticalLayout
Grid<ProductResponse> grid = new Grid<>(ProductResponse.class, false)
Columns: sku, name, category, unitPrice (formatted), currency, active (badge), Actions
Toolbar:
  TextField searchField (ValueChangeMode.LAZY)
  "New Product" Button (LUMO_PRIMARY)
Action buttons: Edit, Toggle Active (green/grey), Delete
Dialog fields: TextField sku, name; TextArea description;
               ComboBox<ProductCategory> category;
               NumberField unitPrice; TextField currency
```

#### FE Step 12.8 — Wire products into line-item dialogs

In **`QuotesView`** and **`SalesOrdersView`** line-item dialogs:
- Replace free-text `product_name` TextField with a `ComboBox<ProductResponse>` backed by `productService.findAll()`
- On selection, auto-fill `unitPrice` from the chosen product
- Still allow manual override of unit price

#### FE Step 12.9 — Navigation

Add Products under Sales in `MainLayout.createDrawer()`:
```java
sales.addItem(new SideNavItem("Products", ProductsView.class, VaadinIcon.CART.create()));
```

#### Verification Phase 12

1. Create 3 products with different SKUs and prices
2. Open a Quote → add line item → confirm product ComboBox appears and auto-fills price
3. Disable a product → confirm it no longer appears in the ComboBox (filter `active=true`)
4. Check `GET /api/v1/products` in Swagger

---

### Phase 13 — User Management UI (Admin) ✅ COMPLETE (built)

OpenCRX's "Managing Users" guide covers creating principals, assigning groups, and disabling segment access. Adds an admin-only Users view to manage CRM users and their roles.

#### BE Step 13.1 — User DTOs

**`UserRequest`** record: `@NotBlank String username`, `@Email String email`, `String password`, `Set<String> roles`

**`UserResponse`** record: `Long id`, `String username`, `String email`, `Set<String> roles`, `boolean enabled`, `LocalDateTime createdAt`
— `from(User u)`: map all fields

#### BE Step 13.2 — UserService additions

Add to **`UserService`** (or create **`UserManagementService`**):
- `Page<UserResponse> findAll(Pageable, String search)` `@Transactional(readOnly=true)`
- `long count(String search)` `@Transactional(readOnly=true)`
- `UserResponse create(UserRequest)` — encode password with `PasswordEncoder`
- `UserResponse update(Long id, UserRequest)` — update email/roles; only re-encode password if non-blank
- `UserResponse toggleEnabled(Long id)` — flip `enabled` flag
- `void delete(Long id)` — prevent deleting last admin

#### BE Step 13.3 — UserController

**`UserController`** `@RestController @RequestMapping("/api/v1/users") @PreAuthorize("hasRole('ADMIN')")`:
```
GET    /              → list (paginated, ?search=)
GET    /{id}          → findById
POST   /              → create
PUT    /{id}          → update
PATCH  /{id}/toggle   → toggleEnabled
DELETE /{id}          → delete (204)
```

#### FE Step 13.4 — UsersView

**`ui/UsersView.java`**
```
@Route("users") @PageTitle("Users | CRM") @RolesAllowed("ADMIN")
extends VerticalLayout
Grid<UserResponse> grid = new Grid<>(UserResponse.class, false)
Columns: username, email, roles (badges), enabled (badge), createdAt, Actions
Toolbar: TextField searchField + "New User" Button
Action buttons: Edit, Toggle Active, Delete (disabled for self)
Dialog fields: TextField username, email, password (hint: leave blank to keep current);
               CheckboxGroup<String> roles (choices: ROLE_USER, ROLE_ADMIN, ROLE_SALES, ROLE_SUPPORT)
```

#### FE Step 13.5 — Navigation

Add Users under Settings in `MainLayout.createDrawer()` (admin-only, check role):
```java
SecurityService sec = // injected
if (sec.hasRole("ADMIN")) {
    settings.addItem(new SideNavItem("Users", UsersView.class, VaadinIcon.USERS.create()));
}
```

#### Verification Phase 13

1. Log in as admin → navigate Settings → Users
2. Create a new user with `ROLE_SALES`
3. Disable the user — confirm they cannot log in (enabled=false)
4. Re-enable the user
5. Verify `GET /api/v1/users` returns 401 without admin JWT

---

### Phase 14 — Data Import / Export ✅ COMPLETE (built)

OpenCRX supports Excel/CSV import for Contacts and Accounts, and export of any grid. Adds CSV import for bulk data loading and a "Download CSV" button on every grid.

#### BE Step 14.1 — CSV Export utility

**`util/CsvExporter.java`** — generic utility:
```java
// Writes List<T> to a CSV InputStream using reflection on getter methods
// Columns are derived from the record component names
public static <T extends Record> InputStream export(List<T> rows, Class<T> type)
```

Add to each service a `List<XxxResponse> findAllForExport(filters...)` that fetches all (no page limit) for the export case.

#### BE Step 14.2 — Export endpoints

Add to **`AccountController`**, **`ContactController`**, **`LeadController`**, **`OpportunityController`**, **`ActivityController`**:
```
GET /api/v1/{resource}/export?format=csv  → streams CSV file (Content-Disposition: attachment)
```

Use `StreamingResponseBody` or `ResponseEntity<Resource>` to stream the CSV.

#### BE Step 14.3 — CSV Import endpoint (Contacts)

Add to **`ContactController`**:
```
POST /api/v1/contacts/import  → multipart/form-data with CSV file
```

CSV columns: `first_name, last_name, email, phone, job_title, department`

**`ContactImportService`**:
- Parse CSV with `java.io.BufferedReader` (no extra deps)
- Skip header row, skip blank lines
- Call `contactService.create()` per row
- Return `ImportResultResponse` record: `int imported`, `int skipped`, `List<String> errors`

Add same `POST /api/v1/accounts/import` for Accounts:
CSV columns: `name, industry, website, phone, email, type`

#### FE Step 14.4 — Export button on grid views

In every grid view toolbar, add an "Export CSV" button (VaadinIcon.DOWNLOAD):
```java
Button exportBtn = new Button("Export CSV", VaadinIcon.DOWNLOAD.create());
exportBtn.addClickListener(e -> {
    StreamResource resource = new StreamResource("export.csv",
        () -> getUI().map(ui -> /* call /api/v1/{resource}/export */).orElse(null));
    Anchor download = new Anchor(resource, "");
    download.getElement().setAttribute("download", true);
    download.getElement().executeJs("this.click()");
    add(download); // temporary, remove after click
});
```

#### FE Step 14.5 — Import dialog (Contacts + Accounts views)

Add "Import CSV" button to `ContactsView` and `AccountsView` toolbar:
- Opens a dialog with an `Upload` component (Vaadin's built-in)
- On file received, POST to `/api/v1/contacts/import` using `RestTemplate` / `HttpClient`
- Show result: "Imported 42 contacts, 3 skipped" notification

#### Verification Phase 14

1. Export Contacts grid → verify CSV downloads with correct columns
2. Create a CSV with 5 contacts → import → verify they appear in the grid
3. Import a CSV with a duplicate email → verify that row is skipped and reported in errors
4. Test export with active filters (e.g., only WON leads)

---

### Phase 15 — Subscribe / Notify & In-App Alerts ✅ COMPLETE (built)

OpenCRX's Subscribe/Notify system is built around three core entities — **Alert**, **Subscription**, and **Topic** — backed by a `SubscriptionHandlerServlet` that scans an audit trail for object changes and fires matching workflows. This phase replicates that model faithfully.

---

#### OpenCRX Source Mapping

| OpenCRX Class / Concept | Spring Boot Equivalent |
|---|---|
| `Alert` entity (in `UserHome`) | `Alert` entity |
| `AlertState` enum (`NEW`, `READ`, `ACCEPTED`, `EXPIRED`) | `AlertState` Java enum |
| `Subscription` entity (in `UserHome`) | `Subscription` entity |
| `Topic` entity (system-level, 15 standard topics) | `Topic` entity + `DataInitializer` seed |
| `SubscriptionHandlerServlet` — polls audit trail | `SubscriptionHandlerService` `@Scheduled` bean |
| `WorkflowHandlerServlet` — dispatches async workflows | `NotificationDispatchService` |
| `SendAlert` synchronous workflow | `AlertService.sendAlert()` |
| `SendMailNotificationWorkflow` async workflow | `EmailNotificationService.sendMailNotification()` |
| `Base.sendAlert(target, toUsers, name, desc, importance, resendDelay, reference)` | `AlertService.sendAlert(...)` |
| `UserHomes.markAsRead / markAsAccepted / markAsNew` | `AlertService.markAsRead / markAsAccepted / markAsNew` |
| `UserHomes.refreshItems()` — expires stale alerts | `AlertService.expireStaleAlerts()` `@Scheduled` |
| `subscriptionMatches(topic, eventType, filters 0–4)` | `SubscriptionMatcherService.matches(...)` |

---

#### BE Step 15.1 — Enums

**`AlertState.java`**
```java
public enum AlertState {
    NEW,       // initial — shown in bell badge
    READ,      // user opened the alert
    ACCEPTED,  // user explicitly acknowledged
    EXPIRED    // auto-set: READ alerts > 3 months old, or invalid reference > 1 month
}
```

**`AlertImportance.java`**
```java
public enum AlertImportance {
    LOW, NORMAL, HIGH, URGENT
}
```

**`SubscriptionEventType.java`**
```java
public enum SubscriptionEventType {
    OBJECT_CREATION,    // eventType value 1
    OBJECT_REPLACEMENT, // eventType value 3
    OBJECT_REMOVAL,     // eventType value 4
    TIMER               // timer-triggered
}
```

---

#### BE Step 15.2 — Alert Entity

**`domain/entity/Alert.java`**
```
@Entity @Table("alerts")
Fields:
  Long id
  @ManyToOne(fetch=LAZY) @JoinColumn("user_id") User user           // owner (private)
  @Column(nullable=false) String name                                // alert title (default "--")
  @Column(columnDefinition="TEXT") String description               // HTML body
  @Enumerated(EnumType.STRING)
  AlertState alertState = AlertState.NEW
  @Enumerated(EnumType.STRING)
  AlertImportance importance = AlertImportance.NORMAL
  String entityType    // "LEAD","ACTIVITY","OPPORTUNITY","QUOTE","SALES_ORDER",
                       // "CONTRACT","ACCOUNT","CONTACT","PRODUCT","ACTIVITY_NOTE"
  Long entityId        // FK to the triggering record (nullable)
  @Column(nullable=false) LocalDateTime resendCutoffAt  // now() + resendDelaySeconds
                       // new alert suppressed if one exists for same (user,entityType,entityId)
                       // within this window — mirrors OpenCRX resendDelayInSeconds (default 60 s)
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
```

> **OpenCRX parity notes:**
> - `accessLevelDelete` and `accessLevelUpdate` are both `PRIVATE` in OpenCRX — only the owning user can read, update, or delete their own alerts.
> - `reference` in OpenCRX is the XRI path of the triggering object; here it is captured as `entityType` + `entityId`.
> - Deduplication: before inserting, query for an existing alert with the same `(user, entityType, entityId)` where `resendCutoffAt > now()`. If found, skip creation.

---

#### BE Step 15.3 — Subscription Entity

Models the OpenCRX `Subscription` object that lives on a `UserHome`. Each user manages their own subscriptions.

**`domain/entity/Subscription.java`**
```
@Entity @Table("subscriptions")
Fields:
  Long id
  @Column(nullable=false) String name
  @Column(columnDefinition="TEXT") String description
  boolean active = true
  @ManyToOne(fetch=LAZY) @JoinColumn("user_id") User user          // subscription owner
  @ManyToOne(fetch=LAZY) @JoinColumn("topic_id") Topic topic

  // Event type filter — empty collection means "all types"
  @ElementCollection
  @CollectionTable(name="subscription_event_types")
  @Enumerated(EnumType.STRING)
  List<SubscriptionEventType> eventTypes = new ArrayList<>()

  // Up to 5 named attribute filters (AND across names, OR within values)
  // Value prefixed with "!" means negation (OpenCRX convention)
  String filterName0; String filterValue0   // e.g. filterName0="assignedTo", filterValue0="admin"
  String filterName1; String filterValue1
  String filterName2; String filterValue2
  String filterName3; String filterValue3
  String filterName4; String filterValue4

  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
  @LastModifiedDate LocalDateTime updatedAt
```

---

#### BE Step 15.4 — Topic Entity

System-level; seeded once by `DataInitializer`. Mirrors OpenCRX's 15 standard topics.

**`domain/entity/Topic.java`**
```
@Entity @Table("topics")
Fields:
  Long id
  @Column(nullable=false, unique=true) String name
  @Column(nullable=false) String entityType   // entity class the topic watches
  // sendAlertEnabled → creates in-app Alert via AlertService.sendAlert()
  boolean sendAlertEnabled = true
  // sendMailEnabled → sends email via EmailNotificationService (requires mail config)
  boolean sendMailEnabled = false
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
```

**Standard topics seeded in `DataInitializer`** (mirrors OpenCRX `Workflows.initTopic()`):

| Topic Name | `entityType` | Default Actions |
|---|---|---|
| Account Modifications | `ACCOUNT` | SendAlert |
| Activity Modifications | `ACTIVITY` | SendAlert |
| Activity Follow Up Modifications | `ACTIVITY_NOTE` | SendAlert |
| Lead Modifications | `LEAD` | SendAlert |
| Opportunity Modifications | `OPPORTUNITY` | SendAlert |
| Quote Modifications | `QUOTE` | SendAlert |
| Sales Order Modifications | `SALES_ORDER` | SendAlert |
| Contract Modifications | `CONTRACT` | SendAlert |
| Product Modifications | `PRODUCT` | SendAlert |
| Contact Modifications | `CONTACT` | SendAlert |
| Alert Modifications | `ALERT` | SendMailNotification (email on new alert) |

---

#### BE Step 15.5 — Repositories

```java
// AlertRepository extends JpaRepository<Alert, Long>
// List<Alert> findByUser_IdAndAlertStateInOrderByCreatedAtDesc(Long userId, List<AlertState> states)
// long countByUser_IdAndAlertState(Long userId, AlertState state)
// Page<Alert> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable)
// List<Alert> findByUser_IdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId)
// List<Alert> findByAlertStateAndCreatedAtBefore(AlertState state, LocalDateTime cutoff) // for expiry

// SubscriptionRepository extends JpaRepository<Subscription, Long>
// List<Subscription> findByUser_IdAndActiveTrue(Long userId)
// List<Subscription> findByTopic_IdAndActiveTrue(Long topicId)

// TopicRepository extends JpaRepository<Topic, Long>
// Optional<Topic> findByEntityType(String entityType)
// Optional<Topic> findByName(String name)
```

---

#### BE Step 15.6 — DTOs

**`AlertResponse`** record: `Long id`, `String name`, `String description`, `AlertState alertState`, `AlertImportance importance`, `String entityType`, `Long entityId`, `LocalDateTime createdAt`
— `from(Alert a)`

**`SubscriptionRequest`** record: `@NotBlank String name`, `String description`, `boolean active`, `Long topicId`, `List<SubscriptionEventType> eventTypes`, `String filterName0`, `String filterValue0`, `String filterName1`, `String filterValue1`, `String filterName2`, `String filterValue2`, `String filterName3`, `String filterValue3`, `String filterName4`, `String filterValue4`

**`SubscriptionResponse`** record: `Long id`, `String name`, `String description`, `boolean active`, `String topicName`, `String entityType`, `List<SubscriptionEventType> eventTypes`, `LocalDateTime createdAt`

**`TopicResponse`** record: `Long id`, `String name`, `String entityType`, `boolean sendAlertEnabled`, `boolean sendMailEnabled`

---

#### BE Step 15.7 — AlertService

**`AlertService`** `@Service @Transactional`:

```java
// Mirrors Base.sendAlert() — deduplication via resendCutoffAt window
List<AlertResponse> sendAlert(
    String toUsernames,          // comma/semicolon-delimited (mirrors OpenCRX toUsers)
    String name,                 // alert title; defaults to "--" if blank
    String description,          // HTML body (Bootstrap-styled like OpenCRX Notifications.getNotificationText())
    AlertImportance importance,  // defaults to NORMAL
    int resendDelaySeconds,      // default 60; suppress duplicate within window
    String entityType,           // triggering entity class
    Long entityId                // triggering entity PK
)

AlertResponse findById(Long id) @Transactional(readOnly=true)
Page<AlertResponse> findAll(Long userId, List<AlertState> states, Pageable) @Transactional(readOnly=true)
long countUnread(Long userId)   // counts alertState = NEW @Transactional(readOnly=true)

// Mirrors UserHomes.markAsRead / markAsAccepted / markAsNew
AlertResponse markAsRead(Long alertId)
AlertResponse markAsAccepted(Long alertId)
AlertResponse markAsNew(Long alertId)
void markAllRead(Long userId)

// Mirrors UserHomes.refreshItems() — run on @Scheduled(fixedDelay=3_600_000)
void expireStaleAlerts()
// Logic:
//   • alertState = READ  AND createdAt < now() - 3 months  →  EXPIRED
//   • alertState = NEW   AND entityId no longer exists      →  READ after 1 month
//   • deduplication: skip new alert if (user,entityType,entityId) alert exists within resendCutoffAt
```

---

#### BE Step 15.8 — SubscriptionMatcherService

Mirrors `UserHomes.subscriptionMatches()` — called by `SubscriptionHandlerService` for each audit event.

**`SubscriptionMatcherService`** `@Service`:

```java
boolean matches(Subscription sub, String entityType, SubscriptionEventType eventType, Map<String,String> entityAttributes) {
    // 1. topicMatches: sub.topic.entityType == entityType
    // 2. eventTypeMatches: sub.eventTypes is empty OR eventType in sub.eventTypes
    // 3. filterMatches: for each configured filterNameN/filterValueN pair (0..4):
    //      entityAttributes.get(filterNameN) matches filterValueN
    //      multiple values within one filter → OR logic
    //      multiple filter slots → AND logic
    //      value prefixed with "!" → negation
    return topicMatches && eventTypeMatches && filterMatches;
}
```

---

#### BE Step 15.9 — SubscriptionHandlerService

Mirrors OpenCRX `SubscriptionHandlerServlet` — scans audit/change records and fires alerts.

**`SubscriptionHandlerService`** `@Service`:

```java
// @Scheduled(fixedDelay=30_000)  — every 30 s (OpenCRX default cycles continuously)
void handleSubscriptions() {
    // 1. Pull unprocessed AuditLog entries (batch size 50, same as OpenCRX BATCH_SIZE)
    // 2. For each entry: determine entityType + eventType + entityAttributes
    // 3. Find active Subscriptions where topic.entityType matches
    // 4. For each Subscription: call subscriptionMatcherService.matches(...)
    // 5. On match: call alertService.sendAlert(sub.user.username, ..., entityType, entityId)
    // 6. If topic.sendMailEnabled: call emailNotificationService.sendMailNotification(...)
    // 7. Mark AuditLog entry as processed
    // User-enabled guard: skip subscriptions for disabled users
}
```

**`AuditLog`** entity (lightweight — no UI needed):
```
@Entity @Table("audit_log")
  Long id
  String entityType
  Long entityId
  @Enumerated(EnumType.STRING) SubscriptionEventType eventType
  boolean processed = false
  @CreatedDate LocalDateTime createdAt
```

Populate `AuditLog` from each service's `create()`, `update()`, and `delete()` methods by saving an `AuditLog` row at the end of the transaction.

---

#### BE Step 15.10 — EmailNotificationService

Mirrors OpenCRX `SendMailNotificationWorkflow`. Produces HTML email matching `Notifications.getNotificationText()`.

**`EmailNotificationService`** `@Service`:

```java
// @Value("${app.mail.enabled:false}") guard — same as Phase 18 EmailService
void sendMailNotification(User toUser, Alert alert) {
    // Subject: configurable prefix + alert.name
    //   e.g. "[CRM Alert] New lead assigned: Demo Corp"
    // Body (HTML, Bootstrap 3.3.6 styled):
    //   For ACTIVITY / ACTIVITY_NOTE alerts — include:
    //     • Reporting contact name
    //     • Handler / assigned-to username
    //     • Priority, status
    //     • Scheduled start / due date, actual end
    //     • Meeting attendees with acceptance status (+/−/?)
    //   For all other entity types:
    //     • Alert name + description
    //     • Entity type + entity ID
    //     • Trigger timestamp
    // Sent asynchronously: @Async + try/catch(MailException) — mail failure never breaks main tx
}
```

---

#### BE Step 15.11 — Controllers

**`AlertController`** `@RestController @RequestMapping("/api/v1/alerts")`:
```
GET    /                → list for current user (?state=NEW&state=READ, paginated)
GET    /count           → countUnread (NEW alerts only)
GET    /{id}            → findById
PATCH  /{id}/read       → markAsRead
PATCH  /{id}/accepted   → markAsAccepted
PATCH  /{id}/new        → markAsNew
PATCH  /read-all        → markAllRead (current user)
DELETE /{id}            → delete (204, own alerts only)
```

**`SubscriptionController`** `@RestController @RequestMapping("/api/v1/subscriptions")`:
```
POST   /     → create (for current user)
GET    /     → list for current user
GET    /{id} → findById
PUT    /{id} → update
DELETE /{id} → delete (204)
```

**`TopicController`** `@RestController @RequestMapping("/api/v1/topics")`:
```
GET    /     → list all topics (read-only for regular users)
GET    /{id} → findById
PUT    /{id} → update sendAlertEnabled / sendMailEnabled (ADMIN only)
```

---

#### BE Step 15.12 — Wire AuditLog to all services

Add at the end of every service `create()`, `update()`, and `delete()` method:

```java
auditLogRepository.save(new AuditLog(entityType, entity.getId(), eventType));
```

Affected services: `AccountService`, `ContactService`, `ActivityService`, `LeadService`, `OpportunityService`, `QuoteService`, `SalesOrderService`, `ContractService`, `ProductService`, `ActivityNoteService` (via `ActivityService.addNote`).

---

#### FE Step 15.13 — Notification bell in header

In **`MainLayout.createHeader()`**:
- `Button` with `VaadinIcon.BELL` + `Badge` showing `alertService.countUnread(userId)` (NEW alerts only)
- Poll every 60 s: `ui.setPollInterval(60_000)` with `addPollListener`
- Click → `Dialog` "Alerts" listing NEW + READ alerts, most recent first:
  - Each row: alert name, entity type badge, "time ago" label, importance indicator
  - Per-row buttons: **Mark Read** / **Mark Accepted** / **Mark New** (mirrors OpenCRX three-state model)
  - **Mark All Read** button at dialog top
- Badge disappears when `countUnread == 0`

---

#### FE Step 15.14 — Subscriptions management view

**`ui/SubscriptionsView.java`**
```
@Route("subscriptions") @PageTitle("Subscriptions | CRM") @PermitAll
extends VerticalLayout
Grid<SubscriptionResponse>
  Columns: name, topicName (entity type), active (badge), eventTypes, createdAt, Actions
Toolbar: "New Subscription" Button
Dialog fields:
  TextField name, TextArea description
  Toggle active
  ComboBox<TopicResponse> topic (shows all 11 standard topics by name)
  CheckboxGroup<SubscriptionEventType> eventTypes ("All" if none selected)
  FormLayout 5×2 for filterName0/filterValue0 … filterName4/filterValue4 (optional)
    — label: "Filter N: field name" + "Filter N: value (prefix ! to negate)"
```

Add to **`MainLayout.createDrawer()`** under Settings:
```java
settings.addItem(new SideNavItem("Subscriptions", SubscriptionsView.class, VaadinIcon.BELL.create()));
```

---

#### Verification Phase 15

1. On first boot: confirm `DataInitializer` seeds 11 Topic rows in DB
2. Create a Lead assigned to admin — within 30 s, confirm an `Alert` row is created for admin with `alertState = NEW`
3. Check bell badge shows count 1 → open panel → lead alert appears
4. Click **Mark Read** — badge disappears; `alertState` changes to `READ`
5. Click **Mark Accepted** — `alertState` changes to `ACCEPTED`
6. Create a Subscription for `Opportunity Modifications` topic with `eventType = OBJECT_REPLACEMENT`; set `filterName0 = stage`, `filterValue0 = WON` → update an Opportunity to stage WON — confirm only that subscription fires
7. Set `filterValue0 = !LOST` — update Opportunity to stage PROPOSAL — confirm alert fires; update to LOST — confirm no alert
8. Run `alertService.expireStaleAlerts()` — confirm READ alerts older than 3 months become EXPIRED and no longer appear in bell count
9. Confirm `AuditLog` table grows one row per create/update/delete event across all entity types
10. Enable mail (`app.mail.enabled=true`) — create a Lead → verify email arrives via `Alert Modifications` topic's `sendMailEnabled` flag

---

### Phase 16 — Documents & Attachments ✅ COMPLETE (built)

OpenCRX supports attaching documents to any CRM object. Adds a generic `Attachment` entity that can be linked to an Account, Contact, Activity, Lead, Opportunity, or Contract.

#### BE Step 16.1 — Attachment Entity

**`domain/entity/Attachment.java`**
```
@Entity @Table("attachments")
Fields:
  Long id
  @Column(nullable=false) String filename
  @Column(nullable=false) String contentType
  long fileSize
  @Lob @Column(columnDefinition="BYTEA") byte[] data
  String entityType    // "ACCOUNT", "CONTACT", "ACTIVITY", "LEAD", "OPPORTUNITY", "CONTRACT"
  Long entityId
  @ManyToOne(fetch=LAZY) @JoinColumn("uploaded_by_id") User uploadedBy
  @CreatedDate @Column(updatable=false) LocalDateTime createdAt
```

> PostgreSQL: `BYTEA` stores binary data efficiently. Max upload size configured via `spring.servlet.multipart.max-file-size=10MB`.

#### BE Step 16.2 — Repository

```java
// AttachmentRepository extends JpaRepository<Attachment, Long>
// List<Attachment> findByEntityTypeAndEntityId(String entityType, Long entityId)
```

#### BE Step 16.3 — Service

**`AttachmentService`** `@Service @Transactional`:
- `AttachmentResponse upload(MultipartFile file, String entityType, Long entityId, String uploadedByUsername)`
- `List<AttachmentResponse> findByEntity(String entityType, Long entityId)` `@Transactional(readOnly=true)`
- `byte[] download(Long id)` `@Transactional(readOnly=true)` — returns raw bytes
- `void delete(Long id)`

**`AttachmentResponse`** record: `Long id`, `String filename`, `String contentType`, `long fileSize`, `String uploadedByName`, `LocalDateTime createdAt`

#### BE Step 16.4 — Controller

**`AttachmentController`** `@RestController @RequestMapping("/api/v1/attachments")`:
```
POST   /upload?entityType=ACCOUNT&entityId=1  → multipart upload
GET    /entity?entityType=ACCOUNT&entityId=1  → list attachments
GET    /{id}/download                          → stream file (Content-Disposition: attachment)
DELETE /{id}                                   → delete (204)
```

#### FE Step 16.5 — Attachment panel in detail views

In **`AccountsView`**, **`ContactsView`**, **`ActivitiesView`** detail sections:
- Add an "Attachments" `Accordion` section at the bottom of the edit dialog
- List existing attachments with download links
- Vaadin `Upload` component for adding new files (max 10 MB)
- On upload success, refresh the attachment list

#### Verification Phase 16

1. Open an Account → attach a PDF file
2. Verify it appears in the attachments list with correct filename and size
3. Click download → confirm the file streams correctly
4. Delete the attachment → confirm it disappears
5. Verify `GET /api/v1/attachments/entity?entityType=ACCOUNT&entityId=1` returns the attachment metadata

---

### Phase 17 — Calendar View for Activities

OpenCRX has a calendar-based view for Meeting, Sales Visit, and Task activities. Adds a monthly calendar grid showing scheduled activities, with click-to-create for any date.

#### BE Step 17.1 — Calendar query endpoint

Add to **`ActivityController`**:
```
GET /api/v1/activities/calendar?from=2024-01-01&to=2024-01-31
```

Returns `List<ActivityResponse>` filtered by `dueDate` between `from` and `to`, types `MEETING, TASK, SALES_VISIT` only, sorted by `dueDate`.

Add to **`ActivityRepository`**:
```java
List<Activity> findByDueDateBetweenAndTypeIn(LocalDate from, LocalDate to, List<ActivityType> types);
```

#### BE Step 17.2 — iCalendar export

Add to **`ActivityController`**:
```
GET /api/v1/activities/ical  → produces text/calendar (RFC 5545)
```

Generates a `.ics` file for all `MEETING`, `TASK`, `SALES_VISIT` activities due in the next 90 days using manual string building (no extra deps):
```
BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
DTSTART:20240115T090000Z
SUMMARY:Meeting with Acme Corp
DESCRIPTION:...
END:VEVENT
...
END:VCALENDAR
```

#### FE Step 17.3 — CalendarView

**`ui/CalendarView.java`**
```
@Route("calendar") @PageTitle("Calendar | CRM") @PermitAll
extends VerticalLayout

State:
  private YearMonth currentMonth = YearMonth.now()

Layout:
  HorizontalLayout nav bar:
    Button "< Prev" → currentMonth = currentMonth.minusMonths(1); refreshCalendar()
    H3 showing "June 2024"
    Button "Next >" → currentMonth = currentMonth.plusMonths(1); refreshCalendar()
    Button "Download .ics" (VaadinIcon.CALENDAR)

  GridLayout (7 columns, N rows) for the calendar grid:
    - Header row: Mon Tue Wed Thu Fri Sat Sun
    - Each day cell: date number + list of activity chips (title, color by type)
    - Empty cells for days before month start

Activity chips:
  MEETING = blue, TASK = orange, SALES_VISIT = green
  Click chip → opens read-only activity detail dialog
  Click empty day → opens "New Activity" dialog with dueDate pre-filled
```

#### FE Step 17.4 — Navigation

Add Calendar under Support in `MainLayout.createDrawer()`:
```java
support.addItem(new SideNavItem("Calendar", CalendarView.class, VaadinIcon.CALENDAR.create()));
```

#### Verification Phase 17

1. Create 3 MEETING activities with due dates in the current month
2. Open Calendar — verify the meetings appear on the correct days
3. Click "< Prev" — confirm the month changes and activities for previous month show
4. Click an empty date — confirm "New Activity" dialog opens with that date pre-filled
5. Download .ics — open in calendar app and verify events appear

---

### Phase 18 — Email Activity & Spring Mail Integration

OpenCRX has full email service configuration. Adds email sending capability when an EMAIL-type Activity is created or a Lead/Opportunity is assigned, using Spring Mail with SMTP.

#### BE Step 18.1 — Spring Mail dependency

Add to **`pom.xml`**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

#### BE Step 18.2 — Mail configuration

Add to **`application-postgres.properties`**:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.enabled=${MAIL_ENABLED:false}
```

Mail is disabled by default (`MAIL_ENABLED=false`). Set env vars to enable it in production.

#### BE Step 18.3 — EmailService

**`service/EmailService.java`** `@Service`:
```java
@Value("${app.mail.enabled:false}") private boolean mailEnabled;

public void sendActivityAssigned(String toEmail, String activityTitle, String assignedByName) {
    if (!mailEnabled) return;
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(toEmail);
    msg.setSubject("CRM: Activity assigned to you — " + activityTitle);
    msg.setText("Hello,\n\nYou have been assigned a new activity: " + activityTitle +
                "\nAssigned by: " + assignedByName + "\n\nLog in to view it.");
    mailSender.send(msg);
}

public void sendLeadAssigned(String toEmail, String leadTitle, String assignedByName) { ... }
public void sendOpportunityWon(String toEmail, String opportunityName) { ... }
```

#### BE Step 18.4 — Wire email sends

Inject `EmailService` into:
- **`ActivityService.create()`** — if `assignedTo` has an email, call `sendActivityAssigned`
- **`LeadService.create()`** — call `sendLeadAssigned` if assignee has email
- **`OpportunityService.update()`** — when stage becomes `WON`, call `sendOpportunityWon`

All sends are fire-and-forget wrapped in `try/catch(MailException e)` — mail failures never break the main transaction.

#### BE Step 18.5 — Email Activity type handling

When `ActivityService.create()` receives `type = EMAIL`:
- Require `contact_id` or `account_id` to be set (validate in service — throw `IllegalArgumentException` if both null)
- Store email subject in `title`, email body in `description`
- If `mailEnabled`, send the email immediately to the linked contact's email address

#### FE Step 18.6 — Email composition dialog

In **`ActivitiesView`**, when the type `ComboBox` value is `EMAIL`:
- Show extra fields: `TextField subject` (maps to `title`), `TextArea body` (maps to `description`)
- `ComboBox<ContactResponse> to` (required — maps to `contact`)
- On save, show "Email sent" notification if `mailEnabled`, or "Email activity saved (mail disabled)" otherwise

#### Verification Phase 18

1. Set `MAIL_ENABLED=false` (default) — create an EMAIL activity → confirm it saves without errors
2. Create a Lead assigned to admin — confirm no mail error even with mail disabled
3. To test real mail: set `MAIL_USERNAME` + `MAIL_PASSWORD` env vars with a Gmail app password, set `MAIL_ENABLED=true`
4. Create an EMAIL activity to a contact with a real email — verify the email arrives

---

### Phase 19 — Email OTP 2-Factor Authentication ✅ COMPLETE (built)

Adds mandatory email OTP on every login, optional trusted-device cookie to skip OTP for repeat logins, and HTML-formatted OTP emails.

#### New dependencies (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### New entities

**`TrustedDevice`** — `trusted_devices` table  
`id`, `userEmail` (indexed), `tokenHash` VARCHAR(64) UNIQUE, `expiresAt` (indexed), `createdAt`  
Raw UUID token in cookie; only SHA-256 hash stored in DB.

#### New services

**`OtpService`** — Generates and validates 6-digit OTPs using Caffeine in-memory cache (TTL = `otp.expiry-minutes`, default 5). Codes are single-use: invalidated on successful validation.

**`EmailService`** — Sends HTML OTP emails via `JavaMailSender` on a Spring `@Async` thread. Uses `MimeMessage` + `MimeMessageHelper`. From address must match the authenticated SMTP account (`spring.mail.username`).

**`DeviceTrustService`** — Creates and validates `DEVICE_TRUST` HttpOnly cookies:
- `createTrustToken(email)` → UUID raw token saved to cookie, SHA-256 hash persisted in DB with expiry
- `isTokenValid(rawToken, email)` → verifies hash exists, email matches, not expired
- `@Scheduled(cron = "0 0 3 * * *")` nightly cleanup of expired rows

**`AsyncConfig`** — `@Configuration @EnableAsync` to enable `@Async` on `EmailService.sendOtp()`.

#### Updated security flow

**`LoginView`** (Vaadin form, `@AnonymousAllowed`):
1. Authenticate username + password via `AuthenticationManager`
2. Look up user email via `UserService.findEmailByUsername()`
3. Read `DEVICE_TRUST` cookie → if valid, call `completeAuthentication()` and navigate to `/`
4. Otherwise: generate OTP, send HTML email, store `2fa_username` + `2fa_email` in `VaadinSession`, navigate to `verify-otp`

**`OtpVerificationView`** (`@Route("verify-otp")`, `@AnonymousAllowed`):
- `beforeEnter()`: reroutes to login if no `2fa_username` in session
- Verify button: validates OTP → completes Spring Security authentication programmatically
- "Remember this device" checkbox: creates trust token + writes HttpOnly cookie (`maxAge = days × 86400`)
- Resend button: generates new OTP and sends email (green confirmation shown)
- Masked email hint: `i*****h@gmail.com`

#### Admin email sync (`DataInitializer`)

On every startup, if `app.admin.email` differs from the stored admin email:
1. Any other user owning that email is moved to `username@crm.internal`
2. Admin's email is updated to `app.admin.email`

This resolves the chicken-and-egg problem (need email to receive OTP, need OTP to log in and set email).

#### Bug fix — `AlertService.sendAlertFull` transaction isolation

`sendAlertFull` is annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)` so that a DB constraint failure inside alert creation (e.g. duplicate dedup key) rolls back only the alert's transaction and does not poison the caller's transaction in `ScheduledTaskService.executeWithReEvaluation()`. Without this, the task's FAILED status would never be persisted.

#### Required `application.properties` settings

```properties
app.admin.email=your@email.com
otp.expiry-minutes=5
otp.length=6
device-trust.days=14
device-trust.cookie-name=DEVICE_TRUST
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx   # Gmail App Password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### Verification Phase 19

1. Start app — confirm no startup errors; check logs that admin email was synced
2. Go to `http://localhost:9080/login` — log in with `admin / admin123`
3. Check inbox for HTML OTP email with large blue code box
4. Enter code → confirm redirect to dashboard
5. Log out and log in again — check "Remember this device" → enter code → on next login, OTP step is skipped
6. Wait 14 days (or manually delete from `trusted_devices`) → OTP required again

---

### Phase 20 — Dashboard Charts ✅ COMPLETE (built)

Enhanced `DashboardView` with 7 KPI stat cards and 4 CSS horizontal bar charts. No new entities — reads from existing repositories.

#### FE Step 20.1 — Additional repository injections

Inject `OpportunityRepository` and `ContractRepository` into `DashboardView` alongside the existing ones.

#### FE Step 20.2 — KPI cards (7 total)

| Card label | Data source |
|---|---|
| Accounts | `accountRepository.count()` |
| Contacts | `contactRepository.count()` |
| Open Activities | `activityRepository.countByStatus(ActivityStatus.OPEN)` |
| New Leads | `leadRepository.countByStatus(LeadStatus.NEW)` |
| Pipeline Value | `opportunityService.sumPipelineAmount()` formatted as `"USD " + String.format("%,.0f", value)` |
| Contracts Expiring 30d | `contractService.findExpiringWithin(30).size()` |
| Win Rate | `wonOpps / (wonOpps + lostOpps)` as `"%.0f%%"` — shows `"—"` when no closed opportunities |

Each card is a `VerticalLayout` with `border-top: 3px solid <accentColor>` and `box-shadow`.

#### FE Step 20.3 — Bar chart cards (4 in a 2×2 grid)

Use a private helper `bar(label, value, max, color)` that returns a `Div` row with:
- Fixed-width label `Span` (`min-width: 110px`)
- Track `Div` (`flex: 1`, `background: #f0f4ff`, `border-radius: 4px`, `height: 18px`) containing a fill `Div` sized as `value / max * 100%`
- Bold count `Span` (`min-width: 32px`, right-aligned)

Wrap bars in `chartCard(title, bars)` — a white `Div` with `box-shadow` and a blue `font-weight: 700` title.

Arrange two `chartCard` Divs side-by-side in `chartRow(left, right)` — a `HorizontalLayout` with `setWidthFull()` and `flex: 1` on each card.

| Chart | Bars |
|---|---|
| Pipeline by Stage | Prospecting (#90caf9), Qualification (#42a5f5), Proposal (#1e88e5), Negotiation (#1565c0), Won (#43a047), Lost (#e53935) |
| Lead Funnel | New, Contacted, Qualified, Won, Lost |
| Activity Breakdown | Open (#ef6c00), In Progress (#1e88e5), Resolved (#43a047), Closed (#78909c) |
| Contract Health | Active (#43a047), Draft (#1e88e5), Expired (#e53935), Terminated (#78909c) |

Scale: max value in each chart = 100% bar width; `Math.max(1, ...)` prevents division-by-zero when all counts are 0.

#### Verification Phase 20

1. Open `http://localhost:9080/dashboard`
2. Confirm 7 KPI cards appear in the top row with colored accent borders
3. Scroll down — confirm "Sales Overview" heading and 4 chart cards appear below
4. Create WON and LOST Opportunities → confirm Win Rate card updates on next page load
5. Verify charts render correctly with no external JavaScript errors in browser console

---

### Phase 21 — Time Clock: Database Schema

Adds two new tables to the PostgreSQL schema: `attendance` (clock-in/clock-out sessions) and `holidays` (Israeli public holidays). This module lives in package `com.crm.timetracking` and is decoupled from the main CRM domain package.

#### Step 21.1 — Maven dependency

Add to `pom.xml` (needed in Phase 24 for KosherJava, add now so later phases compile cleanly):
```xml
<dependency>
    <groupId>com.kosherjava</groupId>
    <artifactId>zmanim</artifactId>
    <version>2.5.0</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

#### Step 21.2 — `attendance` table DDL

```sql
CREATE TABLE attendance (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT                   NOT NULL,
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    end_time         TIMESTAMP WITH TIME ZONE,
    duration_seconds BIGINT,
    note             TEXT,
    source           VARCHAR(32)              NOT NULL DEFAULT 'MANUAL',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_attendance_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_end_after_start
        CHECK (end_time IS NULL OR end_time > start_time),
    CONSTRAINT chk_duration_positive
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

-- Enforces at most one open session per user at the DB level
CREATE UNIQUE INDEX uq_attendance_active_session
    ON attendance (user_id) WHERE end_time IS NULL;

CREATE INDEX idx_attendance_user_start ON attendance (user_id, start_time);
CREATE INDEX idx_attendance_start_time ON attendance (start_time);
```

The partial unique index `uq_attendance_active_session` is the authoritative guard: a second `INSERT` with `end_time IS NULL` for the same `user_id` raises `DataIntegrityViolationException` even if a race condition slips past the application-layer check.

Auto-update trigger for `updated_at`:
```sql
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER attendance_set_updated_at
    BEFORE UPDATE ON attendance
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();
```

#### Step 21.3 — `holidays` table DDL

```sql
CREATE TABLE holidays (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date         DATE         NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(32)  NOT NULL DEFAULT 'PUBLIC',
    country      CHAR(2)      NOT NULL DEFAULT 'IL',
    year         SMALLINT     NOT NULL,
    credit_hours NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_holiday_date_name UNIQUE (date, name, country)
);

CREATE INDEX idx_holidays_year_country ON holidays (year, country);
CREATE INDEX idx_holidays_date ON holidays (date);
```

`credit_hours`: `8.00` for full holiday days, `4.00` for Erev (eve) days. Used by monthly report to credit employees automatically.

#### Step 21.4 — Flyway migrations (production)

Create under `src/main/resources/db/migration/`:
```
V2.0.0__create_attendance_table.sql
V2.0.1__create_holidays_table.sql
```

Enable Flyway: add `spring.flyway.enabled=true` to `application-postgres.properties`.

With `ddl-auto=update` (development), Hibernate creates the tables automatically on startup. Use Flyway for production.

#### Verification Phase 21

```sql
-- Run after first startup
SELECT table_name FROM information_schema.tables
WHERE table_name IN ('attendance', 'holidays');

SELECT indexname FROM pg_indexes
WHERE tablename = 'attendance' AND indexname = 'uq_attendance_active_session';
```

---

### Phase 22 — Time Clock: Data Layer

#### BE Step 22.1 — `Attendance` entity

Create **`com/crm/timetracking/entity/Attendance.java`**:
```
@Entity @Table(name = "attendance")
Fields:
  Long id
  @Column(name="user_id", nullable=false) Long userId      // plain FK — no @ManyToOne to stay decoupled
  @Column(name="start_time", nullable=false, columnDefinition="TIMESTAMP WITH TIME ZONE") OffsetDateTime startTime
  @Column(name="end_time", columnDefinition="TIMESTAMP WITH TIME ZONE") OffsetDateTime endTime
  @Column(name="duration_seconds") Long durationSeconds
  @Column(name="note", columnDefinition="TEXT") String note
  @Column(name="source", nullable=false, length=32) String source = "MANUAL"
  @Column(name="created_at", nullable=false, updatable=false, columnDefinition="TIMESTAMP WITH TIME ZONE") OffsetDateTime createdAt
  @Column(name="updated_at", nullable=false, columnDefinition="TIMESTAMP WITH TIME ZONE") OffsetDateTime updatedAt

@PrePersist: createdAt = updatedAt = OffsetDateTime.now()
@PreUpdate:  updatedAt = OffsetDateTime.now()

Constructor: Attendance(Long userId, OffsetDateTime startTime, String source)
```

Setters required: `setEndTime`, `setDurationSeconds`, `setNote`, `setSource`, `setStartTime` (needed for manager edits in Phase 23).

> `userId` is stored as a plain `Long` (not `@ManyToOne`) to keep the `timetracking` package decoupled from `com.crm.domain.entity.User`.

#### BE Step 22.2 — `Holiday` entity

Create **`com/crm/timetracking/entity/Holiday.java`**:
```
@Entity @Table(name = "holidays", uniqueConstraints = @UniqueConstraint(name="uq_holiday_date_name", columnNames={"date","name","country"}))
Fields:
  Long id
  @Column(name="date", nullable=false) LocalDate date
  @Column(name="name", nullable=false, length=255) String name
  @Column(name="type", nullable=false, length=32) String type = "PUBLIC"
  @Column(name="country", nullable=false, length=2) String country = "IL"
  @Column(name="year", nullable=false) Short year                       // set from date.getYear() in constructor
  @Column(name="credit_hours", nullable=false, precision=4, scale=2) BigDecimal creditHours = BigDecimal.ZERO
  @Column(name="created_at", nullable=false, updatable=false, columnDefinition="TIMESTAMP WITH TIME ZONE") OffsetDateTime createdAt

@PrePersist: createdAt = OffsetDateTime.now()

Constructor: Holiday(LocalDate date, String name, String type, String country, BigDecimal creditHours)
  → sets year = (short) date.getYear() inside constructor body
```

#### BE Step 22.3 — `AttendanceRepository`

Create **`com/crm/timetracking/repository/AttendanceRepository.java`**:
```java
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndEndTimeIsNull(Long userId);

    boolean existsByUserIdAndEndTimeIsNull(Long userId);

    @Query("""
        SELECT a FROM Attendance a
        WHERE a.userId = :userId
          AND a.startTime >= :from AND a.startTime < :to
        ORDER BY a.startTime ASC
    """)
    List<Attendance> findByUserIdAndPeriod(
        @Param("userId") Long userId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to);

    @Query("""
        SELECT a FROM Attendance a
        WHERE a.startTime >= :from AND a.startTime < :to
        ORDER BY a.userId ASC, a.startTime ASC
    """)
    List<Attendance> findAllByPeriod(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to);
}
```

#### BE Step 22.4 — `HolidayRepository`

Create **`com/crm/timetracking/repository/HolidayRepository.java`**:
```java
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByYearAndCountry(Short year, String country);
    boolean existsByDateAndNameAndCountry(LocalDate date, String name, String country);
    void deleteByYearAndCountry(Short year, String country);
    List<Holiday> findByDateBetween(LocalDate from, LocalDate to);
}
```

#### Verification Phase 22

1. `./mvnw compile` — no errors
2. Start app — confirm Hibernate creates `attendance` and `holidays` columns matching the DDL
3. Check H2/psql: `SELECT column_name FROM information_schema.columns WHERE table_name = 'attendance'`

---

### Phase 23 — Time Clock: Core Business Logic

#### BE Step 23.1 — `AttendanceService` — Punch-In

Create **`com/crm/timetracking/service/AttendanceService.java`**:

```java
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;

    public AttendanceService(AttendanceRepository attendanceRepo) {
        this.attendanceRepo = attendanceRepo;
    }

    @Transactional
    public Attendance punchIn(Long userId, String note, String source) {
        if (attendanceRepo.existsByUserIdAndEndTimeIsNull(userId)) {
            throw new IllegalStateException(
                "User " + userId + " already has an active session. Punch out first.");
        }
        Attendance session = new Attendance(
            userId, OffsetDateTime.now(), source != null ? source : "MANUAL");
        session.setNote(note);
        return attendanceRepo.save(session);
    }
}
```

**Race-condition guard:** Both an application-level check (returns a clean error message) and the DB partial unique index `uq_attendance_active_session` (raises `DataIntegrityViolationException` for concurrent inserts). Add to the global `@ControllerAdvice`:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponse> handleDuplicateSession(DataIntegrityViolationException ex) {
    if (ex.getMessage() != null && ex.getMessage().contains("uq_attendance_active_session")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_SESSION",
                "An active session already exists. Punch out first."));
    }
    throw ex;
}
```

#### BE Step 23.2 — Punch-Out

Add to `AttendanceService`:
```java
@Transactional
public Attendance punchOut(Long userId) {
    Attendance session = attendanceRepo.findByUserIdAndEndTimeIsNull(userId)
        .orElseThrow(() -> new IllegalStateException(
            "No active session found for user " + userId + ". Cannot punch out."));
    OffsetDateTime now = OffsetDateTime.now();
    session.setEndTime(now);
    session.setDurationSeconds(Duration.between(session.getStartTime(), now).getSeconds());
    return attendanceRepo.save(session);
}
```

`Duration.between(...).getSeconds()` returns whole seconds (sub-second precision discarded — sufficient for payroll).

#### BE Step 23.3 — Utility methods

```java
// Monthly records for one user — boundaries in Asia/Jerusalem timezone
@Transactional(readOnly = true)
public List<Attendance> getMonthlyRecords(Long userId, int year, int month) {
    ZoneId zone = ZoneId.of("Asia/Jerusalem");
    OffsetDateTime from = YearMonth.of(year, month).atDay(1).atStartOfDay(zone).toOffsetDateTime();
    return attendanceRepo.findByUserIdAndPeriod(userId, from, from.plusMonths(1));
}

// Manager override — corrects start/end retroactively and recalculates duration
@Transactional
public Attendance editSession(Long sessionId, OffsetDateTime newStart, OffsetDateTime newEnd) {
    Attendance session = attendanceRepo.findById(sessionId)
        .orElseThrow(() -> new EntityNotFoundException("Attendance record " + sessionId + " not found"));
    if (newEnd != null && !newEnd.isAfter(newStart))
        throw new IllegalArgumentException("end_time must be after start_time");
    session.setStartTime(newStart);
    session.setEndTime(newEnd);
    session.setDurationSeconds(
        newEnd != null ? Duration.between(newStart, newEnd).getSeconds() : null);
    return attendanceRepo.save(session);
}
```

#### BE Step 23.4 — `AttendanceController`

Create **`com/crm/timetracking/controller/AttendanceController.java`**:
```
@RestController @RequestMapping("/api/v1/attendance")

POST   /punch-in
  Body: { "note": "...", "source": "MANUAL" }
  → resolves userId from SecurityContext → attendanceService.punchIn(userId, note, source)
  Returns 201 + Attendance JSON

POST   /punch-out
  → resolves userId from SecurityContext → attendanceService.punchOut(userId)
  Returns 200 + Attendance JSON

GET    /active?userId={id}
  → attendanceRepository.findByUserIdAndEndTimeIsNull(userId)
  Returns 200 + Attendance JSON or 204 if no active session

GET    /monthly?userId={id}&year={y}&month={m}
  → attendanceService.getMonthlyRecords(userId, year, month)
  Returns List<Attendance>

PUT    /{id}
  @PreAuthorize("hasRole('ADMIN')")
  Body: { "newStart": "...", "newEnd": "..." }
  → attendanceService.editSession(id, newStart, newEnd)
```

> Use `SecurityContextHolder.getContext().getAuthentication().getName()` to get the username, then resolve `userId` via `userRepository.findByUsername(username)`. Users should only be able to punch in/out for themselves — punch endpoints ignore the request body's userId if present.

#### Verification Phase 23

1. `POST /api/v1/attendance/punch-in` → confirm row created with `end_time = NULL`
2. `POST /api/v1/attendance/punch-in` again → confirm HTTP 409 `DUPLICATE_SESSION`
3. `POST /api/v1/attendance/punch-out` → confirm `end_time` and `duration_seconds` populated
4. `POST /api/v1/attendance/punch-out` again → confirm HTTP 500 / `No active session`
5. `GET /api/v1/attendance/monthly?userId=1&year=2026&month=6` → confirm list returned

---

### Phase 24 — Time Clock: Israeli Holidays

#### BE Step 24.1 — `IsraeliHolidayService`

Create **`com/crm/timetracking/service/IsraeliHolidayService.java`**:

```java
@Service
public class IsraeliHolidayService {

    private final HolidayRepository holidayRepo;
    private static final BigDecimal FULL_DAY = new BigDecimal("8.00");
    private static final BigDecimal HALF_DAY = new BigDecimal("4.00");

    @Transactional
    public List<Holiday> generateHolidaysForYear(int gregorianYear) {
        holidayRepo.deleteByYearAndCountry((short) gregorianYear, "IL");
        List<Holiday> holidays = new ArrayList<>();
        LocalDate cursor = LocalDate.of(gregorianYear, 1, 1);
        LocalDate end    = LocalDate.of(gregorianYear + 1, 1, 1);
        while (cursor.isBefore(end)) {
            JewishCalendar jCal = new JewishCalendar(
                cursor.getYear(), cursor.getMonthValue(), cursor.getDayOfMonth());
            jCal.setInIsrael(true);  // 1-day Yom Tov (Israeli rule, not diaspora 2-day)
            String name = resolveHolidayName(jCal);
            if (name != null) {
                BigDecimal credit = isErev(jCal) ? HALF_DAY : FULL_DAY;
                holidays.add(new Holiday(cursor, name, "PUBLIC", "IL", credit));
            }
            cursor = cursor.plusDays(1);
        }
        return holidayRepo.saveAll(holidays);
    }

    private String resolveHolidayName(JewishCalendar jCal) {
        return switch (jCal.getYomTovIndex()) {
            case JewishCalendar.ROSH_HASHANA       -> "Rosh Hashanah";
            case JewishCalendar.YOM_KIPPUR         -> "Yom Kippur";
            case JewishCalendar.SUCCOS             -> "Sukkot";
            case JewishCalendar.SHEMINI_ATZERES    -> "Shemini Atzeret / Simchat Torah";
            case JewishCalendar.PESACH             -> "Pesach";
            case JewishCalendar.SHAVUOS            -> "Shavuot";
            case JewishCalendar.YOM_HAATZMAUT      -> "Yom Ha'atzmaut";
            case JewishCalendar.YOM_HAZIKARON      -> "Yom HaZikaron";
            case JewishCalendar.EREV_ROSH_HASHANA  -> "Erev Rosh Hashanah";
            case JewishCalendar.EREV_YOM_KIPPUR    -> "Erev Yom Kippur";
            case JewishCalendar.EREV_PESACH        -> "Erev Pesach";
            case JewishCalendar.EREV_SUCCOS        -> "Erev Sukkot";
            case JewishCalendar.CHOL_HAMOED_PESACH -> "Chol HaMoed Pesach";
            case JewishCalendar.CHOL_HAMOED_SUCCOS -> "Chol HaMoed Sukkot";
            default -> null;
        };
    }

    private boolean isErev(JewishCalendar jCal) {
        int i = jCal.getYomTovIndex();
        return i == JewishCalendar.EREV_ROSH_HASHANA || i == JewishCalendar.EREV_YOM_KIPPUR
            || i == JewishCalendar.EREV_PESACH       || i == JewishCalendar.EREV_SUCCOS;
    }
}
```

`jCal.setInIsrael(true)` is critical: it switches from diaspora 2-day Yom Tov to Israeli 1-day rules so dates are accurate for an Israeli company.

#### BE Step 24.2 — `HolidayRecalculationJob`

Create **`com/crm/timetracking/scheduler/HolidayRecalculationJob.java`**:
```java
@Component
public class HolidayRecalculationJob {

    private final IsraeliHolidayService holidayService;

    // Fires at 02:00 AM on October 1 every year (before the Hebrew new year)
    // Pre-computes NEXT year's holidays so the table is ready before January
    @Scheduled(cron = "0 0 2 1 10 *")
    public void recalculateUpcomingYearHolidays() {
        int nextYear = Year.now().getValue() + 1;
        var result = holidayService.generateHolidaysForYear(nextYear);
        log.info("Persisted {} Israeli holidays for year {}", result.size(), nextYear);
    }
}
```

#### BE Step 24.3 — Bootstrap on first deployment

In `DataInitializer` (or a dedicated `ApplicationRunner` bean), seed holidays for the current and next year on first startup:
```java
@Bean
ApplicationRunner bootstrapHolidays(IsraeliHolidayService svc, HolidayRepository repo) {
    return args -> {
        int year = LocalDate.now().getYear();
        if (repo.findByYearAndCountry((short) year, "IL").isEmpty()) {
            svc.generateHolidaysForYear(year);
            svc.generateHolidaysForYear(year + 1);
            log.info("Bootstrapped Israeli holidays for {} and {}", year, year + 1);
        }
    };
}
```

#### BE Step 24.4 — Holiday credit helper

Used by `MonthlyReportJob` (Phase 25) to auto-credit employees for public holidays. Israeli workweek is Sunday–Thursday; Friday and Saturday are weekend days:
```java
public BigDecimal computeHolidayCredits(int year, int month) {
    LocalDate from = LocalDate.of(year, month, 1);
    return holidayRepo.findByDateBetween(from, from.plusMonths(1)).stream()
        .filter(h -> {
            DayOfWeek dow = h.getDate().getDayOfWeek();
            return dow != DayOfWeek.FRIDAY && dow != DayOfWeek.SATURDAY;
        })
        .map(Holiday::getCreditHours)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

#### BE Step 24.5 — REST endpoints

```
GET  /api/v1/holidays?year=2026&country=IL
  → holidayRepository.findByYearAndCountry((short) year, country)
  → returns List<HolidayResponse>

POST /api/v1/holidays/regenerate?year=2026
  @PreAuthorize("hasRole('ADMIN')")
  → israeliHolidayService.generateHolidaysForYear(year)
  → returns count of persisted holidays
```

#### Verification Phase 24

1. Start app — confirm bootstrap seeds holidays for current and next year
2. `GET /api/v1/holidays?year=2026&country=IL` — confirm ~14–16 entries
3. Spot-check: Rosh Hashanah 2026 falls on 2026-09-11 (cross-check with `https://www.hebcal.com`)
4. Confirm Erev Rosh Hashanah has `credit_hours = 4.00`
5. `POST /api/v1/holidays/regenerate?year=2027` (admin JWT) → confirm new rows appear

---

### Phase 25 — Time Clock: Monthly Accountant Report

#### BE Step 25.1 — `ExcelReportService`

Create **`com/crm/timetracking/service/ExcelReportService.java`**. Generates a multi-sheet `.xlsx` workbook using Apache POI:

**Sheets produced:**
- **Summary** — one row per employee: `Employee | Total Sessions | Total Hours | Total Minutes`
- **Per-employee sheets** (named after the employee, max 31 chars, special chars replaced with `_`):  
  Columns: `Date | Clock In | Clock Out | Duration (h:mm) | Note`

**Key implementation details:**
- All times stored as UTC (`OffsetDateTime`), displayed in `Asia/Jerusalem` timezone using `atZoneSameInstant(IL_ZONE).format(...)`
- Open sessions (no `end_time`) show `"OPEN"` in the Clock Out column
- Duration format: `String.format("%d:%02d", hours, minutes)` from `durationSeconds`
- `sheet.autoSizeColumn(i)` called on all columns
- Header row styled: bold font, grey fill (`GREY_25_PERCENT`), bottom border
- Sheet names sanitized: `name.replaceAll("[\\[\\]:*?/\\\\]", "_").substring(0, Math.min(name.length(), 31))`

```java
public byte[] generateMonthlyReport(
        List<Attendance> records,
        Map<Long, String> userNames,
        String monthLabel) throws IOException {

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
        // 1. Create header cell style (bold, grey background)
        // 2. Group records by userId
        // 3. Build Summary sheet
        // 4. For each user: build per-employee detail sheet
        // 5. Add row to Summary sheet with totals
        // 6. Serialize to byte[]
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
```

See `timeclock.md Phase E.3` for the full method body.

#### BE Step 25.2 — `ReportEmailService`

Create **`com/crm/timetracking/service/ReportEmailService.java`**:

```java
@Service
public class ReportEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.reporting.accountant-email}") private String accountantEmail;
    @Value("${app.reporting.sender-email}")      private String senderEmail;

    public void sendMonthlyReport(byte[] excelBytes, String monthLabel) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(accountantEmail);
        helper.setSubject("Monthly Attendance Report — " + monthLabel);
        helper.setText("Please find attached the attendance report for " + monthLabel + ".\n\n" +
                       "This report was generated automatically by the CRM system.");
        helper.addAttachment(
            "attendance-report-" + monthLabel + ".xlsx",
            new ByteArrayResource(excelBytes),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mailSender.send(message);
    }
}
```

#### BE Step 25.3 — `MonthlyReportJob`

Create **`com/crm/timetracking/scheduler/MonthlyReportJob.java`**:

```java
@Component
public class MonthlyReportJob {

    private static final ZoneId IL_ZONE = ZoneId.of("Asia/Jerusalem");

    private final AttendanceRepository attendanceRepo;
    private final UserRepository       userRepository;
    private final ExcelReportService   excelService;
    private final ReportEmailService   emailService;

    // Fires at 06:00 AM Israel time on the 1st of every month
    // Processes the PREVIOUS month's data
    @Scheduled(cron = "0 0 6 1 * *", zone = "Asia/Jerusalem")
    public void generateAndSendMonthlyReport() {
        YearMonth lastMonth = YearMonth.now(IL_ZONE).minusMonths(1);
        OffsetDateTime from  = lastMonth.atDay(1).atStartOfDay(IL_ZONE).toOffsetDateTime();
        OffsetDateTime to    = from.plusMonths(1);
        String label = lastMonth.toString(); // "2026-05"

        List<Attendance> records = attendanceRepo.findAllByPeriod(from, to);
        if (records.isEmpty()) {
            log.warn("No attendance records for {}. Skipping report.", label);
            return;
        }

        // Resolve userId → display name from users table
        List<Long> userIds = records.stream().map(Attendance::getUserId).distinct().toList();
        Map<Long, String> names = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getUsername));

        try {
            byte[] excel = excelService.generateMonthlyReport(records, names, label);
            emailService.sendMonthlyReport(excel, label);
            log.info("Monthly attendance report for {} sent successfully.", label);
        } catch (Exception ex) {
            log.error("Monthly report job FAILED for {}: {}", label, ex.getMessage(), ex);
        }
    }
}
```

#### BE Step 25.4 — Required configuration

Add to `application-postgres.properties`:
```properties
app.reporting.accountant-email=${ACCOUNTANT_EMAIL:accountant@yourcompany.co.il}
app.reporting.sender-email=${SENDER_EMAIL:crm-reports@yourcompany.co.il}
app.reporting.timezone=Asia/Jerusalem
```

#### BE Step 25.5 — Manual trigger endpoint

Create **`com/crm/timetracking/controller/ReportAdminController.java`**:
```java
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportAdminController {

    private final MonthlyReportJob reportJob;

    @PostMapping("/monthly/trigger")
    public ResponseEntity<String> triggerManually() {
        reportJob.generateAndSendMonthlyReport();
        return ResponseEntity.ok("Monthly report job triggered.");
    }
}
```

#### BE Step 25.6 — Enable scheduling

If `@EnableScheduling` is not already on the main class or a config bean, add:
```java
@Configuration
@EnableScheduling
public class SchedulingConfig { }
```

`@EnableScheduling` is already present if Phase 15's `SubscriptionHandlerService` uses `@Scheduled`. Verify before adding a duplicate.

#### Verification Phase 25

1. Clock in and out several times with test data
2. `POST /api/v1/admin/reports/monthly/trigger` (admin JWT) → confirm 200 OK
3. Check accountant inbox — verify `.xlsx` attachment received
4. Open the file: confirm a Summary sheet plus one sheet per employee
5. Verify durations appear as `h:mm`, dates as `yyyy-MM-dd`, times as `HH:mm:ss` in Jerusalem TZ
6. Leave one session open (no punch-out) → trigger report → confirm that session shows `"OPEN"` in Clock Out column

---

## 10. Running the Application

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java (JDK) | 17 | Set `JAVA_HOME` to your JDK directory |
| Maven | via wrapper | No separate install needed — `mvnw.cmd` is included |
| Docker | any recent | Required for the PostgreSQL container (default profile) |

**Set JAVA_HOME before any Maven command (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.5"   # adjust to your path
```

**Find your JDK path:**
```powershell
Get-ChildItem "C:\Program Files\Java" | Select-Object Name
```

---

### Step 1 — Start the PostgreSQL Docker container

The app uses PostgreSQL by default (profile `postgres`). A dedicated Docker container is required.

**First-time setup — create and start the container:**
```powershell
docker run -d `
  --name crm-postgres `
  -e POSTGRES_DB=crmdb `
  -e POSTGRES_USER=crm `
  -e POSTGRES_PASSWORD=crm123 `
  -p 5433:5432 `
  postgres:14.4
```

**After every machine restart — just start the existing container:**
```powershell
docker start crm-postgres
```

**Verify it is running:**
```powershell
docker ps --filter name=crm-postgres
```

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5433` |
| Database | `crmdb` |
| Username | `crm` |
| Password | `crm123` |

Hibernate creates all 18 tables automatically on first startup (`ddl-auto=update`). Data persists across app restarts.

---

### Step 2 — Run the application (Windows / PowerShell)

The Maven wrapper must be invoked via Java directly on Windows due to a PATH quirk with `mvnw.cmd`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.5"
$java = "$env:JAVA_HOME\bin\java.exe"
$jar  = ".\.mvn\wrapper\maven-wrapper.jar"

& $java -cp $jar org.apache.maven.wrapper.MavenWrapperMain `
    "-Dmaven.multiModuleProjectDirectory=$PWD" `
    spring-boot:run
```

Once started (takes ~30–60 s on first run while Vaadin downloads front-end resources):

| URL | Purpose |
|---|---|
| http://localhost:9080 | Main UI — log in as `admin` / `admin123` |
| http://localhost:9080/swagger-ui.html | REST API docs |

> **Shortcut** — if you have `mvn` on your PATH (standalone Maven install):
> ```powershell
> mvn spring-boot:run
> ```

---

### Run the application (Linux / macOS / WSL)

```bash
docker start crm-postgres          # ensure the container is running
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk   # adjust to your path
./mvnw spring-boot:run
```

---

### Switch back to H2 (no Docker needed)

```powershell
# Temporarily override the profile for a single run
& $java -cp $jar org.apache.maven.wrapper.MavenWrapperMain `
    "-Dmaven.multiModuleProjectDirectory=$PWD" `
    spring-boot:run "-Dspring.profiles.active=dev"
```

H2 console is available at http://localhost:9080/h2-console (`jdbc:h2:mem:crmdb`) when using the `dev` profile.

---

### Production — PostgreSQL (external server)

```powershell
# 1. Create database on your server
psql -c "CREATE USER crm WITH PASSWORD 'yourpassword';"
psql -c "CREATE DATABASE crmdb OWNER crm;"

# 2. Set environment variables
$env:DATABASE_URL      = "jdbc:postgresql://localhost:5432/crmdb"
$env:DATABASE_USER     = "crm"
$env:DATABASE_PASSWORD = "yourpassword"
$env:JWT_SECRET        = "<base64-256-bit-secret>"   # see generator below

# 3. Build and run
& $java -cp $jar org.apache.maven.wrapper.MavenWrapperMain `
    "-Dmaven.multiModuleProjectDirectory=$PWD" `
    clean package -DskipTests -Pproduction

java -jar target\crm-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Generate JWT Secret (PowerShell)

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

---

## 11. Configuration Reference

### `application.properties`

| Property | Value | Notes |
|---|---|---|
| `server.port` | `9080` | Avoids conflict with OpenCRX on 9090 |
| `spring.profiles.active` | `postgres` | `dev` for H2, `prod` for external PostgreSQL |
| `app.jwt.expiration-ms` | `86400000` | 24 hours |
| `spring.jpa.open-in-view` | `false` | |
| `app.admin.email` | `admin@crm.com` | Synced to the `admin` user on every startup; conflicts with existing users are resolved automatically |
| `otp.expiry-minutes` | `5` | Caffeine TTL for OTP codes |
| `otp.length` | `6` | Digits in the generated OTP |
| `device-trust.days` | `14` | How long a trusted-device cookie is valid |
| `device-trust.cookie-name` | `DEVICE_TRUST` | HttpOnly cookie name |
| `spring.mail.host` | `smtp.gmail.com` | SMTP server |
| `spring.mail.port` | `587` | STARTTLS port |
| `spring.mail.username` | — | Gmail address (also used as `From`) |
| `spring.mail.password` | — | Gmail App Password (16-char, spaces OK) |
| `app.reporting.accountant-email` | `accountant@yourcompany.co.il` | Monthly attendance report recipient (Phase 25) |
| `app.reporting.sender-email` | `crm-reports@yourcompany.co.il` | From address for accountant reports (Phase 25) |
| `app.reporting.timezone` | `Asia/Jerusalem` | Timezone for report month boundaries and display (Phase 25) |

### `application-dev.properties`

| Property | Value |
|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:crmdb` |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` |
| `spring.h2.console.enabled` | `true` |

### `application-prod.properties`

| Property | Env Var |
|---|---|
| `spring.datasource.url` | `DATABASE_URL` |
| `spring.datasource.username` | `DATABASE_USER` |
| `spring.datasource.password` | `DATABASE_PASSWORD` |
| `app.jwt.secret` | `JWT_SECRET` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |

---

## 12. Testing

### Run all tests (Windows / PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.5"   # adjust to your path
$java = "$env:JAVA_HOME\bin\java.exe"
$jar  = ".\mvn\wrapper\maven-wrapper.jar"

& $java -cp $jar org.apache.maven.wrapper.MavenWrapperMain `
    "-Dmaven.multiModuleProjectDirectory=$PWD" `
    test
```

### Run a single test class

```powershell
& $java -cp $jar org.apache.maven.wrapper.MavenWrapperMain `
    "-Dmaven.multiModuleProjectDirectory=$PWD" `
    test -Dtest=AccountServiceTest
```

### Run tests (Linux / macOS / WSL)

```bash
./mvnw test
./mvnw test -Dtest=AccountServiceTest
```

### Existing test classes

| Class | Type | What it covers |
|---|---|---|
| `CrmApplicationTests` | Context load | Spring context starts without errors |
| `AccountServiceTest` | Unit (Mockito) | `create`, `findById`, `delete` |
| `ContactRepositoryTest` | `@DataJpaTest` (H2) | `findByEmail`, `findByAccount_Id` |

Test reports are written to `target/surefire-reports/` after each run.

### Existing Tests

| Class | Type | Coverage |
|---|---|---|
| `AccountServiceTest` | Unit (Mockito) | create, findById, delete |
| `ContactRepositoryTest` | `@DataJpaTest` | findByEmail, findByAccount_Id |
| `CrmApplicationTests` | Context load | starts |

### Test Convention for New Modules

For each new service (e.g., `LeadService`), add `LeadServiceTest`:
- `@ExtendWith(MockitoExtension.class)`
- Mock the repository with `@Mock`
- Test: create (happy path), create (validation error), findById (not found → exception), update, delete

For each new repository, add `LeadRepositoryTest`:
- `@DataJpaTest` — H2 auto-configured
- Test each custom query method with test data using `@BeforeEach` saves

---

## 13. Default Credentials

Seeded on first boot by `DataInitializer`:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Email | Configurable via `app.admin.email` (default `admin@crm.com`). On every startup the admin user's email is synced to this value; any other user that already owns that email is moved to `username@crm.internal`. |
| Roles | `ROLE_USER`, `ROLE_ADMIN` |

**Change this password before exposing to any network.**

> **2FA note:** Login sends a 6-digit OTP to the admin email via SMTP. Configure `spring.mail.*` and `app.admin.email` before first login. Check "Remember this device" to skip OTP for 14 days on the same browser.

---

## OpenCRX Reference

`http://localhost:9090/opencrx-core-CRX`

| OpenCRX Tab | Route | Phase |
|---|---|---|
| Contacts → Accounts | `/accounts` | 1 ✅ |
| Contacts → Account Groups | `/account-groups` | 2 ✅ |
| Contacts → Addresses | `/addresses` | 2 ✅ |
| Support → Bugs + Features | `/activities` | 3 ✅ |
| Sales → Leads | `/leads` | 4 ✅ |
| Sales → Opportunities | `/opportunities` | 5 ✅ |
| Sales → Quotes | `/quotes` | 6 ✅ |
| Sales → Sales Orders | `/sales-orders` | 7 ✅ |
| Sales → Contracts | `/contracts` | 7 ✅ |
| Sales → Forecast | `/forecast` | 8 ✅ |
| Workspaces → Default | `/workspaces` | 9 ✅ |
| Activity → Follow-up Notes | activity notes timeline | 11 |
| Activity → 11 Types | EMAIL, SALES_VISIT, MAILING, SMS, ABSENCE added | 11 |
| Products / Price List | `/products` | 12 |
| Admin → Managing Users | `/users` (admin only) | 13 |
| Data Import/Export | CSV import/export on grid views | 14 |
| Subscribe/Notify → Alerts | `Alert` (NEW/READ/ACCEPTED/EXPIRED) + `Subscription` (5 named filters, event types) + `Topic` (11 standard) + `AuditLog` + `SubscriptionHandlerService` + bell badge + `SubscriptionsView` | 15 |
| Documents / Attachments | file upload on any entity | 16 |
| Calendar / Meetings | `/calendar` (month grid + .ics export) | 17 |
| E-Mail Services | Spring Mail + EMAIL activity type | 18 |
| Security → 2FA | Email OTP login + trusted device cookie + HTML email | 19 ✅ |
| Dashboard Charts | CSS bar charts: Pipeline, Lead Funnel, Activity, Contract Health + Win Rate KPI | 20 ✅ |
| Time Clock → Clock In/Out | `attendance` table + punchIn/punchOut + dedup index | 21–23 |
| Time Clock → Israeli Holidays | KosherJava + `holidays` table + annual cron recalc | 24 |
| Time Clock → Accountant Report | Apache POI `.xlsx` + monthly cron + email attachment | 25 |
| **Time Clock** | Manual Attendance Reports — Hilan "עדכון נוכחות" | `AttendanceReport` + `AttendanceReportType` enum + `AttendanceReportService` + `DurationCalculator` + `AttendanceCalendarView` + `AttendanceReportEditor` (Phase F–G) |


---

### Phase F — Manual Attendance Reporting: Data Layer Extensions

#### Design Decision: New Table vs. Extending `attendance`

The existing `attendance` table (Phases 21–22) models **clock sessions**: a single open row per user, entered implicitly by the punch-in/punch-out clock. Its invariants — the partial unique index `uq_attendance_active_session`, the `CHECK (end_time > start_time)`, and the `source` field — are tightly coupled to the clock workflow.

The new manual-reporting module models **declared attendance records**: one or more explicit entries per `(user_id, date)`, entered by an HR user or the employee themselves, possibly carrying no clock times at all (e.g., a vacation day). The semantics differ enough that merging them into `attendance` would either break existing Phase 23 constraints or require a discriminator column that pollutes every existing query.

**Decision: introduce a new `attendance_report` table linked to `users.id`.** The two tables coexist independently. `MonthlyReportJob` (Phase 25) is extended to read both (see Integration note at the end of Phase G).

---

#### BE Step F.1 — `attendance_report` Table DDL

Flyway script: `V2.2.0__create_attendance_report_table.sql`

```sql
CREATE TABLE attendance_report (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    report_date        DATE        NOT NULL,
    entry_time         TIME,                           -- NULL allowed for absence-style types
    exit_time          TIME,                           -- NULL allowed for absence-style types
    duration_minutes   INT         CHECK (duration_minutes IS NULL OR duration_minutes >= 0),
    note               TEXT,
    report_type        VARCHAR(32) NOT NULL DEFAULT 'PRESENCE',
    equate_to_standard BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_attendance_report_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    -- PRESENCE reports must supply both clock times; absence-style types may omit them
    CONSTRAINT chk_presence_requires_times
        CHECK (
            report_type != 'PRESENCE'
            OR (entry_time IS NOT NULL AND exit_time IS NOT NULL)
        )
);

-- Primary query pattern: all reports for a user within a given month
CREATE INDEX idx_ar_user_date      ON attendance_report (user_id, report_date);
-- Secondary: admin queries across all users by date range
CREATE INDEX idx_ar_date           ON attendance_report (report_date);
-- Covering index for the monthly calendar GROUP BY aggregation
CREATE INDEX idx_ar_user_date_type ON attendance_report (user_id, report_date, report_type);
```

**Field rationale:**
- `report_date DATE` — the calendar day being reported. Deliberately `DATE`, not `TIMESTAMP`, to avoid timezone ambiguity for a date-only concept.
- `entry_time TIME` / `exit_time TIME` — wall-clock times without a timezone component. The service interprets them as `Asia/Jerusalem` local times. Pure `TIME` (no TZ) avoids DST rewrites: Israeli DST should not alter a historical "arrived at 08:00" record.
- `duration_minutes INT` — stored in whole minutes (not seconds) for readability in SQL and in the Excel report. Maximum representable single shift: 1439 min (23 h 59 m).
- `equate_to_standard BOOLEAN` — Hilan "השוואה לתקן": when `true`, the day counts as a full standard workday regardless of actual clock times.

Reuse the existing auto-update trigger function defined in Phase 21:

```sql
CREATE TRIGGER attendance_report_set_updated_at
    BEFORE UPDATE ON attendance_report
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();
```

Flyway script `V2.2.1__add_report_type_check_constraint.sql`:

```sql
ALTER TABLE attendance_report
    ADD CONSTRAINT chk_report_type_valid
    CHECK (report_type IN (
        'PRESENCE', 'VACATION', 'SICK', 'RESERVE_DUTY', 'HOLIDAY', 'ABSENCE'
    ));
```

---

#### BE Step F.2 — `AttendanceReportType` Enum

Create **`com/crm/timetracking/enums/AttendanceReportType.java`**:

```java
public enum AttendanceReportType {

    //                    hebrewLabel   countsAsWorked  creditsStandardHours
    PRESENCE     ("נוכחות",   true,  false),
    VACATION     ("חופשה",    false, true),
    SICK         ("מחלה",     false, true),
    RESERVE_DUTY ("מילואים",  false, true),
    HOLIDAY      ("חג",       false, true),
    ABSENCE      ("היעדרות",  false, false);

    private final String  hebrewLabel;
    /**
     * True only for PRESENCE: actual clock-time worked counts toward "hours worked" totals.
     * All other types use creditsStandardHours instead.
     */
    private final boolean countsAsWorked;
    /**
     * True for VACATION/SICK/RESERVE_DUTY/HOLIDAY: the day receives a credit equal to
     * the standard workday hours (ה.לתקן), regardless of actual clock time.
     */
    private final boolean creditsStandardHours;

    AttendanceReportType(String hebrewLabel,
                         boolean countsAsWorked,
                         boolean creditsStandardHours) {
        this.hebrewLabel          = hebrewLabel;
        this.countsAsWorked       = countsAsWorked;
        this.creditsStandardHours = creditsStandardHours;
    }

    public String  getHebrewLabel()          { return hebrewLabel; }
    public boolean isCountsAsWorked()        { return countsAsWorked; }
    public boolean isCreditsStandardHours()  { return creditsStandardHours; }
}
```

No separate `report_type` DB table is needed: the six values are a closed set defined by Israeli labour-law and Hilan conventions. The `CHECK` constraint in Step F.1 provides DB-level enforcement.

Seed data summary (for documentation and reference):

| Enum Constant | Hebrew Label | countsAsWorked | creditsStandardHours |
|---|---|---|---|
| `PRESENCE` | נוכחות | true | false |
| `VACATION` | חופשה | false | true |
| `SICK` | מחלה | false | true |
| `RESERVE_DUTY` | מילואים | false | true |
| `HOLIDAY` | חג | false | true |
| `ABSENCE` | היעדרות | false | false |

---

#### BE Step F.3 — Duration Calculation: Midnight-Crossing Shifts

Create **`com/crm/timetracking/util/DurationCalculator.java`**:

```java
public final class DurationCalculator {

    private DurationCalculator() {}

    /**
     * Computes the duration in whole minutes between two LocalTime values.
     *
     * Handles midnight-crossing shifts by adding 1440 minutes when the raw
     * Duration is negative.  This correctly models a single midnight crossing.
     *
     * Examples:
     *   entry=08:00, exit=17:00  -->  540 min  (9 h, normal shift)
     *   entry=22:00, exit=00:00  -->  120 min  (2 h, midnight-crossing)
     *   entry=22:00, exit=00:30  -->  150 min  (2 h 30 m, midnight-crossing)
     *
     * Precondition: the caller must ensure duration is less than 1440 min;
     * the service validator rejects duration >= 1440.
     */
    public static int computeMinutes(LocalTime entry, LocalTime exit) {
        int minutes = (int) Duration.between(entry, exit).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;   // 1440 added once for a single midnight crossing
        }
        return minutes;
    }

    /** Formats total minutes as "H:mm" for display in Vaadin and Excel. */
    public static String formatMinutes(int totalMinutes) {
        return String.format("%d:%02d", totalMinutes / 60, totalMinutes % 60);
    }
}
```

**Verification:**
- `Duration.between(LocalTime.of(22,0), LocalTime.of(0,0)).toMinutes()` = `-1320` → `-1320 + 1440` = **120** ✓
- `Duration.between(LocalTime.of(8,0), LocalTime.of(17,0)).toMinutes()` = **540** ✓

---

#### BE Step F.4 — `AttendanceReport` Entity

Create **`com/crm/timetracking/entity/AttendanceReport.java`**:

```java
@Entity
@Table(
    name = "attendance_report",
    indexes = {
        @Index(name = "idx_ar_user_date",      columnList = "user_id, report_date"),
        @Index(name = "idx_ar_user_date_type", columnList = "user_id, report_date, report_type")
    }
)
public class AttendanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain Long -- mirrors Attendance.userId pattern (Phase 22) to keep
    // the timetracking package decoupled from com.crm.domain.entity.User.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "entry_time")
    private LocalTime entryTime;        // null for absence-style types

    @Column(name = "exit_time")
    private LocalTime exitTime;         // null for absence-style types

    // Stored in minutes; null when no clock times are present; computed on every save.
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 32)
    private AttendanceReportType reportType = AttendanceReportType.PRESENCE;

    @Column(name = "equate_to_standard", nullable = false)
    private boolean equateToStandard = false;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = OffsetDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public AttendanceReport() {}

    // Getters and setters for all fields (no Lombok -- follow existing entity pattern)
}
```

---

#### BE Step F.5 — `AttendanceReportRepository`

Create **`com/crm/timetracking/repository/AttendanceReportRepository.java`**:

```java
@Repository
public interface AttendanceReportRepository extends JpaRepository<AttendanceReport, Long> {

    /** All reports for one user on one day, ordered by entry time ascending. */
    List<AttendanceReport> findByUserIdAndReportDateOrderByEntryTimeAscIdAsc(
            Long userId, LocalDate reportDate);

    /**
     * All reports for a user within an inclusive date range.
     * Used by getMonthlyCalendar and the Excel report extension.
     */
    @Query("""
        SELECT r FROM AttendanceReport r
        WHERE r.userId     = :userId
          AND r.reportDate >= :from
          AND r.reportDate <= :to
        ORDER BY r.reportDate ASC, r.entryTime ASC NULLS LAST, r.id ASC
    """)
    List<AttendanceReport> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to);

    /**
     * Per-day duration sums for calendar cell totals.
     * Returns Object[]{LocalDate reportDate, Long sumDurationMinutes}.
     */
    @Query("""
        SELECT r.reportDate, SUM(r.durationMinutes)
        FROM AttendanceReport r
        WHERE r.userId          = :userId
          AND r.reportDate      >= :from
          AND r.reportDate      <= :to
          AND r.durationMinutes IS NOT NULL
        GROUP BY r.reportDate
        ORDER BY r.reportDate ASC
    """)
    List<Object[]> sumWorkedMinutesByDay(
            @Param("userId") Long userId,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to);

    /** All reports for ALL users within a date range -- used by ExcelReportService. */
    @Query("""
        SELECT r FROM AttendanceReport r
        WHERE r.reportDate >= :from
          AND r.reportDate <= :to
        ORDER BY r.userId ASC, r.reportDate ASC, r.entryTime ASC NULLS LAST
    """)
    List<AttendanceReport> findAllByDateRange(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);
}
```

---

### Phase F — Manual Attendance Reporting: Service Layer

#### BE Step F.6 — DTOs

**`AttendanceReportRequest`** record (`com/crm/timetracking/dto/AttendanceReportRequest.java`):

```java
public record AttendanceReportRequest(
    @NotNull LocalDate            reportDate,
    LocalTime                     entryTime,       // null for absence-style types
    LocalTime                     exitTime,        // null for absence-style types
    String                        note,
    @NotNull AttendanceReportType reportType,
    boolean                       equateToStandard
) {}
```

**`AttendanceReportResponse`** record:

```java
public record AttendanceReportResponse(
    Long                 id,
    Long                 userId,
    LocalDate            reportDate,
    LocalTime            entryTime,
    LocalTime            exitTime,
    Integer              durationMinutes,
    String               durationFormatted,  // "H:mm" or null
    String               note,
    AttendanceReportType reportType,
    String               reportTypeLabel,    // enum.getHebrewLabel()
    boolean              equateToStandard,
    OffsetDateTime       createdAt
) {
    public static AttendanceReportResponse from(AttendanceReport r) {
        return new AttendanceReportResponse(
            r.getId(), r.getUserId(), r.getReportDate(),
            r.getEntryTime(), r.getExitTime(),
            r.getDurationMinutes(),
            r.getDurationMinutes() != null
                ? DurationCalculator.formatMinutes(r.getDurationMinutes()) : null,
            r.getNote(),
            r.getReportType(), r.getReportType().getHebrewLabel(),
            r.isEquateToStandard(), r.getCreatedAt()
        );
    }
}
```

**`DayCalendarEntry`** record (one entry per calendar day inside `MonthlyCalendarResponse`):

```java
public record DayCalendarEntry(
    LocalDate                      date,
    List<AttendanceReportResponse> reports,
    int                            totalWorkedMinutes,  // computed by service
    int                            standardMinutes,     // expected hours for this day
    boolean                        isWeekend,
    boolean                        isHoliday,
    String                         holidayName,         // null if not a holiday
    int                            deltaMinutes         // totalWorkedMinutes - standardMinutes
) {}
```

**`MonthlyCalendarResponse`** record:

```java
public record MonthlyCalendarResponse(
    Long                   userId,
    String                 username,
    int                    year,
    int                    month,
    List<DayCalendarEntry> days,
    int                    totalWorkedMinutes,
    int                    totalStandardMinutes,
    int                    totalDeltaMinutes
) {}
```

---

#### BE Step F.7 — `AttendanceReportService`

Create **`com/crm/timetracking/service/AttendanceReportService.java`**:

```java
@Service
@Transactional
public class AttendanceReportService {

    // Standard workday in minutes (8 hours).
    private static final int DEFAULT_STANDARD_MINUTES = 480;

    private final AttendanceReportRepository reportRepo;
    private final HolidayRepository          holidayRepo;   // Phase 22 -- already built
    private final UserRepository             userRepo;

    public AttendanceReportService(AttendanceReportRepository reportRepo,
                                   HolidayRepository holidayRepo,
                                   UserRepository userRepo) {
        this.reportRepo  = reportRepo;
        this.holidayRepo = holidayRepo;
        this.userRepo    = userRepo;
    }

    // ---- CREATE -------------------------------------------------------------

    public AttendanceReportResponse createReport(Long userId, AttendanceReportRequest req) {
        validate(req, /* excludeId */ null, userId);
        AttendanceReport report = new AttendanceReport();
        applyRequest(report, userId, req);
        return AttendanceReportResponse.from(reportRepo.save(report));
    }

    // ---- EDIT ---------------------------------------------------------------

    public AttendanceReportResponse editReport(Long reportId, AttendanceReportRequest req) {
        AttendanceReport report = getOrThrow(reportId);
        validate(req, reportId, report.getUserId());
        applyRequest(report, report.getUserId(), req);
        return AttendanceReportResponse.from(reportRepo.save(report));
    }

    // ---- DELETE -------------------------------------------------------------

    public void deleteReport(Long reportId) {
        reportRepo.delete(getOrThrow(reportId));
    }

    // ---- DAY QUERY ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AttendanceReportResponse> getReportsForDay(Long userId, LocalDate date) {
        return reportRepo
            .findByUserIdAndReportDateOrderByEntryTimeAscIdAsc(userId, date)
            .stream().map(AttendanceReportResponse::from).toList();
    }

    // ---- MONTHLY CALENDAR ---------------------------------------------------

    /**
     * Returns a full monthly calendar for one user.
     *
     * Israeli workweek: Sunday-Thursday.  Friday and Saturday are non-work days
     * (standardMinutes = 0 on those days).  Holiday credit comes from the holidays
     * table populated by IsraeliHolidayService (Phase 24).
     */
    @Transactional(readOnly = true)
    public MonthlyCalendarResponse getMonthlyCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate from      = yearMonth.atDay(1);
        LocalDate to        = yearMonth.atEndOfMonth();

        // 1. All manual reports for the month -- one DB round-trip
        List<AttendanceReport> reports = reportRepo.findByUserIdAndDateRange(userId, from, to);
        Map<LocalDate, List<AttendanceReport>> byDate = reports.stream()
            .collect(Collectors.groupingBy(
                AttendanceReport::getReportDate, LinkedHashMap::new, Collectors.toList()));

        // 2. Holidays for the month -- one DB round-trip (Phase 22 repo reused)
        Map<LocalDate, Holiday> holidayMap =
            holidayRepo.findByDateBetween(from, to).stream()
                       .collect(Collectors.toMap(Holiday::getDate, h -> h, (a, b) -> a));

        // 3. Resolve display name
        String username = userRepo.findById(userId)
            .map(User::getUsername).orElse("unknown");

        // 4. Build one DayCalendarEntry per calendar day
        List<DayCalendarEntry> days = new ArrayList<>();
        int totalWorked   = 0;
        int totalStandard = 0;

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayOfWeek dow       = d.getDayOfWeek();
            boolean   isWeekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY);
            Holiday   holiday   = holidayMap.get(d);
            boolean   isHoliday = holiday != null;
            int       standard  = resolveStandardMinutes(isWeekend, isHoliday, holiday);

            List<AttendanceReport> dayReports = byDate.getOrDefault(d, List.of());
            int worked = computeWorkedMinutes(dayReports, standard);

            days.add(new DayCalendarEntry(
                d,
                dayReports.stream().map(AttendanceReportResponse::from).toList(),
                worked, standard, isWeekend, isHoliday,
                isHoliday ? holiday.getName() : null,
                worked - standard));

            totalWorked   += worked;
            totalStandard += standard;
        }

        return new MonthlyCalendarResponse(
            userId, username, year, month, days,
            totalWorked, totalStandard, totalWorked - totalStandard);
    }

    // ---- PRIVATE HELPERS ----------------------------------------------------

    private void validate(AttendanceReportRequest req, Long excludeId, Long userId) {
        if (req.reportType() == AttendanceReportType.PRESENCE
                && (req.entryTime() == null || req.exitTime() == null)) {
            throw new AttendanceValidationException(
                "סוג דיווח נוכחות דורש שעת כניסה ושעת יציאה");
        }

        if (req.entryTime() != null && req.exitTime() != null) {
            int minutes = DurationCalculator.computeMinutes(req.entryTime(), req.exitTime());

            if (minutes == 0) {
                throw new AttendanceValidationException(
                    "שעת כניסה ויציאה זהות -- המשך חייב להיות גדול מאפס");
            }
            if (minutes >= 24 * 60) {
                throw new AttendanceValidationException(
                    "משך משמרת חייב להיות קצר מ-24 שעות");
            }
            if (req.reportType() == AttendanceReportType.PRESENCE) {
                checkNoPresenceOverlap(userId, req.reportDate(),
                                       req.entryTime(), req.exitTime(),
                                       excludeId != null ? excludeId : -1L);
            }
        }
    }

    /**
     * Loads all PRESENCE reports for (userId, date), excludes the record being
     * edited, then checks pairwise interval overlap using minute-of-day arithmetic
     * so midnight-crossing shifts are handled correctly.
     */
    private void checkNoPresenceOverlap(Long userId, LocalDate date,
                                        LocalTime entry, LocalTime exit,
                                        Long excludeId) {
        reportRepo.findByUserIdAndReportDateOrderByEntryTimeAscIdAsc(userId, date)
            .stream()
            .filter(r -> r.getReportType() == AttendanceReportType.PRESENCE)
            .filter(r -> !r.getId().equals(excludeId))
            .filter(r -> r.getEntryTime() != null && r.getExitTime() != null)
            .forEach(r -> {
                if (timeRangesOverlap(entry, exit, r.getEntryTime(), r.getExitTime())) {
                    throw new AttendanceValidationException(
                        "קיימת חפיפה עם דיווח נוכחות אחר באותו יום");
                }
            });
    }

    /**
     * Returns true when two time ranges overlap.  Converts each range to
     * minute-of-day [0, 2879) so midnight-crossing (entry > exit) is handled
     * by adding 1440 to the end endpoint.
     */
    private boolean timeRangesOverlap(LocalTime s1, LocalTime e1,
                                      LocalTime s2, LocalTime e2) {
        int start1 = s1.toSecondOfDay() / 60;
        int end1   = s1.isAfter(e1) ? e1.toSecondOfDay() / 60 + 1440
                                    : e1.toSecondOfDay() / 60;
        int start2 = s2.toSecondOfDay() / 60;
        int end2   = s2.isAfter(e2) ? e2.toSecondOfDay() / 60 + 1440
                                    : e2.toSecondOfDay() / 60;
        return !(end1 <= start2 || end2 <= start1);
    }

    /** Populates all mutable fields from an inbound request and recomputes duration. */
    private void applyRequest(AttendanceReport report, Long userId,
                              AttendanceReportRequest req) {
        report.setUserId(userId);
        report.setReportDate(req.reportDate());
        report.setEntryTime(req.entryTime());
        report.setExitTime(req.exitTime());
        report.setNote(req.note());
        report.setReportType(req.reportType());
        report.setEquateToStandard(req.equateToStandard());
        report.setDurationMinutes(
            (req.entryTime() != null && req.exitTime() != null)
                ? DurationCalculator.computeMinutes(req.entryTime(), req.exitTime())
                : null);
    }

    /**
     * Resolves expected (standard) minutes for one calendar day:
     * - Weekend (Fri/Sat): 0
     * - Holiday on a workday: holiday.creditHours * 60
     * - Normal workday: DEFAULT_STANDARD_MINUTES (480)
     */
    private int resolveStandardMinutes(boolean isWeekend, boolean isHoliday, Holiday holiday) {
        if (isWeekend) return 0;
        if (isHoliday) return holiday.getCreditHours()
                                     .multiply(BigDecimal.valueOf(60)).intValue();
        return DEFAULT_STANDARD_MINUTES;
    }

    /**
     * Computes total "counted" minutes for one calendar day:
     * - PRESENCE: adds actual durationMinutes
     * - creditsStandardHours types (VACATION/SICK/RESERVE_DUTY/HOLIDAY): credits
     *   standardMinutes once (first occurrence wins; subsequent ones are ignored)
     * - ABSENCE: contributes 0
     * - equateToStandard: overrides to standardMinutes, applied once
     */
    private int computeWorkedMinutes(List<AttendanceReport> reports, int standardMinutes) {
        int     worked           = 0;
        boolean standardCredited = false;

        for (AttendanceReport r : reports) {
            if (r.isEquateToStandard() && !standardCredited) {
                worked += standardMinutes;
                standardCredited = true;
                continue;
            }
            if (r.getReportType().isCountsAsWorked() && r.getDurationMinutes() != null) {
                worked += r.getDurationMinutes();
            } else if (r.getReportType().isCreditsStandardHours() && !standardCredited) {
                worked += standardMinutes;
                standardCredited = true;
            }
        }
        return worked;
    }

    private AttendanceReport getOrThrow(Long id) {
        return reportRepo.findById(id).orElseThrow(
            () -> new EntityNotFoundException("AttendanceReport " + id + " not found"));
    }
}
```

---

#### BE Step F.8 — Exception and HTTP Mapping

Create **`com/crm/timetracking/exception/AttendanceValidationException.java`**:

```java
public class AttendanceValidationException extends RuntimeException {
    public AttendanceValidationException(String message) { super(message); }
}
```

Add to the existing **`GlobalExceptionHandler`** (`@ControllerAdvice`):

```java
@ExceptionHandler(AttendanceValidationException.class)
public ResponseEntity<ErrorResponse> handleAttendanceValidation(
        AttendanceValidationException ex) {
    // 422: the request is well-formed but fails semantic validation
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse("ATTENDANCE_VALIDATION", ex.getMessage()));
}
```

422 Unprocessable Entity is more precise than 400 for semantic errors on syntactically valid requests, consistent with RFC 9110.

---

#### BE Step F.9 — `AttendanceReportController`

Create **`com/crm/timetracking/controller/AttendanceReportController.java`**:

```
@RestController @RequestMapping("/api/v1/attendance-reports")

POST   /
  Body: AttendanceReportRequest
  Resolves userId from SecurityContext (own reports);
  ADMIN may pass ?userId= to create on behalf of another user.
  Returns 201 + AttendanceReportResponse

GET    /?userId={id}&date={date}
  --> attendanceReportService.getReportsForDay(userId, date)
  Returns List<AttendanceReportResponse>

GET    /calendar?userId={id}&year={y}&month={m}
  --> attendanceReportService.getMonthlyCalendar(userId, year, month)
  Returns MonthlyCalendarResponse

PUT    /{id}
  Body: AttendanceReportRequest
  --> attendanceReportService.editReport(id, req)
  Returns 200 + AttendanceReportResponse

DELETE /{id}
  --> attendanceReportService.deleteReport(id)
  Returns 204
```

Security guard: if the calling user is not `ROLE_ADMIN` and the target `userId` differs from the authenticated user's ID, throw `AccessDeniedException` (maps to 403).

---

#### BE Step F.10 — Project Structure Update

Add under `timetracking/` in Section 4:

```
timetracking/
├── entity/
│   └── AttendanceReport.java             # Phase F
├── enums/
│   └── AttendanceReportType.java         # Phase F
├── repository/
│   └── AttendanceReportRepository.java   # Phase F
├── service/
│   └── AttendanceReportService.java      # Phase F
├── util/
│   └── DurationCalculator.java           # Phase F -- midnight-crossing helper
├── dto/
│   ├── AttendanceReportRequest.java
│   ├── AttendanceReportResponse.java
│   ├── DayCalendarEntry.java
│   └── MonthlyCalendarResponse.java
└── controller/
    └── AttendanceReportController.java   # Phase F
```

#### Verification Phase F

1. `./mvnw compile` — no errors
2. Start app — confirm Hibernate creates `attendance_report` with all columns and indexes
3. `POST /api/v1/attendance-reports` with `reportType=PRESENCE, entryTime=08:00, exitTime=17:00` → confirm `durationMinutes=540`
4. Same call with `entryTime=22:00, exitTime=00:00` → confirm `durationMinutes=120` (midnight-crossing)
5. Second overlapping PRESENCE report for same user/day → confirm HTTP 422 with Hebrew error
6. `POST` with `reportType=VACATION, entryTime=null, exitTime=null` → confirm 201
7. `POST` with `reportType=PRESENCE, entryTime=null` → confirm HTTP 422
8. `GET /api/v1/attendance-reports/calendar?userId=1&year=2026&month=6` → confirm 30 `DayCalendarEntry` items; Friday/Saturday have `standardMinutes=0`; a holiday day shows `holidayName` and `isHoliday=true`

---

### Phase G — Manual Attendance Reporting: Vaadin UI

#### FE Step G.1 — `AttendanceCalendarView`

Create **`com/crm/ui/attendance/AttendanceCalendarView.java`**:

```java
@Route(value = "attendance-calendar", layout = MainLayout.class)
@PageTitle("דוח נוכחות | CRM")
@PermitAll
public class AttendanceCalendarView extends VerticalLayout {

    private final AttendanceReportService reportService;
    private final UserService             userService;
    private final SecurityService         securityService;

    private YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Jerusalem"));
    private Long      selectedUserId;

    private final Button                 prevBtn    = new Button(VaadinIcon.ANGLE_LEFT.create());
    private final Button                 nextBtn    = new Button(VaadinIcon.ANGLE_RIGHT.create());
    private final H3                     monthLabel = new H3();
    private final ComboBox<UserResponse> userCombo  = new ComboBox<>("עובד");
    private final Div                    calGrid    = new Div();

    public AttendanceCalendarView(AttendanceReportService reportService,
                                  UserService userService,
                                  SecurityService securityService) {
        this.reportService   = reportService;
        this.userService     = userService;
        this.securityService = securityService;
        getElement().setAttribute("dir", "rtl");
        setPadding(true);
        buildHeader();
        add(calGrid);
        initUserContext();
    }

    private void buildHeader() {
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevBtn.addClickListener(e -> navigate(-1));
        nextBtn.addClickListener(e -> navigate(+1));
        userCombo.setItemLabelGenerator(UserResponse::username);
        userCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) { selectedUserId = e.getValue().id(); refresh(); }
        });
        HorizontalLayout nav = new HorizontalLayout(prevBtn, monthLabel, nextBtn, userCombo);
        nav.setAlignItems(Alignment.BASELINE);
        add(nav);
    }

    private void initUserContext() {
        if (securityService.isAdmin()) {
            userCombo.setItems(userService.findAll());
        } else {
            userCombo.setVisible(false);
            selectedUserId = securityService.getAuthenticatedUserId();
            refresh();
        }
    }

    void refresh() {
        if (selectedUserId == null) return;
        Locale he = Locale.forLanguageTag("he");
        monthLabel.setText(
            currentMonth.getMonth().getDisplayName(TextStyle.FULL, he)
            + " " + currentMonth.getYear());
        MonthlyCalendarResponse cal = reportService.getMonthlyCalendar(
            selectedUserId, currentMonth.getYear(), currentMonth.getMonthValue());
        buildCalendarGrid(cal);
    }

    private void navigate(int delta) {
        currentMonth = currentMonth.plusMonths(delta);
        refresh();
    }

    // ---- CALENDAR GRID -------------------------------------------------------

    private void buildCalendarGrid(MonthlyCalendarResponse cal) {
        calGrid.removeAll();
        calGrid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(7, 1fr)")
            .set("direction", "rtl")   // rightmost column = Sunday in RTL
            .set("gap", "4px");

        // RTL column order: first item rendered appears on the right.
        // Result: Sunday (index 0) rightmost, Saturday (index 6) leftmost.
        String[] headers = {"ראשון","שני","שלישי","רביעי","חמישי","שישי","שבת"};
        for (String h : headers) {
            Div cell = new Div(new Span(h));
            cell.addClassName("calendar-header-cell");
            calGrid.add(cell);
        }

        // Leading empty cells to align the 1st of the month with its column
        LocalDate first       = YearMonth.of(cal.year(), cal.month()).atDay(1);
        int       leading     = israeliDayIndex(first.getDayOfWeek());
        for (int i = 0; i < leading; i++) calGrid.add(new Div());

        for (DayCalendarEntry entry : cal.days()) calGrid.add(buildDayCell(entry));
    }

    /**
     * Maps DayOfWeek to 0-based Israeli week index.
     * Sunday=0 (rightmost in RTL grid), Saturday=6 (leftmost).
     */
    private int israeliDayIndex(DayOfWeek dow) {
        return switch (dow) {
            case SUNDAY    -> 0;
            case MONDAY    -> 1;
            case TUESDAY   -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY  -> 4;
            case FRIDAY    -> 5;
            case SATURDAY  -> 6;
        };
    }

    private Div buildDayCell(DayCalendarEntry entry) {
        Div cell = new Div();
        cell.addClassName("calendar-day-cell");
        if (entry.isWeekend()) cell.addClassName("weekend");
        if (entry.isHoliday()) cell.addClassName("holiday");
        if (entry.deltaMinutes() < 0 && !entry.isWeekend() && !entry.isHoliday())
            cell.addClassName("deficit");

        Span dayNum = new Span(String.valueOf(entry.date().getDayOfMonth()));
        dayNum.addClassName("day-number");
        cell.add(dayNum);

        if (entry.isHoliday()) {
            Span hn = new Span(entry.holidayName());
            hn.getStyle().set("font-size","0.75em").set("color","var(--lumo-primary-color)");
            cell.add(hn);
        }

        if (entry.totalWorkedMinutes() > 0) {
            Span total = new Span(DurationCalculator.formatMinutes(entry.totalWorkedMinutes()));
            total.addClassName("day-total");
            if (entry.deltaMinutes() < 0 && !entry.isWeekend())
                total.getStyle().set("color","var(--lumo-error-color)");
            cell.add(total);
        }

        for (AttendanceReportResponse r : entry.reports()) {
            String chipText = r.reportTypeLabel()
                + (r.durationFormatted() != null ? " " + r.durationFormatted() : "");
            Span chip = new Span(chipText);
            chip.addClassName("report-chip");
            chip.addClickListener(ev -> openEditor(entry.date(), r));
            cell.add(chip);
        }

        Button addBtn = new Button("+");
        addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addBtn.addClickListener(ev -> openEditor(entry.date(), null));
        cell.add(addBtn);

        return cell;
    }

    private void openEditor(LocalDate date, AttendanceReportResponse existing) {
        new AttendanceReportEditor(
            reportService, this, selectedUserId, date, existing).open();
    }
}
```

---

#### FE Step G.2 — `AttendanceReportEditor`

Create **`com/crm/ui/attendance/AttendanceReportEditor.java`** as a `Dialog` subclass, mirroring the Hilan "דיווח נוכחות" screen:

```java
public class AttendanceReportEditor extends Dialog {

    private final AttendanceReportService  service;
    private final AttendanceCalendarView   parent;
    private final Long                     userId;
    private final LocalDate                reportDate;
    private final AttendanceReportResponse editing;   // null = create mode

    private final ComboBox<AttendanceReportType> typeCombo =
                      new ComboBox<>("סוג דיווח");
    private final TimePicker  entryPicker = new TimePicker("שעת כניסה");
    private final TimePicker  exitPicker  = new TimePicker("שעת יציאה");
    private final Span        totalSpan   = new Span("--");
    private final TextArea    notesField  = new TextArea("הערות");
    private final Checkbox    equateBox   = new Checkbox("השוואה לתקן");

    private final Button saveBtn   = new Button("שמור",  VaadinIcon.CHECK.create());
    private final Button cancelBtn = new Button("ביטול", VaadinIcon.CLOSE.create());
    private final Button deleteBtn = new Button("מחק",   VaadinIcon.TRASH.create());

    public AttendanceReportEditor(AttendanceReportService service,
                                   AttendanceCalendarView parent,
                                   Long userId, LocalDate reportDate,
                                   AttendanceReportResponse editing) {
        this.service    = service;
        this.parent     = parent;
        this.userId     = userId;
        this.reportDate = reportDate;
        this.editing    = editing;
        build();
        if (editing != null) populate();
    }

    private void build() {
        setHeaderTitle(editing == null
            ? "דיווח נוכחות חדש -- " + reportDate
            : "עריכת דיווח -- " + reportDate);
        getElement().setAttribute("dir", "rtl");
        setWidth("440px");

        typeCombo.setItems(AttendanceReportType.values());
        typeCombo.setItemLabelGenerator(AttendanceReportType::getHebrewLabel);
        typeCombo.setValue(AttendanceReportType.PRESENCE);
        typeCombo.addValueChangeListener(e -> onTypeChange(e.getValue()));

        entryPicker.setStep(Duration.ofMinutes(15));
        exitPicker.setStep(Duration.ofMinutes(15));
        entryPicker.addValueChangeListener(e -> recomputeTotal());
        exitPicker.addValueChangeListener(e -> recomputeTotal());

        totalSpan.getStyle().set("font-size","1.3em").set("font-weight","bold");
        HorizontalLayout totalRow = new HorizontalLayout(new Span("סה\"כ:"), totalSpan);
        totalRow.setAlignItems(Alignment.BASELINE);

        notesField.setWidth("100%");
        notesField.setMaxHeight("80px");
        equateBox.setTooltipText("ה.לתקן -- קבל תקן מלא עבור יום זה ללא קשר לשעות בפועל");

        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> doSave());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelBtn.addClickListener(e -> close());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.setVisible(editing != null);
        deleteBtn.addClickListener(e -> doDelete());

        HorizontalLayout timeRow = new HorizontalLayout(entryPicker, exitPicker);
        VerticalLayout form = new VerticalLayout(typeCombo, timeRow, totalRow,
                                                  notesField, equateBox);
        form.setPadding(false);
        add(form);
        getFooter().add(new HorizontalLayout(saveBtn, cancelBtn, deleteBtn));
    }

    // ---- LIVE TOTAL RECOMPUTATION -------------------------------------------

    private void recomputeTotal() {
        LocalTime e = entryPicker.getValue(), x = exitPicker.getValue();
        totalSpan.setText(e != null && x != null
            ? DurationCalculator.formatMinutes(DurationCalculator.computeMinutes(e, x))
            : "--");
    }

    // ---- TIME FIELD VISIBILITY (hidden for absence-style types) -------------

    private void onTypeChange(AttendanceReportType type) {
        boolean needsClock = (type == AttendanceReportType.PRESENCE);
        entryPicker.setVisible(needsClock);
        exitPicker.setVisible(needsClock);
        if (!needsClock) { entryPicker.clear(); exitPicker.clear(); totalSpan.setText("--"); }
    }

    // ---- POPULATE FOR EDIT --------------------------------------------------

    private void populate() {
        typeCombo.setValue(editing.reportType());
        onTypeChange(editing.reportType());
        entryPicker.setValue(editing.entryTime());
        exitPicker.setValue(editing.exitTime());
        notesField.setValue(editing.note() != null ? editing.note() : "");
        equateBox.setValue(editing.equateToStandard());
        recomputeTotal();
    }

    // ---- SAVE ---------------------------------------------------------------

    private void doSave() {
        AttendanceReportRequest req = new AttendanceReportRequest(
            reportDate, entryPicker.getValue(), exitPicker.getValue(),
            notesField.getValue().isBlank() ? null : notesField.getValue(),
            typeCombo.getValue(), equateBox.getValue());
        try {
            if (editing == null) service.createReport(userId, req);
            else                 service.editReport(editing.id(), req);
            close();
            parent.refresh();
            Notification.show("הדיווח נשמר", 3000, Notification.Position.TOP_CENTER);
        } catch (AttendanceValidationException ex) {
            Notification n = Notification.show(
                ex.getMessage(), 5000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ---- DELETE -------------------------------------------------------------

    private void doDelete() {
        ConfirmDialog dlg = new ConfirmDialog(
            "מחיקת דיווח", "האם למחוק את הדיווח? לא ניתן לבטל פעולה זו.",
            "מחק", ev -> {
                service.deleteReport(editing.id());
                close();
                parent.refresh();
                Notification.show("הדיווח נמחק", 3000, Notification.Position.TOP_CENTER);
            },
            "ביטול", ev -> {});
        dlg.setConfirmButtonTheme("error primary");
        dlg.open();
    }
}
```

---

#### FE Step G.3 — Navigation

Add under the Time Clock group in **`MainLayout.createDrawer()`**:

```java
SideNavItem timeClock = new SideNavItem("Time Clock");
timeClock.setPrefixComponent(VaadinIcon.CLOCK.create());
timeClock.addItem(new SideNavItem(
    "דוח נוכחות",
    AttendanceCalendarView.class,
    VaadinIcon.CALENDAR.create()));
nav.addItem(timeClock);
```

---

#### FE Step G.4 — CSS

Add to `src/main/frontend/themes/crm/styles.css`:

```css
/* ---- Attendance Calendar ----------------------------------------- */
.calendar-header-cell {
    text-align: center;
    font-size: 0.78em;
    font-weight: bold;
    color: var(--lumo-secondary-text-color);
    padding: 4px 0;
    border-bottom: 2px solid var(--lumo-contrast-20pct);
}
.calendar-day-cell {
    border: 1px solid var(--lumo-contrast-10pct);
    border-radius: var(--lumo-border-radius-s);
    padding: 4px 6px;
    min-height: 82px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    overflow: hidden;
}
.calendar-day-cell.weekend { background: var(--lumo-contrast-5pct); }
.calendar-day-cell.holiday { background: var(--lumo-primary-color-10pct); }
.calendar-day-cell.deficit { border-color: var(--lumo-error-color-50pct); }
.day-number {
    font-size: 0.82em;
    font-weight: bold;
    color: var(--lumo-secondary-text-color);
    align-self: flex-end;
}
.day-total { font-size: 0.9em; }
.report-chip {
    background: var(--lumo-primary-color-10pct);
    border-radius: 10px;
    padding: 1px 7px;
    font-size: 0.78em;
    cursor: pointer;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.report-chip:hover { background: var(--lumo-primary-color-50pct); }
```

---

#### Verification Phase G

1. Navigate to http://localhost:9080/attendance-calendar
2. Confirm month header shows correct Hebrew name (e.g., "יוני 2026")
3. Confirm day 1 of the month lands under the correct column in RTL layout
4. Click "+" on a Thursday cell → `AttendanceReportEditor` opens
5. Type נוכחות, entry `08:00`, exit `17:00` → confirm **סה"כ** shows **9:00** live
6. Change exit to `00:30` with entry `22:00` → confirm **סה"כ** shows **2:30** (midnight-crossing)
7. Save → calendar cell gains chip "נוכחות 2:30" without page reload
8. Try adding a second overlapping PRESENCE for the same day → confirm Hebrew error notification
9. Change type to חופשה → confirm time pickers hide and total shows "--"
10. Save the vacation report → chip shows "חופשה" with no time suffix
11. Click the chip → editor opens pre-populated with saved data
12. Click מחק → ConfirmDialog appears; confirm → chip removed, calendar refreshes
13. Navigate prev/next month with arrows → grid re-renders for the new month
14. As admin: switch employee selector → calendar reloads for the selected employee

---

#### Migration Notes

**Flyway scripts** (apply in order before deploying in production):

```
V2.2.0__create_attendance_report_table.sql    -- table, indexes, updated_at trigger
V2.2.1__add_report_type_check_constraint.sql  -- CHECK constraint for report_type values
```

In development (`ddl-auto=update`) Hibernate creates the table automatically. Flyway is activated only when `spring.flyway.enabled=true` (the `postgres`/`prod` profile). Store these under `src/main/resources/db/migration/` alongside the Phase 21 scripts (`V2.0.0`, `V2.0.1`).

---

#### Coexistence with Phase C Punch-In/Out Clock

| Concern | Phase C -- Clock | Phase F -- Manual Reports |
|---|---|---|
| Storage table | `attendance` | `attendance_report` |
| Trigger | Employee hardware/UI clock | HR or employee manual entry |
| Cardinality | 1 open row per user max | Multiple rows per (user_id, date) |
| Time columns | `TIMESTAMP WITH TIME ZONE` | `DATE` + `TIME` (no TZ) |
| Midnight handling | Not applicable (absolute timestamps) | `DurationCalculator.computeMinutes` |
| Constraint | Partial unique index on `end_time IS NULL` | Service-layer overlap check |
| Monthly query entry point | `AttendanceService.getMonthlyRecords` | `AttendanceReportService.getMonthlyCalendar` |

A single employee may simultaneously have a clock session (from `attendance`) and a manual report (from `attendance_report`) for the same day -- for example, a morning worked via punch clock plus an afternoon `RESERVE_DUTY` record entered manually. The two services read their respective tables and remain independent data products.

---

#### Integration with Phase E Monthly Excel Report

Extend **`ExcelReportService.generateMonthlyReport`** signature:

```java
public byte[] generateMonthlyReport(
        List<Attendance>       clockRecords,    // Phase C -- unchanged parameter
        List<AttendanceReport> manualReports,   // Phase F -- new parameter
        Map<Long, String>      userNames,
        String                 monthLabel) throws IOException
```

Inside the method, add a new sheet **"דיווחי נוכחות"** with columns:

```
עובד | תאריך | שעת כניסה | שעת יציאה | משך (ד"ש) | סוג דיווח | הערות | השוואה לתקן
```

Also extend the **Summary** sheet with two new columns per employee row:
- **"דיווחים ידניים"** — count of `AttendanceReport` rows for that employee
- **"שעות ידניות"** — sum of PRESENCE `durationMinutes` / 60, formatted via `DurationCalculator.formatMinutes`

Extend **`MonthlyReportJob`** to load manual reports alongside clock records:

```java
LocalDate fromDate = lastMonth.atDay(1);
LocalDate toDate   = lastMonth.atEndOfMonth();

List<AttendanceReport> manualReports =
    attendanceReportRepo.findAllByDateRange(fromDate, toDate);

byte[] excel = excelService.generateMonthlyReport(
    clockRecords, manualReports, names, label);
```

The accountant receives a single `.xlsx` file that consolidates clock-session sheets (Phase 25), the new manual-reports sheet, and a Summary that aggregates both sources per employee.
