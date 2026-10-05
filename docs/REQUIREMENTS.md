# English Tutoring Platform — Requirements Specification

**Version:** 0.3
**Status:** Reconciled tutoring requirements with explicit open decision gates
**Project Type:** Graduation Project

# 1. Scope, Authority, Terminology and Requirement Conventions

This document defines observable functional and non-functional requirements for
Development of an English Tutoring Platform (Xây Dựng Nền Tảng Gia Sư Dạy Tiếng Anh).

AGENTS.md and PROJECT_SPEC.md establish the approved scope. Project-local skills
provide subordinate working guidance; they never override canonical requirements,
rules or design decisions. Conflicting legacy documents do not override current authority.

The authorization roles are STUDENT, TEACHER and ADMIN. Enrollment and payment
state are not roles. Course is the whole teaching/commercial unit purchased by
Students; Session is an individual Course learning occurrence; Assignment belongs
to Session; participant review/rating is feedback about tutoring participation.
These concepts do not prescribe persistence structures.

Sections 2–14 contain active requirements. SHALL states a mandatory invariant or
approved capability; SHOULD retains an existing qualified quality objective.
A capability with a named gate is approved in scope, but dependent policy,
acceptance details and implementation remain blocked until that gate is resolved.
A gate never weakens the stated ownership, privacy or payment-trust invariant.
Section 15 records open decisions; section 16 records historical IDs, not active work.

Preserve compatible IDs; allocate new IDs for new meanings. This document defines
no database schema, API contract, UI layout or integration protocol. No implementation
or downstream document changes are authorized by this reconciliation.

---

# 2. Authentication and Account Security

## FR-ACC-001 — JWT Authentication

Email canonicalization is lowercase(trim(inputEmail)) for registration, login,
verification, email change and password-reset/account lookup. Persist users.email and
verification target_email canonically; do not remove dots/+tags or apply provider-specific
normalization. users.email is UNIQUE; target_email is not unique.

The system shall support JWT-based authentication for authenticated web access.

## FR-ACC-002 — Access Token

The system shall issue an authentication Access Token after successful authentication according to the approved security design.

The Access Token shall have a limited lifetime.

## FR-ACC-003 — Refresh Token

The system shall support Refresh Tokens for obtaining new Access Tokens without requiring the user to repeatedly provide credentials while the Refresh Token remains valid.

## FR-ACC-004 — Refresh Access Token

The system shall allow a valid Refresh Token to be used to request a new Access Token.

The backend shall validate the Refresh Token before issuing a new Access Token.

## FR-ACC-005 — Reject Invalid Refresh Token

Expired, invalid, revoked, or otherwise unusable Refresh Tokens shall not result in a valid new Access Token.

## FR-ACC-006 — Refresh Token Revocation

The system shall support backend-controlled refresh rotation and logout invalidation.
Password reset revokes all refresh sessions; logged-in password change revokes other
sessions while preserving the authenticated current session. Session selection, replay and
concurrent issuance must not bypass these effects.


## FR-ACC-007 — Current User

The system shall allow an authenticated user to retrieve permitted information about the currently authenticated account.

## FR-ACC-008 — Email Verification

The system shall verify the intended account email using an expiring, purpose-specific
hashed credential. Email changes verify target_email first; the old email remains effective
until successful verification and atomic change. Verification cannot override locking or
Teacher onboarding. Relevant future Calendar attendees must then be updated.


## FR-ACC-009 — Resend Email Verification

The system should allow an eligible user to request a new email-verification message
when verification has not been completed. Resend eligibility, limits and verification
expiry remain under the Security gate.

## FR-ACC-010 — Change Password

An eligible authenticated user shall be able to change their password through the dedicated security
operation. Other refresh sessions are revoked while the current session is preserved;
plaintext secrets are not logged or returned.


## FR-ACC-011 — Forgot Password

The system shall allow a user who cannot access their account password to initiate an approved password-recovery process.

## FR-ACC-012 — Reset Password

The system shall allow an eligible User to reset the password through valid recovery authority.

A valid, unexpired, single-use account-bound reset credential permits password reset.
Successful reset consumes the credential and revokes all refresh sessions atomically with
the password change.


## FR-ACC-013 — Password Reset Expiration

Expired, invalid, previously used, or otherwise unusable password-reset credentials shall not permit a password reset.

## FR-ACC-014 — Account Status

The system shall enforce the authoritative locked-account restriction.

V1 uses users.locked as the account-blocking mechanism. A locked account cannot authenticate
or use normal account functionality; email verification does not bypass the lock.


## FR-ACC-015 — Locked Account Restriction

The system shall block authentication and normal account use when users.locked is true. This
retained identifier no longer defines a separate disabled-account lifecycle; V1 has no
disabled, enabled or account_status field.

---

## FR-ACC-016 — Role Preservation

Authentication and token-refresh operations shall not allow a client to assign or elevate its own role.

Role information shall originate from trusted backend state.

---

# 3. Student and Teacher Accounts / Profiles

## FR-STU-001 — Student Registration

