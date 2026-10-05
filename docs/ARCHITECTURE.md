# English Tutoring Platform — System Architecture

**Version:** 0.2
**Status:** Reconciled architecture direction; implementation and policy gates remain explicit.

---

## 1. Authority, Purpose and Architecture Status

This document describes the architecture for **Development of an English Tutoring
Platform**. It translates approved tutoring requirements into responsibility and
trust boundaries; it does not select unresolved business policies or authorize
implementation.

Authority order is AGENTS.md, PROJECT_SPEC.md, REQUIREMENTS.md, BUSINESS_RULES.md,
USE_CASES.md and DOMAIN_MODEL.md.
The current reconciled domain model takes precedence over historical architecture
assumptions.

Project-local skills provide subordinate working guidance for applying the canonical
documentation. They do not override or redefine the canonical project documents.

Three kinds of information must remain distinct:

| Kind | Current meaning |
| --- | --- |
| Implemented foundation | TASK-001 backend foundation, TASK-002 frontend foundation and TASK-003 PostgreSQL connectivity have recorded completed verification. Preserve their implementation and evidence. |
| Approved architecture direction | The modular monolith, feature organization, backend authority and tutoring boundaries below guide future work; they do not imply product features exist. |
| Deferred decisions | Section 18 lists policies and mechanisms requiring approval before dependent design or implementation. |

Spring Security/JWT and controlled migrations are approved directions. The current
backend does not yet implement authentication or install Spring Security or Flyway.
Product capability modules, payment integration and Calendar integration are not
implemented. Documentation reconciliation does not change live implementation status;
FEATURE_STATUS.md remains its authority.

The approved Physical V1 defines 22 tables; G-PHYSICAL is RESOLVED. Migration
implementation requires separate approval.
Documentation approval does not reopen completed foundation tasks or implement features.

---

## 2. Architecture Goals and Constraints

Use a maintainable modular monolith with clear business responsibilities and a
responsive browser frontend. Favor demonstrable correctness, privacy, authorization,
testability and understandable code over speculative infrastructure.

- Spring Boot owns authentication, authorization, business rules and persistence.
- Feature boundaries follow cohesive use cases, not one module per domain concept.
- Controllers handle HTTP concerns; services orchestrate authorized business actions.
- Repositories handle persistence, and DTOs define external contracts.
- Students use a mobile-first, touch-friendly web experience; Teacher and Admin
  management is desktop-oriented while remaining usable on smaller screens.
- No native application is required. Exact screens and interactions await UI/UX design.
- Preserve Java 17 and existing technical names, packages, artifacts and databases.
- Do not introduce microservices, distributed transactions, event buses, queues or
  caches without demonstrated requirements.

References: NFR-MNT-001, NFR-MNT-003, NFR-MNT-005, NFR-MNT-006, NFR-MNT-007,
NFR-MNT-008, NFR-MNT-010; BR-WEB-001, BR-WEB-002, BR-WEB-003, BR-WEB-004.

---

## 3. System Context

Students discover Teachers/Courses, purchase entire Courses and participate through
authorized Enrollment. Teachers own Courses and manage Sessions, Assignments and
relevant teaching/payment information. Admin performs specifically authorized
platform management and monitoring.

```text
Student / Teacher / Admin browser
              |
         Next.js web UI
              | REST
       Spring Boot backend
              | Spring Data JPA / Hibernate
          PostgreSQL

External boundaries:
- Google Meet: externally created meeting, manually supplied Course URL
- Google Calendar: system/organization account, Session event synchronization
- Email delivery: verification/recovery support; provider unresolved
- Course payment: direct-to-Teacher VietQR direction; provider contracts gated
- Supabase Storage: approved avatar/thumbnail/Assignment/Submission/refund-proof files
```

Approved providers do not require speculative jobs, queues or integration frameworks.
Supabase supplies PostgreSQL hosting and bounded file Storage, not application identity
or business logic. Public discovery and protected participation are separate paths.

References: FR-DIS-003, FR-DIS-004, FR-ENR-006, FR-MEET-001, INT-CAL-001;
BR-COURSE-007, BR-ENR-007, BR-PAY-008; DM-COURSE-001, DM-ENR-001.

---

## 4. High-Level Architecture

The authoritative core data path is:

```text
Browser
  -> Next.js
  -> Spring Boot REST API
  -> authorized application/service operation
  -> Spring Data JPA / Hibernate
  -> PostgreSQL
```

Next.js renders and coordinates interaction with backend DTO contracts. Browser or
Next.js server-side execution does not bypass Spring Boot to access core data.
Client-supplied identities, ownership, payment outcomes or participation flags are
never persistence authority.

The backend remains a single modular application. Internal capability calls use
ordinary application boundaries; they do not require network calls or messaging.
Dependencies should be explicit and cohesive, avoiding circular feature coupling.

Illustrative dependency direction, not a mandatory service graph:

