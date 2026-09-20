# English Learning Platform --- Use Cases

**Version:** 0.1\
**Status:** Draft\
**Project Type:** Graduation Project

---

# 1. Purpose

This document defines the main use cases of the English Learning
Platform.

It intentionally stays concise. Detailed functional behavior belongs to
`REQUIREMENTS.md`, detailed rules belong to `BUSINESS_RULES.md`, and
technical implementation belongs to architecture/API documents.

Use case identifiers should remain stable so that later tasks, APIs,
tests, and implementation can reference them.

---

# 2. Actors

- **Visitor** --- unauthenticated user.
- **Student** --- authenticated learner with STANDARD or PREMIUM
  access.
- **Teacher** --- authenticated content creator.
- **Admin** --- authenticated platform administrator.
- **System** --- trusted backend processes.
- **External Provider** --- approved Dictionary, Audio/TTS, Email, or
  Payment provider.

---

# 3. Use Case Index

## Authentication

- `UC-AUTH-REGISTER-01` --- Register Student
- `UC-AUTH-VERIFY-EMAIL-01` --- Verify Email
- `UC-AUTH-LOGIN-01` --- Login
- `UC-AUTH-REFRESH-01` --- Refresh Authentication
- `UC-AUTH-LOGOUT-01` --- Logout
- `UC-AUTH-ME-01` --- View Current User
- `UC-AUTH-PROFILE-01` --- Update Own Profile
- `UC-AUTH-CHANGE-PASSWORD-01` --- Change Password
- `UC-AUTH-FORGOT-PASSWORD-01` --- Forgot Password
- `UC-AUTH-RESET-PASSWORD-01` --- Reset Password

## Student and Course

- `UC-STU-BROWSE-COURSES-01` --- Browse Courses
- `UC-STU-VIEW-COURSE-01` --- View Course
- `UC-STU-ENROLL-01` --- Enroll in Course
- `UC-STU-MY-COURSES-01` --- View My Courses
- `UC-STU-VIEW-LESSON-01` --- Open Lesson

## Learning

- `UC-LEARN-VOCABULARY-01` --- Learn Lesson Vocabulary
- `UC-LEARN-FILL-01` --- Complete Fill Word Exercise
- `UC-LEARN-LISTENING-01` --- Complete Listening Exercise
- `UC-LEARN-QUIZ-01` --- Complete Quiz
- `UC-LEARN-PROGRESS-01` --- View Learning Progress

## Vocabulary and Review

- `UC-VOC-SEARCH-01` --- Search Vocabulary
- `UC-VOC-DETAIL-01` --- View Vocabulary Details
- `UC-VOC-SAVE-01` --- Save Vocabulary
- `UC-VOC-MY-01` --- Manage My Vocabulary
- `UC-REV-REVIEW-01` --- Review Vocabulary

## Premium, Subscription, and Payment

- `UC-PRE-BENEFITS-01` --- View Premium Benefits
- `UC-PRE-SUBSCRIBE-01` --- Purchase Premium
- `UC-PRE-SUBSCRIPTION-01` --- View Subscription

## Teacher

- `UC-TEA-COURSE-01` --- Manage Owned Courses
- `UC-TEA-PUBLISH-01` --- Publish or Archive Course
- `UC-TEA-LESSON-01` --- Manage Lessons
- `UC-TEA-VOCABULARY-01` --- Add Vocabulary to Lesson
- `UC-TEA-EXERCISE-01` --- Manage Exercises
- `UC-TEA-ANALYTICS-01` --- View Course Analytics

## Admin

- `UC-ADM-USERS-01` --- Manage User Accounts
- `UC-ADM-TEACHERS-01` --- Provision Teacher Account
- `UC-ADM-COURSES-01` --- Course Oversight
- `UC-ADM-CLASSIFY-01` --- Classify Course Access
- `UC-ADM-SUBSCRIPTIONS-01` --- View Subscription Statistics
- `UC-ADM-TRANSACTIONS-01` --- View Transactions
- `UC-ADM-ANALYTICS-01` --- View Platform Statistics
- `UC-ADM-REVENUE-01` --- View Revenue Statistics

---

# 4. Authentication Use Cases

## UC-AUTH-REGISTER-01 --- Register Student

**Actor:** Visitor\
**Goal:** Create a Student account.

**References:** `FR-STU-001`, `FR-ACC-008`,
`BR-AUTHN-001`--`BR-AUTHN-005`

**Preconditions:** Visitor is not authenticated.