The system shall allow a user to register a Student account using email.
Student registration shall not permit arbitrary client-selected privileged roles.
Use canonical email and required full_name with protected password handling.
Detailed profile validation and security transport remain under the Accounts/Security gates.

## FR-STU-002 — Student Login

The system shall allow a registered Student to authenticate.

## FR-STU-003 — Student Logout

The system shall allow an authenticated Student to log out.

Logout shall invalidate or revoke applicable authentication session/Refresh Token state according to the approved authentication design.

## FR-STU-004 — Student Profile

The system shall allow an authenticated Student to view their own permitted profile
information. The exact profile-field contract remains under the Accounts gate.

## FR-STU-005 — Update Student Profile

The system shall allow a Student to update their own permitted profile information,
resolving identity from authenticated backend state. Exact editable fields and
validation remain under the Accounts gate. Dedicated verified email change keeps
the old address until the new one verifies; ordinary editing does not authorize
changes to role, account restrictions or payment/participation privileges.

## FR-TEA-001 — Teacher Authentication

Authorized Teachers shall be able to authenticate to the platform.

Teacher authentication shall use the approved platform authentication mechanism.

## FR-TEA-002 — Teacher Profile

The system shall maintain the approved Teacher public profile separately from retained
application review snapshots. Teaching information includes specialization, teaching
experience and introduction; avatar bytes are in Supabase Storage and only the path is
persisted. Private banking/account security data is excluded from public profiles.


## FR-TEA-003 — Update Teacher Profile

The system shall allow a Teacher to manage their own permitted profile information,
including specialization, teaching experience and introduction. Identity shall be
resolved by the backend. Exact editable DTO validation remains under the Accounts
gate; email changes use verified target-email replacement. Profile editing shall
not assign roles or bypass account restrictions.

## FR-TEA-005 — Teacher Registration

The system shall support separate Teacher registration under the following onboarding rules.

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

---

# 4. Teacher and Course Discovery

## FR-DIS-003 — View Course Information

The system shall allow Students to view permitted Course information before
Enrollment, including name, description, owning Teacher, tuition/price and learning
schedule information, with Session count where applicable. Public discovery shall
respect FR-AUTH-012 and shall not grant protected participation access.

## FR-DIS-004 — View Teacher Information

The system shall allow Students to view permitted Teacher profile information,
including specialization, teaching experience and introduction. Protected account,
payment and Student information shall not become public through this capability.

## FR-DIS-005 — Discover Teachers

The system shall allow Students to discover, search and filter Teachers.
Exact filter fields and search behavior remain under the Discovery gate.

## FR-DIS-006 — Discover Courses

The system shall allow Students to discover, search and filter Courses offered by
different Teachers. Exact filter fields and search behavior remain under the
Discovery gate; no proficiency taxonomy is mandated.

---

# 5. Teacher-Owned Course Management

## FR-TCR-001 — Create Course

The system shall allow an authorized Teacher to create Courses. Each created Course
shall have exactly one owning Teacher under FR-TCR-003. Exact initial state and
lifecycle transitions remain under the Course lifecycle gate.

## FR-TCR-003 — Course Ownership

Every Course shall have exactly one owning Teacher established by the backend. Ownership
cannot be transferred in V1; Teacher role alone does not establish eligible business
authority.


## FR-TCR-004 — View Owned Courses

Teachers shall be able to view Courses they own.

## FR-TCR-005 — Update Owned Course

Teachers shall be able to update permitted information and manage content for
Courses they own, subject to backend ownership checks. Publishing is supported by
FR-TCR-007; exact lifecycle permissions remain under the Course lifecycle gate.

## FR-TCR-006 — Prevent Unauthorized Course Modification

The backend shall prevent a Teacher from modifying another Teacher's Course.
Any future exception requires an explicit approved rule; none is inferred from
role membership or unresolved administrative override policy.

## FR-TCR-007 — Publish Owned Course

An authorized Teacher shall be able to publish an owned Course after backend validation.

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots. Concrete
Sessions must exist before publication; detailed publication/completion validations remain
workflow clarifications; the valid Meet URL publication prerequisite is approved.


## FR-TCR-008 — Course Information and Tuition

Course information shall include its name, description, exactly one Category,
tuition/currency, optional percentage-discount window and applicable schedule/capacity
information. Historical Payments preserve their own price snapshots. Course thumbnail bytes
use Supabase Storage; PostgreSQL stores only the path.


## FR-TCR-009 — Course Schedule and Capacity Configuration

A Teacher shall be able to configure owned-Course learning schedules and permitted minimum/maximum Student capacity, subject to these participation rules.

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.


## FR-TCR-010 — Manage Enrolled-Student Teaching Information

Teachers shall be able to manage permitted enrolled-Student teaching information
for owned Courses. This shall not grant general account administration or
cross-Course access; exact permitted information/actions require later rules.

## FR-TAN-002 — Course Enrollment Statistics

Teachers should be able to view enrollment statistics for owned Courses.

---

