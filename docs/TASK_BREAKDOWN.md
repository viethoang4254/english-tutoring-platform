# Task Breakdown

## 1. Purpose and authority

This plan decomposes the approved design into 22 stable features and 30 stable implementation tasks. It does not authorize implementation or decide open contracts. The source hierarchy remains AGENTS.md and the approved project documents.

Implementation status and evidence live only in [FEATURE_STATUS.md](FEATURE_STATUS.md). This document contains specifications and dependencies, not a second live status ledger. IDs are stable, never renumbered for presentation order; new scope requires approval.

Read [PROJECT_SPEC.md](PROJECT_SPEC.md), [REQUIREMENTS.md](REQUIREMENTS.md), [BUSINESS_RULES.md](BUSINESS_RULES.md), [USE_CASES.md](USE_CASES.md), [DOMAIN_MODEL.md](DOMAIN_MODEL.md), [ARCHITECTURE.md](ARCHITECTURE.md), [DATABASE_DESIGN.md](DATABASE_DESIGN.md) and [API_DESIGN.md](API_DESIGN.md) with the relevant project-local skills before implementation. Resolve conflicts explicitly; planning IDs are not new requirement IDs.

## 2. Scope and architecture safeguards

- Browser -> Next.js responsive web frontend -> Spring Boot REST API -> Spring Data JPA/Hibernate -> Supabase-hosted PostgreSQL; modular monolith and feature-based packages.
- Flyway manages approved migrations; JPA entities/repositories are implemented with their owning capabilities. Local PostgreSQL is permitted for development/testing.
- Spring Security/JWT authentication components stay in features/auth/security/. Backend role, account eligibility, ownership and entitlement checks remain authoritative.
- STUDENT/TEACHER/ADMIN are roles; STANDARD/PREMIUM are Student tiers or Course access classifications, never roles. Premium remains subscription-based.
- Teacher creation supplies DRAFT/STANDARD; Teacher create/update excludes accessClassification but retains owned editing/content/publish/archive rights. Admin classification remains separate.
- Required fullName and optional avatarUrl use the approved creation/profile rules; no new Teacher identity, Admin self-edit grant, email-change or upload workflow.
- DTO-only APIs, safe BIGINT/money JSON, currency, ISO-8601 time and structured errors; learner delivery never leaks expected answers or provider/credential secrets.
- Exactly the approved 18-table baseline: no speculative retry/issuance schema, reporting infrastructure, mutable Premium flag, generic RBAC or general content-versioning engine.
- No Supabase Auth, direct frontend database access, Prisma, per-Course purchase, accounting/expense subsystem or unapproved provider-specific infrastructure.

## 3. Phases and dependency interpretation

| Phase | Deliverable focus | Tasks |
|---|---|---|
| P1 | Safe technical foundation and early build checks | TASK-001, TASK-002, TASK-003, TASK-005; early TASK-030 |
| P2 | Reviewed persistence and security foundations | TASK-004, TASK-006 |
| P3 | Identity, authoring, vocabulary and entitlement foundations | TASK-007 through TASK-010; TASK-012 through TASK-015; TASK-021 |
| P4 | Discovery, enrollment, learning, history and Review | TASK-011; TASK-016 through TASK-020 |
| P5 | Payments and authorized administration/analytics | TASK-022 through TASK-027 |
| P6 | Integrated verification and approved release operations | TASK-028, TASK-029; release TASK-030 |

Phases are delivery groupings, not a rigid numeric execution order. Follow the dependency graph; for example vocabulary precedes Lesson assignments, entitlement precedes enrollment, and content plus enrollment precede Student protected reads. Feature tasks include their own tests; testing does not wait for P6.

Dependencies name integration prerequisites. Safe independent portions may start earlier; do not mark a whole task complete on that basis. TASK-004 requires TASK-003 local database readiness, not unavailable deployed credentials. TASK-003's Supabase check remains necessary before its own completion. TASK-030 can start build checks early; its release portion additionally requires TASK-028/TASK-029 readiness. This staged prerequisite is not an early deployment authorization.

TASK-002 establishes only technical responsive shell infrastructure. Product navigation, layout, screen flows and interaction behavior require G-UI. UI/UX is not documented complete. Backend work that does not depend on those decisions may proceed when separately authorized.

No approved capability is removed merely to reach an early runnable build. FR-AAN-005/FR-AAN-006 remain optional/deferred approved analytics, not new mandatory report routes; provider/audio and advanced Premium choices stay conditional on their sources. TASK-027 tracks this deferred scope without inventing its measures.

## 4. Feature/task index

| Feature | Capability | Tasks |
|---|---|---|
| FEAT-001 | Development/runtime foundation | TASK-001, TASK-002, TASK-003, TASK-005 |
| FEAT-002 | Persistence and migration foundation | TASK-004 |
| FEAT-003 | Security and authorization foundation | TASK-006 |
| FEAT-004 | Authentication and account lifecycle | TASK-007, TASK-008 |
| FEAT-005 | Current-user/profile | TASK-009 |
| FEAT-006 | Course discovery and Student content access | TASK-011 |
| FEAT-007 | Teacher Course management | TASK-010 |
| FEAT-008 | Lesson/content authoring | TASK-012 |
| FEAT-009 | Shared vocabulary and dictionary integration | TASK-013, TASK-014 |
| FEAT-010 | Exercise/question authoring | TASK-015 |
| FEAT-011 | Enrollment and Course access enforcement | TASK-016 |
| FEAT-012 | Attempts, answer evaluation and historical results | TASK-017 |
| FEAT-013 | Progress and vocabulary performance | TASK-018 |
| FEAT-014 | Saved vocabulary and Review | TASK-019, TASK-020 |
| FEAT-015 | Subscription plans and entitlement | TASK-021 |
| FEAT-016 | Payment lifecycle | TASK-022 |
| FEAT-017 | Admin users and Teacher administration | TASK-023 |
| FEAT-018 | Admin Course administration | TASK-024 |
| FEAT-019 | Teacher analytics | TASK-025 |
| FEAT-020 | Admin subscription/payment administration | TASK-026 |
| FEAT-021 | Admin Dashboard and revenue analytics | TASK-027 |
| FEAT-022 | Release verification and operations | TASK-028, TASK-029, TASK-030 |

## 5. Task specifications

For every task, completion also requires its applicable API validations/authorization/errors/retries, approved UI where applicable, relevant tests passing and evidence recorded in FEATURE_STATUS.md. Finalize dependent contracts first; an API marked FINAL is a design classification, not implementation evidence. No task authorizes new scope. Frontend browser E2E verification uses @playwright/test as a Next.js development dependency, not a runtime application dependency; feature-specific browser checks may reuse it. Spring Boot/JUnit unit and backend integration testing remain separate and are not replaced by Playwright. No browser-cloud, testing-service or deployment infrastructure is introduced.

### TASK-001 — Repository and Spring Boot skeleton

- Purpose/completion outcome: Backend builds and starts reproducibly; directories and commands are documented; no unapproved infrastructure or domain implementation.
- Feature/phase: FEAT-001; P1.
- Prerequisite tasks: None.
- Decision gates: G-TOOLCHAIN.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-MNT-004; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Establish the modular monolith build and feature-based package root without generating empty CRUD layers.
- Frontend: None; frontend foundation belongs to TASK-002.
- Persistence/migrations: None; no schema generation on startup.
- Verification: Run the chosen backend build and a minimal application-context startup check.

