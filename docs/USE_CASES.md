# English Tutoring Platform --- Use Cases

**Version:** 0.2
**Status:** Reconciled tutoring actor goals with explicit decision gates
**Project Type:** Graduation Project

---

# 1. Authority, Conventions, Actors and Traceability

Authority follows AGENTS.md -> PROJECT_SPEC.md -> REQUIREMENTS.md ->
BUSINESS_RULES.md -> USE_CASES.md. Later design documents do
not override these sources. This document describes actor/system interactions,
not detailed business policy, technical design or implementation readiness.

Project-local skills provide subordinate working guidance for applying the canonical
documentation. They do not override or redefine the canonical project documents.

STUDENT, TEACHER and ADMIN are authorization roles. Visitor is an unauthenticated
actor context; prospective Teacher describes a registration goal, not an assigned
role. The system is the application responding to the actor. Provider/bank evidence uses the
VietQR direction; Google Calendar uses a system/organization account. Google Meet is created
externally and is not an integrated application actor.

Sections 2-15 define active use cases. Each has targeted current requirement and
business-rule references; references never supply missing policy. Named decision
gates refer to section 16 and the authoritative source documents. Flows describe
approved high-level interactions, conditional on resolution of their stated gates.
A gated step is not permission to implement an invented default. Approved Calendar and
feedback behavior is described below; unresolved integration details remain gated.

## Shared Authorization and Denial Behavior

For every protected interaction, the system checks authenticated identity, the
required role, current account eligibility and operation permission before
protected disclosure or mutation. Missing/invalid authentication, wrong role,
applicable account restrictions or insufficient permission deny the protected
operation without exposing protected information or applying its requested change.
Stale client state does not override locked-account restrictions.

Teacher resource checks follow Course -> Teacher, Session -> Course -> Teacher,
Assignment -> Session -> Course -> Teacher and Submission -> Assignment -> Session
-> Course -> Teacher. These are authorization paths, not storage structures.
Teacher receiving information belongs to its Teacher; relevant transactions follow
the Course owner and the actor's visibility permission. Teacher A cannot manage
Teacher B's protected resources.

Student protected participation requires authoritative Enrollment where applicable.
Student identity and private submission/result access are checked separately.
Discovery and listing an Enrollment do not grant participation. Admin must have
the particular administrative permission; the role grants no general override.
Hiding frontend controls is not authorization. These shared denial paths apply
alongside the specific alternatives below and do not select error codes or screens.

**Requirement Traceability:** FR-AUTH-001, FR-AUTH-002, FR-AUTH-003, FR-AUTH-006,
FR-AUTH-007, FR-AUTH-008, FR-AUTH-009, FR-AUTH-011, FR-AUTH-012, FR-AUTH-013,
NFR-SEC-002, NFR-SEC-003, NFR-SEC-004, NFR-SEC-016.
**Business Rule Traceability:** BR-ROLE-001, BR-AUTH-001, BR-AUTH-002,
BR-AUTH-003, BR-AUTH-007, BR-AUTHN-017, BR-AUTHN-018, BR-AUTHN-019.

Read-only goals do not grant modification authority. Denials do not establish
new account, Course, payment or Enrollment states. No use case grants unapproved
retention/deletion behavior. Preserve completed TASK-001, TASK-002 and TASK-003
infrastructure and evidence; this document authorizes no implementation.

---
# 2. Shared Authentication and Account Use Cases

## UC-AUTH-REGISTER-01 --- Register Student

**Primary Actor:** Visitor

**Goal:** Register a Student through the separate Student flow.

**Trigger:** Visitor requests Student registration.

**Preconditions:** Visitor seeks a Student account and supplies canonicalizable email,
required full name and permitted credential information; detailed validation/security
controls remain gated.

**Main Success Flow:**

1. Canonicalize and validate permitted account information and protect the password.
2. Assign exactly STUDENT through the backend and initiate purpose-bound email verification.
3. Return the permitted outcome without granting Teacher authority.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** A Student account exists with canonical email, backend-assigned STUDENT
role and applicable verification state; registration grants no Teacher authority.

**Requirement Traceability:** `FR-STU-001`, `FR-ACC-008`, `FR-ACC-016`, `NFR-SEC-001`, `NFR-SEC-004`.

**Business Rule Traceability:** `BR-AUTHN-002`, `BR-AUTHN-006`, `BR-AUTHN-017`, `BR-AUTHN-020`.

**Decision Gates / Deferred Details:** Field validation, password
and verification delivery controls.

---

## UC-AUTH-REGISTER-TEACHER-01 --- Register as a Prospective Teacher

**Primary Actor:** Prospective Teacher (registration context)

**Goal:** Register a Teacher subject to verified, unlocked, approved onboarding.

**Trigger:** Prospective Teacher requests registration.

**Preconditions:** Applicant uses the separate Teacher registration flow and supplies
approved account/application information; a Student-to-Teacher promotion flow is not
available.

**Main Success Flow:**

1. Validate/canonicalize the separate Teacher registration and assign TEACHER through
   trusted backend logic; no Student promotion flow exists.
2. Retain a Teacher application snapshot, with at most one PENDING application; do not grant
   business authority from role alone.
3. Authorized review records approval/rejection, reviewer and time, plus rejection reason
   when rejected.
4. After approval establish the current public Teacher profile separately; business
   authority also requires verified email and unlocked account.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** A Teacher account/application exists under controlled onboarding;
business authority is granted only with verified email, unlocked account and approval.
Reviewed snapshots remain separate from the current profile.

**Requirement Traceability:** `FR-TEA-005`, `FR-ACC-016`, `FR-AUTH-009`, `NFR-SEC-004`.

**Business Rule Traceability:** `BR-AUTHN-024`, `BR-AUTHN-020`.

**Decision Gates / Deferred Details:** Review-operation permissions/details and required
validation; no invented identity-document workflow.

---

## UC-AUTH-VERIFY-EMAIL-01 --- Verify Email

**Primary Actor:** User (including initial verification or verified email-change context)

**Goal:** Verify an initial email or a requested new email.

**Trigger:** User supplies an initial-verification or email-change credential, or requests
an eligible resend.

**Preconditions:** Account is eligible for the approved verification interaction.

**Main Success Flow:**

1. Validate purpose, intended User, canonical target email, expiry and unused hashed credential.
2. Consume verification once; for EMAIL_CHANGE retain the old email until the verified
   replacement succeeds.
3. Update relevant future Calendar attendees after a verified change without overriding
   account locks or Teacher approval.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** The intended email is verified through single-use authority; a verified
replacement takes effect only after validation. Locks and onboarding approval are unchanged;
relevant future attendees require synchronization.

**Requirement Traceability:** `FR-ACC-008`, `FR-ACC-009`, `NFR-SEC-015`.

**Business Rule Traceability:** `BR-AUTHN-006`, `BR-AUTHN-017`.

**Decision Gates / Deferred Details:** Canonicalization details, resend/expiry controls and
Calendar delivery/retry contract.

---

## UC-AUTH-LOGIN-01 --- Login

**Primary Actor:** Student / Teacher / Admin

**Goal:** Establish authenticated access.

**Trigger:** Account holder submits login credentials.

**Preconditions:** An account exists; authentication eligibility is checked by the system.

**Main Success Flow:**

1. User supplies credentials.
2. System validates credentials and authoritative account eligibility.
3. System establishes authenticated identity and backend-derived role authority.
4. System issues the approved short-lived JWT Access Token and Refresh Token and returns permitted account information.

**Alternate / Exception / Denial Flows:** Invalid credentials or a locked account prevent
authentication and normal account use. Pending-verification handling remains limited to
approved account actions and does not bypass restrictions.

**Postconditions:** Successful authentication establishes only permitted authenticated access, not ownership or Course participation.

**Requirement Traceability:** `FR-STU-002`, `FR-TEA-001`, `FR-ADM-001`, `FR-ACC-001`, `FR-ACC-002`, `FR-ACC-003`, `FR-ACC-014`, `FR-ACC-015`, `FR-ACC-016`, `NFR-SEC-010`.