# 6. Sessions and Scheduling

## FR-SES-001 — Session Belongs to Course

Each Session shall belong to one Course and represent an individual learning
occurrence. A Course shall support multiple Sessions. Session content and behavior
shall follow tutoring requirements, not inherited legacy learning rules.

## FR-SES-002 — Manage Owned-Course Sessions

Teachers shall be able to create/manage Sessions and learning content only for Courses they own.

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.


## FR-SES-003 — View Authorized Sessions

Students with the required valid Enrollment shall be able to view appropriate
Session information and learning content for the corresponding Course. Protected
content shall not be exposed through public discovery.

## FR-SES-004 — Session Assignments

A Session shall support associated Assignments under FR-ASN-001.
The system shall not require every Session to contain an Assignment.

## FR-SCH-001 — Recurring Course Schedule

The system shall generate exactly the fixed planned Session count under BR-SCH-001,
with ISO weekday rules, required IANA timezone and unique Course session_number.
Cancellation shall retain the Session row/number/count and synchronize its Calendar
event; no replacement or automatic make-up Session is supported.

The system shall represent recurring weekly Course schedule rules using Course-local
dates/times and timezone information, and generate concrete Sessions before publication.
Generation follows BR-SCH-001: ISO weekdays, required IANA timezone, chronological
occurrences from planned_start_date, exactly session_count and unique Course numbering.
Published concrete instants are authoritative; no blind regeneration.


## FR-SCH-002 — Reschedule Individual Session

An authorized owning Teacher shall be able to reschedule the same Session; old/new instants are recorded
in session_schedule_changes. Cancellation retains the same Session and number, changes
status to CANCELLED and synchronizes its Calendar event without changing session_count.
No replacement or automatic make-up Session is created.

---

# 7. Assignments and Submissions

## FR-ASN-001 — Assignment Belongs to Session

Each Assignment shall belong to one Session and therefore to that Session's
Course for ownership and access decisions. This does not prescribe storage design.

## FR-ASN-002 — Manage Assignments

Owning Teachers shall manage Session Assignments using ACTIVE/CANCELLED. An Assignment with
any Submission must not be hard-deleted. Supabase Storage holds Assignment file bytes;
PostgreSQL retains references and metadata.


## FR-ASN-003 — View Assignments

Students shall be able to view appropriate Assignments for Courses in which they
have the required valid Enrollment. Viewing shall respect FR-ENR-007.

## FR-ASN-004 — Submit Assignment

An eligible Student shall have one current Submission per Assignment, with
DRAFT/SUBMITTED/GRADED states. No submission revision-history table is introduced.
Submission files use Supabase Storage; the backend validates authorship and Course
participation.


## FR-ASN-005 — Teacher Submission and Result Review

Teachers shall be able to inspect authorized submissions/results within their owned Courses.

Only the owning Teacher may inspect authorized Course submissions and grade them. Grading
information remains on submissions; any numeric score must not exceed Assignment max_score.
GRADED requires submission time and grader/time metadata, but does not by itself require a
numeric score.


## FR-ASN-006 — Student Assignment Results

A Student shall be able to view their own authorized Submission result. Score, Teacher feedback and
grading metadata are stored on Submission; there is no separate result/grading table and
missing score is not zero.

---

# 8. Enrollment and Participation Access

## FR-ENR-004 — My Courses

The system shall allow Students to view Courses in which they are enrolled.
Listing an Enrollment shall not itself establish current participation eligibility;
protected access shall satisfy FR-ENR-007.

## FR-ENR-005 — Course Progress

The system shall support progress tracking for enrolled Courses under FR-PRO-004.
The Progress gate governs calculation; this requirement defines no separate formula.

## FR-ENR-006 — Whole-Course Participation Relationship

The system shall support whole-Course participation through Enrollment, distinct from Payment.

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.


## FR-ENR-007 — Authoritative Enrollment Access

The backend shall require valid authoritative Enrollment for protected Course
participation where applicable, including protected Session content, Assignments
and Meet access. It shall reject access when the required Enrollment condition
is not met; client claims shall not establish eligibility. Exact lifecycle and
validity rules remain under the Enrollment gate.

## FR-ENR-008 — Payment-Dependent Enrollment Effects

The system shall validate trustworthy payment confirmation and applicable Enrollment rules before payment-dependent activation.

Payment and Enrollment remain distinct. A free Course requires no fake Payment; a paid
Enrollment may be PENDING during payment. One Enrollment may have multiple historical
Payment attempts but only one PENDING attempt. Trustworthy confirmation and concurrency-safe
capacity checks govern approved activation effects; late transactions cannot overbook.

---

# 9. Teacher Payment Destinations and Course Payments

## FR-PAY-002 — Record Transaction

The system shall retain price-snapshotted Payment attempts and actual provider/bank
payment_transactions separately. Transactions may be unmatched and may retain nullable
receiving-account context independently of nullable Payment matching. Unresolved receivers
remain a reconciliation concern.


## FR-PAY-007 — Teacher Receiving Information