### TASK-002 — Next.js technical foundation and responsive shell infrastructure

- Purpose/completion outcome: Frontend builds and renders the technical foundation; no unapproved navigation or interaction design is implemented. G-UI remains a dependency for every later product screen.
- Feature/phase: FEAT-001; P1.
- Prerequisite tasks: None.
- Decision gates: G-TOOLCHAIN, G-UI.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-UI-001; NFR-UI-002; NFR-UI-003; NFR-UI-004; NFR-UI-005; NFR-UI-006; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: None; establish only the client/server boundary needed for later API integration.
- Frontend: Create TypeScript/Next.js build and responsive technical primitives that do not select product navigation, layout, screen flows or interactions. Those decisions require G-UI; no shell is described as UI/UX-approved. When appropriate, prepare the Playwright test-tooling/configuration foundation without inventing UI/UX behavior merely to create tests.
- Persistence/migrations: None; no Prisma, database client or direct Supabase data access.
- Verification: Run the frontend build and verify the neutral technical shell can render at narrow/wide viewport sizes without inventing product screens.

### TASK-003 — Local/dev configuration and PostgreSQL connectivity

- Purpose/completion outcome: Local development is reproducible and approved database connection checks pass; secrets remain external. Supabase validation cannot be claimed complete while deferred.
- Feature/phase: FEAT-001; P1.
- Prerequisite tasks: TASK-001, TASK-002.
- Decision gates: G-ENV.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-MNT-004; NFR-SEC-005; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Document approved environment-variable loading and connect Spring Boot/JPA to local PostgreSQL and, when authorized, Supabase-hosted PostgreSQL.
- Frontend: Configure only the backend API origin boundary; never frontend database credentials.
- Persistence/migrations: Validate connectivity without creating unapproved objects; migrations remain TASK-004.
- Verification: Verify local connection, failure behavior and absence of committed secrets; validate approved Supabase connection when credentials/environment authorization exist.

### TASK-004 — Flyway schema and persistence-integrity foundation

- Purpose/completion outcome: All approved baseline schema is migration-managed and verified, with documented treatment of provisional details and no uncontrolled ORM schema generation. Partial migrations do not make the task complete.
- Feature/phase: FEAT-002; P2.
- Prerequisite tasks: TASK-001, TASK-003.
- Decision gates: G-SCHEMA, G-NORMALIZATION.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-DATA-001; NFR-DATA-005; NFR-DATA-006; NFR-DATA-007; NFR-DATA-008; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Integrate Flyway and a minimal repository-test foundation; avoid implementing every JPA entity here.
- Frontend: None.
- Persistence/migrations: Translate the approved 18-table schema and reviewed constraints into migrations only after relevant provisional choices are settled. Do not invent fields for retry/provider protocols.
- Verification: Apply migrations to a clean supported PostgreSQL database; verify important FK/unique/check and restrictive-deletion behavior; verify migration repeat behavior.

### TASK-005 — Common API DTO/error/pagination infrastructure

- Purpose/completion outcome: Shared conventions are usable by feature controllers/clients and verified; no JPA entities leak as public contracts.
- Feature/phase: FEAT-001; P1.
- Prerequisite tasks: TASK-001, TASK-002.
- Decision gates: None specific; common approved boundaries apply.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement approved /api/v1 JSON conventions, safe BIGINT/money representation, validation errors and bounded paging without a generic framework.
- Frontend: Provide the minimal typed API/error handling reused by feature integrations after TASK-002.
- Persistence/migrations: None.
- Verification: Verify safe identifier/money serialization, invalid-input errors, paging bounds and no internal exception disclosure.

### TASK-006 — Spring Security/JWT and authorization foundation

- Purpose/completion outcome: Approved security foundation passes enforcement checks; transport-dependent portions cannot be called complete before their decisions.
- Feature/phase: FEAT-003; P2.
- Prerequisite tasks: TASK-004, TASK-005.
- Decision gates: G-AUTH, G-SECURITY.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: BR-AUTH-001; BR-AUTH-002; BR-AUTH-003; BR-AUTH-004; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement features/auth/security/ foundation, principal resolution, current account eligibility, role checks and reusable ownership/entitlement boundaries.
- Frontend: No product screens; transport-sensitive client integration waits for G-AUTH and TASK-002.
- Persistence/migrations: Map User/security persistence only as approved; do not add token blacklist or session infrastructure.
- Verification: Verify invalid/expired credentials, wrong-role access, account restrictions and nested ownership decisions.

### TASK-007 — Registration, verification and authentication sessions

- Purpose/completion outcome: Registration and permitted session lifecycle work end-to-end without credential leakage; all owned API contracts and required security tests pass.
- Feature/phase: FEAT-004; P3.
- Prerequisite tasks: TASK-006.
- Decision gates: G-AUTH, G-SECURITY, G-NORMALIZATION, G-UI.
- Primary API operations: `POST /api/v1/auth/register`; `POST /api/v1/auth/email-verification`; `POST /api/v1/auth/email-verification/resend`; `POST /api/v1/auth/login`; `POST /api/v1/auth/refresh`; `POST /api/v1/auth/logout`.
- Source references: FR-STU-001; BR-AUTHN-001; BR-AUTHN-004; BR-PROFILE-001; UC-AUTH-REGISTER-01; FR-ACC-008; BR-AUTHN-006; UC-AUTH-VERIFY-EMAIL-01; FR-ACC-009; BR-AUTHN-005; FR-STU-002; FR-TEA-001; FR-ADM-001; UC-AUTH-LOGIN-01; FR-ACC-003; FR-ACC-004; FR-ACC-005; BR-AUTHN-009; BR-AUTHN-010; UC-AUTH-REFRESH-01; FR-STU-003; BR-AUTHN-011; UC-AUTH-LOGOUT-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement registration/fullName, verification/resend, login, refresh and logout under approved policies.
- Frontend: Implement approved authentication flows once G-UI and transport decisions are settled.
- Persistence/migrations: Use users, refresh_sessions and email_verification_tokens; preserve credential hashing and configured expiration.
- Verification: Verify privilege escalation rejection, pending/locked/disabled behavior, expiry/revocation and approved rotation/logout cases.

### TASK-008 — Password change and recovery

- Purpose/completion outcome: All approved password operations pass success/rejection/replay checks and frontend flow verification.
- Feature/phase: FEAT-004; P3.
- Prerequisite tasks: TASK-007.
- Decision gates: G-SECURITY, G-UI.
- Primary API operations: `POST /api/v1/auth/password/change`; `POST /api/v1/auth/password/forgot`; `POST /api/v1/auth/password/reset`.
- Source references: FR-ACC-010; BR-AUTHN-013; UC-AUTH-CHANGE-PASSWORD-01; FR-ACC-011; BR-AUTHN-014; UC-AUTH-FORGOT-PASSWORD-01; FR-ACC-012; FR-ACC-013; BR-AUTHN-015; BR-AUTHN-016; UC-AUTH-RESET-PASSWORD-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement dedicated password change/forgot/reset operations and atomic recovery consumption.
- Frontend: Provide only approved password/recovery interactions and neutral request responses.
- Persistence/migrations: Use password_reset_tokens and approved password updates; no generic profile password mutation.
- Verification: Verify neutral recovery response, invalid/expired/used credentials, concurrent consumption and approved session invalidation.