```text
HTTP controllers -> application services -> repositories -> PostgreSQL
auth -> shared User access
Session authorization -> parent Course ownership
Assignment authorization -> parent Session -> Course ownership
Submission authorization -> Assignment parentage + submitting Student
protected participation -> authoritative Enrollment policy
Course purchase context -> Course owner -> Teacher receiving information
Admin operations -> specifically authorized capability queries/actions
```

References: FR-AUTH-009, FR-AUTH-011, FR-AUTH-013; BR-AUTH-001,
BR-AUTH-002; INV-001, INV-002, INV-023.

---

## 5. Frontend Architecture

Preserve Next.js and TypeScript with feature-oriented organization. The existing
App Router foundation and React Query provider are implemented technical foundations.
Zustand is installed; no product stores are implied. Add state, hooks and API-client
behavior only for approved use cases.

Presentation, form feedback and responsive behavior belong to the frontend.
Authorization, authoritative validation and persisted business state remain in
Spring Boot. UI hiding improves usability but cannot protect a resource.

A future feature may group its UI and client contract usage together. Shared UI and
API-client concerns should be reused where actually shared. This is not a directory
creation plan, a fixed route hierarchy or approval of an interaction design.

| Audience | Approved capability areas, not fixed pages/navigation |
| --- | --- |
| Student | Authentication/account; Teacher and Course discovery/detail; whole-Course registration/purchase; My Courses; Sessions; Assignments; own Submission/result access; progress; protected Meet access; participant feedback. |
| Teacher | Authentication/account; Teacher profile; owned Course configuration and publishing; Sessions/scheduling; Assignments; enrolled-Student information; appropriate Submission/result inspection; progress; own receiving information; owned-Course transaction visibility; Course Meet URL management. |
| Admin | Authorized account management, Course oversight, category/topic management, Enrollment/transaction monitoring and platform statistics. |

Public Teacher/Course projections contain only approved public information.
Private teaching information, receiving information and protected participation
content must not leak through discovery, cached responses or error messages.

Exact pages, layouts, navigation, DTOs and routes remain subject to UI/UX and API
reconciliation. Installed tooling does not imply any product capability is complete.

References: FR-STU-002, FR-TEA-001, FR-DIS-005, FR-AUTH-012;
BR-AUTH-007, BR-TEA-003; UC-DIS-TEACHERS-01, UC-STU-BROWSE-COURSES-01,
UC-STU-VIEW-COURSE-01, UC-STU-MY-COURSES-01.

---

## 6. Backend Modular-Monolith Architecture

Preserve the backend base package/group `com.englishlearning`.
Organize future code primarily by cohesive feature, using only needed layers.
For example, a Course capability may have controller, service, repository, entity,
DTO and mapping responsibilities; this does not require empty directories or one
class/interface for every listed responsibility.

| Responsibility | Boundary |
| --- | --- |
| Controller | HTTP request/response handling, request-shape validation and invocation of the authorized use case. |
| Service/application operation | Business validation, identity/eligibility/ownership/participation checks, orchestration and appropriate transaction boundaries. |
| Repository | Persistence access; no business-authorization policy, unrelated workflows or duplicated persistence ownership. |
| DTO/projection | Explicit permitted input/output; never expose JPA entities directly. |
| Mapping | Translate representations where needed without speculative mapping infrastructure. |
| External interaction | Isolate vendor-specific behavior only when a selected integration needs it. |
| Common/shared | Genuinely cross-cutting errors, validation or configuration; not a dumping ground for business logic. |

Authentication-specific security belongs under `features/auth/security/`, including
future JWT authentication filtering/token validation components. Do not place those
components in a root-level security package. Only explicitly justified,
application-wide security responsibilities may live outside auth; naming alone is
not a reason to relocate code.

Auth uses shared User access rather than duplicating User persistence in auth,
payment or Teacher capabilities. TeacherProfile extends teaching information for a
User; it does not introduce a separate login identity.

References: NFR-MNT-001, NFR-MNT-006, NFR-MNT-007, NFR-MNT-008,
NFR-MNT-009, NFR-MNT-010; DM-USER-001, DM-TEA-001.

---

## 7. Domain Capability Boundaries

The following are conceptual responsibilities within one backend. They are not an
exhaustive package list, a one-package-per-entity rule or a physical schema.

| Capability | Responsibility and boundary | Domain references |
| --- | --- | --- |
| Authentication/account | Credentials, current identity, account eligibility, verification/recovery and shared User access. | DM-USER-001, DM-USER-002, DM-AUTH-001, DM-AUTH-002, DM-AUTH-003 |
| Teacher profile/discovery | Authorized teaching profile management and approved public teaching information. Approved onboarding plus verified/unlocked eligibility; review-operation details remain open. | DM-TEA-001 |
| Course/category | Teacher-owned Course management/publishing and selection of Admin-managed categories/topics; exactly one active/inactive managed Category per Course. | DM-COURSE-001, DM-CAT-001 |
| Session/scheduling | Course content occurrences with dates/times; Course recurring schedule is separate from an individual Session occurrence. | DM-SES-001, DM-SCH-001 |
| Assignment/Submission/Result | Session Assignments, Student-authored work and authorized result visibility; owning-Teacher grading stays on the single current Submission; detailed validation remains open. | DM-ASN-001, DM-ASN-002, DM-ASN-003 |
| Enrollment/participation | Student-Course relationship and authoritative protected participation checks, distinct from payment. | DM-ENR-001 |
| Course progress | Authorized personal and owned-Course progress information; derived calculation remains open; no progress table. | DM-PRO-002 |
| Payment | Teacher-owned receiving information and related whole-Course transactions, with trustworthy provider/bank confirmation and full Teacher-performed refunds. | DM-PAY-002, DM-PAY-001, DM-PAY-003 |
| Participant feedback | Participation-related feedback/rating; one per COMPLETED Enrollment, rating 1..5; editing/moderation remain open. | DM-RATE-001 |
| Admin | Authorized oversight and monitoring; no implicit ownership, grading or payment-confirmation authority. | INV-022 |
| Common/integration | Minimal shared infrastructure and only subsequently selected external interactions. | INV-023 |

