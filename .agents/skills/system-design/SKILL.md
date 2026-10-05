---
name: system-design
description: Use when designing or reviewing the architecture, modules, boundaries, data flow, backend/frontend responsibilities, external integrations, authorization architecture, code organization, or technical structure of the English Tutoring Platform.
---

# System Design

## Purpose

Use this skill when translating approved English Tutoring Platform requirements into technical system design.

Do not use architecture to invent new business requirements.

Architecture must serve the approved requirements and Business Rules.

---

# Required Context

Current AGENTS.md is authoritative during the tutoring-scope transition.
Conflicting legacy business rules in other documents are superseded; consult
reconciled requirements or compatible technical/security guidance. Report remaining
ambiguities rather than treating historical plans as current approval.
Preserve completed TASK-001/TASK-002/TASK-003 infrastructure and verification evidence.
This skill does not authorize schema changes, implementation or unrelated edits.

Before making major system-design decisions, consult:

1. `AGENTS.md`
2. `docs/PROJECT_SPEC.md`
3. `docs/REQUIREMENTS.md`
4. `docs/BUSINESS_RULES.md`

When available, also consult:

5. `docs/USE_CASES.md`
6. `docs/ARCHITECTURE.md`
7. existing architecture documentation

Use `english-tutoring-domain` when domain interpretation is required.

Use `requirements-analysis` when requirements are incomplete.

Use `authentication-security` when authentication, JWT, authorization,
account security, or protected API behavior is involved.

---

# Architecture Goal

Prefer the simplest maintainable architecture that supports the approved graduation-project scope.

Current intended direction:

Frontend:

- Next.js
- TypeScript

Backend:

- Java
- Spring Boot
- Spring Security

Database:

- PostgreSQL, with Supabase-hosted development PostgreSQL

Authentication:

- JWT Access Token
- Refresh Token

VietQR is the direct-to-Teacher payment direction; Google Calendar uses a system/
organization account with Session event mapping. Exact integration contracts remain
gated. Supabase Storage is approved for bounded file uses. Google Meet remains one
externally created, manually supplied protected Course URL, not a Meet API integration.

---

# Default Architecture Style

Prefer a modular monolithic backend unless project requirements demonstrate a clear need for distributed services.

Conceptually:

```text
Browser
   ↓
Next.js
   ↓
Spring Boot API
   ↓
PostgreSQL
```

Spring Boot may communicate with external providers only under separately approved
integration decisions. Keep core business state backend-authoritative.

Do not introduce microservices by default.

Do not create separate deployable services merely because the project
contains multiple business modules.

---

# Feature-Based Architecture

The project uses feature-based organization.

Code should primarily be organized around business capabilities rather than
placing all unrelated features into global technical-layer directories.

Boundary candidates include auth/user, teacher, course, session/scheduling,
enrollment, payment, assignment, review, category and calendar integration.
These are candidates inside the modular monolith, not required packages, tables,
separate services or authorization to implement them.

Prefer high cohesion within a feature and low coupling between features.

---

# Backend Feature Architecture

Prefer:

```text
course/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── mapper/
```

Group business capabilities rather than placing unrelated features into global
controller/service/repository directories.

Each feature may contain only the layers it actually needs.

Possible feature layers include:

- controller
- service
- repository
- entity
- dto
- mapper
- provider
- exception

Do not create empty directories merely for structural symmetry.

---

# Backend Package Direction

Preserve existing technical packages and feature-based organization.
Authentication-specific security belongs under `features/auth/security/`.
Use the Course example only when implementation needs those layers.
New business boundaries are candidates, not a scaffold or database design.
Do not rename `com.englishlearning` merely because the product title changed.

---

# Layer Responsibilities

## Controller

Controllers primarily:

- receive HTTP requests
- trigger request validation
- invoke application services
- return HTTP responses

Conceptually:

```text
HTTP Request
     ↓
Controller
     ↓
Service
```

Controllers should remain thin.

Do not place large business workflows inside controllers.

Do not directly implement persistence logic inside controllers.

---

## Service

Services primarily handle:

- use-case orchestration
- business rules
- authorization-related domain checks where appropriate
- transaction boundaries where appropriate
- repository interaction
- provider interaction
- coordination between related domain operations

