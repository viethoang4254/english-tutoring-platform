# English Tutoring Platform — Business Rules

**Version:** 0.3
**Status:** Reconciled tutoring invariants with explicit open decision gates
**Project Type:** Graduation Project

# 1. Authority, Rule Conventions and Scope

These rules constrain the approved Development of an English Tutoring Platform.
Authority follows AGENTS.md, PROJECT_SPEC.md and current REQUIREMENTS.md,
in that order. Conflicting legacy business/design content
does not establish current policy.

Project-local skills provide subordinate working guidance for applying the canonical
documentation. They do not override or redefine the canonical project documents.

Sections 2–14 contain active BR definitions. Each states a governing constraint,
not an implementation task or a repetition of every functional capability.
Requirement references identify supporting active requirements; they do not
authorize missing policy. Sections 15–16 record open decisions and historical
disposition. Deferred/retired IDs do not count as active rules.

A role, client claim or public discovery response does not establish ownership,
payment success or participation eligibility. Apply all relevant approved checks.
An unresolved mechanism never weakens an approved trust/access invariant.

This document prescribes no schema, API, UI layout, provider protocol or integration
implementation. Preserve the approved technical foundations and TASK-001/002/003
evidence. This reconciliation authorizes no implementation or downstream edits.

---

# 2. Roles, Account Security and Onboarding Boundaries

## BR-ROLE-001 — Roles and Conditional Authorization

The primary roles are STUDENT, TEACHER and ADMIN. Role membership does not by itself authorize arbitrary resource access: applicable account eligibility, ownership, Enrollment and approved operation permissions must also hold.

**Requirements:** FR-AUTH-001, FR-AUTH-007, FR-AUTH-011, FR-AUTH-013.

## BR-AUTHN-002 — Student Role Authority

Student registration assigns the STUDENT role through trusted backend logic. A client cannot select privileged roles through that registration. This rule is limited to Student registration and does not define Teacher onboarding.

**Requirements:** FR-STU-001, FR-ACC-016.

## BR-AUTHN-006 — Verification Credential

Email canonicalization is lowercase(trim(inputEmail)) for registration, login,
verification, email change and password-reset/account lookup. Persist users.email and
verification target_email canonically; do not remove dots/+tags or apply provider-specific
normalization. users.email is UNIQUE; target_email is not unique.

Verification credentials are purpose-specific, hashed, expiring and single-use.
INITIAL_VERIFICATION and EMAIL_CHANGE are distinct purposes. Canonicalize the target email;
retain the old effective email until the new email is successfully verified. Verification
alone does not override a lock or approve Teacher onboarding. Exact lifetime/resend controls
remain open.

**Requirements:** FR-ACC-008, FR-ACC-009, NFR-SEC-015.

## BR-AUTHN-007 — Access Token

Successful authentication uses a short-lived JWT Access Token. The retained configurable default lifetime is 15 minutes.

**Requirements:** FR-ACC-001, FR-ACC-002, NFR-SEC-007.

## BR-AUTHN-008 — Refresh Token

A valid Refresh Token permits obtaining a new Access Token without resubmitting account credentials, subject to current eligibility checks. The retained configurable default lifetime is 7 days.

**Requirements:** FR-ACC-003, FR-ACC-004, NFR-SEC-008.

## BR-AUTHN-009 — Refresh Validation

A new Access Token may be issued from a Refresh Token only if it is valid, unexpired, unrevoked, associated with an eligible account/session and accepted by approved security rules. Invalid or unusable refresh credentials cannot mint an Access Token.

**Requirements:** FR-ACC-004, FR-ACC-005.

## BR-AUTHN-010 — Refresh Rotation

Refresh Token rotation must be supported according to the approved security design. When a replacement is issued under that design, replaced credentials must be invalidated accordingly. Client state is not authoritative. Timing, replay, concurrency and session-limit policies remain open.

**Requirements:** FR-ACC-006, NFR-SEC-009.

## BR-AUTHN-011 — Logout

Logout invalidates applicable Refresh Token/session state. Frontend deletion alone is not authoritative logout. This does not imply immediate revocation of all previously issued Access Tokens.

**Requirements:** FR-STU-003, FR-ACC-006.

## BR-AUTHN-012 — Current Identity

