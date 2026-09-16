# English Learning Platform — Business Rules

**Version:** 0.1
**Status:** Draft
**Project Type:** Graduation Project

---

# 1. Purpose

This document defines the core business rules of the English Learning Platform.

Business Rules describe how the system behaves independently from implementation technology.

Requirement IDs from `REQUIREMENTS.md` should be referenced where appropriate.

---

# 2. Role Rules

## BR-ROLE-001 — Primary Roles

The system contains three primary roles:

- STUDENT
- TEACHER
- ADMIN

A user's authorization shall be determined by their assigned role.

---

## BR-ROLE-002 — Student Access Tier

STANDARD and PREMIUM are Student access tiers, not system roles.

A Student remains a STUDENT regardless of subscription status.

---

## BR-ROLE-003 — Teacher Access

Teachers do not require a Premium subscription to perform Teacher functions.

---

## BR-ROLE-004 — Admin Access

Admins do not require a Premium subscription to perform Admin functions.

---

# 3. CEFR Rules

## BR-CEFR-001 — Supported Levels

The platform supports:

- A1
- A2
- B1
- B2
- C1
- C2

---

## BR-CEFR-002 — Platform Managed Levels

CEFR Levels belong to the platform.

Teachers shall not create arbitrary CEFR Levels.

---

## BR-CEFR-003 — Course Level

Every published Course must belong to exactly one CEFR Level.

---

# 4. Teacher and Course Rules

## BR-COURSE-001 — Course Ownership

Every Course has exactly one primary Teacher.

---

## BR-COURSE-002 — Multiple Courses

A Teacher may own multiple Courses.

---

## BR-COURSE-003 — Multiple Teachers

The platform may contain multiple Teachers.

---

## BR-COURSE-004 — Teacher Modification

A Teacher may modify Courses they own.

A Teacher shall not modify another Teacher's Course.

Admin permissions are handled separately.

---

## BR-COURSE-005 — Course Access Type

A Course shall have one access classification:

- STANDARD
- PREMIUM

---

## BR-COURSE-006 — Course Access Classification Authority

Teachers create educational content but do not make the final business decision regarding whether a Course is Standard or Premium.

Admin controls the final Course access classification.

---

# 5. Course Status Rules

Courses shall support at least the following lifecycle states:

- DRAFT
- PUBLISHED
- ARCHIVED

---

## BR-CSTATUS-001 — Draft Course

A newly created Course starts as DRAFT unless explicitly published through an authorized action.

---

## BR-CSTATUS-002 — Published Course

Only PUBLISHED Courses are discoverable by Students.

---

## BR-CSTATUS-003 — Archived Course

An ARCHIVED Course shall not accept new enrollments.

Existing learning records associated with the Course shall not be deleted merely because the Course is archived.

---

# 6. Enrollment Rules

## BR-ENR-001 — Enrollment Required

A Student must enroll in a Course before the Course becomes part of My Courses and tracked Course learning.

---

## BR-ENR-002 — Standard Course

Both Standard and Premium Students may enroll in a STANDARD Course.

---

## BR-ENR-003 — Premium Course

Only Students with active Premium access may enroll in a PREMIUM Course.

---

## BR-ENR-004 — Duplicate Enrollment

A Student shall not have multiple active enrollment records for the same Course.

---

## BR-ENR-005 — Premium Expiration

If a Student enrolled in a Premium Course and Premium later expires:

- the enrollment record remains
- previous learning history remains
- previous progress remains
- Premium Course learning access becomes restricted

Access may resume if Premium becomes active again.

---

# 7. Lesson Rules

## BR-LESSON-001 — Course Relationship

Every Lesson belongs to exactly one Course.

---

## BR-LESSON-002 — Topic-Based Organization

Lessons should primarily represent learning topics.

Examples:

- Family
- Food & Drinks
- Travel
- Technology

Part of speech shall not be the primary Lesson hierarchy.

---

## BR-LESSON-003 — Lesson Order

Lessons within a Course shall support an explicit display order.

---

## BR-LESSON-004 — Repeat Learning

Students may repeat accessible Lesson learning activities.

Repeating an activity shall not delete previous attempt history.

---

# 8. Lesson Completion

For the initial project version, Lesson learning consists primarily of:

1. Learn Vocabulary
2. Fill Word
3. Listening
4. Quiz

---

## BR-LCOMP-001 — Flexible Learning

Students may revisit completed learning sections.

The platform shall not permanently lock earlier activities after completion.

---

## BR-LCOMP-002 — Lesson Completion Condition

A Lesson is considered completed when the Student has completed all required assessment sections configured for that Lesson.

For the initial core design, the expected required sections are:

- Fill Word
- Listening
- Quiz

Viewing the Learn Vocabulary section alone does not complete the Lesson.

---

## BR-LCOMP-003 — Reattempt

