# English Learning Platform --- Domain Model

**Version:** 0.1\
**Status:** Draft\
**Project Type:** Graduation Project

---

# 1. Purpose

This document defines the conceptual domain model of the English
Learning Platform.

It describes domain concepts, responsibilities, relationships,
ownership, and important invariants.

This document does **not** define:

- PostgreSQL tables
- JPA annotations
- REST endpoints
- DTO classes
- frontend components
- implementation package structure

Those decisions belong to later design stages.

---

# 2. Design Principles

1.  Use one shared account model for authentication and authorization.
2.  STUDENT, TEACHER, and ADMIN are roles.
3.  STANDARD and PREMIUM are Student access tiers, not roles.
4.  Vocabulary is reusable platform data and is not owned independently
    by each Teacher.
5.  A Vocabulary may have multiple senses.
6.  A Lesson selects the appropriate Vocabulary Sense for its learning
    context.
7.  A Teacher owns Courses, not global Vocabulary.
8.  Learning history must remain interpretable after Course archival or
    Premium expiration.
9.  Payment success and Premium entitlement are backend-authoritative.
10. The initial system is a modular monolith; domain boundaries should
    remain clear without introducing microservices.

---

# 3. High-Level Domain Areas

```text
Identity & Access
├── User
├── Role
├── AccountStatus
├── RefreshSession
├── EmailVerification
└── PasswordReset

Learning Content
├── CefrLevel
├── Course
├── Lesson
├── Vocabulary
├── VocabularySense
├── LessonVocabulary
└── PronunciationAudio

Exercises
├── Exercise
└── ExerciseQuestion

Student Learning
├── Enrollment
├── ExerciseAttempt
├── AnswerRecord
├── LessonProgress
├── CourseProgress
├── VocabularyPerformance
└── SavedVocabulary

Commercial
├── SubscriptionPlan
├── Subscription
└── PaymentTransaction
```

Analytics are primarily derived from authoritative domain data rather
than requiring a separate core business entity for every dashboard
metric.

---

# 4. Identity and Access Domain

## DM-USER-001 --- User

`User` represents an account that can authenticate to the platform.

### Core Conceptual Attributes

- id
- email
- password credential
- full_name (required display/full name)
- avatar_url (optional profile image reference)
- role
- account status
- email verification state
- created time
- updated time

### Role

A User has exactly one primary role in the initial version:

- STUDENT
- TEACHER
- ADMIN

### Rules

- Public registration creates STUDENT only.
- TEACHER is provisioned through an authorized Admin workflow.
- ADMIN is not publicly self-registered.
- Role is backend-authoritative.
- PREMIUM must not be stored as a role.
- All account creation supplies full_name; Student/Teacher self-profile updates
  allow only full_name and avatar_url under BR-PROFILE-001. No separate profile
  identity or Admin self-edit permission is introduced.

### References

- `BR-ROLE-*`
- `BR-AUTHN-001`--`BR-AUTHN-023`
- `UC-AUTH-*`

---

## DM-USER-002 --- AccountStatus

Initial account states:

```text
PENDING_VERIFICATION
ACTIVE
LOCKED
DISABLED
```

Conceptual transition:

```text
registration
    ↓
PENDING_VERIFICATION
    ↓ email verified
ACTIVE
 ├────────→ LOCKED
 └────────→ DISABLED
```

The exact administrative unlock/enable workflow may be refined later.

---

## DM-AUTH-001 --- RefreshSession

`RefreshSession` represents server-authoritative refresh authentication
state.

### Conceptual Attributes

- id
- user
- refresh credential identifier/hash
- issued time
- expiration time
- revoked time/status
- replacement/rotation information where required

### Rules

- Refresh state belongs to one User.
- Invalid, expired, or revoked state cannot create a new Access Token.
- Rotation must be enforceable by the backend.
- Logout invalidates applicable refresh state.
- Raw sensitive token material should not be unnecessarily persisted.

---

## DM-AUTH-002 --- EmailVerification

Represents a purpose-specific email verification process.

### Conceptual Attributes

- id
- user
- verification credential identifier/hash
- created time
- expiration time
- consumed time/status

### Rules

- belongs to one User
- expires
- cannot be reused after successful consumption where applicable

---

