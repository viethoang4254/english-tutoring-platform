# English Tutoring Platform — API Design

**Status:** Reconciled API responsibilities and trust boundaries; detailed wire contracts remain gated.

## 1. Authority, Purpose and API Status

This document translates the approved English Tutoring Platform scope into API
responsibilities, access boundaries and decisions needed before implementation.
It does not claim that documented capabilities or application endpoints exist.

Authority follows [AGENTS.md](../AGENTS.md) ->
[PROJECT_SPEC.md](PROJECT_SPEC.md) -> [REQUIREMENTS.md](REQUIREMENTS.md) ->
[BUSINESS_RULES.md](BUSINESS_RULES.md) -> [USE_CASES.md](USE_CASES.md) ->
[DOMAIN_MODEL.md](DOMAIN_MODEL.md) -> [ARCHITECTURE.md](ARCHITECTURE.md) ->
[DATABASE_DESIGN.md](DATABASE_DESIGN.md) -> this document.
Apply the compatible requirements-analysis, english-tutoring-domain, system-design
and authentication-security project-local skills within that authority.

| Status | Meaning |
| --- | --- |
| APPROVED RESPONSIBILITY | Current capability or security invariant established upstream; this does not finalize a method, path, DTO or lifecycle. |
| CANDIDATE CONTRACT | An explicitly illustrative wire convention requiring review before implementation. It cannot close a business-policy gate. |
| DEFERRED | A named policy or representation decision prevents finalizing the dependent contract. |
| RETIRED | Historical API direction only, confined to Section 23; not current implementation work. |

Sections 3-18 describe approved responsibility areas with their dependent contracts
deferred as stated. Section 19 includes limited candidate wire conventions.
No new method/path catalogue is selected in this rewrite. Section 23 contains
historical method/path pairs only; it is not a current endpoint catalogue.
There is no final current endpoint count or assertion that all contracts are ready.

TASK_BREAKDOWN.md, FEATURE_STATUS.md and README.md cannot supply missing policy.
Preserve completed TASK-001, TASK-002 and TASK-003 infrastructure and evidence.
FEATURE_STATUS.md remains the sole live implementation-status authority.
This document authorizes no code, schema, configuration or downstream edits.

## 2. API Principles and Versioning

Preserve use-case-oriented REST, JSON where appropriate, and the /api/v1 version
direction. Design around actor goals and permitted effects, not table CRUD.
A domain concept need not have a standalone API resource.

The core application path remains:

~~~text
Browser -> Next.js -> Spring Boot REST API
        -> Spring Data JPA / Hibernate -> Supabase-hosted PostgreSQL
~~~

Supabase is development PostgreSQL hosting. Local PostgreSQL remains supported;
production hosting choices remain unresolved. Browser and Next.js server execution
must not bypass Spring Boot for core data. No Supabase Auth, Data API, supabase-js,
Prisma, Storage, Realtime or Edge Functions is introduced. Supabase RLS does not
replace application authorization. Preserve verified Session Pooler/TLS configuration.

Spring Boot owns authentication, validation, authorization and business effects.
Use explicit allowlisted DTOs, never public JPA entity serialization. Keep the
modular monolith and feature boundaries without prescribing controller/repository
names, classes, annotations, frontend hooks or provider SDKs.

Keep identifiers and monetary values safe from JSON numeric precision loss.
Wire examples do not select database keys, columns, enums or constraints.
General response conventions are in Section 19; detailed limits remain gated.

References: NFR-MNT-001, NFR-MNT-005, NFR-MNT-006, NFR-MNT-010;
AR-001, AR-002, AR-003, AR-004, AR-005, AR-006, AR-008, AR-010.

## 3. Authentication and Identity

**APPROVED RESPONSIBILITY:** Shared User identity, email/password authentication,
Spring Security and JWT Access Tokens with Refresh Token support.
Authentication-specific security remains under features/auth/security/.
No separate Student, Teacher or Admin login identity is introduced.

| Responsibility | Actor/authority | Required boundary; deferred detail |
| --- | --- | --- |
| Student registration | Visitor seeking a Student account | Backend assigns STUDENT; validate approved account information and initiate applicable verification. Required fields and detailed initial transitions remain DEFERRED. |
| Teacher registration | Prospective Teacher | Separate registration assigns TEACHER through the backend; verified email, unlocked account and approved onboarding are required for business authority. Review-operation details remain DEFERRED. Client role input cannot grant TEACHER privilege. |
| Email verification | Eligible account-verification context | Purpose-specific, account-bound, expiring credential; consume according to approved flow. Verification cannot override administrative restrictions or define Teacher activation. |
| Resend verification | Eligible verification context | Eligibility, expiry, resend limits and delivery contract remain DEFERRED; avoid unnecessary account-existence disclosure. |
| Login | Student, Teacher or Admin | Validate credentials and current eligibility; issue only permitted authentication. Transport and detailed pending-account behavior remain DEFERRED. |
| Refresh | Holder of usable refresh authority | Validate server-side expiry, revocation and current account/session eligibility; do not require an unexpired Access Token as proof of refresh eligibility. |
| Logout | Applicable authenticated/session context | Invalidate applicable refresh/session authority and clear applicable client state; frontend deletion alone is insufficient. Session selection and invalid-state handling remain DEFERRED. |
| Change password | Authenticated eligible User | Dedicated security operation; satisfy approved checks and protect credentials. Revoke other refresh sessions while preserving the current one; password policy remains DEFERRED. |
| Forgot password | Visitor/account holder unable to authenticate | Neutral recovery response; no password change here and no claim of delivery after failure. Delivery/security details remain DEFERRED. |
| Reset password | Valid reset-credential holder | Validate purpose, account, expiry and usability; successful consumption prevents reuse. Revoke all refresh sessions on successful reset. |
| Current account/profile | Authenticated eligible User | Subject comes from backend identity. Return only permitted information; current-account viewing does not grant Admin self-profile editing. |
| Own-profile change | Eligible Student or Teacher | Only permitted own fields; dedicated password security and protected privileges cannot be bypassed. Exact editable DTO remains DEFERRED; email change requires new-email verification before replacing the old email. |

