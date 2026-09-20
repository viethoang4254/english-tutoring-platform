# English Learning Platform --- System Architecture

**Version:** 0.1\
**Status:** Draft\
**Project Type:** Graduation Project

---

# 1. Purpose

This document defines the technical architecture of the English Learning
Platform.

It translates the approved requirements, business rules, use cases, and
domain model into a maintainable implementation structure without
defining the final database schema or every API endpoint.

The architecture is intentionally simple enough for a graduation project
while remaining production-oriented and extensible.

---

# 2. Architecture Goals

The system should:

- support Student, Teacher, and Admin web experiences
- provide a mobile-first Student experience without requiring a native
  application
- keep business logic in the backend
- use clear feature boundaries
- support JWT-based authentication
- enforce authorization server-side
- support reusable vocabulary and learning history
- integrate external Dictionary, Audio/TTS, Email, and Payment
  providers through adapters
- remain easy to test and demonstrate
- avoid unnecessary microservices and infrastructure
- allow Codex/agents to work on one feature without loading unrelated
  project context

---

# 3. Selected Technology Direction

## Frontend

- Next.js
- TypeScript
- responsive web design
- mobile-first Student interface
- desktop-oriented Teacher/Admin management interfaces

## Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- Bean Validation
- REST API

## Database

- Supabase-hosted PostgreSQL (managed database hosting)

## Initial Architecture Style

```text
Responsive Web Client
Next.js + TypeScript
        │
        │ HTTPS / JSON REST API
        ▼
Spring Boot Modular Monolith
        │
        ├── Identity & Access
        ├── Course
        ├── Lesson
        ├── Vocabulary
        ├── Exercise
        ├── Learning
        ├── Review
        ├── Subscription
        ├── Payment
        ├── Teacher
        └── Admin
        │
        ▼
PostgreSQL
```

PostgreSQL nodes in the architecture diagrams denote Supabase-hosted PostgreSQL.
Spring Boot accesses it through Spring Data JPA / Hibernate.

External providers connect through backend integration adapters.

---

# 4. Architecture Decision --- Modular Monolith

The initial system shall use a **modular monolith**.

This means:

- one primary Spring Boot backend application
- one PostgreSQL database
- feature-oriented modules/packages
- clear internal boundaries
- no distributed microservice communication for core features

## Why

The project does not currently require independent service scaling,
distributed transactions, service discovery, message brokers, or
operational complexity associated with microservices.

A modular monolith is sufficient for:

- graduation-project scope
- maintainability
- testing
- deployment
- future extraction of modules if genuinely required

Do not introduce microservices without an approved requirement.

---

# 5. High-Level System Context

```text
┌────────────────────────────────────────────────────┐
│                    Web Browser                     │
│                                                    │
│ Student: mobile-first                             │
│ Teacher: responsive desktop-first                 │
│ Admin: responsive desktop-first                   │
└───────────────────────┬────────────────────────────┘
                        │ HTTPS
                        ▼
┌────────────────────────────────────────────────────┐
│                  Next.js Frontend                  │
│                                                    │
│ UI / Forms / Client State / API Client            │
└───────────────────────┬────────────────────────────┘
                        │ REST / JSON
                        ▼
┌────────────────────────────────────────────────────┐
│                Spring Boot Backend                 │
│                                                    │
│ Controller → Application/Service → Repository     │
│                  ↓                                 │
│            Domain / Policies                       │
└───────────────┬───────────────────┬────────────────┘
                │                   │
                ▼                   ▼
       ┌────────────────┐   ┌──────────────────────┐
       │   PostgreSQL   │   │ External Providers   │
       │                │   │ Dictionary           │
       │ domain data    │   │ Audio / TTS          │
       │ learning data  │   │ Email                │
       │ commercial     │   │ Payment              │
       └────────────────┘   └──────────────────────┘
```

---

# 6. Repository Structure

Recommended project repository:

```text
english-learning-platform/
│
├── AGENTS.md
├── README.md
│
├── docs/
│   ├── PROJECT_SPEC.md
│   ├── REQUIREMENTS.md
│   ├── BUSINESS_RULES.md
│   ├── USE_CASES.md
│   ├── DOMAIN_MODEL.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   ├── UI_UX.md
│   └── API_DESIGN.md
│
├── backend/
│
├── frontend/
│
└── tasks/
```

`tasks/` should be created after database/API/UI design is sufficiently
stable.

---

# 7. Backend Architecture

The backend should use **feature-based packaging**, not one global
package per technical layer.

Recommended conceptual structure:

```text
backend/
└── src/
    ├── main/
    │   ├── java/com/example/englishlearning/
    │   │   ├── EnglishLearningApplication.java
    │   │   │
    │   │   ├── features/auth/
    │   │   ├── user/
    │   │   ├── course/
    │   │   ├── lesson/
    │   │   ├── vocabulary/
    │   │   ├── exercise/
    │   │   ├── learning/
    │   │   ├── review/
    │   │   ├── subscription/
    │   │   ├── payment/
    │   │   ├── teacher/
    │   │   ├── admin/
    │   │   ├── integration/
    │   │   └── common/
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │
    └── test/
        └── java/com/example/englishlearning/
```

The exact Java base package may be selected when the project is
initialized.

---

# 8. Feature Module Structure

A feature may contain only the layers it actually needs.

Typical structure:

```text
feature/
├── controller/
├── dto/
├── service/
├── repository/
├── entity/
├── mapper/
└── exception/
```

Do not create empty folders only for visual symmetry.

For example:

```text
course/
├── controller/
│   └── CourseController.java
├── dto/
│   ├── request/
│   └── response/
├── service/
│   └── CourseService.java
├── repository/
│   └── CourseRepository.java
├── entity/
│   └── Course.java
└── mapper/
    └── CourseMapper.java
```

This is preferable to:

```text
controller/
├── AuthController
├── CourseController
├── LessonController
├── PaymentController
└── ...

service/
├── AuthService
├── CourseService
├── LessonService
└── ...
```

because feature-based packaging keeps related code and agent context
together.

---

# 9. Backend Layer Responsibilities

## Controller

Responsible for:

- HTTP request/response handling
- request validation trigger
- authenticated principal extraction
- calling application/service logic
- HTTP status mapping

Controller must not contain substantial business logic.

---

## DTO

Responsible for API input/output contracts.

Rules:

- do not expose JPA entities directly
- separate request and response DTOs where useful
- validate external input
- never accept authoritative fields such as role, payment success,
  ownership, score, or Premium entitlement merely because the client
  sends them

---

## Service / Application Logic

Responsible for:

- use-case orchestration
- business-rule enforcement
- authorization requiring domain/resource information
- transaction boundaries
- coordinating repositories and integrations

Examples:

```text
AuthService
CourseService
VocabularyService
LearningService
SubscriptionService
```

Avoid giant services containing unrelated features.

---

## Repository

Responsible for persistence access.

Repositories should:

- expose domain-relevant queries
- avoid embedding business decisions
- be scoped to their feature/domain

Example:

```text
UserRepository
CourseRepository
VocabularyRepository
EnrollmentRepository
ExerciseAttemptRepository
```

---

## Entity / Domain

Responsible for persistent domain state and local invariants appropriate
to the implementation.

Database/JPA-specific details are deferred to `DATABASE_DESIGN.md`.

---

## Mapper

Responsible for explicit conversion where entity ↔ DTO mapping would
otherwise clutter controllers/services.

Do not add a mapping framework unless it clearly reduces complexity.

---

# 10. Identity and Authentication Architecture

Authentication establishes identity; authorization determines permitted actions.
STUDENT, TEACHER, and ADMIN are authorization roles; STANDARD and PREMIUM are
Student subscription/access tiers, never roles. Refresh Token denotes the credential;
RefreshSession denotes its server-side state.

Authentication uses:

```text
Email + Password
      ↓
Spring Security
      ↓
JWT Access Token
+
Refresh Token backed by server-side RefreshSession state
```

Initial defaults from Business Rules:

```text
Access Token  = 15 minutes
Refresh Token = 7 days
Password-reset credential = 15 minutes
```

These values must be configuration-driven.

---

# 11. Authentication Flow

```text
POST Login
    │
    ▼
validate credentials
    │
    ▼
validate AccountStatus
    │
    ▼
create Access Token
    │
    ├── short-lived JWT
    │
    ▼
create Refresh Session
    │
    ▼
return authenticated result
```

Refresh:

```text
Refresh credential
      ↓
validate server-side refresh state
      ↓
rotate refresh state when applicable under the approved security design
      ↓
new Access Token
+
replacement Refresh Token when rotated
```

Rotation support follows `BR-AUTHN-010`; exact rotation timing, replay/concurrency
handling, and broader session-invalidation policies remain unresolved.

Logout:

```text
Logout
  ↓
revoke applicable Refresh Session
  ↓
client clears local authentication state
```

---

# 12. JWT Responsibilities

JWT should contain only information required for
authentication/authorization decisions that is appropriate for a
short-lived credential.

Potential claims:

- subject/user identifier
- role
- issued time
- expiration time
- token identifier where required

Do not use JWT as the permanent source of:

- Premium entitlement
- payment state
- current account ownership
- mutable learning progress
- revenue information

Premium entitlement must be checked from authoritative backend state
when required.

---

# 13. Authentication Package

Recommended structure:

```text
features/auth/
├── controller/
│   └── AuthController.java
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   └── ResetPasswordRequest.java
│   │
│   └── response/
│       ├── AuthResponse.java
│       └── CurrentUserResponse.java
│
├── service/
│   ├── AuthService.java
│   ├── TokenService.java
│   ├── EmailVerificationService.java
│   └── PasswordResetService.java
│
├── repository/
│   ├── RefreshSessionRepository.java
│   ├── EmailVerificationRepository.java
│   └── PasswordResetRepository.java
│
├── entity/
│   ├── RefreshSession.java
│   ├── EmailVerification.java
│   └── PasswordReset.java
│
└── security/
    ├── SecurityConfig.java
    ├── JwtAuthenticationFilter.java
    ├── JwtTokenProvider.java
    ├── CustomUserDetailsService.java
    └── SecurityUserPrincipal.java
```

Authentication-specific security belongs in `features/auth/security/`, not a
root-level security package. Only explicitly required application-wide security
components that are not authentication-specific may remain outside `auth`.

Exact class names may be refined during implementation.

`UserRepository` belongs to the `user` feature rather than being
duplicated inside `auth`.

---

# 14. User Module

Conceptual structure:

```text
user/
├── controller/
├── dto/
├── service/
├── repository/
└── entity/
```

Responsibilities include:

- account/profile data
- role
- account status
- user lookup
- permitted account management

Authentication-specific refresh/reset/verification state remains in
`auth`.

---

# 15. Course Module

Responsibilities:

- Course creation
- Course update
- Course ownership
- CEFR association
- lifecycle status
- Standard/Premium classification data
- Course discovery/query behavior

Teacher ownership must be enforced in backend service/security logic.

---

# 16. Lesson Module

Responsibilities:

- Lesson creation/update
- Course association
- topic/order
- authorization derived from parent Course ownership

Lesson should not independently redefine Teacher ownership.

---

# 17. Vocabulary Module

Responsibilities:

- shared Vocabulary
- VocabularySense
- LessonVocabulary association
- pronunciation metadata
- vocabulary search
- Teacher vocabulary selection
- Dictionary integration coordination

Conceptual structure:

```text
vocabulary/
├── controller/
├── dto/
├── service/
├── repository/
├── entity/
└── mapper/
```

External Dictionary-specific code should not be embedded directly into
core entities.

---

# 18. Exercise Module

Responsibilities:

- Fill Word
- Listening
- Quiz
- ExerciseQuestion
- Teacher exercise configuration
- trusted answer definition

The exercise module defines content.

Student attempts/results belong primarily to `learning`.

---

# 19. Learning Module

Responsibilities:

- Enrollment
- ExerciseAttempt
- AnswerRecord
- score calculation
- accuracy
- Lesson progress
- Course progress
- VocabularyPerformance
- mastery state
- learning history

Conceptual structure:

```text
learning/
├── controller/
├── dto/
├── service/
├── repository/
├── entity/
└── calculator/
```

Possible calculators/policies:

```text
ScoreCalculator
MasteryCalculator
ProgressCalculator
```

These should remain small and testable.

---

# 20. Review Module

The initial Review feature should reuse learning data.

Responsibilities:

- select vocabulary from approved Review sources
- coordinate review practice
- apply Standard/Premium access rules
- send submitted review answers through trusted learning evaluation

Do not implement a complex spaced-repetition engine unless requirements
change.

---

# 21. Subscription Module

Responsibilities:

- SubscriptionPlan
- Subscription
- current Premium entitlement
- Monthly/Yearly plan logic
- activation/extension after verified payment
- expiration behavior

Premium check should be centralized rather than duplicated across
controllers.

Example conceptual service:

```text
PremiumAccessService
```

Possible operation:

```text
hasActivePremium(userId)
```

---

# 22. Payment Module

Responsibilities:

- payment initiation coordination
- provider transaction references
- trusted payment verification
- PaymentTransaction persistence
- successful payment → Subscription activation/extension

The frontend must never directly activate Premium.

Conceptual flow:

```text
Student
   ↓
Backend creates payment
   ↓
Payment Provider
   ↓
trusted callback / verification
   ↓
PaymentService
   ↓
verified PaymentTransaction
   ↓
SubscriptionService
   ↓
Premium entitlement
```

Exact flow depends on the selected provider.

---

# 23. Teacher Module

`teacher` should not duplicate Course/Lesson/Vocabulary business
entities.

It primarily represents Teacher-specific application queries/use cases
such as:

- Teacher dashboard
- owned Course summaries
- Course analytics
- Teacher-specific management orchestration

Core Course operations remain in `course`.

---

# 24. Admin Module

`admin` should provide Admin-specific application workflows without
duplicating core entities.

Responsibilities include:

- user oversight
- Teacher provisioning
- Course oversight
- Course Standard/Premium classification workflow
- subscription statistics
- transaction monitoring
- system analytics
- revenue analytics

Admin services may query multiple modules through clear
application/service boundaries.

---

# 25. Common Module

`common/` must remain small.

Appropriate shared concerns:

```text
common/
├── exception/
├── response/
├── validation/
├── config/
└── util/
```

Do not move feature-specific business logic into `common` merely because
multiple classes use it.

A large `common` package is a warning that module boundaries are
becoming unclear.

---

# 26. Integration Architecture

External systems must be accessed through backend adapters.

Recommended conceptual structure:

```text
integration/
├── dictionary/
│   ├── DictionaryClient.java
│   └── ...
│
├── audio/
│   ├── AudioProvider.java
│   └── ...
│
├── email/
│   ├── EmailSender.java
│   └── ...
│
└── payment/
    ├── PaymentGateway.java
    └── ...
```

Core services should depend on internal interfaces/contracts rather than
provider-specific HTTP details wherever practical.

This allows the provider to change without rewriting core learning
logic.

---

# 27. Dictionary Integration Boundary

Conceptual flow:

```text
Teacher
   ↓
VocabularyService
   ↓
DictionaryProvider interface
   ↓
Provider Adapter
   ↓
External Dictionary API
```

The adapter is responsible for translating provider data into an
internal provider-neutral representation.

Before persisting external content, the system must follow approved
licensing/storage rules.

---

# 28. Audio Architecture

Vocabulary pronunciation audio may be provided through:

1.  approved Dictionary audio
2.  licensed stored audio
3.  approved TTS fallback

The exact strategy remains open.

Core UI/backend code should therefore use an abstraction such as an
audio reference rather than hardcoding a specific provider into
Vocabulary logic.

---

# 29. Email Architecture

Email is required for:

- email verification
- password recovery

Conceptual flow:

```text
AuthService
    ↓
EmailSender interface
    ↓
Email Provider Adapter
    ↓
External Email Provider
```

Email delivery failure must not expose secrets/tokens in logs.

---

# 30. Frontend Architecture

Recommended Next.js structure:

```text
frontend/
├── src/
│   ├── app/
│   │   ├── (public)/
│   │   ├── (auth)/
│   │   ├── (student)/
│   │   ├── teacher/
│   │   └── admin/
│   │
│   ├── features/
│   │   ├── auth/
│   │   ├── course/
│   │   ├── lesson/
│   │   ├── vocabulary/
│   │   ├── exercise/
│   │   ├── learning/
│   │   ├── review/
│   │   ├── subscription/
│   │   ├── teacher/
│   │   └── admin/
│   │
│   ├── components/
│   │   └── shared/
│   │
│   ├── lib/
│   │   ├── api/
│   │   ├── auth/
│   │   └── validation/
│   │
│   ├── hooks/
│   └── types/
│
└── public/
```

Exact App Router route-group names may be refined during UI design.

---

# 31. Frontend Feature Structure

Example:

```text
features/auth/
├── api/
├── components/
├── hooks/
├── schemas/
└── types/
```

Example:

```text
features/vocabulary/
├── api/
├── components/
├── hooks/
└── types/
```

Shared UI primitives belong in shared components.

Feature-specific components should remain inside their feature.

---

# 32. Frontend Responsibility

Frontend is responsible for:

- rendering UI
- responsive layouts
- form interaction
- client-side validation for usability
- calling backend APIs
- displaying backend validation/errors
- non-authoritative UI state

Frontend is **not** authoritative for:

- role
- ownership
- Premium entitlement
- score
- correct answers
- payment success
- revenue
- account security state

Backend always revalidates protected operations.

---

# 33. Student Web Experience

Student UI must be mobile-first.

Primary navigation should prioritize:

- Home / learning dashboard
- Courses
- Search Vocabulary
- My Vocabulary
- Review
- Progress
- Account / Premium

Learning screens should support touch interaction and small displays.

No native mobile application is required.

---

# 34. Teacher Web Experience

Teacher UI should prioritize desktop content-management workflows while
remaining responsive.

Primary areas:

- Dashboard
- My Courses
- Lessons
- Vocabulary selection
- Exercise management
- Course analytics
- Account

---

# 35. Admin Web Experience

Admin UI should prioritize desktop dashboard/management workflows while
remaining responsive.

Primary areas:

- Dashboard
- Users
- Teachers
- Courses
- Subscriptions
- Transactions
- System statistics
- Revenue statistics

---

# 36. API Architecture

The frontend communicates with the backend through versioned REST-style
endpoints.

Recommended base path:

```text
/api/v1
```

Examples are conceptual only:

```text
/api/v1/auth/...
/api/v1/courses/...
/api/v1/vocabulary/...
/api/v1/learning/...
```

Exact endpoints belong to `API_DESIGN.md`.

---

# 37. API Response Principles

APIs should:

- use consistent JSON contracts
- return appropriate HTTP status codes
- provide structured validation errors
- avoid leaking stack traces/internal implementation
- avoid returning secrets
- avoid exposing JPA entities directly

A consistent error model should be defined before implementation.

---

# 38. Validation Strategy

Validation occurs at multiple levels.

```text
Frontend validation
      ↓ usability only
Backend DTO validation
      ↓
Business-rule validation
      ↓
Database constraints
```

Frontend validation never replaces backend validation.

Examples:

- required fields → DTO validation
- Teacher owns Course → service authorization/business validation
- unique email → service + database constraint
- Student/Course enrollment uniqueness → business check + database
  constraint
- LessonVocabulary sense integrity → service/domain + database design
  where possible

---

# 39. Authorization Strategy

Authorization has two levels.

## Role-Level

Examples:

```text
STUDENT → Student learning endpoints
TEACHER → Teacher management endpoints
ADMIN   → Admin management endpoints
```

Spring Security may enforce coarse role-level access.

## Resource-Level

Examples:

```text
Teacher may edit Course only if:
course.teacherId == authenticatedUser.id
```

```text
Student may access Premium feature only if:
PremiumAccessService.hasActivePremium(studentId)
```

Resource-level authorization belongs in trusted backend
application/service logic or reusable authorization policies.

---

# 40. Transaction Strategy

Use database transactions around operations that must remain consistent.

Examples:

```text
Student submits exercise
    ↓
create ExerciseAttempt
create AnswerRecords
calculate result
update VocabularyPerformance
update Progress
```

and:

```text
verified successful payment
    ↓
persist/update PaymentTransaction
activate/extend Subscription
```

These operations should not leave partially applied business state.

Exact transaction boundaries are finalized during implementation design.

---

# 41. Persistence Strategy

Spring Boot accesses Supabase-hosted PostgreSQL through Spring Data JPA / Hibernate.
Keep the schema portable PostgreSQL where practical; database hosting does not
change application-owned domain concepts or finalize table names.

Rules:

- repositories belong to relevant features
- JPA entities are not API contracts
- use database constraints for important data integrity
- use migrations rather than relying on uncontrolled schema generation
  in deployed environments
- avoid unnecessary eager loading
- prevent N+1 query problems in important list/detail workflows

Exact tables/indexes/constraints belong to `DATABASE_DESIGN.md`.

---

# 42. Database Migration Strategy

Use a migration tool when database implementation begins.

Preferred direction:

```text
Flyway
```

Migrations should be version-controlled with the backend.

Conceptual location:

```text
backend/src/main/resources/db/migration/
```

Do not depend on `ddl-auto=create` or destructive automatic schema
recreation for deployed environments.

---

# 43. Security Principles

The system must:

- hash passwords using a modern password encoder supported by Spring
  Security
- never store plaintext passwords
- never log passwords or raw sensitive tokens
- keep secrets outside source code
- validate JWT signature and expiration
- validate current account eligibility
- enforce role and resource authorization
- verify payment results server-side
- validate external input
- use HTTPS in deployed environments
- configure CORS deliberately
- avoid exposing unnecessary internal error details

Exact production secret-management strategy belongs to deployment
design.

---

# 44. Token Storage Direction

Access Token should remain short-lived.

Refresh authentication state must support server-side
revocation/rotation.

The exact browser transport/storage mechanism should be finalized during
API/security implementation.

For a browser-based platform, prefer a design that minimizes exposure of
long-lived refresh credentials to JavaScript.

Do not permanently place sensitive long-lived credentials in insecure
browser storage merely for convenience.

---

# 45. Error Handling

Backend should use centralized exception handling.

Conceptual structure:

```text
common/exception/
├── GlobalExceptionHandler
├── ResourceNotFoundException
├── BusinessRuleException
├── ForbiddenOperationException
└── ...
```

Do not create one exception class for every trivial validation
condition.

Error responses should be stable enough for the frontend to handle
consistently.

---

# 46. Logging

Logging should support debugging and auditability without leaking
sensitive information.

Log useful events such as:

- authentication failures at an appropriate level
- administrative account changes
- payment verification outcomes
- unexpected provider failures

Do not log:

- plaintext passwords
- raw password-reset credentials
- raw email-verification credentials
- full sensitive authentication tokens
- unnecessary payment secrets

---

# 47. Testing Architecture

Testing should be layered.

## Unit Tests

Best for:

- score calculation
- mastery calculation
- progress calculation
- subscription renewal rules
- authorization policies
- service business logic

## Repository Tests

Best for:

- important persistence queries
- uniqueness/integrity behavior

## Controller / API Tests

Best for:

- request validation
- authentication
- authorization
- HTTP contracts

## Integration Tests

Best for important end-to-end backend workflows such as:

```text
Register → Verify → Login
Enroll → Learn → Submit Exercise → Progress
Verified Payment → Premium Activation
```

Frontend should test important user flows and components without
attempting to duplicate all backend business-rule tests.

---

# 48. External Provider Testing

External providers should be wrapped so tests can use fakes/mocks.

Examples:

```text
DictionaryProvider
EmailSender
PaymentGateway
AudioProvider
```

Core tests must not require real payments or real external email
delivery.

---

# 49. Configuration

Environment-specific configuration must remain outside source-code
constants.

Examples:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET / signing configuration
ACCESS_TOKEN_TTL
REFRESH_TOKEN_TTL
EMAIL_PROVIDER credentials
DICTIONARY_API credentials
PAYMENT_PROVIDER credentials
FRONTEND_ORIGIN
```

Use environment variables and Spring/Next.js configuration conventions.

Do not commit real secrets.

---

# 50. Deployment Boundary

Deployment is not fully designed yet.

The approved deployment direction is:

```text
User Browser
      ↓
Next.js
      ↓ REST API
Spring Boot
      ↓ Spring Data JPA / Hibernate
Supabase-hosted PostgreSQL
```

Supabase provides managed PostgreSQL hosting; Spring Boot remains the authoritative
backend for business logic, validation, authentication, authorization, ownership,
Course access, learning rules, scoring, Premium entitlement, subscriptions, and
payment verification. Next.js accesses core application data through the Spring
Boot REST API, not through direct frontend-to-Supabase database access.

Spring Security + JWT and `features/auth/security/` remain unchanged. Application
users and authentication state remain application-owned; do not duplicate users
into Supabase Auth.

Do not introduce Supabase Auth, Realtime, Edge Functions, Storage, other Supabase
services, or Supabase-specific business logic without a documented requirement and
explicit approval. RLS must not replace Spring Boot authorization.

Spring Boot connects securely using environment-based configuration as described
in section 49; never hardcode credentials, connection strings, or other secrets.
Hosting providers for Next.js and Spring Boot remain unresolved. Local PostgreSQL
may still be used for development or testing; deployed environments use Supabase.

External providers remain separate managed services/APIs.

Containerization may be used if useful, but Docker/Kubernetes must not
be introduced as a business requirement.

Kubernetes is unnecessary for the initial project unless later
explicitly justified.

---

# 51. Performance Direction

Initial performance strategy should remain simple:

- database indexes for important lookup/filter fields
- pagination for large Course/User/Transaction lists
- avoid N+1 queries
- avoid loading unnecessary large object graphs
- cache only when a measured need exists
- external API calls should have timeouts and controlled failure
  handling

Do not add Redis solely because it is common in production
architectures.

---

# 52. Scalability Direction

The initial modular monolith should be designed so major feature
boundaries remain recognizable.

If future scale requires extraction, candidates could theoretically
include:

- payment
- notifications/email
- analytics

This is not a requirement for the current project.

Do not design current implementation around hypothetical microservices.

---

# 53. Agent / Codex Context Strategy

Agents should not load every document for every task.

For an implementation task:

```text
1. Read AGENTS.md
2. Identify feature/task
3. Read referenced Requirement IDs
4. Read referenced Business Rule IDs
5. Read referenced Use Case
6. Read relevant Domain section
7. Read relevant Architecture section
8. Read relevant Database/API section
9. Read relevant Skill
10. Implement only the task scope
```

Example:

```text
Task: AUTH-LOGIN

Context:
AGENTS.md
FR-STU-002
FR-ACC-001..003
BR-AUTHN-007..009
UC-AUTH-LOGIN-01
DM-USER-001
DM-AUTH-001
Architecture sections 10–13
authentication-security/SKILL.md
```

The agent should not read Quiz, Payment, Revenue, or Teacher Analytics
documentation for this task unless a dependency actually requires it.

---

# 54. Dependency Direction

Avoid uncontrolled circular dependencies between feature modules.

Preferred conceptual direction:

```text
controller
   ↓
service/application
   ↓
domain/repository/integration contracts
```

Cross-feature calls should occur through clear services/interfaces
rather than reaching into another feature's controller.

Examples:

```text
PaymentService
    ↓
SubscriptionService
```

is preferable to Payment code directly manipulating arbitrary
Subscription internals.

Likewise:

```text
Teacher dashboard
    ↓
