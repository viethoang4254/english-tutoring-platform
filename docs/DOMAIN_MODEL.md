# English Tutoring Platform --- Domain Model

**Version:** 0.2
**Status:** Reconciled conceptual tutoring model with explicit decision gates
**Project Type:** Graduation Project

---

# 1. Authority, Purpose and Modeling Conventions

This document defines the conceptual model for Development of an English Tutoring
Platform. Authority follows AGENTS.md -> PROJECT_SPEC.md -> REQUIREMENTS.md ->
BUSINESS_RULES.md -> USE_CASES.md -> DOMAIN_MODEL.md.
Downstream designs cannot establish missing business policy.

Project-local skills provide subordinate working guidance for applying the canonical
documentation. They do not override or redefine the canonical project documents.

Concepts describe responsibilities, relationships, ownership and invariants.
An identified concept need not be a separate persistence entity or class.
This document describes the approved domain; DATABASE_DESIGN.md defines its physical
representation. No API contract or implementation is authorized here.
Shared identity and supporting authentication are distinguished from core tutoring.

Only DM and INV definition headings below are active definitions. Section 18
reserves historical identifiers; its retired/deferred entries are not active rules.
References are targeted current IDs, not approval of the former wording of an ID.
Section 17 records unresolved decisions; a diagram or concept name cannot close them.

"One" identifies a justified parent/subject relationship. "May have many" does
not require existing children at creation. Physical details are specified in
DATABASE_DESIGN.md. Authorization traversal does
not prescribe persistence nesting, cascading operations or transaction boundaries.

Preserve completed TASK-001, TASK-002 and TASK-003 infrastructure and evidence.
This reconciliation authorizes no implementation; dependent documents follow the approved decisions.

---

# 2. Domain Overview and Conceptual Relationships

Teacher-owned Courses are purchased as a whole and accessed through Enrollment.
The approved V1 persistence direction is 22 tables; conceptual result/progress views
do not imply extra entities.

```text
User (one role) -> many immutable TeacherApplication snapshots; at most one PENDING
Teacher User -> zero or one current TeacherProfile after approval
Teacher -> many Courses; Course -> exactly one non-transferable Teacher
Category -> many Courses; Course -> exactly one Category
Course -> weekly ScheduleRules -> generated Sessions
Session -> schedule-change history; cancellation retains the same planned numbered row
Session -> zero or one Calendar mapping per provider
Session -> many Assignments -> many Submissions
Student + Assignment -> at most one current Submission; grading/result on Submission
Course + Student -> at most one Enrollment -> many historical Payments
Teacher -> many receiving accounts; at most one ACTIVE
Payment -> one historical receiving account; zero or one Refund
BankTransaction -> zero or one matched Payment; zero or one known receiving account
COMPLETED Enrollment -> zero or one ParticipantFeedback (rating 1..5)
Course -> one protected manually supplied Meet URL, shared by all Sessions
CourseProgress/statistics -> derived views; no independent persisted identity
```

Payment and Enrollment remain separate. Storage contains file bytes; domain records
retain authorized paths/metadata. No attendance, generic audit or multi-role model.

**References:** FR-TCR-003, FR-SES-001, FR-ASN-004, FR-ENR-006, FR-PAY-008,
FR-PAY-013, BR-COURSE-001, BR-ENR-001, BR-PAY-011.

---

# 3. Identity, Accounts, Roles and Supporting Authentication Concepts

## DM-USER-001 --- User

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

User includes email, protected password hash, full name and permitted optional
phone/address/date of birth. Exact profile API validation remains explicit. V1 uses
users.locked as its account-blocking mechanism: a locked account cannot authenticate or use
normal account functionality. There is no separate disabled, enabled or account_status
field/lifecycle. Email uses lowercase(trim(inputEmail)) for storage/login; a verified email change retains
the old email until successful verification of the new one. Password reset revokes all
refresh sessions; logged-in password change revokes other sessions while preserving the
current session. Raw refresh, verification and reset secrets are not persisted. Admin is not
publicly self-registered.