**Main Flow:** 1. Visitor opens Student registration. 2. Visitor
provides required account information including fullName. 3. System validates the
input under `BR-PROFILE-001`. 4.
System verifies that the account identifier can be registered. 5. System
creates the account with role STUDENT and access tier STANDARD. 6.
System sets the initial account state to PENDING_VERIFICATION. 7. System
initiates email verification. 8. System confirms registration.

**Alternative / Exception:** - Invalid input → registration is rejected
with validation feedback. - Existing account identifier → duplicate
registration is rejected. - Client attempts to register TEACHER/ADMIN →
requested role is ignored/rejected.

**Postconditions:** Student account exists and awaits verification.

---

## UC-AUTH-VERIFY-EMAIL-01 --- Verify Email

**Actor:** Student\
**Goal:** Verify the registered email address.

**References:** `FR-ACC-008`, `FR-ACC-009`,
`BR-AUTHN-004`--`BR-AUTHN-006`

**Preconditions:** Account is eligible for email verification.

**Main Flow:** 1. Student receives or requests a verification
credential. 2. Student submits the verification credential. 3. System
validates purpose, account association, expiration, and usability. 4.
System marks the email as verified. 5. System changes the account to
ACTIVE when allowed.

**Alternative / Exception:** - Credential is invalid/expired/used →
verification fails. - Student requests resend → System may issue a new
eligible verification message.

**Postconditions:** Eligible account is verified and ACTIVE.

---

## UC-AUTH-LOGIN-01 --- Login

**Actor:** Student / Teacher / Admin\
**Goal:** Establish authenticated access.

**References:** `FR-STU-002`, `FR-ACC-001`--`FR-ACC-003`,
`BR-AUTHN-007`--`BR-AUTHN-009`, `BR-AUTHN-017`--`BR-AUTHN-021`

**Preconditions:** Account exists.

**Main Flow:** 1. User submits credentials. 2. System validates
credentials. 3. System checks account status. 4. System establishes
authenticated identity. 5. System issues a short-lived Access Token and
valid Refresh Token backed by server-side RefreshSession state. 6. System returns permitted
authenticated account information.

**Alternative / Exception:** - Invalid credentials → authentication
fails. - LOCKED or DISABLED account → protected access is rejected. -
PENDING_VERIFICATION → access is limited to permitted
verification/account actions.

**Postconditions:** Valid authentication state exists if login succeeds.

---

## UC-AUTH-REFRESH-01 --- Refresh Authentication

**Actor:** Student / Teacher / Admin\
**Goal:** Obtain a new Access Token.

**References:** `FR-ACC-003`--`FR-ACC-006`,
`BR-AUTHN-008`--`BR-AUTHN-010`

**Preconditions:** A Refresh Token credential exists.

**Main Flow:** 1. Client requests token refresh. 2. System validates the
Refresh Token. 3. System verifies current account/session eligibility. 4. System issues a new Access Token. 5. System rotates the Refresh Token
when applicable.

**Alternative / Exception:** - Expired, invalid, revoked, or unusable
Refresh Token → refresh is rejected.

**Postconditions:** New authentication credentials exist only after
successful validation.

---

## UC-AUTH-LOGOUT-01 --- Logout

**Actor:** Student / Teacher / Admin\
**Goal:** End the current authenticated session.

**References:** `FR-STU-003`, `FR-ACC-006`, `BR-AUTHN-011`

**Preconditions:** User has authentication/session state.

**Main Flow:** 1. User requests logout. 2. System invalidates applicable
server-side state for the applicable Refresh Token/session. 3. Client authentication cookies/state are
cleared as applicable. 4. System confirms logout.

**Postconditions:** The revoked session cannot be refreshed.

---

## UC-AUTH-ME-01 --- View Current User

**Actor:** Student / Teacher / Admin\
**Goal:** Retrieve permitted information about the authenticated
account.

**References:** `FR-ACC-007`, `BR-AUTHN-012`, `BR-AUTHN-020`,
`BR-AUTHN-021`

**Preconditions:** Valid authentication.

**Main Flow:** 1. User requests current-account information. 2. System
resolves authenticated identity. 3. System retrieves authoritative
account information. 4. System returns permitted profile (including fullName and
optional avatarUrl), role, status,
and applicable entitlement information.

**Postconditions:** No account data is modified.

---

## UC-AUTH-PROFILE-01 --- Update Own Profile

**Actor:** Student / Teacher\
**Goal:** Update the authenticated User's personal profile.

