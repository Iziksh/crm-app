# Test Gap Analysis

Analysis of automated test coverage in `crm-app` as of 2026-06-11.

---

## Summary

| Area | Total | With tests | Gap |
|------|-------|------------|-----|
| REST controllers | 26 | 0 | **100% untested** |
| Services | ~40 | 15 | **~63% untested** |
| Repositories | ~25 | 7 | **~72% untested** |
| Security components | 5+ | 0 | **100% untested** |
| Vaadin UI views | 20+ | 0 | **100% untested** |
| Time-tracking module | 8+ classes | 0 | **100% untested** |

Existing tests are strong in core CRM services (leads, opportunities, quotes, activities) but leave API boundaries, authentication, and the attendance module largely unverified.

---

## Critical Areas With No Tests

### 1. Authentication and authorization

| Component | Risk |
|-----------|------|
| `AuthController` | Login, OTP, JWT issuance — primary attack surface |
| `JwtService` | Token creation, validation, expiry |
| `JwtAuthenticationFilter` | Request-level auth enforcement |
| `SecurityConfig` | Role-based `/api/**` access rules |
| `UserDetailsServiceImpl` | User lookup for authentication |
| `OtpService` | **Has tests** — good baseline |
| `DeviceTrustService` | **Has tests** — good baseline |

**Impact:** Auth bypass, privilege escalation, or token handling bugs would not be caught by CI.

### 2. REST API layer (all controllers untested)

No `@WebMvcTest` or `@SpringBootTest` + `MockMvc` tests exist for any controller:

- `AuthController`, `UserController`
- `LeadController`, `OpportunityController`, `QuoteController`, `SalesOrderController`
- `ContractController`, `ActivityController`, `ContactController`, `AccountController`
- `AlertController`, `AttachmentController`, `ProductController`, `SubscriptionController`
- `WorkspaceController`, `AccountGroupController`, `AddressController`
- `ForecastController`, `ScheduledTaskController`, `SavedSearchController`
- `NotificationController`, `TopicController`
- Time-tracking: `AttendanceController`, `AttendanceReportController`, `HolidayController`, `ReportAdminController`

**Impact:** HTTP status codes, validation errors, security annotations, and JSON contracts are unverified.

### 3. Time-tracking (attendance) module

Entire submodule lacks tests:

- `AttendanceService`, `AttendanceReportService`, `IsraeliHolidayService`
- `ExcelReportService`, `ReportEmailService`
- Related repositories and controllers

**Impact:** Payroll/reporting logic, holiday calculations, and Excel generation are high business-risk paths with zero automation.

### 4. Import and subscription processing

| Component | Risk |
|-----------|------|
| `ImportService` | Bulk data import — data integrity |
| `SubscriptionService` / `SubscriptionHandlerService` / `SubscriptionMatcherService` | Event-driven CRM automation |
| `NotificationTriggerService` / `NotificationService` | Alert delivery pipeline |
| `ScheduledTaskProcessor` | Background job execution |

---

## Services With Weak or No Coverage

### Tested (good coverage)

| Service | Test class |
|---------|------------|
| `LeadService` | `LeadServiceTest` |
| `OpportunityService` | `OpportunityServiceTest` |
| `QuoteService` | `QuoteServiceTest` |
| `ContractService` | `ContractServiceTest` |
| `ActivityService` | `ActivityServiceTest` |
| `AlertService` | `AlertServiceTest` |
| `AccountService` | `AccountServiceTest` |
| `AttachmentService` | `AttachmentServiceTest` |
| `ScheduledTaskService` | `ScheduledTaskServiceTest` |
| `TopicService` | `TopicServiceTest` |
| `OtpService` | `OtpServiceTest` |
| `DeviceTrustService` | `DeviceTrustServiceTest` |
| `EmailService` | `EmailServiceTest` |
| `EmailTemplateService` | `EmailTemplateServiceTest` |
| `TranslationService` | `TranslationServiceTest` |

### Untested services (priority order)