A Teacher shall be able to maintain their own permitted receiving-bank information; cross-Teacher configuration changes are forbidden.

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference.


## FR-PAY-008 — Direct-to-Owner Course Payment

Course tuition shall go directly to the owning Teacher's historical receiving-account
context under the VietQR integration direction. Spring Boot establishes the Course,
amount/currency, discount and receiver; client fields are not authority.


## FR-PAY-009 — No Platform Tuition Custody

The platform/Admin shall not hold tuition or perform payouts, wallets, escrow, commission,
withdrawal, revenue sharing or accounting. The approved full-refund workflow is a Teacher
transfer to the Student with Admin completion verification.


## FR-PAY-010 — Trustworthy Payment Confirmation

The system shall require trustworthy provider/bank evidence before treating payment as confirmed.

Actual provider/bank transactions may be unmatched. They retain receiving-account context
when resolvable, independently of Payment matching; an unresolved receiver remains a
reconciliation concern. Browser, Student and Teacher claims are not confirmation evidence.
Confirmation requires trustworthy provider/bank evidence matching the intended receiver,
code, amount and currency; wrong/missing codes or amounts do not auto-confirm, partial
transfers are not summed automatically, and late transactions cannot cause overbooking.


## FR-PAY-011 — Authorized Payment Visibility

The system shall support permitted payment transaction/status visibility for the
related Student, owning Teacher and authorized Admin where required. It shall
prevent cross-Teacher unauthorized management/access and exposure through public
discovery. Exact permitted information remains under the Payment and Administration
gates; visibility shall not imply confirmation or modification authority.

## FR-PAY-012 — Confirmation Gate Before Access

The system shall deny payment-dependent activation from initiation or unverified claims.

Payment initiation or unverified claims cannot activate Enrollment. Enforce FR-PAY-010 plus
capacity/cutoff rules transactionally before the corresponding effect. Approved Payment
states and one-PENDING attempt are settled; detailed cancellation/late/retry reconciliation
remains open.

---

## FR-PAY-013 — Full Refund Workflow

V1 supports full refunds only, with at most one Refund per Payment. The amount equals the
applicable full Payment amount under the approved workflow. Teacher performs the bank
transfer back to the Student and submits proof; Admin verifies completion. Refund statuses
are PENDING, SUBMITTED, COMPLETED and CANCELLED. Payment remains historical and has no
REFUNDED status. This does not authorize platform custody, payouts, commissions, escrow or
accounting.

---

# 10. Google Meet and Google Calendar Boundaries

## FR-MEET-001 — Single Manually Supplied Course URL

The owning Teacher shall manually supply exactly one Course Google Meet URL for
a meeting created externally. All Sessions of that Course shall use that same URL.
The application shall not create a separate meeting per Session. Detailed URL
validation and when the URL must be supplied remain under the Course lifecycle gate.

## FR-MEET-002 — Protected Meet Access

The backend shall treat the Course Meet URL as protected participation information.
It shall deny unauthorized access, including non-enrolled access where valid
Enrollment is required, and shall not disclose the URL through public discovery.

## FR-MEET-003 — External Meet Boundary

The application shall not integrate the Google Meet API or automatically create
Meet meetings. No dedicated GoogleMeet domain object or meeting infrastructure is
required by this capability.

## INT-CAL-001 — Calendar Schedule and Reminder Capability

The system shall support Google Calendar integration under the following authority and synchronization boundaries.

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

# 11. Results, Progress and Participant Reviews

## FR-PRO-004 — Course Progress

The system shall track Student Course progress from authoritative learning
information under later-approved rules. Progress calculation, completion measures
and any grading aggregation remain under the Progress gate; no formula is inferred.

## FR-PRO-009 — View Own Progress

Students shall be able to view their own appropriate learning progress.
The system shall enforce personal-data access boundaries. Exact indicators and
calculation remain under the Progress gate; Assignment results follow FR-ASN-006.

## FR-TAN-003 — Course Progress Statistics

Teachers shall be able to monitor appropriate learning-progress information for
Students enrolled in their owned Courses. Exact indicators and calculations remain
under the Progress gate, without granting access to another Teacher's protected data.

## FR-RATE-001 — Participant Review and Rating

The system shall support Student feedback under the following completed-Enrollment rules.

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.

---

# 12. Admin Management and Monitoring

## FR-ADM-001 — Admin Authentication

Authorized Admins shall be able to authenticate.

Admin authentication shall use the approved platform authentication mechanism.

## FR-ADM-002 — View Users

Admins shall be able to view permitted account/profile information for Students
and Teachers, distinguishing users by authorization role. Teacher inspection shall
expose only authorized information and associated Courses. Exact fields and
overrides remain under the Accounts and Administration gates.

## FR-ADM-003 — Manage Student Accounts

Admins shall be able to perform approved management actions on Student accounts
and permitted profile information. Exact actions and fields remain under the
Accounts and Administration gates; the role alone grants no arbitrary mutation.

## FR-ADM-004 — Manage Teacher Accounts