Students may reattempt completed assessment sections.

The system shall retain relevant attempt history.

---

# 9. Course Completion

## BR-CCOMP-001 — Course Completion

A Course is considered completed for a Student when all required Lessons in that Course are completed.

---

## BR-CCOMP-002 — Progress Percentage

Initial Course progress may be calculated as:

Completed Required Lessons / Total Required Lessons × 100

Example:

8 completed Lessons / 10 Lessons = 80%

This rule may later be refined if optional Lessons are introduced.

---

# 10. Vocabulary Rules

## BR-VOC-001 — Shared Vocabulary

Vocabulary is a shared platform resource.

Teachers should reuse existing vocabulary where appropriate.

---

## BR-VOC-002 — Duplicate Prevention

The system should avoid unnecessary duplicate vocabulary records representing the same canonical English word.

---

## BR-VOC-003 — Multiple Senses

One Vocabulary entry may contain multiple Vocabulary Senses.

---

## BR-VOC-004 — Sense Information

A Vocabulary Sense may contain:

- part of speech
- English definition
- Vietnamese meaning
- example sentence

---

## BR-VOC-005 — Lesson Sense Selection

When vocabulary has multiple senses, a Lesson should identify which sense is being taught in that Lesson.

---

## BR-VOC-006 — CEFR

Vocabulary may have a CEFR classification where reliable data is available.

Absence of CEFR metadata shall not automatically make the vocabulary unusable.

---

# 11. Dictionary Import Rules

## BR-DIC-001 — Platform Search First

Teacher vocabulary workflow should search the platform vocabulary database before requesting external dictionary data.

---

## BR-DIC-002 — External Lookup

External Dictionary lookup should be used when suitable vocabulary data does not already exist.

---

## BR-DIC-003 — Teacher Review

Dictionary results shall not automatically become approved learning content without appropriate Teacher review.

---

## BR-DIC-004 — Licensing

Storage, caching, redistribution, and audio usage shall follow the selected Dictionary Provider's license.

No implementation shall assume permanent storage rights before provider selection.

---

# 12. Vocabulary Search Rules

## BR-SEARCH-001 — Student Search

Both Standard and Premium Students may search vocabulary.

Vocabulary Search is not a Premium-only feature.

---

## BR-SEARCH-002 — Basic Search Information

Standard Students should receive useful basic information where available:

- word
- IPA
- part of speech
- basic definition
- Vietnamese meaning
- basic example
- pronunciation audio
- CEFR level

---

## BR-SEARCH-003 — Premium Detail

Premium may provide additional vocabulary depth such as:

- multiple advanced senses
- collocations
- synonyms
- antonyms
- word families
- additional examples

The exact Premium vocabulary dataset depends on available licensed data.

---

# 13. Saved Vocabulary Rules

## BR-SAVE-001 — Save Vocabulary

Authenticated Students may save vocabulary to My Vocabulary.

---

## BR-SAVE-002 — Duplicate Saved Word

The same Student shall not have duplicate active saved records for the same Vocabulary entry.

---

## BR-SAVE-003 — Remove Vocabulary

Removing a word from My Vocabulary removes it from the Student's saved collection.

It shall not delete the shared Vocabulary record.

---

## BR-SAVE-004 — Subscription Independence

Saved Vocabulary shall remain stored if Premium expires.

---

# 14. Exercise Rules

The initial core exercise types are:

- Fill Word
- Listening
- Quiz

---

## BR-EX-001 — Lesson Vocabulary

Exercise questions should use vocabulary relevant to the associated Lesson.

---

## BR-EX-002 — Reattempts

Students may perform multiple attempts.

Previous relevant attempts shall remain available for history and analytics.

---

## BR-EX-003 — Correctness

Each answerable exercise question shall have sufficient information for the system to determine correctness.

---

# 15. Fill Word Rules

## BR-FILL-001 — Objective

Fill Word primarily evaluates spelling and vocabulary recall.

---

## BR-FILL-002 — Question Information

A Fill Word question may include:

- IPA
- English definition
- partially hidden word

---

## BR-FILL-003 — Answer Comparison

For the initial version:

- leading/trailing whitespace is ignored
- comparison is case-insensitive
- spelling must otherwise match the expected answer

Example:

Expected:

`beautiful`

Accepted:

`Beautiful`

`beautiful`

Not accepted:

`beautifull`

---

# 16. Listening Rules

## BR-LIS-001 — Audio

Listening questions require playable pronunciation audio.

---

## BR-LIS-002 — Supported Formats

Initial Listening formats may include:

- LISTEN_AND_CHOOSE
- LISTEN_AND_TYPE

---

## BR-LIS-003 — Listen and Type Comparison

Listen-and-Type answers follow the same basic normalization as Fill Word unless otherwise specified.

---

# 17. Quiz Rules