## DM-AUTH-003 --- PasswordReset

Represents a password recovery process.

### Conceptual Attributes

- id
- user
- reset credential identifier/hash
- created time
- expiration time
- consumed time/status

### Rules

- belongs to one User
- is purpose-specific
- expires
- successfully consumed credentials are not reusable

---

# 5. CEFR and Course Domain

## DM-CEFR-001 --- CefrLevel

Represents the supported English proficiency level.

Supported values:

```text
A1
A2
B1
B2
C1
C2
```

A Course belongs to exactly one CEFR level.

Vocabulary may also carry CEFR classification where available.

CEFR levels are platform reference data rather than Teacher-owned
content.

---

## DM-COURSE-001 --- Course

Represents a structured learning Course.

### Conceptual Attributes

- id
- title
- description
- CEFR level
- primary Teacher
- lifecycle status
- access classification
- created time
- updated time

### Lifecycle Status

```text
DRAFT
PUBLISHED
ARCHIVED
```

### Access Classification

```text
STANDARD
PREMIUM
```

### Relationships

```text
Teacher 1 ─────── * Course
CefrLevel 1 ───── * Course
Course 1 ──────── * Lesson
Course 1 ──────── * Enrollment
```

### Rules

- One Course has one primary Teacher.
- Spring Boot initializes Teacher-created Courses as DRAFT and STANDARD.
- Teacher create/update input excludes access classification; other approved
  ownership-based editing and content-management capabilities remain intact.
- One Teacher may own many Courses.
- Teacher may modify only owned Courses.
- Teacher may publish/archive an owned Course according to business
  rules.
- Admin has final authority for STANDARD/PREMIUM Course
  classification.
- Archiving a Course does not delete historical Student learning data.

### References

- `BR-COURSE-*`
- `BR-AUTH-003`
- `BR-CSTATUS-*`
- `UC-TEA-COURSE-01`
- `UC-TEA-PUBLISH-01`

---

# 6. Lesson Domain

## DM-LESSON-001 --- Lesson

Represents a learning unit inside a Course.

### Conceptual Attributes

- id
- course
- title
- topic
- description/content summary where required
- ordering position
- created time
- updated time

### Relationships

```text
Course 1 ───── * Lesson
Lesson 1 ───── * LessonVocabulary
Lesson 1 ───── * Exercise
```

### Rules

- A Lesson belongs to exactly one Course.
- Teacher authorization for Lesson modification derives from Course
  ownership.
- Lessons are primarily organized by topic.
- Part of speech is Vocabulary/Sense metadata, not the primary Lesson
  hierarchy.
- Lesson order belongs to its Course context.

### References

- `BR-LESSON-*`
- `UC-STU-VIEW-LESSON-01`
- `UC-TEA-LESSON-01`

---

# 7. Vocabulary Domain

## DM-VOC-001 --- Vocabulary

Represents a reusable vocabulary entry shared across the platform.

### Conceptual Attributes

- id
- canonical word
- CEFR level where available
- pronunciation/IPA data where appropriate
- source/provenance metadata where required
- created time
- updated time

### Relationships

```text
Vocabulary 1 ───── * VocabularySense
Vocabulary 1 ───── * PronunciationAudio
Vocabulary 1 ───── * LessonVocabulary
Vocabulary 1 ───── * SavedVocabulary
Vocabulary 1 ───── * VocabularyPerformance
```

### Rules

- Vocabulary is reusable across Teachers and Courses.
- Removing Vocabulary from one Lesson does not delete the shared
  Vocabulary.
- Dictionary licensing/storage rules must be respected.
- A Vocabulary may contain multiple senses.

---

## DM-VOC-002 --- VocabularySense

Represents one meaning/usage of a Vocabulary.

### Conceptual Attributes

- id
- vocabulary
- part of speech
- English definition
- Vietnamese meaning
- example sentence
- source/provenance where required

### Relationship

```text
Vocabulary 1 ───── * VocabularySense
```

### Rules

- One Vocabulary may have multiple Vocabulary Senses.
- Different senses may use different parts of speech.
- A Teacher selects the sense appropriate to the Lesson context.

Example:

```text
record
├── Sense 1
│   ├── noun
│   └── "information kept about something"
│
└── Sense 2
    ├── verb
    └── "to store sound, video, or information"
```

