---
name: requirements-analysis
description: Use when defining, reviewing, modifying, clarifying, or extending functional requirements, non-functional requirements, business rules, actors, permissions, workflows, acceptance criteria, or project scope for the English Learning Platform.
---

---

# Requirements Analysis

## Purpose

Use this skill whenever a request changes or may change what the English Learning Platform is expected to do.

Examples include:

- adding a feature
- changing an existing feature
- changing Student behavior
- changing Teacher permissions
- changing Admin permissions
- changing Standard/Premium access
- changing Course or Lesson behavior
- changing exercise behavior
- changing subscription behavior
- defining a new workflow
- resolving unclear requirements

Do not immediately implement a feature when its requirements are incomplete.

---

# Source of Truth

Before analyzing requirements, read the relevant sections of:

1. `AGENTS.md`
2. `docs/PROJECT_SPEC.md`
3. `docs/REQUIREMENTS.md`
4. `docs/BUSINESS_RULES.md`

When necessary, also consult:

- `docs/USE_CASES.md`
- `docs/ARCHITECTURE.md`

if those documents exist.

Do not assume an undocumented behavior is already approved.

---

# Requirement Analysis Workflow

For every new or changed requirement, perform the following analysis.

## Step 1 — Identify the Request

Restate the requested behavior in clear domain language.

Example:

User request:

"Teacher should be able to delete a Course."

Normalized requirement:

"An authorized Teacher may request deletion of a Course they own."

Do not change the meaning of the request while normalizing it.

---

## Step 2 — Identify the Actor

Determine the primary actor.

Possible actors:

- STUDENT
- TEACHER
- ADMIN
- SYSTEM
- external provider

A feature may involve multiple actors.

Clearly identify the primary actor and supporting actors.

---

## Step 3 — Identify the Domain Area

Classify the request.

Examples:

- Authentication
- Student Account
- Teacher Account
- Admin
- CEFR
- Course
- Enrollment
- Lesson
- Vocabulary
- Vocabulary Search
- Saved Vocabulary
- Exercise
- Fill Word
- Listening
- Quiz
- Score
- Progress
- Mastery
- Review
- Subscription
- Payment
- Analytics
- External Integration

---

## Step 4 — Search Existing Requirements

Determine whether the requested behavior:

- already exists
- partially exists
- conflicts with an existing requirement
- extends an existing requirement
- is completely new

Reference existing requirement IDs where relevant.

Example:

`FR-TCR-005`

Do not create duplicate requirements when an existing requirement can be extended cleanly.

---

## Step 5 — Check Business Rules

Identify applicable rules from:

`docs/BUSINESS_RULES.md`

Examples:

- Course ownership
- Premium entitlement
- enrollment
- data preservation
- exercise scoring
- subscription expiration

A requirement must not silently contradict an existing Business Rule.

---

## Step 6 — Determine Preconditions

Identify what must already be true.

Example:

Teacher updates Course.

Possible preconditions:

- Teacher is authenticated
- Course exists
- Teacher owns Course
- Course is editable

Do not invent unnecessary preconditions.

---

## Step 7 — Determine Main Flow

Describe the normal successful workflow.

Keep the flow implementation-independent.

Example:

1. Teacher opens owned Course.
2. Teacher chooses Edit.
3. System displays editable Course information.
4. Teacher modifies permitted fields.
5. Teacher submits changes.
6. System validates changes.
7. System saves the Course.
8. System confirms success.

---

## Step 8 — Determine Alternative Flows

Consider relevant alternatives.

Examples:

- invalid input
- unauthorized action
- Course not found
- Premium required
- duplicate enrollment
- external provider unavailable
- payment failed

Only include alternatives relevant to the requirement.

---

## Step 9 — Determine Postconditions

Describe what must be true after successful completion.

Example:

- Course information is updated.
- Existing enrollment data remains unchanged.
- Course ownership remains unchanged.

---

# Authorization Analysis

Every requirement involving protected data or actions must identify authorization.

Ask:

- Must the user be authenticated?
- Which role can perform the action?
- Does ownership matter?
- Does Standard/Premium matter?
- Is Admin override allowed?
- Must authorization be enforced server-side?

Never rely solely on frontend visibility.

---

# Student Access Analysis

For Student functionality, determine whether the feature is:

- available to STANDARD
- available to PREMIUM
- available to both
- dependent on Course access

Do not classify a feature as Premium simply because it appears advanced.

Premium restrictions must follow approved Business Rules.

---

# Data Impact Analysis

For each important requirement, identify affected domain data.

Examples:

Course creation may affect:

- Course
- Teacher ownership
- CEFR relationship

Exercise submission may affect:

- Attempt
- Answer
- Score
- Vocabulary performance
- Mastery
- Learning History

Premium payment may affect:

- Transaction
- Subscription
- Student entitlement

Do not design database tables during requirements analysis unless specifically requested.

Identify domain data, not implementation schema.

---

# Learning Impact Analysis

Student learning features should be checked for impact on:

- Course progress
- Lesson progress
- attempts
- score
- accuracy
- vocabulary performance
- mastery
- Weak Vocabulary
- Review
- Learning History

Example:

If a new exercise type contributes to vocabulary performance, the requirement must define whether its answers affect mastery.

Do not assume this automatically.

---

# External Integration Analysis

