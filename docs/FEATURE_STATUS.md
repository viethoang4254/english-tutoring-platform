# English Tutoring Platform — Feature Status

## 1. Authority and Status Semantics

This is the sole authoritative live implementation-status and completion-evidence ledger. [TASK_BREAKDOWN.md](TASK_BREAKDOWN.md) owns work, scope, prerequisites, acceptance boundaries, planning readiness and decision-gate definitions. This ledger does not redefine requirements, duplicate task specifications or authorize implementation.

Follow AGENTS.md and the reconciled PROJECT_SPEC, REQUIREMENTS, BUSINESS_RULES, USE_CASES, DOMAIN_MODEL, ARCHITECTURE, DATABASE_DESIGN and API_DESIGN. Apply the relevant project-local skills. Design responsibility is distinct from implemented and verified behavior.

Use only these live implementation statuses:

| Status | Meaning |
|---|---|
| TODO | Implementation has not started; applicable prerequisites and gates still apply. Safe future work does not mean all contracts are ready. |
| IN_PROGRESS | Authorized implementation genuinely started; full acceptance and verification are not complete. |
| BLOCKED | A concrete central dependency/decision prevents meaningful core implementation. Record the blocker and what approval/evidence would remove it. |
| DONE | Applicable acceptance criteria and required verification passed, with actual evidence and attribution. |

Planning readiness is derived from TASK_BREAKDOWN, not independently decided here. Its DEFERRED label is not a fifth live status. Historical disposition RETIRED belongs only to inactive history; it is not a live status.

The planning-readiness column is a reference snapshot of TASK_BREAKDOWN. Completed foundations are labelled Preserved foundation, not assigned a new readiness enum. Retired records are excluded from active totals.

## 2. Current Implementation Summary

The repository contains the Spring Boot technical foundation, Next.js technical foundation, environment-bound datasource/JPA/PostgreSQL connectivity setup, foundation tests and a neutral frontend shell. Completed connectivity includes recorded local and Supabase development verification.

Flyway and the approved 22-table Physical V1 migration are implemented and verified on disposable local PostgreSQL under TASK-004. No Supabase schema deployment is claimed. Tutoring business controllers, entities/repositories, authentication, production deployment and CI remain unimplemented.

| Active task status | Count |
|---|---:|
| DONE | 5 |
| TODO | 24 |
| BLOCKED | 8 |
| IN_PROGRESS | 0 |
| Total | 37 |

Planning readiness: 26 TODO, 8 DEFERRED and 3 preserved completed foundations. Eight retired task IDs are tracked separately. TASK-031 is DONE for documentation/operational guidance only. This documentation reconciliation does not establish application implementation or verification.

## 3. Active Task Ledger

Each current active task appears exactly once below. Responsibilities and readiness derive from TASK_BREAKDOWN; the evidence/blocker column does not replace its full gate and dependency list. TODO is not permission to bypass a prerequisite or open contract.

| Task | Responsibility | Implementation status | Planning readiness | Current blocker / evidence pointer |
|---|---|---|---|---|
| TASK-001 | Backend technical foundation | DONE | Preserved foundation | Preserved evidence: Section 8, TASK-001. |
| TASK-002 | Frontend technical foundation | DONE | Preserved foundation | Preserved evidence: Section 8, TASK-002. |
| TASK-003 | PostgreSQL connectivity foundation | DONE | Preserved foundation | Preserved evidence: Section 8, TASK-003. |
| TASK-004 | Physical Database V1 migration and persistence integrity | DONE | TODO | Flyway V1 and isolated PostgreSQL verification passed; see Section 8, TASK-004. |
| TASK-005 | Shared API and client boundary conventions | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-006 | Authentication and backend authorization foundation | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-007 | Student registration and shared authentication sessions | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-008 | Dedicated password change and recovery | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-009 | Current account and own personal profile | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-010 | Teacher-owned Course management | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-011 | Public Teacher and Course discovery | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-016 | Whole-Course Enrollment and participation authorization | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-018 | CourseProgress and own learning-progress visibility | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-022 | Whole-Course payment and trustworthy transaction effects | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-023 | Admin account oversight and permitted account management | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-024 | Admin Course oversight | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-025 | Teacher owned-Course Student/progress monitoring | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-026 | Admin transaction/status monitoring and refund verification | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-027 | Admin operational statistics | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-028 | Tutoring cross-feature E2E and release verification | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-029 | Deployment and operational readiness | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-030 | Safe continuous build/test verification | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-031 | Reconcile downstream tracking and operational guidance | DONE | TODO | Documentation acceptance passed; see Section 8, TASK-031. |
| TASK-032 | Teacher registration and controlled onboarding | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-033 | Teacher teaching profile and public projection | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-034 | Admin category/topic management and Teacher selection | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-035 | Teacher management of Course Sessions | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-036 | Protected Student Session and content access | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-037 | Recurring Course schedule and individual Session adjustments | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-038 | Session Assignment management and authorized viewing | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-039 | Student Assignment Submission | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-040 | Owned-Course Submission inspection and Student result visibility | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-041 | Private Teacher PaymentReceivingInformation | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-042 | Manual Course Meet configuration and protected access | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-043 | Calendar schedule/reminder capability | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-044 | ParticipantFeedback and rating | BLOCKED | DEFERRED | No implementation evidence; see Section 5. |
| TASK-045 | Continuous cross-feature authorization and trust verification | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |

TASK-031 is complete for downstream tracking and README operational guidance. Its planning readiness remains TODO as recorded in TASK_BREAKDOWN; completion does not count as business implementation.

## 4. Preserved Completed Foundations

TASK-001, TASK-002 and TASK-003 remain DONE. Their original evidence is retained in Section 8; no build, browser test, startup or database connection was rerun for this reconciliation.

- TASK-001 proves Java 17/Spring Boot build, context, packaging and recorded packaged-JAR startup only.
- TASK-002 proves reproducible Next.js setup and responsive neutral technical rendering only, not tutoring screens, navigation or approved product UI/UX.
- TASK-003 proves environment configuration and local/development connectivity only, not application schema, migration installation or production database readiness.

### FEAT-001 — Development/runtime foundation

- Overall status: IN_PROGRESS.
- Component status: Backend IN_PROGRESS; Frontend IN_PROGRESS; Persistence DONE; Tests IN_PROGRESS.
- Related tasks: TASK-001, TASK-002, TASK-003 and TASK-005.
- Remaining work: TASK-005 shared API/client conventions and their required verification; applicable G-API/G-UI decisions remain in TASK_BREAKDOWN.
- Persistence DONE means foundation connectivity only. It does not mark the separate migration foundation or any domain persistence complete.
- Evidence: Section 8. The business-scope migration does not invalidate these completed checks.

Compatible existing feature IDs retain their current meaning below. Their scope is defined by the associated current tasks, not by historical completion definitions. No new feature-ID taxonomy is needed to represent the new task capabilities.

| Feature | Current responsibility | Overall status | Related current tasks |
|---|---|---|---|
| FEAT-002 | Physical Database V1 migration/persistence foundation | DONE | TASK-004 |
| FEAT-003 | Authentication/authorization foundation | TODO | TASK-006 |
| FEAT-004 | Student registration, shared sessions and password security | TODO | TASK-007, TASK-008 |
| FEAT-005 | Current account and own personal profile | TODO | TASK-009 |
| FEAT-006 | Public Teacher/Course discovery | TODO | TASK-011 |
| FEAT-007 | Teacher-owned Course management | TODO | TASK-010 |
| FEAT-011 | Whole-Course Enrollment and participation access | TODO | TASK-016 |
| FEAT-013 | CourseProgress | BLOCKED | TASK-018 |
| FEAT-016 | Course payment and trustworthy effects | BLOCKED | TASK-022 |
| FEAT-017 | Limited Admin account oversight | TODO | TASK-023 |
| FEAT-018 | Admin Course oversight | TODO | TASK-024 |
| FEAT-019 | Teacher owned-Course monitoring | TODO | TASK-025 |
| FEAT-020 | Admin transaction/status monitoring and refund verification | TODO | TASK-026 |
| FEAT-021 | Admin operational statistics | TODO | TASK-027 |
| FEAT-022 | Tutoring release verification, operations and safe CI | TODO | TASK-028, TASK-029, TASK-030 |