Conceptually:

```text
Controller
    ↓
 Service
  ↙     ↘
Repository Provider
```

Avoid creating giant services responsible for unrelated features.

---

## Repository

Repositories primarily handle persistence access.

Conceptually:

```text
Service
   ↓
Repository
   ↓
PostgreSQL
```

Repositories should not contain unrelated application workflows.

Do not create multiple repositories representing the same persistence concept
simply because several features need that data.

---

## Entity

Entities represent persisted domain data where appropriate.

Do not expose persistence entities directly as public API contracts by default.

Entity design belongs to detailed domain/database design.

---

## DTO

DTOs represent API/application boundaries.

Prefer explicit request and response DTOs.

Example:

```text
dto/
├── request/
│   ├── CreateCourseRequest.java
│   └── UpdateCourseRequest.java
│
└── response/
    └── CourseResponse.java
```

Do not use request DTOs as persistence entities.

---

## Mapper

Mappers may translate between:

- Entity
- Request DTO
- Response DTO
- internal application representations

Use mappers where they improve clarity.

Do not introduce unnecessary mapping abstractions for trivial cases.

---

## Provider

Provider abstractions isolate external systems from core application logic.

Examples:

- a payment adapter, if the eventual method requires one
- a calendar adapter, if the eventual integration requires one

Vendor-specific code should remain behind appropriate provider boundaries.

---

# Shared Components

Do not duplicate shared entities, repositories, or infrastructure across features.

For example:

```text
auth/AuthService
        ↓
user/UserRepository
```

is preferable to:

```text
auth/AuthUserRepository
user/UserRepository
payment/PaymentUserRepository
```

when all represent the same User persistence concept.

Authentication-specific security belongs in `features/auth/security/`.
Only genuinely application-wide security components explicitly required by the
architecture may remain outside `auth`; do not move components merely for naming.
Other shared infrastructure may remain in:

```text
common/
```

Feature-specific business logic should remain inside the owning feature.

---

# Dependency Direction

Avoid unnecessary circular dependencies between features.

A feature must not freely reach into internal implementation details of every
other feature.

Prefer clear application/service boundaries where useful.

Example:

```text
auth
 ↓
user
```

Auth may use appropriate User functionality rather than duplicate User logic.

If two features become tightly coupled, review the domain boundary before
adding more cross-dependencies.

---

# Frontend Responsibilities

Next.js should primarily handle:

- presentation
- responsive UI
- navigation
- user interaction
- form handling
- client-side state where appropriate
- communication with backend APIs

The frontend must not be the sole authority for:

- authentication validity
- authorization
- Enrollment-based participation access
- Course ownership
- payment verification
- persisted submission/result integrity
- protected business rules

The browser is an untrusted client for protected business state.

---

# Frontend Feature Architecture

The Next.js frontend should use feature-oriented organization where practical.

Conceptually:

```text
src/
├── app/
│   ├── (public)/
│   ├── (auth)/
│   ├── student/
│   ├── teacher/
│   └── admin/
│
├── features/
│   ├── auth/
│   ├── course/
│   ├── progress/
│   ├── review/
│   ├── payment/
│   └── analytics/
│
├── components/
│   └── ui/
│
├── lib/
├── hooks/
├── types/
└── config/
```

Route organization and feature implementation organization are separate concerns.

The `app/` directory primarily represents routing/layout structure.

The `features/` directory primarily contains feature implementation.

---

# Frontend Feature Structure

A feature may contain:

```text
features/course/
├── api/
├── components/
├── hooks/
├── schemas/
└── types/
```

Another feature may require fewer layers.

Do not require every frontend feature to contain every directory.

Shared reusable UI should remain outside feature-specific directories where
appropriate.

Prefer:

- high cohesion within features
- low coupling between features
- reusable shared UI
- clear API boundaries
- minimal duplicated client logic

---

# Backend Responsibilities

Spring Boot should be authoritative for:

- authentication
- authorization
- account status
- role enforcement
- Course ownership
- Enrollment-based participation access
- business rules
- enrollment rules
- learning submissions
- progress updates
- payment verification
- protected data access

Do not trust frontend claims for protected business state.

