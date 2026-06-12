# GitHub Actions Retention Analysis

Analysis of the `crm-app` CI pipeline to identify storage growth risks and optimization opportunities.

**Date:** 2026-06-12

---

## Workflow Inventory

| Workflow | File | Triggers | Jobs |
|----------|------|----------|------|
| CI | `.github/workflows/ci.yml` | `push` / `pull_request` → `master` | 7 |
| Cache Cleanup | `.github/workflows/cache-cleanup.yml` | Weekly schedule, `workflow_dispatch` | 1 |

No other GitHub Actions workflows exist in this repository.

---

## Concurrency Settings

### Before optimization

```yaml
concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Concurrency was already enabled. The group included the workflow name, which is redundant while only one CI workflow exists.

### After optimization

```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
```

| Behavior | Status |
|----------|--------|
| New push to same branch cancels in-progress runs | Yes |
| New commit on PR cancels previous PR runs | Yes |
| Runs on different branches run in parallel | Yes |
| `master` and feature branches do not cancel each other | Yes |

**Impact:** Reduces wasted runner minutes when developers push frequently. A branch with 5 rapid pushes previously could leave 4 obsolete runs consuming runners until completion; now only the latest run continues.

---

## Artifact Usage

| Artifact | Job | Path | Retention (before) | Retention (after) |
|----------|-----|------|--------------------|-------------------|
| `crm-app-production-jar` | `production-build` | `target/crm-app-*.jar` | 7 days | 7 days (unchanged) |
| `jacoco-html-report` | `coverage` | `target/site/jacoco/` | 14 days | **7 days** |
| `jacoco-xml-report` | `coverage` | `target/site/jacoco/jacoco.xml` | 14 days | **7 days** |
| `owasp-dependency-check-report` | `owasp-dependency-check` | `target/dependency-check-report.html` | 14 days | **7 days** |

### Observations

- No Surefire/Failsafe test-result artifacts are uploaded (test output is only in job logs).
- All artifacts use explicit `retention-days`; none rely on the repository default indefinitely.
- The production JAR is a build artifact, not a deployment artifact — 7 days is sufficient for debugging failed builds.
- JaCoCo HTML reports can be large (multi-MB). Standardizing to 7 days halves the artifact storage window for coverage reports.

### Estimated artifact storage impact

Assuming ~10 CI runs per week with all jobs completing:

| Artifact | Approx. size | Weekly uploads | 14-day window | 7-day window |
|----------|--------------|----------------|---------------|--------------|
| Production JAR | ~80–120 MB | ~10 | ~1.2 GB | ~0.6 GB |
| JaCoCo HTML | ~2–5 MB | ~10 | ~70 MB | ~35 MB |
| JaCoCo XML | ~100 KB | ~10 | ~2 MB | ~1 MB |
| OWASP HTML | ~500 KB | ~10 | ~10 MB | ~5 MB |

**Estimated savings:** ~50% reduction in peak artifact storage from retention changes alone (primarily from halving the coverage report window; JAR was already at 7 days).

---

## Cache Usage

### Maven dependency cache (`actions/setup-java` with `cache: maven`)

Used in 6 jobs: `build`, `production-build`, `unit-tests`, `integration-tests`, `coverage`, `owasp-dependency-check`.

| Property | Value |
|----------|-------|
| Provider | `actions/setup-java@v4` (wraps `actions/cache@v4`) |
| Cache key | Deterministic hash of `**/pom.xml` (managed by setup-java) |
| Path | `~/.m2/repository` (Maven local repo subset) |
| Variants per pom change | 1 new key; old key becomes idle |

**Assessment:** Well-behaved. All jobs share the same cache key for a given `pom.xml` state. No manual cache configuration needed. GitHub evicts caches not accessed within 7 days automatically.

### OWASP NVD database cache (`actions/cache@v4`)

| Property | Before | After |
|----------|--------|-------|
| Path | `~/.m2/repository/org/owasp/dependency-check-data` | Same |
| Key | `owasp-nvd-v2-${{ runner.os }}-${{ hashFiles('pom.xml') }}` | `owasp-nvd-v3-${{ runner.os }}` |
| Restore keys | `owasp-nvd-v2-${{ runner.os }}-` | Removed (single deterministic key) |
| Approx. size | ~300–500 MB per entry | 1 entry per OS |

**Problem identified:** The NVD vulnerability database is global — it does not change when application dependencies in `pom.xml` change. Including `hashFiles('pom.xml')` in the cache key created a new ~500 MB cache entry on every dependency bump, while old entries lingered until GitHub's idle eviction.

**Fix:** Key by OS only (`owasp-nvd-v3-${{ runner.os }}`). One cache entry per runner OS. Bump the `v3` prefix if `dependency-check-maven` requires a incompatible data format upgrade.

### Cache growth risk summary

| Cache type | Unbounded growth risk (before) | After optimization |
|------------|-------------------------------|-------------------|
| Maven (setup-java) | Low | Low |
| OWASP NVD | **High** (new entry per pom change) | **Low** (1 per OS) |

---

## Workflow Run Retention

GitHub does not support per-workflow run deletion in YAML. Run log retention is controlled at the **repository** (or organization) level:

**Settings → Actions → General → Artifact and log retention**

| Setting | GitHub default | Recommendation for this repo |
|---------|----------------|------------------------------|
| Artifact and log retention | 90 days | **30 days** (see policy doc) |

### Why not auto-delete all runs?

- Run logs are essential for debugging flaky tests, OWASP failures, and Maven build errors.
- Audit trails for security and compliance require a minimum history window.
- Workflow-level artifact `retention-days` already limits artifact storage independently of log retention.

The new `cache-cleanup.yml` workflow prunes **caches only** — it does not delete workflow runs or artifacts.

---

## Scheduled Cache Cleanup

A new `cache-cleanup.yml` workflow complements GitHub's built-in 7-day idle cache eviction:

| Parameter | Default | Purpose |
|-----------|---------|---------|
| Schedule | Sunday 03:00 UTC | Weekly maintenance |
| `retention_days` | 14 | Delete caches not accessed within 14 days |
| `keep_most_recent` | 5 | Always retain the 5 newest caches |

Manual runs via `workflow_dispatch` allow adjusting retention for one-off cleanups.

---

## Gaps and Limitations

| Item | Status | Notes |
|------|--------|-------|
| Test report artifacts | Not uploaded | Test failures visible in job logs only; consider adding Surefire XML artifacts if flaky-test debugging becomes painful |
| Deployment workflow | None | No release/deploy pipeline; production JAR retention at 7 days is adequate |
| Fork PR secrets | OWASP job fails without `NVD_API_KEY` | Documented in `github-branch-protection.md`; not a retention issue |
| Repository log retention | Requires manual Settings change | Documented in policy; cannot be set via workflow YAML |
| Cache cleanup permissions | `actions: write` on cleanup workflow only | CI workflow retains read-only permissions |

---

## Validation Checklist

| Check | How to verify |
|-------|---------------|
| Obsolete runs cancelled | Push two commits quickly to a PR; confirm first run shows "Cancelled" |
| Artifact expiry | Upload artifact → confirm `retention-days: 7` in workflow → artifact disappears after 7 days |
| OWASP cache stable | After several pom.xml changes, confirm only one `owasp-nvd-v3-*` cache exists per OS |
| Cache cleanup runs | Trigger `Cache Cleanup` via Actions → Run workflow; review summary |
| CI speed maintained | Maven cache hit on subsequent runs; OWASP job skips NVD re-download on cache hit |