All unfinished feature rows have no implementation evidence; component work remains unimplemented and subject to the associated task blockers. FEAT-022 has queued meaningful verification/CI work despite its deployment task being blocked. Task completion and combined feature completion are assessed separately. New tutoring responsibilities are tracked by TASK-031 through TASK-045 and Section 6, without reusing retired feature IDs.

## 5. Blocked / Deferred-Readiness Work

The following core implementation cannot proceed until the named decisions are approved. Each task is BLOCKED in the live ledger and DEFERRED in TASK_BREAKDOWN planning. Preparing independent prerequisites does not resolve its central gate.

| Task | Current blocker / required decision |
|---|---|
| TASK-018 | G-PROGRESS/G-RESULT: derived representation is settled; formulas, result availability and applicable API contracts remain open. |
| TASK-022 | G-PAYMENT/G-CONFIRMATION/G-OUTCOME/G-PAYEFFECT: VietQR, trustworthy bank evidence and full refunds are settled directions; concrete provider/authenticity, retry/late and refund contracts remain open. |
| TASK-029 | G-DEPLOY: approve production hosting, secrets and operational/release procedures. |
| TASK-032 | G-TEACHER/G-SECURITY/G-API: separate registration and verified/unlocked/approved authority are settled; review-operation validation and dependent executable contracts remain open. |
| TASK-037 | G-TIME/G-RECURRENCE/G-SESSION: ISO weekly generation, IANA timezone, fixed unique numbering and no-replacement cancellation settled; DST/cutoff and operation exceptions remain open. |
| TASK-041 | G-RECEIVING/G-PAYMENT: historical bank accounts and one ACTIVE settled; bank checks and verified adapter integration contracts remain open; conceptual provider fields are omitted. |
| TASK-043 | G-CALENDAR: organizational synchronization settled; single configured Calendar/event-ID scope settled; execution/retry contracts remain open. |
| TASK-044 | G-FEEDBACK/G-API: one completed-Enrollment rating 1..5 settled; exact acceptance/visibility/edit/moderation and executable contracts remain open. |

These entries record missing decisions, not failures of the completed infrastructure. Do not invent schema, integration or policy to remove a blocker. Other tasks remain TODO where meaningful safe prerequisite work can later be authorized.

## 6. Current Capability Summary

Approved/designed responsibilities below are not implemented or verified. Exact candidate API and physical contracts remain gated as described upstream.

| Capability | Current implementation/evidence boundary | Task ownership |
|---|---|---|
| Authentication/account security | Student/Teacher registration, login, JWT, RefreshSession, refresh/logout, email verification/resend and password change/forgot/reset are not implemented. No Spring Security/JWT implementation is present. Teacher onboarding remains blocked. | TASK-006, TASK-007, TASK-008, TASK-032 |
| User/Teacher profiles | Current account, own personal profile, specialization/experience/introduction and safe Teacher public projection are not implemented; exact fields remain gated. | TASK-009, TASK-033 |
| Public discovery | Teacher/Course search/filter and public Course detail are not implemented; protected participation data must remain separate. | TASK-011 |
| Courses | Teacher-owned Course creation/management/publication is not implemented; owner derives from backend identity and five-state lifecycle is approved but detailed operation contracts remain gated. | TASK-010 |
| Categories/topics | Admin management and Teacher selection are not implemented. | TASK-034 |
| Enrollment | Whole-Course participation and protected access are not implemented. Payment is distinct; no lifecycle/status/uniqueness implementation is claimed. | TASK-016 |
| Sessions/scheduling | Course Sessions, protected reads and recurring/individual scheduling are not implemented; scheduling decisions block TASK-037. | TASK-035, TASK-036, TASK-037 |
| Assignments | Owned-Session management and authorized Student viewing are not implemented. | TASK-038 |
| Submissions/results | Authenticated Student authorship, Teacher inspection and own-result visibility are not implemented; one current Submission and owning-Teacher grading on it are approved; detailed validation/contracts remain gated. | TASK-039, TASK-040 |
| CourseProgress | No implementation; derived representation is approved; calculation/projection remain unresolved. | TASK-018 |
| PaymentReceivingInformation | No implementation; historical bank accounts/one ACTIVE are approved; provider verification and concrete contracts remain gated. | TASK-041 |
| Course payments/transactions | No implementation of VietQR execution, trustworthy bank-evidence confirmation, reconciliation or full refund. Detailed provider/retry contracts remain dependencies; Teacher/Admin claims cannot confirm payment. | TASK-022 |
| Google Meet | No implementation. Approved direction is one manually configured protected Course URL shared by all its Sessions; Student access requires authorized participation. No Meet API, OAuth or automatic creation. | TASK-042 |
| Google Calendar | No implementation; organizational Google Calendar synchronization is approved, with configured-calendar/identifier and delivery contracts still blocked. | TASK-043 |
| ParticipantFeedback | No implementation; one COMPLETED-Enrollment rating 1..5 is approved; exact DTO/visibility/edit/moderation remains gated. | TASK-044 |
| Admin / Teacher monitoring | Account/Course oversight, category and Enrollment monitoring, transaction monitoring, operational counts and owned-Course progress monitoring are not implemented. Teacher tuition is not platform/Admin revenue. | TASK-016, TASK-023, TASK-024, TASK-025, TASK-026, TASK-027, TASK-034 |
| Cross-feature security | No tutoring authorization verification: wrong-Teacher ownership, participation denial, cross-Student privacy, protected Meet, private receiving information and payment trust cases remain future work. | TASK-045 |
| Tutoring E2E | Foundation tests do not prove integrated tutoring journeys. Playwright browser checks remain separate from Spring Boot/JUnit/backend integration tests. | TASK-028 |
| Deployment / CI | Production hosting is unresolved; no deployment or CI implementation. Safe build/test CI is separate from later release automation. Development Supabase connectivity is not deployment. | TASK-029, TASK-030 |

