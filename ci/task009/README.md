# TASK-009 isolated acceptance

Manual workflow only: `.github/workflows/task009-acceptance.yml`. It must run on
GitHub-hosted Ubuntu, never on a developer machine or self-hosted runner.

The SMTP job executes one `LocalResetSmtpTests` test, including decoded capture
assertions. Only after it passes, the browser job builds the frontend, creates its
own labelled PostgreSQL 18 container, checks the exact empty database target,
and starts the backend to apply unchanged Flyway V1–V6 (target pinned to 6). It then runs 24 existing
mocked cases and 8 live cases (Student and pending/rejected/approved Teachers,
each on mobile and desktop). Missing services, skipped cases and wrong test
counts fail acceptance. These expected counts are not execution evidence.

Calendar 01.2 extends the same browser job, without another workflow or service stack.
After the existing profile groups, it runs 58 mocked Dashboard/Course/Session/Calendar
regressions, then 2 live populated Calendar cases (one desktop, one mobile). Groups
run sequentially with one worker and zero retries. The browser job timeout is 60 minutes.

Each live Calendar case registers two Teachers, one rejected Teacher and an Admin
fixture through the shared registration/verification helper. The real Admin review
API approves/rejects applications. Only then can `calendar-fixture` seed one Category,
two paid DRAFT Courses and eight concrete Sessions (six for A, two for B), including
cancelled rows, equal-start tie ordering and a UTC-midnight-crossing interval.
The helper checks distinct numeric owners, disposable CI emails, current approval,
empty owned Course sets, exact database/user and labelled loopback container identity.
The fixture insert is atomic and no existing owner data is reused or cleaned.

Live assertions exercise real PostgreSQL through Spring Boot: ownership both ways,
rejected eligibility, privacy, stable ordering, size-2 pagination, half-open overlap,
explicit UTC-offset equivalence and midnight crossing. Browser assertions cover real
populated month/week views, cancelled styling, Vietnam/New York Course timezone
formatting, Session detail navigation, persistent sidebar and mobile overflow.
Fixtures are removed with the disposable container by the existing always-cleanup.
No screenshots, traces, credentials or captured mail are uploaded.

Static validation is not CI execution. The branch dispatched later must include the
uncommitted Course/Session/Calendar backend and unified Dashboard dependencies, not
just these CI files. No Supabase credentials or connection are used.

Live fixtures register through existing APIs. A narrowly scoped SQL helper
provisions a verified Admin only inside the owned disposable container; review
and verification use real APIs. Profile assertions check persistence, immutable
security/application/profile snapshots, validation, locks, role boundaries,
unchanged refresh cookies and subsequent refresh. Mocked cases cover server
failure handling and uncertain-outcome reconciliation.

All credentials, RSA keys, OTP HMAC keys, certificates, captures and private logs
are generated under `RUNNER_TEMP/task009`, never in source or uploaded artifacts.
The SMTP sink binds to loopback, requires authenticated STARTTLS, accepts only
`example.invalid`, and has no relay. The PKCS12 trust store contains only its
public certificate; its certificate entries are readable without passing a
password to Java. TLS identity and certificate verification remain enabled.
PostgreSQL binds only to `127.0.0.1:55434/task009_profile_browser_ci` using owner
`task007_test`. Cleanup removes only the labelled CI container and owned services.

No repository secrets or production credentials are required. Required generated
settings include `DATABASE_*`, `SMTP_*`, `TEST_SMTP_*`, `APP_SECURITY_JWT_*`,
`APP_SECURITY_PASSWORDRESET_HMACSECRET`, browser origin/local HTTP settings and
`NEXT_PUBLIC_API_ORIGIN`; `run.py prepare` supplies them through `GITHUB_ENV`.
Use runners with at least 7 GB RAM. Maven is capped at 256 MB heap, the backend
and test JVM at 512 MB, PostgreSQL at 512 MB, Node at 1536 MB; browser workers=1,
retries=0. Resource checks stop jobs with insufficient headroom.

Before publishing, review the existing uncommitted Teacher Profile implementation:
the workflow tests that implementation and cannot establish acceptance if it is
omitted from the pushed ref. Keep unrelated TASK-046 documentation and crash logs
out of the commit. Obtain approval before staging, committing or pushing.

After an approved commit/push, manually dispatch:

```sh
gh workflow run task009-acceptance.yml --ref <approved-branch>
gh run list --workflow task009-acceptance.yml --branch <approved-branch>
gh run watch <run-id> --exit-status
```

GitHub requires the dispatch workflow to exist on the repository default branch
before it can be manually dispatched on another ref. Use the repository's normal
review/merge policy; do not switch the trigger to push or pull_request to bypass it.
Inspect both job conclusions and safe counts before recording acceptance. No
runtime result is claimed by adding this infrastructure.

## Full feature acceptance expansion (pending CI execution)

The existing workflow runs sequentially: SMTP -> backend-feature-acceptance ->
profile-browser-acceptance. No parallel database groups or browser workers.
`backend_tests.py` runs 18 explicit unit/HTTP suites, then CategoryPersistenceTests
and CategoryMigrationTests, CategoryLiveHttpTests, and CoursePersistenceTests.
The latter groups create fresh labelled PostgreSQL 18 containers bound only to
127.0.0.1:55436 or :55437, with generated CI passwords and the exact test identities.
Migration tests own V1-V6 application; the legacy-conflict test intentionally
expects V6 rejection and verifies preservation of V1-V5 and conflicting rows.
Each container is removed in finally; runner disposal covers interrupted jobs.
No Supabase configuration or shared database is used.

The browser runner adds Admin Categories (12), Admin Dashboard (12), Course
Discovery (4), Student Dashboard (16), Teacher Dashboard (20): 64 additional
cases, 156 total including the existing 92. Counts include desktop and mobile.
Frontend CI also runs typecheck, lint, all four existing Node unit-test files and
production build. Backend packaging in the browser job still skips duplicate
execution; it depends on the required backend test job succeeding first.

Only sanitized suite counts are uploaded for seven days. Raw Surefire XML can
contain environment properties and raw browser artifacts can contain credentials,
so neither is uploaded. Build/startup failures may consequently require a separate
safe diagnostic patch. Exact backend counts come from CI reports, not estimates.
Unrelated opt-in auth persistence/SMTP/operator suites are not enabled by this
expansion; existing SMTP and live profile acceptance remain. Other E2E files
(account, auth-navigation, change-password, foundation, password-recovery,
post-login, student-admin) remain outside this workflow selection.

Estimated sequential runtime: 20-45 minutes, dependent on dependency downloads;
job limits are 20/45/60 minutes. Use GitHub-hosted Ubuntu with at least 7 GB RAM;
database groups require at least 3 GiB available before startup, 512 MiB PostgreSQL
and a single 512 MiB Maven test JVM. Browser workers remain one, retries zero.
No results are claimed until the expanded workflow runs on its published commit.
