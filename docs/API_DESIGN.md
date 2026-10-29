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
Section 3.1 approves one bounded current-account contract; no broader method/path catalogue is finalized. Section 23 contains
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
| Student registration | Visitor seeking a Student account | Backend assigns STUDENT; required email/password/fullName and initial verification follow Section 3.3. |
| Teacher registration | Prospective Teacher | Section 3.4 defines separate Teacher registration; verified email, unlocked account and approved onboarding remain required for business authority. Sections 3.5–3.8 define bounded Admin review, own onboarding status, pending queue and resubmission. |
| Email verification | Eligible account-verification context | Purpose-specific, account-bound, expiring credential; consume according to approved flow. Verification cannot override administrative restrictions or define Teacher activation. |
| Resend verification | Eligible verification context | Initial verification uses Section 3.3; other verification purposes remain deferred. Avoid unnecessary account-existence disclosure. |
| Login | Student, Teacher or Admin | Validate credentials and current eligibility; issue only permitted authentication. Batch 2 contract below governs verified, unlocked account authentication; Teacher business approval remains separate. |
| Refresh | Holder of usable refresh authority | Validate server-side expiry, revocation and current account/session eligibility; do not require an unexpired Access Token as proof of refresh eligibility. |
| Logout | Applicable authenticated/session context | Invalidate applicable refresh/session authority and clear applicable client state; frontend deletion alone is insufficient. Batch 2 contract below defines targeted, idempotent cookie-based revocation. |
| Change password | Authenticated eligible User | Dedicated security operation; satisfy approved checks and protect credentials. Revoke other refresh sessions while preserving the current one; password policy remains DEFERRED. |
| Forgot password | Visitor/account holder unable to authenticate | Neutral recovery response; no password change here and no claim of delivery after failure. Delivery/security details remain DEFERRED. |
| Reset password | Valid reset-credential holder | Validate purpose, account, expiry and usability; successful consumption prevents reuse. Revoke all refresh sessions on successful reset. |
| Current account/profile | Authenticated eligible User | Subject comes from backend identity. Return only permitted information; current-account viewing does not grant Admin self-profile editing. |
| Own-profile change | Eligible Student or Teacher | Only permitted own fields; dedicated password security and protected privileges cannot be bypassed. Exact editable DTO remains DEFERRED; email change requires new-email verification before replacing the old email. |

### 3.1 Approved Current Account Contract

- GET /api/v1/users/me: read authenticated account identity only.
- Authority: FR-ACC-007, BR-AUTHN-012, BR-AUTHN-018, UC-AUTH-ME-01.
- Require validated Bearer JWT and current User resolution. Allow STUDENT,
  TEACHER and ADMIN; missing/locked accounts receive safe 401.
- Unverified Students may read this projection only with the persisted Student login
  policy; historical unverified Students remain restricted. Existing Teacher/Admin
  account-only JWT behavior is unchanged, including pending Teachers. This does not
  grant Teacher business eligibility or profile editing.
- No query parameters or non-empty request body; reject with 400 INVALID_REQUEST.
  Authentication/account checks take precedence over request-input validation.
- Success: 200 OK with exactly id (decimal string), role (STUDENT/TEACHER/ADMIN)
  and emailVerified (boolean), derived from the current database-backed principal.
  Never expose profiles, entities, hashes, tokens or internal data.
- Shared ApiError responses: 400 INVALID_REQUEST ("Request could not be processed.");
  401 AUTHENTICATION_REQUIRED ("Authentication required.");
  503 AUTHENTICATION_UNAVAILABLE ("Authentication temporarily unavailable.");
  500 INTERNAL_ERROR ("An unexpected error occurred."). No private values.
  Missing accounts do not return 404; all three roles are allowed.
- Read-only/repeatable, with no pagination or mutation. This GET route and the approved Batch 2 auth routes are permitted;
  other production routes/methods remain default-deny.
- Approved sequencing exception: TASK-009's bounded account-read slice may precede
  TASK-007. This does not complete TASK-009 or authorize other profile/onboarding work.

~~~json
{"id":"123","role":"STUDENT","emailVerified":true}
~~~

RefreshSession is server-authoritative security state, distinct from its credential.
No API exposes its persistence rows or makes them ordinary CRUD resources.
Support approved rotation; replaced credentials are invalidated under that design.
Rotate on every successful refresh with a new configurable seven-day lifetime.
Consume/revoke the old row and insert the replacement atomically at READ COMMITTED
with row locking; exactly one concurrent request wins. No per-user session limit,
family tracking, automatic retry or unrelated-session revocation. Signing occurs
after current eligibility checks and before commit; expose credentials only after
commit. Known rollback preserves the old credential; uncertain commits return no
credentials and lost responses cannot recover a replacement through the old token.
Logout does not imply immediate revocation of every issued Access Token.

Retain configurable defaults: Access Token 15 minutes, Refresh Token 7 days and
password-reset credential 15 minutes. These are not hardcoded storage constraints.
Initial verification uses the approved 24-hour lifetime and resend policy in Section 3.3; other purposes remain deferred.

Validate JWT signature and expiry with supported security facilities. Use supported
password protection; do not invent cryptography or select undocumented encoder
parameters. users.locked is the only blocking mechanism: locked Users cannot
authenticate or perform normal account use. No separate disabled/account_status lifecycle.

### 3.2 Approved Batch 2 Login, Refresh, Logout and Browser Transport

All paths below include /api/v1. These contracts do not approve registration, email
delivery, verification/resend or Teacher onboarding implementation.

| Method/path | Request and authority | Success |
| --- | --- | --- |
| POST /api/v1/auth/login | JSON with exactly required string email/password; canonical email, approved password input; CSRF required, no access JWT required. | 200 access-token response and refresh cookie. |
| POST /api/v1/auth/refresh | Refresh cookie plus CSRF; no query parameters or non-empty body; no access JWT required. | 200 access-token response and rotated refresh cookie. |
| POST /api/v1/auth/logout | Refresh cookie plus CSRF; no query parameters or non-empty body; no access JWT required. | 204 and cleared refresh cookie on confirmed success/no-op. |
| GET /api/v1/auth/csrf | Public bootstrap; no query parameters or non-empty body. | 200 JSON { token: string } and Spring CSRF cookie. |

Login rejects missing/null/blank email, malformed email, invalid password input and
unknown/protected properties with 400 INVALID_REQUEST. No query parameters are
accepted. Email is trimmed before syntax validation; matching remains lowercase(trim(email)).
Password input is never trimmed or normalized. Credentials must not be supplied in URLs.
Access-token response has exactly accessToken, tokenType ("Bearer"), issuedAt and
expiresAt (ISO-8601 UTC). Never return refresh credentials or hashes in JSON.
Credential responses and CSRF bootstrap use Cache-Control: no-store.

Login and refresh require an existing, unlocked User with verified email or the explicit Student-only login policy described below. Pending/rejected
Teacher onboarding does not block account-only authentication and never grants
Teacher business authority. Incorrect credentials and locked/missing accounts use
401 AUTHENTICATION_REQUIRED. Only correctly password-authenticated, unlocked but
unverified login uses 403 EMAIL_VERIFICATION_REQUIRED; ineligible refresh uses 401.

Safe errors retain the shared ApiError shape: 400 INVALID_REQUEST;
401 AUTHENTICATION_REQUIRED; 403 EMAIL_VERIFICATION_REQUIRED or ACCESS_DENIED;
503 AUTHENTICATION_UNAVAILABLE for known transient failures;
503 AUTHENTICATION_OUTCOME_UNKNOWN for uncertain commit; 500 INTERNAL_ERROR.
No response contains private values or internal causes. CSRF/CORS denial precedes
authentication mutations. Refresh failures do not clear a possibly newer refresh cookie.
Clients must not automatically retry login/refresh mutations.

Logout revokes only the identified session, even for a locked account. Missing,
malformed, expired, unknown or already-revoked credentials are idempotent 204.
Database failures are not successful logout. No family revocation: an old rotated
credential cannot revoke its replacement. Existing access JWTs remain valid until
expiry, subject to current-account checks.

Production frontend/API must be same-site HTTPS, not necessarily same-origin.
app.security.browser.allowed-origins is an exact comma-separated frontend-origin
allowlist; no wildcard origins or credentialed wildcard CORS. Empty configuration
permits no cross-origin callers. Approved CORS methods are GET/POST/OPTIONS and
headers Content-Type, Authorization and X-XSRF-TOKEN. Credentials are enabled.

Refresh cookie name is refresh_token: host-only (no Domain), HttpOnly, SameSite=Lax,
Path=/api/v1/auth and Max-Age bounded by remaining refresh lifetime. Secure is enabled
by default. app.security.browser.local-http=true is an explicit local-development
exception: only HTTP localhost/loopback origins and loopback HTTP API requests are
accepted, never a production Secure=false option. Use a consistent localhost hostname
for frontend/API; mixing localhost and 127.0.0.1 is not a same-site topology.
Example local origins are http://localhost:3000 frontend and http://localhost:8080 API;
the frontend origin must be explicitly configured, not implicitly trusted.
Actual production origins and trusted reverse-proxy HTTPS handling are deployment
configuration; a cross-site topology requires a new decision, not SameSite weakening.