The approved payment trust path remains Course -> owning Teacher -> authoritative tuition -> receiving destination -> payment -> trustworthy confirmation -> only the approved Enrollment effect. This is a design boundary, not implemented behavior. Client identity, amount, recipient or destination cannot become authoritative. Teacher positive/negative testimony, Admin role and browser/Student claims alone do not confirm payment; unknown evidence is not confirmed failure. No platform tuition custody or accounting subsystem is approved here.

## 7. Retired Historical Tasks / Features

These records are historical only and excluded from active status/readiness totals. IDs are reserved permanently; no implementation evidence is transferred to current tutoring concepts.

| Historical task | Historical responsibility | Historical disposition | Last recorded implementation status | Implementation evidence |
|---|---|---|---|---|
| TASK-012 | Vocabulary Lesson content | RETIRED | TODO | None |
| TASK-013 | Vocabulary search/details | RETIRED | TODO | None |
| TASK-014 | Dictionary integration | RETIRED | TODO | None |
| TASK-015 | Exercise/question authoring | RETIRED | TODO | None |
| TASK-017 | Attempts/answers/scoring | RETIRED | TODO | None |
| TASK-019 | Saved vocabulary | RETIRED | TODO | None |
| TASK-020 | Vocabulary Review | RETIRED | TODO | None |
| TASK-021 | Subscription plans/entitlement | RETIRED | TODO | None |

| Historical feature | Historical responsibility | Disposition / last status / evidence |
|---|---|---|
| FEAT-008 | Lesson/content authoring | RETIRED / TODO / none |
| FEAT-009 | Shared vocabulary and dictionary integration | RETIRED / TODO / none |
| FEAT-010 | Exercise/question authoring | RETIRED / TODO / none |
| FEAT-012 | Attempts, answer evaluation and historical results | RETIRED / TODO / none |
| FEAT-014 | Saved vocabulary and Review | RETIRED / TODO / none |
| FEAT-015 | Subscription plans and entitlement | RETIRED / TODO / none |

Session is not renamed Lesson; Assignment is not Exercise/Quiz; Submission is not Attempt/AnswerRecord; ParticipantFeedback is not vocabulary Review; Enrollment is not Subscription. SavedVocabulary, Standard/Premium access/classification, subscription revenue, CEFR and vocabulary mastery/scoring are retired scope, not current completion criteria.

The former 22-feature, 30-task, 72-operation and 18-table baseline is historical. Its FINAL/PROVISIONAL counts do not establish a current API catalogue or endpoint implementation. All 30 original task IDs are preserved as active compatible IDs or retired history, and the 15 new task IDs have independent current responsibilities.

