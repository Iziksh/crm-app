# GitHub Actions CI Analysis

Analysis of the `crm-app` repository to design a quality-gate pipeline for merges into `master`.

**Date:** 2026-06-11

---

## Build System

| Item | Value |
|------|-------|
| Build tool | **Apache Maven** (`pom.xml`) |
| Wrapper | `mvnw.cmd` (Windows only); `.mvn/wrapper/` present |
| Packaging | `jar` (Spring Boot executable) |
| Parent POM | `spring-boot-starter-parent` **3.3.5** |

Maven lifecycle used in CI:

- `mvn clean compile` — build validation
- `mvn test` — unit and in-process integration tests (Surefire)
- `mvn verify` — tests + JaCoCo report (after JaCoCo plugin is added)

---

## Java Version

| Item | Value |
|------|-------|
| Target | **Java 17** (`<java.version>17</java.version>`) |
| CI recommendation | Eclipse Temurin 17 on `ubuntu-latest` |

The POM pins newer Byte Buddy / Mockito versions for JDK 25 compatibility during local development; CI uses JDK 17 as declared.

---

## Test Framework

| Item | Value |
|------|-------|
| Framework | **JUnit 5** (via `spring-boot-starter-test`) |
| Assertions | AssertJ |
| Mocking | Mockito 5.20.0 (inline agent configured in Surefire `argLine`) |
| Spring test support | `@SpringBootTest`, `@DataJpaTest`, `spring-security-test` |

### Test inventory (24 test classes)

| Category | Classes |
|----------|---------|
| Application smoke | `CrmApplicationTests` (`@SpringBootTest`, profile `dev`) |
| Repository (`@DataJpaTest`) | 7 classes (`Lead`, `Contact`, `Contract`, `Activity`, `Alert`, `Opportunity`) |
| Service (mocked unit) | 15 classes |
| i18n | `SupportedLocaleTest` |

There are **no** dedicated Failsafe integration tests (`*IT.java`, `maven-failsafe-plugin`).

---

## Test Coverage

| Item | Status |
|------|--------|
| JaCoCo | **Not configured** before this CI work — added in `pom.xml` |
| Coverage threshold | None enforced |
| Reports | HTML + XML generated at `target/site/jacoco/` after `mvn verify` |

Approximate coverage by area (manual inspection):

- **Services:** ~15 of ~40 service classes have dedicated tests
- **Controllers:** 0 of 26 controllers tested
- **Security:** 0 dedicated tests for JWT filter, `SecurityConfig`, or `AuthController`
- **Repositories:** 7 of ~25 repositories tested
- **UI (Vaadin):** no automated tests

---

## Existing GitHub Actions Workflows

**None.** The `.github/workflows/` directory did not exist prior to this CI implementation.

---

## Integration Test Requirements

| Requirement | Details |
|-------------|---------|
| Dedicated IT suite | **None** — no `*IT.java`, no Failsafe plugin |
| In-process integration | `@SpringBootTest` (`CrmApplicationTests`) loads full context with H2 |
| JPA slice tests | `@DataJpaTest` uses embedded H2, no external DB |
| Testcontainers | **Not used** |
| PostgreSQL | Runtime dependency for `prod`/`postgres` profiles only; tests use H2 |

### Test configuration (`src/test/resources/application.properties`)

- H2 in-memory via Hibernate `create-drop`
- Stub SMTP (localhost:25, no auth)
- Stub admin email and OTP settings

No Docker, PostgreSQL service, or external mail server is required for CI.

---

## Database Dependencies

| Environment | Database | Notes |
|-------------|----------|-------|
| Tests / `dev` | H2 in-memory | Auto schema via Hibernate |
| `postgres` / `prod` | PostgreSQL | `application-postgres.properties` |
| Migrations | SQL scripts in `db/migration/` | Flyway optional (`spring.flyway.enabled=true` in prod) |

CI runs entirely on H2; no database service container is needed.

---

## Frontend Build Requirements

| Item | Value |
|------|-------|
| UI framework | **Vaadin 24.5.6** (server-side + generated frontend) |
| Frontend location | `src/main/frontend/` (generated Vaadin assets) |
| Standalone `package.json` | **None** — no separate npm project |
| Build mechanism | `vaadin-maven-plugin` (`prepare-frontend` on compile; `build-frontend` with `-Pproduction`) |

### CI frontend strategy

- **Development compile:** `mvn clean compile` runs `prepare-frontend` automatically.
- **Production bundle:** optional `-Pproduction` profile runs `build-frontend` (Node/npm invoked by Vaadin plugin).
- **No** standalone `npm install`, `npm run build`, or `npm test` steps — Vaadin handles Node internally when the production profile is active.

The CI pipeline validates the default Maven compile path. A separate production-frontend job can be added later with `-Pproduction` if bundle regressions become a concern.

---

## Static Analysis

| Tool | Status |
|------|--------|
| Checkstyle | **Not configured** — recommend adding `maven-checkstyle-plugin` |
| SpotBugs | **Not configured** — recommend `spotbugs-maven-plugin` |
| PMD | **Not configured** — recommend `maven-pmd-plugin` |

These are documented as recommendations; they are **not** enforced in CI until plugins and rule sets are added to `pom.xml`.

---

## Security Scanning

| Tool | CI approach |
|------|-------------|
| Dependency Review | `actions/dependency-review-action` on pull requests — fails on critical severity |
| OWASP Dependency-Check | `dependency-check-maven` plugin via `mvn verify -Psecurity` — fails on CVSS ≥ 9 |

---

## CI Pipeline Design Summary

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│   build     │────▶│  test + coverage │     │  dependency-review  │ (PR only)
│ mvn compile │     │   mvn verify     │     └─────────────────────┘
└─────────────┘     │  + JaCoCo report │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  owasp-scan      │
                    │  -Psecurity      │
                    └──────────────────┘
```

### Triggers

- `pull_request` targeting `master`
- `push` to `master`

### Artifacts

- JaCoCo HTML report (`target/site/jacoco/`)
- JaCoCo XML report (`target/site/jacoco/jacoco.xml`)
- OWASP HTML report (`target/dependency-check-report.html`)

---

## Risks and Gaps

1. **No controller or security tests** — API and auth regressions are not caught by automation.
2. **No coverage threshold** — reports are generated but merge is not blocked on minimum %.
3. **No static analysis** — style and bug-pattern checks are absent.
4. **Production Vaadin bundle not built in CI** — only `prepare-frontend` runs on default compile.
5. **Secrets in `application.properties`** — mail credentials are committed; rotate and externalize for production (not a CI blocker but a security concern).
6. **Unix `mvnw` missing** — CI uses `actions/setup-java` with Maven cache instead of the wrapper script.

---

## Files Created / Modified for CI

| Path | Purpose |
|------|---------|
| `.github/workflows/ci.yml` | Main quality-gate workflow |
| `pom.xml` | JaCoCo plugin, OWASP profile, Surefire `argLine` merge |
| `docs/github-branch-protection.md` | Branch protection setup guide |
| `docs/test-gap-analysis.md` | Test coverage gap analysis |
