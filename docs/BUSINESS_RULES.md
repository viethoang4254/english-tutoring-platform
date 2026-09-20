# English Learning Platform --- Business Rules\*\*

**Version:** 0.2

**Status:** Draft

**Project Type:** Graduation Project

**---**

# 1. Purpose\*\*

This document defines the core business rules of the English Learning
Platform.

Business Rules describe how the system behaves independently from
implementation technology.

Requirement IDs from `REQUIREMENTS.md` should be referenced where
appropriate.

**---**

# 2. Role Rules\*\*

## BR-ROLE-001 --- Primary Roles\*\*

The system contains three primary roles:

- STUDENT

- TEACHER

- ADMIN

A user's authorization shall be determined by their assigned role.

**---**

## BR-ROLE-002 --- Student Access Tier\*\*

STANDARD and PREMIUM are Student access tiers, not system roles.

A Student remains a STUDENT regardless of subscription status.

**---**

## BR-ROLE-003 --- Teacher Access\*\*

Teachers do not require a Premium subscription to perform Teacher
functions.

**---**

## BR-ROLE-004 --- Admin Access\*\*

Admins do not require a Premium subscription to perform Admin functions.

**---**

# 3. Authentication and Account Rules

## BR-AUTHN-001 --- Public Registration

Public self-registration is available only for Student accounts.

Registration requires fullName according to `BR-PROFILE-001`.

A public user shall not be able to register directly as TEACHER or
ADMIN.

---

## BR-AUTHN-002 --- Default Student Role

A successfully registered public account shall receive the STUDENT role.

Role assignment shall originate from trusted backend logic and shall not
be accepted from an untrusted client request.

---

## BR-AUTHN-003 --- Default Student Access Tier

A newly registered Student starts with STANDARD access.

PREMIUM access is obtained only through an active Premium entitlement.

---

## BR-AUTHN-004 --- Initial Account Status

A newly registered Student account starts as PENDING_VERIFICATION.

After successful email verification, the account becomes ACTIVE unless
an authorized account-management rule prevents activation.

---

## BR-AUTHN-005 --- Email Verification Requirement

Email verification is required before a Student may use authenticated
learning functionality that creates or updates persistent learning data.

A PENDING_VERIFICATION Student may authenticate only to the extent
required to:

- view the verification-required state
- request or resend email verification
- log out
- perform other explicitly approved account-verification actions

---

## BR-AUTHN-006 --- Email Verification Credential

An email-verification credential shall:

- be purpose-specific
- be associated with the intended account
- expire
- become unusable after successful verification where appropriate

The exact verification-token lifetime may be configured during
implementation and shall not be hardcoded into business logic.

---

## BR-AUTHN-007 --- Access Token

Successful authentication uses a short-lived JWT Access Token.

The initial project default Access Token lifetime is 15 minutes.

The lifetime shall be configurable.

---

## BR-AUTHN-008 --- Refresh Token

The system uses a Refresh Token to obtain a new Access Token without
requiring the user to enter credentials again while the Refresh Token
remains valid.

The initial project default Refresh Token lifetime is 7 days.

The lifetime shall be configurable.

---

## BR-AUTHN-009 --- Refresh Token Validation

A new Access Token may be issued only when the submitted Refresh Token
is:

- valid
- unexpired
- unrevoked
- associated with an eligible account/session
- accepted by the current security rules

An invalid, expired, revoked, or otherwise unusable Refresh Token shall
not produce a valid Access Token.

---

## BR-AUTHN-010 --- Refresh Token Rotation

The initial authentication design shall support Refresh Token rotation.

When a Refresh Token is successfully used, the backend may replace it
with a new Refresh Token and invalidate the previous token according to
the approved security design.

The client shall not be authoritative for rotation state.

---

## BR-AUTHN-011 --- Logout

Logout shall invalidate the applicable Refresh Token or
authentication-session state.

Removing frontend state alone is not sufficient to represent
authoritative logout.

---

## BR-AUTHN-012 --- Current User

Current-user information shall be derived from authenticated backend
identity and authoritative account data.

The client shall not be able to select another user merely by changing a
user identifier in the request.

---

## BR-AUTHN-013 --- Change Password

An authenticated eligible user may change their password after
satisfying the required security checks.

A password change shall not expose the old or new plaintext password
through logs or API responses.

Security-sensitive session invalidation after a password change shall
follow the approved authentication design.

