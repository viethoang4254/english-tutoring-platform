# Stage C2 — Protected Meet access

Status: IN_PROGRESS. Implementation prepared for review; hosted verification pending.
Baseline: `c06d88de94e7d79ab872225903620af6e7bfb3d4` on
`codex/calendar-google-integration`. No migrations or infrastructure changes.

## API and security

- `GET /api/v1/student/sessions/{sessionId}/join`: returns only `{ "meetUrl": "https://meet.google.com/abc-defg-hij" }` after checking authenticated unlocked STUDENT, own ACTIVE Enrollment, CONFIRMED Payment, PUBLISHED Course, existing SCHEDULED Session and valid course URL. The example is synthetic, not a real class. Every request rechecks current state. Missing and ineligible resources uniformly return 403; no existence disclosure. Malformed IDs, queries and GET bodies return 400. Unauthenticated/locked accounts return 401. No join-time window is introduced.
- `GET /api/v1/teacher/courses/{courseId}/meeting`: approved unlocked owning Teacher only; email verification is not required. Returns the same shape, or `{}` if no usable URL is configured. Foreign/missing courses return 404. Wrong role or unapproved Teachers are denied. Existing publication URL validation is shared, unchanged.
- Both endpoints are read-only and return `Cache-Control: no-store`. IDs and identity are bound server-side. No writes, reservations, payments, OAuth or external calls. SQL uses EXISTS for confirmed payment checks. Student Calendar and course-access eligibility share the same SQL predicate, preserving existing policy.
- Only exact HTTPS `meet.google.com/abc-defg-hij` format is accepted, matching existing publication rules. No query, fragment, userinfo, alternate host or insecure scheme.
- Calendar and public responses do not gain Meet fields. Existing authorized course access remains intact.

## Browser behavior

Scheduled Student session cards offer “Tham gia Google Meet”; on click only, the authenticated endpoint resolves the current room and navigates in the same tab (no popup dependency). Cancelled sessions show a disabled action. Course navigation, latest rescheduled dates, month selection and pagination remain intact.

Teacher course details offer “Xem phòng Google Meet”, owner-only readback and “Mở Google Meet”. Ordinary opening rechecks authorization; missing configuration and denied/unavailable requests show safe Vietnamese feedback. URL state is memory-only and cleared before each lookup. Requests are not retried automatically. Duplicate clicks are blocked. Obsolete responses after resource/identity change or unmount cannot navigate or update the view. Invalid destinations are rejected defensively in the browser as well as the backend.

## Prepared verification (not executed locally)

- `MeetingHttpTests`: six HTTP cases covering identity, read-only/no-store, malformed URLs, wrong roles, locked accounts, unapproved Teacher, ownership, missing configuration, strict inputs and sanitized infrastructure failure.
- `PaymentPersistenceTests`: isolated PostgreSQL matrix for payment/enrollment/course/session states, ownership, cancelled/moved sessions, missing/malformed room, Teacher readback and locked Student. Existing migration fixture executes only in disposable hosted databases, never production.
- `meeting-access.spec.ts`: seven scenarios x desktop/mobile = 14 executions. Click-only resolution, synthetic destination navigation, rescheduled/cancelled sessions, 401/403/503 handling, stale-response suppression, approved email-unverified Teacher readback and authorization recheck. Real Google Meet participation is intercepted, never automated.
- Existing live Course Publication acceptance now checks pending Join denial, owner/cross-Teacher readback, confirmed-payment Join, cross-Student and wrong-role denial using real isolated APIs. Existing reconciliation and course-access assertions remain.
- TASK-009 registers the HTTP suite, existing payment persistence suite, explicit browser group and reporter source allowlist. Previous suites/counts are preserved.
- Independent screenshot job adds four fixture-only PNGs under `meeting-review-screenshots`: `student-meeting-desktop.png`, `student-meeting-mobile.png`, `teacher-meeting-desktop.png`, `teacher-meeting-mobile.png`. Desktop width 1440; mobile width 390. Existing 10 UI + 6 checkout + 4 Student Calendar captures remain.

## Limits and next step

An already copied room URL cannot be revoked by this application; Google controls admission to the actual room. Cancellation blocks subsequent session-specific API requests, not previously copied links. Teacher room readback is course-level, not a cancelled-session Join action. No scheduled-time gate is invented. Network/API errors require an explicit retry.

No Google OAuth, Calendar synchronization, event mapping or automatic Meet creation is implemented. No production records or credentials were accessed. After review approval, commit/push and run TASK-009 on that exact SHA; inspect all groups and screenshot artifacts before declaring completion.

## Changed paths

- `.github/workflows/task009-acceptance.yml`
- `backend/src/main/java/com/englishlearning/features/auth/security/SecurityConfiguration.java`
- `backend/src/main/java/com/englishlearning/features/course/controller/CourseController.java`
- `backend/src/main/java/com/englishlearning/features/course/repository/CourseRepository.java`
- `backend/src/main/java/com/englishlearning/features/course/service/CourseInput.java`
- `backend/src/main/java/com/englishlearning/features/course/service/CourseService.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/controller/EnrollmentController.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/repository/EnrollmentRepository.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/service/EnrollmentService.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/MeetingHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/PaymentPersistenceTests.java`
- `ci/task009/backend_tests.py`
- `docs/FEATURE_STATUS.md`
- `docs/STAGE_C2_MEETING_ACCESS.md`
- `frontend/playwright.task009.config.ts`
- `frontend/src/features/course/meeting-access.tsx`
- `frontend/src/features/student/student-calendar.tsx`
- `frontend/src/features/student/student-dashboard.module.css`
- `frontend/src/features/teacher/teacher-courses.tsx`
- `frontend/tests/e2e/meeting-access.spec.ts`
- `frontend/tests/live/course-publication.spec.ts`
- `frontend/tests/support/task009-reporter.ts`