RefreshSession is server-authoritative security state, distinct from its credential.
No API exposes its persistence rows or makes them ordinary CRUD resources.
Support approved rotation; replaced credentials are invalidated under that design.
Rotation timing, replay, concurrency and session limits remain unresolved.
Logout does not imply immediate revocation of every issued Access Token.

Retain configurable defaults: Access Token 15 minutes, Refresh Token 7 days and
password-reset credential 15 minutes. These are not hardcoded storage constraints.
Verification lifetime/resend policy is not selected.

Validate JWT signature and expiry with supported security facilities. Use supported
password protection; do not invent cryptography or select undocumented encoder
parameters. users.locked is the only blocking mechanism: locked Users cannot
authenticate or perform normal account use. No separate disabled/account_status lifecycle.

Credential delivery, cookie versus header/body transport, signing details,
CORS/CSRF and broader security policy remain DEFERRED. Do not default to long-lived
credentials in localStorage. If cookies are selected, address HttpOnly, Secure in
production, SameSite, CORS and CSRF together. Do not add social login or OAuth flows.
Admin accounts are not publicly self-registered; authorized administrative/system
setup does not establish a Teacher onboarding mechanism.

References: FR-STU-001, FR-STU-002, FR-STU-003, FR-TEA-001, FR-TEA-005,
FR-ADM-001, FR-ACC-001, FR-ACC-002, FR-ACC-003, FR-ACC-004, FR-ACC-005,
FR-ACC-006, FR-ACC-007, FR-ACC-008, FR-ACC-009, FR-ACC-010, FR-ACC-011,
FR-ACC-012, FR-ACC-013, FR-ACC-014, FR-ACC-015, FR-ACC-016;
BR-AUTHN-002, BR-AUTHN-006, BR-AUTHN-007, BR-AUTHN-008, BR-AUTHN-009,
BR-AUTHN-010, BR-AUTHN-011, BR-AUTHN-012, BR-AUTHN-013, BR-AUTHN-014,
BR-AUTHN-015, BR-AUTHN-016, BR-AUTHN-017, BR-AUTHN-018, BR-AUTHN-019,
BR-AUTHN-020, BR-AUTHN-023, BR-AUTHN-024, BR-PROFILE-002;
NFR-SEC-001, NFR-SEC-007, NFR-SEC-008, NFR-SEC-009, NFR-SEC-010,
NFR-SEC-011, NFR-SEC-012, NFR-SEC-013, NFR-SEC-014, NFR-SEC-015, NFR-SEC-017.

## 4. Authorization and Resource Ownership

**APPROVED RESPONSIBILITY:** Before protected disclosure or mutation, Spring Boot
checks authenticated identity, current account eligibility, required role,
resource relationship and permission for the specific operation.

STUDENT, TEACHER and ADMIN are authorization roles. Enrollment/payment state is
not a role. Students cannot use Teacher/Admin management; Teachers cannot use
Admin-only functions. Role checks alone are insufficient.

Required relationship resolution:

~~~text
Course -> owning Teacher
Session -> Course -> owning Teacher
Assignment -> Session -> Course -> owning Teacher
Submission -> Assignment -> Session -> Course -> owning Teacher
Submission -> submitting Student
PaymentReceivingInformation -> owning Teacher
PaymentTransaction -> Course -> owning Teacher
~~~

Verify actual parentage, including agreement between any supplied parent and child
identifiers. Owning one Course grants no access to another Teacher's resources.
Apply these boundaries to lists, details, changes, enrolled-Student teaching
information, schedules, submissions/results, progress and transactions.

For personal actions, backend authentication establishes the subject. A supplied
teacherId, ownerId or studentId is never authority to act as that person.
A resource identifier can select a lookup target, but cannot establish permission.
Course creation establishes its owner from authenticated Teacher authority.

JWT role claims do not establish current ownership, account eligibility,
Enrollment validity, payment success or protected participation.
Resolve current backend state before the protected operation; frontend route
guards, hidden controls and previous list responses do not replace these checks.

Student private-work access requires submitting identity plus applicable
participation permission. Course ownership permits appropriate Teacher inspection,
not authorship transfer or unrestricted grading. Admin must have the specific
approved permission; no generic ownership, grading or payment override follows.

References: FR-AUTH-001, FR-AUTH-002, FR-AUTH-003, FR-AUTH-004, FR-AUTH-006,
FR-AUTH-007, FR-AUTH-008, FR-AUTH-009, FR-AUTH-011, FR-AUTH-012, FR-AUTH-013;
BR-ROLE-001, BR-AUTH-001, BR-AUTH-002, BR-AUTH-003, BR-ASN-003;
INV-001, INV-002, INV-018, INV-023; AR-006, AR-015, AR-018.

## 5. Public Discovery

**APPROVED RESPONSIBILITY:** Student Teacher/Course search and discovery, Teacher
public teaching profile and Course public detail. Visitor access is limited to
permitted public discovery; exact anonymous availability and fields remain gated.

Public Teacher information follows specialization, teaching experience and
introduction direction. Public Course information follows name, description,
owning Teacher, tuition, permitted schedule information and Session count where
applicable. Configurable Course capacity is not automatically a public field.

Public projections exclude private receiving information, protected Meet URLs,
Session content, participation-restricted Assignments, private Submissions/results,
Student/Enrollment details, protected transactions/evidence and security state.
Do not serialize a protected management projection and rely on UI hiding.

**DEFERRED:** Exact routes, fields, search matching, filters, sorting and Course
discoverability/publication conditions. Discovery performs no purchase or
Enrollment mutation and grants no protected participation.

References: FR-DIS-003, FR-DIS-004, FR-DIS-005, FR-DIS-006, FR-AUTH-012;
BR-AUTH-007, BR-ENR-007; INV-023.

## 6. Teacher Profile

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

V1 uses users.locked as its account-blocking mechanism: a locked account cannot authenticate
or use normal account functionality. There is no separate disabled, enabled or
account_status field/lifecycle. Email uses lowercase(trim(inputEmail)) for storage/login; a verified email
change retains the old email until successful verification of the new one. Password reset
revokes all refresh sessions; logged-in password change revokes other sessions while
preserving the current session. Raw refresh, verification and reset secrets are not
persisted.

Public teaching profile is separate from immutable application review snapshots.
Avatar files use Supabase Storage paths; profile DTOs cannot bind review/role/lock
privileges. Exact editable/public DTO validation and upload delivery remain DEFERRED.