**Business Rule Traceability:** `BR-AUTHN-007`, `BR-AUTHN-008`, `BR-AUTHN-017`, `BR-AUTHN-018`, `BR-AUTHN-019`, `BR-AUTHN-020`.

**Decision Gates / Deferred Details:** Security and Accounts: transport and detailed
pending-account behavior; hashed supporting persistence follows DATABASE_DESIGN.md.

---

## UC-AUTH-REFRESH-01 --- Refresh Authentication

**Primary Actor:** Student / Teacher / Admin

**Goal:** Continue permitted authentication using a Refresh Token.

**Trigger:** User's client requests a new Access Token.

**Preconditions:** A Refresh Token is supplied for validation; an unexpired Access Token is not assumed.

**Main Success Flow:**

1. System receives the refresh request on the user's behalf.
2. System validates refresh credentials, revocation/expiration and current account/session eligibility.
3. System issues a new Access Token only after successful validation.
4. Where the approved security design replaces a Refresh Token, the system invalidates the replaced credential accordingly.

**Alternate / Exception / Denial Flows:** Missing, invalid, expired, revoked or otherwise unusable credentials, or ineligible account/session state, prevent new credentials. Client input cannot change the role.

**Postconditions:** New authentication credentials exist only following successful validation.

**Requirement Traceability:** `FR-ACC-003`, `FR-ACC-004`, `FR-ACC-005`, `FR-ACC-006`, `FR-ACC-016`, `NFR-SEC-009`.

**Business Rule Traceability:** `BR-AUTHN-008`, `BR-AUTHN-009`, `BR-AUTHN-010`, `BR-AUTHN-020`.

**Decision Gates / Deferred Details:** Security: rotation timing, transport, replay/concurrency and session limits.

---

## UC-AUTH-LOGOUT-01 --- Logout

**Primary Actor:** Student / Teacher / Admin

**Goal:** End the applicable authenticated session.

**Trigger:** User requests logout.

**Preconditions:** Authentication/session state exists to which logout may apply; its authority is checked.

**Main Success Flow:**

1. User requests termination of the applicable session.
2. System invalidates the applicable Refresh Token/session state under approved security rules.
3. Applicable client authentication state is cleared and the system reports the outcome.

**Alternate / Exception / Denial Flows:** Client-state deletion alone does not prove server-side logout. A request cannot terminate another user's session through an untrusted identity claim; handling of already-invalid state remains under the Security gate.

**Postconditions:** Successfully invalidated refresh/session state cannot be used to refresh. No immediate revocation of all issued Access Tokens is asserted.

**Requirement Traceability:** `FR-STU-003`, `FR-ACC-006`.

**Business Rule Traceability:** `BR-AUTHN-011`.

**Decision Gates / Deferred Details:** Security: transport and detailed session scope/invalid-state handling.

---

## UC-AUTH-CHANGE-PASSWORD-01 --- Change Password

**Primary Actor:** Student / Teacher / Admin

**Goal:** Change password and revoke other refresh sessions.

**Trigger:** User requests a password change.

**Preconditions:** User is authenticated and eligible for the operation.

**Main Success Flow:**

1. Resolve authenticated eligible User and current session.
2. Validate dedicated password checks and securely replace the password.
3. Revoke other refresh sessions while preserving the current session.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** The credential is securely changed, other refresh sessions revoked and
the current session preserved.

**Requirement Traceability:** `FR-ACC-010`, `NFR-SEC-001`, `NFR-SEC-013`.

**Business Rule Traceability:** `BR-AUTHN-013`.

**Decision Gates / Deferred Details:** Password policy and transport; approved session
invalidation is settled.

---

## UC-AUTH-FORGOT-PASSWORD-01 --- Request Password Recovery

**Primary Actor:** Visitor / account holder unable to authenticate

**Goal:** Initiate account recovery without unnecessary account-existence disclosure.

**Trigger:** Actor requests password recovery.

**Preconditions:** No authenticated session is required.

**Main Success Flow:**

1. Actor supplies the supported recovery identifier.
2. System handles the request without unnecessarily revealing account existence.
3. For an eligible account, the system prepares a purpose-specific recovery credential and makes recovery instructions available through the later-approved channel.
4. System returns a neutral response.

**Alternate / Exception / Denial Flows:** An absent or ineligible account does not cause unnecessary account-existence disclosure. Failure to provide recovery instructions must not expose credentials or claim successful delivery.

**Postconditions:** No password changes here; an eligible account may receive recovery instructions.

**Requirement Traceability:** `FR-ACC-011`, `FR-ACC-012`, `NFR-SEC-014`, `NFR-SEC-013`.

**Business Rule Traceability:** `BR-AUTHN-014`, `BR-AUTHN-015`.

**Decision Gates / Deferred Details:** Security: recovery delivery details and unresolved policy; no external provider is chosen.

---

## UC-AUTH-RESET-PASSWORD-01 --- Reset Password

**Primary Actor:** Visitor / account holder with a reset credential

**Goal:** Reset password through a single-use credential.

**Trigger:** Actor supplies a reset credential and new password.

**Preconditions:** A reset credential is available for validation.

**Main Success Flow:**

1. Validate intended User, purpose, expiry and unused hashed recovery credential.
2. Replace the password, consume the credential and revoke all refresh sessions.
3. Return a privacy-preserving outcome.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** The credential is securely reset, recovery credential consumed and all
refresh sessions revoked.

**Requirement Traceability:** `FR-ACC-012`, `FR-ACC-013`, `NFR-SEC-001`, `NFR-SEC-014`.

**Business Rule Traceability:** `BR-AUTHN-015`, `BR-AUTHN-016`.

**Decision Gates / Deferred Details:** Password/delivery controls; no raw secret persistence.

---

# 3. Personal and Teacher Profiles

## UC-AUTH-ME-01 --- View Current User and Own Profile

**Primary Actor:** Student / Teacher / Admin

**Goal:** Retrieve permitted information about one's authenticated account.

**Trigger:** User requests current-account information.

**Preconditions:** User has valid authentication and applicable account eligibility.

**Main Success Flow:**

1. System resolves identity from authentication, not a requested target user.
2. System obtains permitted account information and, for Student/Teacher profile viewing, the user's authorized profile information.
3. System returns only information the user may view; Teacher profile direction includes specialization, teaching experience and introduction.

**Alternate / Exception / Denial Flows:** Invalid authentication or account restrictions prevent protected disclosure. A client-selected user identifier cannot switch the subject.

**Postconditions:** Permitted own information is returned without modification; viewing does not grant profile-edit authority to Admin.

**Requirement Traceability:** `FR-ACC-007`, `FR-STU-004`, `FR-TEA-002`, `FR-AUTH-013`.

**Business Rule Traceability:** `BR-AUTHN-012`, `BR-PROFILE-002`.

**Decision Gates / Deferred Details:** Accounts: complete profile fields and visibility contract.

---

## UC-AUTH-PROFILE-01 --- Update Own Profile

**Primary Actor:** Student / Teacher

**Goal:** Update permitted own profile without changing authority.

**Trigger:** Student or Teacher requests a profile change.

**Preconditions:** User is authenticated and eligible to edit their own permitted information.

**Main Success Flow:**

1. Resolve the authenticated User and permitted fields.
2. Update authorized User/Teacher profile data, leaving Teacher application snapshots unchanged.
3. Use dedicated verified-new-email and password operations for security changes; store
   Teacher avatar as a Storage path, never a signed URL.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Only permitted own-profile values change; security authority and
retained application snapshots remain intact.

**Requirement Traceability:** `FR-STU-005`, `FR-TEA-003`, `FR-AUTH-013`, `NFR-SEC-004`.

**Business Rule Traceability:** `BR-PROFILE-002`, `BR-AUTHN-012`.