### TASK-009 — Current-user and personal profile

- Purpose/completion outcome: Current identity/profile displays correctly and only permitted own fields change; all contract/security checks pass.
- Feature/phase: FEAT-005; P3.
- Prerequisite tasks: TASK-007, TASK-005.
- Decision gates: G-UI.
- Primary API operations: `GET /api/v1/me`; `PATCH /api/v1/me/profile`.
- Source references: FR-ACC-007; FR-STU-004; FR-TEA-002; BR-AUTHN-012; UC-AUTH-ME-01; FR-STU-005; FR-TEA-003; BR-PROFILE-001; UC-AUTH-PROFILE-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement /me and Student/Teacher self-profile PATCH with authenticated identity and exact field allowlist.
- Frontend: Implement approved profile viewing/editing, including avatar reference only.
- Persistence/migrations: Use existing full_name/avatar_url; no profile table or avatar storage.
- Verification: Verify required/trimmed/nonblank name <=200, nullable HTTPS avatar <=2048, omission/null semantics, protected fields and no Admin self-edit grant.

### TASK-010 — Teacher-owned Course creation, editing and lifecycle

- Purpose/completion outcome: Teacher can create/edit/manage owned Courses and use approved lifecycle operations; Admin-only classification and history retention are enforced.
- Feature/phase: FEAT-007; P3.
- Prerequisite tasks: TASK-006, TASK-005.
- Decision gates: G-UI.
- Primary API operations: `POST /api/v1/teacher/courses`; `GET /api/v1/teacher/courses`; `GET /api/v1/teacher/courses/{courseId}`; `PATCH /api/v1/teacher/courses/{courseId}`; `POST /api/v1/teacher/courses/{courseId}/publish`; `POST /api/v1/teacher/courses/{courseId}/archive`.
- Source references: FR-TCR-001; FR-TCR-002; FR-TCR-003; BR-COURSE-005; BR-CSTATUS-001; UC-TEA-COURSE-01; FR-TCR-004; BR-COURSE-004; FR-TCR-005; FR-TCR-006; BR-COURSE-006; BR-CSTATUS-002; UC-TEA-PUBLISH-01; BR-CSTATUS-003; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement ownership-scoped Course reads/writes; assign DRAFT/STANDARD and owner; preserve authorized publish/archive.
- Frontend: Implement approved Teacher Course workflows after UI/UX decisions; access classification is not Teacher input.
- Persistence/migrations: Map courses to approved schema; no new classification default or ownership-transfer model.
- Verification: Verify nested authorization entry points, foreign-owner rejection, classification injection rejection and history-preserving archive.

### TASK-011 — Course discovery and Student content reads

- Purpose/completion outcome: Discovery and authorized content reads satisfy visibility and DTO boundaries, including pre-enrollment summaries and rejected restricted access.
- Feature/phase: FEAT-006; P4.
- Prerequisite tasks: TASK-010, TASK-012, TASK-013, TASK-015, TASK-016.
- Decision gates: G-DISCOVERY, G-UI.
- Primary API operations: `GET /api/v1/courses`; `GET /api/v1/courses/{courseId}`; `GET /api/v1/courses/{courseId}/lessons`; `GET /api/v1/lessons/{lessonId}`; `GET /api/v1/lessons/{lessonId}/vocabulary`; `GET /api/v1/lessons/{lessonId}/exercises`.
- Source references: FR-DIS-001; FR-DIS-002; BR-CSTATUS-002; UC-STU-BROWSE-COURSES-01; FR-DIS-003; FR-DIS-004; BR-COURSE-005; UC-STU-VIEW-COURSE-01; FR-LES-001; BR-ENR-001; BR-ENR-003; UC-STU-VIEW-LESSON-01; FR-LES-002; FR-LES-004; BR-LCOMP-002; FR-VOC-001; FR-VOC-004; FR-VOC-005; BR-VOC-005; UC-LEARN-VOCABULARY-01; FR-FILL-001; FR-LIS-001; FR-QUIZ-001; BR-EX-001; UC-LEARN-FILL-01; UC-LEARN-LISTENING-01; UC-LEARN-QUIZ-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement published discovery, permitted Lesson summaries and protected Lesson/vocabulary/exercise reads.
- Frontend: Implement approved discovery and learning-content views; separate summaries from restricted content.
- Persistence/migrations: Read existing Course/content/enrollment/entitlement records; no discovery cache table.
- Verification: Verify publication filtering, anonymous policy once approved, enrollment/Premium checks and no answer-key leakage.

### TASK-012 — Lesson authoring and vocabulary assignments

- Purpose/completion outcome: Owned Lesson content and vocabulary assignment operations work with approved completion configuration and preservation rules.
- Feature/phase: FEAT-008; P3.
- Prerequisite tasks: TASK-010, TASK-013.
- Decision gates: G-COMPLETION, G-UI.
- Primary API operations: `GET /api/v1/teacher/courses/{courseId}/lessons`; `POST /api/v1/teacher/courses/{courseId}/lessons`; `PATCH /api/v1/teacher/lessons/{lessonId}`; `GET /api/v1/teacher/lessons/{lessonId}/vocabulary`; `PUT /api/v1/teacher/lessons/{lessonId}/vocabulary/{senseId}`; `DELETE /api/v1/teacher/lessons/{lessonId}/vocabulary/{senseId}`.
- Source references: FR-TLE-001; FR-TLE-006; BR-LESSON-001; UC-TEA-LESSON-01; FR-TLE-003; BR-LESSON-003; FR-TLE-002; FR-TLE-004; FR-TLE-005; BR-VOC-001; UC-TEA-VOCABULARY-01; BR-VOC-005; BR-DATA-002; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement Lesson metadata/order and shared-sense assignment/removal with parent Course ownership.
- Frontend: Implement approved Lesson/sense-selection authoring workflows.
- Persistence/migrations: Map lessons and lesson_vocabulary; required-Lesson configuration waits for G-COMPLETION.
- Verification: Verify cross-owner rejection, sense selection, duplicate assignment prevention and no canonical/history deletion.

### TASK-013 — Platform vocabulary search and details

- Purpose/completion outcome: Platform vocabulary operations work on permitted data with approved normalization; provider integration is independently TASK-014.
- Feature/phase: FEAT-009; P3.
- Prerequisite tasks: TASK-004, TASK-005, TASK-006.
- Decision gates: G-NORMALIZATION, G-DICTIONARY, G-UI.
- Primary API operations: `GET /api/v1/vocabulary`; `GET /api/v1/vocabulary/{vocabularyId}`.
- Source references: FR-SEA-001; FR-SEA-005; FR-TVOC-001; BR-SEARCH-001; UC-VOC-SEARCH-01; FR-SEA-002; FR-SEA-003; FR-VOC-004; BR-SEARCH-002; UC-VOC-DETAIL-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement shared vocabulary/senses and approved search semantics; basic Student search remains available.
- Frontend: Implement approved search/details/sense presentation with honest audio availability.
- Persistence/migrations: Map vocabulary/vocabulary_senses and approved provenance/audio fields; no per-Teacher copies.
- Verification: Verify canonical duplicate handling, search boundaries, multiple senses and restricted/licensed field filtering.

