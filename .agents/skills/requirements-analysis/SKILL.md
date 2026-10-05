---
name: requirements-analysis
description: Use when defining, reviewing, modifying, clarifying, or extending functional requirements, non-functional requirements, business rules, actors, permissions, workflows, acceptance criteria, or project scope for the English Tutoring Platform.
---

---

# Requirements Analysis

## Purpose

Use this skill whenever a request changes or may change what the English Tutoring Platform is expected to do.

Examples include:

- adding a feature
- changing an existing feature
- changing Student behavior
- changing Teacher permissions
- changing Admin permissions
- changing resource access
- changing Course behavior
- changing participation or payment behavior
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

Current AGENTS.md is authoritative during the tutoring-scope transition.
Conflicting legacy business rules in other documents are superseded; consult
reconciled requirements or compatible technical/security guidance. Report remaining
ambiguities rather than treating historical plans as current approval.
Preserve completed TASK-001/TASK-002/TASK-003 infrastructure and verification evidence.
This skill does not authorize schema changes, implementation or unrelated edits.


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
- Course
- Enrollment
- Learning participation
- Progress
- Review
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
- resource access
- enrollment
- data preservation
- permitted state transitions

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
- participation access denied
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
- Does participation or resource-access eligibility matter?
- Is Admin override allowed?
- Must authorization be enforced server-side?

Never rely solely on frontend visibility.

---

# Student Access Analysis

Identify which information is public and which requires authenticated participation,
ownership or other approved access conditions. Use current business rules;
do not infer access from payment initiation or browser state.

---

# Data Impact Analysis

Identify affected domain data and preservation requirements.
Course changes may affect ownership, participation and scheduling.
Payment changes may affect transaction history and access eligibility; those remain
distinct concerns. Do not design database tables during requirements analysis
unless requested. Identify domain data, not implementation schema.

---

# Learning Impact Analysis

Identify affected content, participation, submissions, results, progress and history
where required. Ask which activity contributes to a result; do not invent
evaluation or progress formulas.

---

# External Integration Analysis

Identify external dependencies, data ownership, privacy/licensing constraints,
failure behavior and authoritative verification where relevant.
Distinguish an approved integration goal from its unselected implementation.
Do not assume a provider, confirmation mechanism or synchronization workflow.
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

Payment:

`FR-PAY-xxx`

Non-functional:

`NFR-xxx`

Integration:

`INT-xxx`

Do not renumber existing requirement IDs unnecessarily. Historical IDs do not
approve retired scope; do not silently reuse them for a different requirement.

---

# Business Rule ID Convention

When adding Business Rules, use the existing domain grouping.

Examples:

`BR-COURSE-xxx`

`BR-ENR-xxx`

`BR-PAY-xxx`

Do not silently change an existing Business Rule's meaning.

If a rule changes materially, explicitly identify the change.

---

# Acceptance Criteria

Prefer observable acceptance criteria when a requirement is sufficiently defined.

Example: a Teacher may modify only owned Courses.

- Given an authenticated Teacher
- And a Course owned by another Teacher
- When the Teacher requests an update
- Then the backend rejects the operation
- And the Course remains unchanged

Describe behavior, not UI implementation details unless UI behavior is required.

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

Determine the current core workflow from AGENTS.md and reconciled requirements.
Use `english-tutoring-domain` for domain interpretation. Do not carry a historical
workflow forward solely because it appeared in a plan.

---

# Scope Warning Examples

Features that require explicit approval before entering core scope include:

- AI chatbot
- AI speaking assessment
- messaging
- forum
- platform payout infrastructure
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
- participation/resource-access conditions are defined if relevant
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
9. Access impact
10. Data/learning impact
11. Open decisions
12. Affected documentation

For small requirement changes, use a shorter version of this structure.

Do not create unnecessary documentation for trivial changes.