**References:** FR-STU-001, FR-TEA-005, FR-ACC-007, FR-ACC-016,
FR-AUTH-001, BR-ROLE-001, BR-AUTHN-002, BR-AUTHN-012, BR-AUTHN-020,
BR-AUTHN-023, BR-AUTHN-024, BR-PROFILE-002, UC-AUTH-REGISTER-01,
UC-AUTH-REGISTER-TEACHER-01, UC-AUTH-ME-01, UC-AUTH-PROFILE-01.

## DM-USER-002 — Account Eligibility

Account eligibility is represented by locked and email verification, plus approved
onboarding for Teacher business authority. There is no separate account_status, disabled or
enabled field/lifecycle. Locked means blocked from authentication/normal use, not deleted.
Keep this identifier for account eligibility; it does not require an AccountStatus entity or
enum.

**References:** FR-ACC-014, FR-ACC-015, FR-AUTH-007, FR-ADM-006,
BR-AUTHN-006, BR-AUTHN-017, BR-AUTHN-018, BR-AUTHN-019,
UC-AUTH-VERIFY-EMAIL-01, UC-AUTH-LOGIN-01, UC-ADM-USERS-01.

## Supporting Credentials

Passwords, Access Tokens and Refresh Tokens support authentication, not tutoring
ownership. Passwords require supported secure protection. Access Tokens use the
approved JWT architecture; sensitive credentials must not become public profile
information or unnecessary token claims.

A Refresh Token credential is distinct from the server-authoritative refresh
state represented below. Credential transport and detailed concurrency policy remain
security-design decisions; supporting credential persistence uses hashes, never raw secrets.

Retained configurable defaults are 15 minutes for Access Tokens, 7 days for
Refresh Tokens and 15 minutes for password-reset credentials. These do not
determine account activation or other unresolved policies.

**References:** FR-ACC-001, FR-ACC-002, FR-ACC-003, NFR-SEC-001,
NFR-SEC-010, NFR-SEC-013, BR-AUTHN-007, BR-AUTHN-008, BR-AUTHN-016,
UC-AUTH-LOGIN-01.

## DM-AUTH-001 --- RefreshSession

RefreshSession is User-bound, server-authoritative hashed refresh credential state with
expiry/revocation. Logout invalidates applicable state; rotation invalidates replacements as
approved. Password reset revokes all sessions, while logged-in password change revokes other
sessions and preserves the current one. No access-JWT table. Transport/replay/concurrency
controls remain open.

**References:** FR-ACC-004, FR-ACC-005, FR-ACC-006, NFR-DATA-006,
BR-AUTHN-009, BR-AUTHN-010, BR-AUTHN-011,
UC-AUTH-REFRESH-01, UC-AUTH-LOGOUT-01.

## DM-AUTH-002 --- EmailVerification

EmailVerification is User-bound hashed, expiring, single-use evidence for
INITIAL_VERIFICATION or EMAIL_CHANGE. Canonicalize the target address. The old email remains
effective until the new one verifies; update relevant future Calendar attendees after
verified change. Verification never removes a lock or approves onboarding. Resend/lifetime
controls remain open.

**References:** FR-ACC-008, FR-ACC-009, NFR-SEC-015, BR-AUTHN-006,
BR-AUTHN-017, UC-AUTH-VERIFY-EMAIL-01.

## DM-AUTH-003 --- PasswordReset

PasswordReset uses hashed, purpose-bound, expiring, single-use User credentials. Success
consumes the credential and revokes all refresh sessions. Recovery responses protect account
privacy. Password/delivery policy remains gated.

**References:** FR-ACC-010, FR-ACC-011, FR-ACC-012, FR-ACC-013,
NFR-SEC-014, BR-AUTHN-013, BR-AUTHN-014, BR-AUTHN-015, BR-AUTHN-016,
UC-AUTH-CHANGE-PASSWORD-01, UC-AUTH-FORGOT-PASSWORD-01, UC-AUTH-RESET-PASSWORD-01.

---

# 4. Teacher Profile and Ownership

## DM-TEA-001 --- TeacherProfile

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