### TASK-014 — Dictionary candidate retrieval and reviewed import

- Purpose/completion outcome: Approved candidate/import contracts work and licensing/storage obligations are met; external-provider behavior is not guessed.
- Feature/phase: FEAT-009; P3.
- Prerequisite tasks: TASK-013.
- Decision gates: G-DICTIONARY, G-NORMALIZATION, G-UI.
- Primary API operations: `GET /api/v1/teacher/dictionary-candidates`; `POST /api/v1/teacher/vocabulary/imports`.
- Source references: FR-TVOC-003; FR-TVOC-004; BR-DIC-001; BR-DIC-004; UC-TEA-VOCABULARY-01; FR-TVOC-005; BR-DIC-003; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement the approved provider boundary, platform-first lookup and reviewed import only after provider/license approval.
- Frontend: Implement approved Teacher candidate review and selection, not arbitrary provider JSON editing.
- Persistence/migrations: Persist only licensed approved content/provenance and deduplicate canonical vocabulary; no provider-specific speculative tables.
- Verification: Use controlled provider substitutes for failures/mapping and verify import permission/deduplication; no unlicensed content copies.

### TASK-015 — Core exercise and question authoring

- Purpose/completion outcome: Approved authoring operations pass validation and ownership checks without leaking answer keys or invalidating historical results.
- Feature/phase: FEAT-010; P3.
- Prerequisite tasks: TASK-012, TASK-013.
- Decision gates: G-COMPLETION, G-INTEGRITY, G-DICTIONARY, G-UI.
- Primary API operations: `GET /api/v1/teacher/lessons/{lessonId}/exercises`; `POST /api/v1/teacher/lessons/{lessonId}/exercises`; `PATCH /api/v1/teacher/exercises/{exerciseId}`; `GET /api/v1/teacher/exercises/{exerciseId}/questions`; `POST /api/v1/teacher/exercises/{exerciseId}/questions`; `PATCH /api/v1/teacher/questions/{questionId}`.
- Source references: FR-TEX-001; FR-TEX-002; FR-TEX-003; BR-EX-001; UC-TEA-EXERCISE-01; FR-TEX-005; BR-EX-002; FR-TEX-004; BR-EX-003; BR-LIS-001; BR-DATA-003; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement three core exercise types and owned authoring DTOs; separate author-only expected answers from learner delivery.
- Frontend: Implement approved question authoring/ordering interactions.
- Persistence/migrations: Map exercises/questions; preserve completed snapshots when content changes; no general versioning system.
- Verification: Verify ownership, Lesson vocabulary relevance, format/options, Listening availability and issued-attempt edit consistency.

### TASK-016 — Enrollment and Course access enforcement

- Purpose/completion outcome: Enrollment and access checks are authoritative, reusable and correct for repeated requests and entitlement changes.
- Feature/phase: FEAT-011; P4.
- Prerequisite tasks: TASK-007, TASK-010, TASK-021.
- Decision gates: G-UI.
- Primary API operations: `PUT /api/v1/me/courses/{courseId}/enrollment`; `GET /api/v1/me/courses`.
- Source references: FR-ENR-001; FR-ENR-002; FR-ENR-003; BR-ENR-004; UC-STU-ENROLL-01; FR-ENR-004; BR-ENR-005; UC-STU-MY-COURSES-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement idempotent Student/Course enrollment, My Courses and shared backend content-access decisions.
- Frontend: Implement approved enrollment/access feedback without per-Course purchasing.
- Persistence/migrations: Use enrollments and entitlement coverage; preserve records after expiration/archive.
- Verification: Verify duplicate enrollment, Standard/Premium rules, archival restrictions and expiry preservation.

### TASK-017 — Lesson attempts, scoring and historical results

- Purpose/completion outcome: All Lesson learning types produce exactly-once accepted evidence/results under the approved protocol and preserve understandable history.
- Feature/phase: FEAT-012; P4.
- Prerequisite tasks: TASK-015, TASK-016.
- Decision gates: G-INTEGRITY, G-RETRY, G-COMPLETION, G-UI.
- Primary API operations: `POST /api/v1/me/attempts`; `GET /api/v1/me/attempts/{attemptId}`; `PUT /api/v1/me/attempts/{attemptId}/answers/{position}`; `POST /api/v1/me/attempts/{attemptId}/completion`; `GET /api/v1/me/attempts/{attemptId}/answers`; `GET /api/v1/me/attempts`.
- Source references: FR-REV-001; FR-REV-006; BR-EX-002; BR-REV-001; UC-LEARN-FILL-01; UC-LEARN-LISTENING-01; UC-LEARN-QUIZ-01; UC-REV-REVIEW-01; FR-PRO-001; FR-PRO-008; BR-HIST-001; UC-LEARN-PROGRESS-01; FR-FILL-005; FR-LIS-005; FR-QUIZ-008; BR-EX-003; BR-FILL-003; BR-LIS-003; FR-PRO-002; BR-SCORE-001; BR-ACC-001; BR-HIST-002; BR-DATA-003; BR-HIST-003; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement trusted issuance/submission/completion, immutable result reads and own-history listing.
- Frontend: Implement approved Fill Word/Listening/Quiz and result/history interactions without client-authoritative scoring.
- Persistence/migrations: Use exercise_attempts/answer_records with original denominators and snapshots; no speculative issuance/retry persistence.
- Verification: Verify forged/foreign submissions, repeated slots, completion races, modified content, score/accuracy and complete historical projection.

### TASK-018 — Derived progress, mastery and weak vocabulary

- Purpose/completion outcome: Approved progress/mastery reads match rules and cannot be forged or destroyed by subscription changes.
- Feature/phase: FEAT-013; P4.
- Prerequisite tasks: TASK-017.
- Decision gates: G-COMPLETION, G-UI.
- Primary API operations: `GET /api/v1/me/courses/{courseId}/progress`; `GET /api/v1/me/vocabulary-performance`.
- Source references: FR-PRO-003; FR-PRO-004; BR-LCOMP-002; BR-CCOMP-001; BR-CCOMP-002; UC-LEARN-PROGRESS-01; FR-PRO-005; FR-PRO-006; FR-PRO-007; BR-MAST-003; BR-WEAK-001; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Derive Course/Lesson progress and vocabulary performance from trusted evidence.
- Frontend: Implement approved progress/history/performance presentation.
- Persistence/migrations: No mutable percentage tables; derive aggregates from existing records.
- Verification: Verify required-section mapping, three-answer minimum, weak/mastery thresholds, repeats and retained progress after expiry.

### TASK-019 — Saved vocabulary

- Purpose/completion outcome: Student can reliably save/list/remove own associations without affecting canonical content/history.
- Feature/phase: FEAT-014; P4.
- Prerequisite tasks: TASK-007, TASK-013.
- Decision gates: G-UI.
- Primary API operations: `GET /api/v1/me/saved-vocabulary`; `PUT /api/v1/me/saved-vocabulary/{vocabularyId}`; `DELETE /api/v1/me/saved-vocabulary/{vocabularyId}`.
- Source references: FR-SAV-002; BR-SAVE-004; UC-VOC-MY-01; FR-SAV-001; BR-SAVE-002; UC-VOC-SAVE-01; FR-SAV-003; BR-SAVE-003; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement own saved-word listing and idempotent save/remove.
- Frontend: Implement approved My Vocabulary interactions.
- Persistence/migrations: Use saved_vocabulary association; never delete shared vocabulary.
- Verification: Verify duplicates, ownership, repeated removal and Premium-expiry preservation.

