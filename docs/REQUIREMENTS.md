# English Learning Platform — Requirements Specification

**Version:** 0.2

**Status:** Draft

**Project Type:** Graduation Project

---

# 1. Purpose

This document defines the functional and non-functional requirements of the English Learning Platform.

Requirement identifiers should remain stable whenever possible so that later documents, Use Cases, APIs, tests, and implementation tasks can reference them.

Requirements describe what the system must provide.

Detailed implementation decisions belong primarily to architecture, database, API, UI/UX, and implementation documents.

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

Do not model STANDARD and PREMIUM as security roles.

Public registration is intended for Students.

Teacher and Admin accounts must be provisioned through authorized workflows.

---

# 3. Student Account Requirements

## FR-STU-001 — Student Registration

The system shall allow a user to register a Student account.

Student registration shall require fullName according to `BR-PROFILE-001`.

A publicly registering user shall not be able to select TEACHER or ADMIN as their role.

A newly registered Student shall initially receive STANDARD access unless an approved Business Rule specifies otherwise.

## FR-STU-002 — Student Login

The system shall allow a registered Student to authenticate.

## FR-STU-003 — Student Logout

The system shall allow an authenticated Student to log out.

Logout shall invalidate or revoke applicable authentication session/Refresh Token state according to the approved authentication design.

## FR-STU-004 — Student Profile

The system shall allow a Student to view their profile, including fullName and
optional avatarUrl.

## FR-STU-005 — Update Student Profile

The system shall allow a Student to update only their own fullName and avatarUrl
according to `BR-PROFILE-001`, using authenticated identity resolution.

## FR-STU-006 — Access Tier

The system shall identify a Student as having either STANDARD or PREMIUM access.

Access tier shall remain separate from the Student's security role.

---

# 4. Authentication and Account Security Requirements

## FR-ACC-001 — JWT Authentication

The system shall support JWT-based authentication for authenticated web access.

## FR-ACC-002 — Access Token

The system shall issue an authentication Access Token after successful authentication according to the approved security design.

The Access Token shall have a limited lifetime.

## FR-ACC-003 — Refresh Token

The system shall support Refresh Tokens for obtaining new Access Tokens without requiring the user to repeatedly provide credentials while the Refresh Token remains valid.

## FR-ACC-004 — Refresh Access Token

The system shall allow a valid Refresh Token to be used to request a new Access Token.

The backend shall validate the Refresh Token before issuing a new Access Token.

## FR-ACC-005 — Reject Invalid Refresh Token

Expired, invalid, revoked, or otherwise unusable Refresh Tokens shall not result in a valid new Access Token.

## FR-ACC-006 — Refresh Token Revocation

The system shall support invalidating or revoking Refresh Tokens when required.

Relevant situations may include:

- logout
- security-sensitive account changes
- account disablement
- token rotation
- other approved security events

Refresh validation, rotation support, and logout invalidation follow `BR-AUTHN-009` through `BR-AUTHN-011`; broader revocation and session-invalidation details remain unresolved.

## FR-ACC-007 — Current User

The system shall allow an authenticated user to retrieve permitted information about the currently authenticated account.

## FR-ACC-008 — Email Verification

The system shall support verification of a Student's registered email address.

Email verification and permitted PENDING_VERIFICATION access follow `BR-AUTHN-004` through `BR-AUTHN-006`.

## FR-ACC-009 — Resend Email Verification

The system should allow an eligible user to request a new email-verification message when verification has not yet been completed.

Rate limiting and expiration rules shall be defined during security design.

## FR-ACC-010 — Change Password

An authenticated user shall be able to change their password after satisfying the required security checks.

## FR-ACC-011 — Forgot Password

The system shall allow a user who cannot access their account password to initiate an approved password-recovery process.

## FR-ACC-012 — Reset Password

The system shall allow an eligible user to reset their password using a valid password-reset credential.

## FR-ACC-013 — Password Reset Expiration

