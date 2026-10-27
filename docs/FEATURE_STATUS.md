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

Flyway and the approved 22-table Physical V1 migration are implemented and verified on disposable local PostgreSQL under TASK-004. Supabase development V1 is deployed, independently verified read-only and accepted as the development database baseline; see Section 8, TASK-004. The current-account controller and Batch 2 login/refresh/logout/CSRF HTTP endpoints are implemented; Batch 3 Student registration/initial-email verification/resend are implemented and locally verified; other business controllers, production deployment and CI remain unimplemented; TASK-006 supplies JWT validation/default deny and a minimal read-only User mapping for current-account resolution.

| Active task status | Count |
|---|---:|
| DONE | 8 |
| TODO | 20 |
| BLOCKED | 7 |
| IN_PROGRESS | 3 |
| Total | 38 |

Planning readiness: 27 TODO, 8 DEFERRED and 3 preserved completed foundations. Eight retired task IDs are tracked separately. TASK-031 is DONE for documentation/operational guidance only. This documentation reconciliation does not establish application implementation or verification.

## 3. Active Task Ledger

Each current active task appears exactly once below. Responsibilities and readiness derive from TASK_BREAKDOWN; the evidence/blocker column does not replace its full gate and dependency list. TODO is not permission to bypass a prerequisite or open contract.

| Task | Responsibility | Implementation status | Planning readiness | Current blocker / evidence pointer |
|---|---|---|---|---|
| TASK-001 | Backend technical foundation | DONE | Preserved foundation | Preserved evidence: Section 8, TASK-001. |
| TASK-002 | Frontend technical foundation | DONE | Preserved foundation | Preserved evidence: Section 8, TASK-002. |
| TASK-003 | PostgreSQL connectivity foundation | DONE | Preserved foundation | Preserved evidence: Section 8, TASK-003. |
| TASK-004 | Physical Database V1 migration and persistence integrity | DONE | TODO | Flyway V1 and isolated PostgreSQL verification passed; see Section 8, TASK-004. |
| TASK-005 | Shared API and client boundary conventions | DONE | TODO | Shared boundary and real current-account response/error verified. See Section 8, TASK-005. |
| TASK-006 | Authentication and backend authorization foundation | IN_PROGRESS | TODO | Phases 1-3 and real current-account boundary verified; Teacher eligibility and other security gates remain outstanding. See Section 8, TASK-006. |
| TASK-007 | Student registration and shared authentication sessions | DONE | TODO | Accepted 2026-10-14: mandatory registration/verification, session and browser-security criteria passed; see final acceptance matrix in Section 8. |
| TASK-008 | Dedicated password change and recovery | DONE | TODO | Logged-in Change Password and six-digit OTP recovery passed isolated backend/PostgreSQL and live desktop/mobile acceptance on 2026-10-16; see final evidence below. |
| TASK-009 | Current account and own personal profile | IN_PROGRESS | TODO | Current-account read and bounded Student profile view/edit implemented; Teacher own-profile scope remains. See Student profile evidence below. |
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
| TASK-032 | Teacher registration and controlled onboarding | DONE | DEFERRED | Accepted 2026-10-14: mandatory registration, review, status and resubmission criteria passed; see final acceptance matrix in Section 8. |
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
| TASK-046 | Teacher certificate persistence and management | IN_PROGRESS | TODO | Documentation and V2 preparation only; deployment/application implementation not performed. See Section 8, TASK-046. |

TASK-031 is complete for downstream tracking and README operational guidance. Its planning readiness remains TODO as recorded in TASK_BREAKDOWN; completion does not count as business implementation.

## 4. Preserved Completed Foundations

TASK-001, TASK-002 and TASK-003 remain DONE. Their original evidence is retained in Section 8; no build, browser test, startup or database connection was rerun for this reconciliation.

- TASK-001 proves Java 17/Spring Boot build, context, packaging and recorded packaged-JAR startup only.
- TASK-002 proves reproducible Next.js setup and responsive neutral technical rendering only, not tutoring screens, navigation or approved product UI/UX.
- TASK-003 proves environment configuration and local/development connectivity only, not application schema, migration installation or production database readiness.

### FEAT-001 — Development/runtime foundation

- Overall status: DONE.
- Component status: Backend DONE; Frontend DONE; Persistence DONE; Tests DONE.
- Related tasks: TASK-001, TASK-002, TASK-003 and TASK-005.
- Foundation acceptance for TASK-001/002/003/005 is verified. Product UI and unrelated G-API/G-UI decisions remain open; foundation DONE does not claim product completion.
- Persistence DONE means foundation connectivity only. It does not mark the separate migration foundation or any domain persistence complete.
- Evidence: Section 8. The business-scope migration does not invalidate these completed checks.

Compatible existing feature IDs retain their current meaning below. Their scope is defined by the associated current tasks, not by historical completion definitions. No new feature-ID taxonomy is needed to represent the new task capabilities.

| Feature | Current responsibility | Overall status | Related current tasks |
|---|---|---|---|
| FEAT-002 | Physical Database V1 migration/persistence foundation | DONE | TASK-004 |
| FEAT-003 | Authentication/authorization foundation | IN_PROGRESS | TASK-006 |
| FEAT-004 | Student registration, shared sessions and password security | IN_PROGRESS | TASK-007, TASK-008 |
| FEAT-005 | Current account and own personal profile | IN_PROGRESS | TASK-009 |
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
| FEAT-023 | Teacher certificate management | IN_PROGRESS | TASK-046 |

Other unfinished feature rows have no implementation evidence; component work remains unimplemented and subject to the associated task blockers. FEAT-022 has queued meaningful verification/CI work despite its deployment task being blocked. Task completion and combined feature completion are assessed separately. New tutoring responsibilities are tracked by TASK-031 through TASK-046 and Section 6, without reusing retired feature IDs.

FEAT-023 components: Backend TODO; Frontend TODO; Persistence IN_PROGRESS;
Tests TODO. Only V2 migration preparation has started; no deployed certificate
table, runtime integration or executed migration test is claimed.

## 5. Blocked / Deferred-Readiness Work

The following core implementation cannot proceed until the named decisions are approved. Each task is BLOCKED in the live ledger and DEFERRED in TASK_BREAKDOWN planning. Preparing independent prerequisites does not resolve its central gate.

| Task | Current blocker / required decision |
|---|---|
| TASK-018 | G-PROGRESS/G-RESULT: derived representation is settled; formulas, result availability and applicable API contracts remain open. |
| TASK-022 | G-PAYMENT/G-CONFIRMATION/G-OUTCOME/G-PAYEFFECT: VietQR, trustworthy bank evidence and full refunds are settled directions; concrete provider/authenticity, retry/late and refund contracts remain open. |
| TASK-029 | G-DEPLOY: approve production hosting, secrets and operational/release procedures. |
| TASK-037 | G-TIME/G-RECURRENCE/G-SESSION: ISO weekly generation, IANA timezone, fixed unique numbering and no-replacement cancellation settled; DST/cutoff and operation exceptions remain open. |
| TASK-041 | G-RECEIVING/G-PAYMENT: historical bank accounts and one ACTIVE settled; bank checks and verified adapter integration contracts remain open; conceptual provider fields are omitted. |
| TASK-043 | G-CALENDAR: organizational synchronization settled; single configured Calendar/event-ID scope settled; execution/retry contracts remain open. |
| TASK-044 | G-FEEDBACK/G-API: one completed-Enrollment rating 1..5 settled; exact acceptance/visibility/edit/moderation and executable contracts remain open. |

These entries record missing decisions, not failures of the completed infrastructure. Do not invent schema, integration or policy to remove a blocker. Other tasks remain TODO where meaningful safe prerequisite work can later be authorized.

## 6. Current Capability Summary

The table distinguishes implemented responsibilities from remaining gated work. Unapproved operation contracts remain gated as described upstream.

| Capability | Current implementation/evidence boundary | Task ownership |
|---|---|---|
| Authentication/account security | Student and bounded Teacher registration plus shared email verification/resend are implemented; logged-in password change and bounded OTP forgot/reset passed isolated live acceptance. TASK-007 supplies verified login/refresh/logout HTTP endpoints, CSRF bootstrap and configured cookie/CORS controls over its atomic issuance/refresh core. TASK-006 validates bearer JWTs, resolves current User role/locked state and preserves default deny; Database-backed Teacher eligibility and bounded Admin onboarding review are implemented; business-route ownership integration remains with each feature. Bounded current-account viewing and verified/unlocked Teacher own-onboarding status are implemented; broader onboarding remains gated. | TASK-006, TASK-007, TASK-008, TASK-032 |
| User/Teacher profiles | Minimal current-account identity viewing and atomic TeacherProfile creation on Admin approval are implemented; bounded Student own-profile editing is implemented; Teacher own-profile editing and public Teacher projection remain gated. | TASK-009, TASK-033 |
| Public discovery | Teacher/Course search/filter and public Course detail are not implemented; protected participation data must remain separate. | TASK-011 |
| Teacher certificates | V2 prepared only; own management, Admin review and VERIFIED-only public projection are unimplemented. | TASK-046 |
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

- Supabase development baseline (2026-10-05, agent-observed deployment/read-only verification; user-accepted): V1 is deployed through the Session Pooler. Independent catalog queries in BEGIN READ ONLY confirmed exactly the expected 22 application tables, no missing/unexpected tables, and exactly one flyway_schema_history entry: version 1 / V1__initial_schema.sql / success=true.
- Supabase catalog verification: 22 PKs, 28 FKs, 14 UNIQUE constraints, 51 CHECKs and 59 valid/ready indexes (23 explicit plus 36 PK/UNIQUE backing indexes). Required Session, Submission, Feedback, Calendar, canonical-email, payment relationships and partial-unique constraints matched V1; no replacement-session column or Session self-FK, and no catalog mismatch. This completes the separate development database rollout noted above, not production deployment or business implementation. Existing task/feature statuses remain unchanged.

### TASK-005 — Shared API/client boundary slice

