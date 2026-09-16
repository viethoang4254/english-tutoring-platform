---
name: system-design
description: Use when designing or reviewing the architecture, modules, boundaries, data flow, backend/frontend responsibilities, external integrations, authorization architecture, code organization, or technical structure of the English Learning Platform.
---

# System Design

## Purpose

Use this skill when translating approved English Learning Platform requirements into technical system design.

Do not use architecture to invent new business requirements.

Architecture must serve the approved requirements and Business Rules.

---

# Required Context

Before making major system-design decisions, consult:

1. `AGENTS.md`
2. `docs/PROJECT_SPEC.md`
3. `docs/REQUIREMENTS.md`
4. `docs/BUSINESS_RULES.md`

When available, also consult:

5. `docs/USE_CASES.md`
6. `docs/ARCHITECTURE.md`
7. existing architecture documentation

Use `english-learning-domain` when domain interpretation is required.

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

- PostgreSQL

Authentication:

- JWT Access Token
- Refresh Token

External integrations may include:

- Dictionary Provider
- pronunciation/audio provider
- Text-to-Speech provider
- Payment Provider

Exact providers remain undecided.

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

Spring Boot may communicate with approved external providers.

Conceptually:

```text
                     Browser
                        │
                        ▼
                     Next.js
                        │
                        ▼
                   Spring Boot
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     PostgreSQL     Dictionary      Payment
                     Provider       Provider
                        │
                        ▼
                  Audio / TTS
                  when required
```

Do not introduce microservices by default.

Do not create separate deployable services merely because the project
contains multiple business modules.

---

# Feature-Based Architecture

The project uses feature-based organization.

Code should primarily be organized around business capabilities rather than
placing all unrelated features into global technical-layer directories.

Backend examples:

- auth
- user
- teacher
- course
- lesson
- vocabulary
- exercise
- learning
- progress
- review
- subscription
- payment
- analytics

These are modules/features inside the modular monolith.

They are not separate deployable microservices.

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

instead of organizing the entire application primarily as:

```text
controller/
├── CourseController
├── LessonController
├── VocabularyController
└── PaymentController

service/
├── CourseService
├── LessonService
├── VocabularyService
└── PaymentService

repository/
├── CourseRepository
├── LessonRepository
├── VocabularyRepository
└── PaymentRepository
```

Each feature may contain only the layers it actually needs.

Possible feature layers include:

- controller
- service
- repository
- entity
- dto
- mapper
- provider
- evaluator
- exception

Do not create empty directories merely for structural symmetry.

---

# Backend Package Direction

The intended conceptual Spring Boot package structure is:

```text
com.englishlearning
│
├── auth/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── exception/
│
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
│
├── teacher/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
│
├── course/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
│
├── lesson/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
│
├── vocabulary/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── mapper/
│   └── provider/
│
├── exercise/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── mapper/
│   └── evaluator/
│
├── learning/
│   ├── service/
│   ├── repository/
│   └── entity/
│
├── progress/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
│
├── review/
│   ├── controller/
│   ├── service/
│   └── dto/
│
├── subscription/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── payment/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── provider/
│
├── analytics/
│   ├── controller/
│   ├── service/
│   └── dto/
│
├── security/
│   ├── config/
│   ├── jwt/
│   └── filter/
│
└── common/
    ├── exception/
    ├── response/
    ├── validation/
    └── util/
```

This structure is conceptual.

Do not automatically create every directory shown above.

Only create a package/layer when implementation requires it.

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

- DictionaryProvider
- TextToSpeechProvider
- PaymentProvider

Vendor-specific code should remain behind appropriate provider boundaries.

---

## Evaluator

Exercise evaluation logic may use dedicated evaluator components when useful.

Examples:

```text
exercise/
└── evaluator/
    ├── FillWordEvaluator
    ├── ListeningEvaluator
    └── QuizEvaluator
```