Expired, invalid, previously used, or otherwise unusable password-reset credentials shall not permit a password reset.

## FR-ACC-014 — Account Status

The system shall support account status sufficient to control account access.

The initial account states defined by `BR-AUTHN-017` are:

- PENDING_VERIFICATION
- ACTIVE
- LOCKED
- DISABLED

Initial transitions and access restrictions follow `BR-AUTHN-004`, `BR-AUTHN-005`, and `BR-AUTHN-017` through `BR-AUTHN-019`; detailed administrative unlock/enable workflows remain unresolved.

## FR-ACC-015 — Disabled Account Restriction

An account that is disabled according to approved account rules shall not be permitted to authenticate or access protected functionality.

## FR-ACC-016 — Role Preservation

Authentication and token-refresh operations shall not allow a client to assign or elevate its own role.

Role information shall originate from trusted backend state.

## FR-ACC-017 — Premium Separation

Authentication shall not treat PREMIUM as a security role.

Premium entitlement shall be determined separately from STUDENT, TEACHER, and ADMIN role authorization.

---

# 5. Learning Discovery Requirements

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

# 6. Course Enrollment Requirements

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

# 7. Lesson Requirements

## FR-LES-001 — View Course Lessons

An enrolled Student shall be able to view Lessons belonging to the Course.

## FR-LES-002 — Open Lesson

An eligible Student shall be able to open an accessible Lesson.

## FR-LES-003 — Topic-Based Lessons

Lessons shall primarily represent learning topics rather than only grammatical parts of speech.

## FR-LES-004 — Repeat Lesson Activity

The system shall allow Students to repeat permitted learning activities.

---

# 8. Vocabulary Learning Requirements

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

# 9. Fill Word Requirements

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

Authoritative evaluation for persisted learning results shall be performed or verified by trusted backend logic.

## FR-FILL-005 — Record Fill Word Result

The system shall record relevant Student performance for the exercise attempt.

---

# 10. Listening Requirements

## FR-LIS-001 — Listening Exercise

The system shall provide vocabulary Listening exercises.

## FR-LIS-002 — Play Listening Audio

The Student shall be able to play the audio associated with a Listening question.

Listening functionality shall account for cases where playable audio is unavailable.

## FR-LIS-003 — Listen and Choose

The system may provide Listening questions where the Student chooses the correct vocabulary from multiple options.

## FR-LIS-004 — Listen and Type

The system may provide Listening questions where the Student types the vocabulary that was played.

## FR-LIS-005 — Evaluate Listening Answer

The system shall evaluate submitted Listening answers.

Authoritative evaluation for persisted results shall be performed or verified by trusted backend logic.

## FR-LIS-006 — Record Listening Result

The system shall record relevant Listening performance.

---

# 11. Quiz Requirements

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

The authoritative persisted Quiz result shall be calculated or verified by trusted backend logic.

## FR-QUIZ-008 — Record Quiz Attempt

The system shall retain relevant information about the Quiz attempt.

---

# 12. Score and Progress Requirements

## FR-PRO-001 — Exercise Score

The system shall calculate applicable exercise scores.

Persisted scores shall not rely solely on score values supplied by the frontend.

## FR-PRO-002 — Accuracy

The system shall support calculation of Student answer accuracy.

## FR-PRO-003 — Lesson Progress

The system shall track Student Lesson progress.

## FR-PRO-004 — Course Progress

The system shall track Student Course progress.

## FR-PRO-005 — Vocabulary Performance

The system shall retain sufficient vocabulary-level performance data to support mastery and Review features.

## FR-PRO-006 — Vocabulary Mastery

The system shall support a vocabulary mastery indicator.

The exact mastery calculation shall be defined in Business Rules.

## FR-PRO-007 — Weak Vocabulary

The system shall support identification of weak vocabulary based on learning performance.

## FR-PRO-008 — Learning History

