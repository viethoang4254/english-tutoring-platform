# Public pages and four demo courses

Status: IN_PROGRESS — implementation prepared; hosted validation pending.
No actual TEST or production records were created.

## Public pages and contact contract

`/about` explains whole-course learning, scheduled Meet classes, Teacher onboarding,
and Student-to-Teacher tuition. `/contact` offers name, email, subject and message.
The existing public header, mobile menu and footer link to both pages. Contact uses
a breadcrumb, information panel beside the form, optional phone field and consultation
section. No map is shown because no verified office location is supplied. The three
referenced 28TECH images were unavailable in the continuation attachment; layout follows
its written structure and the existing application theme, not an asserted visual match.

`POST /api/v1/auth/contact` deliberately reuses the existing anonymous browser
transport and `/api/v1/auth` CSRF cookie scope. The caller first obtains the masked
token from `GET /api/v1/auth/csrf`; the POST requires its cookie and X-XSRF-TOKEN
header. No account or role is needed. Existing origin and transport checks apply.

Validation limits: name 120 characters, email 254, optional phone 24, subject 160, message 5000.
Blank fields, malformed email and control characters are rejected. Plain-text
SMTP uses a configured fixed recipient and sender; user email is Reply-To only.
No input becomes a recipient list or HTML. SMTP failures return a fixed safe code,
never a provider exception, credential or submitted message.

Delivery is OFF by default. Separate authorization is required before configuring
`APP_CONTACT_ENABLED=true`, `APP_CONTACT_RECIPIENT` (a real approved support mailbox),
`SMTP_FROM`, and the existing authenticated STARTTLS-required SMTP settings.
Without configuration, POST returns 503 CONTACT_UNAVAILABLE and the UI says it
has not sent the message. There is no invented contact address or success response.
200 ACCEPTED_BY_MAIL_SERVER means SMTP accepted the message, not verified inbox
delivery. A failed/ambiguous send has no automatic retry or success claim.

Abuse protection is CSRF/origin validation, bounded fields and a process-wide
20-attempt/hour limit (including failed sends). This deliberately bounds outgoing
mail without retaining sender IP addresses or adding database tables. It can deny
legitimate traffic when exhausted and resets on process restart. Before an internet
deployment or multiple replicas, add a shared edge rate limit. Support-mailbox
access and message-retention policy still require operational configuration.

## Demo data

One shared JSON source: `backend/src/test/resources/demo-courses.json`.
All dates use Asia/Ho_Chi_Minh. Each course has five 90-minute weekly sessions:

| Course | Tuition VND | Level | Five session dates | Local time |
| --- | ---: | --- | --- | --- |
| Tiếng Anh giao tiếp cơ bản | 500000.00 | Beginner | Nov 19, Nov 26, Dec 3, Dec 10, Dec 17, 2026 | 17:00–18:30 |
| Luyện thi TOEIC nền tảng | 650000.00 | Beginner / Intermediate | Nov 20, Nov 27, Dec 4, Dec 11, Dec 18, 2026 | 17:00–18:30 |
| IELTS Speaking cơ bản | 750000.00 | Intermediate | Nov 21, Nov 28, Dec 5, Dec 12, Dec 19, 2026 | 09:00–10:30 |
| Tiếng Anh giao tiếp công sở | 600000.00 | Intermediate | Nov 22, Nov 29, Dec 6, Dec 13, Dec 20, 2026 | 09:00–10:30 |

Descriptions, distinct objectives/outcomes, level-specific prerequisites and five
lesson objectives are included in JSON. Numbered roadmap topics and their objectives
use existing course-outline fields; Session title/content receives the matching
topic/objective. Level is descriptive audience text,
not a new schema field. Categories are created through the existing category service
inside the disposable fixture; the existing approved synthetic Teacher owns all four.

`DemoCourseFixture.seed` is test-classpath-only, guarded to the exact hosted disposable
course database. It runs in a transaction with a seed advisory lock. Reruns reuse
matching marked courses; collisions or divergent snapshots fail closed. It never
repairs unrelated courses. Course creation, outline updates and session generation
use existing services. No users, payments, enrollments, Meet links or Google events
are created by this seed. No database migration is added.

The four persisted courses are **DRAFT**. Publication requires a legitimate
Teacher-provided Meet URL, approved receiving account and future valid sessions.
The test asserts rejection without a Meet URL and that drafts stay out of public
discovery; existing successful publication and protected Meet suites remain intact.
The browser screenshots use clearly synthetic public API projections of the shared
course descriptions. They are not evidence that the database drafts are published.

## Hosted validation

- ContactHttpTests: real CSRF bootstrap, missing-CSRF rejection, validation,
  fixed-recipient SMTP capture using a mock sender, safe SMTP failure.
- ContactTests: default-off delivery, missing configuration, global attempt bound.
- CoursePersistenceTests: fixed demo clock, four courses/twenty sessions,
  idempotence, ownership denial, chronological/nonoverlapping schedules, no payments,
  unrelated-course preservation and publication rejection. Disposable PostgreSQL only.
