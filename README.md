wha # Spring Boot CRM — OpenCRX Feature Parity Implementation

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
│   ├── DataInitializer.java          # Seeds admin user + Default workspace
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
│   │   └── Workspace.java            # BUILT
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
│       └── ContractStatus.java       # BUILT
├── dto/
│   ├── request/                      # BUILT: Login, Register, Account, Contact
│   └── response/                     # BUILT: Auth, Account, Contact
├── repository/                       # BUILT: User, Account, Contact
├── service/                          # BUILT: Jwt, User, Account, Contact
├── controller/                       # BUILT: Auth, Account, Contact
├── security/                         # BUILT: JwtFilter, UserDetailsServiceImpl
├── exception/                        # BUILT: GlobalHandler, ErrorResponse, exceptions
└── ui/
    ├── LoginView.java                # BUILT
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
    └── SavedSearchesView.java        # BUILT
```

---

## 5. Domain Model and Database Schema

### 5.1 Built Entities

#### `users` — `user_roles (user_id, role)`
`id`, `username` (UNIQUE), `email` (UNIQUE), `password` (BCrypt), `enabled`, `created_at`, `updated_at`

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

## 10. Running the Application

### Development — H2 In-Memory

```bash
cd crm-app
./mvnw spring-boot:run
```

- App: http://localhost:9080
- Swagger: http://localhost:9080/swagger-ui.html
- H2 Console: http://localhost:9080/h2-console (`jdbc:h2:mem:crmdb`)

### Production — PostgreSQL

```bash
# 1. Create database
psql -c "CREATE USER crm WITH PASSWORD 'yourpassword';"
psql -c "CREATE DATABASE crmdb OWNER crm;"

# 2. Environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/crmdb
export DATABASE_USER=crm
export DATABASE_PASSWORD=yourpassword
export JWT_SECRET=<base64-256-bit-secret>

# 3. Build and run
./mvnw clean package -DskipTests -Pproduction
java -jar target/crm-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
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
| `spring.profiles.active` | `dev` | Override with `prod` |
| `app.jwt.expiration-ms` | `86400000` | 24 hours |
| `spring.jpa.open-in-view` | `false` | |

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

```bash
./mvnw test
./mvnw test -Dtest=AccountServiceTest
```

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
| Email | `admin@crm.com` |
| Roles | `ROLE_USER`, `ROLE_ADMIN` |

**Change this password before exposing to any network.**

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