Category/topic responsibilities may remain cohesive with Course, and scheduling
with Course/Session. Receiving information and Course transactions may coexist in
a payment capability. Do not create a separate module merely because a concept
has a domain identifier.

A Course has exactly one owning Teacher. Session, Assignment and Submission have
their own tutoring meanings and parentage. Session management must distinguish
Course recurrence from an occurrence that a Teacher may adjust independently.
Weekly rules generate Sessions before publication; rescheduling updates the same
Session with history; cancellation preserves its row/number/count and synchronizes
its event, without replacement or automatic make-up Sessions.
No recurrence library or background infrastructure is selected.

Assignments belong to Sessions; one current Submission belongs to an Assignment and
Student. Grading/result metadata stays on Submission. Supabase Storage holds approved
files; required deadlines and NUMERIC(5,2) score bounds are settled. Upload/delivery
and operation-specific validation contracts remain implementation work.
Progress is derived with formulas still open.

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.

References: FR-SES-001, FR-SES-002, FR-SCH-001, FR-SCH-002,
FR-ASN-001, FR-ASN-004, FR-ASN-005, FR-ASN-006, FR-PRO-009, FR-RATE-001;
BR-SES-001, BR-SES-002, BR-SCH-001, BR-ASN-001, BR-ASN-002,
BR-ASN-003, BR-TEA-002, BR-AUTH-006, BR-RATE-001;
INV-017, INV-018, INV-020.

---

## 8. Authentication and Authorization Architecture

Spring Security with JWT access tokens remains the approved direction, not a claim
of current implementation. Auth owns authentication-related security under
`features/auth/security/`; repositories and shared User responsibilities retain
the boundaries in Section 6.

Preserve the distinction between a Refresh Token credential and its server-side
RefreshSession. Verification and password-recovery/reset support remain separate
security responsibilities, not ordinary profile mutations.

Current business rules retain configurable default lifetimes of 15 minutes for
access tokens and 7 days for refresh tokens. Refresh validation/rotation, logout and password
operations must follow the current rules; this architecture does not replace them
with an invented session policy. The configurable default
password-reset credential lifetime remains 15 minutes.
Additional transport, concurrency and replay details remain explicit gates.
Validate JWT signature and expiration using supported framework mechanisms before
establishing identity. Refresh issuance checks authoritative expiry, revocation and
account/session eligibility. Logout invalidates applicable refresh/session state;
frontend deletion alone is insufficient and does not revoke every issued access token.

Use an appropriate supported password encoder and protected credential handling.
Algorithm and parameter selection remain subject to approved security configuration.
Never expose credential hashes, raw security credentials or secret material in
ordinary DTOs or logs. Reset credentials are purpose-specific, unpredictable,
account-bound, expiring and unusable after successful consumption; recovery responses
avoid unnecessary account-existence disclosure. Keep sensitive data out of JWT claims and use HTTPS in deployed
environments. Do not default to long-lived credentials in browser localStorage.
Cookie-based designs must address HttpOnly, Secure in production, SameSite, CORS
and CSRF together; the final transport remains unresolved.

Authentication establishes identity; authorization also checks current account
eligibility, role, resource relationship and operation permission. STUDENT, TEACHER
and ADMIN are authorization roles. Enrollment and payment state are not roles.

JWT may carry appropriate authentication facts but must not make mutable ownership,
participation, account eligibility or payment state authoritative merely through
claims. Backend operations resolve current state before protected disclosure/mutation.
V1 uses users.locked as its account-blocking mechanism: a locked account cannot authenticate
or use normal account functionality. There is no separate disabled, enabled or
account_status field/lifecycle. Email uses lowercase(trim(inputEmail)) for storage/login; a verified email
change retains the old email until successful verification of the new one. Password reset
revokes all refresh sessions; logged-in password change revokes other sessions while
preserving the current session. Raw refresh, verification and reset secrets are not
persisted.

Students cannot access Teacher/Admin management, and Teachers cannot access
Admin-only operations. Admin accounts are not publicly self-registered; authorized
administrative/system setup is separate from Teacher onboarding.

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.
Profile editing cannot become a path to arbitrary role or security-sensitive changes.