### TASK-020 — Review using shared attempts and answers

- Purpose/completion outcome: Review works across approved sources with shared trusted persistence and no duplicated learning engine.
- Feature/phase: FEAT-014; P4.
- Prerequisite tasks: TASK-017, TASK-018, TASK-019.
- Decision gates: G-INTEGRITY, G-RETRY, G-COMPLETION, G-REVIEW, G-UI.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: FR-REV-001; FR-REV-002; FR-REV-003; FR-REV-004; FR-REV-005; FR-REV-006; FR-SAV-004; UC-REV-REVIEW-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Reuse TASK-017 for REVIEW context; select approved saved/weak/incorrect/recent sources with eligible access.
- Frontend: Implement approved Review entry/interaction/result flows, not a new assessment engine.
- Persistence/migrations: Use existing attempts/answers; Review has no fake Lesson/Exercise; recent learning derives from answer timestamps.
- Verification: Verify each source, no fake content IDs, trusted scoring, retries, snapshots and approved advanced-access restrictions.

Review shares TASK-017's attempt creation, answer, completion, result and history routes. TASK-017 owns each route once in the catalogue; TASK-020 supplies REVIEW selection/context behavior and integration checks. Both must be complete for the Review feature; no fake Lesson/Exercise or ReviewSession is created.

### TASK-021 — Plan catalogue and authoritative entitlement

- Purpose/completion outcome: Plan/subscription reads and reusable coverage evaluation are verified; no production grant is created without approved payment verification.
- Feature/phase: FEAT-015; P3.
- Prerequisite tasks: TASK-004, TASK-005, TASK-006.
- Decision gates: G-COMMERCE, G-UI.
- Primary API operations: `GET /api/v1/subscription-plans`; `GET /api/v1/me/entitlement`; `GET /api/v1/me/subscriptions`.
- Source references: FR-SUB-001; BR-SUB-001; UC-PRE-SUBSCRIBE-01; FR-SUB-003; FR-SUB-004; FR-SUB-006; BR-SUB-002; UC-PRE-SUBSCRIPTION-01; FR-SUB-002; FR-SUB-005; BR-SUB-004; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement available plan reads and own entitlement/StudentPeriod projections from immutable coverage.
- Frontend: Implement approved plan/subscription displays; Premium benefits remain static approved UI/product content, not a new endpoint.
- Persistence/migrations: Map plans/subscriptions and minimum payment linkage needed for integrity; use synthetic records only in controlled tests until payment integration.
- Verification: Verify current/future/expired coverage, distinct source of truth, safe StudentPeriod and no mutable User Premium flag.

### TASK-022 — Payment lifecycle and immutable grants

- Purpose/completion outcome: Payment flow and retry handling are provider-approved and verified; each successful grant is traceable and historical periods remain immutable.
- Feature/phase: FEAT-016; P5.
- Prerequisite tasks: TASK-007, TASK-021.
- Decision gates: G-PAYMENT, G-RETRY, G-COMMERCE, G-UI.
- Primary API operations: `POST /api/v1/me/payments`; `GET /api/v1/me/payments`; `GET /api/v1/me/payments/{paymentId}`.
- Source references: FR-PAY-001; FR-PAY-002; FR-PAY-006; BR-PAY-001; UC-PRE-SUBSCRIBE-01; BR-PAY-003; FR-PAY-003; FR-PAY-004; BR-PAY-002; BR-PAY-004; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement approved initiation, outcome reads and provider verification; atomically grant once per verified payment.
- Frontend: Implement approved provider continuation/status flow; browser success never grants Premium.
- Persistence/migrations: Use payment_transactions and immutable subscription periods; retain financial snapshots and unsuccessful history; no speculative deduplication column/table.
- Verification: Verify duplicate initiations/events, invalid signatures/results once defined, amount/currency matching, concurrent renewals and failure-without-grant.

Provider notifications/trusted verification are required non-catalogued integration work. No callback method/path is guessed. Provider approval must define that contract; the current inventory remains 72 operations until a separately reviewed API change.

### TASK-023 — Admin users, Teacher provisioning and account actions

- Purpose/completion outcome: Admin inspection and approved provisioning/actions work with verified authorization; unresolved action branches cannot be declared complete.
- Feature/phase: FEAT-017; P5.
- Prerequisite tasks: TASK-007, TASK-009.
- Decision gates: G-ADMIN, G-SECURITY, G-UI.
- Primary API operations: `GET /api/v1/admin/users`; `GET /api/v1/admin/users/{userId}`; `POST /api/v1/admin/teachers`; `POST /api/v1/admin/users/{userId}/account-actions`.
- Source references: FR-ADM-002; FR-ADM-004; BR-ADM-001; UC-ADM-USERS-01; FR-TEA-004; FR-ADM-005; BR-AUTHN-022; BR-PROFILE-001; UC-ADM-TEACHERS-01; FR-ADM-003; FR-ADM-006; BR-AUTHN-018; BR-AUTHN-019; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement role-filtered user inspection and only approved Teacher/account workflows; no generic privilege setter.
- Frontend: Implement approved Admin user/Teacher inspection and allowed actions.
- Persistence/migrations: Reuse users/authentication records; required fullName on creation, existing valid name retained on approved role assignment.
- Verification: Verify Admin authorization, protected fields, provisioning eligibility, action matrix and no Teacher identity duplication.

### TASK-024 — Admin Course inspection and classification

- Purpose/completion outcome: Authorized Admin can inspect/classify Courses; Teacher classification injection and unauthorized mutations fail.
- Feature/phase: FEAT-018; P5.
- Prerequisite tasks: TASK-010, TASK-007.
- Decision gates: G-UI.
- Primary API operations: `GET /api/v1/admin/courses`; `GET /api/v1/admin/courses/{courseId}`; `PUT /api/v1/admin/courses/{courseId}/access-classification`.
- Source references: FR-ACR-001; FR-ACR-002; BR-ADM-002; UC-ADM-COURSES-01; FR-ACR-003; BR-COURSE-006; BR-ADM-003; UC-ADM-CLASSIFY-01; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement Admin list/detail projections and classification operation only.
- Frontend: Implement approved oversight/filter/classification views without granting arbitrary Teacher-style editing.
- Persistence/migrations: Use existing courses; no ownership or schema change.
- Verification: Verify Admin-only STANDARD/PREMIUM changes, permitted detail fields and preserved Teacher edit/ownership/lifecycle rights.

### TASK-025 — Owned-Course Teacher analytics

- Purpose/completion outcome: Teacher analytics match approved definitions and reveal only authorized owned-Course information.
- Feature/phase: FEAT-019; P5.
- Prerequisite tasks: TASK-017, TASK-018.
- Decision gates: G-ANALYTICS, G-UI.
- Primary API operations: `GET /api/v1/teacher/courses/{courseId}/analytics`.
- Source references: FR-TAN-002; FR-TAN-003; FR-TAN-004; FR-TAN-005; FR-TAN-006; BR-TEA-002; BR-TEA-003; UC-TEA-ANALYTICS-01; FR-TAN-001; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement approved aggregate populations/averages for owned Courses.
- Frontend: Implement approved Teacher dashboard/analytics presentation using own Courses and aggregates.
- Persistence/migrations: Derive from existing enrollment/learning records; no analytics warehouse.
- Verification: Verify scope, metric definitions, empty populations, privacy and absence of platform revenue.