Students shall be able to view relevant historical learning activity.

---

# 13. Vocabulary Search Requirements

## FR-SEA-001 — Search Vocabulary

Students shall be able to search vocabulary.

## FR-SEA-002 — Search Result Details

Vocabulary search results shall provide available basic vocabulary information.

## FR-SEA-003 — Search Pronunciation

Students shall be able to play pronunciation from search results when available.

## FR-SEA-004 — Search Across Learning Levels

Vocabulary Search shall not require the Student to be currently studying the vocabulary's CEFR level.

## FR-SEA-005 — Standard Vocabulary Search

Basic Vocabulary Search shall be available to Standard Students.

Premium may provide additional vocabulary information according to Business Rules.

---

# 14. Saved Vocabulary Requirements

## FR-SAV-001 — Save Vocabulary

Students shall be able to save vocabulary to a personal vocabulary collection.

## FR-SAV-002 — My Vocabulary

Students shall be able to view saved vocabulary through My Vocabulary.

## FR-SAV-003 — Remove Saved Vocabulary

Students shall be able to remove vocabulary from their saved collection.

## FR-SAV-004 — Practice Saved Vocabulary

The system should support using saved vocabulary as a source for vocabulary practice or Review.

---

# 15. Review Requirements

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

# 16. Premium Requirements

## FR-PRE-001 — View Premium Benefits

Standard Students shall be able to view information describing Premium benefits.

## FR-PRE-002 — Upgrade Premium

A Standard Student shall be able to initiate a Premium upgrade.

## FR-PRE-003 — Premium Content Access

Students with active Premium access shall be able to access content and functionality included in their Premium entitlement.

## FR-PRE-004 — Restrict Premium Features

The system shall prevent Standard Students from using Premium-only functionality.

Premium restriction shall be enforced by trusted backend authorization where protected resources are involved.

## FR-PRE-005 — Preserve Student Data

Changing from Premium to Standard shall not delete Student learning data.

## FR-PRE-006 — Premium Backend Authority

The frontend shall not be authoritative for determining whether a Student currently has Premium access.

---

# 17. Subscription Requirements

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

- Learning History
- Course progress
- Lesson progress
- Saved Vocabulary
- previous scores
- historical attempts required for learning history

## FR-SUB-006 — Subscription Entitlement Authority

Premium entitlement shall be determined from trusted backend subscription state.

The client shall not be able to grant itself Premium access.

---

# 18. Payment Requirements

## FR-PAY-001 — Initiate Payment

A Student upgrading to Premium shall be able to initiate a payment process.

## FR-PAY-002 — Record Transaction

The system shall retain relevant transaction information.

## FR-PAY-003 — Successful Payment

A successfully verified Premium payment shall activate or extend the corresponding Premium entitlement according to Business Rules.

## FR-PAY-004 — Failed Payment

A failed or unverified payment shall not activate Premium access.

## FR-PAY-005 — Payment Provider

The specific payment provider remains undecided and shall be selected later.

## FR-PAY-006 — Backend Payment Verification

Premium activation shall require trusted backend verification of the payment outcome.

A frontend success page or client-provided payment status alone shall not activate Premium.

---

# 19. Teacher Account Requirements

## FR-TEA-001 — Teacher Authentication

Authorized Teachers shall be able to authenticate to the platform.

Teacher authentication shall use the approved platform authentication mechanism.

## FR-TEA-002 — Teacher Profile

Teachers shall be able to view their User profile, including fullName and
optional avatarUrl; no separate Teacher identity is introduced.

## FR-TEA-003 — Update Teacher Profile

Teachers shall be able to update only their own fullName and avatarUrl according
to `BR-PROFILE-001`, using authenticated identity resolution.

## FR-TEA-004 — Teacher Account Provisioning

A public user shall not be able to self-register directly as a Teacher.

Teacher account creation or role assignment requires an authorized Admin workflow under `BR-AUTHN-022`.