Spring CookieCsrfTokenRepository stores a host-only HttpOnly XSRF-TOKEN cookie with
the same secure/site/path rules. Bootstrap returns the BREACH-masked token in JSON,
so cross-origin frontend code need not read the API-host cookie. Fetch uses
credentials: include; mutations submit that returned token in X-XSRF-TOKEN.
Require the header plus matching cookie; no form/query-token fallback.
Protect unsafe methods throughout /api/v1/auth/**, including future cookie-backed
auth mutations. Separate protected API routes remain stateless header-Bearer only.
Access JWTs stay in frontend memory, never localStorage/sessionStorage; no frontend
implementation is included in Batch 2. Do not add social login or OAuth flows.
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

### 3.3 Student Registration and Initial Email Verification — Batch 3

All three operations use Batch 2 HTTPS/local-loopback, exact CORS allowlist,
CSRF cookie/header and no-store policies. They require JSON, accept no query
parameters and reject unknown/protected properties. No access JWT is required.
No access/refresh credentials, hashes or User entities are returned.

| Method/path | Exact request DTO | Success |
| --- | --- | --- |
| POST /api/v1/auth/register/student | Required string email, password, fullName, username, phone. | 201 {"verificationRequired":false}. |
| POST /api/v1/auth/email-verification | Required nonblank string token; INITIAL_VERIFICATION only. | 204, no body. |
| POST /api/v1/auth/email-verification/resend | Required string email. | 202, no body; also for unknown, locked, already-verified or non-initial-verification accounts. |

Email is trimmed before syntax validation and lowercased with Locale.ROOT before
storage/lookup; no provider-specific normalization. fullName is nonblank, rejects
PostgreSQL-forbidden NUL and is otherwise preserved. Current V1 TEXT has no approved
fixed name-length limit; retired profile limits are not reinstated. Password is
preserved exactly, with the approved 8–64 Unicode-code-point/72-UTF-8-byte limits and
BCrypt cost 12. Backend creates unlocked, unverified STUDENT accounts. No public
Admin registration, Teacher registration or onboarding changes are included.

New Student registration persists User with student_login_without_verification=true and email_verified=false in one READ COMMITTED transaction. It creates no verification credential, sends no email and issues no authentication tokens. The browser navigates to /login with a Vietnamese success notice. Teacher registration and historical initial-verification flows retain the credential protocol below.
Canonical-email uniqueness prevents duplicate overwrites, including races.
Credentials use 32 SecureRandom bytes, canonical unpadded Base64URL and a
purpose-separated SHA-256 lowercase hexadecimal hash. Only the hash, User, canonical
target email, purpose, expiry and single-use metadata are persisted. Lifetime is
24 hours; now >= expires_at is expired. Raw credentials are accepted in the body,
never in URLs, API responses or logs.

Verification locks User before reading mutable credential state, checks current
email, purpose, expiry, unused state, lock and verification status, then atomically
consumes the credential and sets email_verified. One concurrent verification wins.
Existing login eligibility then permits verified/unlocked accounts; verification
never changes roles or approves Teacher onboarding.

Resend uses the same User lock. Cooldown is 60 seconds since the last committed
issuance, including registration. Five accepted resends per rolling 24 hours are
allowed: SMTP failures count; initial registration and rejected requests do not.
The earliest INITIAL_VERIFICATION row is registration; retain credential history
for rate accounting. An entry exactly at the window start is outside the window.
Earlier unexpired credentials remain usable until verification succeeds; afterwards
current account state prevents further initial-verification use. No per-IP or
production-grade distributed abuse protection is claimed.

SMTP delivery occurs after confirmed commit only. Mail contains a verification
code and expiry notice; no frontend verification-link route is invented. Configure
SMTP_HOST, SMTP_PORT (default 587), SMTP_USERNAME, SMTP_PASSWORD and SMTP_FROM via
runtime environment. Boot-managed mail support uses authenticated STARTTLS,
required TLS, server identity validation, bounded timeouts and disabled mail debug.
No credentials/fallback sender are committed. Failed delivery retains the
unverified account and accepted credential so resend can recover after cooldown.
There is no durable delivery queue or automatic mail/transaction retry.

Safe errors: 400 INVALID_REQUEST; 400 INVALID_VERIFICATION_CREDENTIAL;
409 REGISTRATION_CONFLICT; 429 VERIFICATION_RATE_LIMITED;
503 VERIFICATION_DELIVERY_UNAVAILABLE (explicitly states persisted account is not
rolled back and resend is available); 503 AUTHENTICATION_UNAVAILABLE;
503 AUTHENTICATION_OUTCOME_UNKNOWN; 500 INTERNAL_ERROR. No internal causes/private
values are returned. Uncertain commit returns no success and sends no email.
Generic resend no-ops reduce disclosure, but rate-limit/delivery responses do not
claim perfect account-enumeration resistance.

References: FR-STU-001, FR-STU-002, FR-ACC-008, FR-ACC-009, FR-ACC-016,
NFR-SEC-001, NFR-SEC-004, NFR-SEC-015; BR-AUTHN-002, BR-AUTHN-006,
BR-AUTHN-017, BR-AUTHN-020; UC-AUTH-REGISTER-01, UC-AUTH-VERIFY-EMAIL-01,
UC-AUTH-LOGIN-01. Email change, password recovery and frontend flows remain separate.

---

### 3.4 Approved Teacher Registration — TASK-032 bounded slice

POST /api/v1/auth/register/teacher uses the same public, JSON-only, CSRF-protected,
configured CORS/transport and no-store boundary as Student registration. No query
parameters are accepted. Exact required string fields: email, password, fullName,
specialization, teachingExperience and introduction. Reject unknown/protected fields,
including role, status, teacherId, emailVerified and review metadata.

Reuse Section 3.3 email/password/fullName validation unchanged. Application fields
are non-null, nonblank strings without NUL characters. Preserve supplied application
text exactly, including surrounding whitespace; impose no invented fixed lengths.
Success is 201 {"verificationRequired":true}, with no access/refresh credentials.
Use Section 3.3 safe input, REGISTRATION_CONFLICT, persistence/commit and delivery
error mappings; Student/Teacher registrations share canonical email uniqueness.

Atomically persist an unlocked, unverified TEACHER User, hashed INITIAL_VERIFICATION
credential and PENDING Teacher application. Backend assigns teacher_id, status,
submitted_at/created_at/updated_at; review fields remain null. Application failure
rolls back User and verification credential too. No TeacherProfile is created.
SMTP runs only after commit; failure retains all three records for shared resend
recovery. Existing 24-hour verification and resend limits apply unchanged.

Verification never approves the application. Verified pending/rejected Teachers
may authenticate for account-only access under Batch 2; default-deny Teacher business
routes remain denied. Role alone is not business eligibility. This slice does not
implement review, approval/rejection, resubmission, status lookup, profile creation
or Course management. Section 3.5 separately approves bounded Admin review/profile creation;
resubmission and Course management remain outside that slice. Section 3.6 separately defines own onboarding status.

References: FR-TEA-005, FR-ACC-016, FR-AUTH-009, NFR-SEC-004;
BR-AUTHN-024, BR-AUTHN-020, BR-AUTHN-006; UC-AUTH-REGISTER-TEACHER-01.

---

### 3.5 Approved Admin Teacher Application Review — TASK-032 bounded slice

All three operations require a valid Bearer JWT and a current, unlocked, email-verified
ADMIN account. No new reviewer role exists. Authorization precedes request validation;
reviewer identity comes exclusively from authenticated backend context. No cookie-based
review authentication or new CSRF/CORS policy is introduced. Other production routes
remain default-deny. IDs use positive BIGINT-compatible decimal strings; timestamps use
ISO-8601. Unknown/protected fields and invalid scalar types are rejected.

| Method / route | Request | Success |
| --- | --- | --- |
| GET /api/v1/admin/teacher-applications/{applicationId} | No query parameters or non-empty body. | 200, no-store, TeacherApplicationDetail. |
| POST /api/v1/admin/teacher-applications/{applicationId}/approve | No query parameters or non-empty body. | 204, empty response after commit. |
| POST /api/v1/admin/teacher-applications/{applicationId}/reject | No query parameters; exact JSON {"reason":"..."}. Required nonblank, NUL-free string, preserved without arbitrary length limits. | 204, empty response after commit. |

TeacherApplicationDetail contains exactly id, teacherId, specialization,
teachingExperience, introduction, status, submittedAt, reviewedBy, reviewedAt and
rejectionReason. reviewedBy is a decimal-string User ID, not an embedded User record;
review metadata is null while pending and rejectionReason is null on approval.
No credentials, email/password data, tokens or other User/profile fields are exposed.

Only PENDING applications for existing current TEACHER accounts can transition to
APPROVED or REJECTED. Applicant verification/lock does not prevent approval; review does
not verify/unlock accounts. Approval inserts TeacherProfile from the three snapshot fields
with null avatar_path in the same transaction as review metadata. Never overwrite a profile.
Reviewed application history is immutable. Services lock involved User rows in ID order,
then the application, recheck current state and perform one transaction. Exactly one
concurrent review wins; no automatic retry. A known failure rolls back all writes; a commit
failure never returns success, and an uncertain commit must be inspected rather than retried
as though rollback were proven.

| Failure | HTTP / safe code |
| --- | --- |
| Missing/invalid Bearer JWT; missing/locked current account | 401 AUTHENTICATION_REQUIRED |
| Non-ADMIN or unverified reviewer | 403 ACCESS_DENIED |
| Invalid request | 400 INVALID_REQUEST |
| Unknown application after authorization | 404 APPLICATION_NOT_FOUND |
| Repeated or losing concurrent review | 409 APPLICATION_ALREADY_REVIEWED |
| Unexpected existing TeacherProfile | 409 TEACHER_PROFILE_CONFLICT |
| Applicant no longer has current TEACHER identity | 409 APPLICATION_NOT_REVIEWABLE |
| Current-account authentication lookup unavailable | Existing 503 AUTHENTICATION_UNAVAILABLE |
| Review persistence/transaction or unexpected internal failure | Existing safe 500 INTERNAL_ERROR; no internal/private details |

Teacher business authorization uses current database role, email verification, unlocked
state and persisted APPROVED application. Profile existence alone is insufficient. Pending
and rejected Teachers retain approved account-only authentication. This introduces no Course
API or other Teacher business permission; the reusable eligibility check must accompany
operation-specific ownership/authorization when those routes are approved.

The backend slice is complemented by the approved browser interface in Section 3.9. Resubmission is separately approved in Section 3.8; own status in Section 3.6;
the bounded pending queue is approved in Section 3.7.
References: FR-TEA-005, FR-ACC-016, FR-AUTH-009, NFR-SEC-004;
BR-AUTHN-024, BR-AUTHN-018, BR-AUTHN-020, BR-PROFILE-002;
UC-AUTH-REGISTER-TEACHER-01; DM-TEA-001.

---

### 3.6 Approved Teacher Own Onboarding Status — TASK-032 bounded slice

GET /api/v1/teacher/onboarding requires a valid Bearer JWT and a current, unlocked,
email-verified TEACHER account. Approved onboarding is not required. Resolve identity
only from the authenticated account; do not accept userId, teacherId or applicationId.
No query parameters or non-empty request body are accepted, consistent with existing
read-only account endpoints. Authentication/role checks precede input validation.

Select only that Teacher's latest application using submittedAt DESC, id DESC.
Return 200 with Cache-Control: no-store and exactly these JSON properties:

- applicationId: exact decimal-string BIGINT identifier.
- status: PENDING, APPROVED or REJECTED.
- submittedAt: ISO-8601 absolute timestamp.
- reviewedAt: ISO-8601 absolute timestamp, null for PENDING.
- rejectionReason: preserved rejection text where applicable, otherwise null.

Exclude reviewer identity, other account/application fields, credentials and tokens.
No application returns 404 ONBOARDING_APPLICATION_NOT_FOUND. Missing/invalid/expired
JWT or a missing/locked account uses existing 401 AUTHENTICATION_REQUIRED; Student,
Admin and unverified Teacher access returns 403 ACCESS_DENIED. Forbidden request input
returns 400 INVALID_REQUEST. Reuse safe authentication lookup 503 and generic internal
500 handling without SQL/private details. Invalid authentication never performs the lookup.

The service uses a read-only transaction and rechecks current account role/verification/
lock state. It does not mutate applications, create a profile, issue tokens or grant Teacher
business permissions. Current TeacherBusinessEligibility and operation-specific ownership
requirements remain unchanged. Pending/rejected Teachers may view their own status when
verified/unlocked. No queue, resubmission, history-list or frontend contract is introduced.

References: FR-TEA-005, FR-AUTH-009, NFR-SEC-004; BR-AUTHN-018, BR-AUTHN-020,
BR-AUTHN-024; UC-AUTH-REGISTER-TEACHER-01; DM-TEA-001.

---

### 3.7 Approved Admin Pending Teacher Applications Queue — TASK-032 bounded slice

GET /api/v1/admin/teacher-applications/pending requires a valid Bearer JWT and a current,
unlocked, email-verified ADMIN account, using the same reviewer checks as Section 3.5.
Authentication/authorization precedes input validation. No new role or browser policy.

Accept only optional page (default 0) and size (default 20). Both are single unsigned
decimal-integer query values within Java int range; page is zero-based and size is 1–100.
Reject blank, signed, fractional, whitespace-padded, repeated, overflowing or unknown
parameters and non-empty request bodies with 400 INVALID_REQUEST. Reject page * size
greater than Integer.MAX_VALUE before persistence access; never overflow or silently coerce.

Backend fixes status = PENDING and submittedAt ASC, id ASC. Applicant email verification
and lock state do not filter the queue. Return 200 with Cache-Control: no-store and exactly
items, page, size, hasNext. Each item contains only id, teacherId (decimal-string IDs),
status (PENDING) and submittedAt (ISO-8601 timestamp). Full application text remains in
the existing detail operation; no User/profile fields, review metadata or credentials.
Empty/out-of-range pages return items: [] and hasNext: false, not 404.

Fetch size + 1 rows and expose at most size; no count query, row locks, applicant User
lookups or generic pagination framework. Use a read-only transaction. This is not a
snapshot or exclusive review assignment: concurrent reviews can remove rows and shift
later offset pages. Listing never guarantees review eligibility. Existing mutation locks,
state checks and 409 APPLICATION_ALREADY_REVIEWED behavior remain authoritative.

Reuse 401 AUTHENTICATION_REQUIRED for missing/invalid JWT or missing/locked current account;
403 ACCESS_DENIED for non-ADMIN/unverified reviewers; 503 AUTHENTICATION_UNAVAILABLE for
authentication lookup unavailability; safe 500 INTERNAL_ERROR for queue persistence or
unexpected failures. Never disclose SQL or private values. Other routes remain default-deny.

References: FR-TEA-005, FR-ACC-016, NFR-SEC-004, NFR-PERF-004;
BR-AUTHN-024, BR-AUTHN-018, BR-AUTHN-020; UC-AUTH-REGISTER-TEACHER-01.

---

### 3.8 Approved Teacher Application Resubmission — TASK-032 bounded slice

POST /api/v1/teacher/applications/{applicationId}/resubmit requires validated Bearer JWT,
current TEACHER role, verified email and an unlocked account, not approved business eligibility.
The positive decimal-string applicationId identifies the rejected snapshot being superseded;
ownership comes only from authenticated identity. Authentication/authorization precedes validation.
Accept no query parameters and exactly the required JSON strings specialization,
teachingExperience and introduction. Reject null, blank, NUL, invalid types, unknown/protected
properties and invalid IDs; preserve text without trimming or arbitrary length limits.

Only the latest owned REJECTED application (submittedAt DESC, id DESC) is eligible.
Reject any PENDING application, prior APPROVED onboarding or existing TeacherProfile.
No cooldown or lifetime attempt limit. Missing/cross-owner target returns 404 APPLICATION_NOT_FOUND;
pending state returns 409 APPLICATION_ALREADY_PENDING; stale/non-rejected target or prior approval
returns 409 RESUBMISSION_NOT_ALLOWED; unexpected profile returns 409 TEACHER_PROFILE_CONFLICT.
Ownership is checked before conflict disclosures; pending conflict precedes other eligibility conflicts.
Reuse 400 INVALID_REQUEST, 401 AUTHENTICATION_REQUIRED, 403 ACCESS_DENIED, authentication-lookup
503 AUTHENTICATION_UNAVAILABLE and generic persistence/commit 500 INTERNAL_ERROR without private details.

Lock the current User using the existing account-row lock, recheck eligibility and application
state, then insert a new PENDING snapshot in one READ COMMITTED transaction. Never alter any
reviewed application or its review metadata. Backend supplies timestamps at PostgreSQL precision;
submission time cannot precede the latest snapshot if the clock moves backward. Equal timestamps
use the new identity ID tie-breaker. Preserve the one-PENDING unique index and account-before-
application lock order. Return only after commit: 201 Created, Cache-Control: no-store, the exact
Section 3.6 TeacherOnboardingStatus DTO for the new row (null review timestamp/rejection reason).
No User/profile changes, credentials, verification mail or Teacher business permission are issued.

Concurrent resubmissions have one winner; the loser sees a conflict. No automatic retries or
idempotent replay of the original success. A delayed retry against an older rejected ID cannot
create another application after a newer rejection. Known rollback preserves history; uncertain
commit never reports success and callers can inspect own onboarding status before further action.
Existing Admin review and pending-queue behavior remains unchanged. No schema change or frontend.

References: FR-TEA-005, FR-ACC-016, NFR-SEC-004; BR-AUTHN-024, BR-AUTHN-020;
UC-AUTH-REGISTER-TEACHER-01.

---

### 3.9 Approved bounded browser authentication and Teacher onboarding

The /account Next.js workspace implements the approved TASK-007 frontend prerequisite
and TASK-032 Teacher journey. Shared login and explicit session recovery use Section 3.2;
Student registration and verification/resend use Section 3.3; Teacher registration uses Section 3.4.
Access credentials and current-account state stay in component memory only. Reloading
requires explicit recovery or login; there is no automatic refresh or mutation replay.
Auth mutations bootstrap CSRF, include cookies and send X-XSRF-TOKEN. Protected User
and onboarding calls use Bearer headers with cookies omitted. No credential is placed
in browser storage, URLs, logs or a client-created cookie.

Teacher registration creates the initial application; Student registration sends email, password, fullName, username and phone. Neither signs in. Verification codes
are entered from email, never read from query strings. Pending/rejected verified Teachers
can sign in for account-only access. Current role/verification comes from /users/me;
onboarding and mutation authorization remain backend-authoritative. No separate initial
application endpoint is introduced. Resubmission uses Section 3.8 without prefilled
historical application text, which the status projection does not expose.

The workspace displays PENDING/APPROVED/REJECTED, rejection reason and safe errors.
Submission locks immediately against repeated clicks. Any failed/uncertain resubmission
requests latest status without replaying the mutation; no subsequent submission is enabled
until a status read succeeds and returns REJECTED. Backend rechecks all remaining eligibility.
Logout clears local credential/account data and query cache immediately; only a confirmed
204 reports server logout. An explicit Clear server session action can retry logout after
failure. No automatic retry or claim of global access-token revocation is introduced.

This bounded responsive workspace does not implement
password recovery, Teacher profile/Course management or the complete product navigation.
Verified ADMIN accounts can explicitly load the Section 3.7 PENDING queue at size 20,
using zero-based previous/next pages and backend ordering. Empty results are not an error;
concurrent review can shift offset pages. The Section 3.5 detail projection displays only
approved application/review fields, rendering supplied text without HTML interpretation.
Approval sends an empty POST; rejection sends only the nonblank, NUL-free preserved reason.
Review controls appear only for a freshly read PENDING detail. A synchronous request guard
prevents duplicate clicks; successful review refreshes the queue. Failed/uncertain review
re-reads detail and queue without replaying the mutation; failed reconciliation removes
review controls. Current backend authorization and transactional checks remain authoritative.
No queue count, arbitrary status filter, profile edit or extra reviewer role is introduced.
Real deployment must retain the configured exact-origin, same-site and TLS requirements.
Browser tests with intercepted responses validate client contracts, not live SMTP, CORS or
full browser-to-backend/database integration.

---
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


### Teacher Certificate Contract Planning

**APPROVED RESPONSIBILITY:** FR-CERT-001, FR-CERT-002, FR-CERT-003;
BR-CERT-001, BR-CERT-002, BR-CERT-003; UC-CERT-OWN-01, UC-CERT-REVIEW-01,
UC-CERT-PUBLIC-01; DM-CERT-001.
These are separate TASK-046 responsibilities, not TASK-005 completion.

| Operation responsibility | Authority / DTO boundary |
| --- | --- |
| Add certificate | Eligible Teacher; name and validated image/PDF evidence reference only. Backend sets owner and PENDING. |
| List own certificates | Authenticated eligible Teacher; own records and verification status only. |
| Delete/replace certificate | Owning eligible Teacher; replacement resets PENDING atomically. No owner/status binding. |
| List pending/retrieve review evidence | Authorized Admin; PENDING review projection and controlled evidence access. |
| Verify/reject pending certificate | Authorized Admin; approve only the actual reviewed current evidence, reject stale review. |
| Public profile certificates | Student/anonymous reader of an eligible public profile; VERIFIED-only projection/evidence. |

**DEFERRED CONTRACT:** Exact methods/routes, payload/null/omission semantics,
pagination/filter/sort, success/error statuses, retry identity, review concurrency
and evidence upload/delivery. Do not copy persistence rows as DTOs. Reuse approved
Section 19 conventions. Do not return raw file paths as unrestricted public access,
private review data or expose pending/rejected evidence through another route.
Certificate visibility is approved; general Teacher-profile discoverability remains
G-DISCOVERY. G-FILES covers evidence delivery/cleanup and cache/link invalidation.
No product endpoint, upload implementation or UI is introduced by this planning.

## 7. Course

Every Course has one non-transferable owning Teacher and exactly one Category. Used
Categories are retained and deactivated. Course statuses are DRAFT, PUBLISHED, COMPLETED,
CANCELLED and ARCHIVED; teaching in progress remains PUBLISHED. Teacher explicitly completes
a Course after backend validation. Cancellation and archival are distinct. Optional
percentage-discount windows are supported; Payments retain their price snapshots.

Teachers retain authorized own-Course content management. Public, Teacher and Admin
DTOs have separate visibility purposes. Backend resolves owner; requests cannot
transfer it. The approved draft-only operation contract follows. Publication,
completion prerequisites and Meet supply timing remain DEFERRED.

### TASK-010 Phase 1 — Teacher-owned Draft Course API

Bearer-only; reuse TeacherBusinessEligibility (current TEACHER role, verified email,
unlocked account and approved onboarding). Identity comes exclusively from the principal.
Student/Admin roles cannot call these routes. Every detail query/update includes the
owning Teacher ID; foreign and missing Courses both return COURSE_NOT_FOUND.

- POST `/api/v1/teacher/courses`: create DRAFT, return 201, detail DTO and Location.
- GET `/api/v1/teacher/courses`: page=0, size=20 (1–100), optional status from
  DRAFT/PUBLISHED/COMPLETED/CANCELLED/ARCHIVED. All owned states by default, ID ascending.
  Response `{items,page,size,hasNext}`; no total count.
- GET `/api/v1/teacher/courses/{courseId}`: return owned detail in any existing state.
- PATCH `/api/v1/teacher/courses/{courseId}`: update only DRAFT; return detail, 200.

Reject duplicate/unknown list query keys, invalid/overflowed pagination, GET bodies,
and query parameters on detail/mutation routes. Success responses use no-store.
IDs are positive decimal-string ApiId values within BIGINT; counts are JSON integers.

POST requires categoryId, name, description, tuition, minStudents, maxStudents,
plannedStartDate, timezone and sessionCount. Optional discount defaults to null.
PATCH permits these same fields; omitted values are preserved and the complete merged
state is validated. Reject empty updates, wrong types, unknown/protected fields and
explicit null except discount. Nested objects are complete replacements with exact keys.

| Field | Approved Phase 1 validation |
| --- | --- |
| name | ASCII-space trim, nonblank, 1–200 Unicode code points; no ISO control characters or malformed UTF-16 |
| description | Plain text, ASCII-space trim, nonblank, 1–10,000 Unicode code points; only TAB/CR/LF controls allowed; no malformed UTF-16 |
| tuition | Current implemented API: ApiMoney `{amount,currency}`; amount decimal string, 0 through 9999999999999.99, maximum two fractional digits, no rounding/exponent; currency exactly VND. Approved paid-only policy requires amount > 0; backend/frontend validation updates are pending, not implemented by this documentation change. |
| minStudents/maxStudents | Integers 1 through 2147483647, maximum >= minimum; no arbitrary 30/100 capacity cap |
| sessionCount | Integer 1 through 2147483647; no Sessions generated |
| plannedStartDate | Strict valid YYYY-MM-DD, years 0001–9999; past dates permitted in drafts |
| timezone | Java-supported IANA zone ID, not a numeric offset |
| discount | null or complete `{percent,startAt,endAt}`; decimal-string percent 0 < percent < 100, at most two decimals; explicit-offset timestamps with at most microsecond precision and UTC years 0001–9999; start < end |

PATCH discount null clears the entire window; omission preserves it. No current-time
discount constraint or payable-price calculation is introduced. Money/percent responses
have two fractional digits. Duplicate Course names are allowed.

Detail DTO: `{id,name,status,category:{id,name,active},tuition,plannedStartDate,timezone,
sessionCount,minStudents,maxStudents,createdAt,updatedAt,description,discount}`.
List items omit description and discount. Category data is current, including inactive
retained references. Do not expose Meet URLs, storage paths, private Teacher information,
participants or payment data. Ownership/status/timestamps are not writable inputs.

Writes use READ_COMMITTED and lock User -> existing owned Course -> newly selected
Category. Recheck the locked User snapshot and TeacherBusinessEligibility. Lock the
Category FOR UPDATE and validate active before insert/change; Admin deactivation uses
the same Category lock. If deactivation commits first, reject the new selection; if the
Course write locks first, it may commit before later deactivation. Foreign keys alone
do not enforce this rule. Unchanged Category IDs, including explicitly repeated IDs,
retain inactive references; unrelated edits do not require reselection.

PATCH merges against the locked current Course. Disjoint edits preserve each other;
same-field edits use last-committed-write-wins. No version column or ETag is introduced.
POST has no idempotency key: repeated successful requests create separate drafts.
Clients must not automatically retry mutations; uncertain responses require explicit
reconciliation. No Session generation, publication, Meet/Calendar, Enrollment or Payment
side effect is permitted. Existing excluded columns are preserved during PATCH.

Safe ApiError mapping: INVALID_REQUEST 400; existing AUTHENTICATION_REQUIRED 401 and
ACCESS_DENIED 403; COURSE_NOT_FOUND/CATEGORY_NOT_FOUND 404; CATEGORY_INACTIVE and
COURSE_NOT_DRAFT 409; COURSE_UNAVAILABLE 503 for persistence/transaction failures.
Existing AUTHENTICATION_UNAVAILABLE, REQUEST_REJECTED and INTERNAL_ERROR handling remains.
Messages do not echo SQL, rejected values or private records. No migration is required.

References: FR-TCR-001, FR-TCR-003, FR-TCR-004, FR-TCR-005, FR-TCR-006,
FR-TCR-007, FR-TCR-008, FR-TCR-009;
BR-COURSE-001, BR-COURSE-002, BR-COURSE-003, BR-COURSE-004, BR-COURSE-007;
DM-COURSE-001, INV-002; AR-015.


## 8. Category / Topic

### TASK-034 Phase 1 — bounded Admin Category API

Bearer-only; current verified, unlocked ADMIN required. No refresh-cookie authentication,
public listing, Teacher selection endpoint, Course creation or DELETE operation is added.
GET `/api/v1/admin/categories` accepts only `page` (default 0), `size` (default 20,
1–100) and optional `active=true|false`. Duplicate/unknown query keys, negative/overflowed
pagination and nonempty GET bodies are rejected. Sort is ID ascending. Response is
`{items:[{id:"1",name:"English",active:true,createdAt:"...",updatedAt:"..."}],page:0,size:20,hasNext:false}`.
IDs are decimal-string ApiId; pagination remains numeric. No total count is implied.

POST accepts `{name:"English"}` with optional boolean `active` (default true), returns
201 and the safe Category DTO. PATCH `/{categoryId}` accepts name and/or active, returns
200 and that DTO. Omitted fields are preserved; empty updates, explicit null, wrong types,
unknown fields and mutation query parameters are invalid. Names trim only ASCII spaces,
contain 1–100 Unicode code points and reject ISO control characters and malformed UTF-16.
No Unicode normalization or accent stripping is performed. Database `lower(btrim(name))`
uniqueness reserves inactive names too; PostgreSQL database locale defines lowercasing.

All success responses are no-store. Safe failures use existing ApiError: 400
INVALID_REQUEST, 401 AUTHENTICATION_REQUIRED, 403 ACCESS_DENIED, 404 CATEGORY_NOT_FOUND,
409 CATEGORY_NAME_CONFLICT and 503 CATEGORY_UNAVAILABLE for database failures. Existing
security-filter error conventions apply before controller execution. Unexpected failures
retain the generic 500 handler. SQL or rejected values are not returned.

Mutations recheck and lock the acting User in READ COMMITTED; PATCH also locks the Category
before merging omitted fields. Same-field updates use serialized last-writer semantics.
The V6 unique expression index resolves cross-Admin create/rename races. Course references,
created_at and all unrelated data remain unchanged. V6 is not deployed automatically by
this implementation task; see `TASK034_CATEGORY_BACKEND.md` for the deployment gate.


Each Course selects exactly one Admin-managed Category. Categories use active/inactive;
a used Category is retained and deactivated, not deleted. No hierarchy or automatic
Course reassignment. Admin management DTOs/validation are specified above; Course write-time selection remains deferred.

References: FR-ADM-007, FR-TCR-008; BR-ADM-001; DM-CAT-001.



### TASK-034 — Teacher Category read-only API

GET `/api/v1/teacher/categories` accepts only page (default 0) and size
(default 20, 1–100). Duplicate/unknown parameters, pagination overflow and nonempty
GET bodies are rejected. An active query parameter is not supported: the server
always filters active=true in SQL before pagination, ordered by ID ascending.

Bearer authentication is required. The existing TeacherBusinessEligibility policy
requires current TEACHER role, verified email, unlocked account and approved
onboarding. ADMIN and STUDENT cannot use this endpoint. No cookie authentication,
Teacher mutation endpoint or DELETE is added.

Response is the existing {items,page,size,hasNext} page; each item uses CategoryDto
{id,name,active,createdAt,updatedAt}, with decimal-string ApiId and active=true.
Responses use Cache-Control: no-store. Invalid requests return 400 INVALID_REQUEST;
eligibility denial returns 403 ACCESS_DENIED; database failures return safe
503 CATEGORY_UNAVAILABLE. Existing security-filter 401/403 behavior is unchanged.

This endpoint is a read-time selection view, not authorization to save a Course.
Future Course create/update transactions must recheck Teacher eligibility, ownership
and selected Category eligibility at write time, including concurrent deactivation.
Deactivation must not delete a Category or rewrite existing Course references.
No Course logic, Teacher UI, schema change or migration is included in this slice.

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
Phase 1 weekly rules and initial generation are implemented under the following
approved contract. Teacher Session reads/content are implemented in Phase 2 below.
Individual rescheduling/history and cancellation are implemented in Phase 3 below.
Regeneration and publication remain outside these phases. No attendance API or
RESCHEDULED status.

### 9.1 Teacher weekly rules and initial generation (TASK-037 / TASK-035 Phase 1)

Base path: `/api/v1/teacher/courses/{courseId}`. Bearer-only authentication,
current TeacherBusinessEligibility and database-enforced Course ownership apply
to every operation. Successful responses use `Cache-Control: no-store`; all IDs
are decimal strings. Cookie-backed authentication/CSRF behavior is unchanged.

- GET `/schedule`: no body/query; returns `{courseId, plannedStartDate, timezone,
  sessionCount, generatedSessionCount, rules}`. Each rule is
  `{id, dayOfWeek, startTime, endTime}`, ordered by ISO weekday then start time.
- PUT `/schedule`: accepts only `{rules:[{dayOfWeek,startTime,endTime}]}` and
  returns the schedule representation (200). Replaces the complete rule set only
  for an owned DRAFT with no Sessions. An identical canonical set preserves IDs
  and timestamps. Rules are nonempty, at most 100, with integer ISO weekdays 1–7
  and strict minute-precision `HH:mm`. Start must precede end on the same date.
  Duplicate/overlapping intervals are rejected; adjacent half-open intervals are
  allowed. Unknown fields, null elements and malformed types are rejected.
- POST `/sessions/generate`: no body or query parameters. Returns 201 with
  `{courseId, generatedSessionCount}` and Location
  `/api/v1/teacher/courses/{courseId}/sessions`, implemented in Phase 2 below.
  No Session array is returned by generation.

Generation requires an owned DRAFT, no existing Sessions, valid planning metadata
and valid nonempty rules. The synchronous limit is 1,000 Sessions; this does not
change Course creation or persisted integer constraints. Each weekly stream starts
on its first matching date on/after plannedStartDate; a priority queue merges the
streams and advances the selected stream by seven days. Exactly sessionCount
occurrences are numbered chronologically 1..N. The Course IANA timezone resolves
each local endpoint explicitly: zero or multiple valid offsets are rejected.
The first occurrence must start strictly after the injected backend Clock time;
past occurrences are never skipped. Concrete overlaps and invalid end instants
are rejected. All occurrences are validated before insertion and all inserts
commit atomically. Initial status is SCHEDULED; title, content and cancellation
reason are null.

Transactions lock the acting User, then the owned Course, before dependent reads
or writes. This serializes generation, rule replacement and Course PATCH, including
duplicate generation. Schedule reads also hold these locks for a coherent snapshot.
Once any Session exists, PATCH changes to plannedStartDate, timezone or sessionCount
return `COURSE_SCHEDULE_LOCKED`; unchanged values and unrelated valid Draft edits
remain permitted. No automatic mutation retries are performed.

Errors follow the existing safe ApiError envelope:

| Status | Code | Meaning |
|---|---|---|
| 400 | INVALID_REQUEST | Invalid payload, query, ID, rules or planning metadata |
| 401 | AUTHENTICATION_REQUIRED | Missing/invalid authentication under existing security mapping |
| 403 | ACCESS_DENIED | Wrong role or current Teacher eligibility denied |
| 404 | COURSE_NOT_FOUND | Missing or non-owned Course |
| 409 | COURSE_NOT_DRAFT | Write attempted outside DRAFT |
| 409 | COURSE_SCHEDULE_LOCKED | Rule replacement or changed planning field after generation |
| 409 | SESSIONS_ALREADY_GENERATED | Repeated initial generation |
| 409 | SCHEDULE_OVERLAP | Duplicate/overlapping rules or concrete occurrences |
| 409 | SCHEDULE_TIME_CONFLICT | Past/equal-now start, DST gap/ambiguity or invalid instant ordering |
| 422 | SCHEDULE_GENERATION_LIMIT | Stored sessionCount exceeds 1,000 |
| 503 | COURSE_UNAVAILABLE | Existing sanitized Course persistence/transaction failure mapping |

Clients must explicitly recover uncertain mutation outcomes; never automatically
retry writes. No migration, publication, Calendar/Meet operation, Enrollment or
Payment is introduced by this slice.

References: FR-SES-001, FR-SES-002, FR-SES-003, FR-SES-004,
FR-SCH-001, FR-SCH-002; BR-SES-001, BR-SES-002, BR-SCH-001;
DM-SES-001, DM-SCH-001, INV-017, INV-020.


### 9.2 Teacher Session listing, detail and content (TASK-035 Backend Phase 2)

Base: `/api/v1/teacher/courses/{courseId}`. All three operations require header
Bearer authentication, current TeacherBusinessEligibility and actual Course
ownership. A Session must belong to the route Course; client IDs are lookup inputs,
never authorization. Successful responses use `Cache-Control: no-store`.

- GET `/sessions`: page=0, size=20 (1–100), optional status SCHEDULED/CANCELLED;
  order by sessionNumber ascending. Return `{items,page,size,hasNext}`. Each summary
  contains only `{id,courseId,sessionNumber,title,scheduledStartAt,scheduledEndAt,status}`.
  IDs are decimal strings, timestamps are ISO-8601 UTC instants, title may be null.
  Unknown/duplicate query parameters, invalid values and request bodies are rejected.
- GET `/sessions/{sessionId}`: no query/body; summary fields plus nullable content,
  nullable cancellationReason, createdAt and updatedAt. Owned reads are permitted
  in all existing Course and Session lifecycle states.
- PATCH `/sessions/{sessionId}`: no query; accepts only title and/or content, with
  at least one field present. Omitted values are preserved; explicit null clears.
  Non-null text is trimmed only for leading/trailing ASCII spaces, must not be
  blank, and is limited to 200 title / 10,000 content Unicode code points. Malformed
  UTF-16 and ISO control characters are rejected; content alone permits tab, CR and
  LF for plain-text formatting, consistent with existing Course description rules.
  Content is stored/returned as plain text, never interpreted as HTML; future UI
  must render it as text. Return the updated detail (200).

PATCH permits only SCHEDULED Sessions under DRAFT/PUBLISHED Courses. It locks the
acting User, owned Course, then nested Session, reads the current state under lock,
and writes only title/content/updated_at. Scheduling instants, numbering, status,
cancellation reason, creation time and parentage remain unchanged. Concurrent
updates serialize: different omitted fields merge and the last committed update
to the same field wins. Failures roll back the complete mutation. No automatic
mutation retry is introduced.

Existing 400 INVALID_REQUEST, 401 AUTHENTICATION_REQUIRED, 403 ACCESS_DENIED and
503 COURSE_UNAVAILABLE mappings apply. Missing/non-owned Course is 404
COURSE_NOT_FOUND; a missing Session or nested Course mismatch is 404
SESSION_NOT_FOUND. Noneditable Course/Session state returns 409 SESSION_NOT_EDITABLE.
Errors contain safe generic messages without SQL or private record details.
No DTO exposes Meet URLs, Student/payment records or Calendar credentials.
No migration, rescheduling, cancellation, regeneration, frontend or publication
operation is introduced.

### 9.3 Teacher Session rescheduling, history and cancellation (TASK-035 Backend Phase 3)

Base: `/api/v1/teacher/courses/{courseId}/sessions/{sessionId}`. Existing Bearer-only
current Teacher eligibility, Course ownership and nested Session parentage checks
apply. Successful responses are no-store; all IDs use decimal strings.

- POST `/reschedule`: accepts exactly expectedStartAt, expectedEndAt,
  scheduledStartAt and scheduledEndAt. Values are ISO timestamps with explicit Z
  or ±HH:mm offsets, seconds and at most six fractional digits. New times must
  have zero seconds/fractional seconds, end > start, start strictly after the
  injected backend Clock, and endpoints within the same Course-local date.
  Expected values compare as absolute instants to the locked stored interval;
  stale values return 409 SESSION_CHANGED. Matching expected values plus unchanged
  new times return current detail without a write/history row. Actual changes
  return updated detail (200) and atomically append one history row.
- GET `/schedule-changes`: page=0, size=20 (1–100), sorted by history ID ascending;
  returns `{items,page,size,hasNext}`. Items contain only id, sessionId, oldStartAt,
  oldEndAt, newStartAt, newEndAt, changedAt. No actor/reason fields are invented.
  History remains readable after cancellation and in all owned Course states.
- POST `/cancel`: requires a JSON object, optionally containing reason. Empty
  object, omitted reason or null means no reason. Non-null reason uses ASCII-space
  trimming, nonblank text, at most 1,000 Unicode code points, no malformed UTF-16
  or ISO control characters. Unknown fields/types are rejected. Returns detail
  (200). A valid repeated request on an already CANCELLED Session returns the
  stored result without changing reason or timestamps, including after its start
  or a later Course state change; current authorization still applies.

Initial cancellation and rescheduling require a SCHEDULED Session whose current
start is strictly future and Course state DRAFT/PUBLISHED. Mutations lock User ->
Course -> Session. Rescheduling checks overlap under the Course lock against other
SCHEDULED Sessions only, using half-open intervals; adjacency is allowed. Individual
times need not match weekly rules. Explicit instants determine the local date;
the Phase 1 local-time DST inference algorithm is not used for these offset inputs.
Session ID/number/title/content/status remain unchanged during rescheduling,
even if chronological order changes. updated_at records actual mutations.

Before a reschedule (including its same-time request) or initial cancellation,
any session_calendar_events record causes 409 CALENDAR_SYNC_REQUIRED. No Calendar
record is altered. An already-completed cancellation replay is a read, not a new
Calendar-affecting mutation. Future Calendar writers must coordinate with the same
Session/Course locks; no synchronization worker is implemented here.

Cancellation preserves the row, number, interval and all schedule history; only
status, cancellation_reason and updated_at change. It does not alter Course status
or count, generate replacements, initiate refunds or allow restoration.
History insertion failure rolls back the reschedule. Concurrent same-Session
changes detect stale expectations; concurrent same-Course overlap checks serialize.
No automatic mutation retry is introduced.

Strict payload/query/ID errors return 400 INVALID_REQUEST. Existing authentication,
ownership and nested-not-found errors apply. State denial uses 409
SESSION_NOT_EDITABLE; past/equal-now cutoff uses 409 SCHEDULE_TIME_CONFLICT;
overlap uses 409 SCHEDULE_OVERLAP. Transaction failures retain sanitized 503
COURSE_UNAVAILABLE. POST routes accept no query parameters; history rejects unknown
or duplicate query parameters and request bodies. No migrations or external calls.

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
All Courses require positive tuition and payment; no free activation or fake Payment is permitted.
New Enrollment starts PENDING with seat_reserved_until = min(serverNow + 15 minutes,
firstSessionStart). Renewal is explicit and revalidates eligibility, capacity and cutoff;
duplicate requests never extend a valid reservation.
Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.

Enrollment is distinct from payment. Backend participation and personal-data checks
protect content/work/Meet; listing My Courses or browser success grants no access.
Reservation duration is approved above; operation DTOs remain proposals and unresolved
edge transitions remain OPEN.

References: FR-ENR-004, FR-ENR-005, FR-ENR-006, FR-ENR-007, FR-ENR-008,
FR-TCR-010, FR-ADM-008, FR-PAY-012; BR-ENR-001, BR-ENR-006, BR-ENR-007,
BR-ENR-008, BR-COURSE-007, BR-TEA-003; DM-ENR-001, INV-013, INV-014; AR-018.


### TASK-016 / TASK-022 proposed operations — not yet approved or implemented

Business decisions BR-01..06 are approved on 2026-10-29; the following transport and
DTO choices remain proposals requiring API review. BR-ENR-001 governs state transitions.

| Proposed endpoint | Request | Result |
| --- | --- | --- |
| POST /api/v1/student/courses/{courseId}/enrollment | No body/query; principal supplies Student identity | 201 + Location for new PENDING; 200 existing valid PENDING without extension or ACTIVE relationship; expired PENDING requires explicit renewal. |
| GET /api/v1/student/enrollments | page=0, size=20 (1..100), optional status | Own relationships only. |
| GET /api/v1/student/enrollments/{enrollmentId} | No query | Own detail only. |
| POST /api/v1/student/enrollments/{enrollmentId}/renew | No body/query | Revalidate and reuse expired PENDING; no implicit renewal. |
| POST /api/v1/student/enrollments/{enrollmentId}/cancel | No body/query | Own PENDING cancellation; repeated cancellation returns existing result without overwriting history. |
| GET /api/v1/teacher/courses/{courseId}/enrollments | page, size, optional status | Eligible owning Teacher roster; exact personal-data fields require privacy review. |
| POST /api/v1/student/enrollments/{enrollmentId}/payments | No client amount/account/status | Server creates or returns the one pending Payment and dynamic VietQR information. Exact retry, pricing/rounding and expiry contract remains OPEN. |
| GET /api/v1/student/payments/{paymentId} | No query | Own Payment status and required transfer instructions, not provider secrets. |
| POST /api/v1/teacher/courses/{courseId}/payments/{paymentId}/confirm-manually | Audit evidence request schema OPEN | Owning eligible Teacher attestation; receiver-bank check required; actor/time/method server-set. Not enabled before audit/dispute contract and schema review. |

Automatic evidence ingress is a separate authenticated provider adapter, not a Student
or Teacher status-setting endpoint; route/authentication/replay envelope remains OPEN.
Receiving-account management reuses the owned-account boundary in Section 14; exact
ownership-verification and connection-management operations await provider selection.

Proposed Enrollment DTO: id, courseId, courseTitle, status, enrolledAt,
seatReservedUntil, reservationExpired, cancelledAt, cancellationReason, createdAt,
updatedAt, canAccessContent. IDs are decimal strings; timestamps ISO-8601 UTC;
enrolledAt/reservation/cancellation fields may be null. Proposed enrolledAt means
activation time. Derived flags are not persisted and never substitute for resource
endpoint authorization. No Meet URL, protected content, bank evidence or unrelated
Student information in this DTO. Lists use {items, page, size, hasNext}, ordered by
Enrollment ID descending. Proposed roster fields: enrollmentId, studentId,
studentDisplayName, status, enrolledAt; email and historical visibility remain OPEN.

Use existing Bearer authentication, safe ApiError {code,message,fieldErrors?}, strict
field/query validation and Cache-Control: no-store. Proposed mappings: 400 malformed
request; 401 invalid authentication; 403 wrong role/ineligibility/protected access;
404 missing or non-owned private resource; 409 capacity/cutoff/lifecycle/renewal
conflict; 503 unavailable dependency. Never bind Student identity, receiver, amount,
confirmation status or audit actor from untrusted input. Clients must explicitly
reload after uncertain mutations, not automatically retry or extend reservations.

OPEN: Student verified-email eligibility; cancelled re-entry; completed/cancelled/
refunded/archived access; post-first-start rescheduling; late-payment/refund policy;
provider selection and multi-account support; account verification; manual audit,
dispute and Admin oversight details; Payment expiry versus reservation expiry; final
Enrollment DTO and error contracts. Unresolved lifecycle access remains denied until
approved. Do not implement dependent operations from these proposed routes alone.

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
reconciliation concern. Student/browser claims never confirm Payment. Authorized Teacher manual confirmation
is audited attestation under BR-PAY-009, not independent bank evidence. Automatic
confirmation requires authenticated provider/bank evidence matching the intended receiver,
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

**APPROVED SHARED CONTRACT (TASK-005):** Use /api/v1 and camelCase JSON keys.
All API identifiers are decimal strings at the wire boundary; persistence remains
BIGINT / Java Long, including TeacherProfile's shared User key. Backend validation
is authoritative: reject invalid types/values without silent coercion. Explicit
allowlisted DTOs are required; never serialize JPA entities as public contracts.

Represent money as { amount: decimal string, currency: explicit currency }; never use floating-point money.
NUMERIC(15,2)/BigDecimal, VND-only currency and historical price snapshots are approved.
Discounts require a complete window, 0 < percent < 100 and start < end; tuition
must be greater than zero. The existing zero-tuition implementation requires a later update. Rounding remains an implementation contract; no conversion is added.
Absolute timestamps use ISO-8601 strings with an explicit offset or Z. Weekly local times use
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

The **APPROVED SHARED CONTRACT** for structured errors is:

~~~text
{ code: string, message: string,
  fieldErrors?: { [field: string]: string } }
~~~

Shared boundary codes are INVALID_REQUEST for malformed/invalid requests,
REQUEST_REJECTED for other framework HTTP rejections (preserving their status),
and INTERNAL_ERROR for unexpected failures. Field validation uses safe messages,
never rejected values or internal exception details. This does not choose an
operation-specific 403-versus-404 privacy policy.

TASK-005 provides ApiId and ApiMoney for explicit DTO fields and strict Instant
JSON parsing/ISO serialization; ordinary numeric counts are not globally stringified.
Domain-specific fields, limits, rounding and null/omission rules still require their
own contracts. No production fixture endpoint or product DTO is introduced.

The frontend shared client reuses NEXT_PUBLIC_API_ORIGIN and native fetch,
preserving wire strings without numeric/date conversion. It does not add mutation
retries, JWT/cookie/refresh behavior, redirects, stores or product UI. G-UI remains
open. Test-only fixtures prove shared boundaries, not a completed use-case API.

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
the eventual operation; Section 3.1 fixes current-account success at 200.
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

Section 3.7 approves bounded pagination only for the Admin pending-application queue.
Other collection contracts remain deferred; TASK-005 adds no generic pagination infrastructure.

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

Supabase Storage is the approved file-byte direction, now including Teacher
certificate image/PDF evidence. Exact upload APIs, multipart/presigned delivery,
image processing and bucket/path/access contracts remain unapproved. Storage
integration is not implemented by this documentation.

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

The TASK-005 shared conventions in Section 19 are approved. Exact operation
method/route/DTO/error mappings and pagination remain design work where not approved.
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

TeacherCertificate (DM-CERT-001) extends TeacherProfile through additive V2;
Section 6 owns its separate management/review/public contract planning.

### Complete active use-case coverage

All 47 active use cases map to responsibilities, not invented endpoint contracts.
The references in Sections 3-21 supply targeted governing FR/NFR/INT/BR/INV/AR
authority; this table supplies the actor-goal connection. Decision gates in the
source use cases remain applicable even where the capability is approved.

| Current use case | Responsibility section and remaining contract boundary |
| --- | --- |
| UC-AUTH-REGISTER-01 | 3.3: approved Student registration, validation and immediate login without verified email ownership. |
| UC-AUTH-REGISTER-TEACHER-01 | 3.4–3.9: registration, bounded Admin review, own status, pending queue, resubmission and corresponding browser flows approved; live integration remains to be verified. |
| UC-AUTH-VERIFY-EMAIL-01 | 3.3: initial verification/resend approved; other purposes remain deferred. |
| UC-AUTH-LOGIN-01 | 3: Shared authentication with locked checks; transport details deferred. |
| UC-AUTH-REFRESH-01 | 3: Validated refresh; transport/replay/concurrency deferred. |
| UC-AUTH-LOGOUT-01 | 3: Applicable server-side invalidation; session/transport details deferred. |
| UC-AUTH-CHANGE-PASSWORD-01 | 3: Dedicated secure change; other-session revocation settled; password policy deferred. |
| UC-AUTH-FORGOT-PASSWORD-01 | 3: Neutral recovery; delivery/security details deferred. |
| UC-AUTH-RESET-PASSWORD-01 | 3: Single-use recovery authority; all-session revocation settled; password/delivery details deferred. |
| UC-AUTH-ME-01 | 3 and 6: Current identity/profile; permitted fields deferred. |
| UC-AUTH-PROFILE-01 | 6: Eligible own-profile management; exact field/update contract deferred. |
| UC-DIS-TEACHERS-01 | 5-6: Discovery/public teaching information; fields/matching deferred. |
| UC-CERT-OWN-01 | 6: Own certificate add/view/delete/replace; ownership and PENDING reset; exact API/file contracts deferred. |
| UC-CERT-REVIEW-01 | 6: Admin pending review/verify/reject; current-evidence concurrency and delivery deferred. |
| UC-CERT-PUBLIC-01 | 6: VERIFIED-only public certificates; exact projection/delivery and profile eligibility deferred. |
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

### Approved Student registration extension — username and phone (2026-10-14)

New Student registration requires fullName, username, email, phone and password.
Confirm password is frontend-only. Username is trimmed of surrounding ASCII spaces,
validated as 3–30 ASCII characters beginning with a letter and continuing with
letters/digits/underscores, then lowercased with Locale.ROOT. Canonical usernames
are globally unique. Login remains email-only; this slice adds no public username
lookup, editing, or authorization claims.

Phone accepts 0[35789][0-9]{8} or +84[35789][0-9]{8}, after surrounding ASCII-space
trimming. Store +84 form. Phone is required for new Students, nonunique and unverified;
it is not an authentication or recovery factor. No SMS or ownership proof is implied.
Existing accounts and unchanged Teacher/Admin creation retain nullable fields.
V3 adds nullable users.username TEXT with uq_users_username and
ck_users_username_format; users.phone remains unchanged. No fabricated backfill.
Requiredness for new Students is application-enforced; database uniqueness handles
concurrent registrations. Both email and username conflicts use safe generic
409 REGISTRATION_CONFLICT, with complete transaction rollback and no email delivery.
Student creation is atomic and sends no verification email. Teacher User/application/verification credential creation remains atomic; Teacher SMTP runs only after commit.
Deploy V3 before the changed backend and coordinate the frontend release because
old three-field Student registration requests will be rejected.

### Approved new-Student immediate-login policy — 2026-10-15

New Student registration sets users.student_login_without_verification=true while
email_verified remains false. Login eligibility is distinct from proof of email
ownership. V4 adds this NOT NULL boolean with DEFAULT false and a CHECK that only
STUDENT accounts may have it true; existing accounts are not opted in or backfilled.
Username presence never grants eligibility. Login and refresh require an unlocked
account and either verified email or this explicit Student policy. Current-account
JWT resolution also rejects Students who no longer satisfy that policy. Teacher and
Admin authentication/authorization and Teacher verification/onboarding are unchanged.
Student registration creates no verification credential, sends no email, returns
verificationRequired=false and navigates to /login. Email-only login, username/phone
validation and safe generic email/username conflict handling remain unchanged.
V1/V2/V3 are immutable; V4 must precede the coordinated application rollout.


### Approved password recovery OTP contract (TASK-008, 2026-10-15)

- `POST /api/v1/auth/password/forgot` accepts exactly `{email}` and returns an empty,
  neutral `202` with `Cache-Control: no-store`. Canonical email is lowercase(trim(email)).
  Unknown, locked, throttled and SMTP-delivery-failed requests have the same response;
  the UI says instructions may arrive, never that delivery succeeded.
- `POST /api/v1/auth/password/reset` accepts exactly `{email, code, password}` and returns
  empty `204` with no-store. `code` is a six-digit string, including leading zeros;
  password follows the existing unchanged password policy. Confirmation is frontend-only.
- Both endpoints require the existing cookie CSRF token/header and configured CORS origin.
  No access JWT is required. Query parameters and extra request properties are rejected.
- Unlocked accounts of all roles may recover regardless of email-verification state.
  Generate codes with SecureRandom; expire at 15 minutes (`now >= expires_at` is invalid).
  Store HMAC-SHA256 bound to purpose, account ID and microsecond issuance timestamp.
  Required configuration: `app.security.password-reset.hmac-secret` (environment
  `APP_SECURITY_PASSWORDRESET_HMACSECRET`), Base64 encoding of at least 32 independently
  generated random bytes. No fallback; missing/malformed/short configuration fails startup.
  Keep this secret outside the repository and separate from JWT/SMTP credentials.
  Coordinated replacement invalidates outstanding codes; retain the same secret across restarts.
- One accepted request per 60 seconds, five per rolling 24 hours/account, including failed
  deliveries. Only the latest issuance is valid; prior credentials are marked used atomically.
- At most five incorrect attempts per issued code. Lock the User row in READ COMMITTED,
  recheck state, then increment the persistent counter and commit before returning a safe error.
  Exhausted, expired, consumed, wrong and unknown credentials use `400 INVALID_RESET_CREDENTIAL`.
- Successful reset atomically updates the password, invalidates recovery credentials and revokes
  every refresh session. Login/refresh use the same User-first locking order. No automatic retries.
  Email ownership, Student immediate-login policy, role and locked state remain unchanged.
  Existing access JWTs retain their existing expiry/current-account checks; no new JWT revocation
  infrastructure is introduced. Frontend clears its local session and returns to `/login?reset=1`.
- Invalid input uses existing 400 conventions; missing CSRF uses safe 403. Database/unavailable,
  uncertain-commit and internal failures retain the existing safe 503/500 conventions.
- `/forgot-password` and `/reset-password` reuse the website shell and auth client. Email/code
  are entered into the reset form, never placed in URLs or browser persistence. No automatic login.
- Per-IP abuse controls, trusted-proxy policy and production delivery monitoring are required
  deployment work, not claimed implemented. Neutral content is not a constant-time guarantee.


### Approved logged-in password change (TASK-008, 2026-10-16)

`POST /api/v1/auth/password/change` requires a valid Bearer JWT, current-account
refresh cookie and the existing CSRF bootstrap/header. All three account roles use
their existing login eligibility and lock rules. Exact JSON body:
`{currentPassword, newPassword}`; no query parameters or extra properties.
Confirmation is frontend-only. No client-selected account or session ID is accepted.

Success: empty `204`, `Cache-Control: no-store`, no refresh Set-Cookie. The current
refresh credential, row and expiry remain unchanged. Other refresh sessions and
all outstanding recovery OTPs are invalidated atomically with the password update.
The existing BCrypt/password-input policy applies. There is no automatic rotation,
extension, retry or sign-in. Already issued access JWTs retain their existing lifetime
and current-account authorization checks; revoking refresh sessions is not JWT revocation.

Errors: `401 AUTHENTICATION_REQUIRED` for invalid account/session authority;
`403 ACCESS_DENIED` for incorrect current password or CSRF denial;
`400 INVALID_REQUEST` for malformed input, invalid new password or unchanged password;
`503 AUTHENTICATION_UNAVAILABLE` for unavailable persistence;
`503 AUTHENTICATION_OUTCOME_UNKNOWN` for uncertain commit;
`500 INTERNAL_ERROR` for unexpected failures. Responses expose no credentials or internals.
User row locking precedes session locking in one READ COMMITTED transaction, matching
refresh/recovery lock order. The UI under `/account` never retries a mutation automatically.


### Approved Student own-profile slice (TASK-009, 2026-10-16)

- `GET /api/v1/users/me/profile`: authenticated, currently eligible/unlocked STUDENT only.
  No body or query parameters. `PATCH` at the same path accepts a nonempty JSON object
  containing only `fullName` and/or `phone`. Omitted fields remain unchanged; explicit null,
  blank, NUL, non-string and unknown fields are rejected. Identity is principal-only.
- Both return `200`, `Cache-Control: no-store`, exactly
  `{id, fullName, username, email, phone, emailVerified}`. ID is a decimal string;
  historical username/phone nulls remain null. Email, username and emailVerified are read-only.
  Existing `GET /api/v1/users/me` retains its exact three-field contract.
- Full name preserves supplied nonblank text; no invented length cap. Phone reuses the
  registration validator and canonical +84 storage, remains nonunique/unverified. A historical
  null phone may be omitted when editing the name; supplied null/blank is not a clearing operation.
- Header Bearer only; frontend omits cookies. Existing current-account eligibility applies,
  including explicit Student immediate-login policy. No refresh cookie, CSRF bootstrap,
  token rotation or automatic authentication retry is introduced for this business route.
- PATCH locks the User row, rechecks role/eligibility, updates only supplied profile columns
  and updated_at in READ COMMITTED, and returns after commit. Concurrent updates serialize;
  the last committed value wins for the same field. No migration/version column.
- Safe errors: 400 INVALID_REQUEST, 401 AUTHENTICATION_REQUIRED, 403 ACCESS_DENIED,
  503 PROFILE_UNAVAILABLE (profile persistence failure), 500 INTERNAL_ERROR. Existing
  authentication lookup errors retain their established mappings. No automatic mutation retry.
- `/account` provides Student-only view/edit/cancel/save, read-only identity/email status,
  responsive layout and safe Vietnamese messages. Uncertain saves require profile reload
  before another attempt. Change Password remains the existing separate component.
- Avatar, address, birth date, email/username changes and Teacher profile editing are excluded.
  This slice does not complete TASK-009's Teacher profile scope.

## Batch Calendar 01 — Teacher calendar (2026-10-27)

- `GET /api/v1/teacher/calendar/sessions` uses Bearer authentication and current
  TeacherBusinessEligibility. Ownership is derived from the authenticated account.
- Required `from` and `to` are offset timestamps (Z or explicit offset), years 1–9999.
  Require from < to and a maximum 62-day range. Default page=0, size=20; size 1–100.
  Duplicate/unknown query parameters and request bodies are rejected.
- Response: `{items,page,size,hasNext}`. Each item contains decimal-string sessionId
  and courseId, courseTitle, sessionNumber, nullable sessionTitle, startsAt, endsAt,
  courseTimezone and status. No content, Meet URL, payment or Student data is returned.
- Strict interval overlap: scheduled_start_at < to AND scheduled_end_at > from.
  Include CANCELLED Sessions; order by scheduled_start_at then Session ID ascending.
  Pagination uses a bounded size+1 query, not a total count.
- Successful responses use Cache-Control: no-store. Existing Course error mapping
  applies (400 invalid input, 401 authentication, 403 eligibility, safe 503 persistence).
- `/teacher/calendar` reuses the persistent Dashboard and authenticated API client.
  Month/week date buckets use explicitly labelled Asia/Ho_Chi_Minh time; each card
  displays its own Course timezone. Pagination is visible and never silently truncated.