- Status: DONE (2026-10-11), assessed after real current-account integration. Scoped G-API conventions are approved in API_DESIGN Section 19; FEAT-001 foundation and its components are DONE; this does not complete product UI. TASK-001 through TASK-004 evidence/statuses are preserved.
- Implementation: common/api ApiError, ApiExceptionHandler, ApiJsonConfiguration, ApiId and ApiMoney; frontend/src/lib/api.ts reuses NEXT_PUBLIC_API_ORIGIN/native fetch. Added Boot-managed spring-boot-starter-validation for DTO Bean Validation. frontend/tsconfig.json permits .ts imports for dependency-free native Node tests; no frontend dependency was added.
- Verification (agent-observed): Oracle Java 17.0.12; focused ApiBoundaryTests passed, and mvnw.cmd -f backend/pom.xml -B -ntp verify reported BUILD SUCCESS: 20 tests, 0 failures/errors, 2 opt-in database tests skipped (18 passed), executable JAR packaged. MockMvc test-only fixtures cover lossless Long/money round trips, ISO timestamps, rejection of numeric/offsetless timestamps, malformed input, scalar coercion, unknown/protected fields and safe structured errors. DATABASE_URL/USERNAME/PASSWORD were removed from the test process; datasource/Flyway are excluded from the boundary tests. No database or Supabase access occurred.
- Frontend verification: Node 24.21.0; node --test frontend/tests/api.spec.ts passed 6/6 with stubbed fetch (no server/network). npm run typecheck, npm run lint and npm run build passed; output remains the neutral / and built-in /_not-found. Native Node reported module-type reparsing; no package-wide module change was made merely to suppress the warning.
- Completion assessment: GET /api/v1/users/me supplies the first approved real request/response and 400/401/503/500 error cases, using explicit DTOs and shared conventions. Exact large IDs are verified; existing money/input/client evidence remains valid. Maven verify passed 61, failed 0, errors 0, skipped 3 opt-in database tests (64 total), including 37 security/API checks and 6 password checks. Frontend unchanged; prior frontend verification remains applicable. G-UI, pagination and unrelated operation contracts remain open outside this bounded foundation. No Supabase/schema/migration changes or commit/push.

### TASK-006 — JWT boundary and current-account resolution

- Status: IN_PROGRESS (2026-10-11); FEAT-003 IN_PROGRESS. Phases 1/2/3 bounded slices only: no registration/login/refresh/logout, token issuer or Teacher onboarding approval; bounded current-account viewing is added under TASK-009.
- Implementation: features/auth/security/SecurityConfiguration, AccessTokenValidator and ApiSecurityErrors; Boot-managed resource-server JWT starter and test-scoped spring-security-test.
- Contract: header Bearer only; RS256; required sub/iss/aud/iat/exp; positive decimal Long subject without numeric coercion; issuer english-tutoring-platform and audience english-tutoring-api defaults. app.security.jwt.clock-tolerance defaults to 60s; issuer/audience are configurable. Required app.security.jwt.public-key resolves an external X.509 PEM RSA public key (minimum 2048 bits); no fallback key. Missing/invalid configuration prevents servlet startup.
- HTTP boundary: stateless/default deny; safe ApiError 401/403; no Basic/form login/logout, redirects or request cache. CSRF is disabled for this bearer-header-only phase; cookie authentication and browser CORS remain unimplemented. Token roles/scopes grant no production authority. Non-web database verification contexts do not load servlet security.
- Verification: Oracle Java 17.0.12; mvnw.cmd -f backend/pom.xml -B -ntp verify passed: 44 tests, 0 failures/errors, 2 opt-in database tests skipped (42 passed; 24 security checks). Signature/claims/timestamps, malformed and unsigned tokens, subject coercion/overflow, safe errors, default deny, unsupported transports and test-only role boundaries were exercised. Runtime-generated keys and fixtures are test-only; no fixture classes packaged in the executable JAR. Database/Spring/PG environment overrides removed from the test process; no Supabase/database access. git diff --check passed.
- Phase 2 implementation: features/user/entity/User maps only id/role/locked/email_verified read-only; UserRepository exposes lookup only; CurrentAccountService returns immutable AccountSnapshot in a read-only transaction. features/auth/security/CurrentAccountAuthenticationConverter resolves the validated subject on every authenticated request; AccountPrincipal contains no entity or credentials. No cross-request cache or new dependencies.
- Phase 2 behavior: missing/locked User returns safe 401; authorities derive exclusively from current database role, never JWT role/scope claims. Database resource/transaction availability and timeout failures return 503 AUTHENTICATION_UNAVAILABLE; unexpected lookup failures return generic 500 INTERNAL_ERROR without SQL/private values. Production routes remain default deny except the approved current-account GET; ROLE_TEACHER alone establishes neither approved onboarding nor business permission.
- Phase 2 verification: focused security/context tests passed 31/31 (30 security); full Java 17 Maven verify passed 48, failed 0, errors 0, skipped 3 opt-in database tests (51 total), with executable JAR packaging. The opt-in CurrentAccountPersistenceTests separately passed 1/1 on a fresh isolated PostgreSQL 18.6 cluster at 127.0.0.1:55432/task006_accounts using dedicated process-scoped credentials. Unchanged V1/V2 applied only there; Hibernate validated the mapping, precise BIGINT IDs/all roles/current flags/immutable snapshots and absence of lookup writes were checked. Cluster stopped afterward. Previous disposable cluster could not start (missing pg_notify); it was not repaired or reused. Initial fixture compilation/context failures were corrected before the passing runs.
- Phase 2 safety: no Supabase access, frontend changes or new migrations; V1/V2 unchanged. Test fixtures excluded from the packaged JAR; git diff --check passed. Existing Phase 1 and TASK-046 evidence preserved.
- Phase 3 implementation: PasswordConfiguration uses Spring DelegatingPasswordEncoder with BCrypt cost 12 and {bcrypt} storage identifiers; a small adapter validates inputs and rejects malformed/unsupported hashes without plaintext fallback. PasswordInput requires 8-64 Unicode code points and at most 72 UTF-8 bytes, preserving whitespace/case/Unicode form exactly; malformed UTF-16 is rejected rather than replaced during encoding. No dependency changes, credential lookup/writes or workflows.
- Phase 3 verification: Java 17 Maven verify BUILD SUCCESS; 54 passed, 0 failures/errors, 3 opt-in database tests skipped (57 total), including 6 password tests and all 30 security regression checks. Tests cover independent salts, cost/storage format, matching, Unicode/code-point/UTF-8 boundaries, whitespace/case/normalization preservation and safe malformed/unsupported-hash rejection. Initial final-method compilation incompatibility was corrected using composition before the passing run. No database/Supabase access. BCrypt cost must be benchmarked on deployment hardware before production; unit-test timing is not production capacity evidence.
- Remaining: Teacher eligibility integration, real feature permissions, production BCrypt benchmarking, browser transport/CORS/CSRF, key operations and TASK-007 lifecycle contracts. Current-account viewing now supplies real-boundary evidence; wider permissions remain incomplete. TASK-005 is DONE and TASK-046 remains IN_PROGRESS; no migrations/frontend changes or commit/push.

### TASK-007 — Phase 1 internal refresh-session lifecycle

- Status: IN_PROGRESS; FEAT-004 IN_PROGRESS. Explicitly approved bounded sequencing
  exception permits this foundation while TASK-006 remains IN_PROGRESS.
- Implementation: auth/entity/RefreshSession, repository/RefreshSessionRepository,
  security/RefreshTokens and RefreshSessionConfiguration, service/RefreshCredential,
  RefreshFailure and RefreshSessionService. No new dependency or endpoint.
- Credentials: 32 SecureRandom bytes, canonical unpadded Base64URL (43 characters);
  SHA-256 of the canonical ASCII token stored as lowercase hex only. Incoming tokens
  must decode to exactly 32 bytes and re-encode identically; no trim/normalization.
  Immutable internal result redacts its toString; exceptions retain no raw causes.
- app.security.refresh.lifetime defaults to 7d; positive PostgreSQL-microsecond
  representable Duration required. Injected UTC Clock; each rotation gets a fresh
  lifetime. created_at/revoked_at use backend time at database timestamp precision.
- READ COMMITTED row locking precedes expiry/revocation checks; now >= expires_at
  rejects. Old row is revoked and replacement inserted atomically, retaining history.
  A top-level TransactionTemplate commits before returning any replacement credential.
  Ambient transactions are rejected to avoid returning before an outer commit.
- Missing/locked Users and unknown/expired/revoked/replayed credentials fail closed.
  These primitives do not approve login or decide unverified/pending-Teacher eligibility.
  Targeted revocation affects only the identified usable credential. No session limit,
  family tracking, unrelated-session revocation or automatic retries.
- Typed failures: INVALID_CREDENTIAL, UNAVAILABLE, INTERNAL_FAILURE, COMMIT_UNCERTAIN.
  Work/flush failures roll back; failures after successful flush during transaction
  completion are conservatively uncertain. No successful credential is returned or
  rollback claimed in that case. Lost responses cannot recover replacements via old tokens.
- Verification: focused unit run initially passed 10/10; final Maven verify passed
  73, failed 0, errors 0, skipped 12 opt-in database tests (85 total). This includes
  12 refresh unit tests plus existing password/security/API regressions.
- Separate PostgreSQL 18.6 verification passed 9/9, no failures/errors/skips, on a fresh
  protected disposable cluster at 127.0.0.1:55432/task007_refresh with dedicated
  process-scoped credentials. Unchanged V1/V2 applied only there; Hibernate validated
  mappings. Tests proved one concurrent winner, duplicate-hash/NOT NULL insertion
  rollback with the old credential still usable, historical retention, fresh expiry,
  locked/missing User rejection, independent sessions, targeted revocation, committed
  visibility and expiry checked after a verified lock wait. Deliberate constraint
  failures produced expected PostgreSQL warnings. Cluster stopped after testing.
- No database access during general verify; integration tests require explicit
  TASK007_TEST_* settings and an empty allowlisted local database. No Supabase access.
  JAR excludes test fixtures; V1/V2 unchanged; git diff --check/CRLF checks pass.
- Remaining: private-key JWT issuance, full login/refresh eligibility, registration,
  email verification, logout workflow, HTTP contracts and browser transport/CORS/CSRF.
  No authentication grant is inferred from a successful lifecycle primitive.
  TASK-006 stays IN_PROGRESS; TASK-007 is not DONE. No commit/push.

### TASK-007 — Phase 2 internal access-token issuer (verified internal slice)