References: FR-STU-004, FR-STU-005, FR-TEA-002, FR-TEA-003, FR-DIS-004;
BR-PROFILE-002, BR-AUTHN-012, BR-AUTH-007; DM-TEA-001.


## 7. Course

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots.

Teachers retain authorized own-Course content management. Public, Teacher and Admin
DTOs have separate visibility purposes. Backend resolves owner; requests cannot
transfer it. Detailed create/update validation, publication and
completion prerequisites and Meet supply timing remain DEFERRED.

References: FR-TCR-001, FR-TCR-003, FR-TCR-004, FR-TCR-005, FR-TCR-006,
FR-TCR-007, FR-TCR-008, FR-TCR-009;
BR-COURSE-001, BR-COURSE-002, BR-COURSE-003, BR-COURSE-004, BR-COURSE-007;
DM-COURSE-001, INV-002; AR-015.


## 8. Category / Topic

Each Course selects exactly one Admin-managed Category. Categories use active/inactive;
a used Category is retained and deactivated, not deleted. No hierarchy or automatic
Course reassignment. Exact management DTOs/validation remain DEFERRED.

References: FR-ADM-007, FR-TCR-008; BR-ADM-001; DM-CAT-001.


## 9. Session and Scheduling

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

Teacher ownership traverses Course; Student content reads require Enrollment.
Public preview must omit nullable sessions.content and the protected Meet URL.
Weekly rules/local timezone and generated absolute Session times are distinct.
Exact scheduling payloads, DST gap/overlap and cutoff validation remain DEFERRED;
ISO weekday generation, fixed unique numbering and cancellation without replacement
are settled. No attendance API or RESCHEDULED status.

References: FR-SES-001, FR-SES-002, FR-SES-003, FR-SES-004,
FR-SCH-001, FR-SCH-002; BR-SES-001, BR-SES-002, BR-SCH-001;
DM-SES-001, DM-SCH-001, INV-017, INV-020.


## 10. Assignment

Assignments belong to Sessions; ownership traverses Course. ACTIVE/CANCELLED are
the only statuses. Refuse hard deletion once any Submission exists. Authorized
Students view work through Enrollment. Files use Supabase Storage references.
Required due_at and NUMERIC(5,2) score bounds are approved. Exact DTOs,
late/deadline operation rules and upload validation remain DEFERRED. No generic assessment/automatic-scoring engine.

References: FR-ASN-001, FR-ASN-002, FR-ASN-003, FR-SES-004;
BR-ASN-001, BR-ASN-002, BR-TEA-001; DM-ASN-001, INV-017.


## 11. Submission and Result

Assignments belong to Sessions and use ACTIVE/CANCELLED. Any Submission prevents hard
deletion of its Assignment. There is one current Submission per Student/Assignment, using
DRAFT/SUBMITTED/GRADED; no revision-history, result or grading table is introduced. Score,
feedback and grading metadata remain on Submission. Only the owning Teacher grades, and a
score cannot exceed Assignment max_score. A numeric score is not made mandatory merely by
GRADED status.

SUBMITTED requires submitted_at; GRADED requires submitted_at, graded_at and grader.
Backend identity determines author/grader, never client-selected authority. Result
is a permitted Submission projection, not separate CRUD. Student own-work privacy
and nested Teacher ownership apply to lists/details. File bytes reside in Storage.
Exact retry/update/late-work DTOs and result visibility remain DEFERRED; a missing
score is not fabricated as zero.

References: FR-ASN-004, FR-ASN-005, FR-ASN-006, NFR-DATA-001;
BR-ASN-001, BR-ASN-002, BR-ASN-003, BR-TEA-003;
DM-ASN-002, DM-ASN-003, INV-018; AR-015, AR-016.


## 12. Enrollment and Participation

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.

Enrollment is distinct from payment. Backend participation and personal-data checks
protect content/work/Meet; listing My Courses or browser success grants no access.
Exact operation DTOs, reservation duration and edge-transition rules remain DEFERRED.

References: FR-ENR-004, FR-ENR-005, FR-ENR-006, FR-ENR-007, FR-ENR-008,
FR-TCR-010, FR-ADM-008, FR-PAY-012; BR-ENR-001, BR-ENR-006, BR-ENR-007,
BR-ENR-008, BR-COURSE-007, BR-TEA-003; DM-ENR-001, INV-013, INV-014; AR-018.


## 13. Course Progress

Student own progress and Teacher owned-Course views derive from existing authoritative
Enrollment/Session/Submission evidence. No progress/statistics/attendance resource
CRUD or table. Formula, response indicators and reporting boundaries remain DEFERRED.
Viewing content/Meet does not establish attendance or completion.

References: FR-PRO-004, FR-PRO-009, FR-ENR-005, FR-TAN-002, FR-TAN-003;
BR-AUTH-006, BR-TEA-002, BR-TEA-003; DM-PRO-002, INV-023, INV-024.


## 14. Payment Receiving Information

Students pay the owning Teacher directly using the VietQR integration direction; the
platform/Admin does not hold tuition or perform payouts. Payments are separate from
Enrollments, have immutable price snapshots and use PENDING, CONFIRMED, EXPIRED or
CANCELLED. An Enrollment may have historical attempts but at most one PENDING Payment.
Teacher bank-account history is retained with at most one ACTIVE account; existing Payments
keep their historical account reference.

Receiving bank information is private, never public Teacher/Course discovery.
Resolve it through Course ownership and preserve historical references.
provider_account_ref is omitted from Physical V1 and is not an API contract.
Exact verification, input/projection and integration contract remain DEFERRED.

References: FR-PAY-007, FR-PAY-008, FR-AUTH-012, FR-AUTH-013;
BR-PAY-006, BR-PAY-007, BR-AUTH-007; DM-PAY-002, INV-015; AR-019.


## 15. Course Payment and Transaction Boundary

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

Payment attempts and actual bank transactions are distinct. An unmatched transaction
may retain a known receiver independently, or remain receiver-unresolved; amount
alone never matches it. CONFIRMED requires timestamp evidence, not a success claim.
Student, owning Teacher and Admin projections expose only permitted information.
No raw bank/provider payload, hash or secret is returned.

