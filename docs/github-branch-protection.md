# GitHub Branch Protection for `master`

This guide configures repository settings so **no code merges into `master` unless all CI checks pass**.

Apply these settings in GitHub: **Repository → Settings → Branches → Branch protection rules → Add rule** (or edit the existing rule for `master`).

---

## Prerequisites

1. The CI workflow (`.github/workflows/ci.yml`) has run at least once on a pull request so status check names appear in the dropdown.
2. You have **Admin** access to the repository.

### Required status check names

After the first CI run, require these checks (names must match exactly):

| Check name | Job |
|------------|-----|
| `Build` | `build` |
| `Unit Tests` | `unit-tests` |
| `Integration Tests` | `integration-tests` |
| `Test Coverage` | `coverage` |
| `Dependency Review` | `dependency-review` (PRs only) |
| `OWASP Dependency Check` | `owasp-dependency-check` |

> **Note:** `Dependency Review` only runs on pull requests. For pushes directly to `master` (which should be blocked anyway), that check will not appear. Require it for PR merges; blocking direct pushes to `master` covers the push path.

---

## Branch Protection Rule

**Branch name pattern:** `master`

### 1. Require a pull request before merging

- [x] **Require a pull request before merging**
- [x] **Require approvals** — set to at least **1** (adjust per team policy)
- [x] **Dismiss stale pull request approvals when new commits are pushed**
- [ ] Require review from Code Owners (optional — enable if `CODEOWNERS` exists)

### 2. Require status checks to pass before merging

- [x] **Require status checks to pass before merging**
- [x] **Require branches to be up to date before merging**
- Add each required check from the table above

### 3. Require conversation resolution before merging

- [x] **Require conversation resolution before merging**

All review threads on the PR must be marked resolved before merge.

### 4. Dismiss stale approvals

- [x] **Dismiss stale pull request approvals when new commits are pushed** (same as step 1)

Ensures approvers re-review after new commits land on the PR.

### 5. Prevent direct pushes to `master`

- [x] **Restrict who can push to matching branches**
- Leave the allowed-push list **empty** (or limit to release automation bots only)

This blocks developers from pushing commits directly to `master`; all changes must go through a PR.

### 6. Restrict force pushes

- [x] **Do not allow force pushes**

Prevents rewriting `master` history (`git push --force`).

### 7. Restrict branch deletion

- [x] **Do not allow deletions**

Prevents accidental or malicious deletion of `master`.

---

## Recommended Additional Settings

### Rulesets (GitHub Enterprise / newer UI)

If your organization uses **Rulesets** instead of classic branch protection:

1. Go to **Settings → Rules → Rulesets → New ruleset**
2. Target branch: `master`
3. Enable the same restrictions: PR required, required checks, no force push, no deletion
4. Bypass list: empty (or break-glass admins only)

### Merge strategy

Under **Settings → General → Pull Requests**:

- Prefer **Squash merge** or **Rebase merge** for a linear history (team preference)
- Disable merge commits if you want a clean log

### Signed commits (optional)

Under the branch protection rule:

- [x] **Require signed commits** — if the team uses GPG or SSH commit signing

### Admin enforcement

- [x] **Do not allow bypassing the above settings** (includes administrators)

Ensures admins follow the same process.

---

## Verification Checklist

After configuration, verify the gate works:

1. Open a PR targeting `master` with a deliberate test failure (e.g. `assertTrue(false)` in a test).
2. Confirm all CI jobs run automatically on PR open and on each push.
3. Confirm the PR **Merge** button is blocked until checks pass.
4. Fix the test; confirm checks turn green and merge becomes available.
5. Attempt a direct push to `master` — it should be rejected.

---

## Optional: NVD API Key for OWASP

OWASP Dependency-Check downloads the NVD vulnerability database. For faster, more reliable scans:

1. Register for a free API key at [NVD API Key registration](https://nvd.nist.gov/developers/request-an-api-key)
2. Add repository secret: **Settings → Secrets and variables → Actions → New repository secret**
   - Name: `NVD_API_KEY`
   - Value: your API key

The CI workflow passes this to the Maven plugin via `${{ secrets.NVD_API_KEY }}`.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Required check not in dropdown | Merge any PR or push to `master` once so workflows run |
| `Dependency Review` blocks merge on push | Expected — only required for PR merges; block direct pushes |
| OWASP job times out | Add `NVD_API_KEY` secret; or increase `timeout-minutes` in workflow |
| Checks stuck "Expected" | Workflow file must exist on the default branch (`master`) |