TeacherProfile is current teaching information (specialization, experience, introduction and
optional avatar Storage path) sharing the User identity. Application history is a separate
review snapshot, not edited with the public profile. Reviewed applications retain
reviewer/time and rejection reason where applicable; PENDING is unreviewed. No second login
identity is created.

**References:** FR-TEA-002, FR-TEA-003, FR-DIS-004, FR-DIS-005,
FR-AUTH-012, BR-PROFILE-002, BR-AUTH-003, BR-AUTH-007,
UC-AUTH-PROFILE-01, UC-DIS-TEACHERS-01.

---

# 5. Course

## DM-COURSE-001 --- Course

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots.

Course includes name/description, tuition/currency, optional thumbnail Storage path and
discount window, planned start, timezone, capacity and Session count. Course content/Meet
URL are protected. Enrollment is unique per Student/Course and uses PENDING, ACTIVE,
COMPLETED or CANCELLED. A COMPLETED Enrollment is historical and cannot simply re-enroll
into that Course instance. Free Courses activate participation without fake Payments. Paid
participation may reserve a seat while PENDING. Capacity counts ACTIVE plus PENDING
Enrollments with unexpired reservations; min_students counts ACTIVE only. Expired
reservations consume no capacity. No new Enrollment or Payment may begin after the first
Session has started. Activation, reservation and late-payment handling must be
concurrency-safe. Remaining publication/completion operation validations are
implementation gates; approved physical restrictions are in DATABASE_DESIGN.

**References:** FR-TCR-001, FR-TCR-003, FR-TCR-004, FR-TCR-005,
FR-TCR-006, FR-TCR-007, FR-TCR-008, FR-TCR-009, FR-DIS-003,
FR-DIS-006, BR-COURSE-001, BR-COURSE-002, BR-COURSE-003,
BR-COURSE-004, BR-COURSE-007, BR-AUTH-007,
UC-TEA-COURSE-01, UC-TEA-PUBLISH-01, UC-STU-BROWSE-COURSES-01,
UC-STU-VIEW-COURSE-01.

---

# 6. Course Category / Topic

## DM-CAT-001 --- CourseCategoryTopic

CourseCategory is Admin-managed teaching classification with active/inactive lifecycle.
Every Course selects exactly one Category; a Category may serve many Courses. Used
Categories are retained and deactivated instead of deleted. No hierarchy or automatic Course
reassignment is introduced.

**References:** FR-ADM-007, FR-TCR-008, BR-ADM-001,
UC-ADM-CATEGORIES-01, UC-TEA-COURSE-01.

---

# 7. Session and Scheduling

## DM-SES-001 --- Session

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking. Every Session belongs to one
Course; Teacher ownership traverses that Course. Start/end are absolute timestamps. Content
is optional because generation can precede learning-content authoring.

**References:** FR-SES-001, FR-SES-002, FR-SES-003, FR-SES-004,
FR-AUTH-011, BR-SES-001, BR-SES-002, BR-AUTH-003, BR-ENR-006,
UC-SES-MANAGE-01, UC-SES-VIEW-01.

## DM-SCH-001 --- CourseSchedule

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking. Weekly rules use weekday/local
start/end plus Course timezone. Concrete Session times and reschedule history are distinct.
Calendar synchronization is downstream and cannot roll back the Session.

**References:** FR-TCR-009, FR-SCH-001, FR-SCH-002, BR-SCH-001,
UC-SCH-COURSE-01, UC-SCH-SESSION-01.

---

# 8. Assignment, Submission and Results

## DM-ASN-001 --- Assignment

Assignment belongs to one Session and uses ACTIVE/CANCELLED. Once any Submission exists it
cannot be hard-deleted. Assignment content, max_score and deadline direction are
represented: deadline is required and max_score is positive NUMERIC(5,2); nullable
Submission score uses NUMERIC(5,2), is nonnegative and cannot exceed max_score. Supabase
Storage holds Teacher avatars, Course thumbnails, Assignment files, Submission files and
refund proof. PostgreSQL stores paths/references and applicable metadata, never file bytes,
base64 or temporary signed URLs. Resolve each path within an explicitly configured bucket
for its usage; exact bucket identifiers remain configuration, and the path/bucket mapping
must be fixed before integration. Spring Boot authorizes access; Storage does not replace
backend business authorization.