## 8. Evidence Register

The following records preserve earlier evidence and attribution. Their historical dates, commands, scope statements and outcomes describe the original verification sessions, not checks rerun by this rewrite. Historical references to changed files or empty Git indexes concern those sessions. Reports/build artifacts may be overwritten by later runs.

### TASK-001 — Recorded backend foundation evidence

- Evidence: TASK-001 skeleton in backend/, .gitignore and README.md; directories and build/run commands documented, no domain implementation or unapproved infrastructure. On 2026-09-19, wrapper scripts matched official 3.3.4 only-script files before the approved one-line mvnw.cmd null-Target guard repair. Agent-observed verification with Oracle JDK 17.0.12: `mvnw.cmd --version` confirmed Maven 3.9.16; `mvnw.cmd clean verify` reported BUILD SUCCESS, Java release 17 compilation, 1 application-context test passed (0 failures/errors/skips), and successful executable-JAR packaging. Test report: backend/target/surefire-reports/com.englishlearning.EnglishLearningApplicationTests.txt; artifact: backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar. User-reported manual verification in ordinary Windows PowerShell outside the Codex execution environment used `C:\Program Files\Java\jdk-17\bin\java.exe` with that packaged JAR and `--server.address=127.0.0.1 --server.port=18080`: Tomcat started on 127.0.0.1:18080 with context path /; `Started EnglishLearningApplication in 2.957 seconds` was observed; the process remained alive until manually stopped. No database credentials or external services were required. Manual startup/stop evidence is user-reported, not agent-observed.

- Verification environment note: Earlier Codex-launched runs, including elevated retries, failed in Java AF_UNIX/Selector initialization. The user also confirmed direct UNIX-domain BIND/CONNECT/ACCEPT/CLEANUP success in ordinary Windows PowerShell. Record this as a Codex execution-environment limitation, not an application defect or remaining TASK-001 blocker. Java target remains 17; no application/toolchain/network workaround was introduced.

Later repository-history inspection corroborated backend foundation commit `21be402`. That commit reference does not turn user-reported manual startup into agent-observed verification. The historical no-database startup preceded TASK-003 datasource configuration; current configured startup still requires its datasource settings.

### TASK-002 — Recorded frontend foundation evidence

- TASK-002 evidence (2026-09-20, agent-observed): Eleven frontend files provide the manual Next.js technical foundation, React Query provider, dependency lockfile and neutral Playwright smoke checks; .gitignore and README.md document generated artifacts and commands. Explicit E:\Node\node-v24.21.0-win-x64 tooling confirmed Node 24.21.0/npm 11.19.0. npm install succeeded (350 packages; audit reported zero vulnerabilities); npm ci succeeded; npm ls --depth=0 confirmed every approved direct version. npm run typecheck, npm run lint and npm run build passed. Production output contains only the neutral / page and built-in /_not-found. Chromium installation succeeded; npm run test:e2e passed 2/2 checks at 390x844 and 1440x900 (HTTP 200, visible neutral content, no horizontal overflow or uncaught page errors). The final run exited 0 and automatically released the production-server port.

- TASK-002 development verification: npm run dev -- --hostname 127.0.0.1 --port 3000 reached Ready and returned HTTP 200. Codex terminal Ctrl+C delivery did not stop the first run; that task-owned process was stopped explicitly. A no-file Node stdin diagnostic invoked the same Next.js dev CLI, confirmed HTTP 200/neutral content, and invoked its registered SIGINT shutdown handler; DEV_SHUTDOWN_EXIT=0 and no remaining listener on port 3000 confirmed shutdown. This verifies the shutdown handler, not physical keyboard delivery in an ordinary terminal.

