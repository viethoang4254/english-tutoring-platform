# Stage C4 personal Google Calendar synchronization

## Delivery and access

PostgreSQL is authoritative. Connected users get a secondary calendar with the title **English Learning Platform — Lịch học**. Only upcoming scheduled sessions in published courses enter initial backfill. An unlocked, eligible Student must own an ACTIVE enrollment with a CONFIRMED payment. An unlocked, approved Teacher must own the course. Each mapped event has an application link, absolute start/end instants and the course IANA timezone. The worker supplies no attendees, bank information, conference data or Meet destination. Existing in-app Join authorization applies when the user opens a class.

V13 introduces a local destination UUID bound to both application user and verified Google subject. A partial rollout can leave OAuth enabled and synchronization disabled. Database triggers record changes from payments, enrollments, sessions, course publication/lifecycle, account eligibility, onboarding and Google connection changes, in the transaction that changes the business record. An append-only notification queue avoids cross-transaction notification-row lock ordering. A scheduled projector coalesces committed notifications and reconciles the current eligible roster. It serializes projection with a transaction-scoped advisory lock. The MVP reconciles the whole connected roster per batch; measure projection time and query plans before expanding enrollment volume.

Each destination/session has one durable work row, an event ID assigned before insertion, desired and applied revisions, a lease, attempts, a sanitized failure category and a deletion marker. Calendar provisioning is fenced by its persisted attempt UUID. UUID hex fits Google's base32hex event-ID alphabet. Workers take short SKIP LOCKED leases, refresh the appropriate owner's token, verify live ownership/eligibility and the calendar/event markers, then condition writes on the remote ETag. They perform HTTP outside database transactions. Retries read the stored event ID. Rescheduling retains identity. Cancellation and lost access retire the managed event. Reauthorization after a Google tombstone can allocate a new ID. Completed deletion markers are periodically audited for late provider writes.

The legacy organizational mapping has neither a personal subject nor a personal calendar ID. With personal sync enabled, a schedule change records a durable legacy-review item for affected legacy rows. Operations must reconcile the organizational calendar under its own separately authorized credentials. The personal worker never adopts these mappings. With personal sync disabled the existing CALENDAR_SYNC_REQUIRED contract remains in force.

## Configuration and recovery

1. Use the C3 web OAuth client, callback, offline grant, encrypted credential key and approved origin. Enable Google Calendar API on the intended project. Keep openid, email and calendar.app.created. No service account or new Google Meet capability is required.
2. Apply V13 through the approved deployment process before enabling APP_GOOGLE_CALENDAR_SYNC_ENABLED=true alongside the C3 OAuth configuration. The worker is disabled by default. Each instance processes up to twelve session jobs per tick. Default idle delay is three seconds.
3. Read GET /api/v1/integrations/google-calendar for owner-scoped status and counts. A CSRF-protected POST to /api/v1/integrations/google-calendar/sync makes exhausted owner work eligible for processing. The GET never initiates synchronization. Refresh the status panel after retrying.
4. Rate limits, network failures and transient errors back off with jitter. Event jobs stop after eight claims. Missing events are inserted with the saved ID, duplicate insertions are read back, and conditional-write conflicts are reconciled. A revoked refresh grant enters the C3 reconnection flow. Resolve permission failures before requesting retry.
5. A timeout after calendar creation can lose Google's server-assigned calendar ID. The worker quarantines that destination. Do not reset it blindly or identify the calendar by its display name. With the owner's assistance, identify the secondary calendar and verify its destination UUID marker and subject before an authorized operator repairs its binding. A missing **known** calendar is retried against its recorded ID. A permanently removed calendar requires operator recovery.
6. Disconnect disables new work immediately through the C3 connection state and removes local credential access before attempting revocation. An already transmitted Google request cannot be recalled. Remote events may remain after disconnect; the user can remove the app calendar in Google. Uncertain revocation keeps subject ownership quarantined. Reconnecting a different subject cannot operate on previous mappings.
7. For incidents, inspect local counts, revisions, lease expiry and sanitized categories. Do not log credential values, provider response bodies or protected meeting links. Check for unfinished legacy-review items separately. A payment or schedule commit never waits for the Google provider.

## Acceptance

TASK-009 uses isolated PostgreSQL, a loopback Calendar HTTP fixture, controlled worker tests and deterministic browser fixtures. The added HTTP case covers CSRF, caller identity, role and lock restrictions. Worker and transport tests cover owned paths, conditional writes, retries, uncertain creation and cross-account cancellation. PostgreSQL cases cover notification projection after reconciliation, pending access exclusion, revocation/cancellation, concurrent lease claims and a fresh repository instance resuming work. Browser acceptance adds sixteen desktop/mobile executions. Its additional screenshot artifact is google-calendar-sync-review-screenshots (eight files, desktop and mobile). Synthetic acceptance cannot establish real consent, external visibility or revocation timing.

Stage C5 requires a separately authorized non-production Google account and an approved test deployment. Verify a single secondary calendar after reconnection, pending and confirmed enrollment, one event after uncertain insertion, the same event after rescheduling, cancellation cleanup, protected application navigation, revoked access, and disconnect residue. Verify provider quotas and inspect retry behavior before rollout. No real credentials or production operations belong in CI.