- Status: TASK-006, TASK-007 and FEAT-004 remain IN_PROGRESS.
- Prepared AccessTokenIssuer/AccessToken/AccessTokenConfiguration using existing
  NimbusJwtEncoder, explicit RS256/JWT header, external file-URI PKCS#8 private key
  and X.509 public key, minimum 2048 bits, modulus and sign/verify startup checks.
  Required app.security.jwt.private-key has no fallback. Shared jwtClock drives
  issuance and both validator timestamp checks; existing 60s tolerance is unchanged.
- app.security.jwt.access-token-lifetime defaults to 15m; positive whole-second
  Duration required. issue(Long) rejects null/non-positive IDs, uses exact decimal
  subject and only sub/iss/aud/iat/exp. Result toString and signing failures are
  redacted. No issuer calls from account resolution or refresh rotation.
- Approved audience contract: exactly one configured audience; NimbusJwtEncoder
  serializes aud as a JSON string. Decoded audience must equal the singleton
  configured value; missing or multiple audiences are not issued. No array-only
  requirement, custom serialization, signer or validator change.
- Verification 2026-10-12: focused AccessTokenIssuerTests passed 8, failed 0,
  errors 0, skipped 0. Final Maven verify BUILD SUCCESS: passed 81, failed 0,
  errors 0, skipped 12 (93 total). The 12 opt-in database tests were skipped;
  isolated database configuration is unchanged and no database was contacted.
  Tests cover actual production-validator compatibility, exact five-claim payload,
  RS256/JWT header, singleton audience, key startup failures, clock boundaries,
  redaction and same-modulus/wrong-public-exponent challenge rejection.
- Only runtime-generated temporary test keys used; no production key generated,
  database/Supabase access, migration changes, endpoints or commit/push.
  Key replacement/restart invalidation was tested; no multi-key/JWKS support.
- Remaining: separately approve full eligibility and refresh-commit/signing failure
  ordering before authentication workflow integration.
  No authentication workflow integration is authorized by these primitives.

### TASK-007 — Authentication workflow Batch 1 (internal only)

- TASK-006, TASK-007 and FEAT-004 remain IN_PROGRESS; TASK-032 remains BLOCKED.
- UserCredentialRepository/UserCredentialService provide User-owned private scalar
  credential lookup with lowercase(trim(email)) canonicalization. Existing read-only
  User mapping and current-account behavior are unchanged; hashes never enter public DTOs.
- AuthenticationWorkflowService verifies the existing BCrypt password contract and
  performs dummy BCrypt comparison for an unknown account. Missing/locked/unverified
  accounts fail safely. Teacher onboarding is not account authentication eligibility;
  Teacher business authority still requires separate checks. Registration remains
  unimplemented and must create unverified accounts without token issuance.
- RefreshSessionService adds two bounded internal workflow operations. Login locks and
  rereads the User, rejecting changed email/password or ineligible state; refresh locks
  the credential then the User and checks expiry after lock acquisition. Future password
  reset/change workflows must coordinate this lock order. No generic callback API added.
- JWT signing follows eligibility and precedes refresh persistence/commit. Only committed
  immutable AuthenticationResult pairs leave the service. Signing/insert failures roll
  back; uncertain commits return typed COMMIT_UNCERTAIN without credentials or retry.
  INVALID_CREDENTIAL, UNAVAILABLE and INTERNAL_FAILURE are also safe typed outcomes;
  results/credential snapshots redact toString. No session-family or unrelated revocation.
- Verification 2026-10-12: focused unit/issuer regression tests passed 32, failed 0,
  errors 0, skipped 0. Full Maven verify BUILD SUCCESS: passed 96, failed 0, errors 0,
  skipped 20 (116 total). Opt-in persistence tests remain disabled in general verify.
- Separate PostgreSQL 18.6 run passed 17, failed 0, errors 0, skipped 0: 9 existing
  lifecycle regressions plus 8 workflow tests. Fresh isolated cluster bound only to
  127.0.0.1:55432/task007_refresh; dedicated test-only credentials, unchanged V1/V2,
  23 application tables and two successful Flyway entries. No development/remote database.
  Evidence covers canonical login/all roles, pending/rejected Teachers, malformed hashes,
  password whitespace, eligibility, one-winner refresh, signing/unique-collision rollback,
  preserved history/independent sessions and committed account changes after lock waits.
- Initial fixture issues (Mockito restubbing, database-free repository mocks and JUnit
  temporary-key initialization) were corrected before final passing runs. The first
  migrated disposable database was preserved; rerun used a fresh cluster, not a reset.
  Both test clusters were stopped; encrypted test credentials remain outside Git.
- No endpoint, cookie/CSRF/CORS, frontend, registration/email, onboarding, new migration,
  Supabase access, dependency, commit or push. Remaining: approved HTTP/browser transport,
  registration validation/privacy, verification/resend/delivery and logout contracts.
  Internal failure reasons do not independently finalize HTTP status/error contracts.

### TASK-007 — Batch 2 authentication HTTP and browser security (verified backend slice)

- TASK-006, TASK-007 and FEAT-004 remain IN_PROGRESS; TASK-032 remains unchanged.
- Implemented POST /api/v1/auth/login, POST /api/v1/auth/refresh,
  POST /api/v1/auth/logout and GET /api/v1/auth/csrf. API_DESIGN Section 3.2
  and ARCHITECTURE record approved contracts before implementation. No Batch 3.
- Explicit login/access/CSRF DTOs reject invalid/protected input and redact diagnostics.
  Login returns 403 EMAIL_VERIFICATION_REQUIRED only after correct password and unlocked
  account checks; missing/locked/incorrect credentials and ineligible refresh remain 401.
  Safe 400/401/403/503/500 mappings include uncertain outcomes; failure never sets or clears
  refresh credentials. The existing RS256/BCrypt/current-account boundaries remain intact.
- Refresh credentials are host-only HttpOnly SameSite=Lax cookies, Path=/api/v1/auth,
  Secure by default, Max-Age bounded by remaining lifetime, never JSON. Credential/bootstrap
  responses are no-store. Explicit local-http mode permits only loopback HTTP requests and
  HTTP loopback origin configuration; production origins must be exact HTTPS origins.
- Separate auth security chain uses Spring CookieCsrfTokenRepository and BREACH-masked
  bootstrap JSON. X-XSRF-TOKEN plus matching cookie is required before unsafe auth requests;
  query/form fallback is rejected. Exact-origin credentialed CORS runs before security;
  all unapproved routes remain denied. Business routes remain stateless header-Bearer only.
  No concrete local frontend API origin is currently configured; tested local pair is
  http://localhost:3000 -> http://localhost:8080, with explicit origin allowlisting.
  Tested HTTPS pair is https://web.example.test -> https://api.example.test.
- Logout revokes only the credential's row without an account-eligibility lookup, including
  locked accounts. Missing/malformed/unknown/expired/revoked input is idempotent; database or
  commit failures are not success. An old rotated credential never revokes its replacement.
  Logout does not invalidate signed access JWTs; current-account restrictions still apply.
- Verification 2026-10-12: focused tests passed 87, failed 0, errors 0, skipped 0:
  30 HTTP/HTTPS cookie-CSRF-CORS checks, 2 configuration checks, 18 workflow unit checks,
  and 37 existing security/current-account regressions. Tests use real Spring filters and
  real cookie/bootstrap exchanges in MockMvc; internal workflow dependencies are mocked.
- Full Maven verify BUILD SUCCESS: passed 132, failed 0, errors 0, skipped 22 (154 total).
  Separately, isolated PostgreSQL 18.6 passed 19, failed 0, errors 0, skipped 0, including
  existing concurrency/rollback regressions and new targeted logout checks. Remaining three
  opt-in database suites were not rerun. The fresh protected localhost-only cluster at
  127.0.0.1:55432/task007_refresh used test credentials, unchanged V1/V2 and 23 application
  tables, then was stopped and preserved. No development/remote database was contacted.
- Initial compile/test-fixture issues were corrected before final passing verification.
  JAR fixture/key exclusion, changed-file scope, CRLF and git diff --check were checked.
  No dependencies, frontend, migration source, Supabase, staging, commit or push changes.
- Remaining: Batch 3 registration validation/privacy, email provider/delivery, verification
  lifetime/resend limits and approved UI work. Actual deployment origins and trusted proxy
  HTTPS metadata still need operational configuration; no production topology/browser
  deployment was tested and no frontend token storage/refresh logic was implemented.

### TASK-007 — Batch 3 Student registration and initial verification

- TASK-007, FEAT-004 and TASK-006 remain IN_PROGRESS. TASK-032 is unchanged.
- Implemented POST /api/v1/auth/register/student, POST /api/v1/auth/email-verification
  and POST /api/v1/auth/email-verification/resend with explicit DTOs, protected-field
  rejection, CSRF/CORS and no-store. Registration returns 201 verificationRequired=true
  without authentication tokens; duplicate canonical email returns 409 without overwrite.
- StudentRegistrationService owns transaction boundaries; UserCredentialService/
  Repository own User writes. EmailVerificationCredential/Repository reuse V1;
  VerificationTokens hashes purpose-bound credentials. Verification and resend share
  User-row locking. Registration and credential creation/consumption are atomic.
- Approved resend clarification: five accepted resends per rolling 24 hours, including
  SMTP failures, excluding initial registration/rejected requests; 60-second cooldown.
  Earlier unexpired credentials remain usable until verification. Preserve credential
  history for rate accounting. No per-IP limiter is claimed.
- SmtpVerificationMail uses Boot-managed spring-boot-starter-mail and environment-only
  SMTP configuration, required STARTTLS and bounded timeouts. Mail sends after commit;
  failure preserves account state for resend. Tests use a mail double, not real SMTP.
- Agent-observed verification (2026-10-12): focused Batch 3 plus Batch 2 HTTP checks:
  57 passed, 0 failed/errors, 0 skipped. Full Maven verify BUILD SUCCESS: 192 total,
  159 passed, 0 failed/errors, 33 skipped (30 opt-in workflow PostgreSQL checks plus
  three other opt-in database checks).
- Separate isolated PostgreSQL run: 30 passed, 0 failed/errors/skipped; all 19 earlier
  checks preserved plus 11 Batch 3 tests. Covers canonical duplicates, hash-only storage,
  unverified login rejection then verified login success, expiry/replay/locked/purpose
  denial, delivery failure recovery, rollback, cooldown/rolling limit and concurrent
  verification/resend. Unit tests prove commit-before-mail, uncertain-commit suppression
  and safe failure mapping. Required negative-test constraint errors were expected.