- TASK-002 environment/scope notes: Approved Node/npm were selected process-locally because Codex's default PATH selected other versions. Dependency/browser downloads required elevated execution; the first sandbox Playwright run passed both checks but needed explicit server cleanup, while the elevated rerun passed with automatic cleanup. ESLint 9.39.5 reports unsupported/deprecated status; npm withheld unrs-resolver's postinstall script, but lint/build passed without enabling it. Playwright emitted only inherited NO_COLOR/FORCE_COLOR warnings. Next.js generated frontend/AGENTS.md and frontend/CLAUDE.md automatically; both are ignored generated artifacts. No additional dependency, environment file, product UI/UX, API/authentication/database integration, schema, backend, deployment or CI change was introduced. Generated artifacts and secret-file patterns are ignored; source inspection found no secrets or prohibited environment configuration. Existing-file hashes confirmed changes only to .gitignore, README.md and this tracker; Git branch remained dev, with no commit/push/branch operation. No completion commit exists for this work.

Historical annotation: the preceding no-completion-commit statement describes the original verification session. Later repository-history inspection corroborated frontend foundation commit `6bfc326`; it is not a current claim that no commit exists. Preserve the original process/shutdown and environment limitations.

### TASK-003 — Recorded connectivity foundation evidence

- TASK-003 evidence (2026-09-21, agent-observed): Added Boot-managed JPA/PostgreSQL dependencies, environment-bound datasource with ddl-auto=none and SQL init=never, placeholder backend/frontend environment examples, database-independent skeleton context test and opt-in DatabaseConnectivityTests (read-only SELECT 1). README documents process-scoped credentials and commands. Maven Wrapper confirmed Maven 3.9.16/Oracle Java 17.0.12. Elevated clean verify passed: Java release 17 compilation, 1 context test passed and 1 connectivity test skipped; executable JAR packaged. Packaged dependencies include PostgreSQL 42.7.13, HikariCP 7.0.2 and Hibernate 7.4.5.Final; no Flyway. Initial sandbox dependency resolution failed with permission denied; elevated retry succeeded.

- TASK-003 negative verification: Explicit opt-in with database variables removed failed as expected (exit 1; 1 test error, 0 skipped) during datasource configuration. Packaged startup with web application type none also exited 1 for missing configuration; a separate synthetic connection to a confirmed unused loopback port exited 1 with connection refused. No real database was contacted. Ignored logs: backend/target/connectivity-missing-config.log, startup-missing-config.log and startup-unreachable.log. The negative run replaced the normal build's Surefire report at that time; later user-run connectivity tests may replace it again. Recorded outcomes retain their original attribution.

- TASK-003 TLS decision: The official Supabase Spring Boot guide consulted for the recorded TASK-003 decision specified Session Pooler on 5432 with sslmode=require. This requires TLS without plaintext fallback, but pgJDBC does not validate certificate/hostname in this mode. The approved clarification replaces the audit's assumed custom-CA/verify-full requirement; no certificate, trust store, TLS fallback or dependency was added. User-reported Session Pooler JDBC verification with sslmode=require passed; TLSv1.3 was observed separately in psql, not measured by the Java test. Sources: https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot and https://jdbc.postgresql.org/documentation/ssl/.

- TASK-003 infrastructure evidence (user-reported): local PostgreSQL 18.6 and Supabase development PostgreSQL 17.6 connected with psql; no tables were visible to the reported checks. Session Pooler succeeded with SSL; direct IPv6 access was unreliable. These are not agent-observed JDBC results and do not prove provider-internal schemas are absent. No secret values are recorded.

- TASK-003 scope verification: Exactly three new and five modified approved files; no application Java changes, entities, repositories, schema/migration files, database mutations, Supabase services or frontend API client. .gitignore unchanged; real environment files/build output ignored and placeholder examples trackable. Existing LF/CRLF styles and final newlines preserved; git diff --check passed. No Git commit/push/merge/branch operation.

- TASK-003 final live verification (user-reported, outside Codex): The command `mvnw.cmd -DdatabaseConnectivity=true -Dtest=DatabaseConnectivityTests test` passed separately against the approved local PostgreSQL database and Supabase Session Pooler on port 5432 using sslmode=require. Each run reported 1 test, 0 failures, 0 errors, 0 skipped and BUILD SUCCESS. Passwords were supplied privately through the user's local PowerShell environment; no secret or actual Supabase endpoint is recorded here. These are accepted user-supplied results, not agent-observed database connections.