Provider references: [calendars.insert](https://developers.google.com/workspace/calendar/api/v3/reference/calendars/insert), [calendars.get](https://developers.google.com/workspace/calendar/api/v3/reference/calendars/get), [calendarList.list](https://developers.google.com/workspace/calendar/api/v3/reference/calendarList/list), [events.insert](https://developers.google.com/workspace/calendar/api/v3/reference/events/insert).

## Review limitations

Synchronization is eventually consistent. A provider request already in flight at access revocation cannot be recalled; current application Join authorization denies access immediately. Persistent deletion markers allow corrective cleanup when credentials remain usable. Once disconnected, credential revocation takes priority over remote event deletion. Historical orphan calendars require owner/operator attention.

With the restricted app-created scope, Google offers no calendar-list recovery for a lost create response. UNCERTAIN provisioning therefore needs manual verification. A confirmed missing known calendar is retried by ID after explicit retry and is never replaced blindly. The secondary calendar's destination marker must match before any event mutation.

The transactional projector currently scans the eligible connected roster after a bounded notification batch. This trades a smaller implementation for an explicit scale limit. Observe database query latency before a larger rollout. During an unprojected committed batch, status stays pending so it cannot claim that the latest schedule has already reached Google.

No local tests, builds or servers were executed. V13 is a prepared migration file. All C4 runtime, concurrency and screenshot results await hosted acceptance. The preceding C3 baseline is d78c423901e0aa286775d5e18af2645b19648b70, verified at https://github.com/viethoang4254/english-tutoring-platform/actions/runs/43786628507.

## Prepared coverage

| Area | Hosted test location |
| --- | --- |
| Calendar creation, reuse and uncertain outcomes | CalendarSyncWorkerTests; GooglePersistenceTests |
| Payment activation and pending-payment exclusion | ReconciliationPersistenceTests.calendarProjectionFollowsPaidAccessOwnershipAndSchedule |
| Ownership, locked accounts, enrollment and course cancellation | ReconciliationPersistenceTests; GoogleServiceTests; GoogleCalendarHttpTests |
| Reschedule identity, cancel/delete and DST instants | CalendarSyncWorkerTests; ReconciliationPersistenceTests |
| Concurrent leases, restart, stale completion and bounded attempts | GooglePersistenceTests.workerLeaseIsExclusiveAndOldCompletionCannotOverwriteLatestRevision |
| Provider timeout, 401, 403, 404, 409, 429 and 5xx | CalendarSyncWorkerTests; CalendarTransportTests; existing GoogleProviderTests |
| Conditional update/delete and path ownership | CalendarTransportTests |
| Transactional schedule changes and legacy mapping retention | CoursePersistenceTests.personalSyncSchedulesAfterCommitAndRetainsLegacyReview |
| Revoked credentials and account-switch isolation | GoogleServiceTests; existing C3 persistence races |
| Pending, synchronized, error, retry, reconnect and ambiguous provisioning UI | frontend/tests/e2e/calendar-sync.spec.ts |

The eight screenshot names use student-sync-synced, student-sync-pending, student-sync-failed and teacher-sync-synced, each with -desktop.png and -mobile.png. All previous thirty screenshot captures remain registered.

Google versioned writes use If-Match as documented in [conditional modification](https://developers.google.com/workspace/calendar/api/guides/version-resources). No live Google provider result is claimed.

## Changed-file inventory

38 paths in this Stage C4 patch. Final static review corrected the V8-to-V13 migration expectations in payment and receiving-account persistence tests (five new migrations), and retained projection of already-mapped historical sessions so delayed work cannot retain an outdated schedule. The reconciliation persistence scenario covers this historical-update case without broadening initial backfill beyond upcoming sessions.

- `.github/workflows/task009-acceptance.yml`
- `backend/src/main/java/com/englishlearning/features/auth/security/GoogleCalendarSecurity.java`
- `backend/src/main/java/com/englishlearning/features/course/service/CourseService.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/CalendarSyncStore.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/CalendarSyncWorker.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/CalendarTransport.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleController.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleService.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleSettings.java`
- `backend/src/main/resources/db/migration/V13__personal_calendar_sync.sql`
- `backend/src/test/java/com/englishlearning/features/auth/security/GoogleCalendarHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryLiveHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryMigrationTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/course/CoursePersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/enrollment/EnrollmentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/CalendarSyncWorkerTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/CalendarTransportTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GooglePersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GoogleServiceTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/PaymentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/ReconciliationPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/paymentaccount/ReceivingAccountPersistenceTests.java`
- `ci/task009/backend_tests.py`
- `ci/task009/run.py`
- `docs/API_DESIGN.md`
- `docs/ARCHITECTURE.md`
- `docs/BUSINESS_RULES.md`
- `docs/DATABASE_DESIGN.md`
- `docs/FEATURE_STATUS.md`
- `docs/STAGE_C4_CALENDAR_SYNC.md`
- `docs/TASK_BREAKDOWN.md`
- `frontend/playwright.task009.config.ts`
- `frontend/src/features/google-calendar/google-connection.tsx`
- `frontend/src/features/student/student-calendar.tsx`
- `frontend/tests/e2e/calendar-sync.spec.ts`
- `frontend/tests/support/google-calendar.ts`
- `frontend/tests/support/task009-reporter.ts`