Current-user information comes from authenticated backend identity and authoritative account information. Changing a client-supplied user identifier cannot select another user's identity.

**Requirements:** FR-ACC-007, FR-AUTH-013.

## BR-AUTHN-013 — Password Change

An authenticated eligible User may change their password through the dedicated security
operation. Revoke other refresh sessions while preserving the current session. Never expose
plaintext passwords in logs/responses.

**Requirements:** FR-ACC-010, NFR-SEC-013.

## BR-AUTHN-014 — Password Recovery Privacy

Password recovery uses an account's supported recovery identifier. Responses must avoid unnecessary disclosure of account existence; recovery must not bypass the credential safeguards in BR-AUTHN-015.

**Requirements:** FR-ACC-011, FR-ACC-012.

## BR-AUTHN-015 — Password Reset

Password reset requires a valid, purpose-specific, unexpired, unused hashed credential for
the intended account. Consume it once and revoke all refresh sessions on success.

**Requirements:** FR-ACC-012, FR-ACC-013, NFR-SEC-014.

## BR-AUTHN-016 — Password Reset Lifetime

The retained configurable default password-reset credential lifetime is 15 minutes.

**Requirements:** FR-ACC-013, NFR-SEC-014.

## BR-AUTHN-017 — Account Eligibility

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

**Requirements:** FR-ACC-014, FR-AUTH-007.

## BR-AUTHN-018 — Locked Account

users.locked is the sole V1 account-blocking mechanism. A locked account cannot authenticate
or perform normal account use until an authorized unlock. Locking is not deletion and does
not erase history; automatic lockout policy is not implied.

**Requirements:** FR-ACC-014, FR-ADM-006.

## BR-AUTHN-019 — Single Account-Blocking Mechanism

No separate disabled/enabled/account_status lifecycle exists in V1. This retained rule ID
refers to the locked-account restriction in BR-AUTHN-018 and protection of historical data,
not a second blocking state.

**Requirements:** FR-ACC-015, NFR-DATA-001, NFR-DATA-007.

## BR-AUTHN-020 — Role Authority

Only trusted backend state establishes role authority. A client cannot assign, modify or elevate its own role.

**Requirements:** FR-ACC-016, FR-AUTH-009.

## BR-AUTHN-023 — Admin Provisioning Boundary

Admin accounts are not publicly self-registered. Provisioning requires an authorized administrative or system-setup workflow; Students and Teachers cannot promote themselves to ADMIN. This is not a Teacher onboarding rule.

**Requirements:** FR-ACC-016, FR-ADM-001.

## BR-AUTHN-024 — Teacher Onboarding Boundary

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots. Review snapshots are immutable historical evidence after review. PENDING
applications are unreviewed; APPROVED/REJECTED applications identify reviewer/time, and
rejection records a reason. Detailed review-operation contracts remain open.

**Requirements:** FR-TEA-005, FR-ACC-016.

---

# 3. Profile Identity and Permitted Changes

## BR-PROFILE-002 — Own-Profile and Privilege Boundary

Students and Teachers edit only authorized own-profile information using authenticated
identity. Ordinary profile editing cannot change role, locked/verified state, onboarding
approval or participation/payment authority. User profile data and the current public
Teacher profile do not create separate login identities. Teacher application snapshots must
not change with profile edits. Email changes require verified new email before replacement;
password changes use dedicated security operations. Required text is validated as nonblank
by the backend; unspecified field limits are not invented.

**Requirements:** FR-STU-004, FR-STU-005, FR-TEA-002, FR-TEA-003, FR-AUTH-013.

---

# 4. Teacher and Course Ownership

## BR-COURSE-001 — Single Course Owner

Every Course belongs to exactly one owning Teacher. Ownership comes from authoritative
backend state and cannot be transferred in V1.

**Requirements:** FR-TCR-003.

## BR-COURSE-002 — Multiple Courses

A Teacher may own multiple Courses; owning one Course does not grant authority over another Teacher's Course.

**Requirements:** FR-TCR-001, FR-TCR-006.

## BR-COURSE-003 — Multiple Teachers

The platform supports multiple Teachers. Teacher identity alone grants no authority over another Teacher's resources.

**Requirements:** FR-DIS-005, FR-TCR-006.

## BR-COURSE-004 — Owned-Course Management

