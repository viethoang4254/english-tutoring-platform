# English Learning Platform — Requirements Specification

**Version:** 0.1
**Status:** Draft
**Project Type:** Graduation Project

---

# 1. Purpose

This document defines the functional and non-functional requirements of the English Learning Platform.

Requirement identifiers should remain stable whenever possible so that later documents, Use Cases, APIs, tests, and implementation tasks can reference them.

---

# 2. Actors

The system contains three primary authenticated roles:

- STUDENT
- TEACHER
- ADMIN

Students additionally have an access tier:

- STANDARD
- PREMIUM

Access tier is not a separate system role.

---

# 3. Student Account Requirements

## FR-STU-001 — Student Registration

The system shall allow a user to register a Student account.

## FR-STU-002 — Student Login

The system shall allow a registered Student to authenticate.

## FR-STU-003 — Student Logout

The system shall allow an authenticated Student to log out.

## FR-STU-004 — Student Profile

The system shall allow a Student to view their profile.

## FR-STU-005 — Update Student Profile

The system shall allow a Student to update permitted profile information.

## FR-STU-006 — Access Tier

The system shall identify a Student as having either STANDARD or PREMIUM access.

---

# 4. Learning Discovery Requirements

## FR-DIS-001 — Browse CEFR Levels

The system shall allow Students to browse supported CEFR levels:

- A1
- A2
- B1
- B2
- C1
- C2

## FR-DIS-002 — Browse Courses by Level

The system shall allow Students to browse Courses associated with a selected CEFR level.

## FR-DIS-003 — View Course Information

The system shall allow Students to view Course information before enrollment.

Course information may include:

- title
- description
- CEFR level
- Teacher
- number of Lessons
- access type
- enrollment information

## FR-DIS-004 — View Teacher Information

The system shall allow Students to view relevant information about the Teacher responsible for a Course.

---

# 5. Course Enrollment Requirements

## FR-ENR-001 — Enroll in Standard Course

An eligible Student shall be able to enroll in a Standard-access Course.

## FR-ENR-002 — Enroll in Premium Course

A Student with active Premium access shall be able to enroll in a Premium Course.

## FR-ENR-003 — Restrict Premium Enrollment

The system shall prevent a Standard Student from enrolling in a Premium-only Course unless Premium access is obtained.

## FR-ENR-004 — My Courses

The system shall provide Students with a list of Courses in which they are enrolled.

## FR-ENR-005 — Course Progress

The system shall track Student progress for enrolled Courses.

---

# 6. Lesson Requirements

## FR-LES-001 — View Course Lessons

An enrolled Student shall be able to view Lessons belonging to the Course.

## FR-LES-002 — Open Lesson

An eligible Student shall be able to open an accessible Lesson.

## FR-LES-003 — Topic-Based Lessons

Lessons shall primarily represent learning topics rather than only grammatical parts of speech.

## FR-LES-004 — Repeat Lesson Activity

The system shall allow Students to repeat permitted learning activities.

---

# 7. Vocabulary Learning Requirements

## FR-VOC-001 — View Lesson Vocabulary

Students shall be able to view vocabulary assigned to an accessible Lesson.

## FR-VOC-002 — Vocabulary Details

The system shall support displaying vocabulary information including, where available:

- word
- IPA
- pronunciation
- CEFR level
- selected sense
- part of speech
- English definition
- Vietnamese meaning
- example sentence

## FR-VOC-003 — Pronunciation Audio

Students shall be able to play pronunciation audio when audio is available.

## FR-VOC-004 — Multiple Vocabulary Senses

The system shall support vocabulary containing more than one sense.

## FR-VOC-005 — Lesson Vocabulary Sense

A Lesson shall be able to reference the vocabulary sense appropriate for its learning context.

---

# 8. Fill Word Requirements

## FR-FILL-001 — Fill Word Exercise

The system shall provide Fill Word exercises.

## FR-FILL-002 — Fill Word Hints

A Fill Word question may provide:

- IPA
- English definition
- partially hidden word

## FR-FILL-003 — Submit Fill Word Answer

The Student shall be able to submit a word as an answer.

## FR-FILL-004 — Evaluate Fill Word Answer

The system shall determine whether the submitted answer is correct.

## FR-FILL-005 — Record Fill Word Result

The system shall record relevant Student performance for the exercise attempt.

---

