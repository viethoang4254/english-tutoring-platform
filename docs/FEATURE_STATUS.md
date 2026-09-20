# Feature Status

## 1. Authority and scope

This is the sole authoritative implementation-status and completion-evidence source for features and tasks. [TASK_BREAKDOWN.md](TASK_BREAKDOWN.md) defines scope, completion criteria, dependencies, API ownership and decision gates; it does not duplicate live status.

The approved design baseline is 22 features, 30 tasks, 72 API operations (42 FINAL; 30 PROVISIONAL), and 18 application tables. Design documentation does not establish implemented software. TASK-001 implementation and verification are complete; FEAT-001 remains partially implemented. Evidence and attribution are recorded below.

## 2. Status and evidence rules

Use only TODO, IN_PROGRESS, BLOCKED and DONE for implementation status.

- TODO: implementation has not started. Open future decision gates alone do not force BLOCKED when meaningful safe prerequisite work exists.
- IN_PROGRESS: implementation genuinely started but the complete acceptance criteria/verification have not passed.
- BLOCKED: a concrete unresolved dependency prevents further meaningful implementation. Record the dependency, affected work and what would unblock it; do not hide completed portions.
- DONE: the applicable task/feature completion criteria and required verification pass, with actual evidence. Do not reimplement without an explicit change, bug fix, extension or refactor request.

Backend, frontend, persistence and verification/test components are tracked where applicable using the same statuses. Omit non-applicable components instead of inventing a fifth status. A feature is DONE only when every required task/component and its integration are complete; a partial backend or documented API is insufficient.

Update the task ledger and affected feature/component records together to reflect the actual codebase. A blocked task does not automatically block an entire feature if other meaningful work remains. Keep partially completed work IN_PROGRESS unless a concrete gate halts further work. Completion evidence must identify implemented paths, actual verification results/commands, and commit references when available; no commit is required merely to record progress.

Record remaining work and blockers precisely; request approval for material scope/design changes. Never alter status just to make progress look complete. Preserve stable IDs and API FINAL/PROVISIONAL classifications until a separately approved contract change.

The original TODO baseline was intentional for every entry: safe foundation, approved portions, provider-independent boundaries or test preparation can proceed when implementation is authorized. This is not a claim that unresolved contract-dependent behavior is ready to implement. Consult each task's gates before starting; use BLOCKED at the point where no meaningful safe work remains.

## 3. Feature records

Each completion definition below incorporates the concrete criteria and verification of its listed tasks in TASK_BREAKDOWN.md. Pending decision gates are dependencies, not evidence of work completed.

### FEAT-001 — Development/runtime foundation

- Overall status: IN_PROGRESS.
- Component status: Backend IN_PROGRESS; Frontend TODO; Persistence TODO; Tests IN_PROGRESS.
- Related tasks: TASK-001, TASK-002, TASK-003, TASK-005.
- Pending decisions/dependencies: TASK-001 backend toolchain approved and verified; G-TOOLCHAIN for remaining work, G-UI and G-ENV remain open. Prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Backend builds and starts reproducibly; directories and commands are documented; no unapproved infrastructure or domain implementation. Frontend builds and renders the technical foundation; no unapproved navigation or interaction design is implemented. G-UI remains a dependency for every later product screen. Local development is reproducible and approved database connection checks pass; secrets remain external. Supabase validation cannot be claimed complete while deferred. Shared conventions are usable by feature controllers/clients and verified; no JPA entities leak as public contracts.
- Evidence: TASK-001 skeleton in backend/, .gitignore and README.md; directories and build/run commands documented, no domain implementation or unapproved infrastructure. On 2026-09-19, wrapper scripts matched official 3.3.4 only-script files before the approved one-line mvnw.cmd null-Target guard repair. Agent-observed verification with Oracle JDK 17.0.12: `mvnw.cmd --version` confirmed Maven 3.9.16; `mvnw.cmd clean verify` reported BUILD SUCCESS, Java release 17 compilation, 1 application-context test passed (0 failures/errors/skips), and successful executable-JAR packaging. Test report: backend/target/surefire-reports/com.englishlearning.EnglishLearningApplicationTests.txt; artifact: backend/target/english-learning-backend-0.0.1-SNAPSHOT.jar. User-reported manual verification in ordinary Windows PowerShell outside the Codex execution environment used `C:\Program Files\Java\jdk-17\bin\java.exe` with that packaged JAR and `--server.address=127.0.0.1 --server.port=18080`: Tomcat started on 127.0.0.1:18080 with context path /; `Started EnglishLearningApplication in 2.957 seconds` was observed; the process remained alive until manually stopped. No database credentials or external services were required. Manual startup/stop evidence is user-reported, not agent-observed.
- Verification environment note: Earlier Codex-launched runs, including elevated retries, failed in Java AF_UNIX/Selector initialization. The user also confirmed direct UNIX-domain BIND/CONNECT/ACCEPT/CLEANUP success in ordinary Windows PowerShell. Record this as a Codex execution-environment limitation, not an application defect or remaining TASK-001 blocker. Java target remains 17; no application/toolchain/network workaround was introduced.
- Remaining work: None for TASK-001. TASK-002, TASK-003 and TASK-005 remain unstarted; FEAT-001 Backend/Tests are only partially complete, and Frontend/Persistence remain TODO.