For requirements involving:

- Dictionary Provider
- pronunciation audio
- Text-to-Speech
- Payment Provider

identify external dependencies explicitly.

Do not assume provider behavior that has not been verified.

For dictionary content, consider:

- licensing
- caching
- storage
- redistribution
- rate limits

For payment, consider:

- trusted verification
- failed payment
- duplicate callbacks
- transaction state

Detailed technical handling belongs to later design stages.

---

# Requirement Writing Rules

Functional requirements should generally use:

"The system shall..."

or:

"An authorized [Actor] shall be able to..."

Requirements should be:

- clear
- testable
- implementation-independent where possible
- traceable
- non-duplicated
- consistent with Business Rules

Avoid vague requirements such as:

"The system should be user friendly."

Prefer measurable or inspectable requirements where practical.

---

# Requirement ID Convention

Continue using the existing project conventions.

Examples:

Student:

`FR-STU-xxx`

Discovery:

`FR-DIS-xxx`

Enrollment:

`FR-ENR-xxx`

Vocabulary:

`FR-VOC-xxx`

Search:

`FR-SEA-xxx`

Review:

`FR-REV-xxx`

Teacher:

`FR-TEA-xxx`

Teacher Course:

`FR-TCR-xxx`

Admin:

`FR-ADM-xxx`

Subscription:

`FR-SUB-xxx`

Payment:

`FR-PAY-xxx`

Non-functional:

`NFR-xxx`

Integration:

`INT-xxx`

Do not renumber existing requirement IDs unnecessarily.

---

# Business Rule ID Convention

When adding Business Rules, use the existing domain grouping.

Examples:

`BR-COURSE-xxx`

`BR-ENR-xxx`

`BR-VOC-xxx`

`BR-SCORE-xxx`

`BR-MAST-xxx`

`BR-SUB-xxx`

`BR-PAY-xxx`

Do not silently change an existing Business Rule's meaning.

If a rule changes materially, explicitly identify the change.

---

# Acceptance Criteria

When a requirement is sufficiently defined, create acceptance criteria when useful.

Prefer observable behavior.

Example:

Requirement:

A Standard Student cannot enroll in a Premium Course.

Acceptance criteria:

- Given an authenticated Standard Student
- And a published Premium Course
- When the Student attempts to enroll
- Then enrollment is not created
- And the system indicates Premium access is required

Acceptance criteria should describe behavior, not UI implementation details unless UI behavior itself is required.

---

# Requirement Conflict Handling

When two requirements or documents conflict:

1. Identify both conflicting statements.
2. Explain the conflict clearly.
3. Do not silently select one.
4. Request a business decision if necessary.
5. After approval, update the appropriate source-of-truth documents.

---

# Scope Analysis

Before accepting a new feature into the core project, classify it as:

## CORE

Required for the approved primary platform workflow.

## SUPPORTING

Useful for the core workflow but not central.

## FUTURE

Potential future enhancement.

## OUT OF SCOPE

Not part of the current project.

The current core focus is:

Discover
→ Enroll
→ Learn
→ Practice
→ Assess
→ Track
→ Review

Teacher supports content creation.

Admin supports platform management and business operations.

---

# Scope Warning Examples

Features that require explicit approval before entering core scope include:

- AI chatbot
- AI speaking assessment
- live classes
- messaging
- forum
- Teacher marketplace
- Teacher payout
- social features
- native mobile applications
- leaderboard
- complex gamification
- certificates
- placement tests
- complex spaced repetition

Do not silently add these because competing products contain them.

---

# Change Impact Analysis

For significant requirement changes, identify affected artifacts.

Possible affected files:

- `docs/PROJECT_SPEC.md`
- `docs/REQUIREMENTS.md`
- `docs/BUSINESS_RULES.md`
- `docs/USE_CASES.md`
- `docs/ARCHITECTURE.md`
- database design
- API design
- frontend
- backend
- tests

Example:

Changing:

"Course has one Teacher"

to:

"Course can have multiple Teachers"

affects:

- Course ownership Business Rules
- authorization
- Course model
- Teacher analytics
- database relationships
- APIs
- UI
- tests

Flag this before implementation.

---

# Do Not Jump to Implementation

When requirements are not sufficiently defined:

Do not:

- generate database migrations
- generate backend entities
- create REST endpoints
- create frontend pages
- choose infrastructure

Instead:

1. identify the missing requirement
2. propose clear alternatives if useful
3. obtain or document the decision
4. update requirements
5. update Business Rules
6. continue to design

---

# Definition of Ready

A feature is ready for detailed design when:

- Actor is known
- objective is clear
- primary workflow is defined
- important alternative flows are known
- authorization is defined
- Standard/Premium behavior is defined if relevant
- major Business Rules are defined
- important data effects are understood
- unresolved questions are documented

A feature does not need every UI detail before design begins.

---

# Expected Output

When analyzing a new requirement, prefer an output containing:

1. Requirement summary
2. Actor
3. Existing related requirement IDs
4. Applicable Business Rules
5. Proposed requirement/change
6. Main workflow
7. Important alternative flows
8. Authorization
9. Standard/Premium impact
10. Data/learning impact
11. Open decisions
12. Affected documentation

For small requirement changes, use a shorter version of this structure.

Do not create unnecessary documentation for trivial changes.