- Fresh disposable PostgreSQL 18.6 at 127.0.0.1:55432/task007_refresh used protected
  test-only credentials and the empty-database guard. Unchanged V1/V2 applied locally;
  23 application tables and two successful history entries confirmed. Cluster stopped
  and preserved. No development/remote database or Supabase was contacted.
- Remaining: frontend/authentication UX/browser integration, real SMTP operational
  configuration/delivery verification, broader TASK-006 acceptance and separately
  scoped Teacher registration. No schema/source migration, frontend, staging, commit
  or push changes. Executable JAR excludes test fixtures and key files.

### TASK-009 — Bounded current-account read

- Status: IN_PROGRESS; FEAT-005 IN_PROGRESS. Approved sequencing exception permits
  this read-only slice before TASK-007; other prerequisites remain binding.
- Implementation: features/user/controller/CurrentAccountController and
  dto/CurrentAccountResponse reuse current database-backed AccountPrincipal and
  ApiId, without a duplicate persistence query or entity response.
- GET /api/v1/users/me returns exactly id/role/emailVerified for unlocked
  STUDENT/TEACHER/ADMIN, including unverified Users and pending Teachers.
  Authentication/account rejection precedes forbidden query/body validation.
  Only this exact GET route is permitted; other methods/routes remain denied.
- Verification: Maven verify passed 61, failed 0, errors 0, skipped 3 (64 total).
  Seven added production-route integration tests cover all roles, no onboarding
  prerequisite, exact Long.MAX_VALUE, fresh role/verification state, forbidden input,
  missing/locked accounts, invalid/expired JWTs, authentication-first precedence,
  safe 503/500 and route isolation. Strict JSON comparisons verify the three-field
  projection. Phase 1-3 regressions pass; packaged JAR excludes test fixtures.
  git diff --check and CRLF checks pass. No database/Supabase access.
- Remaining: broader profile viewing/editing, approved field contracts, frontend and
  other TASK-007 dependencies. TASK-009 and TASK-006 remain IN_PROGRESS.

### TASK-007 — Bounded frontend authentication prerequisite (2026-10-13)

- /account provides shared login, explicit refresh-based recovery, logout and initial email
  verification/resend using the existing native-fetch API client and React Query provider.
  Access JWT/current-account state is memory-only; no browser-storage or credential URL use.
  Auth requests bootstrap CSRF, include cookies and send X-XSRF-TOKEN. Bearer APIs omit cookies.
  Mutations are not automatically retried. Logout clears local state/cache immediately and
  reports server success only after confirmed response; explicit server logout remains available.
- Implementation: frontend/src/features/auth/browser-api.ts, account-workspace.tsx,
  src/app/account/page.tsx, root link/styles and shared empty-202 resend handling in src/lib/api.ts.
- Verification: npm run lint, npm run typecheck and npm run build passed (Node 24.21.0,
  npm 11.19.0). node --test tests/api.spec.ts tests/browser-api.spec.ts: 11 passed,
  0 failed, 0 skipped. Chromium npm run test:e2e -- --workers=1: 44 passed, 0 failed,
  0 skipped across narrow/wide viewports, including existing foundation checks.
- Tests verify CSRF request construction/denial, explicit recovery, memory-only session state,
  safe logout success/failure, Unicode password limits, verification expiry/resend/delivery errors,
  no retries and safe errors. API responses are intercepted test fixtures; real backend CORS,
  cookie delivery, SMTP and browser-to-database integration were not exercised.
- Initial browser run: 36 passed and 2 logout-test timing failures; assertions were corrected
  to wait for server confirmation after immediate local cleanup. A subsequent parallel run
  exhausted local memory (2 passed, 42 failed); the full one-worker rerun passed without
  application or machine-setting changes. Node module-type and terminal-color warnings remain.
- TASK-007 remains IN_PROGRESS. Student registration frontend and complete live authentication
  acceptance remain outstanding; this bounded prerequisite does not claim overall completion.
### TASK-007 — Student registration frontend completion (2026-10-13)

- Extended the existing /account registration form to Student registration with exactly
  email/password/fullName and /auth/register/student. Reuses validation, CSRF, cookie transport,
  verification/resend and login; no tokens during registration and no client-selected role.
- Files: frontend/src/features/auth/account-workspace.tsx; tests/e2e/student-admin.spec.ts.
  Browser tests cover registration/verification/login, duplicate-click protection, invalid
  password/blank name and safe 400/409/500/503 without automatic retries.
- Final combined frontend checks: npm run lint, npm run typecheck, npm run build passed;
  node --test tests/api.spec.ts tests/browser-api.spec.ts: 12 passed, 0 failed/skipped;
  npm run test:e2e -- --workers=1: 82 Chromium checks passed, 0 failed/skipped, across
  390x844 and 1440x900. Includes all existing authentication and Teacher regressions.
- TASK-007 remains IN_PROGRESS. Live authentication acceptance was not executed: no backend
  listener at 8080, no approved isolated PostgreSQL listener at 55432, no local SMTP listener
  at 1025/587, and JWT key/browser-origin/SMTP environment configuration was absent.
  No database or Supabase connection was attempted; no backend was started against inherited
  DATABASE_* settings. Live CORS, CSRF-cookie delivery, refresh-cookie/session rotation,
  PostgreSQL persistence and SMTP delivery remain unverified for this browser slice.
### TASK-007 — Isolated live integration verification (2026-10-14)

- TASK-007 remains IN_PROGRESS. This evidence supersedes the earlier local-service
  availability blocker; it does not mark overall security/operational acceptance complete.
- Used only 127.0.0.1:55432/task007_live, backend :8080, frontend :3000 and an authenticated
  STARTTLS SMTP capture on :1025. External RSA keys/test secrets remain outside Git.
  All recipients used example.invalid; the sink rejects external recipients and never relays.
- Combined live HTTP scripts: 93 passing assertions (67 core, 9 onboarding concurrency,
  5 resend, 12 ownership/error checks), zero final assertion failures. Counts include
  setup and safe-error assertions; they are not 93 distinct product workflows.
- Chromium used the real frontend/backend without API interception: 18 scenario groups
  passed across 390x844 and 1440x900. Both roles registered, verified captured codes and
  logged in. Explicit recovery after reload, empty local/session storage, HttpOnly/Lax
  refresh-cookie path, logout and denied post-logout recovery passed. Backend checks
  additionally covered exact-origin CORS, denied origin/missing CSRF, refresh rotation,
  replay rejection, no credential response at registration and no-store token responses.
- Resend waited for the real cooldown: concurrent requests returned 202/429, a new email
  was captured, and the older unexpired credential still verified successfully. Replay
  and unverified-login denial passed; live expiry/rolling-24-hour exhaustion were not induced.
- Regression evidence: frontend build/lint/typecheck passed; Node unit tests 12 passed,
  zero failed/skipped; mocked Chromium regression suite 82 passed, zero failed/skipped.
  The existing dev server initially served an inactive UI; restarting the freshly built
  loopback production frontend resolved this. The final 82-test run reused that server
  through external test-only configuration; repository configuration was unchanged.
- Backend Maven verify ran 288 tests: 236 passed, zero failures/errors, 52 opt-in tests
  skipped. Repackaging then failed because the external backend held the output JAR.
  That command had replaced the running artifact; subsequent mail requests returned safe
  503 DELIVERY errors. User stopped the backend; mvnw.cmd -DskipTests verify restored
  executable packaging with BUILD SUCCESS, 88 nested libraries and no test fixtures.
  User restarted externally before the passing live browser/concurrency/resend checks.
  This is separate test and packaging evidence, not a fresh all-tests-executed verify.
- Persistence was checked read-only: successful V1/V2 history, retained review snapshots,
  matching approved TeacherProfile, hashed refresh/verification credentials and consumed/
  revoked history. No migration source changes, Supabase access, staging, commit or push.
- Remaining: final task/prerequisite acceptance review. The 52 skipped opt-in tests were
  not rerun against task007_live because their fixtures require separate empty databases.
  Production TLS/email delivery and fault-injected database outages were not exercised.

### TASK-032 — Bounded Teacher registration and Admin review

**Registration checkpoint (ed86820):**

- IN_PROGRESS (2026-10-13). POST /api/v1/auth/register/teacher implements the approved
  six-field request and 201 {"verificationRequired":true}. Account validation is reused;
  application strings are nonblank/NUL-free, preserved without trimming or fixed limits.
- StudentRegistrationService is renamed RegistrationService for shared Student/Teacher
  creation, verification, resend, commit handling and SMTP delivery. Existing callers and
  database-free test fixtures were updated; Student wire contracts are unchanged.
- UserCredentialRepository/Service supply backend-only TEACHER creation. TeacherApplication
  and its repository persist the PENDING snapshot. User, hashed credential and application
  commit atomically; mail follows commit. No TeacherProfile or authentication tokens are
  created. Duplicate canonical email cannot overwrite/promote existing users.
- Only the approved registration route was added to the CSRF/CORS auth allowlist.
  Protected/unknown fields are rejected. Verification and account-only login do not approve
  onboarding; all unimplemented Teacher business routes remain default-deny. No review,
  approval/rejection, resubmission, status lookup or Course management implementation.
- Focused verification: 108 passed, 0 failed/errors, 0 skipped, including 15 Teacher HTTP
  tests, three added service tests and preserved Student/authentication/security checks.
- Separate isolated PostgreSQL verification: 35 passed, 0 failed/errors, 0 skipped.
  Five new tests cover exact snapshot preservation, shared verification/login, rollback
  on credential/application failure, SMTP persistence, concurrent Teacher duplicates and
  Student-vs-Teacher registration races; all 30 earlier checks remain passing.
- Fresh disposable PostgreSQL 18 cluster used 127.0.0.1:55432/task007_refresh, with
  protected test-only credentials and the existing empty-database guard. Unchanged V1/V2
  applied locally only. The cluster was stopped and preserved; no Supabase access.
- Full clean Maven verify: BUILD SUCCESS, 215 total, 177 passed, 0 failed/errors,
  38 skipped. Of those skips, 35 were separately executed above; three other opt-in
  database checks were not rerun. Initial clean failed only because its redirected log
  was inside target; moving the log outside target resolved the Windows file lock.