# 9. Listening Requirements

## FR-LIS-001 — Listening Exercise

The system shall provide vocabulary Listening exercises.

## FR-LIS-002 — Play Listening Audio

The Student shall be able to play the audio associated with a Listening question.

## FR-LIS-003 — Listen and Choose

The system may provide Listening questions where the Student chooses the correct vocabulary from multiple options.

## FR-LIS-004 — Listen and Type

The system may provide Listening questions where the Student types the vocabulary that was played.

## FR-LIS-005 — Evaluate Listening Answer

The system shall evaluate submitted Listening answers.

## FR-LIS-006 — Record Listening Result

The system shall record relevant Listening performance.

---

# 10. Quiz Requirements

## FR-QUIZ-001 — Vocabulary Quiz

The system shall provide vocabulary Quizzes.

## FR-QUIZ-002 — Word to Definition

Quiz questions may ask the Student to identify a definition from a vocabulary word.

## FR-QUIZ-003 — Definition to Word

Quiz questions may ask the Student to identify a vocabulary word from a definition.

## FR-QUIZ-004 — IPA to Word

Quiz questions may ask the Student to identify a word from IPA.

## FR-QUIZ-005 — Context Question

Quiz questions may evaluate vocabulary using a sentence or context.

## FR-QUIZ-006 — Submit Quiz

Students shall be able to submit a Quiz.

## FR-QUIZ-007 — Quiz Result

The system shall calculate and display the Quiz result.

## FR-QUIZ-008 — Record Quiz Attempt

The system shall retain relevant information about the Quiz attempt.

---

# 11. Score and Progress Requirements

## FR-PRO-001 — Exercise Score

The system shall calculate applicable exercise scores.

## FR-PRO-002 — Accuracy

The system shall support calculation of Student answer accuracy.

## FR-PRO-003 — Lesson Progress

The system shall track Student Lesson progress.

## FR-PRO-004 — Course Progress

The system shall track Student Course progress.

## FR-PRO-005 — Vocabulary Performance

The system shall retain sufficient vocabulary-level performance data to support mastery and review features.

## FR-PRO-006 — Vocabulary Mastery

The system shall support a vocabulary mastery indicator.

The exact mastery calculation shall be defined in Business Rules.

## FR-PRO-007 — Weak Vocabulary

The system shall support identification of weak vocabulary based on learning performance.

## FR-PRO-008 — Learning History

Students shall be able to view relevant historical learning activity.

---

# 12. Vocabulary Search Requirements

## FR-SEA-001 — Search Vocabulary

Students shall be able to search vocabulary.

## FR-SEA-002 — Search Result Details

Vocabulary search results shall provide available basic vocabulary information.

## FR-SEA-003 — Search Pronunciation

Students shall be able to play pronunciation from search results when available.

## FR-SEA-004 — Search Across Learning Levels

Vocabulary Search shall not require the Student to be currently studying the vocabulary's CEFR level.

---

# 13. Saved Vocabulary Requirements

## FR-SAV-001 — Save Vocabulary

Students shall be able to save vocabulary to a personal vocabulary collection.

## FR-SAV-002 — My Vocabulary

Students shall be able to view saved vocabulary through My Vocabulary.

## FR-SAV-003 — Remove Saved Vocabulary

Students shall be able to remove vocabulary from their saved collection.

## FR-SAV-004 — Practice Saved Vocabulary

The system should support using saved vocabulary as a source for vocabulary practice or review.

---

# 14. Review Requirements

## FR-REV-001 — Vocabulary Review

The system shall provide a vocabulary Review capability.

## FR-REV-002 — Weak Vocabulary Review

Weak vocabulary may be included in Review.

## FR-REV-003 — Saved Vocabulary Review

Saved vocabulary may be included in Review.

## FR-REV-004 — Incorrect Vocabulary Review

Vocabulary associated with incorrect answers may be included in Review.

## FR-REV-005 — Recently Learned Vocabulary

Recently learned vocabulary may be included in Review.

## FR-REV-006 — Repeat Review

Students shall be able to repeat available Review activities.

---

# 15. Premium Requirements

## FR-PRE-001 — View Premium Benefits

Standard Students shall be able to view information describing Premium benefits.

## FR-PRE-002 — Upgrade Premium

A Standard Student shall be able to initiate a Premium upgrade.

## FR-PRE-003 — Premium Content Access