**References:** `FR-STU-005`, `FR-TEA-003`, `BR-PROFILE-001`

**Preconditions:** User is authenticated and eligible for the operation.

**Main Flow:** 1. User supplies fullName and/or avatarUrl. 2. Backend resolves the
User from authenticated identity. 3. Backend validates only these writable fields
under `BR-PROFILE-001`. 4. Backend updates supplied fields; omitted fields remain
unchanged. Null fullName is rejected; null avatarUrl clears the avatar.

**Alternative / Exception:** Invalid or protected fields are rejected. A caller
cannot select another User as the update target.

**Postconditions:** Only the permitted profile values and backend-maintained
updated_at change; identity, security fields and entitlement remain unchanged.

---

## UC-AUTH-CHANGE-PASSWORD-01 --- Change Password

**Actor:** Student / Teacher / Admin\
**Goal:** Change the current password securely.

**References:** `FR-ACC-010`, `BR-AUTHN-013`

**Preconditions:** User is authenticated and eligible.

**Main Flow:** 1. User submits required password-change information. 2.
System performs required security validation. 3. System validates the
new password. 4. System securely replaces the password credential. 5.
System applies required session invalidation rules.

**Postconditions:** New password becomes authoritative.

---

## UC-AUTH-FORGOT-PASSWORD-01 --- Forgot Password

**Actor:** Visitor\
**Goal:** Start account password recovery.

**References:** `FR-ACC-011`, `BR-AUTHN-014`

**Preconditions:** None.

**Main Flow:** 1. Visitor submits the supported recovery identifier. 2.
System accepts the request without unnecessary account-existence
disclosure. 3. If an eligible account exists, System creates a
password-reset credential. 4. System sends recovery instructions through
the approved channel. 5. System returns a neutral response.

**Postconditions:** Eligible account may receive a reset credential.

---

## UC-AUTH-RESET-PASSWORD-01 --- Reset Password

**Actor:** Visitor\
**Goal:** Set a new password using a valid reset credential.

**References:** `FR-ACC-012`, `FR-ACC-013`, `BR-AUTHN-015`,
`BR-AUTHN-016`

**Preconditions:** User possesses a password-reset credential.

**Main Flow:** 1. User submits reset credential and new password. 2.
System validates credential purpose, account, expiration, and usability. 3. System validates the new password. 4. System replaces the password
securely. 5. System invalidates the reset credential. 6. System applies
required session invalidation.

**Alternative / Exception:** Invalid/expired/used credential → reset is
rejected.

**Postconditions:** Password is changed only after successful
validation.

---

# 5. Student and Course Use Cases

## UC-STU-BROWSE-COURSES-01 --- Browse Courses

**Actor:** Student\
**Goal:** Discover available Courses by CEFR level.

**References:** `FR-DIS-001`, `FR-DIS-002`, `BR-CEFR-*`, `BR-CSTATUS-*`

**Preconditions:** None for public discovery where permitted.

**Main Flow:** 1. Student selects or browses a CEFR level. 2. System
returns discoverable PUBLISHED Courses. 3. Student may inspect Course
summary information.

**Postconditions:** No enrollment is created.

---

## UC-STU-VIEW-COURSE-01 --- View Course

**Actor:** Student\
**Goal:** Review Course information before or during learning.

**References:** `FR-DIS-003`, `FR-DIS-004`, `BR-COURSE-*`,
`BR-CSTATUS-*`

**Preconditions:** Course is visible to the Student.

**Main Flow:** 1. Student selects a Course. 2. System returns Course
information, CEFR level, Teacher, access classification, and permitted
Lesson information. 3. System indicates enrollment/Premium restrictions
where applicable.

**Postconditions:** No learning state is changed.

---

## UC-STU-ENROLL-01 --- Enroll in Course

**Actor:** Student\
**Goal:** Enroll in an eligible Course.

**References:** `FR-ENR-001`--`FR-ENR-003`, `BR-ENR-*`, `BR-AUTH-004`

**Preconditions:** Student is authenticated, ACTIVE, and Course is
enrollable.

**Main Flow:** 1. Student requests enrollment. 2. System verifies Course
status and accessibility. 3. System verifies Premium entitlement when
required. 4. System verifies Student is not already actively enrolled. 5. System creates enrollment. 6. System initializes applicable Course
progress.

**Alternative / Exception:** - Premium Course + Standard Student →
enrollment rejected and upgrade may be offered. - Already enrolled →
duplicate enrollment is not created. - Course unavailable → enrollment
rejected.