**References:** FR-ASN-001, FR-ASN-002, FR-ASN-003, FR-SES-004,
BR-ASN-001, BR-ASN-002, BR-TEA-001,
UC-ASN-MANAGE-01, UC-ASN-VIEW-01.

## DM-ASN-002 --- Submission

Assignments belong to Sessions and use ACTIVE/CANCELLED. Any Submission prevents hard
deletion of its Assignment. There is one current Submission per Student/Assignment, using
DRAFT/SUBMITTED/GRADED; no revision-history, result or grading table is introduced. Score,
feedback and grading metadata remain on Submission. Only the owning Teacher grades, and a
score cannot exceed Assignment max_score. A numeric score is not made mandatory merely by
GRADED status. SUBMITTED requires submitted_at; GRADED additionally requires graded_at and
graded_by. Student authorship, Enrollment eligibility and Teacher grading ownership are
transactional authorization invariants. Attachments are Storage references; no
revision-history entity.

**References:** FR-ASN-004, FR-ASN-005, FR-AUTH-011, FR-AUTH-013,
NFR-DATA-001, BR-ASN-001, BR-ASN-002, BR-ASN-003, BR-TEA-003,
UC-ASN-SUBMIT-01, UC-ASN-INSPECT-01.

## DM-ASN-003 --- AssignmentResult

AssignmentResult is a permitted projection of Submission score, feedback and grading
metadata, not a separate entity/table. Only owning Teachers grade; Students view authorized
own results. A score is not required merely because status is GRADED. Result-availability
details remain open.

**References:** FR-ASN-005, FR-ASN-006, BR-ASN-003, BR-AUTH-006,
UC-ASN-INSPECT-01, UC-ASN-RESULT-01.

---

# 9. Enrollment and Course Participation

## DM-ENR-001 --- Enrollment

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe. Protected participation
remains backend-authoritative; listing enrollment or a payment claim is insufficient.
Payment attempts and Refunds remain separate records.

**References:** FR-ENR-004, FR-ENR-006, FR-ENR-007, FR-ENR-008,
BR-ENR-001, BR-ENR-006, BR-ENR-007, BR-ENR-008, BR-COURSE-007,
UC-STU-ENROLL-01, UC-STU-MY-COURSES-01.

---

# 10. Teacher Payment Receiving Information

## DM-PAY-002 --- PaymentReceivingInformation

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference. Bank receiving information is private; exactly one
historical account is referenced by each Payment. Actual transactions may independently
reference a known receiving account without matching Payment. Provider account reference
remains conceptual until verified.

**References:** FR-PAY-007, FR-PAY-008, FR-AUTH-012, BR-PAY-006,
BR-PAY-007, BR-AUTH-007, UC-PAY-RECEIVING-01, UC-PAY-COURSE-01.

---

# 11. Course Payment / Transaction Context

## DM-PAY-001 --- PaymentTransaction

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
Actual bank transactions and Payment attempts are separate records: unmatched transactions
may have no Payment or no resolvable receiver. CONFIRMED requires confirmation time. No
FAILED or REFUNDED Payment state. Physical V1 omits conceptual provider order/account
references. Verified adapter-context idempotency is required without claiming provider-wide
transaction-ID uniqueness.

**References:** FR-PAY-002, FR-PAY-008, FR-PAY-009, FR-PAY-010,
FR-PAY-011, FR-PAY-012, FR-ENR-008, NFR-DATA-007,
BR-PAY-002, BR-PAY-003, BR-PAY-007, BR-PAY-008, BR-PAY-009,
BR-PAY-010, BR-ENR-008, BR-ADM-005,
UC-PAY-COURSE-01, UC-PAY-VIEW-01, UC-ADM-TRANSACTIONS-01.

---

## DM-PAY-003 --- Refund