A draft Course may omit meet_url; backend publication requires a valid Teacher-provided
Meet URL. Tuition is nonnegative and finite; zero means free. V1 currency is VND only.
min_students is positive and max_students >= min_students. session_count is the fixed
positive planned count and cancellation does not change it. Discount fields are all
absent or all present, with 0 < discount_percent < 100 and start < end. A 100% discount
is not the representation of a free Course.

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots. Teachers
manage only owned Courses under applicable permissions. Admin oversight neither transfers
ownership nor grants arbitrary mutation. Detailed publication/completion prerequisites and
deletion/retention edges remain gated.

**Requirements:** FR-TCR-005, FR-TCR-006, FR-TCR-007, FR-ACR-002.

## BR-AUTH-003 — Teacher Resource Ownership

Backend Teacher authorization must follow authoritative ownership through Course -> Teacher, Session -> Course -> Teacher, Assignment -> Session -> Course -> Teacher and Submission -> Assignment -> Session -> Course -> Teacher. The Course boundary also applies to enrolled-Student teaching information, results, progress and related transactions. Teacher role alone never permits cross-owner management. These are authorization relationships, not storage design.

**Requirements:** FR-AUTH-008, FR-AUTH-011, FR-TCR-010, FR-PAY-011.

## BR-TEA-001 — Owned Teaching Content

Teacher management of protected teaching content is restricted to owned Courses and their authorized child resources under BR-AUTH-003. No cross-Teacher management exception is implied.

**Requirements:** FR-TCR-006, FR-SES-002, FR-ASN-002.

---

# 5. Course and Session Relationships

## BR-COURSE-007 — Whole-Course Commercial Unit

Students purchase entire Courses, never individual Sessions. Enrollment is unique per
Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A COMPLETED Enrollment is
historical and cannot simply re-enroll into that Course instance. Free Courses activate
participation without fake Payments. Paid participation may reserve a seat while PENDING.
Capacity counts ACTIVE plus PENDING Enrollments with unexpired reservations; min_students
counts ACTIVE only. Expired reservations consume no capacity. No new Enrollment or Payment
may begin after the first Session has started. Activation, reservation and late-payment
handling must be concurrency-safe.

**Requirements:** FR-ENR-006, FR-PAY-008, FR-TCR-008, FR-TCR-009.

## BR-SES-001 — Session Parent and Meaning

Every Session belongs to one Course and may contain nullable protected learning content and
Assignments. Generated Sessions need not have content yet. Public preview must not expose
this content. All Sessions use the Course's protected manual Meet URL.

**Requirements:** FR-SES-001, FR-SES-004.

## BR-SES-002 — Session Management Boundary

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

**Requirements:** FR-SES-002, FR-AUTH-011.

---

# 6. Scheduling Boundaries

## BR-SCH-001 — Recurrence and Occurrence Separation

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking. Recurring rules use Course
timezone/local times and concrete Sessions use absolute timestamps.

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

**Requirements:** FR-SCH-001, FR-SCH-002.

---

# 7. Assignments, Submissions and Results

## BR-ASN-001 — Assignment Parent and Ownership

Every Assignment belongs to exactly one Session. Assignment and submission authorization derives through the Session's Course and owning Teacher under BR-AUTH-003.

**Requirements:** FR-ASN-001, FR-ASN-002, FR-ASN-005.

## BR-ASN-002 — Student Submission Boundary

Student identity and Course participation access are backend-enforced. Assignments belong to
Sessions and use ACTIVE/CANCELLED. Any Submission prevents hard deletion of its Assignment.
There is one current Submission per Student/Assignment, using DRAFT/SUBMITTED/GRADED; no
revision-history, result or grading table is introduced. Score, feedback and grading
metadata remain on Submission. Only the owning Teacher grades, and a score cannot exceed
Assignment max_score. A numeric score is not made mandatory merely by GRADED status.
Submission/Assignment attachments use Supabase Storage references and metadata, not database
file bytes. Deadline/late-submission and file-validation details remain open.

**Requirements:** FR-ASN-003, FR-ASN-004, FR-ENR-007, FR-AUTH-013.

## BR-ASN-003 — Submission and Result Privacy

Assignment due_at is required. max_score is finite positive NUMERIC(5,2); nullable
Submission score is finite nonnegative NUMERIC(5,2) and must not exceed Assignment
max_score. The cross-row comparison is transactional; there is no maximum of 100.