---

## BR-AUTHN-014 --- Forgot Password

A user may initiate password recovery using the account's supported
recovery identifier.

Forgot-password responses should avoid unnecessary disclosure of whether
a specific account exists.

---

## BR-AUTHN-015 --- Password Reset

Password reset requires a valid, purpose-specific, unexpired
password-reset credential associated with the intended account.

A successfully used password-reset credential shall not be reusable.

---

## BR-AUTHN-016 --- Password Reset Lifetime

The initial project default password-reset credential lifetime is 15
minutes.

The lifetime shall be configurable.

---

## BR-AUTHN-017 --- Account Status

The initial account states are:

- PENDING_VERIFICATION
- ACTIVE
- LOCKED
- DISABLED

Only account states permitted by the applicable authentication rule may
access protected functionality.

---

## BR-AUTHN-018 --- Locked Account

A LOCKED account shall not access protected application functionality
until the lock is removed through an authorized workflow.

The exact automatic lockout policy is not required for the initial
version unless separately approved.

---

## BR-AUTHN-019 --- Disabled Account

A DISABLED account shall not authenticate or access protected
application functionality.

Disabling an account shall not automatically delete historical learning,
payment, or subscription records.

---

## BR-AUTHN-020 --- Role Authority

Role information is authoritative only when obtained from trusted
backend state.

The client shall not be allowed to assign, modify, or elevate its own
role.

---

## BR-AUTHN-021 --- Premium Authority

PREMIUM is not a role.

Premium entitlement shall be determined from trusted subscription state
rather than from client-provided flags or a long-lived JWT claim alone.

---

## BR-AUTHN-022 --- Teacher Account Provisioning

Teacher accounts are not created through public self-registration.

For the initial project version, an authorized Admin may create or
provision a Teacher account.

A Teacher account shall use the TEACHER role. Creation/provisioning requires a
valid fullName according to `BR-PROFILE-001`; an existing User's valid name may be
retained when assigning the Teacher role.

---

## BR-AUTHN-023 --- Admin Account Provisioning

Admin accounts are not created through public self-registration.

Admin provisioning shall occur only through an authorized administrative
or system-setup workflow.

A normal Student or Teacher shall not be able to promote themselves to
ADMIN.

---

## BR-PROFILE-001 --- Personal Profile Updates

Every User has a required full_name and an optional avatar_url on the shared User
identity. Existing authorized account-creation workflows must supply fullName.
Spring Boot trims surrounding whitespace from fullName, rejects null/blank values,
and permits at most 200 characters. Names are not unique or split into first/last
name fields.

An avatarUrl may be null; otherwise it must be an absolute HTTPS URL of at most
2048 characters, validated by Spring Boot without a reachability check. This is
only an image reference: no upload API, Storage service, proxy or backend image
download is introduced.

Eligible Students and Teachers may update only their own fullName and avatarUrl,
with identity resolved from authentication. In profile PATCH input, omission means
unchanged; null fullName is rejected, while null avatarUrl clears the avatar.
Only these two fields are writable; other properties are rejected. Ordinary
profile updates exclude id, email, password/password_hash, role, account_status,
email_verified_at, created_at, updated_at and Premium/entitlement state.
Password changes remain dedicated security operations. Email changes are
unsupported until a dedicated verified workflow is approved. This rule does not
grant Admin self-profile editing.

---

# 4. CEFR Rules\*\*

## BR-CEFR-001 --- Supported Levels\*\*

The platform supports:

- A1

- A2

- B1

- B2

- C1

- C2

**---**

## BR-CEFR-002 --- Platform Managed Levels\*\*

CEFR Levels belong to the platform.

Teachers shall not create arbitrary CEFR Levels.

**---**

## BR-CEFR-003 --- Course Level\*\*

Every published Course must belong to exactly one CEFR Level.

**---**

# 5. Teacher and Course Rules\*\*

## BR-COURSE-001 --- Course Ownership\*\*

Every Course has exactly one primary Teacher.

**---**

## BR-COURSE-002 --- Multiple Courses\*\*

A Teacher may own multiple Courses.

**---**

## BR-COURSE-003 --- Multiple Teachers\*\*

The platform may contain multiple Teachers.

**---**

## BR-COURSE-004 --- Teacher Modification\*\*