- Remaining at the registration checkpoint: review-operation contracts, resubmission/status/profile work and
  frontend onboarding. TASK-032 is not DONE. TASK-006/007 statuses and all pre-existing
  TASK-046 deployment evidence are preserved. No dependencies or migrations changed.

**Admin review slice (2026-10-13):**

- Implemented GET /api/v1/admin/teacher-applications/{applicationId} and POST suffixes
  /approve and /reject. Bearer authentication resolves the current account; only verified,
  unlocked ADMIN reviewers are allowed. Explicit safe detail DTO uses decimal-string IDs.
- TeacherApplication maps existing review fields; current-account locks are acquired in
  ID order before the application lock. READ COMMITTED state rechecks enforce PENDING-only
  transitions, current TEACHER applicant role and one-winner review. Applicant lock/email
  state does not prevent approval and is not changed by approval.
- Approval inserts TeacherProfile from the snapshot with null avatar_path, then saves the
  review in one transaction. Existing profile returns TEACHER_PROFILE_CONFLICT without
  overwrite; repeated review returns APPLICATION_ALREADY_REVIEWED. Reviewer/time are
  backend-assigned and rejection reason is validated/preserved. No automatic retries.
- TeacherBusinessEligibility checks current TEACHER role, verification, lock and persisted
  APPROVED application. It is not a blanket route grant: no Teacher business endpoints were
  added, existing unapproved routes remain denied and future approved operations must combine
  this policy with their own ownership checks. Account-only Teacher authentication is unchanged.
- Focused HTTP/security/registration run: 77 passed, 0 failed/errors, 0 skipped, including
  16 review HTTP tests. Three additional eligibility unit tests passed in full verification.
- Isolated PostgreSQL suite: 41 passed, 0 failed/errors, 0 skipped (35 prior regressions plus
  six review tests). Verified opposing concurrent reviews, preserved metadata/snapshots,
  profile conflict, rollback after profile insertion, current eligibility and reviewer checks.
  Fresh protected disposable PostgreSQL 18 cluster on 127.0.0.1:55432/task007_refresh only;
  existing empty-database guard preserved, server stopped afterward. No Supabase access.
- Full clean Maven verify: BUILD SUCCESS; 240 total, 196 passed, 0 failed/errors, 44 skipped.
  The 41 persistence skips were separately executed above; the other three opt-in database
  tests were not rerun. Initial focused compile exposed a Spring Security generic-signature
  mismatch; corrected before successful focused/full runs.
- TASK-032 remains IN_PROGRESS: pending queue, Teacher status lookup, resubmission and
  frontend onboarding are excluded/unimplemented. No V1/V2, dependency, application.yml,
  frontend or unrelated task-status changes. TASK-046 evidence remains untouched.

**Own onboarding status slice (2026-10-13):**

- GET /api/v1/teacher/onboarding requires validated Bearer JWT, current TEACHER role,
  unlocked account and verified email, but not onboarding approval. Identity comes only
  from authenticated context; query parameters/non-empty bodies are rejected. Other
  production routes and Teacher business eligibility remain unchanged.
- TeacherOnboardingService uses a read-only transaction and current-account recheck;
  TeacherApplicationRepository selects that Teacher's latest row by submittedAt DESC,
  id DESC. TeacherOnboardingStatus exposes only applicationId (decimal string), status,
  submittedAt, reviewedAt and rejectionReason. No reviewer identity, credentials or
  other user's application is exposed. Missing application uses the safe 404
  ONBOARDING_APPLICATION_NOT_FOUND response. No state/profile/session mutation.
- Focused verification: 100 passed, 0 failed/errors, 0 skipped. Includes eight own-status
  HTTP tests, two service tests, five added real-JWT rejection cases and preserved
  Student/Teacher registration, security and Admin review tests.
- Separate isolated PostgreSQL suite: 44 passed, 0 failed/errors, 0 skipped, including
  three added own-status tests for all states, timestamp/ID tie-breaking, owner isolation,
  missing application, account restrictions and unchanged application snapshots.
  Fresh protected disposable PostgreSQL 18 cluster at 127.0.0.1:55432/task007_refresh;
  unchanged V1/V2 applied locally through the existing empty-database guard. Server
  stopped afterward; no Supabase access and no existing database reset/reuse.
- Full Maven clean verify: BUILD SUCCESS; 258 total, 211 passed, 0 failed/errors,
  47 skipped. The 44 persistence skips passed separately above; three other opt-in
  database checks were not rerun. Packaged JAR excludes test fixtures; migration bytes,
  CRLF/final newlines and git diff --check verified.
- TASK-032 remains IN_PROGRESS. Pending queue/list, resubmission and frontend onboarding
  remain outside the approved slice. No migrations, dependencies, frontend, commit/push
  or unrelated task-status changes. TASK-046 evidence is preserved.

**Admin pending queue slice (2026-10-13):**

- GET /api/v1/admin/teacher-applications/pending reuses the verified/unlocked current
  ADMIN boundary and reviewer service recheck. Fixed PENDING-only results include
  unverified/locked applicants; no business permission or review reservation is granted.
- Strict page/size validation: defaults 0/20, size 1–100, unsigned single integer values,
  reject unknown/duplicate/invalid parameters, non-empty body and unsupported offsets.
  Authentication/authorization precedes input validation. Safe existing 400/401/403/503/500 errors.
- TeacherApplicationRepository selects only id, teacherId, status and submittedAt, ordered
  submittedAt ASC, id ASC, with offset and size + 1 limit. No count query, row lock or
  applicant User lookup. Explicit immutable TeacherApplicationQueue DTO uses decimal-string
  IDs and ISO timestamps; empty pages return 200 with hasNext=false and no-store caching.
- Focused queue/security verification: 84 passed, 0 failed/errors, 0 skipped. Includes
  strict validation, exact projection, reviewer restrictions, bounded lookup and regressions.
- Isolated PostgreSQL verification: 45 passed, 0 failed/errors, 0 skipped. Fresh protected
  disposable PostgreSQL 18.6 cluster on 127.0.0.1:55432/task007_refresh, empty-database guard,
  unchanged V1/V2 applied only locally; server stopped afterward. Queue tests cover status
  filtering, locked applicant visibility, timestamp/ID ties, last/empty pages and review
  removal/stale review conflicts; existing authentication/review concurrency checks pass.
- Full Maven clean verify: BUILD SUCCESS, 277 total: 229 passed, 0 failed/errors, 48 skipped.
  The 45 persistence skips passed separately above; DatabaseConnectivityTests,
  DatabaseMigrationTests and CurrentAccountPersistenceTests were not rerun in this slice.
- TASK-032 remains IN_PROGRESS. Resubmission and frontend onboarding remain unfinished.
  Offset pages can shift during concurrent review; no snapshot guarantee. No new dependencies,
  migrations, Supabase access, frontend changes, commit or push. TASK-046 evidence preserved.

#### Teacher application resubmission verification (2026-10-13)

- Implemented POST /api/v1/teacher/applications/{applicationId}/resubmit in the existing
  TeacherOnboardingController/Service, with current verified/unlocked Teacher authorization.
  Ownership derives from authenticated identity. Only the latest REJECTED snapshot can be
  superseded; PENDING/APPROVED history or an existing TeacherProfile prevents insertion.
- A READ COMMITTED transaction locks the current User row and inserts a new PENDING snapshot;
  reviewed rows remain unchanged. Return 201/no-store with the existing safe onboarding DTO
  only after commit. No automatic retry, cooldown or lifetime attempt limit is introduced.
- Focused Teacher onboarding/review/security tests: 86 passed, 0 failures/errors, 0 skipped.
  A Mockito restubbing error in the first run was corrected before the successful rerun.
- Isolated PostgreSQL 18.6 at 127.0.0.1:55432/task007_refresh: all 49
  RefreshSessionPersistenceTests passed, 0 failures/errors/skips. Coverage includes concurrent
  one-winner resubmission, insert-failure rollback, historical snapshot preservation, delayed
  retry rejection, ownership/account/profile conflicts and existing authentication/review checks.
  The fresh disposable cluster was stopped cleanly; no existing database was reset.
- Maven clean verify: BUILD SUCCESS; 288 reported, 236 passed, 0 failures/errors, 52 skipped.
  The 49 persistence tests skipped in that default run passed separately above; the other
  3 opt-in database checks were not run. Full verification includes three additional service
  unit tests for account/input rejection, prior approval and backwards-clock ordering.
- TASK-032 remains IN_PROGRESS because frontend onboarding is unfinished. No frontend,
  dependency, migration or Supabase changes; no commit/push. TASK-046 evidence is preserved.
#### Bounded Teacher-facing frontend (2026-10-13)

- /account supports initial Teacher registration, verification/resend, authenticated latest
  onboarding status and resubmission. Initial submission uses /auth/register/teacher only;
  it returns verification guidance without signing in. No separate application endpoint.
- Displays PENDING/APPROVED/REJECTED and available rejection reason. Uses /users/me current
  account and backend onboarding authority; only verified Teachers can view the workflow.
  Resubmission requires the latest REJECTED state, preserves exact submitted text and string IDs,
  guards duplicate clicks, and requests fresh status after any failed/uncertain submission.
  Failed reconciliation keeps submission disabled; no mutation is automatically replayed.
- Tests: tests/e2e/account.spec.ts covers exact requests, all states, 400/401/403/404/409/500/503,
  ownership-facing UI restrictions, duplicate clicks, lost responses with committed PENDING,
  failed status reconciliation, registration and verification. Narrow/wide Chromium suite:
  44 passed, 0 failed/skipped; shared tests 11 passed, 0 failed/skipped. Lint/typecheck/build pass.
- Backend DTO/controller compatibility was inspected; no backend file was changed by this
  frontend slice. Browser responses were mocked, so live backend/SMTP/CORS integration is not
  claimed. Earlier backend/PostgreSQL evidence remains separate and unchanged.
- TASK-032 remains IN_PROGRESS: Admin review UI and complete live browser onboarding/review
  verification remain outstanding. No dependency, migration, Supabase, commit or push changes.
  Unrelated TASK-046 evidence is preserved verbatim.
#### Admin review frontend completion (2026-10-13)

- Added frontend/src/features/teacher/admin-applications.tsx under the shared /account
  session. Verified current ADMIN accounts can load the PENDING queue, page at size 20,
  view safe details, approve with bodyless POST and reject with exactly preserved reason.
  Shared browser-api.ts adds explicit bodyless POST support and safe review-conflict messages.