Students access only authorized own submissions/results; Teachers inspect and grade only
within owned Courses. SUBMITTED requires submission time; GRADED requires submission time,
grading time and grader. Results are the Submission's score/feedback/metadata, not a
separate result identity. Numeric scores remain optional unless a later rule requires them.

**Requirements:** FR-ASN-005, FR-ASN-006, FR-AUTH-011.

---

# 8. Enrollment and Participation Access

## BR-ENR-001 — Enrollment Relationship

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.

**Requirements:** FR-ENR-004, FR-ENR-006.

## BR-ENR-006 — Authoritative Participation

Protected Course participation requires valid authoritative Enrollment where applicable. This governs Student access to protected Session content, Assignments, submissions/results/progress, Meet information and related participation data as appropriate. Enrollment does not grant access to another Student's private information or Teacher/Admin management.

**Requirements:** FR-ENR-007, FR-ASN-006, FR-AUTH-002, FR-AUTH-013.

## BR-ENR-007 — Discovery Is Not Participation

Public Teacher/Course discovery does not grant protected Course participation. Visibility of permitted public information is distinct from authorization for protected resources.

**Requirements:** FR-DIS-003, FR-DIS-004, FR-AUTH-012.

## BR-ENR-008 — Payment and Enrollment Separation

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference. Actual provider/bank transactions may be unmatched.
They retain receiving-account context when resolvable, independently of Payment matching; an
unresolved receiver remains a reconciliation concern. Browser, Student and Teacher claims
are not confirmation evidence. Confirmation requires trustworthy provider/bank evidence
matching the intended receiver, code, amount and currency; wrong/missing codes or amounts do
not auto-confirm, partial transfers are not summed automatically, and late transactions
cannot cause overbooking. Payment and Enrollment transitions require one concurrency-safe
backend workflow; a confirmed late payment never grants an over-capacity seat.

**Requirements:** FR-ENR-006, FR-ENR-008, FR-PAY-012.

---

# 9. Teacher Payment Destinations and Direct Payment

## BR-PAY-006 — Receiving Information Ownership

Teacher receiving accounts are private owned information. Retain account history; at most
one ACTIVE account per Teacher. Existing Payments retain their original receiving-account
reference after account changes. Actual bank account ownership/verification and provider
contracts remain integration gates.

**Requirements:** FR-PAY-007.

## BR-PAY-007 — Course Determines Destination

Payment for a Course must be directed to the configured payment destination of its owning Teacher. Student/client input cannot substitute another Teacher's destination for that Course payment.

**Requirements:** FR-PAY-008, FR-AUTH-013.

## BR-PAY-008 — Direct Tuition and No Custody

Tuition goes directly to the owning Teacher. Platform/Admin does not hold tuition, pay out
funds or operate wallets, escrow, commissions or accounting. The separately approved
full-refund workflow is governed by BR-PAY-011.

**Requirements:** FR-PAY-009, FR-PAY-011.

---

# 10. Payment Trust and Enrollment Effects

## BR-PAY-002 — Untrusted Payment Claims

A frontend success screen, browser state, Student claim or arbitrary client request alone is never sufficient authoritative evidence of payment success.

**Requirements:** FR-PAY-010, FR-PAY-012.

## BR-PAY-003 — Transaction Information

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference. Actual provider/bank transactions may be unmatched.
They retain receiving-account context when resolvable, independently of Payment matching; an
unresolved receiver remains a reconciliation concern. Browser, Student and Teacher claims
are not confirmation evidence. Confirmation requires trustworthy provider/bank evidence
matching the intended receiver, code, amount and currency; wrong/missing codes or amounts do
not auto-confirm, partial transfers are not summed automatically, and late transactions
cannot cause overbooking. Unmatched transactions retain a nullable receiving-account
reference independently of nullable Payment matching.

**Requirements:** FR-PAY-002, FR-PAY-011, NFR-DATA-007.

## BR-PAY-009 — Trustworthy Confirmation Prerequisite