**Postconditions:** Valid enrollment exists.

---

## UC-STU-MY-COURSES-01 --- View My Courses

**Actor:** Student\
**Goal:** View enrolled Courses and progress.

**References:** `FR-ENR-004`, `FR-ENR-005`, `FR-PRO-004`

**Preconditions:** Student is authenticated.

**Main Flow:** 1. Student opens My Courses. 2. System retrieves the
Student's enrollments. 3. System returns Courses with permitted progress
information.

**Postconditions:** No learning state is changed.

---

## UC-STU-VIEW-LESSON-01 --- Open Lesson

**Actor:** Student\
**Goal:** Access a Lesson in an enrolled accessible Course.

**References:** `FR-LES-001`--`FR-LES-004`, `BR-LESSON-*`, `BR-ENR-*`

**Preconditions:** Student is eligible to access the Course/Lesson.

**Main Flow:** 1. Student selects a Lesson. 2. System validates
Course/Lesson access. 3. System returns Lesson topic and learning
content. 4. Student may proceed to vocabulary and exercises.

**Postconditions:** Lesson content is available for learning.

---

# 6. Learning Use Cases

## UC-LEARN-VOCABULARY-01 --- Learn Lesson Vocabulary

**Actor:** Student\
**Goal:** Study vocabulary and selected senses in a Lesson.

**References:** `FR-VOC-001`--`FR-VOC-005`, `BR-VOC-*`, `BR-VOC-003`--`BR-VOC-005`

**Preconditions:** Student can access the Lesson.

**Main Flow:** 1. Student opens Lesson vocabulary. 2. System returns
vocabulary with the Lesson-selected sense. 3. Student views available
word, IPA, definition, meaning, example, and part of speech. 4. Student
may play pronunciation audio when available. 5. Student proceeds to
practice when desired.

**Postconditions:** Vocabulary is presented without changing shared
vocabulary ownership.

---

## UC-LEARN-FILL-01 --- Complete Fill Word Exercise

**Actor:** Student\
**Goal:** Practice spelling and vocabulary recall.

**References:** `FR-FILL-001`--`FR-FILL-005`, `FR-PRO-*`, `BR-FILL-*`,
`BR-SCORE-*`

**Preconditions:** Student can access the exercise.

**Main Flow:** 1. System presents Fill Word questions with permitted
hints. 2. Student enters answers. 3. Student submits the exercise. 4.
Backend evaluates normalized answers. 5. System calculates and stores
attempt results. 6. System updates applicable vocabulary
performance/progress. 7. System returns the result.

**Postconditions:** Attempt and applicable learning data are persisted.

---

## UC-LEARN-LISTENING-01 --- Complete Listening Exercise

**Actor:** Student\
**Goal:** Practice recognition of spoken vocabulary.

**References:** `FR-LIS-001`--`FR-LIS-006`, `FR-PRO-*`, `BR-LIS-*`,
`BR-SCORE-*`

**Preconditions:** Student can access the exercise and required audio is
playable.

**Main Flow:** 1. System presents Listening questions. 2. Student plays
audio. 3. Student chooses or types an answer. 4. Student submits
answers. 5. Backend evaluates the answers. 6. System stores results and
updates applicable learning data. 7. System returns the result.

**Alternative / Exception:** Required audio unavailable → affected
Listening item is not presented as a valid playable question.

**Postconditions:** Completed attempt is persisted.

---

## UC-LEARN-QUIZ-01 --- Complete Quiz

**Actor:** Student\
**Goal:** Evaluate vocabulary understanding.

**References:** `FR-QUIZ-001`--`FR-QUIZ-008`, `FR-PRO-*`, `BR-QUIZ-*`,
`BR-SCORE-*`

**Preconditions:** Student can access the Quiz.

**Main Flow:** 1. System presents supported Quiz questions. 2. Student
answers the questions. 3. Student submits the Quiz. 4. Backend evaluates
correct answers. 5. System calculates score/accuracy. 6. System stores
the attempt. 7. System updates applicable vocabulary performance and
progress. 8. System returns the result.

**Postconditions:** Quiz attempt and result are persisted.

---

## UC-LEARN-PROGRESS-01 --- View Learning Progress

**Actor:** Student\
**Goal:** Understand current learning performance.

**References:** `FR-PRO-001`--`FR-PRO-008`, `BR-SCORE-*`, `BR-ACC-*`,
`BR-MAST-*`, `BR-DATA-*`

**Preconditions:** Student is authenticated.

