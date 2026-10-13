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
| DONE | 6 |
| TODO | 20 |
| BLOCKED | 7 |
| IN_PROGRESS | 5 |
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
| TASK-007 | Student registration and shared authentication sessions | IN_PROGRESS | TODO | Backend registration/verification/resend and login/refresh/logout verified; frontend and production mail delivery remain outstanding. See Section 8, TASK-007. |
| TASK-008 | Dedicated password change and recovery | TODO | TODO | No implementation evidence. Applicable prerequisites/gates remain in TASK_BREAKDOWN. |
| TASK-009 | Current account and own personal profile | IN_PROGRESS | TODO | Bounded account-read exception implemented before TASK-007; profile work remains. See Section 8, TASK-009. |
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
| TASK-032 | Teacher registration and controlled onboarding | IN_PROGRESS | DEFERRED | Registration and bounded Admin review verified; queue/status/resubmission/frontend remain excluded. See Section 8, TASK-032. |
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
| TASK-032 | Registration and bounded Admin detail/review/profile creation are implemented. G-TEACHER/G-API/G-UI remain for queue, resubmission, Teacher status lookup and browser onboarding. |
| TASK-037 | G-TIME/G-RECURRENCE/G-SESSION: ISO weekly generation, IANA timezone, fixed unique numbering and no-replacement cancellation settled; DST/cutoff and operation exceptions remain open. |
| TASK-041 | G-RECEIVING/G-PAYMENT: historical bank accounts and one ACTIVE settled; bank checks and verified adapter integration contracts remain open; conceptual provider fields are omitted. |
| TASK-043 | G-CALENDAR: organizational synchronization settled; single configured Calendar/event-ID scope settled; execution/retry contracts remain open. |
| TASK-044 | G-FEEDBACK/G-API: one completed-Enrollment rating 1..5 settled; exact acceptance/visibility/edit/moderation and executable contracts remain open. |

These entries record missing decisions, not failures of the completed infrastructure. Do not invent schema, integration or policy to remove a blocker. Other tasks remain TODO where meaningful safe prerequisite work can later be authorized.

## 6. Current Capability Summary

The table distinguishes implemented responsibilities from remaining gated work. Unapproved operation contracts remain gated as described upstream.

| Capability | Current implementation/evidence boundary | Task ownership |
|---|---|---|
| Authentication/account security | Student and bounded Teacher registration plus shared email verification/resend are implemented; password change/forgot/reset remain unimplemented. TASK-007 supplies verified login/refresh/logout HTTP endpoints, CSRF bootstrap and configured cookie/CORS controls over its atomic issuance/refresh core. TASK-006 validates bearer JWTs, resolves current User role/locked state and preserves default deny; Database-backed Teacher eligibility and bounded Admin onboarding review are implemented; business-route ownership integration remains with each feature. Bounded current-account viewing is authorized; broader onboarding remains gated. | TASK-006, TASK-007, TASK-008, TASK-032 |
| User/Teacher profiles | Minimal current-account identity viewing and atomic TeacherProfile creation on Admin approval are implemented; own-profile editing and public Teacher projection remain unimplemented/gated. | TASK-009, TASK-033 |
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