A Teacher may modify Courses they own. Existing approved Course/content editing
and authorized publish/archive capabilities remain intact; access classification
is excluded from Teacher-controlled fields.

A Teacher shall not modify another Teacher's Course.

Admin permissions are handled separately.

**---**

## BR-COURSE-005 --- Course Access Type\*\*

Spring Boot assigns STANDARD when a Teacher creates a Course.

A Course shall have one access classification:

- STANDARD

- PREMIUM

**---**

## BR-COURSE-006 --- Course Access Classification Authority\*\*

Teachers create educational content but do not make the final business
decision regarding whether a Course is Standard or Premium.

Teacher create/update operations shall not expose or accept accessClassification.
Admin controls changes between STANDARD and PREMIUM under the approved Admin rules.

**---**

# 6. Course Status Rules\*\*

Courses shall support at least the following lifecycle states:

- DRAFT

- PUBLISHED

- ARCHIVED

**---**

## BR-CSTATUS-001 --- Draft Course\*\*

Spring Boot initializes every Teacher-created Course as DRAFT. Teachers retain
existing authorized publish/archive operations for owned Courses after creation;
this does not add lifecycle transitions or remove existing editing permissions.

**---**

## BR-CSTATUS-002 --- Published Course\*\*

Only PUBLISHED Courses are discoverable by Students.

**---**

## BR-CSTATUS-003 --- Archived Course\*\*

An ARCHIVED Course shall not accept new enrollments.

Existing learning records associated with the Course shall not be
deleted merely because the Course is archived.

**---**

# 7. Enrollment Rules\*\*

## BR-ENR-001 --- Enrollment Required\*\*

A Student must enroll in a Course before the Course becomes part of My
Courses and tracked Course learning.

**---**

## BR-ENR-002 --- Standard Course\*\*

Both Standard and Premium Students may enroll in a STANDARD Course.

**---**

## BR-ENR-003 --- Premium Course\*\*

Only Students with active Premium access may enroll in a PREMIUM Course.

**---**

## BR-ENR-004 --- Duplicate Enrollment\*\*

A Student shall not have multiple active enrollment records for the same
Course.

**---**

## BR-ENR-005 --- Premium Expiration\*\*

If a Student enrolled in a Premium Course and Premium later expires:

- the enrollment record remains

- previous learning history remains

- previous progress remains

- Premium Course learning access becomes restricted

Access may resume if Premium becomes active again.

**---**

# 8. Lesson Rules\*\*

## BR-LESSON-001 --- Course Relationship\*\*

Every Lesson belongs to exactly one Course.

**---**

## BR-LESSON-002 --- Topic-Based Organization\*\*

Lessons should primarily represent learning topics.

Examples:

- Family

- Food & Drinks

- Travel

- Technology

Part of speech shall not be the primary Lesson hierarchy.

**---**

## BR-LESSON-003 --- Lesson Order\*\*

Lessons within a Course shall support an explicit display order.

**---**

## BR-LESSON-004 --- Repeat Learning\*\*

Students may repeat accessible Lesson learning activities.

Repeating an activity shall not delete previous attempt history.

**---**

# 9. Lesson Completion\*\*

For the initial project version, Lesson learning consists primarily of:

1\. Learn Vocabulary

2\. Fill Word

3\. Listening

4\. Quiz

**---**

## BR-LCOMP-001 --- Flexible Learning\*\*

Students may revisit completed learning sections.

The platform shall not permanently lock earlier activities after
completion.

**---**

## BR-LCOMP-002 --- Lesson Completion Condition\*\*

A Lesson is considered completed when the Student has completed all
required assessment sections configured for that Lesson.

For the initial core design, the expected required sections are:

- Fill Word

- Listening

- Quiz

Viewing the Learn Vocabulary section alone does not complete the Lesson.

**---**

## BR-LCOMP-003 --- Reattempt\*\*

Students may reattempt completed assessment sections.

The system shall retain relevant attempt history.

**---**

# 10. Course Completion\*\*

## BR-CCOMP-001 --- Course Completion\*\*

A Course is considered completed for a Student when all required Lessons
in that Course are completed.

**---**

## BR-CCOMP-002 --- Progress Percentage\*\*

Initial Course progress may be calculated as:

Completed Required Lessons / Total Required Lessons × 100

Example:

8 completed Lessons / 10 Lessons = 80%

This rule may later be refined if optional Lessons are introduced.

**---**

# 11. Vocabulary Rules\*\*