Refund is a separately authorized workflow: Teacher submits transfer proof, Admin
verifies completion. SUBMITTED/COMPLETED metadata follows DATABASE_DESIGN.md; no
automatic Enrollment change or partial-refund API is invented.

Provider-specific initiation/confirmation and adapter-context durable retry identity
remain DEFERRED; Physical V1 omits provider_order_id and unverified global transaction-ID uniqueness. No final method/path/provider
DTO is introduced merely to match tables. Read operations never mutate payment.

Additional references: FR-PAY-013, BR-PAY-011, UC-PAY-REFUND-01, DM-PAY-003.

References: FR-PAY-002, FR-PAY-008, FR-PAY-009, FR-PAY-010, FR-PAY-011,
FR-PAY-012, FR-ENR-008, NFR-DATA-007;
BR-PAY-002, BR-PAY-003, BR-PAY-007, BR-PAY-008, BR-PAY-009, BR-PAY-010,
BR-ENR-008, BR-ADM-005; DM-PAY-001, INV-010, INV-014, INV-016;
AR-019, AR-020.


## 16. Google Meet and Google Calendar Boundaries

One protected manually supplied Course Meet URL is shared by all Sessions.
Only authorized owner/participants receive it; no Meet API, meeting entity or
attendance claim. Supply/validation timing remains open.

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

Exact Calendar API-facing application contracts and execution/retry details remain
DEFERRED. SYNCED evidence requires external event identifier and last-synced time.
Do not expose organizational credentials or accept attendee identities as authority.

References: FR-MEET-001, FR-MEET-002, FR-MEET-003, INT-CAL-001;
BR-MEET-001, BR-MEET-002, BR-AUTH-005;
INV-019, INV-020, INV-021; AR-021.


## 17. Participant Feedback

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.

Backend resolves authenticated Student and COMPLETED Enrollment before acceptance.
Exact request/response, edit/delete and moderation contracts remain DEFERRED.

References: FR-RATE-001, FR-AUTH-013; BR-RATE-001, BR-AUTH-001;
DM-RATE-001, INV-023.


## 18. Admin and Statistics

**APPROVED RESPONSIBILITY:** Specific authorized administrative operations, not
unrestricted CRUD or a general permission-management engine.

| Area | Approved responsibility | Contract boundary |
| --- | --- | --- |
| Users/accounts | Inspect permitted Student/Teacher information; authorized management including lock/unlock where approved. | Exact fields/actions, transitions and overrides remain deferred; no arbitrary role assignment or Teacher-onboarding bypass. |
| Courses | Inspect/oversee Courses across Teachers. | No automatic ownership, teaching-content mutation or grading authority. |
| Categories/topics | Manage permitted teaching categories/topics. | Exactly one Category per Course; retain/deactivate used Categories. Exact DTO validation remains deferred. |
| Enrollment | Monitor permitted Course participation information. | No arbitrary activation, cancellation or payment override. |
| Transactions | Inspect appropriate Course transaction/status/timing information. | Monitoring grants no payment-confirmation/custody authority; full-refund completion verification is separately approved under BR-PAY-011. |
| Statistics | User, Student, Teacher and Course totals; appropriate Enrollment/transaction statistics. | Preserve upstream SHALL/SHOULD strength; exact measures, populations, time boundaries and outcome mappings remain deferred. |

Teacher tuition must not be labelled platform/Admin revenue. Prefer appropriately
derived views of authoritative data; no reporting warehouse, materialized reporting
tables or financial/expense subsystem is introduced.
Unresolved measures cannot be fabricated as zero or invented formulas.
An Admin detail projection does not grant mutation of everything it can display.
Teacher inspection includes only approved account/profile and associated Course
information; Teacher identity remains the shared User role context.

References: FR-ADM-002, FR-ADM-003, FR-ADM-004, FR-ADM-006, FR-ADM-007,
FR-ADM-008, FR-ACR-001, FR-ACR-002, FR-ATR-001, FR-ATR-002, FR-ATR-003,
FR-AAN-001, FR-AAN-002, FR-AAN-003, FR-AAN-004, FR-AAN-005, FR-AAN-008;
BR-ADM-001, BR-ADM-002, BR-ADM-005; INV-022.

## 19. Request / Response and Error Conventions

**APPROVED DIRECTION:** JSON, explicit DTOs, backend validation, allowlisted inputs,
structured safe errors, appropriate ISO-8601 timestamps and exact monetary values.

**CANDIDATE CONTRACT:** Retain camelCase JSON keys and string identifiers as wire
conventions. If numeric identifiers exceed safe JavaScript integer precision,
encode them losslessly as decimal strings. BIGINT / Java Long is the approved database
key type for approved tables; TeacherProfile intentionally shares its User key. Validate
identifiers according to the eventually selected
contract, not arbitrary numeric coercion.

Represent money losslessly as decimal strings with relevant currency information.
NUMERIC(15,2)/BigDecimal, VND-only currency and historical price snapshots are approved.
Discounts require a complete window, 0 < percent < 100 and start < end; zero tuition
represents free Courses. Rounding remains an implementation contract; no conversion is added.
Absolute timestamps use ISO-8601 with an explicit offset. Weekly local times use
Course timezone; exact wire format, DST and reporting boundaries remain deferred.

Each finalized request must list permitted fields, types, requiredness, null/omission
semantics and validation. Reject unknown/protected fields rather than binding an
entity. Do not infer current profile lengths or PATCH null rules from a prior DTO.
Ordinary requests cannot mass-assign role, ownership, submitting identity, account
restrictions, authoritative payment amount/recipient/destination/outcome or
participation rights. A Teacher's authorized Course tuition edit is distinct from
a Student supplying an authoritative payment amount.

Responses must be purpose-specific projections. Do not include credential hashes,
refresh/session persistence, signing secrets, verification/reset credentials,
provider secrets or raw sensitive payment payloads in ordinary DTOs.
Dedicated credential delivery remains governed by the deferred security transport;
this exclusion does not remove authentication/recovery responsibilities.

A compatible **CANDIDATE CONTRACT** for structured errors is:

~~~text
{ code: string, message: string,
  fieldErrors?: [{ field: string, code: string, message: string }] }
~~~

Messages and field errors must not echo secrets or private values. Authentication
recovery responses remain neutral; provider delivery failure cannot expose a
credential or imply successful delivery. Do not invent feature-specific error
codes for unresolved state machines.