---

# Database Responsibilities

Supabase hosts PostgreSQL; Spring Boot remains the authoritative application backend.
Next.js uses the Spring Boot REST API for core data; Spring Boot accesses the database
through Spring Data JPA / Hibernate. PostgreSQL nodes in diagrams refer to this database.
Keep the schema portable PostgreSQL where practical and domain concepts application-owned.

Follow current AGENTS.md and compatible infrastructure decisions for Supabase
service boundaries: retain Spring
Security + JWT in `features/auth/security/`; do not introduce Supabase Auth, duplicate
application users into it, or add direct frontend database access or other Supabase
services without explicit approval and documented requirements. RLS does not replace
Spring Boot authorization. Use secure environment-based connections, never hardcoded
secrets. Preserve verified Session Pooler/TLS configuration and local PostgreSQL
support. Do not enable Supabase Data API, Auth, frontend supabase-js or RLS as
application authorization. Preserve TASK-003 evidence; no reconnection or schema
mutation is required by the domain change.

Persistence must follow the later approved database design. Domain concepts do
not mandate tables; do not generate schema from the legacy design or infer a new
table count. No new entities, migrations or database objects are authorized here.

---

# Authentication and Authorization

The approved authentication architecture is JWT-based authentication.

The backend uses:

- Spring Boot
- Spring Security
- JWT Access Token
- Refresh Token

The architecture must support:

- STUDENT
- TEACHER
- ADMIN

Payment and Enrollment state are separate from role authorization.

Authentication answers:

> Who is this user?

Authorization answers:

> What is this user allowed to do?

Participation authorization asks:

> Does this Student have the required valid Enrollment for this resource?

Keep these concerns separate.

---

# JWT Authentication Architecture

Conceptually:

```text
Next.js
   │
   │ Login
   ▼
Spring Boot
   │
   ▼
Spring Security
   │
   ├── JWT Access Token
   │
   └── Refresh Token
```

Spring Boot is authoritative for authentication.

Do not trust authentication state supplied by the frontend.

---

# Authentication Request Flow

Protected requests should conceptually follow:

```text
Browser
   │
   │ authenticated request
   ▼
Spring Security
   │
   ▼
JWT Validation
   │
   ▼
Authentication Context
   │
   ▼
Authorization
   │
   ▼
Controller
   │
   ▼
Service
```

Invalid or expired authentication must not reach protected business operations
as an authenticated user.

---

# Access Token

The Access Token uses JWT.

The Access Token should:

- be signed by the backend
- have a short lifetime
- identify the authenticated user
- contain only necessary authentication/authorization claims

Potential claims include:

- subject / user identifier
- role
- issued-at time
- expiration time

The Access Token must not contain:

- plaintext passwords
- password hashes
- reset tokens
- email-verification tokens
- API secrets
- unnecessary sensitive personal information

Mutable ownership, account eligibility and Enrollment access must not be treated
as permanently authoritative solely because a JWT contains a claim.

---

# Refresh Token

The authentication architecture must support Refresh Tokens.

Refresh Tokens should:

- live longer than Access Tokens
- expire
- support revocation
- support rotation according to the approved security design
- become invalid when required by security-sensitive account events

Use the approved hashed refresh_sessions persistence; no access JWT table.
Reset revokes all refresh sessions; logged-in password change revokes other sessions
while preserving the current one. Transport/replay details remain gated.

Conceptual flow:

```text
Access Token expires
        ↓
Client requests refresh
        ↓
Backend validates Refresh Token
        ↓
Backend verifies account/session state
        ↓
Backend issues new Access Token
        ↓
Refresh Token may be rotated
```

An expired, invalid, revoked, or otherwise unusable Refresh Token must not
produce a valid Access Token.

---

# Browser Authentication Security

Do not store long-lived authentication credentials in `localStorage` by default.

Browser token transport remains unresolved. If cookies are selected, evaluate
their security properties together; this skill does not select transport.

Security design must consider:

- HttpOnly
- Secure in production
- SameSite
- CORS
- CSRF protection

Do not assume HttpOnly cookies alone solve all browser security concerns.

Final cookie configuration depends on frontend/backend deployment architecture.

---

