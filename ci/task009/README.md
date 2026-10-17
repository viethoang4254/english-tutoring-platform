# TASK-009 isolated acceptance

Manual workflow only: `.github/workflows/task009-acceptance.yml`. It must run on
GitHub-hosted Ubuntu, never on a developer machine or self-hosted runner.

The SMTP job executes one `LocalResetSmtpTests` test, including decoded capture
assertions. Only after it passes, the browser job builds the frontend, creates its
own labelled PostgreSQL 18 container, checks the exact empty database target,
and starts the backend to apply unchanged Flyway V1–V5. It then runs 24 existing
mocked cases and 8 live cases (Student and pending/rejected/approved Teachers,
each on mobile and desktop). Missing services, skipped cases and wrong test
counts fail acceptance. These expected counts are not execution evidence.

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