Students with active Premium access shall be able to access content and functionality included in their Premium entitlement.

## FR-PRE-004 — Restrict Premium Features

The system shall prevent Standard Students from using Premium-only functionality.

## FR-PRE-005 — Preserve Student Data

Changing from Premium to Standard shall not delete Student learning data.

---

# 16. Subscription Requirements

## FR-SUB-001 — Subscription Plans

The system shall support Premium subscription plans.

The initial conceptual periods are:

- Monthly
- Yearly

## FR-SUB-002 — View Subscription

Students shall be able to view relevant subscription information.

## FR-SUB-003 — Active Subscription

The system shall determine whether a Student has active Premium access.

## FR-SUB-004 — Subscription Expiration

Expired Premium access shall return the Student to Standard access unless renewed.

## FR-SUB-005 — Preserve History After Expiration

Subscription expiration shall not delete:

- learning history
- Course progress
- Lesson progress
- saved vocabulary
- previous scores

---

# 17. Payment Requirements

## FR-PAY-001 — Initiate Payment

A Student upgrading to Premium shall be able to initiate a payment process.

## FR-PAY-002 — Record Transaction

The system shall retain relevant transaction information.

## FR-PAY-003 — Successful Payment

A successfully verified Premium payment shall activate the corresponding Premium entitlement.

## FR-PAY-004 — Failed Payment

A failed or unverified payment shall not activate Premium access.

## FR-PAY-005 — Payment Provider

The specific payment provider remains undecided and shall be selected later.

---

# 18. Teacher Account Requirements

## FR-TEA-001 — Teacher Authentication

Authorized Teachers shall be able to authenticate to the platform.

## FR-TEA-002 — Teacher Profile

Teachers shall be able to view their Teacher profile.

## FR-TEA-003 — Update Teacher Profile

Teachers shall be able to update permitted Teacher profile information.

---

# 19. Teacher Course Requirements

## FR-TCR-001 — Create Course

A Teacher shall be able to create a Course.

## FR-TCR-002 — Course CEFR Level

A Teacher shall select an existing platform CEFR level for a Course.

## FR-TCR-003 — Course Ownership

A newly created Course shall be associated with its primary Teacher.

## FR-TCR-004 — View Owned Courses

Teachers shall be able to view Courses they own.

## FR-TCR-005 — Update Owned Course

Teachers shall be able to update permitted information for Courses they own.

## FR-TCR-006 — Prevent Unauthorized Course Modification

A Teacher shall not modify another Teacher's Course unless explicitly authorized by a future administrative rule.

---

# 20. Teacher Lesson Requirements

## FR-TLE-001 — Create Lesson

A Teacher shall be able to create Lessons for an owned Course.

## FR-TLE-002 — Update Lesson

A Teacher shall be able to update Lessons belonging to an owned Course.

## FR-TLE-003 — Topic

A Teacher shall be able to define the learning topic of a Lesson.

## FR-TLE-004 — Add Vocabulary

A Teacher shall be able to add appropriate vocabulary to a Lesson.

## FR-TLE-005 — Select Vocabulary Sense

A Teacher shall be able to select the vocabulary sense used by the Lesson.

---

# 21. Teacher Vocabulary Requirements

## FR-TVOC-001 — Search Platform Vocabulary

Teachers shall be able to search existing vocabulary stored by the platform.

## FR-TVOC-002 — Reuse Vocabulary

Teachers shall be able to reuse appropriate existing vocabulary rather than creating unnecessary duplicates.

## FR-TVOC-003 — Dictionary Lookup

When suitable vocabulary is not available, the system should support retrieving vocabulary information from an approved Dictionary Provider.

## FR-TVOC-004 — Review Imported Vocabulary

Teachers shall be able to review relevant dictionary information before using it as learning content.

## FR-TVOC-005 — Dictionary Licensing

Dictionary integration shall respect the selected provider's licensing and storage restrictions.

---

# 22. Teacher Exercise Requirements

## FR-TEX-001 — Configure Fill Word

Teachers shall be able to configure Fill Word exercises for appropriate Lesson vocabulary.

## FR-TEX-002 — Configure Listening

Teachers shall be able to configure Listening exercises for appropriate Lesson vocabulary.

## FR-TEX-003 — Configure Quiz

Teachers shall be able to configure vocabulary Quiz content.