References: FR-ACC-001, FR-ACC-002, FR-AUTH-001, FR-AUTH-002;
BR-ROLE-001, BR-AUTHN-007, BR-AUTHN-008, BR-AUTHN-009,
BR-AUTHN-010, BR-AUTHN-011, BR-AUTHN-013, BR-AUTHN-015,
BR-AUTHN-016, BR-AUTHN-017, BR-AUTHN-018, BR-AUTHN-019,
BR-AUTHN-020, BR-AUTHN-023, BR-AUTHN-024, BR-PROFILE-002;
NFR-SEC-010, NFR-SEC-011, NFR-SEC-012, NFR-SEC-017;
UC-AUTH-LOGIN-01, UC-AUTH-REFRESH-01, UC-AUTH-REGISTER-TEACHER-01;
DM-AUTH-001, DM-AUTH-002, DM-AUTH-003, INV-001.

---

## 9. Course Ownership and Participation Enforcement

Every protected operation resolves authenticated identity, applicable account
eligibility, role and permission. For Teacher operations, TEACHER role alone is
insufficient. Authoritative backend relationships determine ownership:

```text
Course -> owning Teacher
Session -> Course -> owning Teacher
Assignment -> Session -> Course -> owning Teacher
Submission -> Assignment -> Session -> Course -> owning Teacher
Submission -> submitting Student
PaymentReceivingInformation -> owning Teacher
Course transaction -> Course -> owning Teacher
```

Apply checks before mutation or protected disclosure, including enrolled-Student
information, Submissions/results, progress, schedules and transaction information.
A client-provided Teacher, Student or Course identifier is a lookup input, never
proof of the caller's authorization.

Enrollment represents Student-Course participation for whole-Course purchasing.
It is distinct from Payment; individual Sessions are not purchased. Protected
Session/content, Assignment and Meet access requires authoritative participation
eligibility where required by the current rules.

Public discovery and My Courses listing do not prove current protected access.
Payment initiation or a browser success claim cannot grant participation. A
trustworthily confirmed payment may permit an Enrollment effect only under the
approved capacity/cutoff/lifecycle rules. Enrollment is unique per Student/Course;
free activation does not create a Payment and late confirmation cannot overbook.

Students must not access another Student's private work/results. Teachers inspect
only authorized information within owned Courses. Admin authorization is explicit:
ADMIN does not automatically confer Course ownership, grading, payment confirmation,
tuition custody or unrestricted moderation. Admin refund verification is separately
authorized by BR-PAY-011; it does not make Admin the transfer executor.

References: FR-AUTH-011, FR-AUTH-012, FR-ENR-006, FR-ENR-007, FR-ENR-008;
BR-COURSE-001, BR-COURSE-004, BR-AUTH-003, BR-ASN-003,
BR-ENR-001, BR-ENR-006, BR-ENR-007, BR-ENR-008,
BR-ADM-001, BR-ADM-002, BR-ADM-005;
INV-002, INV-013, INV-014, INV-018, INV-022.

---

## 10. Data Access and PostgreSQL/Supabase Boundary

Spring Boot is the only authoritative core application data-access path.
Spring Data JPA/Hibernate accesses PostgreSQL; frontend code does not use direct
database connections, Supabase Data API, supabase-js or Prisma.

Supabase-hosted PostgreSQL is verified for development. Preserve the TASK-003
Session Pooler connection on port 5432, environment-supplied datasource settings
and TLS configuration. Local PostgreSQL remains supported. One configured
datasource is used for the selected environment.

The existing configuration reads DATABASE_URL, DATABASE_USERNAME and
DATABASE_PASSWORD. Keep credentials in the local environment/ignored configuration,
never source control, documentation examples with real secrets or frontend bundles.
Preserve the verified Session Pooler `sslmode=require` decision; this enables TLS
but does not assert certificate/hostname verification. Do not silently weaken a
separately configured stronger verification mode or add certificate infrastructure.

Supabase supplies PostgreSQL hosting and the approved bounded file Storage. Do not
introduce Supabase Auth, Data API, Realtime or Edge Functions; RLS does not replace
Spring Boot authorization.

Hibernate `ddl-auto: none` and SQL initialization `mode: never` are implemented.
Controlled migrations remain the architectural direction; Flyway is not currently
installed. DATABASE_DESIGN.md specifies the 22-table direction and unresolved physical
restrictions/provider scopes. Executable migration still requires separate approval.
Keep PostgreSQL portability where practical.

Transactions should maintain internally consistent application changes after
authorization and validation. Exact boundaries depend on resolved use cases and
schema. Do not prescribe external payment atomicity or distributed transactions
around an unselected mechanism.

References: NFR-MNT-004, NFR-MNT-005, NFR-DATA-001, NFR-DATA-006,
NFR-DATA-007; BR-AUTH-001, BR-ENR-008; INV-023, INV-024.

---

## 11. External Integration Boundaries