### FEAT-002 — Persistence and migration foundation

- Overall status: TODO.
- Component status: Backend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-004.
- Pending decisions/dependencies: G-SCHEMA, G-NORMALIZATION; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: All approved baseline schema is migration-managed and verified, with documented treatment of provisional details and no uncontrolled ORM schema generation. Partial migrations do not make the task complete.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-003 — Security and authorization foundation

- Overall status: TODO.
- Component status: Backend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-006.
- Pending decisions/dependencies: G-AUTH, G-SECURITY; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Approved security foundation passes enforcement checks; transport-dependent portions cannot be called complete before their decisions.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-004 — Authentication and account lifecycle

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-007, TASK-008.
- Pending decisions/dependencies: G-AUTH, G-SECURITY, G-NORMALIZATION, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Registration and permitted session lifecycle work end-to-end without credential leakage; all owned API contracts and required security tests pass. All approved password operations pass success/rejection/replay checks and frontend flow verification.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-005 — Current-user/profile

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-009.
- Pending decisions/dependencies: G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Current identity/profile displays correctly and only permitted own fields change; all contract/security checks pass.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-006 — Course discovery and Student content access

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-011.
- Pending decisions/dependencies: G-DISCOVERY, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Discovery and authorized content reads satisfy visibility and DTO boundaries, including pre-enrollment summaries and rejected restricted access.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-007 — Teacher Course management

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-010.
- Pending decisions/dependencies: G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Teacher can create/edit/manage owned Courses and use approved lifecycle operations; Admin-only classification and history retention are enforced.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-008 — Lesson/content authoring

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-012.
- Pending decisions/dependencies: G-COMPLETION, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Owned Lesson content and vocabulary assignment operations work with approved completion configuration and preservation rules.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-009 — Shared vocabulary and dictionary integration

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-013, TASK-014.
- Pending decisions/dependencies: G-NORMALIZATION, G-DICTIONARY, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Platform vocabulary operations work on permitted data with approved normalization; provider integration is independently TASK-014. Approved candidate/import contracts work and licensing/storage obligations are met; external-provider behavior is not guessed.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-010 — Exercise/question authoring

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-015.
- Pending decisions/dependencies: G-COMPLETION, G-INTEGRITY, G-DICTIONARY, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Approved authoring operations pass validation and ownership checks without leaking answer keys or invalidating historical results.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-011 — Enrollment and Course access enforcement

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-016.
- Pending decisions/dependencies: G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Enrollment and access checks are authoritative, reusable and correct for repeated requests and entitlement changes.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-012 — Attempts, answer evaluation and historical results

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-017.
- Pending decisions/dependencies: G-INTEGRITY, G-RETRY, G-COMPLETION, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: All Lesson learning types produce exactly-once accepted evidence/results under the approved protocol and preserve understandable history.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-013 — Progress and vocabulary performance

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-018.
- Pending decisions/dependencies: G-COMPLETION, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Approved progress/mastery reads match rules and cannot be forged or destroyed by subscription changes.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-014 — Saved vocabulary and Review

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-019, TASK-020.
- Pending decisions/dependencies: G-UI, G-INTEGRITY, G-RETRY, G-COMPLETION, G-REVIEW; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Student can reliably save/list/remove own associations without affecting canonical content/history. Review works across approved sources with shared trusted persistence and no duplicated learning engine.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-015 — Subscription plans and entitlement

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-021.
- Pending decisions/dependencies: G-COMMERCE, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Plan/subscription reads and reusable coverage evaluation are verified; no production grant is created without approved payment verification.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-016 — Payment lifecycle

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-022.
- Pending decisions/dependencies: G-PAYMENT, G-RETRY, G-COMMERCE, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Payment flow and retry handling are provider-approved and verified; each successful grant is traceable and historical periods remain immutable.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-017 — Admin users and Teacher administration

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-023.
- Pending decisions/dependencies: G-ADMIN, G-SECURITY, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Admin inspection and approved provisioning/actions work with verified authorization; unresolved action branches cannot be declared complete.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-018 — Admin Course administration

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-024.
- Pending decisions/dependencies: G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Authorized Admin can inspect/classify Courses; Teacher classification injection and unauthorized mutations fail.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-019 — Teacher analytics

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-025.
- Pending decisions/dependencies: G-ANALYTICS, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Teacher analytics match approved definitions and reveal only authorized owned-Course information.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-020 — Admin subscription/payment administration

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-026.
- Pending decisions/dependencies: G-PAYMENT, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Admin can inspect permitted historical periods/payments with accurate mapping and traceability.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-021 — Admin Dashboard and revenue analytics

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-027.
- Pending decisions/dependencies: G-REPORTING, G-PAYMENT, G-UI; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Required dashboard/analytics metrics are accurate and available under approved reporting policy; unavailable provisional metrics cannot count as completed implementation.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

### FEAT-022 — Release verification and operations