## FR-TEX-004 — Exercise Vocabulary

Exercises shall reference vocabulary associated with the relevant learning content.

---

# 23. Teacher Analytics Requirements

## FR-TAN-001 — Teacher Dashboard

Teachers shall have access to analytics related to their educational content.

## FR-TAN-002 — Course Enrollment Statistics

Teachers should be able to view enrollment statistics for owned Courses.

## FR-TAN-003 — Course Progress Statistics

Teachers should be able to view learning-progress information for owned Courses.

## FR-TAN-004 — Average Performance

Teachers should be able to view aggregated performance information for owned Courses.

## FR-TAN-005 — Difficult Vocabulary

The system should support identifying vocabulary that Students commonly struggle with in a Teacher's Course.

## FR-TAN-006 — Revenue Restriction

Teachers shall not have access to platform-wide revenue analytics.

---

# 24. Admin User Management Requirements

## FR-ADM-001 — Admin Authentication

Authorized Admins shall be able to authenticate.

## FR-ADM-002 — View Users

Admins shall be able to view platform users.

## FR-ADM-003 — Manage Student Accounts

Admins shall be able to perform permitted management actions on Student accounts.

## FR-ADM-004 — Manage Teacher Accounts

Admins shall be able to perform permitted management actions on Teacher accounts.

---

# 25. Admin Course Requirements

## FR-ACR-001 — View Courses

Admins shall be able to view Courses across the platform.

## FR-ACR-002 — Course Oversight

Admins shall have platform-level Course oversight capabilities.

## FR-ACR-003 — Course Access Classification

The system shall support classification of Course access as Standard or Premium.

The exact authorization for changing this classification shall be defined in Business Rules.

---

# 26. Admin Subscription Requirements

## FR-ASU-001 — View Subscriptions

Admins shall be able to view relevant subscription information.

## FR-ASU-002 — Subscription Statistics

Admins shall be able to view subscription statistics.

## FR-ASU-003 — Active Premium Count

Admins shall be able to view the number of active Premium Students.

---

# 27. Admin Transaction Requirements

## FR-ATR-001 — View Transactions

Admins shall be able to view relevant platform transactions.

## FR-ATR-002 — Transaction Status

Admins shall be able to view transaction status.

## FR-ATR-003 — Transaction Time

Admins shall be able to view when a transaction occurred.

---

# 28. Admin Analytics Requirements

## FR-AAN-001 — Total Users

Admins shall be able to view the total number of users.

## FR-AAN-002 — Student Statistics

Admins shall be able to view Student statistics.

## FR-AAN-003 — Teacher Statistics

Admins shall be able to view Teacher statistics.

## FR-AAN-004 — Course Statistics

Admins shall be able to view Course statistics.

## FR-AAN-005 — Enrollment Statistics

Admins should be able to view platform enrollment statistics.

## FR-AAN-006 — Learning Statistics

Admins should be able to view aggregated platform learning statistics.

---

# 29. Revenue Requirements

## FR-REVN-001 — Revenue Statistics

Admins shall be able to view platform revenue statistics.

## FR-REVN-002 — Revenue by Time

Admins shall be able to view revenue over a selected time period.

## FR-REVN-003 — Monthly Revenue

The system shall support monthly revenue reporting.

## FR-REVN-004 — Yearly Revenue

The system shall support yearly revenue reporting.

## FR-REVN-005 — Revenue Source

The initial platform revenue source shall be Student Premium subscriptions.

---

# 30. Authorization Requirements

## FR-AUTH-001 — Role-Based Access

The system shall enforce access based on authenticated user role.

## FR-AUTH-002 — Student Authorization

Students shall not access Teacher or Admin management functionality.

## FR-AUTH-003 — Teacher Authorization

Teachers shall not access Admin-only functionality.

## FR-AUTH-004 — Course Ownership Authorization

Teacher Course modification shall respect Course ownership.

## FR-AUTH-005 — Premium Authorization

Premium functionality shall verify active Premium entitlement.

## FR-AUTH-006 — Server-Side Enforcement

Authorization shall be enforced by the backend and shall not rely solely on frontend visibility.

---

# 31. Responsive Web Requirements

## NFR-UI-001 — Web Platform

The system shall be accessible through modern web browsers.

## NFR-UI-002 — Student Mobile-First

Student interfaces shall use a mobile-first design approach.