- No automatic review retry. Duplicate-click guard is shared with authentication; reviewed
  detail has no mutation controls. Conflict/uncertain outcomes reconcile detail and queue;
  failed reconciliation removes review controls. Backend remains authorization authority.
- Tests/e2e/student-admin.spec.ts covers paging/empty results, exact fields and large IDs,
  Student/Teacher/unverified-Admin denial, review success, blank rejection validation,
  concurrent-review conflict, uncertain outcome, safe 401/403/404/500/503 and mobile overflow.
  Shared tests/browser-api.spec.ts verifies bodyless approval excludes cookies and sends Bearer.
- Combined verification: 12 unit/API tests and 82 desktop/mobile Chromium checks passed,
  0 failed/skipped; lint/typecheck/production build passed. These are mocked API browser tests,
  not live backend/CORS/CSRF/cookie/refresh/PostgreSQL/SMTP verification. Known Node module-type
  and terminal-color notices are non-failing; no dependency or configuration change made.
- TASK-032 remains IN_PROGRESS solely pending full live onboarding/review acceptance and any
  outstanding task dependencies. Admin browser implementation is no longer missing.
  Existing backend/migrations and unrelated TASK-046 evidence remain unchanged; no commit/push.
#### TASK-032 — Isolated live onboarding acceptance evidence (2026-10-14)

- TASK-032 remains IN_PROGRESS pending final acceptance/prerequisite review; see the
  TASK-007 live section for shared environment, regression counts and execution limitations.
- Real browser workflows passed on mobile and desktop: Teacher registration and captured
  email verification, PENDING status, Admin pending queue/detail/rejection, displayed reason,
  resubmission into a new PENDING application, Admin approval and APPROVED status.
- Live concurrent resubmission produced exactly one 201 and one 409; concurrent approval
  produced exactly one 204 and one 409. Reviewed snapshots stayed immutable, and read-only
  database checks confirmed the approved profile matched its application snapshot.
- Live boundaries rejected Student/Teacher Admin access, anonymous/invalid Bearer requests,
  client-supplied identity and cross-Teacher application access (safe 404). Invalid input,
  duplicate review/resubmission and unknown application responses stayed structured and safe.
- Responsive overflow checks passed at 390px/1440px. No API/UI/source changes were needed
  for this verification. Unrelated TASK-046 evidence and V1/V2 migration files are unchanged.

### TASK-007 / TASK-032 — Final acceptance review (2026-10-14)

**Decision: TASK-007 DONE; TASK-032 DONE for their approved implementation scopes.**
No mandatory acceptance gap or blocking code finding remained after tracing current
controllers, services, repositories, security filters, browser callers and test evidence.
Earlier IN_PROGRESS/remaining-work statements in Section 8 are checkpoint history;
this decision and the active task ledger are the current status.

| Mandatory criterion | Implementation and accepted evidence |
|---|---|
| Student registration and verification | RegistrationService atomically creates an unverified STUDENT and hashed purpose-bound credential without tokens; mail follows commit. Executed PostgreSQL tests cover canonical duplicates, rollback, expiry, replay, lock/purpose denial and delivery recovery. Live HTTP/browser registration, captured SMTP verification and eligible login passed. |
| Resend safety | User locking, 60-second cooldown and five accepted resends per rolling 24 hours. Executed persistence tests cover final allowance, failed-delivery counting and verification/resend races; live concurrent resend returned 202/429 and retained the prior unexpired code. |
| Shared login/refresh/logout | Current eligibility precedes signing; commit precedes credential return. Executed persistence tests cover invalid/expired/revoked credentials, one-winner rotation, collision/signing rollback, independent sessions and targeted logout. Live rotation/replay/logout/recovery passed. |
| Security and transport | RS256, database-authoritative role/account checks, BCrypt, explicit DTOs and default deny retained. Security tests cover forged roles, restricted accounts, safe failures and production-cookie configuration. Live exact-origin CORS, CSRF rejection, refresh-cookie attributes, memory-only access state and explicit recovery passed. |
| Teacher registration/eligibility | Atomic TEACHER + credential + PENDING snapshot; no initial profile/token/approval. Executed tests cover duplicate races and rollback. Live verification leaves onboarding PENDING. Eligibility tests require current verified/unlocked TEACHER and persisted approval; unimplemented Course routes remain denied. |
| Admin queue/detail/review | Verified/unlocked current ADMIN, stable bounded PENDING queue and safe projections. Executed tests cover pagination, reviewer restrictions, immutable history, profile conflict and rollback. Live review and concurrent one-winner approval passed. |
| Own status/resubmission | Authenticated owner, latest ordering and latest-REJECTED eligibility; new snapshot under User lock. Executed tests cover account/profile/prior-approval conflicts, rollback and ordering. Live cross-owner denial, retained history and concurrent 201/409 passed. |
| Browser workflow | 18 real-browser scenario groups passed at 390x844/1440x900; 82 mocked browser tests cover validation, denial, conflicts, duplicate-click guards and uncertain-response reconciliation. Frontend unit tests: 12 passed; lint/typecheck/build passed. |

**Evidence accounting:** Latest default Maven test phase: 236 passed, zero failures/errors,
52 skipped. Skips are not counted as passes. The separately recorded 2026-10-13 isolated
PostgreSQL run executed all 49 RefreshSessionPersistenceTests successfully, including the
current authentication/onboarding cases. The other three skipped suites are
DatabaseConnectivityTests, DatabaseMigrationTests and CurrentAccountPersistenceTests:
previously accepted infrastructure, not rerun in this review or counted as new passes.
No affected foundation/schema change requires their rerun for these two tasks.
Live HTTP scripts add 93 assertions, not 93 independent product workflows.
Packaging failed while the external backend held its JAR; after the user stopped it,
mvnw.cmd -DskipTests verify restored executable packaging with BUILD SUCCESS.
The user restarted before the final passing live browser checks. This is separate test
and packaging evidence, not one uninterrupted all-tests-enabled verify.

**Required versus deferred:** Expiry, resend-limit exhaustion, rollback and safe failure
handling are mandatory and covered by deterministic automated tests. Repeating them through
hours/days of live waiting or deliberately breaking the live database is not an additional
acceptance requirement. Controlled SMTP delivery is verified. Production deliverability,
deployed HTTPS/proxy checks, key operations, BCrypt capacity benchmarking and per-IP abuse
controls remain production/operational work; no production-readiness claim is made.
Password change/recovery belongs to TASK-008; email-change/profile work and Course-specific
ownership remain with their owning tasks. No missing mandatory test is reclassified optional.

**Dependencies/status:** The approved bounded sequencing exception allowed TASK-007 to use
verified TASK-006 foundations while broader TASK-006 work remained IN_PROGRESS.
This acceptance does not mark TASK-006 DONE or waive future feature authorization.
TASK-032's TASK-007 prerequisite is now satisfied. FEAT-004 remains IN_PROGRESS because
TASK-008 is unfinished. No other task/feature status or planning-readiness snapshot changes.

Only this tracker changed for acceptance. TASK-046 evidence is preserved exactly.
No tests rerun, database access, migration/source/configuration edits, staging, commit or push.

### TASK-046 — Teacher certificate documentation and V2 preparation

- Status: IN_PROGRESS (2026-10-08), limited to approved documentation and additive migration preparation. TASK-005 remains IN_PROGRESS and is not completed by this feature.
- Prepared backend/src/main/resources/db/migration/V2__create_teacher_certificates.sql: seven columns, TeacherProfile FK, status CHECK and Teacher/pending-review lookup indexes. V1 remains unchanged. V1 has 22 application tables; V1 plus applied V2 would have 23. Supabase V2 deployment is not claimed.
- Reconciled scope, requirements/rules/use cases, domain, architecture, database/API planning and task ownership. FEAT-023 tracks the separate feature; backend/frontend application work remains TODO.
- Verification is static only: compare V1 bytes, FK target/key type, migration order, columns/status constraints and changed-file scope; no Flyway/backend/database execution. Exact final check results are reported with this preparation.
- Remaining: review and deployment approval, isolated migration verification, upload/bucket/path/delivery/cleanup contracts, public-profile eligibility, exact API/UI and stale-review handling. No controllers, services, repositories, UI or provider integration.

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

Preserve its valid boundaries: current-account viewing and Batch 2 authentication HTTP endpoints exist, while the frontend remains neutral; TASK-006 Phases 1/2 add JWT validation/default deny and current-account role/lock resolution only. TASK-004 now installs Flyway as migration owner; Hibernate generation and Spring SQL initialization remain disabled. README's earlier Flyway-absent wording is historical and needs a separately scoped onboarding update. Do not rename existing Java packages, Maven artifacts, directories or database names merely to match the product title.
### Student registration username/phone extension — 2026-10-14

Extension status: DONE (implementation and isolated verification; not deployed).
TASK-007 and TASK-032 remain DONE for their accepted baselines; TASK-046 is unchanged.

- V3 adds nullable canonical unique username without modifying V1/V2 or backfilling.
- Student DTO/controller, StudentRegistrationInput, RegistrationService and User-owned
  persistence validate/normalize username and phone; RegistrationConflict maps only
  the known username unique violation to generic 409. Email-only login and unchanged
  Teacher creation are preserved. The shared /register form submits exactly five fields;
  confirmation stays local. Safe Vietnamese conflict text covers email or username.
- Full Maven verify: 290 passed, 0 failed/errors, 2 skipped (DatabaseConnectivityTests
  and CurrentAccountPersistenceTests; not claimed executed). Includes 50 isolated
  RefreshSessionPersistenceTests and one DatabaseMigrationTests scenario.
- New local PostgreSQL 18 cluster: 127.0.0.1:55433, dedicated task007_test credentials
  outside Git. task007_username_v3 exercises registration, rollback and concurrency;
  task004_username_v3 exercises populated V2 upgrade to V3 and restart. A second fresh
  task004_username_v3_checks run passed added direct CHECK/UNIQUE and duplicate-phone
  assertions. Prior test databases were preserved. No shared/Supabase connection.
- Frontend: 15 unit tests and 100 mocked browser tests passed, 0 failures/skips;
  lint, typecheck and production build passed. Desktop/mobile form and existing
  login, verification, Teacher onboarding and Admin review regressions passed.
