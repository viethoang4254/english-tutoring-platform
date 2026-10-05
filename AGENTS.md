# English Tutoring Platform — Agent Instructions

## 1. Project Mission

Project title: **Development of an English Tutoring Platform**.
Vietnamese title: **Xây Dựng Nền Tảng Gia Sư Dạy Tiếng Anh**.

Build a responsive, multi-teacher English tutoring web platform. Teachers publish
and manage Courses; Students purchase entire Courses, participate in scheduled
Sessions and submit Assignments. Admin manages and monitors the platform.

The three authorization roles are STUDENT, TEACHER and ADMIN.

This graduation project prioritizes clear requirements, maintainable architecture,
correctness, usability, security, testability and demonstrable functionality over
unnecessary complexity.

---

## 2. Development Stage and Infrastructure Preservation

The project is reconciling the approved Physical Database V1 for English tutoring.
Existing technical foundations have already been implemented; this is not a
repository restart or authorization to begin the new business implementation.

Preserve completed TASK-001 backend foundation, TASK-002 frontend foundation and
TASK-003 PostgreSQL/Supabase connectivity, including their status and verification
evidence. A domain change does not invalidate those completed checks.

Do not delete, reset, recreate or revert the repository or its commits. Preserve
unrelated and uncommitted work. Do not modify completed infrastructure unless a
concrete conflict requires an explicitly approved change. Do not rename technical
packages, artifacts, directories or databases merely to match the new title.

Do not begin feature implementation until relevant requirements and business
rules have been defined and the affected design stages reviewed.

Follow this development order:

1. Requirements
2. Business Rules
3. Use Cases
4. Domain Design
5. System Architecture
6. Database Design
7. UI/UX Design
8. API Design
9. Implementation
10. Testing
11. Deployment

Do not skip directly to implementation.

---

## 3. Source of Truth During Scope Transition

This file records the approved English Tutoring Platform direction. Conflicting
old-domain rules in existing documents and project-local skills are superseded
by this direction; they must not be carried into new requirements or implementation.
Compatible technical and security instructions remain applicable.

Consult the following documents, identifying which sections have been reconciled
with the new scope before relying on them:

- `docs/PROJECT_SPEC.md`
- `docs/REQUIREMENTS.md`
- `docs/BUSINESS_RULES.md`
- `docs/USE_CASES.md`
- `docs/DOMAIN_MODEL.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE_DESIGN.md`
- `docs/API_DESIGN.md`
- `docs/TASK_BREAKDOWN.md`
- `docs/FEATURE_STATUS.md`

The current documents record the approved 22-table Physical Database V1 direction
and explicitly retain migration-blocking clarifications. Historical 18-table,
72-operation, 22-feature and 30-task baselines do not constrain the tutoring model.
Do not implement legacy planned work merely because it has a task ID.
Preserve completed infrastructure IDs and evidence when future plans are revised.

Do not silently resolve remaining contradictions or invent business requirements.
Report conflicts, distinguish approved decisions from open questions and obtain
approval before dependent changes. This transition does not authorize editing
skills, other documentation, application code or database objects automatically.

---

## 4. Platform

The product is a web application.

All roles must be able to use the platform through a modern web browser.

### Student

Student interfaces must be:

- responsive
- mobile-first
- touch-friendly
- convenient for learning on smartphones
- usable on tablets and desktop devices

Students must not be required to install a native mobile application.

### Teacher

Teacher interfaces must be:

- responsive
- optimized primarily for desktop management workflows
- still functional on mobile devices

### Admin

Admin interfaces must be:

- responsive
- optimized primarily for desktop dashboards and management
- still functional on mobile devices

---

## 5. Student Capabilities

Students can:

- register and log in using email and manage their personal profile;
- search/filter Teachers and Courses and view Teacher specialization, experience
  and profile information;
- view Course name, description, Teacher, tuition, schedule and Session count
  where applicable;
- register for and pay for an entire Course;
- access the corresponding Course through valid/active Enrollment after payment
  is successfully confirmed;
- view enrolled Course Sessions, learning content and Assignments;
- receive/view learning schedules through Google Calendar;
- join classes using the Course's Google Meet URL;
- complete/submit Session Assignments and view results and learning progress;
- provide one participant-feedback rating (1..5) for a COMPLETED Enrollment;
  editing/moderation details remain open.

---

## 6. Teacher Capabilities

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

Teachers retain permitted own-profile editing, including teaching information. They
create/manage/publish owned Courses and configure name, description, tuition,
schedule, capacity and Admin-managed Category under the approved rules.