Actual provider/bank transactions may be unmatched. They retain receiving-account context
when resolvable, independently of Payment matching; an unresolved receiver remains a
reconciliation concern. Browser, Student and Teacher claims are not confirmation evidence.
Confirmation requires trustworthy provider/bank evidence matching the intended receiver,
code, amount and currency; wrong/missing codes or amounts do not auto-confirm, partial
transfers are not summed automatically, and late transactions cannot cause overbooking. A
CONFIRMED Payment requires confirmation time. Amount alone is not matching evidence.
Physical V1 omits provider_account_ref/provider_order_id and their order index.
Provider transaction IDs carry no unverified global uniqueness guarantee; the selected
adapter must enforce verified-context transactional idempotency before integration.

**Requirements:** FR-PAY-010, FR-ENR-008, FR-PAY-012.

## BR-PAY-010 — Payment Data Visibility

Course-related transaction access must respect owning-Teacher boundaries, related-Student permissions and authorized Admin access. Teacher A cannot obtain unauthorized management of Teacher B's transactions. Permission to view/monitor status grants neither unrestricted outcome modification nor payment confirmation authority.

**Requirements:** FR-PAY-011, FR-ATR-001, FR-ATR-002, FR-AUTH-011.

---

## BR-PAY-011 — Full Refund Evidence and Authority

V1 supports full refunds only, with at most one Refund per Payment. The amount equals the
applicable full Payment amount under the approved workflow. Teacher performs the bank
transfer back to the Student and submits proof; Admin verifies completion. Refund statuses
are PENDING, SUBMITTED, COMPLETED and CANCELLED. Payment remains historical and has no
REFUNDED status. This does not authorize platform custody, payouts, commissions, escrow or
accounting. SUBMITTED requires proof and submission time; COMPLETED additionally requires
completion time and the verifying Admin. Preserve earlier evidence through legitimate later
transitions. Refund amount, Payment eligibility and reviewer authorization require
transactional validation.

**Requirements:** FR-PAY-013, FR-PAY-009, NFR-DATA-007.

---

# 11. Google Meet and Google Calendar Business Boundaries

## BR-MEET-001 — Single External Course Meeting

The owning Teacher creates Google Meet externally and manually supplies exactly one Meet URL for the Course. Every Session uses that same Course URL. No automatic meeting creation, Google Meet API or one-meeting-per-Session model is approved; no dedicated meeting entity is prescribed.

**Requirements:** FR-MEET-001, FR-MEET-003.

## BR-MEET-002 — Protected Meeting Information

The Course Meet URL is protected participation information. Unauthorized users, including non-enrolled Students where Enrollment is required, must not obtain protected Meet access through discovery or another bypass.

**Requirements:** FR-MEET-002, FR-ENR-007, FR-AUTH-012.

## BR-AUTH-005 — Calendar Information Boundary

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

**Requirements:** INT-CAL-001, FR-AUTH-011, FR-ENR-007.

---

# 12. Progress and Participant Feedback Boundaries

## BR-TEA-002 — Owned-Course Progress

Teacher access to learning results/progress is limited to authorized Students/resources in owned Courses. It does not grant cross-Teacher access or establish a progress formula, completion threshold or grading aggregation.

**Requirements:** FR-TAN-003, FR-ASN-005.

## BR-AUTH-006 — Personal Progress

Student progress/results access is limited to the authenticated Student's authorized information. Persisted information remains backend-authoritative; progress calculation and completion rules remain unresolved.

**Requirements:** FR-PRO-004, FR-PRO-009, FR-ASN-006, NFR-SEC-016.

## BR-RATE-001 — Participation-Related Feedback

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.

**Requirements:** FR-RATE-001.

---

# 13. Admin Authority and Monitoring

## BR-ADM-001 — Limited Platform Administration

Admin performs only approved account/profile, Category, Course and Enrollment administration
and monitoring. Used Categories are deactivated, not deleted; each Course has exactly one
Category. Admin verifies refund completion under BR-PAY-011 without receiving tuition or
executing Teacher payouts. Role membership alone grants no additional override/moderation
rights.

**Requirements:** FR-ADM-002, FR-ADM-003, FR-ADM-004, FR-ADM-006, FR-ADM-007, FR-ADM-008, FR-AAN-008.

## BR-ADM-002 — Course Oversight Boundary

Admin oversight across Courses does not make Admin the Course owner or default Assignment grader. Exact override/moderation authority remains open.

**Requirements:** FR-ACR-001, FR-ACR-002.