- Overall status: TODO.
- Component status: Backend TODO; Frontend TODO; Persistence TODO; Tests TODO.
- Related tasks: TASK-028, TASK-029, TASK-030.
- Pending decisions/dependencies: G-UI, G-DEPLOY, G-TOOLCHAIN; prerequisite task links are in TASK_BREAKDOWN.md.
- Completion definition: Required cross-feature checks pass with evidence; unresolved critical behavior or failing checks prevents completion. Approved target environments and operational checks are complete with evidence; no deployment/provider choice is inferred by this plan. Build/check automation and the approved release scope are verified. Early CI completion does not mark the combined task DONE while required release automation remains.
- Evidence: None recorded; implementation not started.
- Remaining work: All listed task deliverables and applicable verification.

## 4. Task status ledger

The full task specification and API mapping remain in TASK_BREAKDOWN.md. This ledger records live task status and evidence only.

| Task | Feature | Status | Pending decision gates | Evidence / remaining work |
|---|---|---|---|---|
| TASK-001 | FEAT-001 | DONE | None for TASK-001; approved Java 17 baseline preserved. | Agent-verified Maven 3.9.16, clean verify BUILD SUCCESS, release 17 compilation, 1 passing context test and executable JAR; user-confirmed Oracle 17.0.12 packaged startup/manual stop outside Codex. See FEAT-001 evidence and environment note. |
| TASK-002 | FEAT-001 | TODO | G-TOOLCHAIN, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-003 | FEAT-001 | TODO | G-ENV | None recorded; all task deliverables/verification remain. |
| TASK-004 | FEAT-002 | TODO | G-SCHEMA, G-NORMALIZATION | None recorded; all task deliverables/verification remain. |
| TASK-005 | FEAT-001 | TODO | Task prerequisites only | None recorded; all task deliverables/verification remain. |
| TASK-006 | FEAT-003 | TODO | G-AUTH, G-SECURITY | None recorded; all task deliverables/verification remain. |
| TASK-007 | FEAT-004 | TODO | G-AUTH, G-SECURITY, G-NORMALIZATION, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-008 | FEAT-004 | TODO | G-SECURITY, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-009 | FEAT-005 | TODO | G-UI | None recorded; all task deliverables/verification remain. |
| TASK-010 | FEAT-007 | TODO | G-UI | None recorded; all task deliverables/verification remain. |
| TASK-011 | FEAT-006 | TODO | G-DISCOVERY, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-012 | FEAT-008 | TODO | G-COMPLETION, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-013 | FEAT-009 | TODO | G-NORMALIZATION, G-DICTIONARY, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-014 | FEAT-009 | TODO | G-DICTIONARY, G-NORMALIZATION, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-015 | FEAT-010 | TODO | G-COMPLETION, G-INTEGRITY, G-DICTIONARY, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-016 | FEAT-011 | TODO | G-UI | None recorded; all task deliverables/verification remain. |
| TASK-017 | FEAT-012 | TODO | G-INTEGRITY, G-RETRY, G-COMPLETION, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-018 | FEAT-013 | TODO | G-COMPLETION, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-019 | FEAT-014 | TODO | G-UI | None recorded; all task deliverables/verification remain. |
| TASK-020 | FEAT-014 | TODO | G-INTEGRITY, G-RETRY, G-COMPLETION, G-REVIEW, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-021 | FEAT-015 | TODO | G-COMMERCE, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-022 | FEAT-016 | TODO | G-PAYMENT, G-RETRY, G-COMMERCE, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-023 | FEAT-017 | TODO | G-ADMIN, G-SECURITY, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-024 | FEAT-018 | TODO | G-UI | None recorded; all task deliverables/verification remain. |
| TASK-025 | FEAT-019 | TODO | G-ANALYTICS, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-026 | FEAT-020 | TODO | G-PAYMENT, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-027 | FEAT-021 | TODO | G-REPORTING, G-PAYMENT, G-UI | None recorded; all task deliverables/verification remain. |
| TASK-028 | FEAT-022 | TODO | G-UI | None recorded; all task deliverables/verification remain. |
| TASK-029 | FEAT-022 | TODO | G-DEPLOY | None recorded; all task deliverables/verification remain. |
| TASK-030 | FEAT-022 | TODO | G-TOOLCHAIN, G-DEPLOY | None recorded; all task deliverables/verification remain. |

## 5. Updating completion evidence

When work is authorized and actually performed, replace the relevant evidence text with concise references to implementation files, verification/test command and result, and commit when available. Include remaining gaps for partial work; for BLOCKED include the exact unmet dependency and required decision/action. Keep these facts here rather than a duplicate progress log in TASK_BREAKDOWN.md. For browser E2E checks, record the Playwright command and result when run; identify Spring Boot/JUnit and backend integration results separately.

Before starting, inspect existing files and evidence to avoid duplicate implementation. Before completion, compare against current requirements/rules/use cases, API contracts and database design. If evidence contradicts status, report and correct the status honestly; do not infer a product change.

No implementation, tests, migration execution, provider connection, deployment or release is claimed by this initial tracker.