Teacher account creation/provisioning shall require a valid fullName under
`BR-PROFILE-001`; this does not introduce Teacher self-registration.

---

# 20. Teacher Course Requirements

## FR-TCR-001 — Create Course

A Teacher shall be able to create a Course. Spring Boot shall initialize it as
DRAFT and STANDARD; Teacher create input shall not expose accessClassification.

## FR-TCR-002 — Course CEFR Level

A Teacher shall select an existing platform CEFR level for a Course.

## FR-TCR-003 — Course Ownership

A newly created Course shall be associated with its primary Teacher.

## FR-TCR-004 — View Owned Courses

Teachers shall be able to view Courses they own.

## FR-TCR-005 — Update Owned Course

Teachers shall be able to update permitted information for Courses they own.
Teacher update input shall not expose accessClassification. All existing approved
ownership-based Course editing, content management and authorized publish/archive
capabilities remain available; Admin classification authority is unchanged.

## FR-TCR-006 — Prevent Unauthorized Course Modification

A Teacher shall not modify another Teacher's Course unless explicitly authorized by a future administrative rule.

Ownership authorization shall be enforced by trusted backend logic.

---

# 21. Teacher Lesson Requirements

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

## FR-TLE-006 — Lesson Ownership Authorization

Teacher operations on Lessons shall respect ownership of the parent Course.

---

# 22. Teacher Vocabulary Requirements

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

# 23. Teacher Exercise Requirements

## FR-TEX-001 — Configure Fill Word

Teachers shall be able to configure Fill Word exercises for appropriate Lesson vocabulary.

## FR-TEX-002 — Configure Listening

Teachers shall be able to configure Listening exercises for appropriate Lesson vocabulary.

## FR-TEX-003 — Configure Quiz

Teachers shall be able to configure vocabulary Quiz content.

## FR-TEX-004 — Exercise Vocabulary

Exercises shall reference vocabulary associated with the relevant learning content.

## FR-TEX-005 — Exercise Ownership Authorization

A Teacher shall only manage exercises belonging to learning content they are authorized to manage.

---

# 24. Teacher Analytics Requirements

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

# 25. Admin User Management Requirements

## FR-ADM-001 — Admin Authentication

Authorized Admins shall be able to authenticate.

Admin authentication shall use the approved platform authentication mechanism.

## FR-ADM-002 — View Users

Admins shall be able to view platform users and filter them by authorization role.
Teacher inspection shall expose only permitted user information and associated
Courses; existing account-management permissions remain unchanged.

## FR-ADM-003 — Manage Student Accounts

Admins shall be able to perform permitted management actions on Student accounts.

## FR-ADM-004 — Manage Teacher Accounts

Admins shall be able to perform permitted management actions on Teacher accounts.

## FR-ADM-005 — Teacher Account Administration

The system shall support an authorized Admin workflow for Teacher account provisioning or role assignment.

Provisioning authority is defined by `BR-AUTHN-022`; detailed provisioning steps remain unresolved.

## FR-ADM-006 — Account Status Administration

Admins shall be able to perform permitted account-status management actions according to Business Rules.

---

# 26. Admin Course Requirements

## FR-ACR-001 — View Courses

Admins shall be able to view Courses across the platform.

## FR-ACR-002 — Course Oversight

Admins shall have platform-level Course oversight capabilities.

## FR-ACR-003 — Course Access Classification

The system shall support classification of Course access as Standard or Premium.

The exact authorization for changing this classification shall be defined in Business Rules.

---

# 27. Admin Subscription Requirements

## FR-ASU-001 — View Subscriptions

Admins shall be able to view relevant subscription information, including active
and expired entitlement periods.

## FR-ASU-002 — Subscription Statistics

Admins shall be able to view new-subscription and renewal counts according to
`BR-ADM-007`, in addition to subscription statistics.

## FR-ASU-003 — Active Premium Count