---

## DM-VOC-003 --- LessonVocabulary

`LessonVocabulary` is the association between a Lesson and reusable
Vocabulary content.

It is required because a Lesson does not merely point to a word; it must
identify the appropriate learning sense/context.

### Conceptual Attributes

- id
- lesson
- vocabulary
- selected VocabularySense
- ordering position
- optional Lesson-specific teaching note where later approved

### Relationships

```text
Lesson 1 ───────── * LessonVocabulary
Vocabulary 1 ───── * LessonVocabulary
VocabularySense 1 ─ * LessonVocabulary
```

### Rules

- Selected VocabularySense must belong to the referenced Vocabulary.
- Removing LessonVocabulary removes only the Lesson association.
- Shared Vocabulary/VocabularySense must not be automatically deleted.
- Duplicate Lesson associations should be prevented when they
  represent the same intended vocabulary/sense.

---

## DM-VOC-004 --- PronunciationAudio

Represents available pronunciation audio metadata.

### Conceptual Attributes

- id
- vocabulary
- audio source/reference
- pronunciation variant where available
- provider/source metadata
- storage/reference strategy

### Rules

- Listening questions require playable audio.
- Audio may originate from an approved Dictionary provider, licensed
  stored media, or approved TTS strategy.
- Permanent storage must not be assumed unless licensing permits it.

The exact provider/storage implementation remains an open design
decision.

---

# 8. Exercise Domain

## DM-EX-001 --- Exercise

Represents a practice/evaluation activity associated with a Lesson.

### Exercise Types

```text
FILL_WORD
LISTENING
QUIZ
```

### Conceptual Attributes

- id
- lesson
- type
- title/instructions where required
- access classification where advanced Premium behavior requires it
- ordering information
- created time
- updated time

### Relationships

```text
Lesson 1 ───── * Exercise
Exercise 1 ─── * ExerciseQuestion
Exercise 1 ─── * ExerciseAttempt
```

### Rules

- Exercise belongs to one Lesson.
- Teacher modification authorization derives from ownership of the
  Lesson's Course.
- Only approved core exercise types are included initially.

---

## DM-EX-002 --- ExerciseQuestion

Represents an answerable question in an Exercise.

### Conceptual Attributes

- id
- exercise
- vocabulary / selected sense where relevant
- question format
- prompt/configuration
- expected answer or correct option
- ordering position

### Initial Formats

Fill Word:

```text
FILL_WORD
```

Listening:

```text
LISTEN_AND_CHOOSE
LISTEN_AND_TYPE
```

Quiz:

```text
WORD_TO_DEFINITION
DEFINITION_TO_WORD
IPA_TO_WORD
CONTEXT_TO_WORD
```

### Rules

- Every answerable question must contain enough information for
  backend evaluation.
- Listening questions must reference usable audio.
- Correct-answer authority belongs to trusted backend content.
- Client-submitted correctness is never authoritative.

---

# 9. Enrollment Domain

## DM-ENR-001 --- Enrollment

Represents a Student's enrollment in a Course.

### Conceptual Attributes

- id
- student User
- course
- enrollment time
- enrollment status where required

### Relationships

```text
Student 1 ───── * Enrollment
Course 1 ────── * Enrollment
```

### Rules

- Only STUDENT accounts enroll as learners.
- Enrollment requires Course accessibility.
- Premium Course enrollment requires active Premium entitlement.
- Duplicate active enrollment for the same Student/Course should not
  exist.
- Enrollment supports Course progress and analytics.
- Course archival does not erase historical Enrollment data.

---

# 10. Exercise Attempt Domain

## DM-ATT-001 --- ExerciseAttempt

Represents one Student attempt at an Exercise.

### Conceptual Attributes

- id
- student
- exercise
- started time
- completed time
- score
- accuracy where applicable
- attempt status

### Relationships

```text
Student 1 ───── * ExerciseAttempt
Exercise 1 ──── * ExerciseAttempt
ExerciseAttempt 1 ─ * AnswerRecord
```

### Rules

- Multiple attempts are allowed.
- Completed attempts retain their own results.
- Previous relevant attempts remain available for history/analytics.
- Score is calculated by the backend.
- Historical attempts must remain interpretable.