- Executable JAR packaging succeeded after the external backend was stopped; it was
  not restarted. Existing test cluster on 55432 has a recovery/filesystem rename error;
  it was not reset/repaired or used for these migration tests.
- Deployment prerequisite: V3 first, then coordinated backend/frontend rollout; old
  three-field Student requests now fail validation. No production SMTP/live browser
  deployment claim, commit or push. Phone format does not prove ownership/allocation.

### Student immediate-login correction — 2026-10-15

Extension status: DONE (implementation and isolated verification; not deployed). TASK-007/TASK-032
accepted baseline statuses and unrelated TASK-046 evidence remain unchanged.
V4 introduces explicit Student-only login eligibility without asserting verified
email ownership; existing accounts default to false. New Student registration no
longer creates/sends verification credentials and redirects to /login.

- Resumed the saved V4/backend/frontend changes; completed affected fixtures, policy,
  migration, registration, security and browser tests rather than reimplementing them.
- Full Maven verify passed: 293 tests, 291 passed, 0 failures/errors, 2 skipped
  (DatabaseConnectivityTests and CurrentAccountPersistenceTests). This included
  50 PostgreSQL workflow tests and one populated V3-to-V4 migration/restart test.
- Subsequent focused verification passed 40/40 with no failures/skips: migration
  CHECK/NOT NULL assertions, 20 registration HTTP cases and 19 workflow cases.
  CurrentAccountPersistenceTests then passed separately (1/1, no failures/skips),
  verifying actual JPA reads of policy changes without asserting email ownership.
  DatabaseConnectivityTests remains skipped; no shared-target connectivity claimed.
- Actual PostgreSQL registration -> immediate login -> refresh -> logout passed;
  email_verified stayed false, no verification row/email was generated. Policy removal
  (including a username-bearing account) and locking rejected login/refresh. Teacher
  verification, review, resubmission, atomic rollback and concurrency regressions passed.
- Fresh isolated PostgreSQL 18 on 127.0.0.1:55434, dedicated task007_test credentials
  outside Git: task007_student_login_v4, task004_student_login_v4,
  task004_student_login_v4_checks and task006_student_login_v4. Existing databases
  were preserved. Port 55432 was untouched. The prior 55433 cluster could not start
  because pg_notify was missing; it was not repaired. New C: initialization also
  failed with a filesystem rename error; a fresh E: test directory initialized normally.
- Frontend: 15 unit tests and 100 mocked desktop/mobile browser tests passed,
  0 failures/skips; lint, typecheck and production build passed. First browser run
  was interrupted after configuration failures (missing NEXT_PUBLIC_API_ORIGIN);
  rebuilding with process-scoped http://127.0.0.1:8080 resolved them. No UI retry or
  authentication behavior was weakened. Browser results do not claim live HTTP/SMTP.
- V1/V2/V3 SHA-256 fingerprints unchanged; git diff --check passed; index remains
  empty. No Supabase, deployment, commit or push. Deploy V4 before the updated backend;
  production rollout and real browser-to-backend deployment checks remain separate.


### TASK-008 — six-digit OTP recovery verification, 2026-10-15

Implemented recovery controller/service, HMAC utility, existing-table repository, SMTP adapter,
User password update and User-first authenticated refresh locking. Added V5 failed-attempt
counter; V1–V4 unchanged. Frontend forgot/reset forms use email/code/password, retain CSRF,
return to login and do not sign in automatically. Password eye icons and concise validation
replace permanent registration helper paragraphs. Student immediate-login/username/phone,
Teacher verification/onboarding and Admin authorization are preserved.

- Backend full test run with isolated PostgreSQL: 316 total, 312 passed, 0 failed/errors,
  4 skipped. This includes 59 passing persistence/concurrency tests on fresh
  `127.0.0.1:55434/task007_otp_verified` (V1–V5 applied).
- Maven verify with separate `task004_otp_v5`: tests 316 total, 254 passed, 0 failed/errors,
  62 skipped; migration/restart test passed. Packaging failed because the existing JAR is
  locked. This is not a successful Maven verify.
- Frontend: 15 unit tests passed; lint, typecheck and production build passed with explicit
  isolated API origin. 112 mocked browser regressions passed at mobile/desktop viewports.
- Earlier timestamp projection failure was corrected and the PostgreSQL suite rerun successfully.
  The browser run without configured API origin was replaced by the passing configured run.
- Live HTTP/browser → SMTP capture → reset → login remains pending: Java NIO socket startup
  fails inside Codex. An external loopback-only startup script is prepared; user action requested.
- Per-IP deployment controls remain deferred; no production delivery or Supabase access.
  TASK-008 remains IN_PROGRESS. TASK-007/TASK-032 acceptance and TASK-046 evidence unchanged.


Recovery verification follow-up: the old backend JAR lock was released and full Maven
verify now passes (316 total: 253 passed, 0 failed/errors, 63 skipped; executable repackaging
successful). The separate PostgreSQL run passed all 59 persistence tests, and the separate
migration run passed its V1–V5/restart test; these are not counted as executed in the final
non-database verify. Authenticated STARTTLS loopback SMTP adapter: 1 passed, 0 failed/skipped.
Live complete recovery remains pending external backend startup; do not mark DONE.


### TASK-008 — bounded recovery final live acceptance, 2026-10-15

The previously pending external backend startup and live recovery checks are resolved.
Backend ran on `127.0.0.1:8081`, using only `127.0.0.1:55434/task007_otp_recovery`.
Read-only identity verification confirmed `task007_test`, loopback address/port, and the
preserved Student flags (`email_verified=false`, immediate-login policy enabled, unlocked).
SMTP used the authenticated STARTTLS capture sink on `127.0.0.1:1025`; its handler rejects
recipients outside `example.invalid` and never relays messages. No external mail was sent.

- Real HTTP journey: 9 checks passed, 0 failed: Student registration, initial login, neutral
  forgot response, six-digit email capture, password reset, revoked refresh rejection,
  old-password rejection, new-password login and consumed-code replay rejection.
- Real frontend journeys: 2 passed, 0 failed/skipped, at 390x844 and 1440x900. Actual backend
  responses were used without API mocks. Both verify forgot/reset navigation, SMTP capture,
  OTP submission, 204 reset, return to login without automatic authentication, revoked refresh,
  rejected old password/replay, successful new login and no horizontal overflow.
- Initial external browser-script failures were navigation races: filling the shared Email
  label before the destination page mounted. Explicit route waits fixed the test; both
  viewports were rerun successfully. No application-code change was needed.
- Evidence scripts outside Git: `E:/CodexHome/tmp/otp-live-http.py`, `otp-live.spec.cjs`,
  `otp-live.config.cjs` and `Read-OtpCapture.py`. OTPs/passwords were not printed; browser
  traces, video and screenshots were disabled for the secret-bearing live flow.
- Earlier verified evidence remains separate: 59 PostgreSQL persistence/concurrency tests;
  V1–V5 migration/restart test; 1 real STARTTLS SMTP adapter test; 15 frontend unit tests;
  112 mocked browser regressions; lint/typecheck/build; full Maven verify (253 passed,
  0 failed/errors, 63 skipped). Skipped tests are not counted as executed in that Maven run.

The bounded Forgot Password / Reset Password slice is ready for final review/acceptance.
TASK-008 remains IN_PROGRESS because logged-in password change is outside this slice and
not implemented. TASK-007 and TASK-032 remain DONE; unrelated TASK-046 evidence is preserved.
Per-IP abuse controls, trusted-proxy policy, production secret provisioning/rotation and
production SMTP delivery monitoring remain deployment work. No production/Supabase access,
port 55432 access, migration V1–V4 edits, staging, commit or push occurred in this acceptance run.


### TASK-008 — Change Password and recovery final acceptance, 2026-10-16

TASK-008 is DONE. This supersedes the earlier recovery-only IN_PROGRESS decision.
TASK-007 and TASK-032 remain DONE; unrelated TASK-046 evidence is unchanged.

Implementation: `PasswordChangeController`, `PasswordChangeService`, targeted
`RefreshSessionRepository.revokeOthers`, safe `AuthenticationFailure` mappings and
endpoint-scoped Bearer authentication in `SecurityConfiguration`. The standard Bearer
filter is used directly in the cookie-backed authentication chain: Resource Server DSL
would otherwise exempt Bearer requests from CSRF. Missing CSRF is tested as 403 before
mutation; public authentication/recovery still ignores unrelated stale Bearer tokens.
The existing User-first READ COMMITTED transaction preserves current session/expiry,
revokes other refresh sessions, changes the password and invalidates OTPs atomically.
No schema changes or new dependencies were needed.

Frontend: `/account` uses `ChangePassword`, shared `PasswordField` controls and
`authMutation` with both Bearer and CSRF/cookie transport. Confirmation is frontend-only;
duplicate clicks are guarded; no automatic refresh or mutation retry occurs. Tests cover
safe 400/401/403/500/503 behavior, input confirmation, transport and mobile/desktop overflow.

Verification (actual executions; skipped tests are not counted as passes):
- Focused HTTP tests: 34 passed, 0 failed/errors, 0 skipped.
- Full Maven `verify -DrefreshPersistence=true`: 325 total, **321 passed, 0 failed/errors,
  4 skipped**; executable JAR packaging succeeded. Log: `%TEMP%/change-acceptance-verify.log`.
- Included PostgreSQL suite: **65 passed, 0 failed/errors, 0 skipped**, fresh isolated
  `127.0.0.1:55434/task007_change_acceptance`, existing V1–V5 applied by the fixture.
  Includes session preservation/expiry, other-session revocation, OTP invalidation,
  rollback, concurrent changes, change-versus-refresh and change-versus-OTP-reset.
- Four conditional Maven checks skipped: DatabaseConnectivityTests, DatabaseMigrationTests,
  LocalResetSmtpTests and CurrentAccountPersistenceTests. No Supabase connectivity was attempted.
  Schema/persistence were exercised in the 65-test suite; real SMTP was exercised below.
- Frontend unit tests: **15 passed, 0 failed, 0 skipped**. Lint, typecheck and production
  build passed. Final mocked browser regression: **124 passed, 0 failed, 0 skipped**.
  The first browser run had 123 passes/1 transient existing navigation failure; that case
  passed both viewport rechecks and the subsequent full 124-test run.