**Main Flow:** 1. Student opens progress/history. 2. System retrieves
authoritative learning records. 3. System calculates or retrieves
applicable score, accuracy, Lesson/Course progress, mastery, and weak
vocabulary. 4. System displays permitted learning history.

**Postconditions:** Learning records remain unchanged.

---

# 7. Vocabulary and Review Use Cases

## UC-VOC-SEARCH-01 --- Search Vocabulary

**Actor:** Student / Teacher\
**Goal:** Find vocabulary independently of current Course progression.

**References:** `FR-SEA-001`--`FR-SEA-005`,
`FR-TVOC-001`--`FR-TVOC-004`, `BR-SEARCH-*`, `BR-DIC-*`

**Preconditions:** Search functionality is available.

**Main Flow:** 1. Actor enters a search term. 2. System searches
platform vocabulary. 3. System returns matching vocabulary and available
basic information. 4. Teacher may request approved external Dictionary
lookup when platform data is insufficient.

**Postconditions:** Search alone does not modify Lesson content.

---

## UC-VOC-DETAIL-01 --- View Vocabulary Details

**Actor:** Student / Teacher\
**Goal:** Inspect available information for a vocabulary item.

**References:** `FR-VOC-002`--`FR-VOC-004`, `FR-SEA-002`, `FR-SEA-003`,
`BR-VOC-*`, `BR-VOC-003`--`BR-VOC-005`

**Preconditions:** Vocabulary item is available.

**Main Flow:** 1. Actor opens vocabulary details. 2. System returns
permitted senses and metadata. 3. Actor may play pronunciation audio
when available. 4. Student receives Standard/Premium detail according to
entitlement.

**Postconditions:** No vocabulary data is changed.

---

## UC-VOC-SAVE-01 --- Save Vocabulary

**Actor:** Student\
**Goal:** Add vocabulary to the personal saved collection.

**References:** `FR-SAV-001`, `BR-SAVE-*`

**Preconditions:** Student is authenticated.

**Main Flow:** 1. Student selects Save. 2. System verifies the
vocabulary item. 3. System creates the Student-vocabulary association if
absent. 4. System confirms saved state.

**Alternative:** Already saved → duplicate association is not created.

**Postconditions:** Vocabulary appears in My Vocabulary.

---

## UC-VOC-MY-01 --- Manage My Vocabulary

**Actor:** Student\
**Goal:** View and remove personally saved vocabulary.

**References:** `FR-SAV-002`--`FR-SAV-004`, `BR-SAVE-*`

**Preconditions:** Student is authenticated.

**Main Flow:** 1. Student opens My Vocabulary. 2. System returns saved
vocabulary. 3. Student may open details, practice, or remove an item. 4.
Removing an item deletes only the personal saved association.

**Postconditions:** Shared platform vocabulary is not deleted.

---

## UC-REV-REVIEW-01 --- Review Vocabulary

**Actor:** Student\
**Goal:** Revisit vocabulary requiring practice.

**References:** `FR-REV-001`--`FR-REV-006`, `BR-REV-*`, `BR-MAST-*`

**Preconditions:** Student is authenticated and eligible for the
selected Review capability.

**Main Flow:** 1. Student opens Review. 2. System selects eligible
vocabulary from approved Review sources. 3. Student performs available
Review activity. 4. System evaluates submitted answers when applicable. 5. System stores relevant learning results. 6. System updates applicable
vocabulary performance.

**Alternative:** Premium-only advanced Review → active Premium
entitlement is required.

**Postconditions:** Relevant Review activity contributes to learning
history.

---

# 8. Premium, Subscription, and Payment Use Cases

## UC-PRE-BENEFITS-01 --- View Premium Benefits

**Actor:** Student\
**Goal:** Compare Standard and Premium capabilities.

**References:** `FR-PRE-001`, `BR-PRE-*`

**Preconditions:** None.

**Main Flow:** 1. Student opens Premium information. 2. System displays
the approved Standard/Premium feature matrix. 3. System displays
available subscription plans and pricing when configured.

**Postconditions:** No subscription is changed.

---

## UC-PRE-SUBSCRIBE-01 --- Purchase Premium

**Actor:** Student\
**Supporting Actor:** Payment Provider\
**Goal:** Activate or extend Premium through a verified payment.

**References:** `FR-PRE-002`--`FR-PRE-006`, `FR-SUB-*`, `FR-PAY-*`,
`BR-SUB-*`, `BR-PAY-*`

**Preconditions:** Student is authenticated and an approved plan/payment
provider is available.