- public-pages.spec.ts: desktop/mobile navigation, required/email validation,
  unavailable feedback, explicit retry and SMTP-acceptance feedback with API fixtures.
- demo-courses.spec.ts: four-course discovery, five-topic detail, no Meet links,
  viewport overflow checks and synthetic public screenshots.

TASK-009 retains all previous groups and adds strict PNG size validation:

- `public-pages-review-screenshots`: about-desktop.png, about-mobile.png,
  contact-desktop.png, contact-mobile.png.
- `demo-courses-review-screenshots`: course-discovery-desktop.png,
  course-discovery-mobile.png, demo-course-detail-desktop.png,
  demo-course-detail-mobile.png.

The demo artifact additionally requires `student-course-roadmap-desktop.png` and
`student-course-roadmap-mobile.png`. The shared public/Student detail component
displays the API-backed “Lộ trình khóa học” section, expandable topic objectives,
and planned weekly times/duration. It does not manufacture concrete session dates
from recurring rules or expose protected Session content through a new endpoint.
The authenticated Student schedule remains authoritative for reschedules.
Browser tests inspect all four roadmaps in the Student shell. These are synthetic
published projections, not a bypass allowing Students to see the actual CI drafts.

Required widths are 1440 and 390 pixels. No local builds/tests or hosted dispatch
were performed while preparing this implementation.

## Separate TEST deployment approval

1. Review and authorize commit/hosted CI, then package the exact successful revision.
2. Independently verify TEST identity/TLS, current migrations and recovery point;
   review installation without overwriting the current runtime or secrets.
3. Approve exactly the four courses, an eligible owning Teacher and existing active
   categories. Translate the shared data through authorized Teacher APIs; the CI-only
   seed is intentionally not executable against TEST. Keep delivery flags disabled.
4. Obtain the real Teacher-provided Meet URL and approved receiving account before
   publication; use normal publication API and verify ownership and protected access.
   If dates are already past, stop and approve a new future schedule rather than
   bypassing scheduling rules. Do not invent payments or enrollments.
5. Contact email delivery needs separate recipient/sender approval and configuration;
   no real email is authorized by preparing these pages.

### Resumable TEST creation procedure (not executed)

After the preceding approvals, use one authorized Teacher session and one operator.
Record the approved Teacher ID, category IDs, dataset checksum and returned course
IDs in a private execution journal outside Git; never store bearer tokens there.
Paginate `GET /api/v1/teacher/courses` before creating anything. Stop on ambiguous
name/owner matches; never adopt or edit an unrelated course by name alone.

For each of the four definitions:

1. If no journaled or matching marked course exists, call `POST /api/v1/teacher/courses`
   with the approved category, tuition, capacity, start date, timezone and count five.
   Record its returned ID immediately. After timeout or connection loss, stop and
   reconcile the owning-Teacher list/detail before any retry: create is not an
   idempotency-key API. Do not run concurrent seed operators.
2. For a journaled ID, GET detail and compare owner-visible fields and DRAFT status.
   Reuse an exact match; stop on differences. PUT `/{id}/outline` only for the approved
   demo, mapping objectives/outcomes/prerequisites and numbered topics from JSON.
3. GET `/{id}/schedule` and `/{id}/sessions`. With zero sessions, install the exact
   weekly rule via PUT `/{id}/schedule`, then POST `/{id}/sessions/generate` once.
   With five matching sessions, skip generation. Any other count or time mismatch
   is a blocker, never permission to regenerate or delete rows.
4. PATCH only these five `/{id}/sessions/{sessionId}` records with their matching
   topic and objective when not already equal. Read back all twenty sessions, check
   their 90-minute durations and no overlap for the owning Teacher.
5. Stop for publication approval. Confirm a genuine Teacher-supplied Meet URL and
   approved ACTIVE receiving account using the existing authorized workflows.
   POST `/{id}/publish` with the real `meetUrl`; on an ambiguous response, GET the
   current course before retrying. Verify all four IDs through the public course API
   and website. No Student enrollment or confirmed payment is needed for discovery.

All relative paths above are under `/api/v1/teacher/courses`. This is a safe,
read-before-write operating procedure, not a claim of globally atomic API seeding.
The existing CI helper must remain blocked from TEST. TEST account approval,
receiving-account status, Meet URLs, backup usability and current migrations have
not been reverified in this task. The inspected historical runtime manifest refers
to commit 22a29b7, so deploy only a newly verified compatible backend/frontend pair.
No new migrations are introduced here; validate the packaged source's existing
V1–V15 requirements and separately approve any missing migrations. Keep contact,
Calendar invitations, synchronization and reminders disabled throughout creation.

For the November 19 plan, the first session is at 17:00 Asia/Ho_Chi_Minh. If any
session would no longer be in the future when generating/publishing, obtain approval
for a revised schedule; never alter the server clock or relax publication checks.