## BR-ADM-005 — Transaction Monitoring Boundary

Authorized Admin transaction monitoring is subject to BR-PAY-010 and does not confer tuition receipt/custody, payment confirmation authority or arbitrary outcome modification. Unverified/unknown information is not automatically a verified success or confirmed failure. Detailed outcome classifications and reporting semantics remain open.

**Requirements:** FR-ATR-001, FR-ATR-002, FR-ATR-003, FR-AAN-008.

---

# 14. Cross-Cutting Authorization, Privacy and Data Integrity

## BR-AUTH-001 — Backend Enforcement

The backend enforces applicable role, account eligibility, ownership, nested ownership, Enrollment and specific administrative permissions before protected effects or disclosure. Frontend/client state cannot establish these authorities.

**Requirements:** FR-AUTH-006, FR-AUTH-007, FR-AUTH-013, NFR-SEC-016.

## BR-AUTH-002 — UI Is Not Authorization

Hiding or disabling frontend controls does not replace backend authorization.

**Requirements:** FR-AUTH-006.

## BR-AUTH-007 — Protected Discovery Boundary

Public discovery must not disclose protected Session content, participation-restricted Assignments, submissions/results, Meet URLs, private Teacher receiving information, protected transactions or enrolled-Student information. Separate authorization is required for protected access.

**Requirements:** FR-AUTH-012, FR-DIS-003, FR-DIS-004.

## BR-TEA-003 — Student Information Privacy

Teacher access to detailed Student information is limited to legitimate authorized educational functions within owned Courses. Aggregated views cannot substitute for authorization, and a preference for aggregation must not prevent approved individual submission/result review. Exact fields remain open.

**Requirements:** FR-TCR-010, FR-ASN-005, FR-AUTH-011.

## BR-WEB-001 — Student Web Direction

The Student mobile-first constraint is governed by NFR-UI-002; this retained reference adds no layout policy.

**Requirements:** NFR-UI-002.

## BR-WEB-002 — Teacher Web Direction

Teacher desktop-oriented, responsive/mobile-usable behavior is governed by NFR-UI-004; no additional interface rule is introduced.

**Requirements:** NFR-UI-004.

## BR-WEB-003 — Admin Web Direction

Admin desktop-oriented, responsive/mobile-usable behavior is governed by NFR-UI-005; no additional interface rule is introduced.

**Requirements:** NFR-UI-005.

## BR-WEB-004 — No Native Application Requirement

The absence of a required native application is governed by NFR-UI-007; this rule does not introduce a separate mobile product.

**Requirements:** NFR-UI-007.

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

G-PHYSICAL is RESOLVED for the final provider-neutral 22-table V1. Integration
contracts remain separate gates. No triggers or implementation are authorized here.

---

# 16. Requirement Traceability and Legacy-ID Disposition

This register records the earlier scope migration, not today's open-decision list.
Independent V1 approvals in active sections supersede its then-deferred choices.

Each active BR lists targeted active requirement references. Capabilities such as
searching Teachers, viewing counts or opening profile information are not duplicated
as BRs unless an ownership, privacy or other governing invariant is needed.

## Original-ID Accounting

| Original-ID outcome | Count | IDs |
|---|---:|---|
| Active/preserved or reconciled with compatible meaning | 38 | `BR-ADM-001`, `BR-ADM-002`, `BR-ADM-005`, `BR-AUTH-001`, `BR-AUTH-002`, `BR-AUTH-003`, `BR-AUTHN-002`, `BR-AUTHN-006`, `BR-AUTHN-007`, `BR-AUTHN-008`, `BR-AUTHN-009`, `BR-AUTHN-010`, `BR-AUTHN-011`, `BR-AUTHN-012`, `BR-AUTHN-013`, `BR-AUTHN-014`, `BR-AUTHN-015`, `BR-AUTHN-016`, `BR-AUTHN-017`, `BR-AUTHN-018`, `BR-AUTHN-019`, `BR-AUTHN-020`, `BR-AUTHN-023`, `BR-COURSE-001`, `BR-COURSE-002`, `BR-COURSE-003`, `BR-COURSE-004`, `BR-ENR-001`, `BR-PAY-002`, `BR-PAY-003`, `BR-ROLE-001`, `BR-TEA-001`, `BR-TEA-002`, `BR-TEA-003`, `BR-WEB-001`, `BR-WEB-002`, `BR-WEB-003`, `BR-WEB-004` |
| Inactive: retired/superseded/deferred | 98 | Listed below |

