# Stage C1 — Real Student Calendar

Status: **IN_PROGRESS** — implementation prepared for code review; hosted acceptance not executed.

Baseline: `codex/course-checkout` at `0f125ac880920bbb820f4f0ec4a65fd8ee0d8822`.
Implementation branch: `codex/calendar-google-integration` in the isolated
`.worktrees/calendar-google-integration` checkout. No changes to the checkout,
Admin CRUD, dev or main branches. No production database access or migrations.

## API and authorization

`GET /api/v1/student/calendar/sessions?from=…&to=…&page=0&size=20`

- JWT authenticated, unlocked STUDENT; mailbox verification is not required.
- Authenticated identity only: no caller-selected student ID.
- Own ACTIVE enrollment AND at least one CONFIRMED Payment AND PUBLISHED Course.
- Both SCHEDULED and CANCELLED Sessions remain visible; other enrollment/payment/course
  states cannot qualify on their own. A historical confirmed Payment is the same
  access criterion used by existing protected course access.
- Required ISO offset-bearing `from`/`to`; years 1–9999, `from < to`, at most 62
  elapsed days. Range uses absolute instants, including offset/DST transitions.
- Page defaults to 0, size to 20; size 1–100 and nonnegative integer page with
  `page * size <= Integer.MAX_VALUE`. Unknown/duplicate query parameters and bodies
  are rejected. Invalid requests return 400; missing/locked authentication 401;
  other roles 403; unavailable persistence 503 with sanitized errors.
- Half-open overlap: `scheduled_start_at < to AND scheduled_end_at > from`.
  Sort by start time then session ID. `EXISTS` prevents duplicates from payment
  history and schedule-change history. Uses read-only transactions and SELECT only.
- No Google Meet URL, Session content, email, bank evidence, Payment snapshot or
  other Student data is projected. Responses use `Cache-Control: no-store`.

Response: `{items, page, size, hasNext}`. Each item contains string-safe `id` and
`courseId`, `courseTitle`, `teacherDisplayName`, `sessionNumber`, nullable `title`,
`startsAt`, `endsAt` (UTC instants), `timezone` (Course IANA ID), `status`, and
`rescheduled` (at least one existing schedule-change history row).

No new tables, indexes, migrations, status values or payment operations.

## Dashboard behavior

The overview uses this API instead of production-bundled demonstration courses and
sessions. Existing calendar styling, month navigation, selected-day details and
upcoming panel remain. The calendar grid uses Vietnam local dates (UTC+7), includes
adjacent-month days and correctly handles overnight overlap. Session details retain
the Course timezone and identify the Course and Teacher. Cancelled sessions remain
on the grid and in day details, but not in upcoming sessions. History-backed changes
are labeled “Đã đổi lịch”. Safe links open the existing Student Course detail route;
there is no Join action or Meet URL in the calendar.

The overview loads all 100-item server pages sequentially before rendering a complete
visible grid. Month changes hide stale data; obsolete requests cannot update the view
or continue pagination. Loading, empty, denied and retryable failure states are distinct.
No synthetic fallback, polling or automatic authentication retry. Returning from a
course/payment route remounts the calendar and reads current eligibility. Manual
“Tải lại lịch học” is also available. Profile loading/error behavior is unchanged.

Upcoming means the next three scheduled future sessions **within the displayed grid**,
not an unbounded all-time query. Offset pages are not a cross-request database snapshot;
a detected duplicate caused by concurrent rescheduling fails safely and offers retry.
A refresh is necessary to see subsequent schedule or enrollment changes.

## Hosted verification prepared (not run)

- `StudentCalendarHttpTests`: 5 tests for role/locked-account enforcement, actor binding,
  no-store/safe DTOs, read-only annotation, offsets/DST, defaults, 62-day and pagination
  bounds, duplicate parameters/body rejection, sanitized infrastructure errors.