V1 supports full refunds only, with at most one Refund per Payment. The amount equals the
applicable full Payment amount under the approved workflow. Teacher performs the bank
transfer back to the Student and submits proof; Admin verifies completion. Refund statuses
are PENDING, SUBMITTED, COMPLETED and CANCELLED. Payment remains historical and has no
REFUNDED status. This does not authorize platform custody, payouts, commissions, escrow or
accounting. Proof is a Storage reference; completion metadata identifies the verifying
Admin. No automatic Enrollment transition is inferred.

**References:** FR-PAY-013, BR-PAY-011, UC-PAY-REFUND-01.

---

# 12. Meet and Calendar Boundaries

## Course Meet Information

The owning Teacher creates Google Meet externally and manually supplies exactly
one Course Meet URL when configured. Every Session uses that same Course URL.
This is protected Course participation information, not an independent meeting
entity or Session-specific meeting model.

Public discovery cannot disclose the URL. Student access checks the requested
Course and authoritative participation. URL access does not prove attendance.
There is no Meet API or automatic meeting creation. Supply timing, URL validation
and update effects remain unresolved.

**References:** FR-MEET-001, FR-MEET-002, FR-MEET-003,
BR-MEET-001, BR-MEET-002, BR-ENR-006,
UC-MEET-MANAGE-01, UC-MEET-ACCESS-01.

## External Calendar Boundary

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

**References:** INT-CAL-001, BR-AUTH-005, UC-CAL-SCHEDULE-01.

---

# 13. Learning Progress

## DM-PRO-002 --- CourseProgress

CourseProgress is an authorized derived view of Course participation and persisted learning
information. No course_progress or attendance table is approved. Student own-data and
Teacher owned-Course boundaries apply; formulas and completion indicators remain open.

**References:** FR-ENR-005, FR-PRO-004, FR-PRO-009, FR-TAN-003,
NFR-DATA-001, BR-AUTH-006, BR-TEA-002,
UC-LEARN-PROGRESS-01, UC-TEA-ANALYTICS-01.

---

# 14. Participant Feedback / Rating

## DM-RATE-001 --- ParticipantFeedback

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open. The Enrollment
identifies the Student, Course and Teacher context; no independent rating target or stored
aggregate is invented.

**References:** FR-RATE-001, FR-AUTH-013, BR-RATE-001, BR-AUTH-001,
UC-RATE-PARTICIPANT-01.

---

# 15. Admin Domain Boundaries

Admin may manage permitted account/profile information, authorized account
restrictions and categories/topics, oversee Courses, monitor Enrollments and
transactions, and view approved platform statistics. Each action requires its
particular authority; viewing does not grant mutation permission.

Admin does not automatically become Course owner, Teacher, Assignment grader,
tuition recipient/custodian, payment confirmer or unrestricted moderator.
Full-refund completion verification is explicitly authorized; unrelated overrides,
moderation and exact operation fields remain unresolved.

Statistics include appropriate user, Student, Teacher and Course totals, and
Enrollment/transaction information. Teacher tuition is not Admin/platform revenue.
Measures, reporting periods and outcome classification remain gated. No separate
analytics warehouse or reporting entity is required by these conceptual views.

Teacher monitoring may include authorized individual submissions/results;
aggregation is not a replacement for access checks or approved individual inspection.

**References:** FR-ADM-002, FR-ADM-003, FR-ADM-004, FR-ADM-006,
FR-ADM-007, FR-ADM-008, FR-ACR-001, FR-ACR-002, FR-ATR-001,
FR-ATR-002, FR-ATR-003, FR-AAN-001, FR-AAN-002, FR-AAN-003,
FR-AAN-004, FR-AAN-005, FR-AAN-008, FR-TCR-010, FR-TAN-002,
BR-ADM-001, BR-ADM-002, BR-ADM-005, BR-TEA-003,
UC-ADM-USERS-01, UC-ADM-COURSES-01, UC-ADM-CATEGORIES-01,
UC-ADM-ENROLLMENTS-01, UC-ADM-TRANSACTIONS-01, UC-ADM-ANALYTICS-01,
UC-TEA-STUDENTS-01, UC-TEA-ANALYTICS-01.