Teachers manage owned Courses, Sessions, protected content and Assignments; inspect
enrolled-Student work/progress; grade only within owned Courses; maintain private
bank-account information and receive tuition directly.

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference.

V1 supports full refunds only, with at most one Refund per Payment. The amount equals the
applicable full Payment amount under the approved workflow. Teacher performs the bank
transfer back to the Student and submits proof; Admin verifies completion. Refund statuses
are PENDING, SUBMITTED, COMPLETED and CANCELLED. Payment remains historical and has no
REFUNDED status. This does not authorize platform custody, payouts, commissions, escrow or
accounting.

---

## 7. Admin Responsibilities

Admin can manage Student/Teacher accounts and relevant profile information,
lock/unlock accounts where required, manage/monitor Courses, manage teaching
categories/topics, monitor Enrollments and Student-to-Teacher payment status,
and view system statistics.

Statistics include total users, Teachers, Students, Courses and appropriate
Enrollment/transaction statistics. Admin is not the recipient of Teacher Course
payments. Admin verifies submitted full-refund completion without holding funds.
Do not reinterpret monitored transaction totals as Admin revenue.

Detailed Admin permissions and override boundaries require business rules;
the ADMIN role does not itself justify arbitrary access or mutation.

---

## 8. Course Structure and Teacher Ownership

```text
Platform -> many Teachers
Teacher -> many Courses
Course -> one owning Teacher
Course -> many Students through Enrollment
Course -> many Sessions
Session -> learning content and Assignments
Assignment -> Student submissions/results
```

Teacher management requires backend verification of authenticated identity,
TEACHER role, ownership of the parent Course and permission for the operation.
Teacher role alone never permits modifying another Teacher's Course.

Traverse Session -> Course and Assignment -> Session -> Course for nested
resources. Apply the same ownership boundary to enrolled-student information,
submissions/results, progress and related teaching/payment data.

Concepts such as User, StudentProfile, TeacherProfile, Category, Course,
CourseSchedule, Session, Enrollment, Payment, Assignment, AssignmentSubmission
and Review are responsibilities represented by the approved 22-table direction in
DATABASE_DESIGN.md, not permission to create additional tables/entities.
Do not assume separate login identities for profile concepts. Do not introduce
TeachingService without a demonstrated business requirement; Teachers directly
publish/manage Courses. Resolve remaining physical gates before migration work.

---

## 9. Course Purchasing and Enrollment

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots.

Backend-authoritative Enrollment protects Sessions, content, Assignments and the
Course Meet URL. Client success flags never grant access.

Actual provider/bank transactions may be unmatched. They retain receiving-account context
when resolvable, independently of Payment matching; an unresolved receiver remains a
reconciliation concern. Browser, Student and Teacher claims are not confirmation evidence.
Confirmation requires trustworthy provider/bank evidence matching the intended receiver,
code, amount and currency; wrong/missing codes or amounts do not auto-confirm, partial
transfers are not summed automatically, and late transactions cannot cause overbooking.

---

## 10. Sessions, Assignments and Learning Progress

Assignments belong to Sessions and use ACTIVE/CANCELLED. Any Submission prevents hard
deletion of its Assignment. There is one current Submission per Student/Assignment, using
DRAFT/SUBMITTED/GRADED; no revision-history, result or grading table is introduced. Score,
feedback and grading metadata remain on Submission. Only the owning Teacher grades, and a
score cannot exceed Assignment max_score. A numeric score is not made mandatory merely by
GRADED status.

Supabase Storage holds Teacher avatars, Course thumbnails, Assignment files, Submission
files and refund proof. PostgreSQL stores paths/references and applicable metadata, never
file bytes, base64 or temporary signed URLs. Resolve each path within an explicitly
configured bucket for its usage; exact bucket identifiers remain configuration, and the
path/bucket mapping must be fixed before integration. Spring Boot authorizes access; Storage
does not replace backend business authorization.

Progress remains an authorized derived view; its formula is not yet selected.
Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.
There is no Quiz or attendance feature.

---

## 11. Course Schedule and Session Scheduling

Generate exactly session_count Sessions chronologically from planned_start_date
(the earliest permitted date, not necessarily a matching weekday), using weekly rules
with ISO weekdays 1=Monday through 7=Sunday. Number Sessions 1 through session_count,
unique within the Course. Combine date and local times with the required backend-validated
IANA Zone ID to persist TIMESTAMPTZ instants. Same-Course rules must not overlap;
validate overlap transactionally. Draft inputs may change and generated Sessions may
be regenerated only before meaningful historical/business activity. Before publication,
Sessions exist for Teacher review and a valid Teacher-provided Meet URL is required.
After publication, concrete timestamps are authoritative; do not blindly regenerate.
Rescheduling updates the same row and Calendar event; cancellation preserves numbering
and count. Each concrete Session has its own event, never one recurring Calendar event.
Synchronization failure never rolls back core Course/Session state; retain durable
sync status and retry. DST gap/overlap validation remains an implementation contract.

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