| Boundary | Approved direction | Remaining contract |
| --- | --- | --- |
| Email | Verification/recovery delivery. | Provider/delivery policy. |
| Payment | VietQR, direct-to-Teacher, trustworthy bank/provider evidence. | Actual API/evidence authentication, conceptual identifiers and retry identity. |
| Refund | Teacher bank transfer/proof; Admin verifies completion. | Detailed eligibility/review and proof-delivery validation. |
| Google Calendar | System/organization account; Session source of truth. | Calendar identity/count, external-ID namespace and retry/reminder contracts. |
| Google Meet | Manually supplied protected Course URL. | Supply/validation timing; no Meet API. |
| Supabase Storage | Approved avatar/thumbnail/Assignment/Submission/refund-proof bytes. | Fixed bucket/path mapping, upload/access validation. |
| PostgreSQL | Backend-only JPA access; Supabase development hosting. | Production operations. |

External failure cannot fabricate success or roll back an authoritative Session change.
No queues, generic provider framework, per-user Google OAuth or background platform
is approved merely by these boundaries.

References: INT-CAL-001, FR-PAY-013, BR-PAY-009, BR-PAY-011, BR-AUTH-005.

---

## 12. Payment Architecture Boundary

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

Backend services own confirmation/matching and atomic Enrollment effects. Enforce
cutoff/capacity and retry/idempotency without distributed transactions around bank
transfers. Unknown receiver is a reconciliation case; matched Payment and receiving
account must agree. Physical V1 omits provider_account_ref/provider_order_id.
Verified adapter-context transaction deduplication remains mandatory integration work;
do not claim provider-wide identifier uniqueness.
DTOs restrict bank details, proof and evidence to authorized purposes.

References: FR-PAY-007, FR-PAY-010, FR-PAY-013, BR-PAY-006, BR-PAY-009,
BR-PAY-011, UC-PAY-COURSE-01, UC-PAY-REFUND-01, DM-PAY-001, DM-PAY-002,
DM-PAY-003, INV-010, INV-016.

---

## 13. Meet and Calendar Boundaries

One externally created, manually supplied protected Course Meet URL serves all its
Sessions. Backend ownership/Enrollment checks protect management/disclosure; no Meet
API, automatic meeting creation or separate Meeting entity. Supply timing remains open.

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

A nullable event ID supports pending mapping; SYNCED requires ID/time. Do not put
Calendar API calls inside a database transaction that rolls back Session changes on
provider failure. Reliable retry/execution details remain open without adding speculative
infrastructure. V1 uses one configured Calendar, with its ID in backend configuration/secrets.
UNIQUE(session_id, provider) and non-null (provider, external_event_id) uniqueness
apply within that boundary. No calendar_id column, attendee table or per-user OAuth
is added; multi-calendar support requires a future migration.

References: FR-MEET-001, FR-MEET-002, INT-CAL-001, BR-AUTH-005,
UC-CAL-SCHEDULE-01, INV-019, INV-021.

---

## 14. Security and Privacy

Protected data requires purpose-specific backend authorization and explicit DTO
projection. Authentication alone is not permission to read all related records.

| Information | Disclosure boundary |
| --- | --- |
| Public Teacher/Course details | Only approved public teaching/discovery information. |
| Teacher receiving information | Private Teacher-owned information; permitted payment context only under approved rules, never public discovery. |
| Course Meet URL | Protected Course participation information. |
| Submission/result | Submitting Student's private work and authorized owned-Course Teacher inspection. |
| Account/security information | Authenticated, specifically authorized operations; no credential/secret leakage. |
| Course transactions | Related Student, owning Teacher or authorized Admin monitoring as applicable. |
| Schedule/Calendar/progress | Appropriate ownership, participation and personal-data checks. |

Reject attempts to bind privileged identity, role, ownership or payment/access
state through ordinary requests. Apply input validation at trust boundaries and
preserve meaningful backend checks regardless of frontend validation.

Retain required learning, authentication and confirmed-payment information according
to current requirements. Retain relevant verified transaction information independently
of changing participation eligibility where required for verification/monitoring.
Exact archival/deletion, retention duration and storage
representation remain for their approved design stages; do not infer a generic audit
system or content-versioning architecture.

Supabase Storage holds Teacher avatars, Course thumbnails, Assignment files, Submission
files and refund proof. PostgreSQL stores paths/references and applicable metadata, never
file bytes, base64 or temporary signed URLs. Resolve each path within an explicitly
configured bucket for its usage; exact bucket identifiers remain configuration, and the
path/bucket mapping must be fixed before integration. Spring Boot authorizes access; Storage
does not replace backend business authorization. No speculative media-processing or
key-management infrastructure.

References: FR-AUTH-009, FR-AUTH-011, FR-AUTH-012, FR-AUTH-013;
NFR-DATA-001, NFR-DATA-006, NFR-DATA-007;
BR-AUTH-001, BR-AUTH-002, BR-AUTH-007, BR-TEA-003,
BR-ASN-003, BR-PAY-010; INV-018, INV-023, INV-024.

---

## 15. Key Runtime and Data Flows

These describe trust boundaries, not final endpoint contracts or implemented flows.

### Authentication