---

# 16. Cross-Cutting Domain Invariants

## INV-001 --- Role Authority and Conditional Access

STUDENT, TEACHER and ADMIN are authorization roles. Payment/participation is not
a role. Role membership alone does not establish resource ownership or access.

**References:** FR-AUTH-001, FR-AUTH-013, BR-ROLE-001, BR-AUTHN-020.

## INV-002 --- Course and Nested Teacher Ownership

Each Course has exactly one owning Teacher. Teacher management checks authoritative
identity, account eligibility, operation permission and ownership before disclosure
or mutation. Traverse Session -> Course -> Teacher, Assignment -> Session -> Course
-> Teacher and Submission -> Assignment -> Session -> Course -> Teacher.
The Course boundary also protects enrolled-Student information, results, progress
and relevant transaction information. Teacher A cannot manage Teacher B's resources.

**References:** FR-TCR-003, FR-AUTH-011, BR-COURSE-001, BR-AUTH-003.

## INV-010 --- Trustworthy Payment Confirmation

Actual provider/bank transactions may be unmatched. They retain receiving-account context
when resolvable, independently of Payment matching; an unresolved receiver remains a
reconciliation concern. Browser, Student and Teacher claims are not confirmation evidence.
Confirmation requires trustworthy provider/bank evidence matching the intended receiver,
code, amount and currency; wrong/missing codes or amounts do not auto-confirm, partial
transfers are not summed automatically, and late transactions cannot cause overbooking.

**References:** FR-PAY-010, FR-PAY-012, FR-ENR-008,
BR-PAY-002, BR-PAY-009, BR-ENR-008, BR-ADM-005.

## INV-013 --- Participation Relationship and Access

Enrollment concerns one Student and one Course. Protected participation requires
authoritative validity where applicable; discovery or My Courses listing alone
does not establish it. Enrollment grants no other Student's private information.

**References:** FR-ENR-006, FR-ENR-007, BR-ENR-001, BR-ENR-006, BR-ENR-007.

## INV-014 --- Whole-Course Purchase and Payment Separation

Purchase concerns the whole Course, never individual Sessions. Payment records
and Enrollment participation are distinct; no unconditional activation is implied.

**References:** FR-ENR-006, FR-ENR-008, BR-COURSE-007, BR-ENR-008.

## INV-015 --- Teacher-Owned Receiving Destination

Receiving information belongs to its Teacher. Course payment resolves Course ->
owning Teacher -> configured destination; client input cannot substitute an
unauthorized destination. Receiving information is not public discovery data.

**References:** FR-PAY-007, FR-PAY-008, BR-PAY-006, BR-PAY-007, BR-AUTH-007.

## INV-016 --- Direct Tuition without Platform Custody

Tuition is directed to the owning Teacher's destination. Platform/Admin does not
hold it for later distribution; monitoring grants no financial custody.

**References:** FR-PAY-009, BR-PAY-008, BR-ADM-005.

## INV-017 --- Session and Assignment Parentage

Each Session belongs to one Course; each Assignment belongs to one Session.
A Session may have no Assignments. Parentage controls authorization.

**References:** FR-SES-001, FR-SES-004, FR-ASN-001, BR-SES-001, BR-ASN-001.

## INV-018 --- Submission Authorship and Privacy

Submission belongs to its Assignment and submitting Student. Backend identity
establishes authorship. Teacher inspection derives through Course ownership;
it does not transfer authorship or grant unrestricted grading.
Students cannot access another Student's private submission/result.

**References:** FR-ASN-004, FR-ASN-005, FR-ASN-006, BR-ASN-002, BR-ASN-003.

## INV-019 --- Shared Protected Course Meet URL

Exactly one manually supplied, externally created Course URL is used by all its
Sessions when configured. Disclosure requires authorization; no public discovery
leak, automatic meeting creation, Meet API or per-Session meeting is permitted.

**References:** FR-MEET-001, FR-MEET-002, FR-MEET-003, BR-MEET-001, BR-MEET-002.

