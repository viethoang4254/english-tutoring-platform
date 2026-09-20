# English Learning Platform — Agent Instructions

## 1. Project Mission

Build a responsive web platform for learning English vocabulary.

The platform must support three primary roles:

- Student
- Teacher
- Admin

The project is a graduation project and should prioritize clear requirements, maintainable architecture, correctness, usability, security, testability, and demonstrable functionality over unnecessary complexity.

---

## 2. Development Stage

The project is currently in the Task Breakdown and Feature Status planning phase. Database and API design baselines are documented; application implementation has not begun.

Do not begin feature implementation until the relevant requirements and business rules have been defined.

Follow this development order:

1. Requirements
2. Business Rules
3. Use Cases
4. Domain Design
5. System Architecture
6. Database Design
7. UI/UX Design
8. API Design
9. Implementation
10. Testing
11. Deployment

Do not skip directly to implementation.

---

## 3. Source of Truth

Before making architectural or implementation decisions, consult:

- `docs/PROJECT_SPEC.md`
- `docs/REQUIREMENTS.md`
- `docs/BUSINESS_RULES.md`
- `docs/USE_CASES.md`
- `docs/ARCHITECTURE.md`

Some later-stage documents may not exist yet.

When documents conflict, do not silently choose one interpretation.

Identify the conflict and request clarification.

Do not invent business requirements that are not documented.

---

## 4. Platform

The product is a web application.

All roles must be able to use the platform through a modern web browser.

### Student

Student interfaces must be:

- responsive
- mobile-first
- touch-friendly
- convenient for learning on smartphones
- usable on tablets and desktop devices

Students must not be required to install a native mobile application.

### Teacher

Teacher interfaces must be:

- responsive
- optimized primarily for desktop management workflows
- still functional on mobile devices

### Admin

Admin interfaces must be:

- responsive
- optimized primarily for desktop dashboards and management
- still functional on mobile devices

---

## 5. Core Roles

### Student

Students consume learning content, enroll in Courses, learn vocabulary, complete exercises, review vocabulary, and track learning progress.

Students may have:

- Standard access
- Premium access

Standard and Premium are Student access tiers.

They are not system roles.

### Teacher

Teachers create and manage educational content.

A Teacher may own multiple Courses.

Teachers do not receive payments directly from Students in the current project scope.

Teacher functionality does not require Premium access.

### Admin

Admins manage the platform.

Admin responsibilities include:

- user management
- Teacher management
- Course oversight
- subscription management
- transaction monitoring
- system statistics
- revenue statistics

Admin functionality does not require Premium access.

### Role and Access Tier Separation

System roles are:

- STUDENT
- TEACHER
- ADMIN

Student access tiers are:

- STANDARD
- PREMIUM

Do not model Standard and Premium as security roles.

Do not create:

- ROLE_STANDARD
- ROLE_PREMIUM

Conceptually:

Student Role
→ STUDENT

Student Access
→ STANDARD or PREMIUM

---

## 6. Learning Structure

The primary learning hierarchy is:

CEFR Level → Course → Lesson → Vocabulary

Supported CEFR levels:

- A1
- A2
- B1
- B2
- C1
- C2

Lessons should primarily be organized by topic rather than by part of speech.

Part of speech is vocabulary metadata and may be used for filtering and practice.

---

## 7. Course Ownership

A Teacher may own multiple Courses.

A Course has one primary Teacher.

A Course belongs to a CEFR level.

Students enroll in Courses.

Course enrollment is used to track:

- Student Courses
- Course progress
- enrollment statistics
- learning activity

Course ownership must be enforced by the backend.

Teacher role alone is not sufficient to modify any Course.

For protected Teacher Course operations, the system must conceptually verify:

Authenticated User
→ Role == TEACHER
→ Teacher owns Course
→ Operation permitted

A Teacher must not modify another Teacher's Course unless future requirements explicitly allow it.

---

## 8. Vocabulary Domain

Vocabulary should be reusable across the platform and should not be unnecessarily duplicated for individual Teachers.

