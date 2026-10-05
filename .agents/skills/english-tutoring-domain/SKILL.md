---
name: english-tutoring-domain
description: Use when analyzing, designing, reviewing, implementing, or testing English tutoring workflows involving Teachers, Courses, Sessions, Enrollment, Assignments, Course payments, scheduling, or participant reviews.
---

# English Tutoring Platform Domain

## Authority and Scope

Follow AGENTS.md for the approved Development of an English Tutoring Platform
direction. Consult PROJECT_SPEC, REQUIREMENTS, BUSINESS_RULES and USE_CASES under
docs/ after checking their reconciliation status. Conflicting legacy business
rules are superseded; compatible technical and security guidance remains applicable.
Preserve completed TASK-001/TASK-002/TASK-003 infrastructure and evidence.

Use `requirements-analysis` for missing requirements, `system-design` for technical
boundaries and `authentication-security` for protected behavior. Do not implement
from a feature name or historical task alone.

## Roles and Relationships

- STUDENT, TEACHER and ADMIN are authorization roles.
- A Teacher owns many Courses; each Course belongs to one Teacher.
- A Course contains many Sessions and has Students through Enrollment.
- A Session belongs to one Course, contains learning content and may have Assignments.
- Each Assignment belongs to a Session; Student submissions/results relate to it.
- A Student purchases an entire Course, never an individual Session.
- Payment and Enrollment are distinct concepts. Participation/access state is not a role.

User, StudentProfile, TeacherProfile, Category/topic, Course, CourseSchedule,
Session, Enrollment, Payment, Assignment, AssignmentSubmission and Review are
responsibilities whose approved 22-table representation is in DATABASE_DESIGN.md.
Profiles do not imply separate login identities. Do not add TeachingService without an
approved need.

## Student

Students register/log in by email, manage their profile, discover/filter Teachers
and Courses, and inspect Teacher specialization/experience and Course information.
Course information includes name, description, Teacher, price, schedule and Session
count where applicable.

Students purchase Courses and participate through valid Enrollment where required:
view Sessions/content, access Assignments, submit work, view results/progress and
access the protected Course Meet URL. Public discovery does not grant participation.
Profile fields and security-sensitive changes require approved contracts.

## Teacher

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

Teachers manage profile information, including specialization, experience and
introduction, and create/manage/publish owned Courses directly. They configure
Course information, price, schedule, applicable capacity and teaching categories;
manage Sessions/content/Assignments; inspect enrolled Students, submissions/results
and progress; and view related payment status. Email changes remain security-sensitive.

Teacher interfaces are desktop-optimized and responsive. Student interfaces are
mobile-first; no native app is required.

## Ownership and Participation

Spring Boot enforces identity, role, account eligibility and operation permission.
Teacher authorization traverses Session -> Course and Assignment -> Session -> Course.
Apply the parent Course boundary to submissions, enrolled-Student information,
results, progress and related payment data. Teacher role grants no cross-owner access.

Student protected participation depends on backend-authoritative valid Enrollment
where required, including content, Assignments and the Course Meet URL.
Client-selected identity or payment flags cannot establish authorization.

## Payment and Enrollment

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

Provider protocols/identifier scope remain gates; conceptual fields are not verified VietQR
contracts.


## Scheduling, Meet and Calendar

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots.

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

One manual protected Course Meet URL serves all Sessions; no Meet API or Meeting entity.

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.


## Assignments, Progress and Reviews

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

Progress is derived; formulas, deadline/score limits, upload contracts and moderation remain open.


## Admin

Admin manages Student/Teacher accounts and relevant profiles, authorized locking/
unlocking, categories/topics, Course oversight, Enrollment/payment monitoring and
system counts. Admin is not the Course owner, payment recipient or default grader.
Exact moderation/override permissions remain open; do not infer unrestricted access.

## Retired Scope and Decision Discipline

Vocabulary, Dictionary, vocabulary Audio, Listening, Quiz, vocabulary Lessons,
old attempts/scoring/mastery, Standard/Premium tiers and subscriptions are retired.
Do not mechanically turn Lessons into Sessions or attempts into submissions.
CEFR-centric structure and legacy platform-revenue rules are not current requirements.

For domain work, identify the actor, current rule, ownership/access boundary,
data/history impact and unresolved decisions before design. Keep business rules,
domain concepts and persistence choices distinct. Preserve relevant history under
approved rules without importing retired retention policies. Do not invent rules
to complete a diagram or introduce speculative infrastructure or product scope.