### TASK-026 — Admin subscription and payment inspection

- Purpose/completion outcome: Admin can inspect permitted historical periods/payments with accurate mapping and traceability.
- Feature/phase: FEAT-020; P5.
- Prerequisite tasks: TASK-022, TASK-007.
- Decision gates: G-PAYMENT, G-UI.
- Primary API operations: `GET /api/v1/admin/subscriptions`; `GET /api/v1/admin/payments`; `GET /api/v1/admin/payments/{paymentId}`.
- Source references: FR-ASU-001; BR-ADM-004; UC-ADM-SUBSCRIPTIONS-01; FR-ATR-001; FR-ATR-002; FR-ATR-003; BR-ADM-005; UC-ADM-TRANSACTIONS-01; BR-PAY-001; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement authorized period/payment list/detail projections and safe status filters.
- Frontend: Implement approved administrative history/traceability inspection.
- Persistence/migrations: Read existing grants/transactions; no mutable entitlement CRUD or financial deletion.
- Verification: Verify Admin authorization, Student/Admin DTO separation, linkage, outcome mapping and absence of provider secrets.

### TASK-027 — Admin operational Dashboard and revenue analytics

- Purpose/completion outcome: Required dashboard/analytics metrics are accurate and available under approved reporting policy; unavailable provisional metrics cannot count as completed implementation.
- Feature/phase: FEAT-021; P5.
- Prerequisite tasks: TASK-023, TASK-024, TASK-026.
- Decision gates: G-REPORTING, G-PAYMENT, G-UI.
- Primary API operations: `GET /api/v1/admin/dashboard`; `GET /api/v1/admin/analytics/subscriptions`; `GET /api/v1/admin/analytics/revenue`.
- Source references: FR-AAN-007; FR-AAN-002; FR-AAN-003; FR-AAN-004; FR-ASU-003; BR-ADM-007; BR-REVN-002; UC-ADM-ANALYTICS-01; FR-ASU-002; BR-ADM-004; UC-ADM-SUBSCRIPTIONS-01; FR-REVN-001; FR-REVN-002; FR-REVN-003; FR-REVN-004; FR-REVN-006; BR-REVN-003; UC-ADM-REVENUE-01; FR-AAN-001; FR-AAN-005; FR-AAN-006; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Implement shared authoritative dashboard/subscription/revenue aggregates under approved definitions.
- Frontend: Implement approved responsive Admin overview and reporting filters; no expense or accounting screens.
- Persistence/migrations: Aggregate existing users/courses/plans/periods/payments; no reporting tables, warehouse or materialized infrastructure.
- Verification: Verify distinct active Students, complete-history first/renewal classification, failed-payment exclusion, currency separation, time boundaries and no join multiplication.

### TASK-028 — Cross-feature integration and E2E release verification

- Purpose/completion outcome: Required cross-feature checks pass with evidence; unresolved critical behavior or failing checks prevents completion.
- Feature/phase: FEAT-022; P6.
- Prerequisite tasks: TASK-008, TASK-009, TASK-011, TASK-017, TASK-018, TASK-020, TASK-022, TASK-023, TASK-024, TASK-025, TASK-026, TASK-027, TASK-014.
- Decision gates: G-UI.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-UI-002; NFR-UI-004; NFR-UI-005; NFR-DATA-005; NFR-SEC-006; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Exercise integrated security/business behavior; fix defects through owning tasks rather than duplicate features.
- Frontend: Verify approved responsive Student/Teacher/Admin journeys and accessibility requirements.
- Persistence/migrations: Verify deployed-compatible migrations, historical preservation and transactional integration on representative PostgreSQL.
- Verification: TASK-028 remains the primary task for cross-feature browser E2E/release verification using Playwright (@playwright/test). Run required end-to-end success/rejection journeys with controlled provider substitutes; record actual results and remaining defects.

### TASK-029 — Deployment preparation and operational checks

- Purpose/completion outcome: Approved target environments and operational checks are complete with evidence; no deployment/provider choice is inferred by this plan.
- Feature/phase: FEAT-022; P6.
- Prerequisite tasks: TASK-003, TASK-028.
- Decision gates: G-DEPLOY.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-MNT-004; NFR-SEC-005; NFR-MNT-001; NFR-MNT-003; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Configure only approved hosting/runtime/secrets and Supabase database connection after authorization.
- Frontend: Deploy to approved frontend host with correct backend API boundary; no direct database path.
- Persistence/migrations: Apply approved migrations through an approved release process; determine pooling/backups operationally without redesigning schema.
- Verification: Verify environment startup, HTTPS/auth transport, provider callbacks when approved, database access and recovery/release procedure.

### TASK-030 — CI checks and approved release automation

- Purpose/completion outcome: Build/check automation and the approved release scope are verified. Early CI completion does not mark the combined task DONE while required release automation remains.
- Feature/phase: FEAT-022; P1/P6.
- Prerequisite tasks: TASK-001, TASK-002.
- Decision gates: G-TOOLCHAIN, G-DEPLOY.
- Primary API operations: None; capability/foundation work, not an invented route.
- Source references: NFR-MNT-003; NFR-SEC-005; NFR-MNT-001; NFR-MNT-005; NFR-MNT-006; NFR-MNT-010; NFR-SEC-003; NFR-SEC-004.
- Backend: Automate the approved build and relevant checks as they become available; do not create an unnecessary pipeline framework.
- Frontend: Include approved frontend build/checks with backend checks.
- Persistence/migrations: Integrate approved migration validation when TASK-004 exists; no unattended schema changes before release approval.
- Verification: Verify pipeline success/failure behavior and secret handling; release stage requires TASK-028 and TASK-029 readiness.

## 6. Decision gates and latest resolution points

Gate IDs are planning cross-references, not new product policies. Resolve only affected decisions; retain already-approved defaults. Where a decision may affect schema, review that specific change before migration or implementation. Do not reinterpret provisional as permission to choose silently.