**Decision Gates / Deferred Details:** Exact editable-field DTO and file validation; no
role/lock/verification/onboarding binding.

---

## UC-CERT-OWN-01 --- Manage Own Certificates

**Actor:** Eligible authenticated Teacher.
**Goal:** Add/view/delete/replace certificates belonging to the Teacher's profile.
**Flow:** Resolve identity and Teacher eligibility; accept permitted name and validated
image/PDF evidence reference; assign owner and PENDING in the backend. Own listing
includes verification status. For deletion/replacement, check existing ownership.
Replacement atomically changes the evidence/name and resets status to PENDING.
**Denials:** Reject cross-owner access, forged owner/status, invalid evidence or
ineligible accounts without mutation/disclosure.
**Postcondition:** Only the caller's permitted certificate changes; no self-verification.
**References:** FR-CERT-001, FR-CERT-002; BR-CERT-001, BR-CERT-002.
**Gates:** Upload/path validation, exact DTO/null semantics, replacement concurrency
and Storage cleanup/retention.

## UC-CERT-REVIEW-01 --- Review Pending Teacher Certificate

**Actor:** Authorized authenticated Admin.
**Flow:** Retrieve pending certificates and authorized evidence; verify or reject the
selected pending certificate after checking current state and the evidence reviewed.
**Denials:** Reject Teacher/non-Admin review, non-pending or stale evidence decisions;
do not verify a replacement using an earlier review.
**Postcondition:** The reviewed record is VERIFIED or REJECTED; onboarding is unchanged.
**References:** FR-CERT-002; BR-CERT-001, BR-CERT-002, BR-CERT-003.
**Gates:** Exact review DTO, stale-review protocol, pending-list bounds and evidence delivery.

## UC-CERT-PUBLIC-01 --- View Verified Teacher Certificates

**Actor:** Student or anonymous visitor.
**Flow:** Resolve an appropriately public Teacher profile; return only its VERIFIED
certificate projection under backend filtering and authorized evidence delivery.
**Denials:** Pending/rejected/deleted certificates are not disclosed through alternate
IDs or application-controlled evidence delivery.
**Postcondition:** Read-only public information; no participation or review authority.
**References:** FR-CERT-003; BR-CERT-003; UC-DIS-TEACHERS-01.
**Gates:** Public Teacher-profile eligibility and exact evidence/name projection,
delivery/cache policy and UI design.

---

# 4. Teacher Discovery

## UC-DIS-TEACHERS-01 --- Discover Teachers and View Public Information

**Primary Actor:** Student; Visitor only for permitted public discovery

**Goal:** Find Teachers and inspect permitted public teaching information.

**Trigger:** Actor requests Teacher search or selects a Teacher.

**Preconditions:** Only information approved for public discovery is requested.

**Main Success Flow:**

1. Actor browses or supplies supported search criteria.
2. System returns matching permitted Teacher information.
3. Actor selects a Teacher and the system presents authorized public specialization, teaching experience and introduction.

**Alternate / Exception / Denial Flows:** No matches yield no Teacher results. Requests for private account/security information, private receiving information, transactions or Student information do not expose those details.

**Postconditions:** Public teaching information is available without granting Course participation or management rights.

**Requirement Traceability:** `FR-DIS-004`, `FR-DIS-005`, `FR-AUTH-012`.

**Business Rule Traceability:** `BR-ENR-007`, `BR-AUTH-007`.

**Decision Gates / Deferred Details:** Discovery: exact filters, matching and public fields; Accounts: complete profile contract.

---

# 5. Course Discovery

## UC-STU-BROWSE-COURSES-01 --- Discover Courses

**Primary Actor:** Student; Visitor only for permitted public discovery

**Goal:** Find suitable Courses offered by different Teachers.

**Trigger:** Actor requests Course discovery/search.

**Preconditions:** Only permitted public discovery information is requested.

**Main Success Flow:**

1. Actor browses or supplies supported Course search criteria.
2. System returns permitted matching Course summaries.
3. Actor may select a Course for UC-STU-VIEW-COURSE-01.

**Alternate / Exception / Denial Flows:** No matches yield no Course results. Unsupported criteria do not establish new discovery policy or protected access.

**Postconditions:** No Enrollment or payment effect occurs.

**Requirement Traceability:** `FR-DIS-006`, `FR-DIS-003`, `FR-AUTH-012`.

**Business Rule Traceability:** `BR-ENR-007`, `BR-AUTH-007`.

**Decision Gates / Deferred Details:** Discovery and Course lifecycle: filters, matching, visibility and publication eligibility.

---

## UC-STU-VIEW-COURSE-01 --- View Public Course Information

**Primary Actor:** Student; Visitor only for permitted public discovery

**Goal:** Inspect a Course before purchasing or participating.

**Trigger:** Actor selects a Course.

**Preconditions:** Course information is permitted for public viewing.

**Main Success Flow:**

1. System resolves the selected Course and checks public visibility.
2. System presents permitted name, description, owning Teacher, tuition/price, schedule information and Session count where applicable.
3. Actor may request whole-Course registration separately.

**Alternate / Exception / Denial Flows:** Missing or non-visible Course information is not disclosed. Protected Session content, Meet URL, private payment information and Student/Enrollment details are excluded; discovery cannot bypass participation checks.

**Postconditions:** Only public information is shown; no protected access is granted.

**Requirement Traceability:** `FR-DIS-003`, `FR-DIS-004`, `FR-AUTH-012`.

**Business Rule Traceability:** `BR-ENR-007`, `BR-AUTH-007`.

**Decision Gates / Deferred Details:** Discovery: exact public fields; Course lifecycle: visibility. Capacity information is not made public merely because a Teacher can configure it.

---

# 6. Teacher Course Management

## UC-TEA-COURSE-01 --- Manage Owned Courses

**Primary Actor:** Teacher

**Goal:** Manage owned Courses under the approved lifecycle.

**Trigger:** Teacher requests owned-Course management or Course creation.

**Preconditions:** Teacher is authenticated and eligible; existing-Course operations require ownership.

**Main Success Flow:**

1. Check verified, unlocked, approved Teacher and authoritative ownership.
2. Manage permitted Course content, exactly one Category, tuition/discount window and
   capacity without transferring ownership.
3. Preserve DRAFT/PUBLISHED/COMPLETED/CANCELLED/ARCHIVED meanings and historical Payment
   snapshots; cancellation differs from archival.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Authorized Course changes are retained without owner transfer or
rewriting historical Payment snapshots.

**Requirement Traceability:** `FR-TCR-001`, `FR-TCR-003`, `FR-TCR-004`, `FR-TCR-005`, `FR-TCR-006`, `FR-TCR-008`, `FR-TCR-009`, `NFR-SEC-004`.

**Business Rule Traceability:** `BR-COURSE-001`, `BR-COURSE-002`, `BR-COURSE-003`, `BR-COURSE-004`, `BR-COURSE-007`, `BR-AUTH-003`.

**Decision Gates / Deferred Details:** Detailed validation/retention and Meet supply timing;
no additional lifecycle permissions.

---

## UC-TEA-PUBLISH-01 --- Publish Owned Course

**Primary Actor:** Teacher

**Goal:** Publish and explicitly complete an owned Course after backend validation.

**Trigger:** Teacher requests publication.

**Preconditions:** Teacher is authenticated; Course exists and is owned by the Teacher.

**Main Success Flow:**

1. Validate Teacher eligibility and ownership.
2. Generate concrete Sessions from weekly rules before publication and validate publication
   prerequisites.
3. Keep teaching-in-progress Courses PUBLISHED. Teacher explicitly requests completion and
   backend validates it before COMPLETED.
4. Keep cancellation and archival distinct.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** The requested authorized publication/completion transition occurs only
after backend validation; teaching in progress remains PUBLISHED.

**Requirement Traceability:** `FR-TCR-007`, `FR-TCR-006`, `FR-AUTH-012`.