| HTTP status | General meaning; operation-specific mapping still requires a contract |
| --- | --- |
| 400 | Malformed request or input validation failure. |
| 401 | Required authentication absent, invalid or expired. |
| 403 | Authenticated but unauthorized, where disclosing that distinction is safe. |
| 404 | Resource unavailable or intentionally not disclosed under a consistent privacy policy. |
| 409 | A justified business conflict under an approved rule; not a substitute for missing policy. |

GET is read-only: no implicit payment confirmation, participation activation or
result mutation. Use creation/update/deletion success semantics appropriate to
the eventual operation; no specific operation's success status is frozen here.
POST is not automatically retry-safe; PUT/PATCH/DELETE selection must match an
approved resource identity and effect. Do not impose retry keys, duplicate rules
or physical uniqueness before dependent policy is resolved. A timeout alone does
not establish failure or permission to repeat a financial mutation.

References: NFR-SEC-004, NFR-SEC-005, NFR-SEC-013, NFR-MNT-010;
BR-AUTHN-014, BR-PROFILE-002, BR-PAY-009; AR-003, AR-006.

## 20. Pagination, Filtering and Sorting

**APPROVED DIRECTION:** Bound collection queries, validate allowlisted filters
and sorting, and use deterministic order where required. Query scoping must enforce
authorization before returning data or totals. An aggregate count can also leak
private information and requires the same appropriate scope.

Relevant areas include Teacher/Course discovery, Teacher-owned Courses and Student
lists, My Courses, Sessions, Assignments, Submissions, related transactions,
Admin users/Courses/Enrollments and permitted statistics.

**DEFERRED:** Exact parameter names, matching, page/cursor choice, defaults, limits,
sort fields/tie-breakers and report time filters. Do not copy obsolete filters or
choose limits merely to complete a table. Bounded lists must not expand into
unlimited nested entity graphs. No pagination method promises a concurrent snapshot
without a justified, approved consistency contract.

References: NFR-PERF-004, NFR-SEC-004; BR-AUTH-001, BR-AUTH-007, BR-PAY-010.

## 21. Security and Privacy

The following disclosure boundaries apply to every later contract, including errors,
list totals, frontend caches and logs:

| Information | Permitted scope |
| --- | --- |
| Public Teacher/Course information | Explicit approved discovery projection only. |
| Account/security information | Current or specifically authorized account operation; no hashes/secrets or supporting security persistence. |
| Teacher receiving information | Private owner-authorized configuration and only justified protected payment context. |
| Course Meet URL | Authorized Course management or required Student participation; never public discovery. |
| Submission/AssignmentResult | Submitting Student's permitted information and appropriate owned-Course Teacher inspection. |
| PaymentTransaction/evidence | Related Student, owning Teacher or specifically authorized Admin information; visibility does not expose all evidence. |
| CourseProgress/schedules | Personal/ownership/participation checks applicable to the operation. |

Authentication is not sufficient authorization. Recheck eligibility, role,
ownership, participation and action permission before protected effects.
Do not trust frontend state or JWT claims as authoritative mutable business state.
Reject privilege escalation and mismatched parent/child identity.

Use supported password/security mechanisms and HTTPS in deployed environments.
Keep credentials out of source control, ordinary DTOs and logs. No unnecessary
Meet URL, receiving-information or sensitive payment-evidence logging is justified.
Supabase RLS is not application authorization.

No upload API, multipart contract, presigned URL, image proxy/download, S3,
Cloudinary or Supabase Storage integration is approved. Assignment/Submission
files, avatars and Course media remain subject to storage decisions.

Preserve required learning, security and transaction information under current
rules. Exact retention/archive/delete behavior remains deferred; no blanket
immutability, global soft delete or cascade policy is selected.

Later verification should test cross-Teacher access denial, Student work privacy,
forged identity/payment/participation claims, protected Meet and receiving-data
disclosure, limited Admin authority and credential misuse. Tests must use approved
policies; this documentation rewrite creates/runs no application tests.

References: NFR-SEC-002, NFR-SEC-003, NFR-SEC-005, NFR-SEC-010,
NFR-SEC-013, NFR-SEC-016, NFR-SEC-017, NFR-DATA-001, NFR-DATA-006,
NFR-DATA-007, NFR-MNT-003;
BR-AUTH-001, BR-AUTH-002, BR-AUTH-007, BR-ASN-003, BR-TEA-003, BR-PAY-010;
INV-018, INV-023, INV-024; AR-008, AR-016.

## 22. Deferred API Decisions

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

Exact method/route/DTO/error/pagination contracts remain design work where not approved.
No endpoint catalogue is finalized by these physical decisions. No raw arbitrary JSON
provider continuation object, infrastructure or additional schema is inferred.


## 23. Retired Legacy API

**Historical migration register only.** The following records the approved audit
of all 72 former operations under the historical /api/v1 prefix. It is not a
current API catalogue, compatibility promise, redirect plan or implementation list.
Historical numbers identify only their former catalogue positions.

| Audit disposition | Count | Meaning |
| --- | ---: | --- |
| Reconcile | 32 | A compatible capability remains, but the former method/path/DTO/policy is not approved unchanged. |
| Retire | 39 | Former operation withdrawn from active design; no automatic replacement. |
| Defer | 1 | Former Course archive contract remains inactive pending current policy. |
| Preserve unchanged | 0 | No complete old operation contract is carried forward unchanged. |
| Mechanically replace | 0 | No endpoint/domain renaming defines a new tutoring API. |
| Total | 72 | Every former method/path pair appears once below. |

Retired scope includes Vocabulary, vocabulary senses, CEFR hierarchy, Dictionary
integration, vocabulary pronunciation/audio, vocabulary Lessons, Fill Word,
Listening exercises, Exercise, Quiz, ExerciseAttempt, AnswerRecord,
SavedVocabulary, vocabulary Review/practice, mastery/weak vocabulary,
STANDARD/PREMIUM tiers/classification, Premium gating, SubscriptionPlan,
Subscription entitlement/renewal/expiry and subscription revenue.

Lesson is not renamed Session; Exercise/Quiz is not renamed Assignment; Attempt
is not renamed Submission; Subscription is not renamed Enrollment; vocabulary
Review is not renamed ParticipantFeedback. New tutoring APIs derive independently
from current requirements. Retired concepts provide no active dependency.