| Gate | Unresolved dependency | Latest resolution point | Affected tasks |
|---|---|---|---|
| G-TOOLCHAIN | Exact supported versions/build/package-management choices; choose only justified tooling when implementation is authorized. | Before TASK-001/002 builds or TASK-030 automation; not a product-scope decision. | TASK-001, TASK-002, TASK-030 |
| G-UI | UI/UX is not documented complete. Navigation, layout, screens, flows and interactions require approved design; responsive/accessibility requirements alone do not specify it. | Before product UI in any task; TASK-002 can deliver neutral technical infrastructure only. | TASK-002, TASK-007, TASK-008, TASK-009, TASK-010, TASK-011, TASK-012, TASK-013, TASK-014, TASK-015, TASK-016, TASK-017, TASK-018, TASK-019, TASK-020, TASK-021, TASK-022, TASK-023, TASK-024, TASK-025, TASK-026, TASK-027, TASK-028 |
| G-ENV | Environment credentials and authorized connection settings; no secrets in documentation or browser. | Before actual Supabase connection in TASK-003; local database work may proceed independently. | TASK-003 |
| G-SCHEMA | Draft CEFR nullability, provisional credential/payment uniqueness and other flagged database constraints remain subject to their source decisions. | Before affected irreversible migrations in TASK-004; preserve exactly the approved 18-table scope. | TASK-004 |
| G-NORMALIZATION | Exact email comparison, vocabulary canonical keys, search matching and conflicts are open. | Before affected uniqueness migration and TASK-007/013/014 normalization-dependent behavior. | TASK-004, TASK-007, TASK-013, TASK-014 |
| G-AUTH | AUTH-WIRE: transport, browser storage, cookies/CORS/CSRF; refresh rotation/replay/concurrency, session selection/limits and retry policy remain open. | Before transport/session code in TASK-006/007; finalized routes elsewhere do not bypass this shared gate. | TASK-006, TASK-007 |
| G-SECURITY | Password policy/encoding parameters, signing configuration, verification lifetime/resend, password-change/reset session invalidation and credential cleanup remain open where source documents say so. | Before affected security work in TASK-006/007/008/023; preserve all already-approved defaults. | TASK-006, TASK-007, TASK-008, TASK-023 |
| G-DISCOVERY | Anonymous availability of the two Course discovery reads remains open. | Before anonymous access configuration and final discovery integration in TASK-011. | TASK-011 |
| G-COMPLETION | Required Lesson/assessment mapping, zero-denominator/skipped completion behavior, thresholds and effects of content edits remain open. | Before dependent TASK-012/015 authoring and TASK-017/018/020 completion/progress behavior. | TASK-012, TASK-015, TASK-017, TASK-018, TASK-020 |
| G-DICTIONARY | Provider selection, licensing including history snapshots, retainable fields, candidate/import mapping and audio/fallback strategy remain open. | Before external retrieval/storage or dependent audio delivery in TASK-013/014/015; use only permitted data. | TASK-013, TASK-014, TASK-015 |
| G-INTEGRITY | Original issued-question binding, confidential integrity/persistence protocol, resume, expiry and feedback timing are not selected. | Before TASK-015 edit interactions or TASK-017/020 issuance/submission. A schema change, if actually needed, requires separate approval. | TASK-015, TASK-017, TASK-020 |
| G-RETRY | Attempt-creation identity and durable payment-initiation deduplication remain undecided. | Before creation paths in TASK-017/020/022; do not invent persistence or imply slot uniqueness solves creation retries. | TASK-017, TASK-020, TASK-022 |
| G-REVIEW | Recent-learning window and final advanced/Premium Review restrictions remain open. | Before dependent TASK-020 selection/access behavior; preserve approved basic sources and tier distinction. | TASK-020 |
| G-COMMERCE | Exact price/currency configuration, calendar boundaries and final advanced Premium benefits remain open. | Before dependent TASK-021/022 configuration/grant calculation or advanced benefits; no new benefits route. | TASK-021, TASK-022 |
| G-PAYMENT | Provider/verification callback, allowlisted continuation, status/reference mapping, failure timestamps and refund treatment remain open. | Before TASK-022 integration and dependent TASK-026/027 projections; no guessed callback route or schema. | TASK-022, TASK-026, TASK-027 |
| G-ADMIN | Teacher credentials/provisioning/role-assignment transitions and allowed account-action matrix remain open. | Before TASK-023 mutations; read-only authorized inspection can proceed. | TASK-023 |
| G-ANALYTICS | Teacher analytics population/averaging definitions remain open. | Before final TASK-025 aggregate semantics; never expose unrelated Teacher data. | TASK-025 |
| G-REPORTING | Timezone/ranges/boundaries/buckets, total-count status filters, disabled-Student inclusion, grant/failure-event timing, refunds and freshness remain open. | Before dependent TASK-027 reports; verified_at revenue, currency separation and full-history first/renewal meaning remain fixed. | TASK-027 |
| G-DEPLOY | Next.js/Spring Boot hosts, pooling/backups/secrets, release process and retention/performance operations remain undecided. | Before TASK-029 and release portions of TASK-030; Supabase PostgreSQL hosting is already approved. | TASK-029, TASK-030 |

Detailed deletion/retirement/Teacher-transfer workflows remain unresolved; implement only approved archive/disable and restrictive preservation rules. No deletion/transfer endpoint is added. Schema and operation-specific gates are distinct: a FINAL API can still depend on shared transport, UI or persistence decisions.

## 7. Complete API ownership and provisional mapping

All routes below include /api/v1. Each method/route has exactly one primary implementation task. Feature mapping is through section 4; shared integration does not create a second primary owner. Read the full operation in API_DESIGN.md for DTOs, permissions, validation, statuses and references. Gate columns identify contract-specific provisional dependencies; common gates also apply.

### 7.1 Authentication and account lifecycle

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| POST | /api/v1/auth/register | TASK-007 | PROVISIONAL | G-SECURITY, G-NORMALIZATION: Provisional email/password policy. |
| POST | /api/v1/auth/email-verification | TASK-007 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/auth/email-verification/resend | TASK-007 | PROVISIONAL | G-SECURITY: Provisional resend policy. |
| POST | /api/v1/auth/login | TASK-007 | PROVISIONAL | G-AUTH: Provisional AUTH-WIRE. |
| POST | /api/v1/auth/refresh | TASK-007 | PROVISIONAL | G-AUTH: Provisional AUTH-WIRE and rotation. |
| POST | /api/v1/auth/logout | TASK-007 | PROVISIONAL | G-AUTH: Provisional AUTH-WIRE. |
| POST | /api/v1/auth/password/change | TASK-008 | PROVISIONAL | G-SECURITY: Provisional password/session policy. |
| POST | /api/v1/auth/password/forgot | TASK-008 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/auth/password/reset | TASK-008 | PROVISIONAL | G-SECURITY: Provisional password/session policy. |

### 7.2 Current User and personal profile

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/me | TASK-009 | FINAL | No operation-specific provisional dependency |
| PATCH | /api/v1/me/profile | TASK-009 | FINAL | No operation-specific provisional dependency |

### 7.3 Course discovery and Student Lesson access

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/courses | TASK-011 | PROVISIONAL | G-DISCOVERY: Anonymous availability provisional. |
| GET | /api/v1/courses/{courseId} | TASK-011 | PROVISIONAL | G-DISCOVERY: Anonymous availability provisional. |
| GET | /api/v1/courses/{courseId}/lessons | TASK-011 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/lessons/{lessonId} | TASK-011 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/lessons/{lessonId}/vocabulary | TASK-011 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/lessons/{lessonId}/exercises | TASK-011 | FINAL | No operation-specific provisional dependency |

### 7.4 Teacher Course management

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| POST | /api/v1/teacher/courses | TASK-010 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/teacher/courses | TASK-010 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/teacher/courses/{courseId} | TASK-010 | FINAL | No operation-specific provisional dependency |
| PATCH | /api/v1/teacher/courses/{courseId} | TASK-010 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/teacher/courses/{courseId}/publish | TASK-010 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/teacher/courses/{courseId}/archive | TASK-010 | FINAL | No operation-specific provisional dependency |