- Real browser-to-backend/SMTP acceptance: **4 passed, 0 failed, 0 skipped** (Change Password
  and OTP recovery each on 390x844 mobile and 1440x900 desktop). Backend8081, local database
  `task007_otp_recovery` on55434, frontend3100 and authenticated STARTTLS capture on1025 only.
  Change checks compared actual refresh-cookie value and expiry before/after (unchanged),
  rejected missing CSRF, rejected the other session's refresh, invalidated captured OTP,
  allowed preserved-session refresh, rejected old password and accepted new password.
  Recovery checks captured a real six-digit email code, reset through the UI, rejected old
  password/replay/revoked refresh, and logged in with the new password without automatic login.
  Scripts/logs: `E:/CodexHome/tmp/change-live.spec.cjs`, `otp-live.spec.cjs`,
  `change-live.config.cjs`, `%TEMP%/change-live.log`. Secret-bearing traces/screenshots/video
  disabled; generated accounts use only `example.invalid`; SMTP never relays externally.
- `git diff --check` passed; staging area empty. No commit/push, port55432 access,
  Supabase access, migration modification or normal-backend8080 modification.

Remaining deployment limitations: other sessions' already-issued JWTs remain usable until
expiry under existing current-account checks; refresh revocation is not immediate JWT
revocation. Production per-IP abuse controls, secret management and production SMTP
monitoring remain deployment work, not claimed as verified by this isolated acceptance.


### TASK-009 — bounded Student profile verification, 2026-10-16

Implemented GET/PATCH `/api/v1/users/me/profile` with principal-derived Student identity,
current eligibility/lock checks, safe six-field projection and no-store responses.
PATCH allows only nonblank/non-null fullName/phone strings, preserves supplied name text,
rejects NUL/unknown fields/empty updates, and normalizes phone using registration validation.
Historical null username/phone values remain supported. User-row locks serialize updates;
only supplied profile columns and updated_at change. No migration/version column.
Existing `/users/me`, password/session/OTP workflows and read-only security fields are preserved.

Files for this slice:
- User feature: `dto/StudentProfile.java`, `repository/StudentProfileRepository.java`,
  `service/StudentProfileService.java`, `service/ProfileFailure.java`,
  `controller/StudentProfileController.java`, `controller/ProfileExceptionHandler.java`.
- SecurityConfiguration allows only the approved GET/PATCH for STUDENT; BrowserSecurityFilters
  adds PATCH to exact-origin CORS methods. No cookie authentication added to business APIs.
- Frontend `features/user/student-profile.tsx`, account-workspace integration and a scoped
  responsive grid in globals.css; existing ChangePassword component behavior is unchanged.
- StudentProfileServiceTests, StudentProfileHttpTests, four PostgreSQL profile tests in
  RefreshSessionPersistenceTests, existing no-database test fixture mocks and
  `frontend/tests/e2e/student-profile.spec.ts`.
- API_DESIGN.md, BUSINESS_RULES.md, USE_CASES.md and this evidence record.

Verification:
- Focused backend tests: 6 passed, 0 failed/errors, 0 skipped.
- Full Maven verify with isolated persistence enabled: 335 total, **331 passed, 0 failed/errors,
  4 skipped**; executable JAR packaging passed (`%TEMP%/profile-verify.log`).
- Included PostgreSQL suite: **69 passed, 0 failed/errors, 0 skipped**, on fresh local
  `127.0.0.1:55434/task007_profile_acceptance`; previous databases preserved. Profile coverage
  includes historical nulls, eligibility/locks, invalid-update atomicity, forced rollback,
  security-field preservation, concurrent partial updates and last-committed same-field wins.
- Conditional skipped Maven checks: DatabaseConnectivityTests, DatabaseMigrationTests,
  LocalResetSmtpTests and CurrentAccountPersistenceTests. They are not counted as executed.
  Existing migrations run in the persistence fixture; live SMTP is separately covered below.
- Frontend unit tests: **15 passed, 0 failed, 0 skipped**; lint, typecheck and production build
  passed. Final desktop/mobile browser regression: **136 passed, 0 failed, 0 skipped**
  (`%TEMP%/profile-browser-final.log`), including 12 new profile cases.
- Live acceptance: **6 passed, 0 failed, 0 skipped**: Student Profile, Change Password and
  six-digit OTP recovery each on desktop1440x900/mobile390x844. Backend8081, frontend3100,
  isolated database55434 and authenticated STARTTLS SMTP capture1025 only. Profile checks
  verify actual CORS PATCH, persisted name/+84 phone, protected-field rejection, unchanged
  `/users/me` response, unchanged refresh cookie/expiry, successful refresh and grid geometry.
  The first profile layout run used the earlier build and timed out on the new grid selector;
  the corrected/rebuilt run passed all six journeys. No credentials captured in traces or reports.
  Live scripts: `E:/CodexHome/tmp/profile-live.spec.cjs` and `profile-live.config.cjs`;
  final log `%TEMP%/profile-live-final.log`.

TASK-009 and FEAT-005 remain IN_PROGRESS: Teacher own-profile scope is not implemented here.
TASK-007, TASK-008 and TASK-032 remain DONE; unrelated TASK-046 evidence preserved. No Supabase,
port55432, production email, migration modification, staging, commit or push. No new dependencies.

Student profile slice is ready for final review/acceptance. Final `git diff --check` passed;
staging remains empty. Teacher profile work is the remaining TASK-009 scope.

### Batch Calendar 01 evidence — 2026-10-27

- Added eligible-Teacher, owned-Course interval Calendar API and `/teacher/calendar`
  month/week UI inside the existing persistent Dashboard. The existing Lịch dạy
  navigation opens this route; Course/Session management remains unchanged.
- Backend: 39 passed, 0 failures, 0 errors, 0 skipped (Calendar HTTP/repository/range,
  Course service/HTTP, Session HTTP and Teacher Category HTTP focused tests).
- Frontend: TypeScript and ESLint passed; 19 unit tests passed; isolated production
  build passed with NEXT_PUBLIC_API_ORIGIN=http://localhost:8081.
- Mocked desktop/mobile browser regressions: 58 passed, 0 failed, 0 skipped,
  one worker, zero retries, isolated port 3012. Includes 8 Calendar cases plus
  Unified Dashboard, Teacher login, Course and Session regressions.
- Evidence: E:/CodexHome/tmp/calendar01-backend-final.log and
  E:/CodexHome/tmp/calendar01-validation/{checks.log,browser.log}.
- Repository query tests verify ownership predicates, overlap SQL and pagination
  through mocked EntityManager; they do not prove real PostgreSQL query execution.
- Live Calendar acceptance remains pending: the running backend has previous classes
  and the available approved disposable Teacher has no Courses/Sessions. No fixtures,
  migrations or Supabase records were changed; existing services were preserved.
- TASK-035/TASK-037 remain IN_PROGRESS. Student Calendar remains sample-based;
  authorized Enrollment integration and Google Calendar are outside this batch.

### Batch Calendar 01.1 live acceptance — 2026-10-27

- Restarted only the verified project backend using the existing guarded launcher;
  loaded the Calendar classes with Flyway disabled, ddl-auto=none, SQL init=never.
  JWT, authenticated STARTTLS and Hikari startup passed. Existing SMTP preserved.
- Served the previously verified Calendar build from the isolated validation directory
  on port 3000, with NEXT_PUBLIC_API_ORIGIN=http://localhost:8081.
- Pinned Supabase project ddmjdhnjhyllirjqhqrc, database postgres, schema public.
  Bounded READ ONLY transaction confirmed successful V1–V6, zero Courses and zero
  Sessions, and ended with ROLLBACK. No schema or application records changed.
- Live API assertions: 28 passed, 0 failed. Includes existing disposable account
  login/logout, approved Teacher 200, Student/PENDING Teacher 403, anonymous 401,
  range/pagination/unknown-identity validation, no-store, offset-boundary queries,
  empty response contract and repeated/paginated real repository query execution.
- The fixture named rejected is actually PENDING. REJECTED denial remains untested
  live; no application status was changed to manufacture coverage.
- Live browser assertions: desktop 15 passed, mobile 15 passed, 0 failed. No mocked
  API responses. Covered Calendar month/week/navigation, empty state, persistent
  shell, back/forward, refresh-to-login boundary, overflow, offline error and online
  retry, and logout cleanup. Desktop/mobile screenshots reviewed.
- Evidence outside repository: E:/CodexHome/tmp/calendar011-api-results.json,
  calendar011-browser-results.json, calendar011-api.cjs, calendar011-browser.cjs,
  calendar011-1440.png and calendar011-390.png. git diff --check passed.
- SQL compatibility and parameter binding succeeded against real PostgreSQL for
  empty results. Populated DTO mapping, ordering, timezone display, cancelled rows
  and cross-Teacher disclosure cannot be established with zero Session fixtures.
- Batch Calendar 01 is not DONE. Required approved fixtures: a second eligible
  disposable Teacher, Teacher-owned Courses with distinct Sessions across both
  owners (including a cancelled Session and timezone/boundary cases), and a genuinely
  REJECTED disposable Teacher. Fixture creation and onboarding changes need approval.
- No application source changes were required. Existing dirty work preserved.
  Only authorized authentication sessions were created/revoked during acceptance;
  no account/profile/Course/Session business records were modified.


### Batch Calendar 01.2 — CI preparation (2026-10-27)

- Extended the existing manual-only task009-acceptance.yml; no duplicate workflow.
  Existing sequential SMTP/profile jobs and 24 mocked/8 live profile cases remain.
- Browser job adds 58 mocked Dashboard/Course/Session/Calendar regressions and two
  live populated Calendar cases (desktop/mobile) using the existing PostgreSQL 18
  container, private SMTP sink, generated secrets and single-worker safe reporter.
- Flyway target/history checks now require exactly V1–V6; no migration SQL changes.
- Calendar fixtures use API registration/email verification and Admin review, then
  guarded atomic insertion of two owned Courses/eight Sessions only in the exact
  disposable CI container. No Supabase connection or local service startup occurred.
- Live tests cover both owners, rejected eligibility, cancelled Sessions, stable
  ordering, pagination, strict overlap, offset/midnight boundaries, month/week UI,
  Course timezone display and navigation into existing Session details.
- CI has NOT been triggered. Batch Calendar 01 remains IN_PROGRESS pending actual
  populated execution. Publishing must include the existing uncommitted backend
  and frontend dependencies. No commits or pushes.