Course timezone and weekly local time rules remain distinct from absolute Session
instants. Remaining time/numbering edge contracts are recorded in DATABASE_DESIGN.

---

## 12. Google Meet Boundary

One Course uses one Google Meet URL, manually entered by its Teacher after creating
the meeting externally. All Sessions in that Course use the same Course Meet URL.

Only appropriately enrolled/authorized Students may access protected participation
information. Backend access checks must protect the Meet URL; hiding a UI element
is insufficient. Validate supplied URLs according to the later approved contract.

Do not automatically create meetings. Do not integrate a Google Meet API or create
a Google Meet entity unless a later requirement explicitly justifies it.

---

## 13. Google Calendar Boundary

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

---

## 14. Payments and Confirmation Uncertainty

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference.

Actual provider/bank transactions may be unmatched. They retain receiving-account context
when resolvable, independently of Payment matching; an unresolved receiver remains a
reconciliation concern. Browser, Student and Teacher claims are not confirmation evidence.
Confirmation requires trustworthy provider/bank evidence matching the intended receiver,
code, amount and currency; wrong/missing codes or amounts do not auto-confirm, partial
transfers are not summed automatically, and late transactions cannot cause overbooking.

V1 supports full refunds only, with at most one Refund per Payment. The amount equals the
applicable full Payment amount under the approved workflow. Teacher performs the bank
transfer back to the Student and submits proof; Admin verifies completion. Refund statuses
are PENDING, SUBMITTED, COMPLETED and CANCELLED. Payment remains historical and has no
REFUNDED status. This does not authorize platform custody, payouts, commissions, escrow or
accounting.

Physical V1 omits provider_account_ref, provider_order_id and provider-order uniqueness.
Adapter mapping/idempotency is integration work; never assert unverified VietQR guarantees.

---

## 15. Retired Domain and Scope Control

The old vocabulary-learning business domain is obsolete. Retain its concepts only
where necessary for clearly identified historical documentation; do not implement
or carry forward its business rules into the tutoring platform.

Explicitly retired:

- Vocabulary learning/search/saved vocabulary and vocabulary mastery/review;
- dictionary integration and vocabulary pronunciation/audio;
- vocabulary Lessons and Fill Word/Listening exercises;
- Quiz and the old question/attempt/answer exercise engine;
- Standard/Premium subscriptions, access tiers and Premium content gating;
- Standard/Premium Course classification and platform subscription revenue rules.

Review now means participant feedback/rating of a Teacher or Course, not vocabulary
practice. Do not mechanically rename legacy Lessons as Sessions or exercise attempts
as Assignment submissions. CEFR-centric structure is not an assumed requirement
of the new domain.

The former blanket exclusion of online tutoring and Student-to-Teacher Course
payments no longer applies to the approved capabilities above. This does not approve
built-in video infrastructure, chat/social features, native apps, AI tutors,
certificates, leaderboards or other speculative extensions. Apply YAGNI.

---

## 16. Security and Authorization

Spring Boot owns authentication, authorization, role assignment, account eligibility,
Teacher ownership, Enrollment access, payment-related business rules and database
access. Keep Spring Security + JWT and authentication-specific components under
`features/auth/security/`; do not implement new security policy during this transition.

Use the three roles STUDENT, TEACHER and ADMIN. Payment/Enrollment state must not
be represented as a role. Client-selected identity, ownership, role or payment state
must not become authoritative through request binding.

Protect personal profiles, submissions, results and participation information through
backend authorization. Hash passwords with supported security facilities, validate
inputs and keep secrets/credential material out of source control, DTOs and logs.
Do not expose JPA entities directly as API contracts or invent cryptography.

V1 uses users.locked as its account-blocking mechanism: a locked account cannot authenticate
or use normal account functionality. There is no separate disabled, enabled or
account_status field/lifecycle. Email uses lowercase(trim(inputEmail)) for storage/login; a verified email
change retains the old email until successful verification of the new one. Password reset
revokes all refresh sessions; logged-in password change revokes other sessions while
preserving the current session. Raw refresh, verification and reset secrets are not
persisted.

Exact operation contracts and unrelated Admin overrides remain open. Preserve
security safeguards without restoring old Student-only registration or Premium.

---

## 17. Open Decisions and Approval Gates