Course query/service
```

rather than duplicating Course persistence logic inside Teacher module.

---

# 55. Initial Module Dependency Map

```text
auth ─────────────► user

course ───────────► user
course ───────────► CEFR/reference data

lesson ───────────► course

vocabulary ───────► lesson (association use cases)
vocabulary ───────► integration.dictionary
vocabulary ───────► integration.audio

exercise ─────────► lesson
exercise ─────────► vocabulary

learning ─────────► course
learning ─────────► lesson
learning ─────────► exercise
learning ─────────► vocabulary
learning ─────────► user

review ───────────► learning
review ───────────► vocabulary

subscription ─────► user

payment ──────────► subscription
payment ──────────► integration.payment

teacher ──────────► course
teacher ──────────► learning

admin ────────────► user
admin ────────────► course
admin ────────────► subscription
admin ────────────► payment
admin ────────────► learning
```

This map is conceptual and may be refined to reduce coupling during
implementation.

---

# 56. Architecture Rules

## AR-001

Use a modular monolith for the initial backend.

## AR-002

Package backend code primarily by feature.

## AR-003

Do not expose JPA entities directly through the API.

## AR-004

Controllers must not contain core business logic.

## AR-005

Repositories must not determine business authorization.

## AR-006

Backend is authoritative for role, ownership, Premium, score,
correctness, payment success, and protected state.

## AR-007

External providers must be isolated behind integration boundaries.

## AR-008

Do not hardcode secrets or environment-specific configuration.

## AR-009

Use database migrations for schema evolution.

## AR-010

Do not introduce infrastructure without a demonstrated requirement.

## AR-011

Student frontend is mobile-first responsive web.

## AR-012

Teacher/Admin frontend remains responsive but is primarily optimized for
management workflows.

## AR-013

Feature-specific frontend code should remain near its feature.

## AR-014

Premium entitlement is resolved from authoritative subscription state,
not treated as a role.

## AR-015

Resource ownership authorization must be checked server-side.

## AR-016

Historical learning data must remain preserved according to Business
Rules.

## AR-017

Implementation tasks should load only relevant documented context.

---

# 57. Decisions Deferred to Later Documents

## Database Design

Will decide:

- PostgreSQL tables
- primary/foreign keys
- unique constraints
- indexes
- relationship mappings
- deletion strategy
- timestamps
- transaction persistence details

## UI/UX Design

Will decide:

- routes/pages
- navigation
- responsive layouts
- learning-screen flow
- Teacher/Admin dashboard layout
- form behavior

## API Design

Will decide:

- exact endpoints
- HTTP methods
- request DTOs
- response DTOs
- error response format
- pagination contracts
- authentication transport details

## Deployment Design

Will decide:

- hosting providers for Next.js and Spring Boot
- domain
- CI/CD
- environment separation
- production secrets
- observability
- backup strategy

---

# 58. Architecture Open Decisions

The following remain open and should not be invented during
implementation:

1.  Exact Java base package/group ID.
2.  Exact Next.js version at project initialization.
3.  Exact Spring Boot version at project initialization.
4.  Exact Dictionary Provider.
5.  Exact audio/TTS provider and storage model.
6.  Exact Email Provider.
7.  Exact Payment Provider.
8.  Exact browser refresh-token transport/storage mechanism.
9.  Exact production hosting providers for Next.js and Spring Boot.
10. Exact production secret-management mechanism.
11. Exact provider retry/rate-limit strategy.
12. Exact API error schema until API Design.
13. Exact database schema until Database Design.

---

# 59. Next Step

After this architecture is approved:

```text
Requirements             ✓
Business Rules           ✓
Use Cases                ✓
Domain Model             ✓
Architecture             ✓
        ↓
DATABASE DESIGN          ← NEXT
        ↓
UI/UX Design
        ↓
API Design
        ↓
Task Breakdown
        ↓
Implementation
        ↓
Testing
        ↓
Deployment
```

The next document should be:

```text
docs/DATABASE_DESIGN.md
```

Database design should translate the approved domain model into
PostgreSQL tables and constraints without changing business
requirements.