Old FINAL/PROVISIONAL labels, the 42/30 split, the 18-table compatibility claim,
fixed fullName/avatarUrl rules, DRAFT/STANDARD initialization, immutable attempt
snapshots, scoring formulas and provider-to-subscription-grant atomicity are
historical only. None settles current fields, policies or readiness.

| Old # | Historical method/path (relative to /api/v1) | Disposition | Reason |
| --- | --- | --- | --- |
| 1 | POST /auth/register | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 2 | POST /auth/email-verification | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 3 | POST /auth/email-verification/resend | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 4 | POST /auth/login | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 5 | POST /auth/refresh | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 6 | POST /auth/logout | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 7 | POST /auth/password/change | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 8 | POST /auth/password/forgot | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 9 | POST /auth/password/reset | Reconcile | Authentication responsibility retained; common/account/security contract requires reconciliation. |
| 10 | GET /me | Reconcile | Own-account/profile responsibility retained; old fields and entitlement projection withdrawn. |
| 11 | PATCH /me/profile | Reconcile | Own-account/profile responsibility retained; old fields and entitlement projection withdrawn. |
| 12 | GET /courses | Reconcile | Discovery retained; old CEFR, Lesson and tier projection withdrawn. |
| 13 | GET /courses/{courseId} | Reconcile | Discovery retained; old CEFR, Lesson and tier projection withdrawn. |
| 14 | GET /courses/{courseId}/lessons | Retire | Vocabulary Lesson learning is retired; no Session route equivalence. |
| 15 | GET /lessons/{lessonId} | Retire | Vocabulary Lesson learning is retired; no Session route equivalence. |
| 16 | GET /lessons/{lessonId}/vocabulary | Retire | Vocabulary Lesson learning is retired; no Session route equivalence. |
| 17 | GET /lessons/{lessonId}/exercises | Retire | Vocabulary Lesson learning is retired; no Session route equivalence. |
| 18 | POST /teacher/courses | Reconcile | Owned Course capability retained; fixed lifecycle/classification/fields withdrawn. |
| 19 | GET /teacher/courses | Reconcile | Owned Course capability retained; fixed lifecycle/classification/fields withdrawn. |
| 20 | GET /teacher/courses/{courseId} | Reconcile | Owned Course capability retained; fixed lifecycle/classification/fields withdrawn. |
| 21 | PATCH /teacher/courses/{courseId} | Reconcile | Owned Course capability retained; fixed lifecycle/classification/fields withdrawn. |
| 22 | POST /teacher/courses/{courseId}/publish | Reconcile | Owned Course capability retained; fixed lifecycle/classification/fields withdrawn. |
| 23 | POST /teacher/courses/{courseId}/archive | Defer | Archive authority, lifecycle and participation effects remain unresolved. |
| 24 | GET /teacher/courses/{courseId}/lessons | Retire | Lesson management retired; Session management is independently justified. |
| 25 | POST /teacher/courses/{courseId}/lessons | Retire | Lesson management retired; Session management is independently justified. |
| 26 | PATCH /teacher/lessons/{lessonId} | Retire | Lesson management retired; Session management is independently justified. |
| 27 | GET /vocabulary | Retire | Vocabulary/sense/audio and Dictionary search/import retired. |
| 28 | GET /vocabulary/{vocabularyId} | Retire | Vocabulary/sense/audio and Dictionary search/import retired. |
| 29 | GET /teacher/dictionary-candidates | Retire | Vocabulary/sense/audio and Dictionary search/import retired. |
| 30 | POST /teacher/vocabulary/imports | Retire | Vocabulary/sense/audio and Dictionary search/import retired. |
| 31 | GET /teacher/lessons/{lessonId}/vocabulary | Retire | Lesson-vocabulary association retired; not a tutoring Assignment. |
| 32 | PUT /teacher/lessons/{lessonId}/vocabulary/{senseId} | Retire | Lesson-vocabulary association retired; not a tutoring Assignment. |
| 33 | DELETE /teacher/lessons/{lessonId}/vocabulary/{senseId} | Retire | Lesson-vocabulary association retired; not a tutoring Assignment. |
| 34 | GET /teacher/lessons/{lessonId}/exercises | Retire | Exercise/question engine retired; not Assignment management. |
| 35 | POST /teacher/lessons/{lessonId}/exercises | Retire | Exercise/question engine retired; not Assignment management. |
| 36 | PATCH /teacher/exercises/{exerciseId} | Retire | Exercise/question engine retired; not Assignment management. |
| 37 | GET /teacher/exercises/{exerciseId}/questions | Retire | Exercise/question engine retired; not Assignment management. |
| 38 | POST /teacher/exercises/{exerciseId}/questions | Retire | Exercise/question engine retired; not Assignment management. |
| 39 | PATCH /teacher/questions/{questionId} | Retire | Exercise/question engine retired; not Assignment management. |
| 40 | PUT /me/courses/{courseId}/enrollment | Reconcile | Participation retained; natural-key PUT, lifetime uniqueness and Premium eligibility withdrawn. |
| 41 | GET /me/courses | Reconcile | Own-Course listing retained; old tier/expiry semantics withdrawn. |
| 42 | POST /me/attempts | Retire | Attempt/AnswerRecord practice, scoring and history retired; not Submission. |
| 43 | GET /me/attempts/{attemptId} | Retire | Attempt/AnswerRecord practice, scoring and history retired; not Submission. |
| 44 | PUT /me/attempts/{attemptId}/answers/{position} | Retire | Attempt/AnswerRecord practice, scoring and history retired; not Submission. |
| 45 | POST /me/attempts/{attemptId}/completion | Retire | Attempt/AnswerRecord practice, scoring and history retired; not Submission. |
| 46 | GET /me/attempts/{attemptId}/answers | Retire | Attempt/AnswerRecord practice, scoring and history retired; not Submission. |
| 47 | GET /me/attempts | Retire | Attempt/AnswerRecord practice, scoring and history retired; not Submission. |
| 48 | GET /me/courses/{courseId}/progress | Reconcile | CourseProgress capability retained; Lesson formulas/history assumptions withdrawn. |
| 49 | GET /me/vocabulary-performance | Retire | Vocabulary mastery, weak/saved vocabulary responsibilities retired. |
| 50 | GET /me/saved-vocabulary | Retire | Vocabulary mastery, weak/saved vocabulary responsibilities retired. |
| 51 | PUT /me/saved-vocabulary/{vocabularyId} | Retire | Vocabulary mastery, weak/saved vocabulary responsibilities retired. |
| 52 | DELETE /me/saved-vocabulary/{vocabularyId} | Retire | Vocabulary mastery, weak/saved vocabulary responsibilities retired. |
| 53 | GET /subscription-plans | Retire | SubscriptionPlan/Premium entitlement and Subscription periods retired. |
| 54 | GET /me/entitlement | Retire | SubscriptionPlan/Premium entitlement and Subscription periods retired. |
| 55 | GET /me/subscriptions | Retire | SubscriptionPlan/Premium entitlement and Subscription periods retired. |
| 56 | POST /me/payments | Retire | Subscription plan purchase retired; Course payment needs independent contract. |
| 57 | GET /me/payments | Reconcile | Related transaction visibility retained; plan/provider/outcome contract withdrawn. |
| 58 | GET /me/payments/{paymentId} | Reconcile | Related transaction visibility retained; plan/provider/outcome contract withdrawn. |
| 59 | GET /teacher/courses/{courseId}/analytics | Reconcile | Owned teaching visibility retained; vocabulary/score metrics withdrawn. |
| 60 | GET /admin/users | Reconcile | Limited account inspection retained; old profile/projection assumptions withdrawn. |
| 61 | GET /admin/users/{userId} | Reconcile | Limited account inspection retained; old profile/projection assumptions withdrawn. |
| 62 | POST /admin/teachers | Retire | Mandatory Admin-only Teacher provisioning model superseded; no automatic replacement flow. |
| 63 | POST /admin/users/{userId}/account-actions | Reconcile | Authorized account management retained; actions/transitions remain gated. |
| 64 | GET /admin/courses | Reconcile | Course oversight retained; tier/CEFR assumptions withdrawn. |
| 65 | GET /admin/courses/{courseId} | Reconcile | Course oversight retained; tier/CEFR assumptions withdrawn. |
| 66 | PUT /admin/courses/{courseId}/access-classification | Retire | STANDARD/PREMIUM classification retired. |
| 67 | GET /admin/subscriptions | Retire | Subscription administration retired; not Enrollment monitoring. |
| 68 | GET /admin/payments | Reconcile | Course transaction monitoring retained; subscription grants/linkage withdrawn. |
| 69 | GET /admin/payments/{paymentId} | Reconcile | Course transaction monitoring retained; subscription grants/linkage withdrawn. |
| 70 | GET /admin/dashboard | Reconcile | Operational statistics retained; Premium and subscription-revenue measures withdrawn. |
| 71 | GET /admin/analytics/subscriptions | Retire | Subscription/renewal/platform-revenue analytics retired. |
| 72 | GET /admin/analytics/revenue | Retire | Subscription/renewal/platform-revenue analytics retired. |