**Business Rule Traceability:** `BR-COURSE-004`, `BR-AUTH-003`, `BR-AUTH-007`.

**Decision Gates / Deferred Details:** Exact publication/completion prerequisites,
reversal/deletion effects and scheduling boundaries.

---

## UC-TEA-STUDENTS-01 --- Manage Enrolled-Student Teaching Information

**Primary Actor:** Teacher

**Goal:** Inspect and manage permitted teaching information for Students in owned Courses.

**Trigger:** Teacher requests enrolled-Student information or a permitted teaching-information change.

**Preconditions:** Teacher is authenticated; relevant Course and Enrollment context exist.

**Main Success Flow:**

1. System resolves the Course and verifies ownership and the particular information/action permission.
2. System shows only appropriate enrolled-Student teaching information.
3. If Teacher requests an approved change, the system validates and applies only that permitted teaching-information change.

**Alternate / Exception / Denial Flows:** Cross-owner access, unrelated Student data, general account administration or an unapproved action is denied.

**Postconditions:** Permitted teaching information is viewed or changed; no general account, payment or Enrollment override authority is granted.

**Requirement Traceability:** `FR-TCR-010`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-AUTH-003`, `BR-TEA-003`.

**Decision Gates / Deferred Details:** Accounts and Administration: exact permitted fields/actions.

---

# 7. Sessions and Scheduling

## UC-SES-MANAGE-01 --- Manage Owned-Course Sessions

**Primary Actor:** Teacher

**Goal:** Manage owned Session content and lifecycle.

**Trigger:** Teacher requests a Session creation or change within a Course.

**Preconditions:** Teacher is authenticated; the parent Course exists and is owned by the Teacher.

**Main Success Flow:**

1. Traverse Session to Course owner and verify Teacher eligibility.
2. Manage nullable protected content and SCHEDULED/CANCELLED state; content is absent from
   public preview.
3. Reschedule the same Session with immutable change history. Cancellation keeps its row
   and number, sets CANCELLED and synchronizes the Calendar event; no make-up Session
   or change to session_count is permitted.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Authorized content/state changes preserve protected visibility and valid
Session identity, numbering and schedule history.

**Requirement Traceability:** `FR-SES-001`, `FR-SES-002`, `FR-SES-004`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-SES-001`, `BR-SES-002`, `BR-AUTH-003`, `BR-TEA-001`.

**Decision Gates / Deferred Details:** Schedule/cancellation operation validation
and content validation; fixed numbering and no-replacement cancellation are approved.

---

## UC-SES-VIEW-01 --- Access Course Sessions and Content

**Primary Actor:** Student

**Goal:** View permitted Sessions and learning content for a Course.

**Trigger:** Student requests Course Sessions or selects a Session.

**Preconditions:** Student is authenticated; required authoritative Course participation conditions must hold.

**Main Success Flow:**

1. System resolves the Student and the requested Course or Session's parent Course.
2. System checks authoritative Enrollment and the requested participation permission.
3. System returns only authorized Session information and content.
4. Student may request Assignment or Meet access through their separately authorized interactions.

**Alternate / Exception / Denial Flows:** Missing required Enrollment, wrong Course association or unauthorized access prevents protected disclosure. A public Course listing or client payment claim is insufficient.

**Postconditions:** Authorized Session information is available without changing participation state.

**Requirement Traceability:** `FR-SES-003`, `FR-ENR-007`, `FR-AUTH-013`.

**Business Rule Traceability:** `BR-SES-001`, `BR-ENR-006`, `BR-ENR-007`.

**Decision Gates / Deferred Details:** Enrollment participation-edge checks and Course
content availability; approved states are settled.

---

## UC-SCH-COURSE-01 --- Configure Course Schedule

**Primary Actor:** Teacher

**Goal:** Configure recurring weekly Course schedule and generate Sessions.

**Trigger:** Teacher requests a Course schedule configuration/change.

**Preconditions:** Teacher is authenticated and owns the Course.

**Main Success Flow:**

1. Validate Teacher eligibility/ownership, required IANA timezone and non-overlapping
   ISO weekly rules (1=Monday through 7=Sunday).
2. Starting from planned_start_date, generate chronologically until exactly session_count
   Sessions exist, numbered uniquely 1..session_count; persist absolute timestamps.
3. Allow draft regeneration only before meaningful historical/business activity; Teacher
   reviews concrete Sessions before publication. Publication requires a valid Meet URL.
4. After publication preserve concrete timestamps as authority; reschedule or cancel
   the same Session under its workflow, synchronize its individual Calendar event and
   retain core state on synchronization failure.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Validated weekly schedule rules and generated Sessions are available
before Course publication.

**Requirement Traceability:** `FR-TCR-009`, `FR-SCH-001`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-SCH-001`, `BR-COURSE-004`, `BR-AUTH-003`.

**Decision Gates / Deferred Details:** DST gap/overlap handling and operation validation.
ISO weekdays, IANA timezone, generation/count and numbering are settled by BR-SCH-001.

---

## UC-SCH-SESSION-01 --- Reschedule an Owned Session

**Primary Actor:** Teacher

**Goal:** Reschedule an owned Session with history.

**Trigger:** Teacher requests an individual Session schedule change.

**Preconditions:** Teacher is authenticated; Session exists under an owned Course.

**Main Success Flow:**

1. Validate ownership and permitted time changes.
2. Update the same Session, recording old/new times in schedule history; do not create
   RESCHEDULED status.
3. Propagate Calendar synchronization independently so its failure does not roll back the Session.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** The same Session has updated times plus retained change history; a
Calendar failure cannot roll back that core change.

**Requirement Traceability:** `FR-SCH-002`, `FR-SES-002`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-SCH-001`, `BR-SES-002`, `BR-AUTH-003`.

**Decision Gates / Deferred Details:** Detailed cutoff/conflict/cancellation rules and
external retry contract.

---

# 8. Assignments, Submissions and Results

## UC-ASN-MANAGE-01 --- Manage Session Assignments

**Primary Actor:** Teacher

**Goal:** Manage ACTIVE/CANCELLED Session Assignments.

**Trigger:** Teacher requests an Assignment creation or change.

**Preconditions:** Teacher is authenticated; parent Session belongs to an owned Course.

**Main Success Flow:**

1. Validate ownership through Session/Course.
2. Manage Assignment content and authorized Storage attachment metadata.
3. Refuse hard deletion once any Submission exists.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Authorized Assignment changes and Storage references are retained;
existing Submissions prevent hard deletion.

