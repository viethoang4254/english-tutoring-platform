---
name: authentication-security
description: Use when designing, implementing, reviewing, or testing authentication, Spring Security, JWT and refresh tokens, password handling, account security, or backend role, ownership, and Enrollment-based authorization for the English Tutoring Platform.
---

# Authentication and Security

## Context and Scope

Follow `AGENTS.md` and the project's requirements-first development order. This skill does not authorize implementation before relevant requirements and designs are defined.

Current AGENTS.md is authoritative during the tutoring-scope transition.
Conflicting legacy business rules in other documents are superseded; consult
reconciled requirements or compatible technical/security guidance. Report remaining
ambiguities rather than treating historical plans as current approval.
Preserve completed TASK-001/TASK-002/TASK-003 infrastructure and verification evidence.
This skill does not authorize schema changes, implementation or unrelated edits.

Read relevant sections of these repository-root documents for the task:

- `docs/PROJECT_SPEC.md`: scope and actors.
- `docs/REQUIREMENTS.md`: `FR-STU-*`, `FR-ACC-*`, `FR-AUTH-*`, `NFR-SEC-*`, and affected feature requirements.
- `docs/BUSINESS_RULES.md`: `BR-AUTHN-*` active definitions, `BR-ROLE-*`, `BR-AUTH-*`, and
  applicable ownership/access rules.
- `docs/USE_CASES.md`: affected `UC-AUTH-*` or protected feature workflow.
- `docs/DOMAIN_MODEL.md`: `DM-USER-*`, `DM-AUTH-*`, and affected resource boundaries.
- `docs/ARCHITECTURE.md`: current authentication, ownership, integration and privacy sections.

Use `requirements-analysis` for missing business decisions, `english-tutoring-domain` for domain interpretation, and `system-design` for architecture changes. Legacy identifiers and section numbers are lookup aids, not approval of superseded rules. Distinguish compatible documented defaults from unresolved details. Report conflicting statements with their sources and request clarification before dependent work. Do not silently select an interpretation or invent permissions.

## Authentication and Account State

- Use the documented email/password, Spring Security, JWT Access Token, and Refresh Token architecture with one shared User identity model.
- Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher
  registration are separate; V1 has no Student-to-Teacher promotion. Teacher business
  authority requires TEACHER role, verified email, an unlocked account and approved
  onboarding. Application snapshots/history are retained, with at most one PENDING
  application per Teacher; the current public TeacherProfile is created after approval and
  does not rewrite application snapshots. Admin provisioning remains authorized
  administrative/system setup.
- Preserve applicable account-verification safeguards without inventing new onboarding states or transitions. Verification must not override an administrative restriction; recheck legacy account rules against the current approved workflow.
- users.locked is the sole account-blocking mechanism: locked Users cannot authenticate or
  perform normal account use. No disabled/enabled/account_status lifecycle. Teacher
  authority additionally requires verified email and approved onboarding. Stale token state
  cannot bypass checks.
- Resolve current-user information from authenticated backend identity, not a client-selected user ID.

## JWT and Refresh Lifecycle

- Validate JWT signature and expiration before establishing authenticated identity. Use supported security mechanisms, never custom cryptography.
- Keep claims minimal; exclude passwords, hashes, reset/verification credentials, and secrets. JWT claims do not permanently establish mutable ownership or Enrollment access.
- Preserve configurable defaults: Access Token **15 minutes**, Refresh Token **7 days**, password-reset credential **15 minutes** (`BR-AUTHN-007`, `BR-AUTHN-008`, `BR-AUTHN-016`).
- Validate server-side refresh state, expiration, revocation, and account/session eligibility before issuing credentials. Invalid, expired, or revoked refresh credentials cannot mint an Access Token.
- Support backend-controlled rotation and invalidation of replaced credentials according to the approved security design. Do not invent replay, concurrency, or session-limit policies.
- Logout revokes applicable refresh/session state and clears applicable client cookies/state. Frontend deletion alone is insufficient; do not claim this automatically revokes every issued Access Token.
- Keep refresh/reset/verification state User-bound and hashed; never persist raw secrets.
  Use approved supporting tables, no access JWT table.

## Passwords and Recovery

- Use an appropriate Spring Security `PasswordEncoder`; never store plaintext, expose hashes, or implement custom hashing. Do not select an undocumented algorithm, cost, or password-complexity policy as an approved requirement.
- Follow dedicated password-change checks; revoke other refresh sessions while preserving
  the current session. Successful password reset revokes all refresh sessions.
- Reset credentials must be unpredictable, purpose-specific, account-bound, expiring, and unusable after successful consumption. Validate these conditions before changing the password.
- Verification credentials are purpose-specific, User-bound, expiring and single-use.
  Canonicalize email consistently; verified email change keeps the old address effective
  until the new one verifies and updates relevant future Calendar attendees.