## NFR-UI-003 — Student Responsive Design

Student interfaces shall remain usable on tablet and desktop screen sizes.

## NFR-UI-004 — Teacher Responsive Design

Teacher interfaces shall be responsive and functional on mobile while optimized primarily for desktop management.

## NFR-UI-005 — Admin Responsive Design

Admin interfaces shall be responsive and functional on mobile while optimized primarily for desktop management.

## NFR-UI-006 — Touch Interaction

Student learning interactions should be suitable for touch-screen devices.

---

# 32. Security Requirements

## NFR-SEC-001 — Password Protection

Passwords shall not be stored in plaintext.

## NFR-SEC-002 — Authentication Protection

Protected resources shall require valid authentication.

## NFR-SEC-003 — Authorization Validation

Sensitive operations shall verify authorization.

## NFR-SEC-004 — Input Validation

User-provided input shall be validated.

## NFR-SEC-005 — Secret Management

API keys, payment credentials, database credentials, and other secrets shall not be committed directly to source control.

## NFR-SEC-006 — Payment Verification

Premium access shall not be granted solely from untrusted frontend payment information.

---

# 33. Data Requirements

## NFR-DATA-001 — Learning Data Persistence

Relevant Student learning information shall persist across sessions.

## NFR-DATA-002 — Subscription Expiration

Subscription expiration shall not delete Student learning data.

## NFR-DATA-003 — Vocabulary Reuse

The data model should support vocabulary reuse across Lessons.

## NFR-DATA-004 — Multiple Senses

The data model shall support multiple senses for a vocabulary word.

## NFR-DATA-005 — Historical Attempts

The system shall retain sufficient attempt information to support learning history and progress calculation.

---

# 34. Maintainability Requirements

## NFR-MNT-001 — Separation of Concerns

The system should maintain clear separation between presentation, application/business logic, and data access responsibilities.

## NFR-MNT-002 — API Documentation

Backend APIs should be documented.

## NFR-MNT-003 — Testability

Important business logic should be designed so that it can be tested.

## NFR-MNT-004 — Configuration

Environment-specific configuration shall not be unnecessarily hardcoded.

## NFR-MNT-005 — Scope Simplicity

The architecture should remain appropriate for a graduation project and avoid unnecessary distributed-system complexity.

---

# 35. Performance Requirements

## NFR-PERF-001 — Normal Interaction

Common Student learning interactions should respond within a reasonable time under expected project-scale usage.

## NFR-PERF-002 — Vocabulary Search

Vocabulary search should return results within a reasonable interactive response time.

## NFR-PERF-003 — Audio Loading

Pronunciation audio should be delivered in a manner suitable for normal mobile-web learning.

Exact measurable performance targets will be defined after architecture and deployment assumptions are known.

---

# 36. External Integration Requirements

## INT-DIC-001 — Dictionary Provider

The system may integrate with an approved external Dictionary Provider.

## INT-DIC-002 — Dictionary Data

The integration may retrieve:

- pronunciation
- IPA
- definitions
- part of speech
- examples
- audio

depending on provider capabilities.

## INT-DIC-003 — Provider License

Provider licensing shall be reviewed before final implementation.

## INT-PAY-001 — Payment Provider

The system shall eventually integrate with an approved payment provider for Premium subscriptions.

The provider has not yet been selected.

---

# 37. Traceability

Future artifacts should reference requirement IDs where practical.

Examples:

Use Case:

`UC-STU-LEARN-01 → FR-VOC-001, FR-VOC-002, FR-VOC-003`

API:

`POST /quiz/{id}/submit → FR-QUIZ-006, FR-QUIZ-007, FR-QUIZ-008`

Test:

`TC-PRE-003 → FR-PRE-003, FR-AUTH-005`

This allows:

Requirement
→ Use Case
→ Design
→ API
→ Implementation
→ Test

to remain traceable.

---

# 38. Requirements Pending Business Rules

The following requirements require additional Business Rules before they are considered fully specified:

- Standard versus Premium feature access
- Course Standard/Premium classification
- Lesson completion
- Course completion
- score calculation
- vocabulary mastery calculation
- weak vocabulary threshold
- Review selection
- subscription activation
- subscription expiration
- payment verification
- Course publishing
- Teacher permissions
- Admin moderation permissions

These items shall be addressed in `BUSINESS_RULES.md`.