- `PaymentPersistenceTests`: 2 additional transactional rollback tests on its existing
  empty disposable runner PostgreSQL database. Eligibility matrix, cross-Student
  isolation, no mutation, multiple confirmed Payments/history deduplication, stable
  pagination, cancelled/rescheduled sessions, timezone/DST instants and exact overlap
  boundaries. The suite retains its exact localhost URL and disposable database guard.
- `student-calendar.spec.ts`: 12 scenarios × desktop/mobile = 24 executions. Rendering,
  changes, month requests, page draining, loading, empty, retry, 401/403, stale-response
  isolation, return navigation/refetch, overnight boundaries and inconsistent pages.
- Existing Student Dashboard and Course discovery tests now mock the calendar API
  instead of operating a production demo toggle. Their case counts stay unchanged.
- Existing live Course Publication test additionally checks no calendar access while
  Payment is pending, two eligible sessions after owning-Teacher confirmation, no Meet
  leakage and no sessions for a second Student. Existing reconciliation and protected
  course-access assertions remain intact.
- TASK-009 registers the HTTP suite, retains persistence groups, adds a 24-case browser
  step and four screenshot captures. Existing SMTP, backend, Teacher Calendar,
  Checkout/VietQR, live acceptance, 10 UI screenshots and 6 checkout screenshots remain.

New artifact: `student-calendar-review-screenshots`, full-page PNGs:

- `student-calendar-desktop.png` (1440 × at least 900)
- `student-calendar-mobile.png` (390 × at least 844)
- `student-calendar-changes-desktop.png`
- `student-calendar-changes-mobile.png`

Screenshots use deterministic, **mocked API fixtures**, not production accounts or
proof of live database integration. CI verifies PNG headers and widths. The separate
persistence/live tests establish real API behavior on disposable CI data.

## Remaining stages

- C2: session-specific protected Join authorization and any approved Teacher Calendar
  consistency work; existing protected course access remains unchanged.
- C3: Google account connection/disconnection, subject to separate approval and the
  organizational-calendar/security decisions. No OAuth implementation in C1.
- C4: durable one-way synchronization, event updates/cancellation, idempotency and
  recovery. No Google resources or event mappings changed in C1.
- C5: hosted acceptance and visual review on the exact approved commit, then later
  integration verification for separately approved C2–C4 work.

Next: focused code review, explicit approval to commit/push, then authorized TASK-009
execution. Do not claim completion or CI success before that run.

## Change inventory

Backend:
- `backend/src/main/java/com/englishlearning/features/auth/security/SecurityConfiguration.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/controller/EnrollmentController.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/dto/StudentCalendarSessionDto.java` (new)
- `backend/src/main/java/com/englishlearning/features/enrollment/repository/EnrollmentRepository.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/service/EnrollmentService.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/StudentCalendarHttpTests.java` (new)
- `backend/src/test/java/com/englishlearning/features/payment/PaymentPersistenceTests.java`

Frontend:
- `frontend/src/features/student/student-dashboard.tsx`
- `frontend/src/features/student/student-dashboard.module.css`
- `frontend/src/features/student/student-calendar.tsx` (new)
- `frontend/src/features/student/calendar-dates.ts` (pure date helpers moved from demo file)
- `frontend/src/features/student/dashboard-preview.ts` (removed production demo data)
- `frontend/tests/support/student-calendar.ts` (new synthetic API fixture)
- `frontend/tests/e2e/student-calendar.spec.ts` (new)
- `frontend/tests/e2e/student-dashboard.spec.ts`
- `frontend/tests/e2e/course-discovery.spec.ts`
- `frontend/tests/live/course-publication.spec.ts`
- `frontend/tests/support/task009-reporter.ts`

CI and documentation:
- `frontend/playwright.task009.config.ts`
- `ci/task009/backend_tests.py`
- `.github/workflows/task009-acceptance.yml`
- `docs/API_DESIGN.md`
- `docs/FEATURE_STATUS.md`
- `docs/STAGE_C1_STUDENT_CALENDAR.md` (new)