**Main Flow:** 1. Student selects a Premium plan. 2. System
creates/initiates the payment flow. 3. Student completes the provider
payment flow. 4. Backend receives and verifies trusted payment result. 5. System records the transaction. 6. For verified success, System
activates or extends Premium according to subscription rules. 7. System
returns current subscription state.

**Alternative / Exception:** - Failed/unverified payment → Premium is
not activated. - Student abandons payment → no successful entitlement
change. - Provider error → transaction/subscription state remains
consistent.

**Postconditions:** Premium changes only after trusted verification.

---

## UC-PRE-SUBSCRIPTION-01 --- View Subscription

**Actor:** Student\
**Goal:** View current Premium subscription state.

**References:** `FR-SUB-002`--`FR-SUB-006`, `BR-SUB-*`, `BR-DATA-001`

**Preconditions:** Student is authenticated.

**Main Flow:** 1. Student opens subscription information. 2. System
retrieves authoritative subscription state. 3. System displays access
tier, active/expired state, plan, and relevant dates.

**Postconditions:** No subscription data is changed.

---

# 9. Teacher Use Cases

## UC-TEA-COURSE-01 --- Manage Owned Courses

**Actor:** Teacher\
**Goal:** Create, view, and update Courses the Teacher owns.

**References:** `FR-TCR-001`--`FR-TCR-006`, `BR-COURSE-*`,
`BR-AUTH-003`

**Preconditions:** Teacher is authenticated and ACTIVE.

**Main Flow:** 1. Teacher views owned Courses or creates a Course. 2.
For creation, Teacher selects an existing CEFR level and provides Course
information. 3. System creates the Course with the Teacher as primary
owner and backend-assigned DRAFT status and STANDARD classification. 4. Teacher
may update permitted fields of owned Courses and manage approved content;
accessClassification is excluded from Teacher create/update input. Existing
publish/archive operations remain available. 5. Backend verifies ownership for protected
modifications.

**Alternative:** Attempt to modify another Teacher's Course → rejected.

**Postconditions:** Only authorized Course data is changed.

---

## UC-TEA-PUBLISH-01 --- Publish or Archive Course

**Actor:** Teacher\
**Goal:** Change lifecycle status of an owned Course.

**References:** `BR-CSTATUS-001`--`BR-CSTATUS-003`, `BR-COURSE-004`, `FR-TCR-*`

**Preconditions:** Teacher owns the Course.

**Main Flow:** 1. Teacher selects an owned Course. 2. Teacher requests
Publish or Archive. 3. System validates ownership and required content
conditions. 4. System changes Course status when valid. 5. System
preserves historical enrollments/progress when archiving.

**Postconditions:** Course lifecycle status is updated without deleting
historical learning data.

---

## UC-TEA-LESSON-01 --- Manage Lessons

**Actor:** Teacher\
**Goal:** Create and update Lessons inside an owned Course.

**References:** `FR-TLE-001`--`FR-TLE-006`, `BR-LESSON-*`, `BR-COURSE-004`, `BR-AUTH-003`

**Preconditions:** Teacher owns the parent Course.

**Main Flow:** 1. Teacher opens an owned Course. 2. Teacher creates or
selects a Lesson. 3. Teacher defines/updates Lesson topic and permitted
content. 4. Backend verifies Course ownership. 5. System persists the
Lesson.

**Postconditions:** Lesson belongs to the authorized Course.

---

## UC-TEA-VOCABULARY-01 --- Add Vocabulary to Lesson

**Actor:** Teacher\
**Supporting Actor:** Dictionary Provider\
**Goal:** Select reusable vocabulary/senses for an owned Lesson.

**References:** `FR-TVOC-001`--`FR-TVOC-005`, `FR-TLE-004`,
`FR-TLE-005`, `BR-VOC-*`, `BR-VOC-003`--`BR-VOC-005`, `BR-DIC-*`

**Preconditions:** Teacher owns the Lesson's Course.

**Main Flow:** 1. Teacher searches platform vocabulary. 2. Teacher
selects an existing vocabulary item/sense when suitable. 3. If needed,
Teacher requests approved Dictionary lookup. 4. System maps available
provider information into internal reviewable data. 5. Teacher
reviews/selects the appropriate sense. 6. System associates the selected
vocabulary/sense with the Lesson.

**Alternative:** Provider data cannot legally be stored as intended →
System must follow the approved provider/storage strategy rather than
assume unrestricted storage.

