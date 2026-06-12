# GitHub Actions Retention Policy

Balanced retention strategy for the `crm-app` repository — minimize storage growth while preserving enough history for debugging, auditing, and CI reliability.

**Effective:** 2026-06-12  
**Applies to:** All GitHub Actions workflows in this repository

---

## Policy Summary

| Resource | Retention | Enforcement |
|----------|-----------|-------------|
| Workflow run logs | 30 days (recommended) | Repository Settings |
| Artifacts | 7 days | Workflow `retention-days` |
| Action caches (idle) | 7 days | GitHub platform default |
| Action caches (stale) | 14 days + keep 5 newest | `cache-cleanup.yml` schedule |
| In-progress CI runs | Cancelled on newer push | `ci.yml` concurrency |

---

## 1. Workflow Run History

### Recommendation: 30-day log retention

Configure at **Settings → Actions → General → Artifact and log retention → Days**:

```
30
```

### Rationale

| Retention | Pros | Cons |
|-----------|------|------|
| 7 days | Minimal storage | Too short for intermittent bugs, weekend-to-weekend comparisons |
| **30 days** | Covers a full sprint; enough for most debugging | Moderate storage cost |
| 90 days (GitHub default) | Maximum history | Logs accumulate with every push/PR; diminishing returns |

### What we do NOT do

- No automated deletion of every workflow run.
- No reduction below 14 days without team agreement (audit and compliance needs may vary).

### Manual steps (repository admin)

1. Open **github.com → crm-app → Settings → Actions → General**
2. Under **Artifact and log retention**, set **30** days
3. Save

This setting applies to **workflow run logs and artifacts that do not specify `retention-days`**. All current CI artifacts explicitly set `retention-days: 7`, so they expire sooner than the repo default.

---

## 2. Artifact Strategy

All uploaded artifacts must specify explicit `retention-days`. Never rely on open-ended storage.

| Artifact type | Retention | Workflow | Rationale |
|---------------|-----------|----------|-----------|
| Production JAR | 7 days | `ci.yml` → `production-build` | Debugging failed builds; not used for deployment |
| Coverage (HTML + XML) | 7 days | `ci.yml` → `coverage` | Sufficient for sprint-level coverage review |
| Security scan (OWASP HTML) | 7 days | `ci.yml` → `owasp-dependency-check` | Re-run scan if historical report needed |

### Adding new artifacts

When introducing new `actions/upload-artifact` steps:

1. Always set `retention-days` (default to **7** unless deployment requires longer).
2. Upload only files needed for debugging — not entire `target/` directories.
3. Document the artifact in this policy table.

### Deployment exception

If a deploy workflow is added later that consumes build artifacts across pipeline stages, increase retention **only for that deploy artifact** (e.g., 14 days) and document the reason.

---

## 3. Cache Strategy

### Principles

1. **Deterministic keys** — cache keys must be stable for identical inputs; avoid timestamps or commit SHAs in keys.
2. **Minimal variants** — do not include unrelated file hashes in cache keys (see OWASP NVD fix).
3. **Platform eviction** — GitHub removes caches not accessed in 7 days; do not fight this with aggressive restore-key fan-out.
4. **Scheduled cleanup** — weekly `cache-cleanup.yml` removes caches idle for 14+ days, keeping the 5 newest.

### Current caches

| Cache | Key pattern | Invalidation trigger |
|-------|-------------|---------------------|
| Maven dependencies | Managed by `setup-java` (`cache: maven`) | `pom.xml` / lockfile hash change |
| OWASP NVD database | `owasp-nvd-v3-{os}` | Manual key bump when `dependency-check-maven` major version changes |

### OWASP cache key bump procedure

When upgrading `dependency-check-maven` to a version that changes the NVD data format:

1. Increment the key prefix in `ci.yml` (e.g., `owasp-nvd-v3` → `owasp-nvd-v4`).
2. Merge and let CI populate the new cache.
3. Old `v3` caches expire via idle eviction and weekly cleanup.

### Cache cleanup workflow

| Setting | Value |
|---------|-------|
| Schedule | Sunday 03:00 UTC |
| Retention threshold | 14 days since last access |
| Protected caches | 5 most recently accessed (always kept) |
| Manual trigger | Actions → Cache Cleanup → Run workflow |

Adjust `retention_days` or `keep_most_recent` via `workflow_dispatch` inputs for one-off maintenance.

---

## 4. Concurrency and Run Cancellation

### Policy

When a new commit is pushed to the same branch or pull request, **in-progress CI runs for that ref are cancelled**. Only the newest run should consume runners.

```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
```

### Scope

| Scenario | Behavior |
|----------|----------|
| Rapid pushes on `feature/foo` | Only latest push runs to completion |
| PR updated while CI running | Previous PR run cancelled |
| `master` push during PR CI | Independent — different `github.ref` |
| Cache cleanup workflow | Separate concurrency group (`cache-cleanup`) |

### Tradeoff

Cancelled runs appear as "Cancelled" in the Actions tab. This is expected and preferable to queuing obsolete builds. Logs from cancelled runs are retained per the log retention policy until expiry.

---

## 5. Storage and Cost Tradeoffs

| Optimization | Storage impact | CI speed impact | Debuggability impact |
|--------------|----------------|-----------------|----------------------|
| `cancel-in-progress` | None (reduces runner minutes) | Faster feedback on active branch | Cancelled run logs still available until log retention expires |
| Artifact 7-day retention | ~50% vs 14-day for reports | None | Download reports within 7 days or re-run CI |
| OWASP cache key fix | Prevents ~500 MB × N pom changes | Faster OWASP job (cache hit) | None |
| Maven setup-java cache | Shared across jobs | Faster `mvn` dependency resolution | None |
| 30-day log retention (vs 90) | ~66% log storage reduction | None | 30-day window for log investigation |
| Weekly cache cleanup | Caps orphaned cache growth | None | None |

### Expected outcomes

- **Artifact storage:** Peak storage roughly halved for coverage/security reports compared to the previous 14-day window.
- **Cache storage:** OWASP NVD cache stabilized at ~500 MB per OS instead of growing with every `pom.xml` change.
- **Runner minutes:** Concurrency cancellation saves minutes on branches with frequent pushes (estimated 20–40% reduction on active feature branches).
- **Log storage:** Setting repo retention to 30 days (manual step) reduces log accumulation vs the 90-day default.

---

## 6. Responsibilities

| Role | Action |
|------|--------|
| Repository admin | Set 30-day log retention in Settings |
| Developers | Download artifacts within 7 days if needed offline |
| CI maintainers | Review cache count quarterly via Settings → Actions → Caches |
| CI maintainers | Bump OWASP cache key prefix on major `dependency-check` upgrades |

---

## 7. Review Schedule

Revisit this policy when:

- A deploy/release workflow is added
- Artifact storage approaches GitHub plan limits
- Compliance requirements change audit retention needs
- New cache patterns are introduced

**Next scheduled review:** 2026-12-12 (6 months)

---

## Related Documentation

- [GitHub Actions Retention Analysis](./github-actions-retention-analysis.md) — detailed audit and before/after comparison
- [GitHub Actions CI Analysis](./github-actions-analysis.md) — original CI design
- [GitHub Branch Protection](./github-branch-protection.md) — required checks and secrets