Browser -> Next.js -> Spring Security/auth capability -> authoritative
account/security state -> permitted access/refresh result.
Verification, refresh rotation, logout and recovery follow existing business rules;
token transport and unresolved security configuration remain gated.

### Teacher-owned management

Teacher request -> authenticated identity and eligibility -> TEACHER permission ->
Course ownership (traversing parents for child resources) -> business validation ->
appropriate persistence operation -> permitted response.
For Course creation, the backend establishes ownership from authenticated identity.
Publication/lifecycle policy is not completed by this flow.

### Public discovery

Student/Visitor -> Teacher/Course query -> approved public projection.
No protected participation information, receiving information or private work is
included merely because the parent Course is discoverable.

### Protected participation

Student -> authentication/eligibility -> authoritative Enrollment/participation
check for the requested Course -> permitted Session/content/Assignment access.
A prior list result or client-held flag does not replace this check.

### Submission and result

Student -> authorized Assignment -> validated permitted Submission.
Teacher -> owned Course -> Session -> Assignment -> authorized Submission/result
inspection. Student result retrieval resolves the submitting identity.
Only owning Teachers grade; result metadata stays on Submission. Detailed validation
and result availability remain gated; this description assigns no scores.

### Course payment

Student selects Course -> backend resolves owning Teacher -> backend resolves
historical receiving bank account -> price-snapshotted Payment -> VietQR direction
subject to verified integration contract -> trustworthy bank evidence -> transactional
confirmation and eligible Enrollment effect.

Any future payment-dependent Enrollment effect must cross trustworthy confirmation
and approved eligibility/lifecycle checks. Provider notification/authenticity and
durable retry contracts remain explicit integration gates.

### Protected Meet access

Student -> authentication -> Course participation authorization -> permitted
disclosure of the Course's shared, manually supplied Meet URL.

Traceability: UC-AUTH-LOGIN-01, UC-TEA-COURSE-01, UC-TEA-PUBLISH-01,
UC-STU-VIEW-COURSE-01, UC-SES-VIEW-01, UC-ASN-SUBMIT-01,
UC-ASN-INSPECT-01, UC-ASN-RESULT-01, UC-PAY-COURSE-01,
UC-MEET-ACCESS-01; INV-002, INV-010, INV-013, INV-018, INV-019.

---

## 16. Deployment and Environment Direction

The following versions/names are recorded from current repository configuration,
not selected afresh during this rewrite:

| Foundation | Repository baseline |
| --- | --- |
| Java / Spring Boot | Java release 17 / Spring Boot 4.1.1 |
| Build | Maven 3.9.16 / Maven Wrapper 3.3.4, only-script distribution |
| Backend identity | Group/base package com.englishlearning; artifact english-learning-backend |
| Frontend runtime | Node 24.21.0 / npm 11.19.0 |
| Web framework | Next.js 16.3.5 / React and React DOM 19.3.0 |
| Language | TypeScript 5.9.3 |
| Server-state foundation | React Query 5.103.1 provider |
| Client-state dependency | Zustand 5.0.15; no speculative stores |
| Browser testing | @playwright/test 1.63.0, development dependency |

Evidence sources: backend/pom.xml, backend/.mvn/wrapper/maven-wrapper.properties,
backend/src/main/java/com/englishlearning/EnglishLearningApplication.java,
frontend/.node-version, frontend/package.json and frontend/src/app/providers.tsx.

TASK-001 records backend build/context/JAR verification including manual Oracle
JDK 17.0.12 startup outside the Codex execution environment. TASK-002 records the
neutral frontend foundation and browser verification, not completed UI/UX.
TASK-003 records local PostgreSQL and Supabase Session Pooler connectivity,
TLS/no-application-table verification and configured startup/shutdown.
Preserve that evidence; no database reconnection is needed for this rewrite.

Next.js production hosting, Spring Boot production hosting, final production
database hosting, deployment provider and production secrets/operations remain
unresolved. Development Supabase usage does not decide production hosting.
No deployment/container/CI infrastructure is introduced by this document.

---

## 17. Observability, Error Handling, Testing and Operational Concerns

Keep validation layered: request shape/syntax, business rules, ownership,
participation and approved persistence integrity. Do not turn an unresolved policy
into a validation requirement simply to complete a request contract.

Use consistent structured API errors without secret material or private resource
details. Exact DTOs, routes and status mappings belong to API design; retain the
/api/v1 version direction without copying obsolete operation catalogues.
Authorization failures must prevent mutation and protected disclosure.

Logs should support diagnosis while avoiding passwords, tokens, Teacher receiving
information, unnecessarily recorded Meet URLs and sensitive payment evidence.
Do not select a large observability stack. Production retention, backups,
monitoring and recovery policy remain open.

Bound list queries and external work according to known access patterns; detailed
pagination, timeouts and query plans belong to API/persistence design.
No cache, queue or background worker is justified merely by anticipated scale.

Backend unit/integration/security/persistence verification remains separate from
frontend component/integration and browser verification. Preserve Spring Boot/JUnit
and the Playwright foundation. Playwright is a frontend development test dependency,
not a runtime service or substitute for backend tests.