Admins shall be able to view the distinct number of Students with current Premium
entitlement coverage, not the number of subscription periods (`BR-ADM-004`).

---

# 28. Admin Transaction Requirements

## FR-ATR-001 — View Transactions

Admins shall be able to view relevant platform transactions.

## FR-ATR-002 — Transaction Status

Admins shall be able to view transaction status and counts of verified successful
and confirmed-failed payments. Unverified/unknown outcomes are not automatically
failures; detailed provider mapping remains unresolved (`BR-ADM-005`).

## FR-ATR-003 — Transaction Time

Admins shall be able to view when a transaction occurred.

---

# 29. Admin Analytics Requirements

## FR-AAN-001 — Total Users

Admins shall be able to view the total number of users.

## FR-AAN-002 — Student Statistics

Admins shall be able to view Student statistics, including the total count.

## FR-AAN-003 — Teacher Statistics

Admins shall be able to view Teacher statistics, including the total count.

## FR-AAN-004 — Course Statistics

Admins shall be able to view Course statistics, including the total count.

## FR-AAN-005 — Enrollment Statistics

Admins should be able to view platform enrollment statistics.

## FR-AAN-006 — Learning Statistics

Admins should be able to view aggregated platform learning statistics.

## FR-AAN-007 — Admin Overview Dashboard

Admins shall have an operational Overview Dashboard combining Student, Teacher
and Course totals, distinct current Premium Students, total and selected-range
verified revenue, new subscriptions, renewals, payment outcome counts and supported
plan revenue breakdowns. Apply `FR-ASU-001`–`FR-ASU-003`, `FR-ATR-002` and
`FR-REVN-001`–`FR-REVN-006`; existing optional analytics remain optional.

---

# 30. Revenue Requirements

## FR-REVN-001 — Revenue Statistics

Admins shall be able to view total verified successful-payment revenue and
platform revenue statistics, separately by currency (`BR-REVN-002`).

## FR-REVN-002 — Revenue by Time

Admins shall be able to view revenue over a selected time period.

## FR-REVN-003 — Monthly Revenue

The system shall support monthly revenue reporting.

## FR-REVN-004 — Yearly Revenue

The system shall support yearly revenue reporting.

## FR-REVN-005 — Revenue Source

The initial platform revenue source shall be Student Premium subscriptions.

## FR-REVN-006 — Revenue by Subscription Plan

Admins shall be able to group verified revenue by stored subscription-plan identity
or purchased Monthly/Yearly evidence, using historical transaction amounts and
currencies. Current catalog prices shall not replace historical payment values.
Historical plan names shall not be assumed available.

---

# 31. Authorization Requirements

## FR-AUTH-001 — Role-Based Access

The system shall enforce access based on authenticated user role.

Supported roles are:

- STUDENT
- TEACHER
- ADMIN

## FR-AUTH-002 — Student Authorization

Students shall not access Teacher or Admin management functionality.

## FR-AUTH-003 — Teacher Authorization

Teachers shall not access Admin-only functionality.

## FR-AUTH-004 — Course Ownership Authorization

Teacher Course modification shall respect Course ownership.

## FR-AUTH-005 — Premium Authorization

Premium functionality shall verify active Premium entitlement.

Premium authorization shall remain separate from role authorization.

## FR-AUTH-006 — Server-Side Enforcement

Authorization shall be enforced by the backend and shall not rely solely on frontend visibility.

## FR-AUTH-007 — Account Status Authorization

Protected access shall consider account status where applicable.

## FR-AUTH-008 — Resource Ownership Authorization

Protected operations involving Teacher-owned resources shall verify resource ownership or other explicitly approved authorization.

## FR-AUTH-009 — Client Role Restriction

The system shall not trust client-supplied role or privilege information as authoritative authorization state.

## FR-AUTH-010 — Client Premium Restriction

The system shall not trust client-supplied Premium status as authoritative entitlement state.

---

# 32. Responsive Web Requirements

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

