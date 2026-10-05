# Development of an English Tutoring Platform

# 1. Project Identity and Document Status

**Vietnamese title:** Xây Dựng Nền Tảng Gia Sư Dạy Tiếng Anh
**English title:** Development of an English Tutoring Platform
**Document:** Project Specification
**Status:** Approved tutoring and Physical V1 direction; remaining execution clarifications below
**Project Type:** Graduation Project

This specification describes the approved tutoring scope. It does not approve
unresolved business rules or authorize implementation.

---

# 2. Product Overview and Context

The product is a responsive, multi-teacher English tutoring web platform.
Students discover Teachers and their Courses, purchase an entire Course, and
participate in scheduled Sessions through valid Enrollment where required.

Teachers publish and manage owned Courses, learning content and Assignments.
Admin manages and monitors the platform within approved administrative boundaries.
The platform connects these activities without providing built-in video
conferencing or holding Course tuition for distribution to Teachers.

---

# 3. Project Objectives

- Help Students discover Teachers, their experience and suitable Courses.
- Support Teacher-owned Courses with tuition, schedules and individual Sessions.
- Support whole-Course purchasing with tuition paid directly to the owning Teacher.
- Govern protected participation through backend-authoritative Enrollment.
- Support Session content, Assignments, submissions, results and learning progress.
- Provide Course-level access to externally created Google Meet meetings.
- Support system-account Google Calendar synchronization without blocking core scheduling.
- Support one 1–5 feedback entry per completed Enrollment.
- Provide authorized account, category, Course, Enrollment and transaction oversight.

Prioritize correctness, usability, security, maintainability, testability and
demonstrable functionality appropriate to a graduation project.

---

# 4. Actors and High-Level Capabilities

The authorization roles are STUDENT, TEACHER and ADMIN.

## Student

Students can:

- register/log in using email and manage their personal profile;
- discover, search and filter Teachers and Courses;
- view Teacher profiles, specialization, teaching experience and introduction;
- view Course information, tuition/price and learning schedule;
- register for/purchase an entire Course and pay tuition to its owning Teacher's
  configured payment destination;
- access appropriately enrolled Courses, Sessions and Session learning content;
- access and submit Assignments according to later-defined rules;
- view their own Assignment results and learning progress;
- access the authorized Course Meet URL;
- use the approved system-account Calendar scheduling capability;
- review/rate a completed Enrollment under the approved feedback rules.

Detailed search filters, editable profile fields and interaction design are not
defined here.

## Teacher

Teachers can:

- register/log in and manage Teacher profile information, including specialization,
  teaching experience and introduction;
- maintain/configure their own payment receiving information/account at a high level;
- create, manage and publish multiple owned Courses;
- configure Course information, tuition/price, learning schedule and maximum
  Student count where applicable;
- create/manage Sessions with topic, date, start time, end time and learning content;
- create/manage Assignments for Sessions in their Courses;
- manage enrolled-Student teaching information for owned Courses;
- review Assignment submissions/results and monitor learning progress for owned Courses;
- view relevant Course payment transaction/status information;
- receive Course tuition directly through their configured payment destination.

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

## Admin

Admin can manage Student/Teacher accounts and relevant profile information,
lock/unlock accounts where authorized, oversee Courses, manage categories/topics,
monitor Enrollments and appropriate payment transaction/status information, and
view platform statistics.

High-level statistics include total users, Students, Teachers and Courses, plus
appropriate Enrollment and transaction statistics. Exact dashboards, formulas,
reports and aggregation periods are not defined here.

Admin verifies submitted full-refund completion. Admin is not the owner of Teacher
Courses, recipient or intermediary holder of Teacher tuition, or default grader. Monitoring
transactions
does not imply receiving or controlling Teacher Course revenue. Exact override
and moderation permissions remain unresolved.

---

# 5. Core Scope and Domain Relationships

- A Teacher owns many Courses, has Teacher profile information and has their own
  payment receiving information/account.
- Each Course belongs to exactly one Teacher, contains many Sessions and has many
  Students through Enrollment.
- A Course has tuition/price, learning schedule information and exactly one
  manually supplied Google Meet URL.
- A Session belongs to one Course, represents an individual learning occurrence,
  contains learning content and may contain Assignments.
- Each Assignment belongs to one Session.
- Enrollment represents the Student-Course participation relationship.
- Payment and Enrollment are related but distinct concepts.

Course is the teaching and commercial unit. Students purchase entire Courses,
never individual Sessions.

These business relationships are realized by the 22-table Physical V1 design in
DATABASE_DESIGN.md, subject to its remaining migration clarifications.
No TeachingService concept is introduced.

---

# 6. Course Discovery, Purchasing and Participation

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots.

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.

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

---

# 7. Scheduling and External Integration Boundaries

## Course and Session Scheduling

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

Absolute instants and Course-local weekly rules are distinct. Remaining timezone,
numbering and lifecycle edge contracts require clarification before dependent work.


## Google Meet

The Teacher creates Google Meet externally and manually supplies exactly one
Meet URL per Course. All Sessions of that Course use the same Course Meet URL.

The URL is protected participation information subject to backend authorization.
The application does not use the Google Meet API, create meetings automatically
or create a separate meeting per Session. A dedicated GoogleMeet domain concept
requires a later justified and approved requirement.