### 7.5 Teacher Lesson management

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/teacher/courses/{courseId}/lessons | TASK-012 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/teacher/courses/{courseId}/lessons | TASK-012 | PROVISIONAL | G-COMPLETION: Required-completion configuration provisional. |
| PATCH | /api/v1/teacher/lessons/{lessonId} | TASK-012 | FINAL | No operation-specific provisional dependency |

### 7.6 Shared vocabulary and Teacher dictionary usage

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/vocabulary | TASK-013 | PROVISIONAL | G-NORMALIZATION: search matching/normalization semantics. |
| GET | /api/v1/vocabulary/{vocabularyId} | TASK-013 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/teacher/dictionary-candidates | TASK-014 | PROVISIONAL | G-DICTIONARY, G-NORMALIZATION: Provider/license-dependent. |
| POST | /api/v1/teacher/vocabulary/imports | TASK-014 | PROVISIONAL | G-DICTIONARY, G-NORMALIZATION: Provider/license/normalization-dependent. |

### 7.7 Lesson vocabulary assignments

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/teacher/lessons/{lessonId}/vocabulary | TASK-012 | FINAL | No operation-specific provisional dependency |
| PUT | /api/v1/teacher/lessons/{lessonId}/vocabulary/{senseId} | TASK-012 | FINAL | No operation-specific provisional dependency |
| DELETE | /api/v1/teacher/lessons/{lessonId}/vocabulary/{senseId} | TASK-012 | FINAL | No operation-specific provisional dependency |

### 7.8 Exercise and question management

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/teacher/lessons/{lessonId}/exercises | TASK-015 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/teacher/lessons/{lessonId}/exercises | TASK-015 | PROVISIONAL | G-COMPLETION: Required-completion configuration provisional. |
| PATCH | /api/v1/teacher/exercises/{exerciseId} | TASK-015 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/teacher/exercises/{exerciseId}/questions | TASK-015 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/teacher/exercises/{exerciseId}/questions | TASK-015 | FINAL | No operation-specific provisional dependency |
| PATCH | /api/v1/teacher/questions/{questionId} | TASK-015 | PROVISIONAL | G-INTEGRITY: Depends on issued-question integrity protocol. |

### 7.9 Enrollment

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| PUT | /api/v1/me/courses/{courseId}/enrollment | TASK-016 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/me/courses | TASK-016 | FINAL | No operation-specific provisional dependency |

### 7.10 Attempts, answer submission and results

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| POST | /api/v1/me/attempts | TASK-017 | PROVISIONAL | G-INTEGRITY, G-RETRY, G-REVIEW: Issued-question binding and creation retry protocol provisional. |
| GET | /api/v1/me/attempts/{attemptId} | TASK-017 | FINAL | No operation-specific provisional dependency |
| PUT | /api/v1/me/attempts/{attemptId}/answers/{position} | TASK-017 | PROVISIONAL | G-INTEGRITY: Issued-question binding and feedback timing provisional. |
| POST | /api/v1/me/attempts/{attemptId}/completion | TASK-017 | PROVISIONAL | G-COMPLETION: Completion eligibility provisional. |
| GET | /api/v1/me/attempts/{attemptId}/answers | TASK-017 | FINAL | No operation-specific provisional dependency |

### 7.11 Progress and history

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/me/attempts | TASK-017 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/me/courses/{courseId}/progress | TASK-018 | PROVISIONAL | G-COMPLETION: Required-section mapping/content-change effects provisional. |
| GET | /api/v1/me/vocabulary-performance | TASK-018 | FINAL | No operation-specific provisional dependency |

### 7.12 Saved vocabulary

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/me/saved-vocabulary | TASK-019 | FINAL | No operation-specific provisional dependency |
| PUT | /api/v1/me/saved-vocabulary/{vocabularyId} | TASK-019 | FINAL | No operation-specific provisional dependency |
| DELETE | /api/v1/me/saved-vocabulary/{vocabularyId} | TASK-019 | FINAL | No operation-specific provisional dependency |

### 7.13 Plans and current entitlement

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/subscription-plans | TASK-021 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/me/entitlement | TASK-021 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/me/subscriptions | TASK-021 | FINAL | No operation-specific provisional dependency |

### 7.14 Student payment lifecycle

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| POST | /api/v1/me/payments | TASK-022 | PROVISIONAL | G-PAYMENT, G-RETRY: provider, allowlisted continuation and initiation deduplication. |
| GET | /api/v1/me/payments | TASK-022 | PROVISIONAL | G-PAYMENT: Outcome filter vocabulary provisional. |
| GET | /api/v1/me/payments/{paymentId} | TASK-022 | PROVISIONAL | G-PAYMENT: shared public Payment outcome mapping. |

### 7.15 Teacher analytics

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/teacher/courses/{courseId}/analytics | TASK-025 | PROVISIONAL | G-ANALYTICS: Detailed analytics population/averaging provisional. |

### 7.16 Admin users and Teacher administration

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/admin/users | TASK-023 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/admin/users/{userId} | TASK-023 | FINAL | No operation-specific provisional dependency |
| POST | /api/v1/admin/teachers | TASK-023 | PROVISIONAL | G-ADMIN: Provisioning credentials, verification and role-assignment transitions provisional. |
| POST | /api/v1/admin/users/{userId}/account-actions | TASK-023 | PROVISIONAL | G-ADMIN: Not implementable until account-action matrix is approved. |

### 7.17 Admin Course administration

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/admin/courses | TASK-024 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/admin/courses/{courseId} | TASK-024 | FINAL | No operation-specific provisional dependency |
| PUT | /api/v1/admin/courses/{courseId}/access-classification | TASK-024 | FINAL | No operation-specific provisional dependency |

### 7.18 Admin subscription and payment administration

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/admin/subscriptions | TASK-026 | FINAL | No operation-specific provisional dependency |
| GET | /api/v1/admin/payments | TASK-026 | PROVISIONAL | G-PAYMENT: Provider outcome mapping provisional. |
| GET | /api/v1/admin/payments/{paymentId} | TASK-026 | PROVISIONAL | G-PAYMENT: shared public Payment outcome mapping. |

### 7.19 Admin Dashboard and approved analytics

| Method | Route | Primary task | Contract | Gate/dependency |
|---|---|---|---|---|
| GET | /api/v1/admin/dashboard | TASK-027 | PROVISIONAL | G-REPORTING: Reporting semantics flagged in section 9. |
| GET | /api/v1/admin/analytics/subscriptions | TASK-027 | PROVISIONAL | G-REPORTING: Grant-event time and disabled-Student treatment open. |
| GET | /api/v1/admin/analytics/revenue | TASK-027 | PROVISIONAL | G-REPORTING: Reporting bucket policy open. |

## 8. Completion evidence and handoff

Record actual implementation paths, verification commands/results and commit references when available in FEATURE_STATUS.md. Never invent evidence, use placeholder hashes or interpret this plan as successful tests. For partial work, state remaining behavior and the concrete gate if work cannot proceed.

Before declaring completion, verify cross-role/foreign-owner rejection, nested Teacher ownership, profile protected fields, immutable learning history, Review context, trusted scoring/replay handling, payment verification/deduplication, safe projections and currency-separated analytics as applicable. Preserve approved responsive/accessibility requirements without inventing UI decisions.

This plan adds no API operation, schema change, code, SQL, migration, deployment configuration or test execution. Design readiness and implementation progress remain separate.