---

## DM-ATT-002 --- AnswerRecord

Represents the Student's answer to one exercise question.

### Conceptual Attributes

- id
- exercise attempt
- exercise question
- submitted answer/selected option
- correctness
- answered time

### Rules

- Correctness is calculated by trusted backend logic.
- AnswerRecord supports vocabulary-level performance.
- Client-supplied `correct=true` is never authoritative.

---

# 11. Progress Domain

## DM-PRO-001 --- LessonProgress

Represents a Student's progress through a Lesson.

### Conceptual Attributes

- student
- lesson
- completion/progress value
- last activity time
- completion time where applicable

The exact completion formula follows Business Rules and may be derived
from learning activity.

---

## DM-PRO-002 --- CourseProgress

Represents a Student's progress through an enrolled Course.

### Conceptual Attributes

- student/enrollment
- course
- progress value
- last activity time
- completion time where applicable

CourseProgress may be derived from Lesson progress according to approved
rules.

---

## DM-PRO-003 --- VocabularyPerformance

Represents accumulated learning evidence for one Student and one
Vocabulary item.

### Conceptual Attributes

- student
- vocabulary
- correct answer count
- answered question count
- mastery percentage/state
- last practiced time

### Initial Mastery State

```text
INSUFFICIENT_DATA
WEAK
LEARNING
MASTERED
```

### Rules

- Fewer than 3 answered vocabulary questions → INSUFFICIENT_DATA.
- With sufficient evidence:
  - 0--49% → WEAK
  - 50--79% → LEARNING
  - 80--100% → MASTERED
- Performance should be based on authoritative AnswerRecord/learning
  history.
- Aggregated fields may later be stored or derived depending on
  database/performance design.

---

# 12. Saved Vocabulary Domain

## DM-SAVE-001 --- SavedVocabulary

Represents a Student saving a Vocabulary item to My Vocabulary.

### Conceptual Attributes

- student
- vocabulary
- saved time

### Relationship

```text
Student * ───── * Vocabulary
      through SavedVocabulary
```

### Rules

- A Student should not have duplicate saved associations for the same
  Vocabulary.
- Removing SavedVocabulary does not delete Vocabulary.
- Saved Vocabulary may be a Review source.

---

# 13. Review Domain

Review is initially modeled as a learning process using existing domain
data rather than as a complex independent spaced-repetition engine.

### Initial Review Sources

- WEAK vocabulary
- saved vocabulary
- incorrectly answered vocabulary
- recently learned vocabulary

### Inputs

```text
VocabularyPerformance
SavedVocabulary
AnswerRecord
ExerciseAttempt
recent learning history
```

### Outputs

Review activity may create new:

```text
ExerciseAttempt / AnswerRecord
        ↓
VocabularyPerformance update
```

### Rules

- Complex spaced repetition is outside initial scope.
- Premium may unlock advanced/personalized Review.
- Exact advanced selection algorithm remains open.

A dedicated persistent `ReviewSession` entity should be introduced only
if later requirements need session history or workflow state that cannot
be represented cleanly by existing attempt data.

---

# 14. Subscription Domain

## DM-SUB-001 --- SubscriptionPlan

Represents a Premium plan offered by the platform.

### Initial Plan Types

```text
MONTHLY
YEARLY
```

### Conceptual Attributes

- id
- plan type
- display name
- price
- active/available state
- duration semantics

Pricing remains a business configuration decision.

---

## DM-SUB-002 --- Subscription

Represents a Student's Premium entitlement period.

### Conceptual Attributes

- id
- student
- plan
- start time
- expiration time
- status
- created/updated time

### Conceptual Status

At minimum the system must be able to determine:

```text
ACTIVE
EXPIRED
```

Additional payment/subscription statuses should be introduced only when
required by the selected payment provider.

### Rules

- Subscription belongs to a STUDENT.
- Premium entitlement derives from authoritative active subscription
  state.
- Premium is not a role.
- Expiration returns the Student to Standard access.
- Expiration does not delete learning history/progress.
- Active manual renewal extends from current expiration.
- Renewal after expiration starts from verified activation/payment
  time.

---

# 15. Payment Domain

## DM-PAY-001 --- PaymentTransaction