## BR-VOC-001 --- Shared Vocabulary\*\*

Vocabulary is a shared platform resource.

Teachers should reuse existing vocabulary where appropriate.

**---**

## BR-VOC-002 --- Duplicate Prevention\*\*

The system should avoid unnecessary duplicate vocabulary records
representing the same canonical English word.

**---**

## BR-VOC-003 --- Multiple Senses\*\*

One Vocabulary entry may contain multiple Vocabulary Senses.

**---**

## BR-VOC-004 --- Sense Information\*\*

A Vocabulary Sense may contain:

- part of speech

- English definition

- Vietnamese meaning

- example sentence

**---**

## BR-VOC-005 --- Lesson Sense Selection\*\*

When vocabulary has multiple senses, a Lesson should identify which
sense is being taught in that Lesson.

**---**

## BR-VOC-006 --- CEFR\*\*

Vocabulary may have a CEFR classification where reliable data is
available.

Absence of CEFR metadata shall not automatically make the vocabulary
unusable.

**---**

# 12. Dictionary Import Rules\*\*

## BR-DIC-001 --- Platform Search First\*\*

Teacher vocabulary workflow should search the platform vocabulary
database before requesting external dictionary data.

**---**

## BR-DIC-002 --- External Lookup\*\*

External Dictionary lookup should be used when suitable vocabulary data
does not already exist.

**---**

## BR-DIC-003 --- Teacher Review\*\*

Dictionary results shall not automatically become approved learning
content without appropriate Teacher review.

**---**

## BR-DIC-004 --- Licensing\*\*

Storage, caching, redistribution, and audio usage shall follow the
selected Dictionary Provider's license.

No implementation shall assume permanent storage rights before provider
selection.

**---**

# 13. Vocabulary Search Rules\*\*

## BR-SEARCH-001 --- Student Search\*\*

Both Standard and Premium Students may search vocabulary.

Vocabulary Search is not a Premium-only feature.

**---**

## BR-SEARCH-002 --- Basic Search Information\*\*

Standard Students should receive useful basic information where
available:

- word

- IPA

- part of speech

- basic definition

- Vietnamese meaning

- basic example

- pronunciation audio

- CEFR level

**---**

## BR-SEARCH-003 --- Premium Detail\*\*

Premium may provide additional vocabulary depth such as:

- multiple advanced senses

- collocations

- synonyms

- antonyms

- word families

- additional examples

The exact Premium vocabulary dataset depends on available licensed data.

**---**

# 14. Saved Vocabulary Rules\*\*

## BR-SAVE-001 --- Save Vocabulary\*\*

Authenticated Students may save vocabulary to My Vocabulary.

**---**

## BR-SAVE-002 --- Duplicate Saved Word\*\*

The same Student shall not have duplicate active saved records for the
same Vocabulary entry.

**---**

## BR-SAVE-003 --- Remove Vocabulary\*\*

Removing a word from My Vocabulary removes it from the Student's saved
collection.

It shall not delete the shared Vocabulary record.

**---**

## BR-SAVE-004 --- Subscription Independence\*\*

Saved Vocabulary shall remain stored if Premium expires.

**---**

# 15. Exercise Rules\*\*

The initial core exercise types are:

- Fill Word

- Listening

- Quiz

**---**

## BR-EX-001 --- Lesson Vocabulary\*\*

Exercise questions should use vocabulary relevant to the associated
Lesson.

**---**

## BR-EX-002 --- Reattempts\*\*

Students may perform multiple attempts.

Previous relevant attempts shall remain available for history and
analytics.

**---**

## BR-EX-003 --- Correctness\*\*

Each answerable exercise question shall have sufficient information for
the system to determine correctness.

**---**

# 16. Fill Word Rules\*\*

## BR-FILL-001 --- Objective\*\*

Fill Word primarily evaluates spelling and vocabulary recall.

**---**

## BR-FILL-002 --- Question Information\*\*

A Fill Word question may include:

- IPA

- English definition

- partially hidden word

**---**

## BR-FILL-003 --- Answer Comparison\*\*

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

**---**

# 17. Listening Rules\*\*

## BR-LIS-001 --- Audio\*\*

Listening questions require playable pronunciation audio.

**---**

## BR-LIS-002 --- Supported Formats\*\*

Initial Listening formats may include:

- LISTEN_AND_CHOOSE

