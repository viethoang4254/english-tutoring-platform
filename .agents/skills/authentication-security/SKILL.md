---
name: authentication-security
description: Use when designing, implementing, reviewing, or testing authentication, Spring Security, JWT and refresh tokens, password handling, account security, or backend role, ownership, and Premium authorization for the English Learning Platform.
---

# Authentication and Security

## Context and Scope

Follow `AGENTS.md` and the project's requirements-first development order. This skill does not authorize implementation before relevant requirements and designs are defined.

Read relevant sections of these repository-root documents for the task:

- `docs/PROJECT_SPEC.md`: scope and actors.
- `docs/REQUIREMENTS.md`: `FR-STU-*`, `FR-ACC-*`, `FR-AUTH-*`, `NFR-SEC-*`, and affected feature requirements.
- `docs/BUSINESS_RULES.md`: `BR-AUTHN-001` through `BR-AUTHN-023`, `BR-ROLE-*`, `BR-AUTH-*`, and applicable ownership/subscription rules.
- `docs/USE_CASES.md`: affected `UC-AUTH-*` or protected feature workflow.
- `docs/DOMAIN_MODEL.md`: `DM-USER-*`, `DM-AUTH-*`, and affected resource boundaries.
- `docs/ARCHITECTURE.md`: sections 9-14, 38-49, and relevant feature boundaries.

Use `requirements-analysis` for missing business decisions, `english-learning-domain` for domain interpretation, and `system-design` for architecture changes. Documents remain marked Draft; distinguish explicitly documented defaults from unresolved details. Report conflicting statements with their sources and request clarification before dependent work. Do not silently select an interpretation or invent permissions.

## Authentication and Account State

- Use the documented email/password, Spring Security, JWT Access Token, and Refresh Token architecture with one shared User identity model.
- Public registration creates STUDENT + STANDARD + PENDING_VERIFICATION; clients cannot select privileged roles. Teacher provisioning requires an authorized Admin; Admin provisioning requires an authorized administrative/system-setup workflow.
- Apply `BR-AUTHN-005`: pending Students have only permitted verification/account actions, not authenticated learning that persists data. Verification activates an eligible account; it must not override an administrative restriction.
- Check current account eligibility during authentication, refresh, and protected access. LOCKED accounts cannot use protected application functionality; DISABLED accounts cannot authenticate or use it. Stale client/token state must not bypass these restrictions.
- Resolve current-user information from authenticated backend identity, not a client-selected user ID.

## JWT and Refresh Lifecycle

- Validate JWT signature and expiration before establishing authenticated identity. Use supported security mechanisms, never custom cryptography.
- Keep claims minimal; exclude passwords, hashes, reset/verification credentials, and secrets. JWT claims do not permanently establish mutable ownership or Premium entitlement.
- Preserve configurable defaults: Access Token **15 minutes**, Refresh Token **7 days**, password-reset credential **15 minutes** (`BR-AUTHN-007`, `BR-AUTHN-008`, `BR-AUTHN-016`).
- Validate server-side refresh state, expiration, revocation, and account/session eligibility before issuing credentials. Invalid, expired, or revoked refresh credentials cannot mint an Access Token.
- Support backend-controlled rotation and invalidation of replaced credentials according to the approved security design. Do not invent replay, concurrency, or session-limit policies.
- Logout revokes applicable refresh/session state and clears applicable client cookies/state. Frontend deletion alone is insufficient; do not claim this automatically revokes every issued Access Token.
- Keep refresh/reset/verification state tied to its User. Avoid unnecessary persistence of raw sensitive credentials; final persistence details belong to database/security design.

## Passwords and Recovery

- Use an appropriate Spring Security `PasswordEncoder`; never store plaintext, expose hashes, or implement custom hashing. Do not select an undocumented algorithm, cost, or password-complexity policy as an approved requirement.
- Follow documented eligibility and security checks for password changes. Apply session invalidation only according to the approved design.
- Reset credentials must be unpredictable, purpose-specific, account-bound, expiring, and unusable after successful consumption. Validate these conditions before changing the password.
- Verification credentials must be purpose-specific, account-bound, expiring, and consumed according to the documented verification flow.
- Forgot-password responses avoid unnecessary account-existence disclosure. Provider failures must not expose recovery credentials.