## INV-020 --- Recurrence and Occurrence Separation

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

**References:** FR-SCH-001, FR-SCH-002, BR-SCH-001.

## INV-021 --- Calendar Information Boundary

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

**References:** INT-CAL-001, BR-AUTH-005, UC-CAL-SCHEDULE-01.

## INV-022 --- Limited Administrative Authority

Administrative monitoring does not confer Course ownership, grading, confirmation,
arbitrary mutation or tuition custody. Exact overrides remain unresolved.

**References:** FR-ACR-002, FR-ADM-008, FR-ATR-001,
BR-ADM-001, BR-ADM-002, BR-ADM-005.

## INV-023 --- Backend Authority and Protected Information

The backend establishes identity, role, account eligibility, ownership, required
participation and permitted effects before protected disclosure/mutation.
Public discovery excludes private submissions/results, receiving information,
transactions, Student information and protected participation content.
Client state cannot overwrite authoritative results or payment/participation rights.

**References:** FR-AUTH-006, FR-AUTH-012, FR-AUTH-013, NFR-SEC-016,
BR-AUTH-001, BR-AUTH-007, BR-AUTH-006.

## INV-024 --- Required Information and Retention Boundary

Retain Teacher applications, bank-account history, Payment snapshots, transactions, Refund
evidence and Session schedule-change history. Assignment hard deletion is forbidden
after any Submission. Used Categories are deactivated. Exact legal retention periods remain
open; no global soft-delete/audit system.

**References:** NFR-DATA-001, NFR-DATA-006, NFR-DATA-007,
BR-AUTHN-019, BR-PAY-003.

Unresolved policies must not be encoded as fixed state, multiplicity, formula or
acceptance rules. This modeling discipline follows the source-document decision
gates and is not a new business workflow.

---

# 17. Deferred Modeling Decisions

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

---

# 18. Retired Legacy Concepts and Identifier Disposition

This register records the earlier scope migration, not today's open-decision list.
Independent V1 approvals in the active sections supersede its then-deferred choices.

## Original Domain Identifier Register

All 24 original DM identifiers are accounted for: 9 preserved/reconciled and
15 retired. Retired IDs are reserved and are not active dependencies.

| Original ID | Concept | Disposition |
|---|---|---|
| DM-USER-001 | User | Reconciled: shared identity retained; old onboarding/profile contract withdrawn. |
| DM-USER-002 | AccountStatus | Reconciled: restrictions retained; universal transitions withdrawn. |
| DM-AUTH-001 | RefreshSession | Preserved/reconciled as supporting authentication state. |
| DM-AUTH-002 | EmailVerification | Preserved/reconciled supporting security; no unconditional activation. |
| DM-AUTH-003 | PasswordReset | Preserved/reconciled supporting recovery authority. |
| DM-CEFR-001 | CefrLevel | Retired: no mandatory CEFR Course organization. |
| DM-COURSE-001 | Course | Reconciled: tutoring/commercial ownership; old classification/lifecycle withdrawn. |
| DM-LESSON-001 | Lesson | Retired; Session has an independent definition. |
| DM-VOC-001 | Vocabulary | Retired. |
| DM-VOC-002 | VocabularySense | Retired. |
| DM-VOC-003 | LessonVocabulary | Retired. |
| DM-VOC-004 | PronunciationAudio | Retired. |
| DM-EX-001 | Exercise | Retired; Assignment has an independent definition. |
| DM-EX-002 | ExerciseQuestion | Retired. |
| DM-ENR-001 | Enrollment | Reconciled: Student-Course participation; old access/uniqueness assumptions withdrawn. |
| DM-ATT-001 | ExerciseAttempt | Retired; Submission has an independent definition. |
| DM-ATT-002 | AnswerRecord | Retired. |
| DM-PRO-001 | LessonProgress | Retired. |
| DM-PRO-002 | CourseProgress | Reconciled direction; old inputs/formulas withdrawn. |
| DM-PRO-003 | VocabularyPerformance | Retired. |
| DM-SAVE-001 | SavedVocabulary | Retired. |
| DM-SUB-001 | SubscriptionPlan | Retired. |
| DM-SUB-002 | Subscription | Retired; not converted into Enrollment. |
| DM-PAY-001 | PaymentTransaction | Reconciled as Course transaction information; Premium/plan semantics withdrawn. |