## 24. Traceability and Downstream Boundaries

### Complete current domain coverage

The following covers all 19 active concepts. A responsibility mapping does not
require one endpoint family or table per concept. References use current meanings.

| Active concept | API relevance and document section |
| --- | --- |
| DM-USER-001 — User | Shared account/current identity and permitted profile capability; Sections 3 and 6. |
| DM-USER-002 — Account Eligibility | Supporting eligibility, not unrestricted state CRUD; Sections 3-4. |
| DM-AUTH-001 — RefreshSession | Internal server-authoritative authentication support, no persistence exposure; Section 3. |
| DM-AUTH-002 — EmailVerification | Supporting purpose-bound credential interaction; Section 3. |
| DM-AUTH-003 — PasswordReset | Supporting recovery authority, not ordinary profile editing; Section 3. |
| DM-TEA-001 — TeacherProfile | Public teaching projection and own management; Sections 5-6. |
| DM-COURSE-001 — Course | Discovery and authorized owned-Course management; Sections 5 and 7. |
| DM-CAT-001 — CourseCategoryTopic | Admin-managed and Teacher-selectable information; Sections 8 and 18. |
| DM-SES-001 — Session | Nested Course occurrence/content capability; Section 9. |
| DM-SCH-001 — CourseSchedule | Course-associated scheduling direction; weekly rules/generated Sessions; Section 9. |
| DM-ASN-001 — Assignment | Nested Session work capability; Section 10. |
| DM-ASN-002 — Submission | Student-authored work and owned-Course inspection; Section 11. |
| DM-ASN-003 — AssignmentResult | Authorized result capability; Submission projection; Section 11. |
| DM-ENR-001 — Enrollment | Whole-Course participation, visibility and access checks; Section 12. |
| DM-PRO-002 — CourseProgress | Personal/owned-Course read capability; derived representation; formula deferred; Section 13. |
| DM-PAY-002 — PaymentReceivingInformation | Private Teacher-owned configuration and payment-context use; Section 14. |
| DM-PAY-001 — PaymentTransaction | Related transaction visibility and gated Course payment responsibility; Section 15. |
| DM-PAY-003 — Refund | Full Teacher refund with proof and Admin completion verification; Section 15. |
| DM-RATE-001 — ParticipantFeedback | Participation-related feedback; one completed-Enrollment row; editing/moderation deferred; Section 17. |

Meet remains protected Course information; Calendar is an external boundary.
Admin/statistics are permitted views/actions, not new identity or reporting entities.

### Complete active use-case coverage

All 44 active use cases map to responsibilities, not invented endpoint contracts.
The references in Sections 3-21 supply targeted governing FR/NFR/INT/BR/INV/AR
authority; this table supplies the actor-goal connection. Decision gates in the
source use cases remain applicable even where the capability is approved.