- Forgot-password responses avoid unnecessary account-existence disclosure. Provider failures must not expose recovery credentials.

## Authorization and Ownership

- Keep authentication, role authorization and Enrollment-based participation separate. Roles are STUDENT, TEACHER, ADMIN; Payment/Enrollment state is not a role.
- Spring Security handles authentication/coarse role checks; backend services or reusable policies enforce account state, ownership, enrollment, Course accessibility as applicable. UI guards are not security boundaries.
- Students cannot access Teacher/Admin management; Teachers cannot access Admin-only operations. Do not infer unrestricted Admin overrides from the Admin role.
- Teacher Course operations require ownership and operation permission. Traverse Session -> Course and Assignment -> Session -> Course for children, including submissions/results, enrolled-Student information, progress and related payment data. Teacher role alone grants no cross-owner access.
- Resolve Student/Teacher personal workflows from authenticated identity. Protect profiles, submissions, results and participation information. Any broader access requires a documented permission.
- Student protected Course participation, content, Assignments and the Course Meet URL require valid backend-authoritative Enrollment where required. Do not trust client flags or a long-lived JWT claim alone.
- Actual provider/bank transactions may be unmatched. They retain receiving-account context
  when resolvable, independently of Payment matching; an unresolved receiver remains a
  reconciliation concern. Browser, Student and Teacher claims are not confirmation evidence.
  Confirmation requires trustworthy provider/bank evidence matching the intended receiver,
  code, amount and currency; wrong/missing codes or amounts do not auto-confirm, partial
  transfers are not summed automatically, and late transactions cannot cause overbooking.
  VietQR is the direction, with provider contracts still gated. Full refunds use Teacher
  transfer/proof and authorized Admin verification, not Admin custody. Enrollment effects
  require capacity/cutoff transactions.
- Only the owning Teacher grades; score/feedback and evidence remain on Submission. GRADED
  does not automatically require a numeric score. Never infer an automatic-scoring engine.

## Boundaries, Validation, and Errors

- Controllers handle HTTP, validation triggers, principal extraction, and service calls. Services orchestrate authentication workflows, domain authorization, and necessary transactions. Repositories handle persistence, not authorization policy.
- Authentication owns refresh/reset/verification state; `user` owns User persistence. Reuse User access rather than duplicating repositories or separate authentication models per role.
- Keep authentication-specific JWT infrastructure in `features/auth/security/`, including `JwtAuthenticationFilter` and `JwtTokenProvider`, not a root-level security package. Only explicitly required application-wide security components may remain outside `auth`. Do not scaffold illustrative classes or empty layers automatically.
- Use explicit DTOs and backend input validation. Client-supplied identity, role, ownership, or participation/payment state must not become authoritative through binding.
- Use consistent centralized error handling and the eventual API contract. Do not expose stack traces, hashes, tokens, or secrets in errors or logs.
- Keep signing keys and environment configuration outside committed source. Use HTTPS in deployed environments.
- Browser token transport is unresolved. Do not default to long-lived credentials in localStorage; minimize JavaScript exposure. For cookie-based designs, address HttpOnly, Secure in production, SameSite, CORS, and CSRF together with deployment assumptions.

## Decisions Requiring Clarification

Recheck the source documents before relying on this list:

- Password complexity, email-verification lifetime/resend limits, exact browser
  transport/cookie settings, concurrent sessions, rotation/replay details, need further
  definition where absent; reset/change invalidation is already settled.
- Exact password encoder parameters, JWT signing configuration, account unlock workflows,
  API errors, and deployment secret management are not fixed by this skill.
- Exact onboarding-review/profile contracts, remaining workflow edges, provider
  authentication/identifier scope, Storage access contracts and unrelated Admin overrides
  remain gated. Calendar uses a system/organization account, not per-user OAuth; no Meet
  API.

Request the missing decision before implementing dependent behavior; continue independent work when possible. Do not add MFA, social login, SSO, token blacklists, Redis, or other scope/infrastructure merely to fill a gap.

## Security Testing and Output

For the affected behavior, trace tests to requirement/rule/use-case IDs. Use service/policy tests for rules, API tests for actual enforcement, repository tests for important persistence guarantees, and backend integration tests for critical workflows.

Cover relevant success and rejection paths: registration privilege escalation;
unverified/locked/unapproved-Teacher accounts; invalid or expired JWTs; expired/revoked
refresh state; approved rotation and logout behavior; wrong-role and cross-owner requests;
forged payment/Enrollment state and unauthorized participation/Meet access; invalid/used
recovery credentials; and sensitive-data leakage. Test backend behavior directly rather than
relying on hidden UI controls. Fake email/payment providers; no real delivery or payment is
needed.

Report applicable rules, enforcement points, tests performed or planned, and unresolved decisions. Keep output proportional to the task and preserve the approved scope.