Admins shall be able to perform approved management actions on Teacher accounts
and permitted profiles. This does not decide Teacher registration approval,
activation or role assignment; those remain under the Accounts gate.

## FR-ADM-006 — Account Status Administration

Authorized Admins shall be able to lock/unlock users under approved permissions. locked is V1's
only account-blocking mechanism and is not a deletion state.


## FR-ADM-007 — Manage Course Categories and Topics

Admin shall manage Course Categories using active/inactive. Each Course has exactly one
Category; a used Category is retained and deactivated rather than deleted.


## FR-ADM-008 — Monitor Enrollments

Admins shall be able to inspect permitted Course Enrollment information for
platform administration. Monitoring shall not grant arbitrary Enrollment alteration
or resolve lifecycle/override policy.

## FR-ACR-001 — View Courses

Admins shall be able to view Courses across the platform.

## FR-ACR-002 — Course Oversight

Admins shall have authorized platform-level Course oversight. Admin shall not
thereby become a Course owner or default Assignment grader. Exact moderation and
override permissions remain under the Administration gate.

## FR-ATR-001 — View Transactions

Admins shall be able to view permitted Course payment transaction information
under FR-PAY-011. Monitoring shall not grant tuition ownership/custody or
unrestricted transaction modification authority.

## FR-ATR-002 — Transaction Status

Admins shall be able to inspect authorized payment status information.
Unverified claims shall not become verified success through display or monitoring.
Exact states and outcome mappings remain under the Payment gate.

## FR-ATR-003 — Transaction Time

Admins shall be able to view permitted timing information for recorded transactions.
The precise event meanings and reporting boundaries remain under the Payment and
Reporting gates.

## FR-AAN-001 — Total Users

Admins shall be able to view the total number of users.

## FR-AAN-002 — Student Statistics

Admins shall be able to view Student statistics, including the total count.

## FR-AAN-003 — Teacher Statistics

Admins shall be able to view Teacher statistics, including the total count.

## FR-AAN-004 — Course Statistics

Admins shall be able to view Course statistics, including the total count.

## FR-AAN-005 — Enrollment Statistics

Admins should be able to view platform enrollment statistics.

## FR-AAN-008 — Transaction Statistics

Admins shall be able to view appropriate authorized transaction statistics.
Exact aggregates, formulas, periods and outcome classifications remain under the
Reporting and Payment gates. Statistics shall not be represented as Admin ownership
of Teacher Course tuition.

---

# 13. Shared Authorization and Security Requirements

## FR-AUTH-001 — Role-Based Access

The system shall enforce access based on authenticated user role.

Supported roles are:

- STUDENT
- TEACHER
- ADMIN

## FR-AUTH-002 — Student Authorization

Students shall not access Teacher or Admin management functionality.

## FR-AUTH-003 — Teacher Authorization

Teachers shall not access Admin-only functionality.

Teacher business authority requires role TEACHER, email_verified true, locked false and
approved onboarding. Each protected management/grade operation also requires ownership of
the parent Course.


## FR-AUTH-004 — Course Ownership Authorization

Teacher Course modification shall respect Course ownership.

## FR-AUTH-006 — Server-Side Enforcement

Authorization shall be enforced by the backend and shall not rely solely on frontend visibility.

## FR-AUTH-007 — Account Status Authorization

Spring Boot shall enforce users.locked before authentication and normal account use, and
additional operation eligibility such as Teacher approval and Enrollment. No separate
disabled-account lifecycle is introduced.


## FR-AUTH-008 — Resource Ownership Authorization

Protected operations involving Teacher-owned resources shall verify resource ownership or other explicitly approved authorization.

## FR-AUTH-009 — Client Role Restriction

The system shall not trust client-supplied role or privilege information as authoritative authorization state.

## FR-AUTH-011 — Nested Tutoring Ownership

Protected Teacher operations shall follow authoritative ownership relationships:
Session -> Course -> Teacher; Assignment -> Session -> Course -> Teacher;
Submission -> Assignment -> Session -> Course -> Teacher. The same Course boundary
shall protect enrolled-Student information, results, progress and related payment
data. Teacher role alone shall not permit cross-owner access. This is authorization
behavior, not a database relationship design.

## FR-AUTH-012 — Discovery and Protected Information Separation

Public Course/Teacher discovery shall not expose protected Session content,
Assignments/submissions, Meet URLs, payment information or Student information.
Any permitted participation or administrative access shall be checked separately.

## FR-AUTH-013 — Authoritative Participation and Payment State

The system shall not trust client-supplied identity, ownership, payment success or
Enrollment eligibility as authoritative state. Backend role/account, ownership
and required Enrollment checks shall govern access. Payment success shall satisfy
FR-PAY-010; Admin access shall remain within approved responsibilities.

---

Supabase Storage holds Teacher avatars, Course thumbnails, Assignment files, Submission
files and refund proof. PostgreSQL stores paths/references and applicable metadata, never
file bytes, base64 or temporary signed URLs. Resolve each path within an explicitly
configured bucket for its usage; exact bucket identifiers remain configuration, and the
path/bucket mapping must be fixed before integration. Spring Boot authorizes access; Storage
does not replace backend business authorization.