**Postconditions:** Lesson references appropriate reusable vocabulary
data.

---

## UC-TEA-EXERCISE-01 --- Manage Exercises

**Actor:** Teacher\
**Goal:** Configure Fill Word, Listening, and Quiz exercises for owned
learning content.

**References:** `FR-TEX-001`--`FR-TEX-005`, `BR-EX-*`, `BR-FILL-*`,
`BR-LIS-*`, `BR-QUIZ-*`

**Preconditions:** Teacher owns the relevant Course/Lesson.

**Main Flow:** 1. Teacher selects owned Lesson content. 2. Teacher
selects an approved exercise type. 3. Teacher configures questions using
relevant vocabulary. 4. System validates that questions contain enough
information for evaluation. 5. System saves the exercise configuration.

**Alternative:** Listening question lacks playable audio → invalid
Listening item is rejected or excluded.

**Postconditions:** Exercise is associated with authorized learning
content.

---

## UC-TEA-ANALYTICS-01 --- View Course Analytics

**Actor:** Teacher\
**Goal:** View learning analytics for owned Courses.

**References:** `FR-TAN-001`--`FR-TAN-006`, `BR-TEA-*`

**Preconditions:** Teacher is authenticated.

**Main Flow:** 1. Teacher selects an owned Course. 2. System verifies
ownership. 3. System returns permitted enrollment, progress,
performance, and difficult-vocabulary aggregates. 4. System protects
unnecessary Student detail and platform-wide revenue information.

**Postconditions:** No learning data is modified.

---

# 10. Admin Use Cases

## UC-ADM-USERS-01 --- Manage User Accounts

**Actor:** Admin\
**Goal:** View users and perform permitted account-management actions.

**References:** `FR-ADM-001`--`FR-ADM-006`, `BR-ADM-*`,
`BR-AUTHN-017`--`BR-AUTHN-023`

**Preconditions:** Admin is authenticated and authorized.

**Main Flow:** 1. Admin opens user management. 2. System returns
permitted user information, with role filtering and inspection of a Teacher's
permitted information and associated Courses. 3. Admin selects an eligible
account-management action. 4. System validates Admin authorization. 5.
System applies the account change and preserves required historical
data.

**Postconditions:** Authorized account state is updated.

---

## UC-ADM-TEACHERS-01 --- Provision Teacher Account

**Actor:** Admin\
**Goal:** Create/provision an authorized Teacher account.

**References:** `FR-ADM-004`, `FR-ADM-005`, `FR-TEA-004`, `BR-AUTHN-022`

**Preconditions:** Admin is authenticated and authorized.