Future tutoring tests should cover cross-Teacher ownership denial, unauthorized
Student participation, Submission/result privacy, protected Meet disclosure,
receiving-information privacy, constrained Admin access and payment trust.
These tests are architectural expectations, not claims of implemented coverage.
Use resolved policies for behavioral assertions; do not invent policy to write tests.

References: NFR-MNT-002, NFR-MNT-003, NFR-PERF-004, NFR-PERF-005;
BR-AUTH-001, BR-AUTH-002, BR-PAY-009; INV-002, INV-010,
INV-018, INV-019, INV-023.

---

## 18. Deferred Architecture Decisions

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

Email delivery, production hosting/operations and UI/UX remain undecided. The approved
provider directions do not resolve concrete protocols or authorize implementation.

---

## 19. Retired Legacy Architecture

This section records historical disposition only. None of the following is an
active backend/frontend module, authorization dependency, runtime flow or backlog:

| Historical concept | Disposition |
| --- | --- |
| Vocabulary, VocabularySense, LessonVocabulary, SavedVocabulary, vocabulary learning/search and weak vocabulary | Retired vocabulary-learning responsibilities. |
| Dictionary integration, pronunciation/audio, vocabulary TTS and licensed-audio storage | Retired external integrations and storage assumptions. |
| Lesson, vocabulary Lessons and Lesson player | Retired; Session is an independently defined tutoring concept. |
| Fill Word, Listening exercise, Quiz, Exercise, ExerciseQuestion, ExerciseAttempt, AnswerRecord | Retired engine, attempt/question/answer and automatic correctness responsibilities; not renamed as Assignment/Submission. |
| Mastery, VocabularyPerformance, LessonProgress and automatic accuracy/score aggregation | Retired progress assumptions; current Course progress has unresolved calculation/storage. |
| Vocabulary Review/practice | Retired; participant feedback is independently defined. |
| STANDARD/PREMIUM, Premium gating and Course classification | Retired access tiers and authorization rules; never current roles. |
| SubscriptionPlan, Subscription, subscription entitlement/expiry/revenue | Retired commercial model and expiry processing. Enrollment is participation, not a renamed Subscription or Payment. |
| PaymentService -> SubscriptionService | Retired payment-to-Premium transition; no inherited automatic Enrollment activation. |

No mechanical Lesson-to-Session, Exercise/Quiz-to-Assignment,
ExerciseAttempt-to-Submission or Subscription-to-Enrollment transformation is valid.

Original architecture-rule disposition (17 identifiers, none silently reassigned):

| Disposition | Identifiers | Meaning |
| --- | --- | --- |
| Preserved | AR-001, AR-002, AR-003, AR-004, AR-005, AR-008, AR-009, AR-010, AR-011, AR-012, AR-013, AR-017 | Compatible technical and development constraints remain active in Section 20. |
| Reconciled | AR-006, AR-007, AR-015, AR-016 | Backend authority, conditional provider isolation, ownership and history use current tutoring semantics. |
| Retired | AR-014 | Historical Premium-entitlement rule; not reused for Enrollment or any other concept. |

---

## 20. Traceability and Downstream Boundaries

### Active architecture rules

These rules constrain future design within current requirements; they are not
implementation-status claims.

| ID | Current rule | Targeted authority |
| --- | --- | --- |
| AR-001 | Use a modular-monolith backend unless approved requirements justify a change. | NFR-MNT-005 |
| AR-002 | Organize backend code by cohesive feature, using only needed layers. | NFR-MNT-006, NFR-MNT-007 |
| AR-003 | Use explicit DTOs; do not expose JPA entities through public APIs. | NFR-MNT-010 |
| AR-004 | Keep controllers focused on HTTP; services own use-case/business orchestration. | NFR-MNT-001 |
| AR-005 | Repositories handle persistence, not business-authorization policy. | NFR-MNT-001, NFR-MNT-008 |
| AR-006 | Spring Boot owns business rules, authoritative state and protected disclosure/mutation. | BR-AUTH-001, INV-023 |
| AR-007 | Isolate selected external-provider behavior when needed; an unresolved boundary does not require an adapter. | NFR-MNT-009, INT-CAL-001 |
| AR-008 | Keep secrets and environment-specific configuration outside source code. | NFR-MNT-004 |
| AR-009 | Use controlled migrations for approved schema changes; do not enable automatic schema mutation. | AGENTS.md Section 18; current backend configuration |
| AR-010 | Avoid unneeded infrastructure and speculative abstractions. | NFR-MNT-005 |
| AR-011 | Keep Student web interaction mobile-first, responsive and touch-friendly. | BR-WEB-001 |
| AR-012 | Optimize Teacher/Admin management for desktop while retaining responsive access. | BR-WEB-002, BR-WEB-003 |
| AR-013 | Organize frontend responsibilities by feature with proportionate shared UI/client concerns. | AGENTS.md Section 19; NFR-MNT-007 |
| AR-015 | Enforce Teacher ownership in the backend, traversing parent Course relationships before protected access. | BR-AUTH-003, INV-002 |
| AR-016 | Preserve required current-domain information/history without importing unapproved retention mechanisms. | NFR-DATA-001, NFR-DATA-006, NFR-DATA-007, INV-024 |
| AR-017 | Implementation tasks load only relevant documented context; resolve material contradictions before dependent work. | AGENTS.md Section 3 |
| AR-018 | Resolve protected Student participation from backend Enrollment state; keep Payment and Enrollment distinct. | BR-ENR-006, BR-ENR-008, INV-013, INV-014 |
| AR-019 | Resolve the Teacher receiving destination through Course ownership; tuition is direct-to-Teacher without platform custody. | BR-PAY-006, BR-PAY-007, BR-PAY-008, INV-015, INV-016 |
| AR-020 | Require trustworthy provider/bank evidence before payment-dependent effects; VietQR protocol/namespace details remain gated. | BR-PAY-002, BR-PAY-009, INV-010 |
| AR-021 | Protect the manually supplied shared Course Meet URL; Calendar uses the separate organizational-account synchronization boundary with remaining contract gates. | BR-MEET-001, BR-MEET-002, BR-AUTH-005, INV-019, INV-021 |

