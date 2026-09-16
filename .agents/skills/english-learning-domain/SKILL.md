---
name: english-learning-domain
description: Use when analyzing, designing, implementing, reviewing, or testing functionality related to the English Learning Platform domain, including Students, Teachers, Admins, CEFR levels, Courses, Lessons, Vocabulary, exercises, progress, review, Premium subscriptions, payments, and analytics.
---

---

# English Learning Platform Domain

## Purpose

Use this skill for work involving the business domain of the English Learning Platform.

This skill does not replace the project's specification or business rules.

Before making domain decisions, consult:

1. `AGENTS.md`
2. `docs/PROJECT_SPEC.md`
3. `docs/REQUIREMENTS.md`
4. `docs/BUSINESS_RULES.md`

These documents are the project source of truth.

---

# Core Domain Model

The platform has three primary roles:

- STUDENT
- TEACHER
- ADMIN

STANDARD and PREMIUM are Student access tiers, not roles.

The primary learning hierarchy is:

CEFR Level
→ Course
→ Lesson
→ Vocabulary

The primary Student learning flow is:

Discover
→ Enroll
→ Learn
→ Practice
→ Assess
→ Track Progress
→ Review

---

# Student Domain

Students are learners.

Student functionality may include:

- account management
- Course discovery
- Course enrollment
- My Courses
- Lesson learning
- Vocabulary Search
- My Vocabulary
- Fill Word
- Listening
- Quiz
- scores
- progress
- mastery
- Weak Vocabulary
- Review
- Learning History
- Standard/Premium access
- subscriptions

When designing Student functionality, prioritize the learning experience.

Student UI is mobile-first.

Do not require a native mobile application.

---

# Teacher Domain

Teachers are educational content creators.

A Teacher may own multiple Courses.

A Course has one primary Teacher.

Teachers primarily manage:

- Courses
- Lessons
- Lesson vocabulary
- vocabulary senses
- exercises
- Course learning analytics

Teacher functionality is not a marketplace.

Do not introduce:

- Teacher payouts
- Teacher commissions
- Teacher withdrawals
- Student-to-Teacher payments

unless the project specification is explicitly changed.

Teacher interfaces are desktop-optimized but must remain responsive.

---

# Admin Domain

Admins manage platform operations.

Admin responsibilities may include:

- Student management
- Teacher management
- Course oversight
- Course access classification
- subscriptions
- transactions
- system analytics
- revenue analytics

Revenue belongs to the platform business model.

Teachers must not automatically receive access to platform-wide revenue data.

---

# CEFR Domain

Supported CEFR levels are:

- A1
- A2
- B1
- B2
- C1
- C2

CEFR Levels belong to the platform.

Teachers select an existing CEFR Level when creating a Course.

Do not create arbitrary CEFR levels unless requirements explicitly change.

---

# Course Domain

A Course:

- belongs to one CEFR Level
- has one primary Teacher
- contains Lessons
- may be STANDARD or PREMIUM
- may have Student enrollments

Students enroll in Courses.

Enrollment supports:

- My Courses
- Course progress
- enrollment statistics
- learning tracking

Do not model Premium as purchasing an individual Teacher's Course.

Premium belongs to the platform.

---

# Lesson Domain

A Lesson belongs to one Course.

Lessons should primarily be organized by learning topic.

Examples:

- Family
- Food & Drinks
- Travel
- Technology
- School
- Shopping

Do not make part of speech the primary Lesson hierarchy.

A Lesson may contain vocabulary and learning activities.

The core learning sequence concept is:

Learn Vocabulary
→ Fill Word
→ Listening
→ Quiz
→ Result

Students may repeat permitted activities.

---

# Vocabulary Domain

Vocabulary is a shared platform resource.

Avoid unnecessary duplication of vocabulary for different Teachers.

A Vocabulary entry may include:

- word
- IPA
- CEFR level
- pronunciation audio
- one or more senses

A Vocabulary Sense may include:

- part of speech
- English definition
- Vietnamese meaning
- example sentence

One word may have multiple senses.

Example:

book

Sense A:

- noun
- a written or printed work

Sense B:

- verb
- to reserve something

Lessons should reference the appropriate sense when context matters.

---

# Dictionary Integration

Teacher vocabulary workflow should conceptually follow:

Search Platform Vocabulary
→ Found?
→ Yes: reuse existing Vocabulary
→ No: query approved Dictionary Provider
→ Teacher reviews result
→ Select appropriate data
→ Use in Lesson

Do not assume external dictionary content can be permanently stored.

Provider licensing must be verified before deciding:

- caching
- permanent storage
- redistribution
- pronunciation audio storage

The Dictionary Provider has not yet been finalized.

Do not invent one as a settled project dependency.

---

# Vocabulary Search

Vocabulary Search is a core Student feature.

It must not be treated as Premium-only.

Standard Students should receive useful basic vocabulary information where available.

Premium may provide deeper information depending on licensed data.

Students may save vocabulary to My Vocabulary.

Saved Vocabulary belongs to the Student's personal collection.

Removing a saved word must not delete the shared Vocabulary resource.

---

# Exercise Domain

The initial core exercise groups are:

1. Fill Word
2. Listening
3. Quiz

Do not introduce major new exercise categories without checking project scope.

---

# Fill Word

Purpose:

- spelling practice
- vocabulary recall

A Fill Word question may provide:

- IPA
- English definition
- partially hidden word

Answer comparison initially:

- trim surrounding whitespace
- case-insensitive
- otherwise require correct spelling

---

# Listening

Purpose:

- spoken vocabulary recognition

Initial formats:

- LISTEN_AND_CHOOSE
- LISTEN_AND_TYPE

Listening requires playable audio.

Do not design Listening functionality that assumes audio always exists without defining fallback behavior.

---

# Quiz

Initial Quiz concepts:

- WORD_TO_DEFINITION
- DEFINITION_TO_WORD
- IPA_TO_WORD
- CONTEXT_TO_WORD

Each question must have a deterministic evaluation method.

---

# Attempts and Scores

Exercise attempts should be retained when required for:

- Learning History
- analytics
- mastery
- Weak Vocabulary

Initial percentage score:

Correct Answers / Total Questions × 100

Do not discard previous attempt history simply because a Student retries an exercise.

---

# Vocabulary Mastery

Vocabulary-level learning performance is important to the platform.

Initial conceptual mastery uses Student answer history.

Current default categories:

- 0–49%: Weak
- 50–79%: Learning
- 80–100%: Mastered

These are configurable project business thresholds, not universal language-learning truths.

Do not describe them as scientifically optimal.

Check `BUSINESS_RULES.md` before implementing mastery calculations.

---

# Review Domain

Review may use vocabulary from:

- Weak Vocabulary
- Saved Vocabulary
- incorrect answers
- recently learned vocabulary

The initial project does not require a complex spaced-repetition algorithm.

Do not introduce a complex SRS system unless explicitly approved.

---

# Standard Access

Standard must provide genuine educational value.

Do not design Standard as an unusable demo.

Core Standard concepts include:

- Standard Courses
- vocabulary learning
- basic pronunciation
- definitions
- meanings
- examples
- core exercises
- Vocabulary Search
- My Vocabulary
- basic Review
- basic Progress

Always verify the final feature matrix in Business Rules.

---

# Premium Access

Premium belongs to the platform.

Potential Premium value includes:

- Premium Courses
- specialized vocabulary Courses
- deeper vocabulary information
- advanced exercises
- advanced Listening
- advanced Weak Vocabulary practice
- personalized Review
- detailed analytics

Do not assume entire CEFR levels are Premium-only.

---

# Subscription Domain

Initial subscription periods:

- MONTHLY
- YEARLY

Premium requires an active entitlement.

When Premium expires:

- Student remains registered
- Student returns to Standard
- enrollments remain
- progress remains
- attempts remain
- scores remain
- Learning History remains
- Saved Vocabulary remains

Premium-only access becomes restricted.

---

# Payment Domain

The Student pays the platform for Premium.

A verified payment may activate Premium.

Never grant Premium solely because the frontend reports payment success.

Payment verification must be trusted by backend logic.

Payment Provider is currently undecided.

Do not invent a provider as an approved dependency.

---

# Analytics Domain

Student analytics focus on personal learning.

Teacher analytics focus on owned Courses.

Admin analytics focus on the platform.

Examples:

Student:

- progress
- scores
- mastery
- Weak Vocabulary
- Learning History

Teacher:

- Course enrollments
- completion
- average performance
- difficult vocabulary

Admin:

- total users
- Teachers
- Courses
- enrollments
- subscriptions
- transactions
- revenue

Respect role boundaries when designing analytics.

---

# Domain Decision Procedure

When asked to add or change functionality:

## Step 1 — Identify Actor

Determine whether the functionality belongs to:

- Student
- Teacher
- Admin
- shared platform infrastructure

## Step 2 — Identify Domain Area

Examples:

- Course
- Lesson
- Vocabulary
- Exercise
- Progress
- Review
- Subscription
- Payment

## Step 3 — Find Requirements

Identify relevant requirement IDs in:

`docs/REQUIREMENTS.md`

Do not implement based only on the feature name.

## Step 4 — Find Business Rules

Check:

`docs/BUSINESS_RULES.md`

Determine applicable rules.

## Step 5 — Check Authorization

Determine:

- who may view
- who may create
- who may update
- who may delete
- whether ownership matters
- whether Premium matters

## Step 6 — Check Data Impact

Identify affected domain data.

Do not duplicate existing domain concepts unnecessarily.

## Step 7 — Check Learning Impact

For Student-facing features, determine whether the change affects:

- learning
- attempts
- scores
- progress
- mastery
- Review
- Learning History

## Step 8 — Check Subscription Impact

Determine whether the feature is:

- Standard
- Premium
- independent of Student subscription

Do not invent Premium restrictions.

## Step 9 — Check Scope

Compare the proposed feature against current project scope.

If the feature is outside scope, identify it as a proposed extension instead of silently adding it.

## Step 10 — Proceed

Only after the domain rules are understood should design or implementation begin.

---

# Important Constraints

Do not silently introduce:

- native mobile applications
- microservice architecture
- Teacher marketplace
- Teacher payout
- social networking
- live classes
- AI chatbot
- AI speaking evaluation
- complex gamification
- certificates
- leaderboard
- complex spaced repetition

unless explicitly approved.

---

# Expected Output Behavior

When working on domain-sensitive functionality:

- reference relevant requirements where useful
- mention affected business rules
- preserve role boundaries
- preserve Course ownership
- preserve Student learning history
- preserve shared Vocabulary semantics
- preserve Standard/Premium separation
- avoid unnecessary scope expansion

When a required business decision is missing, identify the missing decision rather than inventing it.