Represents a Premium payment transaction.

### Conceptual Attributes

- id
- student
- subscription plan
- provider
- provider transaction/reference identifier
- amount
- currency
- status
- initiated time
- verified/completed time

### Conceptual Status

The exact provider-specific state machine is deferred.

The domain must at minimum distinguish a verified successful transaction
from one that must not activate Premium.

### Rules

- Client-reported payment success is not authoritative.
- Premium activates/extends only after trusted backend verification.
- Revenue uses verified successful transactions.
- Transaction history is available to authorized Admin.
- Teacher payout/commission is outside scope.

---

# 16. Analytics Domain

Analytics should primarily be projections/aggregations over existing
authoritative data.

## Student Analytics Sources

```text
Enrollment
ExerciseAttempt
AnswerRecord
LessonProgress
CourseProgress
VocabularyPerformance
```

## Teacher Analytics Sources

For Courses owned by the Teacher:

```text
Enrollment
CourseProgress
ExerciseAttempt
VocabularyPerformance
```

Teacher analytics should prioritize aggregated learning information.

## Admin Analytics Sources

```text
User
Course
Enrollment
Subscription
PaymentTransaction
learning records
```

Admin metrics include:

- total users
- Student count/statistics
- Teacher count/statistics
- Course statistics
- enrollment statistics
- learning statistics
- active Premium Students
- subscription statistics
- revenue over time

A separate analytics warehouse is not required for the initial
graduation project.

---

# 17. Aggregate and Ownership Boundaries

These are conceptual ownership boundaries, not microservices.

## User / Authentication

```text
User
├── RefreshSession
├── EmailVerification
└── PasswordReset
```

User is the identity root.

## Course Content

```text
Course
├── Lesson
│   ├── LessonVocabulary
│   └── Exercise
│       └── ExerciseQuestion
```

Course ownership controls Teacher content modification.

Global Vocabulary remains outside the Course ownership boundary.

## Vocabulary

```text
Vocabulary
├── VocabularySense
└── PronunciationAudio
```

Vocabulary is shared platform content.

## Student Learning

```text
Enrollment
ExerciseAttempt
AnswerRecord
Progress
VocabularyPerformance
SavedVocabulary
```

These records belong to Student learning history and must not disappear
merely because content is archived or Premium expires.

## Commercial

```text
SubscriptionPlan
Subscription
PaymentTransaction
```

Payment verification controls Premium entitlement changes.

---

# 18. Conceptual Relationship Diagram

```text
                         ┌──────────────┐
                         │  CefrLevel   │
                         └──────┬───────┘
                                │ 1
                                │
                                │ *
┌────────────┐ 1          * ┌───▼──────────┐
│ Teacher    ├──────────────►│    Course    │
│   User     │               └─────┬────────┘
└────────────┘                     │ 1
                                   │
                                   │ *
                              ┌────▼─────┐
                              │  Lesson  │
                              └─┬─────┬──┘
                                │     │
                    *           │     │ 1
              ┌─────────────────┘     │
              │                       │ *
      ┌───────▼──────────┐       ┌────▼────────┐
      │ LessonVocabulary │       │  Exercise   │
      └───────┬──────────┘       └────┬────────┘
              │                       │ 1
              │                       │
              │                       │ *
      ┌───────▼──────────┐       ┌────▼──────────────┐
      │    Vocabulary    │       │ ExerciseQuestion │
      └──────┬───────────┘       └───────────────────┘
             │ 1
             │
             │ *
      ┌──────▼───────────┐
      │ VocabularySense │
      └─────────────────┘


┌────────────┐      * ┌────────────┐ *      1 ┌────────────┐
│ Student    ├────────► Enrollment ├──────────►│   Course   │
│   User     │        └────────────┘           └────────────┘
└─────┬──────┘
      │
      │ 1
      │
      │ *         * ┌─────────────────┐ 1
      ├────────────►│ ExerciseAttempt ├────────► Exercise
      │             └────────┬────────┘
      │                      │ 1
      │                      │
      │                      │ *
      │               ┌──────▼──────┐
      │               │ AnswerRecord│
      │               └─────────────┘
      │
      ├──────────────► VocabularyPerformance ◄──── Vocabulary
      │
      └──────────────► SavedVocabulary ◄────────── Vocabulary


Student User
    │
    ├──── * Subscription ──── 1 SubscriptionPlan
    │
    └──── * PaymentTransaction ──── 1 SubscriptionPlan
```