- LISTEN_AND_TYPE

**---**

## BR-LIS-003 --- Listen and Type Comparison\*\*

Listen-and-Type answers follow the same basic normalization as Fill Word
unless otherwise specified.

**---**

# 18. Quiz Rules\*\*

## BR-QUIZ-001 --- Question Types\*\*

Initial Quiz question concepts may include:

- WORD_TO_DEFINITION

- DEFINITION_TO_WORD

- IPA_TO_WORD

- CONTEXT_TO_WORD

**---**

## BR-QUIZ-002 --- Question Evaluation\*\*

Every Quiz question shall have a defined correct answer or correct
option.

**---**

# 19. Score Rules\*\*

For the initial project version, scoring should remain understandable
and demonstrable.

**---**

## BR-SCORE-001 --- Basic Score\*\*

For exercises consisting of equally weighted questions:

Score Percentage = Correct Answers / Total Questions × 100

Example:

8 correct / 10 questions = 80%

**---**

## BR-SCORE-002 --- Score Range\*\*

Scores shall be represented from 0 to 100.

**---**

## BR-SCORE-003 --- Reattempt Scores\*\*

Every completed attempt may retain its own score.

**---**

## BR-SCORE-004 --- Best Score\*\*

The system may use the highest completed score as the Student's
displayed best score for an exercise.

Attempt history shall still be retained.

**---**

# 20. Accuracy Rules\*\*

## BR-ACC-001 --- Basic Accuracy\*\*

Accuracy may initially be calculated as:

Correct Answers / Total Answered Questions × 100

**---**

# 21. Vocabulary Performance Rules\*\*

The system should track Student performance at vocabulary level.

Relevant events may include:

- correct answer

- incorrect answer

- exercise type

- attempt time

**---**

# 22. Vocabulary Mastery\*\*

A simple initial mastery model shall be used before introducing advanced
learning algorithms.

**---**

## BR-MAST-001 --- Initial Mastery\*\*

Initial Vocabulary Mastery may be calculated from the Student's answer
history for that vocabulary.

Conceptually:

Correct Vocabulary Answers / Total Vocabulary Answers × 100

**---**

## BR-MAST-002 --- Minimum Evidence\*\*

A vocabulary item should not be treated as strongly mastered based on a
single correct answer.

The UI may distinguish insufficient learning data from established
mastery.

**---**

## BR-MAST-003 --- Mastery Categories\*\*

After the minimum evidence requirement is satisfied, the initial mastery
categories are:

- 0--49% → WEAK
- 50--79% → LEARNING
- 80--100% → MASTERED

Before the minimum evidence requirement is satisfied:

- fewer than 3 answered vocabulary questions → INSUFFICIENT_DATA

These thresholds are initial project defaults and may be tuned only
through an explicit Business Rule change.

**---**

# 23. Weak Vocabulary\*\*

## BR-WEAK-001 --- Weak Threshold\*\*

Vocabulary with at least 3 answered vocabulary questions and established
mastery below 50% is classified as WEAK.

**---**

## BR-WEAK-002 --- Insufficient Data\*\*

Vocabulary with fewer than 3 answered vocabulary questions is classified
as INSUFFICIENT_DATA rather than WEAK.

**---**

## BR-WEAK-003 --- Review Source\*\*

Weak vocabulary may become a source for Review activities.

**---**

# 24. Review Rules\*\*

## BR-REV-001 --- Review Sources\*\*

Review may select vocabulary from:

- Weak Vocabulary

- Saved Vocabulary

- Incorrect Answers

- Recently Learned Vocabulary

**---**

## BR-REV-002 --- No Complex SRS Requirement\*\*

The initial version does not require a full spaced-repetition scheduling
algorithm.

**---**

## BR-REV-003 --- Premium Review\*\*

Basic Review may be available to Standard Students.

More advanced personalized Weak Vocabulary practice and analytics may be
Premium.

**---**

# 25. Learning History Rules\*\*

## BR-HIST-001 --- Attempt History\*\*

Relevant completed exercise attempts shall be retained.

**---**

## BR-HIST-002 --- Historical Information\*\*

Learning History may include:

- activity

- Course

- Lesson

- score

- attempt time

**---**

## BR-HIST-003 --- Subscription Independence\*\*

Learning History shall remain available as stored learning data when
Premium expires, subject to the Standard/Premium display rules defined
by the product.

**---**