| Area | Remaining clarification; approved V1 decisions above are not reopened |
| --- | --- |
| Provider contracts | Physical V1 omits provider_account_ref/provider_order_id and provider-order uniqueness. Adapter mapping/authenticity and transaction idempotency remain integration work, not a physical gate; no unverified provider-wide transaction-ID guarantee. |
| Accounts/security | Canonical email is lowercase(trim(inputEmail)); password encoding/transport/rotation/replay, resend controls and operation-specific review contracts remain implementation gates. Locked is the sole account-blocking mechanism. |
| Physical restrictions | Resolved: nullable draft Meet URL with publication validation; required deadline; NUMERIC(5,2) scores; positive minimum; fixed planned Session count and unique numbering; complete 0<discount<100 window; VND only. |
| Calendar | One organizational account and one configured Calendar; backend-configured calendar ID, one event per Session, unique non-null (provider, external_event_id). Execution/retry/reminder details remain integration work. |
| Storage | Fixed bucket/path mapping, upload limits and file validation/delivery contracts; no binary or signed-URL persistence. |
| Workflow edges | Detailed publication/completion validations, first-start cutoff under schedule changes, cancellation/re-entry and late-payment reconciliation; do not invent extra lifecycle states or refund eligibility policy. |
| Progress/reporting | Indicators, formulas, filters and time boundaries; derive from authoritative data without progress/statistics tables. |
| Privacy/operations | Detailed retention periods, feedback editing/moderation, unrelated Admin overrides, UI/API details and deployment/production operations. |

G-PHYSICAL is RESOLVED for the final 22-table V1. TASK-004 is TODO and awaits
explicit implementation approval. Documentation reconciliation is not implementation.

---

## 18. Architecture and Infrastructure

Preserve the existing Java Spring Boot backend, Next.js/TypeScript frontend,
PostgreSQL database, modular monolith, REST API and approved toolchain versions.

```text
Browser -> Next.js -> Spring Boot REST API -> JPA/Hibernate -> PostgreSQL
```

Development PostgreSQL hosting is Supabase; preserve local PostgreSQL support and
the verified Session Pooler connectivity/configuration. Supabase supplies database
hosting, not application identity or business logic. Preserve TASK-003's evidence
and secret-management/TLS decisions; no database reconnection is required by the
domain change alone.

Do not enable Supabase Data API, Auth, frontend direct database access, supabase-js,
or RLS as application authorization. Supabase Storage is approved for the bounded file uses
above; Realtime and Edge
Functions remain unapproved. Hosting/deployment decisions must not
be inferred from the domain change.

Keep schema changes under separately approved migration work. Do not generate
migrations from the obsolete database design or enable automatic schema mutation.
Physical V1 specifies 22 tables; no database execution is authorized by this file.

Avoid unnecessary microservices, Redis, Kafka, RabbitMQ, Elasticsearch, Kubernetes,
event sourcing, CQRS or other infrastructure without a concrete requirement.

---

## 19. Implementation Principles

When implementation begins:

- keep business logic separate from presentation logic
- validate input
- enforce authorization server-side
- avoid duplicated domain logic
- use clear naming
- document important decisions
- write tests for important business rules
- do not expose secrets or API keys in source code
- do not hardcode environment-specific configuration
- keep controllers focused on HTTP concerns
- keep business workflows primarily in services
- keep persistence access in repositories
- use DTOs for public API boundaries where appropriate
- do not expose persistence entities directly through APIs by default
- avoid unnecessary circular dependencies between features

### Feature-Based Code Organization

The project must use feature-based organization.

Backend code must be grouped primarily by business feature/domain rather than globally by technical layer.

Prefer:

```text
course/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── mapper/
```

---

## 20. Feature and Task Status Tracking

- Before implementing or modifying a feature, read `docs/FEATURE_STATUS.md` and the corresponding task in `docs/TASK_BREAKDOWN.md`; inspect existing implementation and evidence first.
- `docs/FEATURE_STATUS.md` is the authoritative feature/task implementation-status source. Do not duplicate live status in `docs/TASK_BREAKDOWN.md`.
- Do not reimplement a feature marked DONE unless the user explicitly requests a change, bug fix, extension or refactor.
- Use only TODO, IN_PROGRESS, BLOCKED and DONE. Set feature/task status to IN_PROGRESS when implementation genuinely starts; leave partial work IN_PROGRESS unless a concrete unresolved dependency prevents further meaningful work, in which case record BLOCKED and its reason.
- Mark DONE only after completion criteria and required verification pass. Record actual implementation files, verification results, remaining work and commit references when available.
- Keep status synchronized with the actual codebase; documentation completion is not implementation completion. Never change status merely to make progress appear complete.
- Preserve approval-first behavior for material design or scope changes.