- TASK-003 live startup and no-mutation evidence (user-reported): After JDBC verification, psql connected through Session Pooler to PostgreSQL 17.6 with TLSv1.3 and reported no visible tables. Configured Spring Boot startup against Supabase initialized JPA EntityManagerFactory, started Tomcat on 8080 and reported Started EnglishLearningApplication in 4.416 seconds. Graceful shutdown closed EntityManagerFactory and HikariPool normally; Maven reported BUILD SUCCESS. No claim is made that this was the packaged-JAR command or that local Tomcat startup was separately observed. The passing local/Supabase JDBC tests, read-only SELECT 1 implementation, disabled Hibernate/SQL initialization, absence of migrations and reported table check satisfy TASK-003's connectivity/no-schema-mutation scope; provider-internal schemas are not claimed absent.

- TASK-003 final acceptance audit: Environment loading and the frontend API-origin boundary are documented; both required PostgreSQL connections passed, normal build/package and negative configuration checks remain valid, and no TASK-004 work was performed. Database-independent final review found an empty Git index, only the approved three new/five modified files, placeholder-only environment examples, no credentials or credential-bearing URLs in intended changes, and ignored local environment/build/generated artifacts. Only this tracker changed during finalization; CRLF/final newline and unrelated content were preserved. No new database connection or credential request was required.

The documented TLS decision is preserved historical evidence, not a fresh online verification. No certificate/hostname verification is claimed from sslmode=require. No new database credentials, connection or schema inspection was requested for this reconciliation.

### Read-only audit corroboration and limitations

The approved audit inspected the existing packaged artifact, bootstrap/configuration files, neutral frontend and foundation tests. Existing Surefire summaries showed one passing context test and one passing connectivity test. Inspection did not rerun them, identify both historical database targets independently or replace the user-reported live evidence. No tutoring business implementation or additional completed task was found.

### TASK-031 — Documentation reconciliation completion

- Evidence: docs/FEATURE_STATUS.md reconciles all 37 active and eight retired task dispositions; README.md now describes the English Tutoring Platform, planned versus implemented scope, current setup/datasource prerequisites and authoritative documentation links. TASK_BREAKDOWN retains scope/readiness authority; this tracker retains live-status/evidence authority; README provides onboarding.
- Acceptance review checked the saved README against TASK-031, source mappings and identifiers, documentation links, the documentation diff and preserved TASK-001/002/003 evidence and unfinished foundation components. The preceding README verification statically checked commands and links; no commands were treated as newly executed runtime evidence. Technical package/artifact/database names remain unchanged. Documentation git diff --check passed.
- Completion is documentation/operational guidance only. All business implementation and open decision gates remain outstanding under their own tasks. No build, test, database connection, implementation or deployment was performed for this completion; no completion commit is claimed. No completion evidence exists for active tasks other than TASK-001/002/003 and TASK-031.

### TASK-004 — Physical Database V1 migration and persistence integrity