## NFR-UI-007 — No Native Application Requirement

Students shall not be required to install a native Android or iOS application to use the core learning platform.

---

# 33. Security Requirements

## NFR-SEC-001 — Password Protection

Passwords shall not be stored in plaintext.

Passwords shall be protected using an appropriate secure password-hashing mechanism supported by the approved backend security architecture.

## NFR-SEC-002 — Authentication Protection

Protected resources shall require valid authentication.

## NFR-SEC-003 — Authorization Validation

Sensitive operations shall verify authorization.

## NFR-SEC-004 — Input Validation

User-provided input shall be validated.

## NFR-SEC-005 — Secret Management

API keys, payment credentials, database credentials, JWT signing secrets/keys, and other secrets shall not be committed directly to source control.

## NFR-SEC-006 — Payment Verification

Premium access shall not be granted solely from untrusted frontend payment information.

## NFR-SEC-007 — Access Token Expiration

JWT Access Tokens shall have a finite expiration time.

## NFR-SEC-008 — Refresh Token Expiration

Refresh Tokens shall have a finite expiration time.

## NFR-SEC-009 — Refresh Token Revocation

The authentication architecture shall support Refresh Token revocation or invalidation.

## NFR-SEC-010 — Sensitive JWT Claims

JWTs shall not contain:

- plaintext passwords
- password hashes
- password-reset credentials
- email-verification credentials
- API secrets
- unnecessary sensitive information

## NFR-SEC-011 — Browser Authentication Storage

Long-lived authentication credentials shall not be stored in browser `localStorage` by default.

The final web authentication design shall use an approved secure browser-storage/transport strategy.

## NFR-SEC-012 — Cookie Security

When authentication credentials are transported or stored using cookies, the security design shall consider:

- HttpOnly
- Secure in production
- SameSite
- CSRF protection
- CORS configuration

according to the deployment architecture.

## NFR-SEC-013 — Sensitive Authentication Logging

The system shall not intentionally log:

- plaintext passwords
- password hashes
- Refresh Tokens
- password-reset credentials
- email-verification credentials
- authentication secrets

## NFR-SEC-014 — Password Reset Protection

Password-reset credentials shall be:

- time-limited
- purpose-specific
- sufficiently unpredictable
- invalid after successful use where applicable

## NFR-SEC-015 — Email Verification Protection

Email-verification credentials shall be:

- time-limited
- purpose-specific
- associated with the intended account

## NFR-SEC-016 — Backend Security Authority

Authentication, role authorization, resource ownership, Premium entitlement, and protected business authorization shall not rely solely on frontend state.

## NFR-SEC-017 — No Custom Cryptography

The project shall not implement custom cryptographic algorithms for authentication or password protection.

Approved framework/library security mechanisms shall be used.

---

# 34. Data Requirements

## NFR-DATA-001 — Learning Data Persistence

Relevant Student learning information shall persist across sessions.

## NFR-DATA-002 — Subscription Expiration

Subscription expiration shall not delete Student learning data.

## NFR-DATA-003 — Vocabulary Reuse

The data model should support vocabulary reuse across Lessons.

## NFR-DATA-004 — Multiple Senses

The data model shall support multiple senses for a vocabulary word.

## NFR-DATA-005 — Historical Attempts

The system shall retain sufficient attempt information to support Learning History and progress calculation.

## NFR-DATA-006 — Authentication State

The data model shall support persistent authentication/session-related state when required by the approved Refresh Token and account-security design.

## NFR-DATA-007 — Transaction History

Relevant verified payment transaction information shall be retained independently from current subscription entitlement.

## NFR-DATA-008 — Subscription History Preservation

Changes in current subscription state shall not unnecessarily destroy historical subscription or learning information required by approved reporting and learning features.

---

# 35. Maintainability Requirements

## NFR-MNT-001 — Separation of Concerns

The system should maintain clear separation between presentation, application/business logic, and data-access responsibilities.