# Authentication Feature Boundary

Authentication and authentication-related security belong to `features/auth/`.

Conceptually:

```text
features/auth/
├── controller/
│   └── AuthController
│
├── service/
│   ├── AuthService
│   └── AuthServiceImpl
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest
│   │   ├── LoginRequest
│   │   ├── ForgotPasswordRequest
│   │   └── ResetPasswordRequest
│   │
│   └── response/
│       └── AuthResponse
│
├── repository/
├── exception/
└── security/
    ├── JwtAuthenticationFilter
    ├── JwtTokenProvider
    └── other authentication-specific security components
```

Do not place JWT/authentication-specific components in a root-level security package.

User persistence remains owned by the User domain.

Conceptually:

```text
auth/AuthService
       │
       ▼
user/UserRepository
```

Do not duplicate User persistence inside Auth.

---

# Public Registration

Each User has exactly one role: STUDENT, TEACHER or ADMIN. Student and Teacher registration
are separate; V1 has no Student-to-Teacher promotion. Teacher business authority requires
TEACHER role, verified email, an unlocked account and approved onboarding. Application
snapshots/history are retained, with at most one PENDING application per Teacher; the
current public TeacherProfile is created after approval and does not rewrite application
snapshots.

Admin provisioning remains an authorized administrative/system concern.

---

# Password Security

Never store plaintext passwords.

Use an appropriate Spring Security `PasswordEncoder`.

Do not:

- implement custom password hashing
- implement custom cryptographic algorithms
- log plaintext passwords
- expose password hashes
- place passwords inside JWTs

---

# Authentication Capabilities

The initial authentication architecture should support:

- Student and Teacher registration subject to approved workflows
- Login
- Logout
- Current User
- Email Verification
- Change Password
- Forgot Password
- Reset Password
- Access Token
- Refresh Token
- account status
- role authorization

Do not automatically add:

- Google Login
- Facebook Login
- OAuth providers
- passkeys
- MFA
- enterprise SSO

without explicit approval.

---

# Account Status

V1 uses users.locked as its account-blocking mechanism: a locked account cannot authenticate
or use normal account functionality. There is no separate disabled, enabled or
account_status field/lifecycle. Email uses lowercase(trim(inputEmail)) for storage/login; a verified email
change retains the old email until successful verification of the new one. Password reset
revokes all refresh sessions; logged-in password change revokes other sessions while
preserving the current session. Raw refresh, verification and reset secrets are not
persisted.

Do not invent an AccountStatus enum, disabled or enabled column. Apply current
eligibility during authentication, refresh and protected access.

---

# Email Verification

Email-verification credentials must:

- expire
- be purpose-specific
- be associated with the intended account
- become invalid after successful verification where appropriate

Do not expose verification credentials through unrelated APIs or logs.

---

# Password Reset

Password-reset credentials must:

- be unpredictable
- expire
- be single-purpose
- be associated with the intended account
- become invalid after successful use

Forgot Password responses should avoid unnecessary disclosure of whether a
particular email address exists.

---

# Logout Architecture

Logout should invalidate the relevant Refresh Token or authentication session
state where applicable.

Applicable authentication cookies should be cleared.

Do not assume deleting frontend state alone is sufficient logout behavior.

---

# Authorization

Authorization checks may include:

- authenticated user
- role
- resource ownership
- account state
- valid Enrollment where required
- Course accessibility

Important authorization must be enforced by Spring Boot.

Frontend route guards and hidden UI elements are UX mechanisms.

They are not security boundaries.

---

# Role Authorization

Conceptually:

```text
/student/**
→ STUDENT

/teacher/**
→ TEACHER

/admin/**
→ ADMIN
```

URL-level role checks alone are insufficient for resource-level authorization.

---

# Course Ownership

Teacher-owned Course operations must verify ownership on the backend.

Never design authorization equivalent to:

> "Button is hidden, therefore Teacher cannot modify the Course."

The backend must independently validate the action.

Conceptually:

```text
Authenticated
AND
Role == TEACHER
AND
Email verified, unlocked, approved onboarding
AND
Course belongs to authenticated Teacher
```

A Teacher must not modify another Teacher's Course unless requirements
explicitly permit it.