**Main Flow:** 1. Admin initiates Teacher provisioning. 2. Admin
provides required Teacher account information including a valid fullName (or
retains an existing User's valid name for role assignment). 3. System validates
the data under `BR-PROFILE-001` and checks uniqueness constraints. 4. System creates/provisions the
account with TEACHER role. 5. System applies the approved
account-verification/status process.

**Postconditions:** Authorized Teacher account exists.

---

## UC-ADM-COURSES-01 --- Course Oversight

**Actor:** Admin\
**Goal:** View and oversee Courses across the platform.

**References:** `FR-ACR-001`, `FR-ACR-002`, `BR-ADM-001`, `BR-ADM-002`

**Preconditions:** Admin is authenticated.

**Main Flow:** 1. Admin opens Course management. 2. System returns
Courses across Teachers. 3. Admin reviews Course ownership, level,
status, and permitted management information. 4. Admin performs only
explicitly authorized oversight actions.

**Postconditions:** Course ownership is not automatically transferred.

---

## UC-ADM-CLASSIFY-01 --- Classify Course Access

**Actor:** Admin\
**Goal:** Set final STANDARD/PREMIUM Course access classification.

**References:** `FR-ACR-003`, `BR-COURSE-006`, `BR-ADM-003`

**Preconditions:** Admin is authenticated and Course exists.

**Main Flow:** 1. Admin selects a Course. 2. Admin selects STANDARD or
PREMIUM classification. 3. System validates Admin authorization. 4.
System saves the classification. 5. Future Student access/enrollment
follows the new classification.

**Postconditions:** Course has authoritative access classification.

---

## UC-ADM-SUBSCRIPTIONS-01 --- View Subscription Statistics

**Actor:** Admin\
**Goal:** Monitor platform subscription state.

**References:** `FR-ASU-001`--`FR-ASU-003`, `BR-ADM-004`, `BR-ADM-007`

**Preconditions:** Admin is authenticated.

**Main Flow:** 1. Admin opens subscription management/statistics. 2.
System retrieves authoritative subscription data. 3. System displays
permitted active/expired periods, distinct current Premium Student counts,
and new-subscription/renewal counts using complete grant history.

**Postconditions:** Subscription state is unchanged.

---

## UC-ADM-TRANSACTIONS-01 --- View Transactions

**Actor:** Admin\
**Goal:** Monitor Premium payment transactions.

**References:** `FR-ATR-001`--`FR-ATR-003`, `BR-ADM-005`, `BR-PAY-*`

**Preconditions:** Admin is authenticated.

**Main Flow:** 1. Admin opens transactions. 2. System retrieves
permitted transaction records. 3. System displays status, time, and
relevant transaction information and verified-success/confirmed-failure counts.
Unknown/unverified outcomes are not automatically classified as failures.

**Postconditions:** Transactions are not modified by viewing.

---

## UC-ADM-ANALYTICS-01 --- View Platform Statistics

**Actor:** Admin\
**Goal:** View platform-wide operational statistics.

**References:** `FR-AAN-001`--`FR-AAN-007`, `FR-ASU-001`--`FR-ASU-003`,
`FR-ATR-002`, `FR-REVN-001`--`FR-REVN-006`, `BR-ADM-*`, `BR-REVN-*`

**Preconditions:** Admin is authenticated.

**Main Flow:** 1. Admin opens the operational Overview Dashboard/system analytics. 2. System
calculates/retrieves authoritative aggregates. 3. System displays user,
Student, Teacher, Course, enrollment, subscription, and learning
statistics as available, including the required `FR-AAN-007` dashboard metrics.

**Postconditions:** Source data is unchanged.

---

## UC-ADM-REVENUE-01 --- View Revenue Statistics

**Actor:** Admin\
**Goal:** View verified subscription revenue over time.

**References:** `FR-REVN-001`--`FR-REVN-006`, `BR-REVN-*`, `BR-PAY-*`

**Preconditions:** Admin is authenticated.

**Main Flow:** 1. Admin opens revenue analytics. 2. Admin selects an
available time period. 3. System aggregates verified successful Premium
payment transactions by verification time. 4. System displays total and selected-
period revenue, separately by currency, with supported historical plan grouping.

**Postconditions:** Transaction records are unchanged.

---

# 11. Global Authorization Rules for Use Cases

The following apply to every protected use case:

1.  Backend authentication and authorization are authoritative.
2.  Frontend-hidden controls do not replace backend checks.
3.  STUDENT, TEACHER, and ADMIN are roles.
4.  STANDARD and PREMIUM are Student access tiers, not roles.
5.  Teacher modification of Course-owned content requires ownership
    verification.
6.  Premium-only Student functionality requires active Premium
    entitlement.
7.  Client-supplied role, ownership, score, payment-success, or Premium
    state is not authoritative.
8.  LOCKED/DISABLED accounts cannot bypass account restrictions using
    stale client state.

---

# 12. Traceability Direction

Use cases should be referenced later without copying their full content.

Example:

```text
Requirement
FR-STU-002
    ↓
Business Rules
BR-AUTHN-007
BR-AUTHN-008
BR-AUTHN-017
    ↓
Use Case
UC-AUTH-LOGIN-01
    ↓
API
POST /api/auth/login
    ↓
Implementation Task
AUTH-LOGIN
    ↓
Tests
TC-AUTH-LOGIN-*
```

This allows Codex or another agent to retrieve only the context needed
for a specific feature instead of reading every project document for
every task.

---

# 13. Open Use-Case Decisions

The following should not be invented inside implementation tasks:

- exact Dictionary Provider behavior not already approved
- exact Dictionary caching/storage rights
- exact pronunciation/TTS fallback
- exact Payment Provider
- Premium pricing
- refund workflow
- exact advanced Premium exercise behavior
- exact personalized Review-selection algorithm
- exact advanced Student analytics
- detailed content moderation workflow

When one of these decisions becomes necessary, update the appropriate
Requirements/Business Rules first and then update the affected use case.

---

# 14. Change Rules

When modifying this document:

1.  Keep existing Use Case IDs stable whenever possible.
2.  Reference Requirement and Business Rule IDs instead of copying full
    rules.
3.  Do not add implementation classes, database tables, or exact
    endpoints here.
4.  Do not invent new product scope.
5.  Update only affected use cases when requirements change.
6.  Keep flows focused on observable actor/system behavior.