This diagram is conceptual. Exact foreign keys and join constraints
belong to `DATABASE_DESIGN.md`.

---

# 19. Important Domain Invariants

## INV-001 --- Role and Premium Separation

```text
role ∈ {STUDENT, TEACHER, ADMIN}
```

Premium is not a role.

---

## INV-002 --- Course Ownership

Every Course has exactly one primary Teacher in the initial version.

Teacher modification of Course-owned content requires ownership.

---

## INV-003 --- Lesson Ownership

A Lesson belongs to one Course.

Teacher permission to modify the Lesson derives from Course ownership.

---

## INV-004 --- Vocabulary Reuse

Vocabulary is shared platform data.

Removing a Lesson association must not automatically delete reusable
Vocabulary.

---

## INV-005 --- Sense Integrity

A LessonVocabulary selected sense must belong to its referenced
Vocabulary.

---

## INV-006 --- Enrollment Uniqueness

A Student should not have duplicate active enrollment for the same
Course.

---

## INV-007 --- Saved Vocabulary Uniqueness

A Student should not have duplicate SavedVocabulary associations for the
same Vocabulary.

---

## INV-008 --- Backend Correctness

Exercise correctness, score, and learning performance are
backend-authoritative.

---

## INV-009 --- Premium Entitlement

Premium access is determined from authoritative Subscription state.

Client flags and stale JWT claims do not independently grant Premium.

---

## INV-010 --- Payment Verification

A PaymentTransaction activates or extends Premium only after trusted
verification.

---

## INV-011 --- Historical Learning Preservation

Premium expiration, Course archival, or temporary inactivity must not
delete Student learning history.

---

## INV-012 --- Mastery Evidence

Vocabulary cannot be classified WEAK, LEARNING, or MASTERED until the
Student has at least 3 answered vocabulary questions for that
Vocabulary.

---

# 20. Concepts Deliberately Not Modeled as Core Entities Yet

The following should **not** be added automatically:

- native mobile application entities
- social posts
- forum threads
- live classes
- video calls
- teacher marketplace
- teacher payouts
- certificates
- leaderboard
- placement tests
- AI chatbot conversations
- AI speaking assessments
- complex spaced-repetition scheduling
- separate Student/Teacher/Admin authentication tables
- separate Vocabulary copies per Teacher
- analytics warehouse entities

They may be introduced later only after approved requirements.

---

# 21. Open Domain Decisions

The following remain open and should be resolved before dependent
database/API implementation:

1.  Exact Dictionary Provider and licensing/storage model.
2.  Exact pronunciation audio/TTS strategy.
3.  Exact Payment Provider and provider transaction states.
4.  Premium pricing.
5.  Exact advanced Premium exercise model.
6.  Exact personalized Review algorithm.
7.  Whether ReviewSession requires persistent state.
8.  Exact Lesson/Course completion formula where not already finalized.
9.  Exact handling of deleted/retired educational content while
    preserving historical attempts.
10. Whether Subscription history is represented by multiple immutable
    periods or another model after payment-provider design is selected.

---

# 22. Traceability Example

```text
Requirement
FR-QUIZ-*
    ↓
Business Rules
BR-QUIZ-*
BR-SCORE-*
    ↓
Use Case
UC-LEARN-QUIZ-01
    ↓
Domain
Exercise
ExerciseQuestion
ExerciseAttempt
AnswerRecord
VocabularyPerformance
    ↓
Database Design
    ↓
API Design
    ↓
Implementation Task
    ↓
Tests
```

Domain identifiers should be used by later documents to avoid repeatedly
loading the full domain specification.

---

# 23. Next Design Stage

After this Domain Model is approved, proceed to:

```text
ARCHITECTURE.md
        ↓
DATABASE_DESIGN.md
        ↓
UI_UX.md
        ↓
API_DESIGN.md
        ↓
TASK BREAKDOWN
        ↓
IMPLEMENTATION
```

Do not generate JPA entities or PostgreSQL tables directly from this
document before architecture and database design are reviewed.