Vocabulary learning, Dictionary integration, vocabulary audio, vocabulary Lessons,
Fill Word, Listening, Quiz, the old Exercise/Attempt/Answer engine, automatic
Score/accuracy/mastery, weak vocabulary, saved vocabulary and vocabulary Review
are retired. STANDARD/PREMIUM access tiers/classification, Premium gating,
Subscription access/renewal and subscription revenue are not active scope.

Lesson is not Session; Exercise/Quiz is not Assignment; ExerciseAttempt is not
Submission; Subscription is not Enrollment; vocabulary Review is not participant
feedback. No old identifier is mechanically renamed into a new tutoring meaning.
The former ReviewSession candidate and its practice algorithm are also retired.

## Original Invariant Register

All 12 original invariant IDs are accounted for. Only INV-001, INV-002 and
INV-010 remain active with compatible reconciled responsibilities.

| Original ID | Disposition |
|---|---|
| INV-001 | Reconciled role authority; Premium framing retired. |
| INV-002 | Preserved Course ownership and extended tutoring-child authorization. |
| INV-003 | Retired Lesson ownership; new Session parentage uses INV-017. |
| INV-004 | Retired Vocabulary reuse. |
| INV-005 | Retired sense integrity. |
| INV-006 | Deferred/inactive duplicate-active-Enrollment assertion; no uniqueness rule retained. |
| INV-007 | Retired SavedVocabulary uniqueness. |
| INV-008 | Retired exercise correctness; current backend authority is independently expressed in INV-023. |
| INV-009 | Retired Premium entitlement. |
| INV-010 | Reconciled trustworthy payment-confirmation responsibility; Premium effects retired. |
| INV-011 | Superseded/inactive blanket historical/archive guarantee; current boundary uses INV-024. |
| INV-012 | Retired mastery evidence thresholds. |

The old fixed account and DRAFT/PUBLISHED/ARCHIVED Course transition assumptions
are withdrawn as operative policy. Their presence in history does not resolve
current lifecycle gates. The old exclusion of live tutoring does not exclude
approved external Meet-based tutoring; built-in video remains unapproved.

---

# 19. Traceability and Subsequent Review Boundaries

Each active concept and invariant carries targeted current references. Only active
definition headings in REQUIREMENTS, BUSINESS_RULES and USE_CASES establish source
IDs; historical registers cannot satisfy active traceability.

The new identified concepts are TeacherProfile, CourseCategoryTopic, Session,
CourseSchedule, Assignment, Submission, AssignmentResult,
PaymentReceivingInformation, Refund and ParticipantFeedback. Naming these responsibilities
does not require separate persistence structures. Meet remains Course information;
Calendar remains external; statistics remain authorized views.

Useful consistency/ownership boundaries are shared identity with supporting auth,
Course-owned teaching content, Student-authored work with Course-derived inspection
authority, Enrollment participation, Teacher receiving information and distinct
Course transactions. These are candidates, not finalized persistence aggregates,
transaction scopes or cascading-deletion rules.

For example, FR-ASN-004 -> BR-ASN-002 -> UC-ASN-SUBMIT-01 ->
DM-ASN-002 expresses submitting identity and participation authorization while
leaving format and resubmission policy open. References preserve those gates.

ARCHITECTURE.md, DATABASE_DESIGN.md, API_DESIGN.md, TASK_BREAKDOWN.md and
FEATURE_STATUS.md consume this reconciled direction. Do not distort this
model to preserve obsolete downstream counts, contracts or constraints.
Preserve TASK-001/TASK-002/TASK-003 implementation and verification evidence.
Phase 2 reconciles affected canonical guidance; technical foundations and actual
implementation remain unchanged.

Follow the approved development order and remaining physical, UI/UX and API gates
before dependent implementation. This reconciliation does not authorize Flyway or
other implementation/testing/deployment work.