---

# 14. Non-Functional Requirements

The approved technical constraints remain Next.js/TypeScript responsive web,
Java Spring Boot with Spring Security/JWT and REST, a feature-based modular
monolith, and PostgreSQL with Supabase-hosted development PostgreSQL. Preserve
established local PostgreSQL support. Core persistence access belongs to the
backend; the frontend shall not directly access PostgreSQL/Supabase. No Supabase
Auth, Data API, frontend supabase-js or RLS replacement for backend authorization
is introduced. No microservices, native application or unapproved infrastructure
is authorized.

Quality requirements below preserve compatible existing IDs. Detailed data
retention, performance measurements and deployment choices remain gated.

## NFR-UI-001 — Web Platform

The system shall be accessible through modern web browsers.

## NFR-UI-002 — Student Mobile-First

Student interfaces shall use a mobile-first design approach.

## NFR-UI-003 — Student Responsive Design

Student interfaces shall remain usable on tablet and desktop screen sizes.

## NFR-UI-004 — Teacher Responsive Design

Teacher interfaces shall be responsive and functional on mobile while optimized primarily for desktop management.

## NFR-UI-005 — Admin Responsive Design

Admin interfaces shall be responsive and functional on mobile while optimized primarily for desktop management.

## NFR-UI-006 — Touch Interaction

Student learning interactions should be suitable for touch-screen devices.

## NFR-UI-007 — No Native Application Requirement

Students shall not be required to install a native Android or iOS application to use the core learning platform.

## NFR-SEC-001 — Password Protection

Passwords shall not be stored in plaintext.

Passwords shall be protected using an appropriate secure password-hashing mechanism supported by the approved backend security architecture.

## NFR-SEC-002 — Authentication Protection

Protected resources shall require valid authentication.

## NFR-SEC-003 — Authorization Validation

Sensitive operations shall verify authorization.

## NFR-SEC-004 — Input Validation

User-provided input shall be validated.

## NFR-SEC-005 — Secret Management

API keys, payment credentials, database credentials, JWT signing secrets/keys, and other secrets shall not be committed directly to source control.

## NFR-SEC-007 — Access Token Expiration

JWT Access Tokens shall have a finite expiration time.

## NFR-SEC-008 — Refresh Token Expiration

Refresh Tokens shall have a finite expiration time.

## NFR-SEC-009 — Refresh Token Revocation

The authentication architecture shall support Refresh Token revocation or invalidation.

## NFR-SEC-010 — Sensitive JWT Claims

JWTs shall not contain:

- plaintext passwords
- password hashes
- password-reset credentials
- email-verification credentials
- API secrets
- unnecessary sensitive information

## NFR-SEC-011 — Browser Authentication Storage

Long-lived authentication credentials shall not be stored in browser `localStorage` by default.

The final web authentication design shall use an approved secure browser-storage/transport strategy.

## NFR-SEC-012 — Cookie Security

When authentication credentials are transported or stored using cookies, the security design shall consider:

- HttpOnly
- Secure in production
- SameSite
- CSRF protection
- CORS configuration

according to the deployment architecture.

## NFR-SEC-013 — Sensitive Authentication Logging

The system shall not intentionally log:

- plaintext passwords
- password hashes
- Refresh Tokens
- password-reset credentials
- email-verification credentials
- authentication secrets

## NFR-SEC-014 — Password Reset Protection

Password-reset credentials shall be:

- time-limited
- purpose-specific
- sufficiently unpredictable
- invalid after successful use where applicable

## NFR-SEC-015 — Email Verification Protection

Email-verification credentials shall be:

- time-limited
- purpose-specific
- associated with the intended account

## NFR-SEC-016 — Backend Security Authority

Authentication, role authorization, account eligibility, ownership, Enrollment
access, payment-dependent privileges and protected business decisions shall not
rely solely on frontend state. Spring Boot shall enforce their approved rules.

## NFR-SEC-017 — No Custom Cryptography

The project shall not implement custom cryptographic algorithms for authentication or password protection.

Approved framework/library security mechanisms shall be used.

## NFR-DATA-001 — Learning Data Persistence

Relevant approved Student learning information shall persist across user sessions.
Specific retention periods and deletion behavior require approved rules; no legacy
assessment-history model is implied.

## NFR-DATA-006 — Authentication State

The system shall retain authentication/session-related state when required by the
approved Refresh Token and account-security design. This requirement does not
prescribe its persistence representation.

## NFR-DATA-007 — Transaction History

The system shall retain historical Teacher bank-account references, price snapshots, actual
bank/provider transactions and full-refund records independently of current Enrollment
state. Changes must not rewrite the historical receiving account; exact cleanup/retention
periods remain gated.


## NFR-MNT-001 — Separation of Concerns

The system should maintain clear separation between presentation, application/business logic, and data-access responsibilities.

## NFR-MNT-002 — API Documentation