## NFR-MNT-002 — API Documentation

Backend APIs should be documented.

## NFR-MNT-003 — Testability

Important business logic should be designed so that it can be tested.

## NFR-MNT-004 — Configuration

Environment-specific configuration shall not be unnecessarily hardcoded.

## NFR-MNT-005 — Scope Simplicity

The architecture should remain appropriate for a graduation project and avoid unnecessary distributed-system complexity.

## NFR-MNT-006 — Feature-Based Organization

Application code should be organized primarily around business features/modules where practical.

Examples include:

- auth
- user
- course
- lesson
- vocabulary
- exercise
- progress
- review
- subscription
- payment
- analytics

Detailed package structure belongs to architecture design.

## NFR-MNT-007 — Module Cohesion

Functionality belonging to the same business feature should remain cohesive and should avoid unnecessary coupling with unrelated features.

## NFR-MNT-008 — Shared Concern Reuse

Shared infrastructure and domain functionality should not be unnecessarily duplicated across features.

## NFR-MNT-009 — External Provider Isolation

External provider-specific implementation should be isolated from core business logic where practical.

Potential providers include:

- Dictionary Provider
- Text-to-Speech Provider
- Payment Provider

## NFR-MNT-010 — API Boundary

Persistence entities should not be exposed directly as public API contracts by default.

Explicit request/response representations should be used where appropriate.

---

# 36. Performance Requirements

## NFR-PERF-001 — Normal Interaction

Common Student learning interactions should respond within a reasonable time under expected project-scale usage.

## NFR-PERF-002 — Vocabulary Search

Vocabulary Search should return results within a reasonable interactive response time.

## NFR-PERF-003 — Audio Loading

Pronunciation audio should be delivered in a manner suitable for normal mobile-web learning.

## NFR-PERF-004 — Pagination

Large management or reporting lists should support pagination or another appropriate bounded retrieval mechanism where necessary.

## NFR-PERF-005 — External API Usage

The system should avoid unnecessary repeated external-provider requests when an approved and license-compliant alternative is available.

Exact measurable performance targets will be defined after architecture and deployment assumptions are known.

---

# 37. External Integration Requirements

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

The project shall not assume unrestricted permanent storage or redistribution of provider content.

## INT-DIC-004 — Provider Abstraction

Dictionary-provider-specific integration should be isolated from core Vocabulary business logic where practical.

## INT-AUD-001 — Audio Availability

The system shall support pronunciation audio when an approved audio source is available.

## INT-AUD-002 — Audio Fallback

The final architecture shall define appropriate behavior when dictionary pronunciation audio is unavailable.

An approved Text-to-Speech provider may be considered as a fallback.

## INT-PAY-001 — Payment Provider

The system shall eventually integrate with an approved Payment Provider for Premium subscriptions.

The provider has not yet been selected.

## INT-PAY-002 — Trusted Payment Result

Payment Provider results used to activate Premium shall be verified through a trusted backend integration flow.

## INT-PAY-003 — Payment Provider Abstraction

Payment-provider-specific integration should be isolated from core subscription logic where practical.

---

# 38. Traceability

Future artifacts should reference requirement IDs where practical.

Examples:

Use Case:

```text
UC-STU-LEARN-01
→ FR-VOC-001
→ FR-VOC-002
→ FR-VOC-003
```

Authentication Use Case:

```text
UC-AUTH-LOGIN-01
→ FR-STU-002
→ FR-ACC-001
→ FR-ACC-002
→ FR-ACC-003
```

Token Refresh API:

```text
POST /api/auth/refresh
→ FR-ACC-003
→ FR-ACC-004
→ FR-ACC-005
```

Quiz API:

```text
POST /quiz/{id}/submit
→ FR-QUIZ-006
→ FR-QUIZ-007
→ FR-QUIZ-008
```

Premium Test:

```text
TC-PRE-003
→ FR-PRE-003
→ FR-AUTH-005
```

Course Ownership Test:

```text
TC-TCR-AUTH-001
→ FR-TCR-006
→ FR-AUTH-004
→ FR-AUTH-008
```

This allows:

```text
Requirement
→ Use Case
→ Design
→ API
→ Implementation
→ Test
```

to remain traceable.

---

# 39. Business Rule Status and Remaining Decisions

The following topics have initial Business Rules where documented; only their remaining unspecified details require further decisions:

## Authentication

- Student email-verification requirement: defined by `BR-AUTHN-004` through `BR-AUTHN-006`
- email-verification expiration
- email-verification resend behavior
- account-state transitions: initial verification transition defined by `BR-AUTHN-004`; administrative unlock/enable details remain unresolved
- account lock behavior: protected access is restricted by `BR-AUTHN-018`; automatic lockout is not required unless separately approved
- password requirements
- Access Token lifetime: configurable 15-minute default (`BR-AUTHN-007`)
- Refresh Token lifetime: configurable 7-day default (`BR-AUTHN-008`)
- Refresh Token rotation: support defined by `BR-AUTHN-010`; timing, replay, and concurrency details remain unresolved
- Refresh Token revocation: validation and applicable logout invalidation defined by `BR-AUTHN-009` and `BR-AUTHN-011`; broader invalidation details remain unresolved
- concurrent login/session behavior
- password-change session invalidation
- password-reset session invalidation: unresolved; recovery and single-use reset credentials with a configurable 15-minute lifetime are defined by `BR-AUTHN-014` through `BR-AUTHN-016`
- Teacher account provisioning: authorized Admin under `BR-AUTHN-022`; detailed steps remain unresolved
- Admin account provisioning: authorized administrative/system-setup workflow under `BR-AUTHN-023`; detailed steps remain unresolved

## Learning

- Standard versus Premium feature access
- Course Standard/Premium classification
- Lesson completion
- Course completion
- score calculation
- vocabulary mastery calculation
- Weak Vocabulary threshold
- Review selection

## Subscription and Payment

- subscription activation
- subscription expiration
- subscription renewal
- subscription extension
- payment verification
- payment transaction status rules

## Teacher and Admin

- Course publishing
- Teacher permissions
- Teacher account management
- Admin moderation permissions
- Course Standard/Premium classification authority
- account disable/lock permissions

Use existing `BUSINESS_RULES.md` provisions where defined; resolve only remaining business decisions there and technical details in the relevant design documents.

---

# 40. Requirement Stability Rules

Existing requirement identifiers should not be renumbered merely because new requirements are added.

When adding requirements:

- prefer adding a new identifier within the relevant requirement family
- do not reuse an identifier for a different meaning
- avoid silently changing the meaning of an approved requirement
- document important requirement changes

Examples:

```text
FR-ACC-018
FR-PRO-009
NFR-SEC-018
```

are preferable to renumbering existing requirements.

This rule preserves traceability between:

```text
Requirements
→ Business Rules
→ Use Cases
→ Architecture
→ APIs
→ Tests
```

---

# 41. Current Requirement Status

This specification remains:

**Status: Draft**

The project has defined the main functional scope for:

- Student accounts
- authentication
- learning discovery
- Course enrollment
- Lessons
- Vocabulary
- Fill Word
- Listening
- Quiz
- learning progress
- Vocabulary Search
- Saved Vocabulary
- Review
- Standard/Premium access
- subscriptions
- payments
- Teacher content management
- Teacher analytics
- Admin management
- Admin analytics
- revenue reporting
- authorization
- responsive web behavior
- security
- persistence
- maintainability
- external integrations

Initial rules and use cases already exist. Resolve remaining decisions needed by each subsequent design task using:

`docs/BUSINESS_RULES.md`

and the relevant design documents before dependent database/API design or implementation is finalized. Draft status does not invalidate explicitly documented defaults.