| Current use case | Responsibility section and remaining contract boundary |
| --- | --- |
| UC-AUTH-REGISTER-01 | 3: Student registration; required fields/transitions/security deferred. |
| UC-AUTH-REGISTER-TEACHER-01 | 3: Teacher registration; separate registration and approved onboarding; operation details deferred. |
| UC-AUTH-VERIFY-EMAIL-01 | 3: Verification and eligible resend; expiry/resend/transitions deferred. |
| UC-AUTH-LOGIN-01 | 3: Shared authentication with locked checks; transport details deferred. |
| UC-AUTH-REFRESH-01 | 3: Validated refresh; transport/replay/concurrency deferred. |
| UC-AUTH-LOGOUT-01 | 3: Applicable server-side invalidation; session/transport details deferred. |
| UC-AUTH-CHANGE-PASSWORD-01 | 3: Dedicated secure change; other-session revocation settled; password policy deferred. |
| UC-AUTH-FORGOT-PASSWORD-01 | 3: Neutral recovery; delivery/security details deferred. |
| UC-AUTH-RESET-PASSWORD-01 | 3: Single-use recovery authority; all-session revocation settled; password/delivery details deferred. |
| UC-AUTH-ME-01 | 3 and 6: Current identity/profile; permitted fields deferred. |
| UC-AUTH-PROFILE-01 | 6: Eligible own-profile management; exact field/update contract deferred. |
| UC-DIS-TEACHERS-01 | 5-6: Discovery/public teaching information; fields/matching deferred. |
| UC-STU-BROWSE-COURSES-01 | 5: Course discovery; filtering/visibility deferred. |
| UC-STU-VIEW-COURSE-01 | 5: Public Course projection; protected information excluded. |
| UC-TEA-COURSE-01 | 7: Owned creation/inspection/editing; lifecycle/field details deferred. |
| UC-TEA-PUBLISH-01 | 7: Owned publishing; eligibility/transitions deferred. |
| UC-TEA-STUDENTS-01 | 4 and 12: Owned-Course Student teaching information; exact actions/fields deferred. |
| UC-SES-MANAGE-01 | 9: Owned-Course Sessions/content and two-state lifecycle; validation edges deferred. |
| UC-SES-VIEW-01 | 9 and 12: Protected Session reads; participation/availability details deferred. |
| UC-SCH-COURSE-01 | 9: Weekly rules/generated Sessions; timezone/DST contract deferred. |
| UC-SCH-SESSION-01 | 9: Individual adjustment; rescheduling consequences deferred. |
| UC-ASN-MANAGE-01 | 10: Owned-Session work definition; format/management details deferred. |
| UC-ASN-VIEW-01 | 10 and 12: Authorized Assignment reads; availability/format deferred. |
| UC-ASN-SUBMIT-01 | 11: Own work submission; one current Submission; exact payload/late-work rules deferred. |
| UC-ASN-INSPECT-01 | 11: Owned-Course work/result inspection; owning Teacher grading, with required evidence. |
| UC-ASN-RESULT-01 | 11: Authorized own work/result viewing; result on Submission; availability details deferred. |
| UC-STU-ENROLL-01 | 12 and 15: Four-state unique Enrollment, capacity/cutoff; workflow edges deferred. |
| UC-STU-MY-COURSES-01 | 12-13: Own Course listing; not proof of protected participation. |
| UC-PAY-RECEIVING-01 | 14: Private own receiving configuration; historical bank accounts; provider field contracts deferred. |
| UC-PAY-COURSE-01 | 15: Direct-to-owner payment context and trust boundary; execution deferred. |
| UC-PAY-REFUND-01 | 15: Full refund, Teacher transfer/proof and Admin verification; exact operation contract deferred. |
| UC-PAY-VIEW-01 | 15: Related Student/Teacher transaction visibility; exact projections/provider mappings deferred. |
| UC-MEET-MANAGE-01 | 16: Manual shared Course URL; validation/supply/update rules deferred. |
| UC-MEET-ACCESS-01 | 16: Protected participation-authorized URL; no attendance claim. |
| UC-CAL-SCHEDULE-01 | 16: Organizational Calendar synchronization; concrete integration contract deferred. |
| UC-LEARN-PROGRESS-01 | 13: Own progress; calculation/projection deferred. |
| UC-TEA-ANALYTICS-01 | 13: Owned-Course learning/Enrollment monitoring; measures deferred. |
| UC-RATE-PARTICIPANT-01 | 17: Participation-related feedback; one COMPLETED-Enrollment rating 1..5; moderation deferred. |
| UC-ADM-USERS-01 | 18: Limited account/profile administration; action details deferred. |
| UC-ADM-COURSES-01 | 18: Course oversight; exact overrides deferred. |
| UC-ADM-CATEGORIES-01 | 8 and 18: Category/topic management; one Category, retain/deactivate used Categories; DTOs deferred. |
| UC-ADM-ENROLLMENTS-01 | 12 and 18: Enrollment monitoring; no alteration authority inferred. |
| UC-ADM-TRANSACTIONS-01 | 15 and 18: Transaction monitoring; no confirmation/custody inferred. |
| UC-ADM-ANALYTICS-01 | 18: Approved platform statistics; exact report definitions deferred. |

### Architecture, physical design and handoff limits

Active source definitions, not historical registers, establish reference validity.
No reference supplies an omitted policy or reactivates its former surrounding text.
Calendar traces to INT-CAL-001; nested ownership to INV-002 and AR-015;
participation to INV-013, INV-014 and AR-018; receiving/payment trust to
INV-015, INV-016, INV-010, AR-019 and AR-020; Meet/Calendar to INV-019,
INV-021 and AR-021. Physical/provider directions are specified upstream; concrete
provider protocols are not invented by these references.

Follow the 22-table DATABASE_DESIGN.md direction, retaining its explicit physical
clarifications. API DTOs cannot silently change keys, enums, uniqueness, deletion or
provider identifier scope. API methods and request identities must be revisited
if they would implicitly select a still-deferred physical/business invariant.

Before a dependent operation becomes implementation-ready, resolve its actual
policy gates and document method/path, actor/eligibility, ownership/participation,
explicit request/response projections, validation, error/success semantics, query
bounds and retry behavior where relevant. Do not use arbitrary JSON or permissive
status setters to disguise an unfinished contract.

TASK_BREAKDOWN.md, FEATURE_STATUS.md and README.md require separately approved
reconciliation where affected. Do not generate backend endpoints or frontend
clients from the historical register. Preserve TASK-001/TASK-002/TASK-003 evidence
and current implementation statuses; no business feature is complete merely
because this design is reconciled.

Follow the eleven-stage development order and remaining UI/UX/design gates.
No SQL, migrations, database connection, dependencies, tests, application code,
deployment configuration or next-stage work is authorized by this rewrite.