Do not introduce evaluator abstractions unless they improve clarity or avoid
large conditional business logic.

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
subscription/SubscriptionUserRepository
```

when all represent the same User persistence concept.

Cross-cutting infrastructure belongs in shared modules such as:

```text
security/
```

and:

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
- Premium entitlement
- Course ownership
- payment verification
- score integrity
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
│   ├── lesson/
│   ├── vocabulary/
│   ├── exercise/
│   ├── progress/
│   ├── review/
│   ├── subscription/
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
- Premium entitlement
- business rules
- enrollment rules
- learning submissions
- scoring
- progress updates
- mastery calculation
- subscription state
- payment verification
- protected data access

Do not trust frontend claims for protected business state.

---

# Database Responsibilities

PostgreSQL should persist approved platform data such as:

- users
- roles
- Student information
- Teacher information
- Courses
- Lessons
- Vocabulary
- Vocabulary Senses
- Course enrollments
- exercises
- attempts
- answers
- learning progress
- Saved Vocabulary
- subscriptions
- transactions
- authentication-related persistent state when required

The exact schema belongs to database-design work.

Do not prematurely create tables during high-level architecture design.

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

STANDARD/PREMIUM is separate from role authorization.

Do not model:

- ROLE_STANDARD
- ROLE_PREMIUM

Authentication answers:

> Who is this user?

Authorization answers:

> What is this user allowed to do?

Premium entitlement answers:

> Does this Student currently have Premium access?

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

Premium state should not be considered permanently authoritative solely because
a JWT contains a Premium-related claim.

---

# Refresh Token

The authentication architecture must support Refresh Tokens.

Refresh Tokens should:

- live longer than Access Tokens
- expire
- support revocation
- support rotation where practical
- become invalid when required by security-sensitive account events

The exact Refresh Token persistence model belongs to detailed Auth and
database design.

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

For this web application, prefer secure cookie-based token transport/storage
where appropriate.

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

Authentication-related use cases belong primarily to the `auth` feature.

Conceptually:

```text
auth/
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
└── exception/
```

JWT infrastructure belongs to the shared `security` module rather than being
duplicated inside Auth.

Conceptually:

```text
security/
├── config/
│   └── SecurityConfig
│
├── jwt/
│   └── JwtService
│
└── filter/
    └── JwtAuthenticationFilter
```

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

Public registration is intended for Students.

A public user must not be able to register directly as:

- TEACHER
- ADMIN

A newly registered Student conceptually receives:

```text
Role: STUDENT
Access Tier: STANDARD
```

Teacher and Admin account provisioning must follow authorized project workflows.

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

- Student registration
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

Authentication architecture should support appropriate account states.

Initial concepts include:

- PENDING_VERIFICATION
- ACTIVE
- LOCKED
- DISABLED

Backend authentication and authorization must consider account status.

A disabled account must not gain access merely because stale frontend state
claims the account is active.

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
- active Premium entitlement
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
Course belongs to authenticated Teacher
```

A Teacher must not modify another Teacher's Course unless requirements
explicitly permit it.

---

# Premium Authorization

Premium is not a security role.

Premium functionality should conceptually verify:

```text
Authenticated
AND
Role == STUDENT
AND
Premium entitlement is active
```

Premium state must originate from authoritative backend data.

Do not trust:

```text
premium=true
```

supplied by the browser.

Do not use a long-lived JWT Premium claim as the sole source of truth for
subscription entitlement.

---

# Student Learning Architecture

Student learning should support the conceptual flow:

```text
Course
→ Lesson
→ Vocabulary
→ Exercise
→ Attempt
→ Answer
→ Result
→ Progress
→ Mastery
→ Review
```

Design data flow so that exercise results can contribute to progress and
vocabulary-level learning information where required.

---

# Exercise Submission

Exercise submission should conceptually follow:

```text
Student
   ↓
Submit answers
   ↓
Backend validates authentication
   ↓
Backend validates Course/Lesson access
   ↓
Backend evaluates answers
   ↓
Backend stores attempt/results
   ↓
Backend updates applicable learning information
   ↓
Response returned to Student
```

Do not rely on the browser as the trusted scorer for persisted results.

---

# Exercise Feature Boundary

Exercise-specific implementation belongs primarily to the `exercise` feature.

Potential structure:

```text
exercise/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── evaluator/
```

Exercise evaluation may vary by exercise type.

Core exercise concepts currently include:

- Fill Word
- Listening
- Quiz

Do not introduce major exercise categories without approved requirements.

---

# Vocabulary Architecture

Vocabulary is shared platform data.

Avoid architecture where every Lesson unnecessarily stores independent copies
of the same vocabulary information.

Support:

```text
Vocabulary
→ Vocabulary Sense
```

and:

```text
Lesson
→ selected Vocabulary/Sense
```

Exact relationship modeling belongs to database design.

Vocabulary should remain reusable across Lessons and Teachers where appropriate.

---

# Vocabulary Feature Boundary

Vocabulary-specific implementation belongs primarily to the `vocabulary`
feature.

Conceptually:

```text
vocabulary/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
└── provider/
```

Provider-specific dictionary integration should not leak throughout the
Vocabulary domain.

---

# Dictionary Integration

External Dictionary access should be isolated behind an application abstraction.

Conceptually:

```text
VocabularyService
       ↓
DictionaryProvider
       ↓
Provider Implementation
       ↓
External Dictionary API
```

Do not couple the entire application directly to one provider's response structure.

Provider replacement should not require redesigning the core Vocabulary domain.

Map external responses into internal application representations.

---

# Dictionary Licensing Boundary

Architecture must not assume that external dictionary content may automatically
be stored permanently.

Before deciding to cache, persist, redistribute, or serve provider content,
consider provider licensing.

This applies especially to:

- definitions
- example sentences
- pronunciation audio
- provider-specific metadata

The final provider and storage strategy remain separate architecture decisions.

---

# Audio Architecture

Do not assume all vocabulary has audio.

The design must allow audio availability to vary.

Possible sources may include:

- Dictionary Provider
- approved stored audio
- Text-to-Speech

The final strategy depends on licensing and provider decisions.

Listening functionality must define appropriate behavior when required audio
is unavailable.

---

# Review Architecture

Review may use learning information such as:

- Weak Vocabulary
- Saved Vocabulary
- incorrectly answered vocabulary
- recently learned vocabulary

The initial architecture does not require a complex spaced-repetition engine.

Do not introduce complex SRS infrastructure without approved requirements.

---

# Progress Architecture

Progress should derive from authoritative learning activity.

Potential concepts include:

- Lesson progress
- Course progress
- exercise scores
- accuracy
- attempts
- vocabulary performance
- mastery
- Weak Vocabulary
- Learning History

Do not allow the frontend to directly overwrite authoritative progress state.

---

# Payment Architecture

Payment must use trusted backend verification.

Conceptually:

```text
Student
   ↓
Checkout
   ↓
Payment Provider
   ↓
Trusted verification / callback
   ↓
Spring Boot
   ↓
Transaction
   ↓
Subscription
```

Do not activate Premium from a frontend redirect alone.

Design payment integration behind a provider abstraction when practical.

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

Provider-specific payment payloads should not spread throughout Subscription
or User logic.

---

# Payment Provider Boundary

Conceptually:

```text
PaymentService
      ↓
PaymentProvider
      ↓
External Payment Provider
```

The core application should consume internal payment representations rather
than depend everywhere on vendor-specific payloads.

---

# Subscription Architecture

Subscription state should be distinct from Student role.

Conceptually:

```text
User
→ Student
→ Subscription
→ Plan / Entitlement
```

The system must be able to determine whether Premium is currently active.

Historical learning data must not depend on an active subscription for existence.

When Premium expires, approved learning history should remain intact according
to Business Rules.

---

# Subscription and Payment Separation

Subscription answers:

> What access entitlement does this Student currently have?

Payment answers:

> What verified financial transaction occurred?

Do not make these the same domain concept.

A successful verified payment may cause subscription activation or extension,
but transaction history and subscription state remain distinct concerns.

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
- active subscriptions
- transactions
- revenue over time

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

External services should be isolated from core business logic.

Potential abstractions:

```text
DictionaryProvider
TextToSpeechProvider
PaymentProvider
```

Core domain code should not depend heavily on vendor-specific payloads.

Map external data into internal application/domain representations.

External provider failure must not automatically corrupt core platform state.

---

# API Design Direction

API endpoints should represent application capabilities clearly.

Do not design APIs solely as direct CRUD wrappers around database tables.

Examples of capability-oriented endpoints may include:

```text
POST /api/auth/login
POST /api/auth/refresh
POST /api/courses/{courseId}/enroll
POST /api/exercises/{exerciseId}/attempts
POST /api/subscriptions/checkout
```

These examples are conceptual.

Exact endpoint definitions belong to API-design work.

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
- exercise submission
- verified payment processing
- subscription activation
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
- inactive/disabled account
- Premium restriction
- external Dictionary failure
- missing audio
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
- Premium entitlement
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
- account disabled

subject to privacy and security requirements.

---

# Performance

Optimize for realistic graduation-project scale.

Prioritize:

- sensible database queries
- pagination for large management lists
- efficient Vocabulary Search
- reasonable audio delivery
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

- Student registration
- authentication
- JWT validation
- Refresh Token behavior
- authorization
- Course ownership
- Premium access
- enrollment
- exercise evaluation
- scoring
- progress
- mastery
- subscription
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
- vocabulary
- exercise
- subscription
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
- Premium entitlement
- score
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
- Premium entitlement
- Course accessibility

## 9. Identify External Dependencies

Determine whether the feature depends on:

- Dictionary
- audio
- TTS
- payment

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
- exact cloud provider
- exact deployment topology
- exact Dictionary Provider
- exact Text-to-Speech Provider
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