Traverse Session -> Course and Assignment -> Session -> Course for child resources.
Apply this boundary to submissions, results, enrolled-Student information, progress
and related payment data; do not infer arbitrary Admin overrides.

---

# Enrollment Authorization

Enrollment is unique per Student/Course and uses PENDING, ACTIVE, COMPLETED or CANCELLED. A
COMPLETED Enrollment is historical and cannot simply re-enroll into that Course instance.
Free Courses activate participation without fake Payments. Paid participation may reserve a
seat while PENDING. Capacity counts ACTIVE plus PENDING Enrollments with unexpired
reservations; min_students counts ACTIVE only. Expired reservations consume no capacity. No
new Enrollment or Payment may begin after the first Session has started. Activation,
reservation and late-payment handling must be concurrency-safe.

Enforce backend resource/Student authorization; claims and browser success grant no access.

---

# Tutoring Participation Architecture

Assignments belong to Sessions and use ACTIVE/CANCELLED. Any Submission prevents hard
deletion of its Assignment. There is one current Submission per Student/Assignment, using
DRAFT/SUBMITTED/GRADED; no revision-history, result or grading table is introduced. Score,
feedback and grading metadata remain on Submission. Only the owning Teacher grades, and a
score cannot exceed Assignment max_score. A numeric score is not made mandatory merely by
GRADED status.

Supabase Storage holds Teacher avatars, Course thumbnails, Assignment files, Submission
files and refund proof. PostgreSQL stores paths/references and applicable metadata, never
file bytes, base64 or temporary signed URLs. Resolve each path within an explicitly
configured bucket for its usage; exact bucket identifiers remain configuration, and the
path/bucket mapping must be fixed before integration. Spring Boot authorizes access; Storage
does not replace backend business authorization.

---

# Review Architecture

Participant feedback is unique per Enrollment, can be created only for a COMPLETED
Enrollment and has a rating from 1 to 5. No aggregate rating column is stored.
Editing/moderation details not supplied by these decisions remain open.

Do not reuse retired vocabulary-practice models.

---

# Progress Architecture

Derive authorized progress from authoritative participation/submission information.
No progress or statistics tables; formulas remain open and browser claims are not results.

---

# Scheduling and Meeting Boundaries

Recurring weekly Course rules generate concrete Sessions before publication. Session
statuses are SCHEDULED and CANCELLED only. Rescheduling updates the same Session and appends
old/new times to schedule history. Cancellation preserves the row and session_number,
optionally records a reason and synchronizes cancellation to its Calendar event.
V1 has no replacement or automatic make-up Sessions; cancellation never regenerates
the schedule or changes the fixed planned session_count. Session content is nullable
protected learning content, never public preview content. V1 has no attendance tracking.

One manual protected Course Meet URL serves all Sessions; no Meet API or Meeting entity.

Google Calendar uses one system/organization account and one configured Calendar,
not per-user OAuth token storage. The Calendar ID belongs to backend configuration/secrets,
not Session rows. Each concrete Session maps to one event; Session remains authoritative.
Synchronize publication, rescheduling and cancellation through durable status/retry;
external failure must not roll back core Course/Session changes. Attendee emails derive
from Users and Enrollments without duplicated email or attendee tables; verified email
changes update relevant future attendees. UNIQUE(session_id, provider) and non-null
(provider, external_event_id) uniqueness apply within V1's single-calendar boundary.
Multi-calendar support requires a future migration. Calendar never creates Meet URLs.

---

# Payment Architecture

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

Conceptual provider fields and identifier scopes require verification or omission
before executable migration. Do not fabricate provider API contracts.

---

# Payment Feature Boundary

Payment-specific logic belongs primarily to the `payment` feature.

Conceptually:

```text
payment/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── provider/
```

If a provider is selected, its payloads should not spread through core business
logic. The example does not approve a provider integration.

---

# Payment Provider Boundary

Isolate verified VietQR/provider payloads from core payment/Enrollment logic.
Conceptual provider fields are not verified API contracts; do not scaffold an
adapter or resolve unknown authenticity/identifier details without evidence.

---

# Payment and Enrollment Separation