**Requirement Traceability:** `FR-ASN-001`, `FR-ASN-002`, `FR-SES-004`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-ASN-001`, `BR-AUTH-003`, `BR-TEA-001`.

**Decision Gates / Deferred Details:** Deadline/score limits and upload contracts; no
speculative assessment engine.

---

## UC-ASN-VIEW-01 --- View an Authorized Assignment

**Primary Actor:** Student

**Goal:** Inspect an Assignment available through authorized Course participation.

**Trigger:** Student requests an Assignment.

**Preconditions:** Student is authenticated; required participation conditions must hold.

**Main Success Flow:**

1. System resolves Assignment -> Session -> Course and the Student's identity.
2. System validates required authoritative Enrollment and Assignment visibility.
3. System presents only the permitted Assignment information.

**Alternate / Exception / Denial Flows:** Missing required Enrollment or unauthorized Assignment access is denied. Discovery or a client-selected Course cannot bypass the actual parent Course.

**Postconditions:** Authorized Assignment information is shown without a submission or result being created.

**Requirement Traceability:** `FR-ASN-001`, `FR-ASN-003`, `FR-ENR-007`.

**Business Rule Traceability:** `BR-ASN-001`, `BR-ASN-002`, `BR-ENR-006`.

**Decision Gates / Deferred Details:** Assignments and Enrollment: exact availability, format and participation validity.

---

## UC-ASN-SUBMIT-01 --- Submit Assignment Work

**Primary Actor:** Student

**Goal:** Maintain one current Student Submission per Assignment.

**Trigger:** Student requests to submit Assignment work.

**Preconditions:** Student is authenticated; Assignment exists and required participation conditions hold.

**Main Success Flow:**

1. Validate authenticated Student and required Enrollment access.
2. Save DRAFT content/authorized attachment references or submit it as SUBMITTED with submitted_at.
3. Preserve one current Submission per Student/Assignment; no revision-history table.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** One current Student/Assignment Submission contains permitted work, with
submission evidence when SUBMITTED.

**Requirement Traceability:** `FR-ASN-004`, `FR-ENR-007`, `FR-AUTH-013`, `NFR-DATA-001`.

**Business Rule Traceability:** `BR-ASN-001`, `BR-ASN-002`.

**Decision Gates / Deferred Details:** Deadlines, late/resubmission and file
validation/delivery contracts.

---

## UC-ASN-INSPECT-01 --- Inspect Course Submissions and Results

**Primary Actor:** Teacher

**Goal:** Inspect and grade owned-Course submissions.

**Trigger:** Teacher requests submissions or selects a submission/result.

**Preconditions:** Teacher is authenticated; relevant Assignment belongs to an owned Course.

**Main Success Flow:**

1. Traverse Submission -> Assignment -> Session -> Course and validate owning Teacher authority.
2. Inspect authorized work and store score/feedback/grading metadata on the Submission itself.
3. GRADED requires submission time, grading time and grader; any numeric score must be
   finite and not exceed Assignment max_score, but a score is not automatically mandatory.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Only owned-Course work is inspected/graded; grading metadata and
optional score/feedback remain on Submission.

**Requirement Traceability:** `FR-ASN-005`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-ASN-001`, `BR-ASN-003`, `BR-AUTH-003`, `BR-TEA-003`.

**Decision Gates / Deferred Details:** Score scale/range and result-availability details.

---

## UC-ASN-RESULT-01 --- View Own Assignment Result

**Primary Actor:** Student

**Goal:** Inspect one's own permitted submission/result information.

**Trigger:** Student requests their Assignment result.

**Preconditions:** Student is authenticated; requested information is associated with their own work.

**Main Success Flow:**

1. System resolves authenticated identity and the submission/result's Assignment and Course.
2. System checks own-information authorization and applicable Course participation requirements.
3. System returns only the Student's permitted submission/result information when available.

**Alternate / Exception / Denial Flows:** Another Student's private submission/result is not disclosed. Missing required participation access is denied. An unavailable result does not imply a zero score, failure or completed grading.

**Postconditions:** Permitted own information is displayed without modification.

**Requirement Traceability:** `FR-ASN-006`, `FR-ENR-007`, `FR-AUTH-013`.

**Business Rule Traceability:** `BR-ASN-003`, `BR-ENR-006`.

**Decision Gates / Deferred Details:** Assignments: score precision/scale and result
availability; owning-Teacher grading is approved. Enrollment: applicable access edges.

---

# 9. Enrollment and Course Participation

## UC-STU-ENROLL-01 --- Register for Whole-Course Participation

**Primary Actor:** Student

**Goal:** Enroll in an entire Course under capacity and cutoff rules.

**Trigger:** Student requests Course registration.

**Preconditions:** Student is authenticated; Course is identified and eligibility must be checked.

**Main Success Flow:**

1. Validate Student identity, Course eligibility and that the first Session has not started.
2. Enforce unique Student/Course Enrollment; COMPLETED historical participation cannot
   simply re-enroll.
3. Free Course activation creates no Payment. Paid enrollment may reserve a PENDING seat
   with expiry.
4. Under concurrency control count ACTIVE plus unexpired PENDING seats; activate only
   through the approved free or trustworthy-payment workflow.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** The unique Enrollment reflects the approved free activation or paid
pending workflow without exceeding capacity; Payment remains separate.

**Requirement Traceability:** `FR-ENR-006`, `FR-ENR-007`, `FR-ENR-008`, `FR-PAY-012`.

**Business Rule Traceability:** `BR-COURSE-007`, `BR-ENR-001`, `BR-ENR-008`, `BR-PAY-009`.

**Decision Gates / Deferred Details:** Reservation duration, cancellation/re-entry and
late/cutoff edge behavior.

---

## UC-STU-MY-COURSES-01 --- View My Courses

**Primary Actor:** Student

**Goal:** View one's enrolled Courses and permitted progress information.

**Trigger:** Student requests their Course list.

**Preconditions:** Student is authenticated and eligible to view their own information.

**Main Success Flow:**

1. System resolves authenticated Student identity.
2. System retrieves only that Student's permitted Enrollment/Course information.
3. System returns the Courses and permitted progress information; current participation eligibility is evaluated separately when accessing protected resources.

**Alternate / Exception / Denial Flows:** A request for another Student's private Course/Enrollment information is denied. No Enrollments yields an empty personal list.

**Postconditions:** Listing a Course does not activate Enrollment or prove current protected-access eligibility.

**Requirement Traceability:** `FR-ENR-004`, `FR-ENR-005`, `FR-PRO-004`, `FR-ENR-007`.

**Business Rule Traceability:** `BR-ENR-001`, `BR-ENR-006`, `BR-AUTH-006`.

**Decision Gates / Deferred Details:** Enrollment access edges and Progress
indicators/calculation; four lifecycle states are approved.

---

# 10. Teacher Receiving Information and Course Payments

## UC-PAY-RECEIVING-01 --- Manage Own Receiving Information

**Primary Actor:** Teacher

**Goal:** Maintain historical Teacher bank receiving information.

**Trigger:** Teacher requests to view or change receiving information.

**Preconditions:** Teacher is authenticated and authorized to manage their own receiving information.

**Main Success Flow:**

1. Check eligible Teacher identity and own receiving-account scope.
2. Retain historical accounts and allow at most one ACTIVE account.
3. Existing Payments retain their original account reference.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Authorized bank information is retained, at most one account is ACTIVE
and existing Payment references remain historical.

**Requirement Traceability:** `FR-PAY-007`, `FR-AUTH-013`, `NFR-SEC-004`.

**Business Rule Traceability:** `BR-PAY-006`, `BR-AUTH-001`.

**Decision Gates / Deferred Details:** Bank validation and adapter verification contract;
provider_account_ref is omitted from Physical V1.

---

## UC-PAY-COURSE-01 --- Pay for an Entire Course

**Primary Actor:** Student

**Goal:** Pay the owning Teacher for the entire Course.

**Trigger:** Student initiates payment for a selected Course, including from UC-STU-ENROLL-01.

**Preconditions:** Student is authenticated; Course and its owning Teacher's configured destination must be authoritatively resolvable. Detailed execution depends on Payment and Enrollment gates.

**Main Success Flow:**

1. Validate Student/Enrollment, first-Session cutoff and capacity/reservation constraints.
2. Snapshot price/currency and historical Teacher bank account; allow at most one PENDING
   Payment per Enrollment.
3. Follow the VietQR direction subject to verified provider contracts. Record actual
   transactions even when unmatched, retaining receiver context when resolvable.
4. Confirm only from trustworthy receiver/code/amount/currency evidence and timestamp
   confirmation; reject claims, wrong codes/amounts and automatic partial aggregation.
5. Apply Enrollment effects transactionally; late evidence must not overbook capacity.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Payment and actual transaction evidence are retained independently; only
trustworthy validated confirmation can cause an eligible capacity-safe Enrollment effect.

**Requirement Traceability:** `FR-PAY-002`, `FR-PAY-008`, `FR-PAY-009`, `FR-PAY-010`, `FR-PAY-011`, `FR-PAY-012`, `FR-ENR-008`, `FR-AUTH-013`, `NFR-DATA-007`.

**Business Rule Traceability:** `BR-PAY-002`, `BR-PAY-003`, `BR-PAY-007`, `BR-PAY-008`, `BR-PAY-009`, `BR-PAY-010`, `BR-ENR-008`.