| Priority | Service | Reason |
|----------|---------|--------|
| P0 | `JwtService` | Security-critical |
| P0 | `UserService` | User lifecycle, password handling |
| P0 | `ContactService` | Core CRM entity |
| P1 | `SalesOrderService` | Revenue workflow |
| P1 | `ProductService` | Catalog management |
| P1 | `WorkspaceService` | Multi-tenancy boundary |
| P1 | `ForecastService` | Sales reporting |
| P1 | `AttendanceService` | Time clock core |
| P1 | `AttendanceReportService` | Compliance reporting |
| P2 | `AddressService` | Supporting entity |
| P2 | `AccountGroupService` | Supporting entity |
| P2 | `SavedSearchService` | User feature |
| P2 | `ImportService` | Data migration |
| P2 | `SubscriptionService` | Automation |
| P2 | `NotificationService` | User notifications |
| P2 | `IsraeliHolidayService` | Calendar logic |
| P2 | `ExcelReportService` | File generation |
| P2 | `ReportEmailService` | Email delivery |
| P2 | `LocaleService` | i18n resolution |

---

## Controllers With No Tests

All 26 controllers lack automated tests. Highest priority:

1. **`AuthController`** — login, OTP verify, token refresh
2. **`UserController`** — user CRUD, role assignment
3. **`LeadController` / `OpportunityController`** — sales pipeline API
4. **`AttendanceController` / `AttendanceReportController`** — time tracking API
5. **`AttachmentController`** — file upload (size limits, auth)

### Recommended pattern

```java
@WebMvcTest(LeadController.class)
@Import(SecurityConfig.class) // or @AutoConfigureMockMvc(addFilters = false) for isolated tests
class LeadControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean LeadService leadService;
    // ...
}
```

For auth flows, use `@SpringBootTest` + `MockMvc` with `@WithMockUser` / `spring-security-test`.

---

## Security-Sensitive Code Lacking Tests

| Code | Concern | Suggested test |
|------|---------|----------------|
| `SecurityConfig.apiFilterChain` | Role rules per `/api/v1/**` path | `@WebMvcTest` or `@SpringBootTest` with `MockMvc` — verify 401/403 for wrong roles |
| `JwtAuthenticationFilter` | Missing/invalid token handling | Filter unit test with `MockHttpServletRequest` |
| `JwtService.isTokenValid` | Expired/tampered tokens | Unit test with fixed clock |
| `AuthController` | OTP brute-force, invalid credentials | Integration test with MockMvc |
| `AttachmentService` | Path traversal, file type validation | Extend existing `AttachmentServiceTest` |
| `ImportService` | CSV injection, oversized imports | New service tests |
| BCrypt password encoding | `SecurityConfig.passwordEncoder` | Verify hash round-trip in `UserService` tests |

---

## Repository Coverage Gaps

**Tested:** `Lead`, `Contact`, `Contract`, `Activity`, `Alert`, `Opportunity`

**Untested (high value):**

- `UserRepository` — auth lookups
- `TrustedDeviceRepository` — device trust persistence
- `SubscriptionRepository`, `ScheduledTaskRepository`
- `AttendanceRepository`, `AttendanceReportRepository`, `HolidayRepository`
- `QuoteRepository`, `SalesOrderRepository`, `ProductRepository`

---

## Recommended Test Additions

### Phase 1 — Security and API (highest ROI)

1. `JwtServiceTest` — token generation, expiry, invalid signature
2. `AuthControllerTest` — login success/failure, OTP flow (MockMvc)
3. `SecurityConfigIntegrationTest` — verify SALES cannot access `/api/v1/activities/**`
4. `UserServiceTest` — create user, encode password, find by email

### Phase 2 — Core API smoke tests

5. `LeadControllerTest`, `OpportunityControllerTest`, `ContactControllerTest` — CRUD happy path + 404
6. `QuoteControllerTest`, `SalesOrderControllerTest` — status transitions

### Phase 3 — Time tracking

7. `IsraeliHolidayServiceTest` — known holiday dates for a given year
8. `AttendanceServiceTest` — clock in/out, overlap detection
9. `AttendanceReportServiceTest` — report generation for a pay period
10. `ExcelReportServiceTest` — workbook structure assertions

### Phase 4 — Infrastructure

11. Add JUnit `@Tag("integration")` to `@DataJpaTest` and `@SpringBootTest` classes
12. Configure `maven-failsafe-plugin` for `*IT.java` naming convention
13. Optional: Testcontainers PostgreSQL profile test for Flyway migrations

### Phase 5 — Coverage enforcement

14. Set JaCoCo minimum threshold (e.g. 50% line coverage) in `pom.xml` once gaps are closed
15. Add `@WebMvcTest` coverage for remaining controllers incrementally

---

## CI Interaction

Current CI runs all existing tests and generates JaCoCo reports but **does not enforce a coverage minimum**. Tests listed above should be added incrementally; once baseline coverage improves, enable JaCoCo `check` goal with a team-agreed threshold.