## BR-QUIZ-001 — Question Types

Initial Quiz question concepts may include:

- WORD_TO_DEFINITION
- DEFINITION_TO_WORD
- IPA_TO_WORD
- CONTEXT_TO_WORD

---

## BR-QUIZ-002 — Question Evaluation

Every Quiz question shall have a defined correct answer or correct option.

---

# 18. Score Rules

For the initial project version, scoring should remain understandable and demonstrable.

---

## BR-SCORE-001 — Basic Score

For exercises consisting of equally weighted questions:

Score Percentage = Correct Answers / Total Questions × 100

Example:

8 correct / 10 questions = 80%

---

## BR-SCORE-002 — Score Range

Scores shall be represented from 0 to 100.

---

## BR-SCORE-003 — Reattempt Scores

Every completed attempt may retain its own score.

---

## BR-SCORE-004 — Best Score

The system may use the highest completed score as the Student's displayed best score for an exercise.

Attempt history shall still be retained.

---

# 19. Accuracy Rules

## BR-ACC-001 — Basic Accuracy

Accuracy may initially be calculated as:

Correct Answers / Total Answered Questions × 100

---

# 20. Vocabulary Performance Rules

The system should track Student performance at vocabulary level.

Relevant events may include:

- correct answer
- incorrect answer
- exercise type
- attempt time

---

# 21. Vocabulary Mastery

A simple initial mastery model shall be used before introducing advanced learning algorithms.

---

## BR-MAST-001 — Initial Mastery

Initial Vocabulary Mastery may be calculated from the Student's answer history for that vocabulary.

Conceptually:

Correct Vocabulary Answers / Total Vocabulary Answers × 100

---

## BR-MAST-002 — Minimum Evidence

A vocabulary item should not be treated as strongly mastered based on a single correct answer.

The UI may distinguish insufficient learning data from established mastery.

---

## BR-MAST-003 — Mastery Categories

The initial conceptual categories are:

- 0–49% → Weak
- 50–79% → Learning
- 80–100% → Mastered

These thresholds are initial project defaults and may be tuned after testing.

---

# 22. Weak Vocabulary

## BR-WEAK-001 — Weak Threshold

Vocabulary with established mastery below 50% may be classified as Weak.

---

## BR-WEAK-002 — Insufficient Data

Vocabulary with insufficient attempt data should not automatically be classified as Weak.

---

## BR-WEAK-003 — Review Source

Weak vocabulary may become a source for Review activities.

---

# 23. Review Rules

## BR-REV-001 — Review Sources

Review may select vocabulary from:

- Weak Vocabulary
- Saved Vocabulary
- Incorrect Answers
- Recently Learned Vocabulary

---

## BR-REV-002 — No Complex SRS Requirement

The initial version does not require a full spaced-repetition scheduling algorithm.

---

## BR-REV-003 — Premium Review

Basic Review may be available to Standard Students.

More advanced personalized Weak Vocabulary practice and analytics may be Premium.

---

# 24. Learning History Rules

## BR-HIST-001 — Attempt History

Relevant completed exercise attempts shall be retained.

---

## BR-HIST-002 — Historical Information

Learning History may include:

- activity
- Course
- Lesson
- score
- attempt time

---

## BR-HIST-003 — Subscription Independence

Learning History shall remain available as stored learning data when Premium expires, subject to the Standard/Premium display rules defined by the product.

---

# 25. Standard Access Rules

Standard is intended to provide genuine educational value.

Standard Students may access core features including:

- account
- Course discovery
- Standard Course enrollment
- basic vocabulary learning
- IPA
- pronunciation audio where available
- English definition
- Vietnamese meaning
- example
- Fill Word
- basic Listening
- basic Quiz
- Vocabulary Search
- My Vocabulary
- basic Review
- basic Progress

---

# 26. Premium Access Rules

Premium includes all Standard functionality plus eligible Premium capabilities.

Premium may include:

- Premium Courses
- specialized Courses
- deeper vocabulary information
- advanced Listening
- advanced exercises
- advanced Weak Vocabulary practice
- personalized Review
- detailed learning analytics

---

## BR-PRE-001 — CEFR Availability

The system shall not assume that entire CEFR Levels are Premium-only.

The platform may provide Standard content at multiple CEFR levels.

---

## BR-PRE-002 — Premium Entitlement

Premium functionality requires active Premium entitlement.

---

# 27. Subscription Rules

## BR-SUB-001 — Subscription Periods

The initial subscription plan periods are:

- MONTHLY
- YEARLY

---

## BR-SUB-002 — Active Premium

A Student is Premium only when the system determines that an applicable Premium entitlement is active.

---

## BR-SUB-003 — Expiration

When the Premium entitlement expires, the Student returns to Standard access.

---

## BR-SUB-004 — Preserve Data

Premium expiration shall not delete:

- account
- enrollments
- progress
- scores
- attempts
- saved vocabulary
- learning history

---

## BR-SUB-005 — Premium Course Restriction

After expiration, Premium Course content becomes inaccessible until Premium access is restored.

Historical enrollment and progress remain stored.

---

# 28. Payment Rules

## BR-PAY-001 — Verified Payment

Premium access shall only be activated after payment success has been verified by trusted backend logic.

---

## BR-PAY-002 — Frontend Trust

A frontend success screen alone shall never be sufficient evidence of payment.

---

## BR-PAY-003 — Transaction Record

Relevant payment attempts shall produce or update appropriate transaction records according to the selected provider workflow.

---

## BR-PAY-004 — Successful Transaction

A verified successful transaction may activate the corresponding subscription.

---

## BR-PAY-005 — Failed Transaction

A failed transaction shall not activate Premium.

---

# 29. Teacher Content Rules

## BR-TEA-001 — Owned Content

Teachers primarily manage educational content associated with their own Courses.

---

## BR-TEA-002 — Course Analytics

Teachers may access learning analytics for Courses they own.

---

## BR-TEA-003 — Student Privacy

Teacher analytics should prioritize aggregated Course learning information.

Detailed Student information shall only be exposed where required for legitimate educational functionality.

---

## BR-TEA-004 — Revenue

Teachers shall not access platform-wide revenue analytics.

---

# 30. Admin Rules

## BR-ADM-001 — Platform Management

Admins have platform-level management capabilities according to authorized Admin functions.

---

## BR-ADM-002 — Course Oversight

Admins may oversee Courses across Teachers.

---

## BR-ADM-003 — Access Classification

Admins control the final Standard/Premium classification of Courses.

---

## BR-ADM-004 — Subscription Oversight

Admins may view platform subscription information.

---

## BR-ADM-005 — Transaction Oversight

Admins may view relevant transaction information.

---

## BR-ADM-006 — Revenue Analytics

Admins may view platform revenue analytics.

---

# 31. Revenue Rules

## BR-REVN-001 — Revenue Source

The initial platform revenue source is Premium Student subscriptions.

---

## BR-REVN-002 — Successful Payments

Revenue calculations shall be based on verified successful payment transactions.

---

## BR-REVN-003 — Revenue Time

Revenue reporting shall support aggregation over time.

Examples:

- day
- month
- year
- custom date range

---

# 32. Data Preservation Rules

## BR-DATA-001 — Learning Data

Student learning data shall not be deleted simply because:

- Premium expires
- Course becomes archived
- Student stops learning temporarily

---

## BR-DATA-002 — Shared Vocabulary

Deleting a Lesson association shall not automatically delete a shared Vocabulary record that may be used elsewhere.

---

## BR-DATA-003 — Historical Integrity

Historical attempts should remain associated with the learning content context needed to interpret those attempts.

---

# 33. Authorization Rules

## BR-AUTH-001 — Backend Enforcement

All important authorization rules shall be enforced by the backend.

---

## BR-AUTH-002 — Frontend Restrictions

Frontend hiding or disabling controls is a user-experience mechanism and shall not replace backend authorization.

---

## BR-AUTH-003 — Teacher Ownership

Teacher Course and Lesson modification shall verify Teacher ownership.

---

## BR-AUTH-004 — Premium Access

Premium content access shall verify active Premium entitlement.

---

# 34. Responsive Web Rules

## BR-WEB-001 — Student

Student UI shall be mobile-first.

---

## BR-WEB-002 — Teacher

Teacher UI shall be desktop-optimized but responsive and usable on mobile.

---

## BR-WEB-003 — Admin

Admin UI shall be desktop-optimized but responsive and usable on mobile.

---

## BR-WEB-004 — No Native App Requirement

No native Android or iOS application is required for the initial project.

---

# 35. Scope Rules

The following are not part of the initial core scope unless explicitly approved later:

- Teacher marketplace
- Teacher commission
- Teacher payout
- live classes
- video calling
- social networking
- forum
- native mobile application
- AI chatbot
- AI speaking assessment
- leaderboard
- complex gamification
- certificates
- placement testing
- complex spaced repetition

---

# 36. Rules Still Requiring Future Decisions

The following remain open and should not be invented during implementation:

1. Exact Dictionary Provider
2. Dictionary storage/caching rights
3. Exact pronunciation audio strategy
4. TTS provider
5. Exact payment provider
6. Premium pricing
7. Exact specialized Premium Courses
8. Final Premium vocabulary dataset
9. Refund handling
10. Subscription renewal behavior
11. Detailed content moderation workflow
12. Teacher account creation/approval workflow
13. Admin account creation workflow
14. Final mastery formula after validation
15. Detailed Review-selection algorithm

Any implementation depending on these decisions should first request or document the missing decision.