The earlier tutoring reconciliation added 23 BR IDs and had 61 active rules.
Phase 2 adds BR-PAY-011 for approved full refunds, bringing the active total to 62.
No original ID is reused for unrelated semantics.


Ranges in the following register are inclusive. Every original definition is
accounted for exactly once. Retired/superseded/deferred entries below are historical
records, not active rules. Deferred entries do not silently retain their old policy.

| Original inactive IDs | Count | Disposition |
|---|---:|---|
| `BR-ROLE-002`, `BR-ROLE-003`, `BR-ROLE-004` | 3 | Retired: Standard/Premium access and subscription framing. |
| `BR-AUTHN-001`, `BR-AUTHN-003`, `BR-AUTHN-004`, `BR-AUTHN-005`, `BR-AUTHN-021`, `BR-AUTHN-022` | 6 | Superseded/retired: Student-only registration, access tiers and fixed Teacher provisioning. BR-AUTHN-001 and BR-AUTHN-022 are superseded by the safe onboarding boundary, not an automatic Teacher workflow; BR-AUTHN-003 and BR-AUTHN-021 are retired. BR-AUTHN-004 and BR-AUTHN-005 are deferred: detailed initial Student transitions/action lists require reconciliation, while verification safeguards remain under active account rules. |
| `BR-PROFILE-001` | 1 | Superseded by BR-PROFILE-002: exact fields, lengths and update semantics are open; the former contract is not active. |
| `BR-CEFR-001`, `BR-CEFR-002`, `BR-CEFR-003` | 3 | Retired: mandatory CEFR organization. |
| `BR-COURSE-005`, `BR-COURSE-006` | 2 | Retired: Standard/Premium classification and authority. |
| `BR-CSTATUS-001`, `BR-CSTATUS-002`, `BR-CSTATUS-003` | 3 | Deferred: exact lifecycle, discovery-state conditions and archival effects; old states/defaults are not operative rules. |
| `BR-ENR-002`, `BR-ENR-003`, `BR-ENR-004`, `BR-ENR-005` | 4 | BR-ENR-002, BR-ENR-003 and BR-ENR-005 retired as tier rules; BR-ENR-004 deferred because duplicate-active-Enrollment policy remains open. |
| `BR-LESSON-001`, `BR-LESSON-002`, `BR-LESSON-003`, `BR-LESSON-004` | 4 | Retired: vocabulary Lesson model; new Sessions have independent BR-SES rules. |
| `BR-LCOMP-001`, `BR-LCOMP-002`, `BR-LCOMP-003` | 3 | Retired: legacy learning sequence, assessment completion and reattempt rules. |
| `BR-CCOMP-001`, `BR-CCOMP-002` | 2 | Retired: Lesson-based completion and percentage formulas. |
| `BR-VOC-001`, `BR-VOC-002`, `BR-VOC-003`, `BR-VOC-004`, `BR-VOC-005`, `BR-VOC-006` | 6 | Retired: shared vocabulary, senses and CEFR metadata. |
| `BR-DIC-001`, `BR-DIC-002`, `BR-DIC-003`, `BR-DIC-004` | 4 | Retired: dictionary import/licensing workflow. |
| `BR-SEARCH-001`, `BR-SEARCH-002`, `BR-SEARCH-003` | 3 | Retired: vocabulary search/access rules. |
| `BR-SAVE-001`, `BR-SAVE-002`, `BR-SAVE-003`, `BR-SAVE-004` | 4 | Retired: saved vocabulary and subscription-independent collection rules. |
| `BR-EX-001`, `BR-EX-002`, `BR-EX-003` | 3 | Retired: legacy exercise, correctness and attempt engine. |
| `BR-FILL-001`, `BR-FILL-002`, `BR-FILL-003` | 3 | Retired: Fill Word behavior and answer normalization. |
| `BR-LIS-001`, `BR-LIS-002`, `BR-LIS-003` | 3 | Retired: Listening behavior. |
| `BR-QUIZ-001`, `BR-QUIZ-002` | 2 | Retired: Quiz behavior; new Assignments do not inherit it. |
| `BR-SCORE-001`, `BR-SCORE-002`, `BR-SCORE-003`, `BR-SCORE-004` | 4 | Retired: score scale, formula, attempts and best-score behavior. |
| `BR-ACC-001` | 1 | Retired: vocabulary answer-accuracy formula. |
| `BR-MAST-001`, `BR-MAST-002`, `BR-MAST-003` | 3 | Retired: mastery formulas, evidence and thresholds. |
| `BR-WEAK-001`, `BR-WEAK-002`, `BR-WEAK-003` | 3 | Retired: weak-vocabulary rules. |
| `BR-REV-001`, `BR-REV-002`, `BR-REV-003` | 3 | Retired: vocabulary Review; participant feedback uses BR-RATE. |
| `BR-HIST-001`, `BR-HIST-002`, `BR-HIST-003` | 3 | Retired: attempt history and subscription-dependent historical rules. |
| `BR-PRE-001`, `BR-PRE-002` | 2 | Retired: Premium content/access. |
| `BR-SUB-001`, `BR-SUB-002`, `BR-SUB-003`, `BR-SUB-004`, `BR-SUB-005` | 5 | Retired: subscription periods, expiry, entitlements and associated preservation policies. |
| `BR-PAY-001`, `BR-PAY-004`, `BR-PAY-005` | 3 | Superseded: Premium/subscription effects removed; new Course-payment rules establish trust and separation without translating lifecycle. |
| `BR-TEA-004` | 1 | Retired: platform-revenue analytics framing; current transaction access is constrained separately. |
| `BR-ADM-003`, `BR-ADM-004`, `BR-ADM-006`, `BR-ADM-007` | 4 | Retired: access classification, subscription oversight, platform revenue and renewal classifications. |
| `BR-REVN-001`, `BR-REVN-002`, `BR-REVN-003` | 3 | Retired: subscription-revenue source, calculations and reporting timestamps; no replacement revenue dashboard implied. |
| `BR-DATA-001`, `BR-DATA-002`, `BR-DATA-003` | 3 | Retired: legacy subscription/archive preservation, shared vocabulary and attempt-context policies. New detailed retention/deletion policy remains open. |
| `BR-AUTH-004` | 1 | Retired: Premium authorization. |