Backend APIs should be documented.

## NFR-MNT-003 — Testability

Important business logic should be designed so that it can be tested.

## NFR-MNT-004 — Configuration

Environment-specific configuration shall not be unnecessarily hardcoded.

## NFR-MNT-005 — Scope Simplicity

The architecture should remain appropriate for a graduation project and avoid unnecessary distributed-system complexity.

## NFR-MNT-006 — Feature-Based Organization

Application code should be organized primarily around business features/modules
where practical. Detailed boundaries and package structures belong to architecture
design, not these requirements.

## NFR-MNT-007 — Module Cohesion

Functionality belonging to the same business feature should remain cohesive and should avoid unnecessary coupling with unrelated features.

## NFR-MNT-008 — Shared Concern Reuse

Shared infrastructure and domain functionality should not be unnecessarily duplicated across features.

## NFR-MNT-009 — External Provider Isolation

Approved external-provider-specific implementation should be isolated from core
business logic where practical. This requirement does not select a payment or
Calendar mechanism or require an integration before its gate is resolved.

## NFR-MNT-010 — API Boundary

Persistence entities should not be exposed directly as public API contracts by default.

Explicit request/response representations should be used where appropriate.

## NFR-PERF-001 — Normal Interaction

Common Student tutoring interactions should respond within a reasonable time under
expected project-scale usage. Measurable targets and workload assumptions remain
under the Performance gate; no numeric target is invented.

## NFR-PERF-004 — Pagination

Large management or reporting lists should support pagination or another appropriate bounded retrieval mechanism where necessary.

## NFR-PERF-005 — External API Usage

For approved integrations, the system should avoid unnecessary repeated external
requests where a permitted alternative exists. This does not authorize a provider,
cache, storage policy or infrastructure. Detailed targets remain gated.

---

# 15. Open Decisions and Readiness Gates

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

Retain configurable access/refresh/reset defaults of 15 minutes, 7 days and
15 minutes. Physical V1 omits conceptual provider account/order fields. G-PHYSICAL
is RESOLVED; integration contracts remain separate gates. No implementation is claimed.

---

# 16. Traceability and Legacy-ID Disposition

Only requirement headings in sections 2–14 define active IDs. Historical IDs in
the register below are retired/superseded and cannot satisfy active acceptance,
implementation or completion claims. Gaps in numbering are intentional.

Keep an ID only when its underlying capability/invariant remains materially
compatible. Profile IDs preserve own-profile management but no longer fix the
former field contract; discovery IDs preserve information viewing with tutoring
content. Course IDs preserve owned creation/editing without legacy classification
or a fixed lifecycle. Authentication IDs preserve compatible security safeguards
without deciding Teacher onboarding. Changes are deliberate scope reconciliation,
not approval of legacy dependent contracts.

Retained cross-cutting requirements may be referenced rather than duplicated:
FR-ENR-005 delegates progress semantics to FR-PRO-004; FR-ENR-008 and FR-PAY-012
depend on FR-PAY-010; ownership uses FR-AUTH-008 and FR-AUTH-011. References do
not remove a requirement's decision gate.

New semantic families are FR-SES (Sessions), FR-SCH (scheduling), FR-ASN
(Assignments/submissions), FR-MEET (Course Meet boundary), FR-RATE (participant
feedback) and INT-CAL (Calendar capability). Existing-family additions start after
the old highest number. Session is not a renamed Lesson; Assignment is not the
old assessment engine; participant feedback is not vocabulary Review; Enrollment
is not Subscription. No one-to-one equivalence is implied by the replacements below.

## Historical Disposition Register

Every old definition absent from the active catalogue is listed below. Ranges are
inclusive. These historical records reserve the IDs against reuse; they do not
keep retired requirements active.