# 26. Standard Access Rules\*\*

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

**---**

# 27. Premium Access Rules\*\*

Premium includes all Standard functionality plus eligible Premium
capabilities.

Premium may include:

- Premium Courses

- specialized Courses

- deeper vocabulary information

- advanced Listening

- advanced exercises

- advanced Weak Vocabulary practice

- personalized Review

- detailed learning analytics

**---**

## BR-PRE-001 --- CEFR Availability\*\*

The system shall not assume that entire CEFR Levels are Premium-only.

The platform may provide Standard content at multiple CEFR levels.

**---**

## BR-PRE-002 --- Premium Entitlement\*\*

Premium functionality requires active Premium entitlement.

**---**

# 28. Subscription Rules\*\*

## BR-SUB-001 --- Subscription Periods\*\*

The initial subscription plan periods are:

- MONTHLY

- YEARLY

**---**

## BR-SUB-002 --- Active Premium\*\*

A Student is Premium only when the system determines that an applicable
Premium entitlement is active.

**---**

## BR-SUB-003 --- Expiration\*\*

When the Premium entitlement expires, the Student returns to Standard
access.

**---**

## BR-SUB-004 --- Preserve Data\*\*

Premium expiration shall not delete:

- account

- enrollments

- progress

- scores

- attempts

- saved vocabulary

- learning history

**---**

## BR-SUB-005 --- Premium Course Restriction\*\*

After expiration, Premium Course content becomes inaccessible until
Premium access is restored.

Historical enrollment and progress remain stored.

**---**

# 29. Payment Rules\*\*

## BR-PAY-001 --- Verified Payment\*\*

Premium access shall only be activated after payment success has been
verified by trusted backend logic.

**---**

## BR-PAY-002 --- Frontend Trust\*\*

A frontend success screen alone shall never be sufficient evidence of
payment.

**---**

## BR-PAY-003 --- Transaction Record\*\*

Relevant payment attempts shall produce or update appropriate
transaction records according to the selected provider workflow.

**---**

## BR-PAY-004 --- Successful Transaction\*\*

A verified successful transaction may activate the corresponding
subscription.

**---**

## BR-PAY-005 --- Failed Transaction\*\*

A failed transaction shall not activate Premium.

**---**

# 30. Teacher Content Rules\*\*

## BR-TEA-001 --- Owned Content\*\*

Teachers primarily manage educational content associated with their own
Courses.

**---**

## BR-TEA-002 --- Course Analytics\*\*

Teachers may access learning analytics for Courses they own.

**---**

## BR-TEA-003 --- Student Privacy\*\*

Teacher analytics should prioritize aggregated Course learning
information.

Detailed Student information shall only be exposed where required for
legitimate educational functionality.

**---**

## BR-TEA-004 --- Revenue\*\*

Teachers shall not access platform-wide revenue analytics.

**---**

# 31. Admin Rules\*\*

## BR-ADM-001 --- Platform Management\*\*

Admins have platform-level management capabilities according to
authorized Admin functions.

The operational Dashboard does not add account-management permissions or an
expense-management, salary, bookkeeping or profit/loss subsystem.

**---**

## BR-ADM-002 --- Course Oversight\*\*

Admins may oversee Courses across Teachers.

**---**

## BR-ADM-003 --- Access Classification\*\*

Admins control the final Standard/Premium classification of Courses.

**---**

## BR-ADM-004 --- Subscription Oversight\*\*

Admins may view platform subscription information.

Current Premium counts shall count distinct Students covered by authoritative
entitlement periods. Future and expired periods do not count as current coverage.
Whether disabled Students with coverage are included or shown separately remains
unresolved; account eligibility and entitlement remain distinct.

**---**

## BR-ADM-005 --- Transaction Oversight\*\*

Admins may view relevant transaction information.

Payment counts use authoritative outcome classifications. Unverified or unknown
payments shall not be treated as failed merely because verification is incomplete.
Detailed failed/invalid/cancelled/pending mapping and failure-report timestamps
remain unresolved.

**---**

## BR-ADM-006 --- Revenue Analytics\*\*

Admins may view platform revenue analytics.

**---**

## BR-ADM-007 --- Subscription Purchase Classification\*\*

A Student's first successfully granted entitlement period is a new subscription.
Each subsequent successfully granted period is a renewal, including after expiry.
Determine first/subsequent from the Student's complete grant history, not only
records in the selected range. Count purchase/grant events independently of future
period start dates. The reporting timestamp/range policy for these events remains
unresolved.