## Semantic Migration Notes

- Retained Student role assignment is scoped to Student registration, not all public registration.
- Account eligibility/verification safeguards remain; fixed Teacher onboarding and legacy activation assumptions are not imported.
- Retained Course/content authorization preserves ownership, while obsolete classification restrictions and fixed lifecycle claims are removed.
- Retained transaction recording no longer mandates a provider workflow. Existing frontend distrust is preserved and strengthened by the new confirmation prerequisite.
- Retained privacy and Admin monitoring do not establish new grading, custody, moderation or reporting policy.
- BR-WEB identifiers remain lightweight NFR references; they add no layout design.
- New BR-SES, BR-SCH, BR-ASN, BR-MEET and BR-RATE families describe new semantics.
  Old Lesson, Quiz, Subscription and vocabulary Review IDs are not renamed into them.
- New numbers within existing families exceed their historical maximum. An old reference remaining syntactically resolvable does not make its legacy context current.

Unnumbered legacy rules are also superseded: the vocabulary learning sequence,
mastery activity assumptions, Standard/Premium benefits, subscription renewal
defaults and old scope exclusions are not active policy. Current publishing
direction is preserved without the former fixed lifecycle. No standalone historical
formula is transferred into grading or progress. The former live-class exclusion
does not exclude the approved external Meet-based tutoring.

## Downstream Reconciliation

USE_CASES.md, DOMAIN_MODEL.md, ARCHITECTURE.md, DATABASE_DESIGN.md, API_DESIGN.md,
TASK_BREAKDOWN.md, FEATURE_STATUS.md and README.md where applicable require
separate semantic reconciliation. Their ranges/wildcards and historical BR references
must not be treated as active approval without checking this register and current
requirements. No downstream reference is repaired by this task.

Keep requirement -> governing rule -> later use case/design/verification traceability
where meaningful. Do not mechanically create one BR per requirement.

Preserve completed TASK-001/TASK-002/TASK-003 infrastructure and evidence. No status
reset, database change, implementation or USE_CASES work is authorized here.