**Decision Gates / Deferred Details:** Provider API/evidence contracts, conceptual order
field, durable retry identity and late-payment reconciliation.

---

## UC-PAY-VIEW-01 --- View Related Course Payment Information

**Primary Actor:** Student / Teacher

**Goal:** Inspect payment/status information one is authorized to view.

**Trigger:** Actor requests related Course transaction information.

**Preconditions:** Actor is authenticated; the requested transaction has relevant Course/Student context.

**Main Success Flow:**

1. System resolves the transaction's Course and the actor's identity.
2. For a Student, system checks related-Student visibility; for a Teacher, it checks the Course's owning Teacher and permitted visibility.
3. System presents only authorized transaction/status information without converting an unverified claim into a confirmed outcome.

**Alternate / Exception / Denial Flows:** Another Teacher's protected transactions or an unrelated Student's private payment information are denied. Viewing requests cannot confirm payment or change a transaction outcome.

**Postconditions:** Permitted information is displayed without payment or Enrollment mutation.

**Requirement Traceability:** `FR-PAY-011`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-PAY-010`, `BR-AUTH-003`.

**Decision Gates / Deferred Details:** Payment and Administration: exact visible
projections/provider evidence mapping; approved states are settled. Viewing grants no
refund, payout or confirmation authority.

---

## UC-PAY-REFUND-01 --- Complete a Full Course-Payment Refund

**Primary Actor:** Teacher; Admin verifies completion.

**Goal:** Return the applicable full Payment amount to the Student without platform custody.

**Preconditions:** Authorized workflow and applicable Payment; at most one Refund per Payment.

**Main Success Flow:**

1. Validate full amount and related ownership; record PENDING refund.
2. Teacher performs the bank transfer and supplies authorized Storage proof; SUBMITTED
   requires proof and submission time.
3. Admin verifies completion; COMPLETED additionally records completion time and verifier.
4. Retain Payment history unchanged by a REFUNDED status; cancellation preserves applicable
   prior evidence.

**Alternate / Exception / Denial Flows:** Reject unauthorized, duplicate or partial refunds
and unsupported completion claims.

**Postconditions:** Refund evidence/status is recorded separately from Payment and Enrollment.

**Requirement Traceability:** FR-PAY-013, FR-PAY-009, NFR-DATA-007.

**Business Rule Traceability:** BR-PAY-011, BR-PAY-008.

**Decision Gates / Deferred Details:** Detailed refund eligibility and validation, proof
delivery and review contract; no automatic Enrollment effect is invented.

---

# 11. Google Meet Access

## UC-MEET-MANAGE-01 --- Supply the Course Meet URL

**Primary Actor:** Teacher

**Goal:** Manually supply/update the single meeting URL for an owned Course.

**Trigger:** Teacher requests a Course Meet URL change.

**Preconditions:** Teacher is authenticated, owns the Course and has created the meeting externally; external creation is outside the automated application flow.

**Main Success Flow:**

1. System verifies Course ownership and change permission.
2. Teacher manually supplies the externally created Google Meet URL.
3. System validates the URL/change under later-approved Course rules and maintains exactly one Course Meet URL shared by all Sessions.
4. System reports the authorized change while keeping the URL protected.

**Alternate / Exception / Denial Flows:** Cross-owner updates or invalid/unapproved URL changes are rejected. No separate meeting is created for an individual Session.

**Postconditions:** The Course has its single manually supplied shared URL; the application creates no meeting and uses no Google Meet API.

**Requirement Traceability:** `FR-MEET-001`, `FR-MEET-003`, `FR-TCR-006`.

**Business Rule Traceability:** `BR-MEET-001`, `BR-COURSE-004`, `BR-AUTH-003`.

**Decision Gates / Deferred Details:** Course lifecycle: detailed URL validation, supply timing and update effects.

---

## UC-MEET-ACCESS-01 --- Access the Course Meeting URL

**Primary Actor:** Student

**Goal:** Obtain the authorized Course URL to join its external meeting.

**Trigger:** Student requests Meet access for a Course or its Session.

**Preconditions:** Student is authenticated; the Course URL is supplied and required participation conditions hold.

**Main Success Flow:**

1. System resolves the Course, including through the requested Session where applicable.
2. System checks authoritative Enrollment and permitted participation access.
3. System provides only the authorized Course Meet URL; every Session uses the same Course URL.
4. Student uses that URL outside the platform's meeting infrastructure.

**Alternate / Exception / Denial Flows:** Missing required Enrollment, unauthorized access or a mismatched Course request does not disclose the URL. If it is unavailable, the system cannot fabricate a link or automatically create a meeting.

**Postconditions:** Authorized URL access is provided; successful external attendance is not asserted or recorded by this use case.

**Requirement Traceability:** `FR-MEET-002`, `FR-MEET-003`, `FR-ENR-007`, `FR-AUTH-012`.

**Business Rule Traceability:** `BR-MEET-001`, `BR-MEET-002`, `BR-ENR-006`.

**Decision Gates / Deferred Details:** Enrollment validity and Course URL availability; no external admission/attendance policy is defined.

---

# 12. Google Calendar Schedule/Reminder Goal

## UC-CAL-SCHEDULE-01 --- Use Authorized Calendar Schedule/Reminder Information

**Primary Actor:** Teacher / enrolled Student

**Goal:** Synchronize authorized Sessions to organizational Google Calendar.

**Trigger:** Actor requests the intended Calendar-related schedule/reminder capability.

**Preconditions:** Actor is authenticated; Teacher ownership or Student participation authorization applies. Detailed interaction requires the Calendar gate.

**Main Success Flow:**

1. Use the configured system/organization account, with at most one provider event mapping
   per Session.
2. Derive Teacher/Student attendee emails from Users/Enrollments; retain no per-user
   OAuth/attendee table.
3. Record synchronization state; SYNCED requires external event ID and synchronization time.
4. Keep Session changes authoritative when synchronization fails; update relevant future
   attendees after verified email changes.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** A successful synchronization has event/time evidence; failure leaves
authoritative Session changes intact and synchronization needing retry/reconciliation.

**Requirement Traceability:** `INT-CAL-001`, `FR-AUTH-011`, `FR-ENR-007`.

**Business Rule Traceability:** `BR-AUTH-005`, `BR-AUTH-003`, `BR-ENR-006`.

**Decision Gates / Deferred Details:** Delivery/retry/reminder details. One configured
Calendar and its event-ID uniqueness scope are approved.

---

# 13. Learning Progress

## UC-LEARN-PROGRESS-01 --- View Own Learning Progress

**Primary Actor:** Student

**Goal:** Understand one's appropriate Course learning progress.

**Trigger:** Student requests personal progress information.

**Preconditions:** Student is authenticated and authorized for the requested personal information.

**Main Success Flow:**

1. System resolves Student identity and relevant Course participation context.
2. System checks own-information and applicable participation access.
3. System returns appropriate progress information derived from authoritative learning information under later-approved calculation rules.

**Alternate / Exception / Denial Flows:** Another Student's private information or missing required participation access prevents disclosure. Missing indicators/results are not converted into an invented score or completion value.

**Postconditions:** Permitted personal progress is displayed without changing learning information.

**Requirement Traceability:** `FR-PRO-004`, `FR-PRO-009`, `FR-ENR-005`, `NFR-SEC-016`.

**Business Rule Traceability:** `BR-AUTH-006`, `BR-ENR-006`.

**Decision Gates / Deferred Details:** Progress: indicators, calculation, completion and grading aggregation; Enrollment access validity.

---

## UC-TEA-ANALYTICS-01 --- Monitor Owned-Course Learning Information

**Primary Actor:** Teacher

**Goal:** Monitor appropriate Enrollment, Student results and progress for owned Courses.

**Trigger:** Teacher requests monitoring information for an owned Course.

**Preconditions:** Teacher is authenticated; relevant Course exists.

**Main Success Flow:**

1. System checks Course ownership and the particular Student-information visibility permission.
2. System obtains permitted Enrollment statistics and appropriate progress/results for Students in that Course.
3. System presents only authorized information. Individual submission inspection uses UC-ASN-INSPECT-01.

**Alternate / Exception / Denial Flows:** Cross-owner Course access or unnecessary/unapproved personal detail is denied. Undefined indicators are not replaced with invented formulas.

**Postconditions:** Appropriate owned-Course information is inspected without changing results or participation.

**Requirement Traceability:** `FR-TAN-002`, `FR-TAN-003`, `FR-ASN-005`, `FR-AUTH-011`.

**Business Rule Traceability:** `BR-TEA-002`, `BR-TEA-003`, `BR-AUTH-003`.

**Decision Gates / Deferred Details:** Progress, Assignments and Reporting: indicator/calculation definitions and result availability.

---

# 14. Participant Feedback and Rating

## UC-RATE-PARTICIPANT-01 --- Provide Participation-Related Feedback

**Primary Actor:** Student

**Goal:** Provide one rating/feedback for a completed Enrollment.

**Trigger:** Student expresses an intent to provide participant feedback.

**Preconditions:** Authenticated Student owns a COMPLETED Enrollment and has not already
created feedback for it.

**Main Success Flow:**

1. Resolve the Student's Enrollment and verify COMPLETED status.
2. Accept at most one feedback record per Enrollment with rating 1..5.
3. Derive aggregates without stored rating totals.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** At most one rating 1..5 is stored for the Student's COMPLETED
Enrollment; no aggregate is persisted.

**Requirement Traceability:** `FR-RATE-001`, `FR-AUTH-013`.

**Business Rule Traceability:** `BR-RATE-001`, `BR-AUTH-001`.

**Decision Gates / Deferred Details:** Editing/deletion, moderation and visibility details.

---

# 15. Admin Management and Monitoring

## UC-ADM-USERS-01 --- Manage Authorized User Accounts

**Primary Actor:** Admin

**Goal:** Inspect Student/Teacher accounts and perform permitted account administration.

**Trigger:** Admin requests account information or a permitted account action.

**Preconditions:** Admin is authenticated and has the particular administrative permission.

**Main Success Flow:**

1. System validates Admin access before returning permitted Student/Teacher account/profile information and associated Course information where allowed.
2. Admin selects an authorized action, including lock/unlock where approved.
3. System validates action-specific authority and account/security constraints.
4. System applies only the authorized change and reports its outcome.

**Alternate / Exception / Denial Flows:** Wrong role, restricted account or action outside approved authority is denied. Admin cannot use this flow for arbitrary role assignment, unrestricted profile editing or bypassing Teacher onboarding policy.

**Postconditions:** Only permitted account information is viewed/changed; no universal Teacher-provisioning obligation is established.

**Requirement Traceability:** `FR-ADM-002`, `FR-ADM-003`, `FR-ADM-004`, `FR-ADM-006`, `FR-ACC-014`.

**Business Rule Traceability:** `BR-ADM-001`, `BR-AUTHN-017`, `BR-AUTHN-018`, `BR-AUTHN-020`, `BR-AUTHN-023`, `BR-AUTHN-024`.

**Decision Gates / Deferred Details:** Accounts and Administration: exact fields/actions, unlock transitions, provisioning and overrides; no new provisioning flow is defined.

---

## UC-ADM-COURSES-01 --- Oversee Courses

**Primary Actor:** Admin

**Goal:** Inspect Courses across Teachers and exercise only approved oversight.

**Trigger:** Admin requests Course oversight.

**Preconditions:** Admin is authenticated and authorized for the requested information/action.

**Main Success Flow:**

1. System validates the specific oversight permission.
2. System returns permitted Course information, including authoritative owning-Teacher association.
3. Admin inspects the Course; any requested oversight action is checked separately against approved authority before its effect.

**Alternate / Exception / Denial Flows:** Unapproved editing, ownership override, grading or moderation is denied. Admin role alone does not bypass Teacher boundaries.

**Postconditions:** Authorized oversight occurs without making Admin the Course owner, Teacher or default Assignment grader.

**Requirement Traceability:** `FR-ACR-001`, `FR-ACR-002`.

**Business Rule Traceability:** `BR-ADM-001`, `BR-ADM-002`.

**Decision Gates / Deferred Details:** Administration and Course lifecycle: exact actions, moderation, overrides and lifecycle effects.

---

## UC-ADM-CATEGORIES-01 --- Manage Course Categories and Topics

**Primary Actor:** Admin

**Goal:** Maintain active/inactive Course Categories.

**Trigger:** Admin requests category/topic management.

**Preconditions:** Admin is authenticated and authorized for the particular management action.

**Main Success Flow:**

1. Verify approved Admin authority.
2. Manage Category information and active/inactive state; each Course selects exactly one Category.
3. Retain and deactivate a used Category instead of deleting it.

**Alternate / Exception / Denial Flows:** Reject unauthorized or invalid requests without
protected disclosure or applying the requested change. Enforce the operation-specific
constraints above; do not invent defaults for remaining gates.

**Postconditions:** Authorized Category changes retain used Categories through deactivation,
preserving exactly one Category per Course.

**Requirement Traceability:** `FR-ADM-007`, `NFR-SEC-004`.

**Business Rule Traceability:** `BR-ADM-001`.

**Decision Gates / Deferred Details:** Exact validation and unrelated Admin overrides; no
hierarchy is introduced.

---

## UC-ADM-ENROLLMENTS-01 --- Monitor Course Enrollments

**Primary Actor:** Admin

**Goal:** Inspect permitted Course participation information for administration.

**Trigger:** Admin requests Enrollment monitoring.

**Preconditions:** Admin is authenticated and authorized for Enrollment visibility.

**Main Success Flow:**

1. System validates monitoring permission.
2. System obtains permitted Student-Course Enrollment information.
3. System displays only authorized information without treating visibility as alteration authority.

**Alternate / Exception / Denial Flows:** Unauthorized information requests and attempted arbitrary creation, activation, cancellation or payment override are denied.

**Postconditions:** Enrollment information is inspected without changing participation or payment state.

**Requirement Traceability:** `FR-ADM-008`, `FR-AUTH-013`.

**Business Rule Traceability:** `BR-ADM-001`, `BR-ENR-001`, `BR-ENR-008`.

**Decision Gates / Deferred Details:** Administration and Enrollment: visible details, edge
transitions and unrelated overrides; approved states are settled.

---

## UC-ADM-TRANSACTIONS-01 --- Monitor Course Transactions

**Primary Actor:** Admin

**Goal:** Inspect authorized Course payment/status information.

**Trigger:** Admin requests transaction monitoring.

**Preconditions:** Admin is authenticated and has transaction-monitoring permission.

**Main Success Flow:**

1. System validates the permitted monitoring scope.
2. System retrieves authorized Course transaction information and permitted status/timing information.
3. System presents that information without turning unverified claims or unknown outcomes into verified success or confirmed failure.

**Alternate / Exception / Denial Flows:** Unauthorized disclosure or attempts to confirm/change payment outcomes, perform refunds or take tuition custody are denied; monitoring permission grants none of those actions.

**Postconditions:** Transactions are viewed without changing payment outcomes or Enrollment.

**Requirement Traceability:** `FR-ATR-001`, `FR-ATR-002`, `FR-ATR-003`, `FR-PAY-011`.

**Business Rule Traceability:** `BR-ADM-005`, `BR-PAY-010`, `BR-PAY-009`.

**Decision Gates / Deferred Details:** Payment, Reporting and Administration: provider
mapping, event/report timing and permitted fields; approved Payment states are settled.

---

## UC-ADM-ANALYTICS-01 --- View Platform Statistics

**Primary Actor:** Admin

**Goal:** Inspect approved operational platform statistics.

**Trigger:** Admin requests permitted platform statistics.

**Preconditions:** Admin is authenticated and authorized for the requested statistics.

**Main Success Flow:**

1. System validates statistical-information access.
2. System obtains authoritative totals for users, Students, Teachers and Courses, plus appropriate Enrollment and transaction statistics under approved definitions.
3. System presents only permitted measures without describing Teacher tuition as platform/Admin revenue.

**Alternate / Exception / Denial Flows:** Unauthorized reports are denied. Unresolved measures/time boundaries are not supplied from invented formulas or dashboard requirements.

**Postconditions:** Approved statistics are viewed without source-data mutation or financial authority.

**Requirement Traceability:** `FR-AAN-001`, `FR-AAN-002`, `FR-AAN-003`, `FR-AAN-004`, `FR-AAN-005`, `FR-AAN-008`.

**Business Rule Traceability:** `BR-ADM-001`, `BR-ADM-005`.

**Decision Gates / Deferred Details:** Reporting and Payment: detailed measures, periods, boundaries and outcome classifications.

---

# 16. Open / Deferred Use-Case Areas

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

Approved lifecycles, full refunds, organizational Calendar integration and Storage scope are
settled. Detailed interaction contracts remain gated; no implementation is authorized.

---

# 17. Legacy UC Disposition

This register records the earlier scope migration, not today's open-decision list.
Independent V1 approvals in the active sections supersede its then-deferred choices.

This register accounts for all 42 original UC definitions exactly once:
22 preserved/reconciled and 20 retired/superseded. Only the UC headings in sections
2-15 are active definitions. Historical entries below are not active dependencies.

| Original UC ID | Disposition | Reason |
|---|---|---|
| UC-AUTH-REGISTER-01 | Preserved / reconciled | Student registration goal retained; obsolete tiers, fields and fixed initial-state assumptions removed. |
| UC-AUTH-VERIFY-EMAIL-01 | Preserved / reconciled | Verification retained without unconditional activation or Teacher approval. |
| UC-AUTH-LOGIN-01 | Preserved / reconciled | Shared login retained; Premium and persistence-model detail removed. |
| UC-AUTH-REFRESH-01 | Preserved / reconciled | Refresh validation retained; unresolved security details remain gated. |
| UC-AUTH-LOGOUT-01 | Preserved / reconciled | Applicable logout invalidation retained without universal token-revocation claims. |
| UC-AUTH-ME-01 | Preserved / reconciled | Own-account/profile viewing retained; obsolete fixed projection and entitlements removed. |
| UC-AUTH-PROFILE-01 | Preserved / reconciled | Own-profile goal retained; fixed fields and detailed update contract superseded. |
| UC-AUTH-CHANGE-PASSWORD-01 | Preserved / reconciled | Secure password-change goal retained. |
| UC-AUTH-FORGOT-PASSWORD-01 | Preserved / reconciled | Recovery initiation and privacy retained. |
| UC-AUTH-RESET-PASSWORD-01 | Preserved / reconciled | Credential-validated password reset retained. |
| UC-STU-BROWSE-COURSES-01 | Preserved / reconciled | Discovery retained; CEFR and fixed publication-state assumptions removed. |
| UC-STU-VIEW-COURSE-01 | Preserved / reconciled | Public Course viewing retained without tiers or vocabulary Lesson information. |
| UC-STU-ENROLL-01 | Preserved / reconciled | Course participation goal retained; Premium checks, unconditional activation and old duplicate policy removed. |
| UC-STU-MY-COURSES-01 | Preserved / reconciled | Personal Course list retained, distinct from participation authorization. |
| UC-STU-VIEW-LESSON-01 | Retired | Vocabulary Lesson access is not Session access; new Session IDs have separate meaning. |
| UC-LEARN-VOCABULARY-01 | Retired | Vocabulary learning is outside current scope. |
| UC-LEARN-FILL-01 | Retired | Fill Word/attempt evaluation is outside current scope. |
| UC-LEARN-LISTENING-01 | Retired | Listening exercise/audio behavior is outside current scope. |
| UC-LEARN-QUIZ-01 | Retired | Quiz and automatic attempt scoring are not Assignment behavior. |
| UC-LEARN-PROGRESS-01 | Preserved / reconciled | Personal progress goal retained; mastery, weak vocabulary and legacy formulas removed. |
| UC-VOC-SEARCH-01 | Retired | Vocabulary/dictionary search is outside current scope. |
| UC-VOC-DETAIL-01 | Retired | Vocabulary/audio/tier detail is outside current scope. |
| UC-VOC-SAVE-01 | Retired | Saved vocabulary is outside current scope. |
| UC-VOC-MY-01 | Retired | Personal vocabulary collection is outside current scope. |
| UC-REV-REVIEW-01 | Retired | Vocabulary Review/practice is not participant feedback; its ID is not reused. |
| UC-PRE-BENEFITS-01 | Retired | Standard/Premium benefits are outside current scope. |
| UC-PRE-SUBSCRIBE-01 | Retired | Premium purchase is not Course payment; its ID is not reused. |
| UC-PRE-SUBSCRIPTION-01 | Retired | Subscription is not Enrollment; its ID is not reused. |
| UC-TEA-COURSE-01 | Preserved / reconciled | Owned-Course management retained; CEFR, DRAFT/default classification and DTO assumptions removed. |
| UC-TEA-PUBLISH-01 | Preserved / reconciled | Publishing retained; former archive action and fixed lifecycle/history effects are deferred, not approved by this ID. |
| UC-TEA-LESSON-01 | Retired | Vocabulary Lesson management is not Session management. |
| UC-TEA-VOCABULARY-01 | Retired | Vocabulary/sense selection and dictionary import are outside current scope. |
| UC-TEA-EXERCISE-01 | Retired | Fill Word/Listening/Quiz management is not Assignment management. |
| UC-TEA-ANALYTICS-01 | Preserved / reconciled | Owned-Course monitoring retained; difficult vocabulary and obsolete metrics removed. |
| UC-ADM-USERS-01 | Preserved / reconciled | Authorized account administration retained without mandatory Teacher provisioning. |
| UC-ADM-TEACHERS-01 | Superseded | Fixed Admin Teacher-provisioning workflow withdrawn; new Teacher-registration goal does not inherit it. |
| UC-ADM-COURSES-01 | Preserved / reconciled | Oversight retained without implied ownership or unrestricted overrides. |
| UC-ADM-CLASSIFY-01 | Retired | Standard/Premium Course classification is outside current scope. |
| UC-ADM-SUBSCRIPTIONS-01 | Retired | Subscription monitoring is not Enrollment monitoring. |
| UC-ADM-TRANSACTIONS-01 | Preserved / reconciled | Transaction monitoring retained for Course payments, without custody or confirmation authority. |
| UC-ADM-ANALYTICS-01 | Preserved / reconciled | Operational counts/monitoring retained; subscription/revenue metrics removed. |
| UC-ADM-REVENUE-01 | Retired | Subscription revenue reporting is withdrawn; Teacher tuition is not Admin/platform revenue. |

New tutoring goals use independent semantic IDs. Session is not a renamed Lesson;
Assignment is not a renamed Quiz/exercise; Enrollment is not Subscription;
participant feedback is not vocabulary Review. Vocabulary, dictionary/audio,
CEFR organization, Fill Word, Listening exercises, mastery/weak vocabulary,
Standard/Premium access, subscription renewal/expiration and subscription revenue
are historical scope only. No old flow is silently imported into a new ID.

Preserving an ID preserves a materially compatible actor goal, not its former
policy or detailed flow. The retained publishing ID does not authorize the former
archive behavior. No old requirement/BR wildcard is carried forward as active
traceability.

DOMAIN_MODEL.md, ARCHITECTURE.md, DATABASE_DESIGN.md, API_DESIGN.md,
TASK_BREAKDOWN.md, FEATURE_STATUS.md and README.md require separately approved
reconciliation where affected. Existing downstream references do not reactivate
retired IDs or validate their old context. This change does not repair them,
reset completed infrastructure evidence or begin the next documentation stage.