Payment records the relevant financial transaction; Enrollment governs Course
participation. Successful confirmation may activate/validate the corresponding
Enrollment, but these remain distinct concepts. Use approved Payment/Enrollment
states and retained history; exact edge-transition contracts remain gated.

---

# Analytics Architecture

Analytics should be derived from authoritative platform data.

Student analytics:

- personal learning

Teacher analytics:

- owned Courses

Admin analytics:

- platform-wide

Admin analytics may include:

- total users
- total Teachers
- total Students
- Courses
- enrollments
- transactions
- authorized transaction statistics

Do not label Teacher Course payments as Admin revenue. Admin is not the Course
owner, payment recipient or default Assignment grader; overrides remain unresolved.

Avoid storing redundant aggregate data unless there is a demonstrated need.

For project-scale data, calculate reasonable aggregates from transactional data
where practical.

---

# Responsive Web Architecture

The project is one responsive web platform.

Student:

- mobile-first

Teacher:

- desktop-optimized and responsive

Admin:

- desktop-optimized and responsive

Do not create separate native applications for initial scope.

Student functionality must remain usable from smartphone browsers.

---

# External Integration Boundaries

Isolate approved external interactions from core logic and map vendor-specific
payloads into internal representations where needed. Provider failure must not
corrupt core state. Payment and Calendar implementations remain undecided; a
manually supplied Course Meet URL does not require an external API adapter.

---

# API Design Direction

Design REST APIs around approved use cases, not database-table CRUD.
Exact routes and DTOs belong to reconciled API design. Historical endpoint counts
and legacy business routes do not constrain the tutoring model.

---

# API DTO Boundary

Public API requests and responses should use explicit DTOs where appropriate.

Conceptually:

```text
HTTP Request
     ↓
Request DTO
     ↓
Controller
     ↓
Service
     ↓
Domain / Persistence
     ↓
Response DTO
     ↓
HTTP Response
```

Do not expose database entities directly merely for convenience.

---

# Transaction Boundaries

Operations that update multiple related pieces of persistent state should
consider transactional consistency.

Potential examples include:

- Student enrollment
- approved Assignment submission
- verified payment processing
- approved Enrollment activation
- password-reset completion

Exact transaction boundaries belong to detailed implementation design.

---

# Error Handling

Architecture should account for failures such as:

- invalid input
- unauthenticated access
- unauthorized access
- resource ownership violations
- missing resources
- locked account or ineligible Teacher onboarding
- participation access denied
- approved external integration failure
- payment failure
- database failure
- invalid or expired authentication token

Do not expose sensitive internal errors directly to end users.

---

# Common Error Handling

Shared API error handling may belong under:

```text
common/
└── exception/
```

Prefer consistent API error responses.

Do not duplicate identical exception-handling logic in every feature.

Do not expose:

- stack traces
- database credentials
- JWT secrets
- password hashes
- Refresh Tokens
- verification credentials
- internal provider secrets

---

# Security Design

System design must consider:

- secure password storage
- authentication
- JWT validation
- Refresh Token handling
- account status
- authorization
- role enforcement
- resource ownership
- Enrollment-based participation access
- validation
- secret management
- protected API endpoints
- payment verification
- CORS
- CSRF where relevant

Never store secrets directly in committed source code.

Never implement custom cryptographic algorithms.

---

# Security Logging

Never log:

- plaintext passwords
- password hashes
- Access Tokens unnecessarily
- Refresh Tokens
- password-reset credentials
- email-verification credentials
- API secrets

Safe security events may be logged where appropriate, such as:

- successful login
- failed login
- password changed
- account locked

subject to privacy and security requirements.

---

# Performance

Optimize for realistic graduation-project scale.

Prioritize:

- sensible database queries
- pagination for large management lists
- efficient approved discovery queries
- avoiding obvious N+1 query problems
- avoiding unnecessary repeated external API calls

Do not introduce distributed caching or complex infrastructure without evidence
that it is needed.

Do not automatically introduce:

- Redis
- Kafka
- RabbitMQ
- Elasticsearch
- Kubernetes

for hypothetical future scale.

---

# Maintainability

Prefer:

- feature-based organization
- clear module boundaries
- explicit business rules
- small controllers
- focused services
- reusable domain/application services
- provider abstractions
- DTO boundaries
- documented APIs
- testable business logic
- clear dependency direction

Avoid:

- giant controllers
- god services
- business logic inside UI components
- duplicated authorization logic
- duplicated repositories representing the same domain data
- provider-specific logic spread throughout the system
- circular feature dependencies
- premature infrastructure complexity

---

# Testing Direction

Architecture should allow important business logic to be tested without requiring
full browser execution.

Important areas include:

- Student and Teacher registration subject to approved workflows
- authentication
- JWT validation
- Refresh Token behavior
- authorization
- Course ownership
- enrollment
- progress
- payment verification

Detailed testing strategy belongs to testing design.

---

# Design Decision Procedure

For a technical design task:

## 1. Identify Requirements

Reference relevant requirement IDs.

Do not design from assumptions when requirements already exist.

## 2. Identify Business Rules

Reference applicable Business Rules.

## 3. Identify Actor

Determine whether the action belongs to:

- Student
- Teacher
- Admin
- System
- external provider

## 4. Identify Owning Feature

Determine which feature owns the primary responsibility.

Examples:

- auth
- course
- session/scheduling
- assignment
- payment

Avoid placing functionality in `common` merely because ownership is unclear.

## 5. Identify Domain Concepts

Determine affected domain entities/capabilities.

## 6. Identify Trust Boundary

Determine which decisions must be authoritative on the backend.

Examples:

- authentication
- authorization
- Course ownership
- Enrollment-based participation access
- persisted results
- payment verification

## 7. Identify Data Flow

Describe how information moves through:

```text
Browser
→ Frontend
→ Backend
→ Database / external provider
```

## 8. Identify Authorization

Determine whether the operation requires:

- authentication
- role check
- ownership check
- account-state check
- Enrollment-based participation access
- Course accessibility

## 9. Identify External Dependencies

Determine whether the feature depends on:

- an approved payment mechanism
- an approved Calendar mechanism

## 10. Evaluate Alternatives

For meaningful architecture decisions, compare reasonable alternatives.

Do not introduce complexity merely because an approach is common in large
production systems.

## 11. Prefer Simplicity

Choose the smallest maintainable design satisfying the requirements.

## 12. Identify Persistence Impact

Determine whether the design affects:

- entities
- relationships
- transactions
- history
- indexes
- persisted security state

Do not create schema prematurely during high-level design.

## 13. Identify Tests

Determine which business and security rules require testing.

## 14. Record Important Decisions

Important architecture decisions should eventually be documented in:

`docs/ARCHITECTURE.md`

## 15. Verify Scope

Do not add infrastructure or features merely because they might be useful later.

---

# Avoid Premature Decisions

Do not finalize the following until the project reaches the relevant design stage:

- exact database schema
- exact hosting providers for Next.js and Spring Boot
- exact deployment topology
- exact Calendar integration
- exact Payment Provider
- Redis
- Kafka
- RabbitMQ
- Kubernetes
- microservices
- Elasticsearch
- event-driven architecture

They may be introduced later only when justified.

JWT authentication itself is already an approved architecture direction and
is therefore not considered an undecided item.

The exact JWT implementation details may still be refined during detailed
authentication design.

---

# Architecture Consistency Rules

When modifying architecture:

1. Do not contradict `AGENTS.md`.
2. Do not contradict approved requirements.
3. Do not contradict Business Rules.
4. Do not silently change established architecture decisions.
5. Identify affected modules.
6. Identify affected APIs.
7. Identify affected persistence.
8. Identify affected authorization.
9. Identify affected frontend features.
10. Identify affected tests.

If an architecture change conflicts with an approved decision, explicitly
identify the conflict before proceeding.

---

# Expected Design Output

For significant system-design work, provide:

1. Objective
2. Related requirements
3. Related Business Rules
4. Actor
5. Owning feature/module
6. Proposed architecture
7. Components
8. Data flow
9. Authorization/trust boundaries
10. Persistence impact
11. External integrations
12. Important alternatives
13. Trade-offs
14. Security considerations
15. Testing impact
16. Open decisions
17. Impact on existing architecture

Keep smaller design tasks proportional to their complexity.