**---**

# 32. Revenue Rules\*\*

## BR-REVN-001 --- Revenue Source\*\*

The initial platform revenue source is Premium Student subscriptions.

**---**

## BR-REVN-002 --- Successful Payments\*\*

Revenue calculations shall be based on verified successful payment
transactions.

Sum historical transaction amounts, not current catalog prices, and report each
currency separately unless a conversion policy is explicitly approved. Failed or
invalid payments contribute neither revenue nor Premium entitlement. Refund
reporting remains unresolved; no net-revenue or profit metric is introduced.

**---**

## BR-REVN-003 --- Revenue Time\*\*

Revenue reporting shall support aggregation over time, using successful-payment
verification time (`verified_at`). Reporting timezone, default range, boundaries
and grouping intervals remain unresolved.

Examples:

- day

- month

- year

- custom date range

**---**

# 33. Data Preservation Rules\*\*

## BR-DATA-001 --- Learning Data\*\*

Student learning data shall not be deleted simply because:

- Premium expires

- Course becomes archived

- Student stops learning temporarily

**---**

## BR-DATA-002 --- Shared Vocabulary\*\*

Deleting a Lesson association shall not automatically delete a shared
Vocabulary record that may be used elsewhere.

**---**

## BR-DATA-003 --- Historical Integrity\*\*

Historical attempts should remain associated with the learning content
context needed to interpret those attempts.

**---**

# 34. Authorization Rules\*\*

## BR-AUTH-001 --- Backend Enforcement\*\*

All important authorization rules shall be enforced by the backend.

**---**

## BR-AUTH-002 --- Frontend Restrictions\*\*

Frontend hiding or disabling controls is a user-experience mechanism and
shall not replace backend authorization.

**---**

## BR-AUTH-003 --- Teacher Ownership\*\*

Teacher Course and Lesson modification shall verify Teacher ownership.

**---**

## BR-AUTH-004 --- Premium Access\*\*

Premium content access shall verify active Premium entitlement.

**---**

# 35. Responsive Web Rules\*\*

## BR-WEB-001 --- Student\*\*

Student UI shall be mobile-first.

**---**

## BR-WEB-002 --- Teacher\*\*

Teacher UI shall be desktop-optimized but responsive and usable on
mobile.

**---**

## BR-WEB-003 --- Admin\*\*

Admin UI shall be desktop-optimized but responsive and usable on mobile.

**---**

## BR-WEB-004 --- No Native App Requirement\*\*

No native Android or iOS application is required for the initial
project.

**---**

# 36. Scope Rules\*\*

The following are not part of the initial core scope unless explicitly
approved later:

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

**---**

# 37. Rules Still Requiring Future Decisions

The following remain open and should not be invented during
implementation:

1.  Exact Dictionary Provider
2.  Dictionary storage/caching rights
3.  Exact pronunciation audio strategy
4.  Exact TTS provider
5.  Exact payment provider
6.  Premium pricing
7.  Exact specialized Premium Courses
8.  Final Premium vocabulary dataset
9.  Refund handling
10. Detailed content moderation workflow
11. Exact email-verification credential lifetime
12. Exact password complexity policy
13. Final mastery formula after validation, if the initial formula is
    later revised
14. Detailed Review-selection algorithm
15. Exact advanced Premium exercise definitions
16. Exact advanced Premium analytics definitions

The following are now initial approved defaults rather than open
decisions:

- public registration creates STUDENT accounts only
- new Students begin with STANDARD access
- Student accounts begin as PENDING_VERIFICATION
- Access Token default lifetime is 15 minutes
- Refresh Token default lifetime is 7 days
- password-reset credential default lifetime is 15 minutes
- Refresh Token rotation is supported
- Teacher accounts are provisioned by authorized Admin workflow
- Admin accounts are not publicly self-registered
- Teachers may publish and archive their own Courses
- Admin retains final STANDARD/PREMIUM Course classification authority
- Premium renewal is manual in the initial version
- active renewal extends from the current expiration time
- expired renewal starts from verified activation/payment time
- vocabulary mastery requires at least 3 answered vocabulary questions
  before WEAK/LEARNING/MASTERED classification

Any implementation depending on an unresolved decision shall first
request or document the missing decision.
