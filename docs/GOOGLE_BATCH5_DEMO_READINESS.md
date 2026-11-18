# Calendar Batch 5 and Meet demo readiness

Status: IN_PROGRESS. Read-only readiness review for the November 19, 2026 demo.
This is not evidence of successful real-provider delivery or installed-runtime readiness.

## Evidence

- Reviewed source: `3c44323a62a7681a337c12759c9f50892552bcba`.
- [TASK-009 run 44288298805](https://github.com/viethoang4254/english-tutoring-platform/actions/runs/44288298805): backend-feature-acceptance, smtp-acceptance, profile-browser-acceptance and ui-review-screenshots succeeded. Windows packaging was skipped.
- Inspected local manifest: `E:\english-learning-test-runtime\stage-c5-22a29b7\manifest.json`; project reference `zobxcixrxsazmngcthvg`, approved commit `22a29b7d5b77ccaf4ce1cea10d351a2cb0b98f22`, package run `43872867514`. This older manifest does not establish the active process version or current database identity.
- No database query, migration, service launch, provider request, email, build or test was executed during this review.

## Implemented behavior

Teacher course invitation endpoints enforce eligible Teacher ownership; mutations retain CSRF protection. Student OAuth is unnecessary. Eligible attendees derive from active enrollment, confirmed payment and Student eligibility. Course overview exposes actual counts and configuration, not delivery receipts. HTTP 202 means queued.

V13 provides durable mappings/leases/revisions. V14 adds invitation opt-ins and lifecycle reconciliation. Domain changes and bounded reconciliation update future attendees, reschedules and cancellations. Stable event identities, conditional provider updates and readback reduce duplicate events/notifications. Retained attendees keep RSVP responses; private invitations link to the protected Student schedule, not raw Meet URLs.

Disconnect first requests durable external cleanup while credentials remain available. Only after cleanup can a second disconnect revoke credentials. Ambiguous revocation remains quarantined. Do not use disconnect as a demo reset.

V15 schedules application-managed reminders approximately 15 minutes before a session. Eligibility is rechecked before submission. Claims and deduplication survive restarts. Definite SMTP authentication rejection has bounded retry; ambiguous submission or abandoned claims become UNKNOWN without automatic resend. SENT means SMTP acceptance, not mailbox delivery. Changes during an external write/send cannot be made atomic with the database; subsequent Calendar reconciliation repairs desired state, but sent mail cannot be recalled.

Meet remains the Teacher-provided course-level URL. Student access requires eligible identity, ACTIVE enrollment, CONFIRMED payment, PUBLISHED course and a SCHEDULED session. Teacher management retains course ownership. No meeting provisioning or invented link is needed.

## Deployment blockers

1. Installed package provenance is older than Batches 1–4. Current process provenance was not established.
2. Actual V13–V15 history/checksums and a usable backup are unverified. Previous V12 verification is historical evidence only.
3. `Start-TestEnvironment.ps1` now requires complete V1-V15 history matching the verified package receipt. Missing, duplicate, unexpected, failed or mismatched entries fail closed. Automatic migrations remain disabled. Sync, invitations, reminders and Contact delivery are explicitly disabled; the Contact page can render while submission remains unavailable. Hosted validation of this launcher update is pending.
4. No recipient allowlist enforcement was found in Calendar/reminder delivery code. Google OAuth consent test users are not an invitation-recipient allowlist. Reminder eligibility is global and independent of course invitation opt-in. Enabling reminders may reach every eligible upcoming Student in TEST.
5. SMTP sender credentials, sender permission, approved recipient inventory and actual mailbox delivery are unverified. Do not inspect or print secret contents to resolve these checks.
6. Existing OAuth ciphertext must retain its original token key file/key ID and client configuration across deployment. Do not regenerate keys, overwrite secrets or reconnect unnecessarily.

## Exact approval sequence before the demo

1. **Approve read-only TEST preflight and recovery preparation.** Independently verify the project-qualified TEST connection and hostname with `sslmode=verify-full`; inspect migration history/checksums, current runtime provenance and eligible recipient/course inventory. Establish a restricted backup/restore point and verify recovery usability. Stop if identity or recovery is uncertain. Do not access production.
2. **Review and approve only the pending migration plan.** V13 is needed for synchronization, V14 for invitation lifecycle, V15 for reminders. Apply only missing reviewed versions after checks; no clean, baseline, repair or schema reset. Preserve all data and verify existing history afterward. No migration is authorized by this document.
3. **Approve the isolated launcher/configuration update and hosted validation.** Keep OAuth enabled, `APP_GOOGLE_CALENDAR_SYNC_ENABLED=false`, `APP_GOOGLE_CALENDAR_INVITATIONS_ENABLED=false`, and `APP_SESSION_REMINDERS_ENABLED=false` initially. Resolve recipient isolation before delivery: enforce a reviewed allowlist or use an independently verified isolated dataset containing only explicitly approved recipients. Do not treat SMTP restrictions as Google invitation restrictions.
4. **Approve packaging and deployment separately.** Package the exact successful TASK-009 SHA; verify trusted artifact digest where available, SHA-256 manifest and backend/frontend provenance. Install in a new directory, preserve the old package and secret files, validate ports and launch with migrations disabled. Preserve the existing Google connection. Do not silently substitute an old JAR.
5. **Verify website behavior with delivery off.** Teacher logs in manually, confirms their connection and owned course; Student checks the website schedule and authorized Meet Join. Use existing legitimate confirmed payments; do not fabricate a payment or Meet URL. Check denied Student/cross-Teacher access without exposing protected URLs.
6. **Approve real Calendar writes and recipient emails explicitly.** Review Teacher, course, sessions and every recipient first. Enabling sync can process existing queued work, so inspect pending mappings/opt-ins before enabling it. Enable sync and then invitations only under the approved scope. Queue once, observe completion and manually verify Google event identity, timezone, recipients and received invitation. Repeat unchanged request, then approved reschedule/cancel/eligibility changes; check stable identity and reconciliation. Do not claim exactly-once notification delivery.
7. **Approve reminder delivery separately.** Configure existing SMTP with authentication and mandatory STARTTLS, `SMTP_FROM`, and trusted `APP_SESSION_REMINDER_SITE_ORIGIN`. Review all eligible upcoming recipients before enabling `APP_SESSION_REMINDERS_ENABLED`. A localhost schedule link works only on the demo machine. Observe an approved session at the reminder window; verify mailbox receipt manually and inspect safe job state. Never automatically retry UNKNOWN outcomes.

If these gates cannot be completed before November 19, demonstrate the website, protected Meet access and hosted synthetic Calendar evidence with delivery disabled. Clearly identify the real-provider portion as pending.

## Existing hosted regression coverage

- GoogleCalendarHttpTests and GooglePersistenceTests: authorization, ownership, OAuth replay/browser binding, locked eligibility, subject ownership and lease/revision behavior.
- CalendarTransportTests and CalendarSyncWorkerTests: mocked provider failures, private events, RSVP retention, duplicate-write readback, rescheduling, cancellation, stale revisions, revoked connections and bounded retries.
- PaymentPersistenceTests and ReconciliationPersistenceTests: disposable PostgreSQL eligibility/lifecycle behavior; reminder precision of scheduling, concurrent claims, retry bounds and UNKNOWN crash recovery with mocked SMTP.
- MeetingHttpTests, StudentCalendarHttpTests, TeacherCalendarHttpTests and browser meeting/calendar/invitation suites: protected Join, schedule and Teacher UI regressions.

Existing registration is retained. No new runtime behavior was introduced by this audit, so no duplicate test suite was added. Hosted success does not validate Google delivery, production credentials, TEST migrations or local service compatibility.