Vocabulary may contain:

- word
- pronunciation / IPA
- CEFR level
- audio pronunciation
- one or more senses

A vocabulary sense may contain:

- part of speech
- English definition
- Vietnamese meaning
- example sentence

A word may have multiple senses and multiple parts of speech.

Teachers should be able to select the appropriate sense when adding vocabulary to a Lesson.

Vocabulary data may originate from approved dictionary providers.

Do not assume that third-party dictionary content or audio may be permanently stored or redistributed unless the provider's license permits it.

External Dictionary Provider logic should eventually be isolated from the core Vocabulary domain.

Do not spread provider-specific response structures throughout the application.

---

## 9. Student Learning Flow

The core learning flow is:

Learn Vocabulary

→ Fill Word

→ Listening

→ Quiz

→ Result

→ Progress

→ Review

The exact ordering requirements may be refined by Business Rules.

Do not make the sequence permanently mandatory unless specified.

Exercise submissions and persisted results must be evaluated or verified by trusted backend logic.

Do not treat a score calculated only by the browser as authoritative persisted learning data.

---

## 10. Core Exercise Types

### Fill Word

Designed to practice spelling and vocabulary recall.

A question may provide:

- IPA
- English definition
- partially hidden word

The Student enters the vocabulary word.

### Listening

Designed to practice recognition of spoken vocabulary.

Supported concepts may include:

- listen and choose
- listen and type

Listening functionality must account for vocabulary that does not have playable audio.

### Quiz

May evaluate:

- word → definition
- definition → word
- IPA → word
- vocabulary in context

Additional exercise types require explicit approval before being added to the core scope.

---

## 11. Learning Progress

The system should support tracking concepts including:

- exercise score
- accuracy
- attempts
- Lesson progress
- Course progress
- vocabulary mastery
- weak vocabulary
- Learning History

Vocabulary-level performance should be retained where appropriate so that the platform can identify vocabulary that requires additional review.

Previous learning history must not be deleted merely because a Student repeats an exercise.

Learning data must remain independent from the current Premium subscription state where required by Business Rules.

---

## 12. Vocabulary Search

Vocabulary Search is a core feature.

Students should be able to:

- search vocabulary
- view vocabulary details
- listen to pronunciation
- save vocabulary
- access saved vocabulary through My Vocabulary

Saved vocabulary may later be used for review and practice.

Teachers use vocabulary search for a different purpose:

- search existing platform vocabulary
- retrieve vocabulary from an approved dictionary provider when appropriate
- select a vocabulary sense
- add vocabulary to a Lesson

Vocabulary Search must not automatically be treated as Premium-only.

---

## 13. Review

Review is part of the core learning system.

Review sources may include:

- weak vocabulary
- saved vocabulary
- incorrectly answered vocabulary
- recently learned vocabulary

Advanced spaced-repetition algorithms are not required for the initial version unless explicitly added later.

Do not introduce a complex SRS system without an approved requirement.

---

## 14. Standard and Premium

Student accounts may use Standard or Premium access.

Standard and Premium are entitlements/access tiers and must remain separate from security roles.

### Standard

Standard must provide meaningful learning value and must not exist only as a paywall demonstration.

### Premium

Premium should focus on deeper learning and additional learning capabilities.

Potential Premium capabilities include:

- advanced vocabulary information
- specialized vocabulary Courses
- advanced exercises
- advanced review
- weak-vocabulary practice
- detailed learning analytics

The final Standard/Premium feature matrix must be defined in Business Rules.

Do not assume that an entire CEFR level is Premium-only unless explicitly documented.

Premium access must be determined by trusted backend state.

Do not trust a `premium=true` value supplied by the frontend.

---

## 15. Subscription Model

Premium belongs to the platform rather than to an individual Teacher.

The initial subscription concept supports:

- Monthly
- Yearly

A Premium Student may access Premium content according to the active subscription.

When a subscription expires:

- the account remains active
- Learning History must not be deleted
- progress must not be deleted
- attempts must not be deleted where they form part of learning history
- Saved Vocabulary must not be deleted
- the Student returns to Standard access unless renewed