New rules AR-018, AR-019, AR-020 and AR-021 capture tutoring trust boundaries.
Nested ownership is covered by reconciled AR-015 and conditional integrations by
reconciled AR-007 rather than duplicate new rules.

### Capability traceability

References below supplement the targeted references in each section. They refer to
active current meanings, not historical text that happened to use the same ID.

| Capability | Requirements/rules | Use case | Domain/invariant |
| --- | --- | --- | --- |
| Teacher ownership | FR-TCR-005, BR-COURSE-001, BR-AUTH-003 | UC-TEA-COURSE-01 | DM-COURSE-001, INV-002 |
| Session/scheduling | FR-SES-001, FR-SCH-001, FR-SCH-002, BR-SCH-001 | UC-SES-MANAGE-01, UC-SCH-COURSE-01, UC-SCH-SESSION-01 | DM-SES-001, DM-SCH-001, INV-020 |
| Assignments/work/results | FR-ASN-004, FR-ASN-005, FR-ASN-006, BR-ASN-003 | UC-ASN-SUBMIT-01, UC-ASN-INSPECT-01, UC-ASN-RESULT-01 | DM-ASN-001, DM-ASN-002, DM-ASN-003, INV-018 |
| Participation | FR-ENR-006, FR-ENR-007, BR-ENR-006, BR-ENR-008 | UC-STU-ENROLL-01, UC-STU-MY-COURSES-01 | DM-ENR-001, INV-013, INV-014 |
| Receiving information | FR-PAY-007, FR-PAY-008, BR-PAY-006, BR-PAY-007 | UC-PAY-RECEIVING-01 | DM-PAY-002, INV-015 |
| Course payment/trust | FR-PAY-009, FR-PAY-010, FR-PAY-012, BR-PAY-009 | UC-PAY-COURSE-01, UC-PAY-VIEW-01 | DM-PAY-001, INV-010, INV-016 |
| Full refunds | FR-PAY-013, BR-PAY-011 | UC-PAY-REFUND-01 | DM-PAY-003 |
| Meet | FR-MEET-001, FR-MEET-003, BR-MEET-002 | UC-MEET-MANAGE-01, UC-MEET-ACCESS-01 | DM-COURSE-001, INV-019 |
| Calendar | One organizational account and one configured Calendar; backend-configured calendar ID, one event per Session, unique non-null (provider, external_event_id). Execution/retry/reminder details remain integration work. |
| Progress | FR-PRO-009, BR-TEA-002, BR-AUTH-006 | UC-LEARN-PROGRESS-01, UC-TEA-ANALYTICS-01 | DM-PRO-002 |
| Participant feedback | FR-RATE-001, BR-RATE-001 | UC-RATE-PARTICIPANT-01 | DM-RATE-001 |
| Admin/category/monitoring | FR-ADM-007, FR-ADM-008, FR-ATR-001, FR-AAN-008, BR-ADM-001, BR-ADM-002, BR-ADM-005 | UC-ADM-CATEGORIES-01, UC-ADM-TRANSACTIONS-01, UC-ADM-ANALYTICS-01 | DM-CAT-001, INV-022 |
| Privacy/retention | FR-AUTH-012, NFR-DATA-001, NFR-DATA-006, NFR-DATA-007, BR-TEA-003 | UC-TEA-STUDENTS-01, UC-ASN-RESULT-01 | INV-023, INV-024 |

### Downstream boundary

DATABASE_DESIGN.md, API_DESIGN.md, TASK_BREAKDOWN.md, FEATURE_STATUS.md and README.md
consume the approved reconciled direction. Their historical structures are not current
design constraints; remaining physical/provider gates prevent implementation approval.

Follow the eleven-stage development order in AGENTS.md. Database design, UI/UX,
API contracts and later implementation must consume current tutoring requirements
and the explicit gates above. Preserve completed foundation IDs/evidence when
future plans change. No schema, application code, configuration, dependency or live
status is changed by this architecture reconciliation.