## Google Calendar

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

This is approved architecture, not implemented integration.

---

# 8. Assignments, Results, Progress and Participant Reviews

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

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.

Students see their own results/progress; Teachers monitor only owned Courses.
Progress/statistics are derived; exact formulas remain open. Retired assessment
engines and attendance tracking are not introduced.

---

V1 uses users.locked as its account-blocking mechanism: a locked account cannot authenticate
or use normal account functionality. There is no separate disabled, enabled or
account_status field/lifecycle. Email uses lowercase(trim(inputEmail)) for storage/login; a verified email
change retains the old email until successful verification of the new one. Password reset
revokes all refresh sessions; logged-in password change revokes other sessions while
preserving the current session. Raw refresh, verification and reset secrets are not
persisted.

---

# 9. Access, Ownership and Administration Principles

Spring Boot owns authentication, authorization and business rules. STUDENT,
TEACHER and ADMIN are roles; payment or Enrollment state must not become a role.

Teacher ownership derives through the parent Course for Sessions, Assignments and
related teaching resources. Teacher A must not manage Teacher B's Courses,
Sessions, Assignments, submissions, enrolled-Student teaching information,
protected participation information or protected payment information.

A Teacher's payment receiving information must not become manageable by another
Teacher. Course transaction visibility respects ownership and authorized Admin
access; it does not grant Admin control of Teacher revenue.

Student access to protected Course participation resources, including content,
Assignments and the Meet URL, depends on authoritative Enrollment checks where
required. Hiding controls in the frontend is insufficient authorization.

Admin access is limited to approved administrative responsibilities. Exact
override/moderation permissions and detailed access contracts belong to later rules.

---

# 10. Web Experience and Technical Baseline

The application is responsive and browser-based.

- Student workflows are mobile-first and usable from phone browsers, tablets and desktops.
- Teacher workflows are responsive and primarily desktop-oriented for Course,
  Session and Assignment management.
- Admin workflows are responsive and primarily desktop-oriented for administration.
- No native mobile application is required.

Preserve the approved technical direction:

- Frontend: Next.js, TypeScript and responsive web.
- Backend: Java Spring Boot, Spring Security/JWT and REST API.
- Architecture: modular monolith with feature-based organization.
- Database: PostgreSQL, with Supabase-hosted PostgreSQL for development.
- Local PostgreSQL remains supported for established development/testing needs.

Browser -> Next.js -> Spring Boot REST API -> PostgreSQL

The backend owns authentication, authorization, business rules, ownership checks,
Enrollment access checks and persistence access. The frontend must not directly
access PostgreSQL/Supabase. Supabase provides database hosting, not application
identity or authorization.

Do not introduce Supabase Auth, Data API, frontend database access, supabase-js or
RLS as application authorization. Preserve existing connectivity and approved
toolchain decisions; no new hosting/deployment choice is made here.

---

# 11. Out-of-Scope and Retired Capabilities

The retired business scope is not an active requirement:

- Vocabulary learning and the CEFR vocabulary hierarchy;
- Dictionary integration and vocabulary audio;
- vocabulary Lessons, Fill Word, Listening exercises and Quiz;
- the old exercise/attempt/scoring/mastery model and weak vocabulary;
- saved vocabulary and vocabulary Review;
- Standard/Premium tiers, Premium gating, subscription access and subscription revenue.

Standard/Premium are not authorization roles and are absent from the active product.

External Meet-based tutoring is in scope. Built-in video conferencing, Meet API
integration, automatic meeting creation and a meeting-per-Session design are not.

The platform does not hold Course tuition before distributing it. Direct payments
to Teachers do not approve wallets, escrow, commission, platform payouts,
withdrawals or revenue sharing. Full Teacher-performed refunds with Admin
verification are approved; no accounting/expense subsystem is introduced.

Do not add native applications, microservices, chat/social features, AI tutoring,
certificates, leaderboards, complex gamification or unapproved infrastructure.

---

# 12. Unresolved Decisions

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

These clarifications do not reopen the approved 22-table scope or authorize execution.

---

# 13. Documentation Workflow and Scope Control

AGENTS.md is the current project instruction authority. This specification records
high-level scope, including the explicitly approved direct-to-Teacher payment
clarification. Conflicting legacy business content is not current approval.

Canonical responsibilities remain:

- REQUIREMENTS defines detailed functional/non-functional requirements.
- BUSINESS_RULES defines approved business constraints.
- USE_CASES defines actor interactions and workflows.
- DOMAIN_MODEL defines domain concepts and relationships.
- ARCHITECTURE, DATABASE_DESIGN and API_DESIGN define their respective later designs.

Follow the established development order: Requirements, Business Rules, Use Cases,
Domain Design, System Architecture, Database Design, UI/UX Design, API Design,
Implementation, Testing and Deployment. Existing documents do not imply that
their legacy business scope is approved for the tutoring platform.

Preserve completed TASK-001, TASK-002 and TASK-003 infrastructure and verification
evidence, including PostgreSQL/Supabase connectivity. This is a scope migration,
not a repository restart or invalidation of completed foundations.

Rewriting this specification does not authorize implementation, schema changes or
downstream edits. Keep detailed designs, formal requirement IDs and implementation
planning in their own documents, and obtain approval before expanding scope.