| Retired/superseded IDs | Count | Disposition |
|---|---:|---|
| `FR-STU-006` | 1 | Access-tier requirement retired. |
| `FR-ACC-017` | 1 | Premium-specific separation retired; current role/access separation is explicit in scope and authorization. |
| `FR-DIS-001` through `FR-DIS-002` | 2 | Mandatory CEFR discovery retired; tutoring discovery uses FR-DIS-005–006. |
| `FR-ENR-001` through `FR-ENR-003` | 3 | Standard/Premium enrollment rules superseded by new FR-ENR-006–008, without equivalent lifecycle assumptions. |
| `FR-LES-001` through `FR-LES-004` | 4 | Vocabulary Lesson model retired; new Session family FR-SES is independent. |
| `FR-VOC-001` through `FR-VOC-005` | 5 | Vocabulary learning retired. |
| `FR-FILL-001` through `FR-FILL-005` | 5 | Fill Word engine retired. |
| `FR-LIS-001` through `FR-LIS-006` | 6 | Listening engine retired. |
| `FR-QUIZ-001` through `FR-QUIZ-008` | 8 | Quiz engine retired; new Assignments use FR-ASN. |
| `FR-PRO-001` through `FR-PRO-003`, `FR-PRO-005` through `FR-PRO-008` | 7 | Legacy exercise/Lesson/mastery/history requirements retired; current progress/results use FR-PRO-004/009 and FR-ASN-006 without inherited formulas or attempt history. |
| `FR-SEA-001` through `FR-SEA-005` | 5 | Vocabulary search retired; Teacher/Course discovery has distinct requirements. |
| `FR-SAV-001` through `FR-SAV-004` | 4 | Saved vocabulary retired. |
| `FR-REV-001` through `FR-REV-006` | 6 | Vocabulary Review retired; participant feedback uses FR-RATE. |
| `FR-PRE-001` through `FR-PRE-006` | 6 | Premium capabilities retired. |
| `FR-SUB-001` through `FR-SUB-006` | 6 | Subscription model retired; no conversion into Enrollment IDs. |
| `FR-PAY-001`, `FR-PAY-003` through `FR-PAY-006` | 5 | Premium payment/activation requirements and provider-selection placeholder superseded by FR-PAY-007–012 and the Payment gate. |
| `FR-TEA-004` | 1 | Admin-only Teacher provisioning restriction superseded by FR-TEA-005 and the Accounts gate. |
| `FR-TCR-002` | 1 | Mandatory Course CEFR classification retired. |
| `FR-TLE-001` through `FR-TLE-006` | 6 | Teacher vocabulary-Lesson requirements retired; nested ownership principle survives in FR-AUTH-011. |
| `FR-TVOC-001` through `FR-TVOC-005` | 5 | Teacher vocabulary/dictionary workflows retired. |
| `FR-TEX-001` through `FR-TEX-005` | 5 | Teacher legacy exercise engine retired; Assignments use FR-ASN. |
| `FR-TAN-001`, `FR-TAN-004` through `FR-TAN-006` | 4 | Legacy dashboard/aggregate performance, difficult-vocabulary and platform-revenue framing retired; owned progress/Enrollment capabilities remain and detailed analytics are gated. |
| `FR-ADM-005` | 1 | Fixed Teacher provisioning authority superseded by the Accounts gate, not reassigned here. |
| `FR-ACR-003` | 1 | Standard/Premium Course classification retired. |
| `FR-ASU-001` through `FR-ASU-003` | 3 | Admin subscription management retired. |
| `FR-AAN-006` through `FR-AAN-007` | 2 | Unspecified learning aggregates and subscription/revenue dashboard retired; current counts/monitoring remain. |
| `FR-REVN-001` through `FR-REVN-006` | 6 | Platform subscription-revenue reporting retired; transaction monitoring does not grant tuition ownership. |
| `FR-AUTH-005`, `FR-AUTH-010` | 2 | Premium authorization/client-tier checks retired; Enrollment and payment trust use new requirements. |
| `NFR-SEC-006` | 1 | Premium-specific payment safeguard superseded by FR-PAY-010 without weakening payment trust. |
| `NFR-DATA-002` through `NFR-DATA-005`, `NFR-DATA-008` | 5 | Subscription/vocabulary/attempt-history guarantees retired; current retention details are explicitly gated. |
| `NFR-PERF-002` through `NFR-PERF-003` | 2 | Vocabulary search/audio targets retired. |
| `INT-DIC-001` through `INT-DIC-004` | 4 | Dictionary integration retired. |
| `INT-AUD-001` through `INT-AUD-002` | 2 | Vocabulary audio integration retired. |
| `INT-PAY-001` through `INT-PAY-003` | 3 | Mandatory Premium provider integration retired; payment mechanism remains open under FR-PAY-010 and its gate. |

## Preserved Families and Downstream Reconciliation

Preserved existing-ID families: `FR-AAN`, `FR-ACC`, `FR-ACR`, `FR-ADM`, `FR-ATR`, `FR-AUTH`, `FR-DIS`, `FR-ENR`, `FR-PAY`, `FR-PRO`, `FR-STU`, `FR-TAN`, `FR-TCR`, `FR-TEA`, `NFR-DATA`, `NFR-MNT`, `NFR-PERF`, `NFR-SEC`, `NFR-UI`.

99 existing IDs remain active; 40 new IDs are allocated; 128 old IDs are retired/superseded. Every one of the 227 old definitions is accounted for exactly once as preserved or retired.

The following documents require separately approved reconciliation:
BUSINESS_RULES.md, USE_CASES.md, DOMAIN_MODEL.md, ARCHITECTURE.md,
DATABASE_DESIGN.md, API_DESIGN.md, TASK_BREAKDOWN.md, FEATURE_STATUS.md and README.md
where applicable. Their old references, ranges and family wildcards do not make
retired requirements current. Do not silently retarget a legacy ID to a new meaning.

Trace active requirements to later business rules, use cases, designs and
verification when those artifacts are reconciled. This document adds no routes,
schema, detailed UI contracts or implementation tasks.

Preserve completed TASK-001/TASK-002/TASK-003 infrastructure and evidence. This
reconciliation does not reset their status or authorize the next task. No
downstream document is modified as part of this requirements migration.