Premium entitlement must not be modeled by changing the Student's role.

Conceptually:

Authentication
→ Who is the user?

Authorization
→ What role does the user have?

Subscription / Entitlement
→ Does this Student currently have Premium access?

Keep these concerns separate.

---

## 16. Payments and Revenue

Students are the paying users in the current business model.

Teachers and Admins do not purchase Premium access for their role.

Teacher payout, marketplace commission, withdrawals, and revenue sharing are outside the current project scope.

Admin may view:

- transactions
- active subscriptions
- Standard/Premium user statistics
- revenue over time

Payment success must be verified by trusted backend logic.

Do not activate Premium solely because:

- the frontend reports payment success
- the browser reaches a success page
- a client-provided field says payment succeeded

Payment Provider integration should eventually be isolated behind a clear provider boundary.

---

## 17. Scope Control

Do not automatically introduce major features such as:

- social networking
- forums
- live classes
- video calling
- Teacher marketplace
- Teacher payouts
- native mobile applications
- AI chatbot
- AI speaking evaluation
- certificates
- leaderboards
- complex gamification
- placement tests
- complex spaced repetition
- enterprise SSO
- unnecessary social login providers

These may be considered future enhancements but are not part of the current core scope unless explicitly approved.

---

## 18. Architecture Principles

Prefer a simple architecture appropriate for a graduation project.

Use a modular monolithic backend as the default architecture.

Avoid unnecessary microservices.

Current intended technology direction:

### Frontend

- Next.js
- TypeScript
- responsive web design

### Backend

- Java
- Spring Boot
- Spring Security

### Database

- PostgreSQL

### Authentication

- JWT-based authentication
- short-lived JWT Access Token
- Refresh Token

Conceptually:

Browser
→ Next.js
→ Spring Boot
→ PostgreSQL

Spring Boot may also communicate with external providers such as:

- Dictionary Provider
- pronunciation/audio provider
- Text-to-Speech provider
- Payment Provider

Do not introduce additional infrastructure without a clear requirement.

Do not automatically introduce:

- Redis
- Kafka
- RabbitMQ
- Elasticsearch
- Kubernetes
- event sourcing
- CQRS

These require technical justification.

---

## 19. Implementation Principles

When implementation begins:

- keep business logic separate from presentation logic
- validate input
- enforce authorization server-side
- avoid duplicated domain logic
- use clear naming
- document important decisions
- write tests for important business rules
- do not expose secrets or API keys in source code
- do not hardcode environment-specific configuration
- keep controllers focused on HTTP concerns
- keep business workflows primarily in services
- keep persistence access in repositories
- use DTOs for public API boundaries where appropriate
- do not expose persistence entities directly through APIs by default
- avoid unnecessary circular dependencies between features

### Feature-Based Code Organization

The project must use feature-based organization.

Backend code must be grouped primarily by business feature/domain rather than globally by technical layer.

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

---

## 20. Feature and Task Status Tracking

- Before implementing or modifying a feature, read `docs/FEATURE_STATUS.md` and the corresponding task in `docs/TASK_BREAKDOWN.md`; inspect existing implementation and evidence first.
- `docs/FEATURE_STATUS.md` is the authoritative feature/task implementation-status source. Do not duplicate live status in `docs/TASK_BREAKDOWN.md`.
- Do not reimplement a feature marked DONE unless the user explicitly requests a change, bug fix, extension or refactor.
- Use only TODO, IN_PROGRESS, BLOCKED and DONE. Set feature/task status to IN_PROGRESS when implementation genuinely starts; leave partial work IN_PROGRESS unless a concrete unresolved dependency prevents further meaningful work, in which case record BLOCKED and its reason.
- Mark DONE only after completion criteria and required verification pass. Record actual implementation files, verification results, remaining work and commit references when available.
- Keep status synchronized with the actual codebase; documentation completion is not implementation completion. Never change status merely to make progress appear complete.
- Preserve approval-first behavior for material design or scope changes.