## Authorization and Ownership

- Keep authentication, role authorization, and subscription entitlement separate. Roles are STUDENT, TEACHER, ADMIN; never create ROLE_STANDARD or ROLE_PREMIUM.
- Spring Security handles authentication/coarse role checks; backend services or reusable policies enforce account state, ownership, enrollment, Course accessibility, and entitlement as applicable. UI guards are not security boundaries.
- Students cannot access Teacher/Admin management; Teachers cannot access Admin-only operations. Do not infer unrestricted Admin overrides from the Admin role.
- Teacher Course changes require ownership. Lesson and exercise authorization follows the parent Course. Teacher analytics are scoped to owned Courses; platform revenue is Admin-only.
- Scope personal profiles, saved vocabulary, and learning records to the authenticated Student for personal workflows. Any broader access requires a documented permission.
- Resolve Premium from authoritative subscription state, not client flags or a long-lived JWT claim alone. Teacher/Admin functions do not require Premium.
- Preserve basic Standard learning and Vocabulary Search. Apply only documented Premium restrictions; expiration restricts Premium access without deleting enrollment, progress, attempts, or saved vocabulary.
- Payment success and persisted scores require trusted backend verification; neither client claims nor a success page grant privileges.

## Boundaries, Validation, and Errors

- Controllers handle HTTP, validation triggers, principal extraction, and service calls. Services orchestrate authentication workflows, domain authorization, and necessary transactions. Repositories handle persistence, not authorization policy.
- Authentication owns refresh/reset/verification state; `user` owns User persistence. Reuse User access rather than duplicating repositories or separate authentication models per role.
- Keep authentication-specific JWT infrastructure in `features/auth/security/`, including `JwtAuthenticationFilter` and `JwtTokenProvider`, not a root-level security package. Only explicitly required application-wide security components may remain outside `auth`. Do not scaffold illustrative classes or empty layers automatically.
- Use explicit DTOs and backend input validation. Client-supplied identity, role, ownership, or entitlement must not become authoritative through binding.
- Use consistent centralized error handling and the eventual API contract. Do not expose stack traces, hashes, tokens, or secrets in errors or logs.
- Keep signing keys and environment configuration outside committed source. Use HTTPS in deployed environments.
- Browser token transport is unresolved. Do not default to long-lived credentials in localStorage; minimize JavaScript exposure. For cookie-based designs, address HttpOnly, Secure in production, SameSite, CORS, and CSRF together with deployment assumptions.

## Decisions Requiring Clarification

Recheck the source documents before relying on this list:

- Password complexity, email-verification lifetime/resend limits, exact browser transport/cookie settings, concurrent sessions, rotation/replay details, and session invalidation after password change/reset need further definition where absent.
- Exact password encoder parameters, JWT signing configuration, account unlock/enable workflows, API errors, and deployment secret management are not fixed by this skill.

Request the missing decision before implementing dependent behavior; continue independent work when possible. Do not add MFA, social login, SSO, token blacklists, Redis, or other scope/infrastructure merely to fill a gap.

## Security Testing and Output

For the affected behavior, trace tests to requirement/rule/use-case IDs. Use service/policy tests for rules, API tests for actual enforcement, repository tests for important persistence guarantees, and backend integration tests for critical workflows.

Cover relevant success and rejection paths: registration privilege escalation; pending/locked/disabled accounts; invalid or expired JWTs; expired/revoked refresh state; approved rotation and logout behavior; wrong-role and cross-owner requests; forged Premium state and expiration; invalid/used recovery credentials; and sensitive-data leakage. Test backend behavior directly rather than relying on hidden UI controls. Fake email/payment providers; no real delivery or payment is needed.

Report applicable rules, enforcement points, tests performed or planned, and unresolved decisions. Keep output proportional to the task and preserve the approved scope.