- Status: DONE (2026-10-05, agent-observed). FEAT-002 is DONE for migration/persistence integrity only; FEAT-001 and its component statuses are unchanged.
- Implementation: backend/pom.xml adds spring-boot-starter-flyway 4.1.1 and Boot-managed flyway-database-postgresql 12.4.0 (flyway-core 12.4.0 transitively). application.yml reuses DATABASE_URL/DATABASE_USERNAME/DATABASE_PASSWORD, disables Flyway clean and uses public; ddl-auto=none and SQL init=never remain unchanged.
- Migration: backend/src/main/resources/db/migration/V1__initial_schema.sql matches the canonical DATABASE_DESIGN Section 17 SQL byte-for-byte: 22 application tables, 22 PKs, 28 FKs, 51 CHECKs and 23 explicitly declared indexes. Static verification checked creation order, names/references, lifecycle evidence, NaN/canonical-email checks, Session numbering and absence of retired/provider-placeholder schema; git diff --check passed.
- Verification: Maven 3.9.16 / Oracle Java 17.0.12; Java release 17. Default clean verify passed with the context test passing and two opt-in database tests skipped. Final mvnw.cmd -B -ntp -DdatabaseMigration=true -DdatabaseConnectivity=true verify passed: 3 tests, 0 failures/errors/skips; executable JAR packaged.
- Runtime: Installed PostgreSQL 18.6 binaries created an isolated temporary SCRAM-authenticated cluster on 127.0.0.1:55432, database task004_migration, with a generated ephemeral password. No shared/local development or Supabase credentials were used. DatabaseMigrationTests rejects non-loopback/non-task004_* targets and nonempty databases before starting Spring/Flyway.
- Flyway applied V1 from empty public schema, recorded version 1 successful, and a second Spring application-context startup plus repeated validate/migrate reported no migration necessary. SQL metadata independently confirmed 22 application tables excluding flyway_schema_history. Tests checked PK/FK deletion counts and representative indexes; rollback-only fixtures verified canonical email, uniqueness, VND/ranges/NaN, lifecycle evidence, RESTRICT and Session numbering. GRADED with null score remains permitted. No application/transactional business invariants are claimed implemented.
- Earlier verification exposed two test-only issues (RESTRICT SQLSTATE expectation and an explicit-ID/identity fixture collision); both were corrected without changing the migration. Final verification passed and the temporary server shut down normally. Temporary diagnostic clusters/logs remain outside the repository; initialization password files were removed.
- DatabaseConnectivityTests explicitly disables Flyway to preserve its read-only SELECT 1 contract. Repeat migration testing uses a fresh empty disposable database; never Flyway clean or a shared database. Supply credentials privately through the existing environment variables.
- Scope: No entities/repositories/services/controllers or payment/Calendar/Storage integration; no Supabase access/migration, no schema redesign, and no commit/staging. Generated build/test output is ignored. There is no remaining TASK-004 completion blocker; later business/integration gates and Supabase rollout remain separate work.

## 9. Decision-Gate Relationship

G-PHYSICAL remains RESOLVED for the approved 22-table design. Separately authorized
TASK-004 implementation and isolated verification are now complete; FEAT-002 is DONE.
This does not complete business persistence/JPA, APIs or external integrations.
Completed TASK-001/002/003 evidence remains unchanged.
Approved Calendar/Storage/payment directions are not implemented integrations.

TASK_BREAKDOWN Section 23 owns the 33 gate definitions and dependency graph. This ledger records only relevant current blockers and points to task scope; it does not maintain a competing gate catalogue.

Removing a blocker requires the appropriate approved decision and reassessment of applicable prerequisites. It never automatically establishes implementation or DONE. TODO tasks still carry the API, UI, physical-design and feature-specific gates in their task specifications. No obsolete gate identifier is presented as current authority.

## 10. Status Maintenance Rules

- Read current task scope and evidence before starting; do not reimplement DONE foundations without an approved reason.
- Keep implementation, verification and design readiness distinct. API design is not endpoint implementation; database connectivity is not schema completion.
- Set IN_PROGRESS when authorized implementation genuinely starts. Use BLOCKED when a concrete central dependency prevents meaningful work, recording completed portions and the unblock condition.
- Keep planning DEFERRED separate from live status; keep RETIRED records outside active totals.
- Mark DONE only after the current task acceptance criteria and required checks pass. Record actual paths, command/results, attribution, limitations and commit references when available; never invent evidence.
- Update affected task and compatible feature/component records together. A blocked subtask does not automatically block all independent feature work. Feature completion requires all applicable tasks/components and integration.
- Record Playwright browser evidence separately from backend unit/integration/security checks; foundation checks do not verify business authorization.
- Preserve historical evidence when later configuration or scope changes. Add dated/attributed clarifications rather than rewriting old results into stronger claims.
- Obtain approval for scope/design changes. Task scope/dependencies belong in TASK_BREAKDOWN; live status/completion evidence belongs here.
- TASK-031 completion covers documentation/operational guidance only; it does not promote any business task or change planning readiness.

## 11. README Reconciliation Impact

README.md reconciliation is complete under TASK-031. It uses the English Tutoring Platform identity, separates planned tutoring scope from completed foundations, documents current dependencies and datasource prerequisites for startup, and links authoritative scope/design/planning/status documents.

Preserve its valid boundaries: business APIs/authentication are absent and the frontend is neutral. TASK-004 now installs Flyway as migration owner; Hibernate generation and Spring SQL initialization remain disabled. README's earlier Flyway-absent wording is historical and needs a separately scoped onboarding update. Do not rename existing Java packages, Maven artifacts, directories or database names merely to match the product title.
