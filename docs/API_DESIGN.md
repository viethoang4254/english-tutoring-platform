# API Design

Status: complete proposed documentation draft for review. Not an implementation-ready
contract for operations explicitly marked provisional.

## 1. Purpose, scope and sources

Define use-case-oriented REST contracts for the English Learning Platform.
No application code, SQL, migrations, database objects, skeletons or deployment
configuration are authorized by this document.

Sources: [AGENTS.md](../AGENTS.md), [PROJECT_SPEC.md](PROJECT_SPEC.md),
[REQUIREMENTS.md](REQUIREMENTS.md), [BUSINESS_RULES.md](BUSINESS_RULES.md),
[USE_CASES.md](USE_CASES.md), [DOMAIN_MODEL.md](DOMAIN_MODEL.md),
[ARCHITECTURE.md](ARCHITECTURE.md), [DATABASE_DESIGN.md](DATABASE_DESIGN.md);
project-local requirements-analysis, english-learning-domain, system-design and
authentication-security skills under ../.agents/skills/.

The current documents resolve Course creation and personal profile contracts:
Spring Boot supplies DRAFT/STANDARD and the authenticated Teacher owner; Teacher
input cannot classify Courses. Existing owned-content editing and publish/archive
remain available. User full_name is required, avatar_url optional, and self-profile
editing is restricted by BR-PROFILE-001. No email-change or avatar-upload API exists.

There is no remaining contradictory requirement requiring database redesign.
Some security/provider workflows remain unresolved and block finalization of their
individual wire contracts, not preparation of this document. Provisional entries
must not be implemented by treating placeholders as approved policy.

## 2. Architecture and trust boundaries

Browser -> Next.js -> Spring Boot REST API -> Spring Data JPA / Hibernate ->
Supabase-hosted PostgreSQL.

Spring Boot authenticates, authorizes, verifies resource ownership, validates
business state, derives Course access/Premium, evaluates answers, verifies payments
and supplies authorized analytics. Keep Spring Security + JWT and
features/auth/security/. Supabase is managed PostgreSQL hosting only.
No Supabase Auth, direct frontend database access, Prisma, Realtime, Storage,
Edge Functions, accounting subsystem or reporting infrastructure is introduced.
Local PostgreSQL remains valid for development/testing.

DTOs never expose JPA entities, password/credential hashes, signing secrets,
refresh-session rows, provider secrets or internal stack traces.
Roles STUDENT/TEACHER/ADMIN are distinct from Student STANDARD/PREMIUM entitlement
and Course accessClassification. Role claims alone do not prove current account
eligibility, ownership or Premium.

## 3. Common wire conventions

All routes below are relative to /api/v1. Bodies and ordinary responses use JSON.
Object keys use camelCase. Id is a positive BIGINT identifier encoded as a decimal
string, never a JSON number; validate range and reject malformed path/query IDs.
Monetary Amount is an exact decimal string paired with currency. Never convert
money through floating-point arithmetic. Absolute Time values are ISO-8601 strings
with an explicit offset. Reporting timezone/bucketing policy remains separately open.

Notation: field:T is required, field?:T is optional, T|null explicitly permits
null, T[] is an array. Id, Time, Amount and string are wire types, not database
entities. Request fields not listed in an operation are rejected. In PATCH,
omission preserves a field; null is allowed only when the DTO says so. Require at
least one permitted property for a profile/content PATCH (API proposal).
Reject wrong JSON types and invalid enums with 400. Business/range validation
also uses 400 in this proposal, rather than mixing 400 and 422.

Creation returns 201 with Location when a resource is created. GET/PATCH/PUT
normally return 200 with the declared DTO. DELETE with no response returns 204.
Where explicitly declared, credential-neutral requests return 202 with
Acknowledgement. No API exposes a writable score, correctness or Premium flag.

Page<T> = {items:T[], page:integer, size:integer, totalElements:IdCount,
totalPages:integer}. IdCount is a nonnegative decimal string for aggregate counts.
Proposed page is zero-based, default 0; size default 20, range 1..100.
Reject unsupported filter/sort keys and directions. sort=field,asc|desc; append id
as a stable tie-breaker. Only the per-operation allowlist is accepted. Offset
pagination is not a snapshot guarantee during concurrent changes.
Ordered child content uses bounded Page responses, not unlimited entity graphs.

All authenticated operations can fail with 401 (absent/invalid/expired
authentication) or 403 (role, current account eligibility, entitlement or permission).
Protected object operations additionally use 404 for a missing/non-visible resource;
choose a consistent non-disclosure policy without bypassing backend authorization.
Common errors: 400 malformed/invalid input; 409 incompatible state or conflicting
retry; 429 when an approved rate limit applies; 503 temporary dependency failure.
Numeric rate limits and provider behavior are not invented here.

Error = {code:string, message:string, fieldErrors?:{field:string,code:string,
message:string}[]}. Use stable domain codes, safe messages and no sensitive values.
Examples: VALIDATION_FAILED, ACCESS_DENIED, COURSE_NOT_ACCESSIBLE,
ATTEMPT_ALREADY_COMPLETED, SUBMISSION_CONFLICT, DEPENDENCY_UNAVAILABLE.
Forgot-password/resend responses must not disclose account existence through
different message content; broader anti-enumeration/rate policy remains security work.

## 4. Authentication and authorization notation

PUBLIC: no existing login required; validate any supplied purpose-specific credential.
CURRENT: authenticated User, current eligibility checked. /me may expose only
information/actions permitted for the current account state; it does not grant
PENDING_VERIFICATION access to persisted learning.
S: eligible authenticated STUDENT. T: eligible authenticated TEACHER.
A: eligible authenticated ADMIN with permission for the named operation.
SELF: use authenticated User identity, never a body-selected owner.
OWN: Teacher owns parent Course, including traversal through Lesson/Exercise/question.
LEARN: backend verifies publication/access, enrollment and Premium when applicable.
The exact public-discovery boundary is unresolved; discovery entries say DISCOVERY,
with authenticated Student access defined and anonymous access provisional.

Admin does not receive arbitrary entity mutation or undocumented ownership bypass.
Teachers do not receive platform revenue analytics. Teacher/Admin functions do not
require Premium. LOCKED/DISABLED restrictions follow BR-AUTHN-018/019;
PENDING_VERIFICATION follows BR-AUTHN-005.

AUTH-WIRE is provisional: access/refresh delivery by cookie versus JSON/header is
not selected. Credential transport, CSRF/CORS/cookie settings and refresh-session
selection must be approved before finalizing affected headers/bodies.
The logical AuthSession response is {user:CurrentUser, accessExpiresAt:Time,
refreshExpiresAt:Time}; secure token delivery accompanies it under AUTH-WIRE,
not through an invented persisted token API. Defaults remain configurable:
Access Token 15 minutes, Refresh Token 7 days, reset credential 15 minutes.

## 5. Retry and consistency contract

GET is side-effect free. Natural-key PUT association operations are idempotent.
PATCH repetition with identical values does not change business meaning.
DELETE saved/assignment association succeeds with 204 when already absent after
authorization. Completed attempts/results/snapshots and entitlement periods are
immutable.

Answer slot uniqueness is (attemptId, position). Same slot and identical accepted
submission returns the stored receipt; different content returns 409. Backend
serializes submission/completion and derives counts/scores; no client total is trusted.
Completion repetition returns the original result and never creates another score.

POST creation is not assumed retry-safe merely because a route is RESTful.
For registration use existing email uniqueness; for Course/content creation,
clients must reconcile an uncertain response before creating another resource.
Attempt creation retry identity and payment initiation deduplication remain
provisional where current tables do not contain a client request key. No generic
idempotency table, unapproved column or in-memory-only guarantee is invented.
A timeout must not trigger automatic duplicate payment initiation. Provider-facing
retry correlation must be finalized with the provider before enabling payment writes.
Existing unique originating_payment_id prevents repeated grant creation for one
verified payment; it does not itself deduplicate distinct payment initiations.

## 6. DTO dictionary

| DTO | Exact proposed public shape |
|---|---|
| Acknowledgement | {message:string} |
| Profile | {fullName:string, avatarUrl:string\|null} |
| CurrentUser | {id:Id, email:string, fullName:string, avatarUrl:string\|null, role:Role, accountStatus:AccountStatus, emailVerified:boolean, entitlement?:Entitlement} |
| Registration | {userId:Id, accountStatus:"PENDING_VERIFICATION", verificationRequired:true} |
| UserSummary | {id:Id, email:string, fullName:string, avatarUrl:string\|null, role:Role, accountStatus:AccountStatus, emailVerified:boolean, createdAt:Time} |
| AdminUserDetail | {user:UserSummary, updatedAt:Time} - existing backend-maintained inspection metadata only; no credentials or unrelated personal data |
| TeacherSummary | {id:Id, fullName:string, avatarUrl:string\|null} |
| Course | {id:Id, title:string, description:string\|null, cefrLevel:Cefr, teacher:TeacherSummary, publicationStatus:CourseStatus, accessClassification:AccessTier} |
| CourseDetail | {course:Course, access:{enrolled:boolean\|null, canLearn:boolean, reasonCode:string\|null}} |
| DiscoveryLessonSummary | {id:Id, title:string, topic:string\|null, position:integer} - no protected Lesson content |
| DiscoveryCourseDetail | {course:Course, access:{enrolled:boolean\|null, canLearn:boolean, reasonCode:string\|null}, lessonSummaries:DiscoveryLessonSummary[]} |
| AdminCourseDetail | {course:Course, createdAt:Time, updatedAt:Time} - inspection/classification context, not new content-edit permission |
| Lesson | {id:Id, courseId:Id, title:string, topic:string\|null, content:string\|null, position:integer} |
| Sense | {id:Id, partOfSpeech:string\|null, englishDefinition:string\|null, vietnameseMeaning:string\|null, exampleSentence:string\|null} |
| Vocabulary | {id:Id, word:string, cefrLevel:Cefr\|null, ipa:string\|null, audioAvailable:boolean, audioReference:string\|null, senses:Sense[]} |
| LessonVocabulary | {vocabularySenseId:Id, position:integer, vocabulary:Vocabulary} |
| ExerciseSummary | {id:Id, lessonId:Id, type:ExerciseType, title:string\|null, instructions:string\|null, position:integer} |
| AuthorQuestion | {id:Id, exerciseId:Id, vocabularyId:Id, vocabularySenseId:Id\|null, format:QuestionFormat, prompt:string, options:Option[]\|null, expectedAnswer:string, audioReference:string\|null, position:integer} |
| Option | {id:string, text:string} |
| LearnerQuestion | {position:integer, format:QuestionFormat, prompt:string, options:Option[]\|null, audioReference:string\|null} |
| Enrollment | {id:Id, courseId:Id, enrolledAt:Time} |
| AttemptSummary | {id:Id, context:"LESSON"\|"REVIEW", exerciseId:Id\|null, type:ExerciseType, startedAt:Time, completedAt:Time\|null, totalQuestions:integer, score:string\|null, accuracy:string\|null} |
| AttemptDelivery | {attempt:AttemptSummary, questions:LearnerQuestion[], submissionContext?:string} |
| AnswerReceipt | {attemptId:Id, position:integer, accepted:true} |
| AttemptResult | {attempt:AttemptSummary, answeredCount:integer, correctCount:integer, courseTitle:string\|null, lessonTitle:string\|null} |
| AnswerHistory | {position:integer, format:QuestionFormat, word:string, sense:string\|null, prompt:string, submittedAnswer:string, isCorrect:boolean, answeredAt:Time} |
| CourseProgress | {courseId:Id, completedRequiredLessons:integer, totalRequiredLessons:integer, percentage:string\|null, lessons:{lessonId:Id, completed:boolean}[]} |
| VocabularyPerformance | {vocabularyId:Id, answeredCount:IdCount, correctCount:IdCount, mastery:string\|null, category:"INSUFFICIENT_DATA"\|"WEAK"\|"LEARNING"\|"MASTERED", lastAnsweredAt:Time} |
| SavedVocabulary | {vocabulary:Vocabulary, savedAt:Time} |
| Plan | {id:Id, type:"MONTHLY"\|"YEARLY", displayName:string, price:Amount, currency:string} |
| Entitlement | {accessTier:AccessTier, asOf:Time, currentCoverageEndsAt:Time\|null} |
| StudentPeriod | {planId:Id, startsAt:Time, endsAt:Time} - own subscription/access history only |
| AdminPeriod | {id:Id, studentId:Id, planId:Id, originatingPaymentId:Id, startsAt:Time, endsAt:Time, createdAt:Time} - authorized administrative traceability |
| Payment | {id:Id, planId:Id, purchasedPlanType:"MONTHLY"\|"YEARLY", amount:Amount, currency:string, outcome:string, initiatedAt:Time, verifiedAt:Time\|null} |
| PaymentInitiation | {transaction:Payment, continuation?:PaymentContinuation} - PROVISIONAL; PaymentContinuation is a closed allowlisted DTO whose permitted fields must be defined after provider approval; not arbitrary JSON/object or a raw provider payload |
| TeacherAnalytics | {courseId:Id, enrollmentCount:IdCount, completedStudentCount:IdCount, averageScore:string\|null, difficultVocabulary:{vocabularyId:Id, answeredCount:IdCount, correctCount:IdCount}[]} |
| SubscriptionAnalytics | {asOf:Time, activePremiumStudentCount:IdCount\|null, newSubscriptionCount:IdCount\|null, renewalCount:IdCount\|null, unavailableMetrics:string[]} |
| Revenue | {from:Time, to:Time, currencies:{currency:string, total:Amount, buckets:{start:Time, end:Time, amount:Amount}[], plans:{planId:Id\|null, purchasedPlanType:string\|null, amount:Amount}[]}[]} |
| Overview | {asOf:Time, studentCount:IdCount, teacherCount:IdCount, courseCount:IdCount, subscriptions:SubscriptionAnalytics, successfulPaymentCount:IdCount, failedPaymentCount:IdCount\|null, lifetimeRevenue:{currency:string,amount:Amount}[], selectedRevenue:Revenue, unavailableMetrics:string[]} |

Role, AccountStatus, CourseStatus, Cefr, AccessTier, ExerciseType and QuestionFormat
use only the sets already specified in DATABASE_DESIGN.md. Ratios/percentages use
decimal strings to avoid unspecified rounding; scale/rounding remains a presentation
decision. NULL means unavailable/not applicable, never numeric zero.
Arrays inside an aggregate represent only the requested Course/report scope;
large detail listings use the paginated operations below. Pagination for large
within-Course progress or analytics detail is provisional if needed.

Profile validation V-PROFILE: fullName is trimmed, required/non-null/nonblank when
supplied, at most 200 characters; avatarUrl nullable, at most 2048 characters,
absolute HTTPS validated by Spring Boot, no reachability check. Registration and
new authorized account creation require fullName; PATCH omission leaves unchanged.
Reject email/password/role/status/verification/timestamps/entitlement and unknown
properties in profile PATCH. No Admin self-edit permission is inferred.

V-COURSE: title required on creation; description optional nullable; supported
cefrLevel required by current design. Teacher mutation excludes accessClassification,
owner, status and timestamps. Status operations remain separate approved workflows.
V-LESSON: title and position required on creation; topic/content nullable; position
nonnegative. Required-section configuration is not silently exposed as a new policy.
V-QUESTION: only approved formats, deterministic expected answer, relevant Lesson
vocabulary, sense belongs to word, unique valid option identifiers, expected option
exists, and Listening has usable approved audio. Option shape is an API proposal.
Unknown advanced exercise variants, arbitrary executable payloads and provider
response blobs are rejected. Existing expected-answer data is author-only.

StudentPeriod and AdminPeriod are projections of the same immutable entitlement
records. Student history excludes originatingPaymentId and grant-creation metadata;
Admin traceability remains access-controlled. PaymentContinuation must never expose
provider secrets, signatures, credentials or raw internal provider responses. No
continuation fields or provider workflow are approved by this placeholder.

Premium benefits remain approved static product/UI content (FR-PRE-001,
UC-PRE-BENEFITS-01); no backend benefits route is proposed. Dynamic/server-managed
benefits require a future API decision.

## 7. Operation catalogue

Common conventions, errors and access notation apply to every entry. Per-operation
errors below emphasize applicable failures; no body means no JSON request payload.

### 7.1 Authentication and account lifecycle

#### 1. POST /auth/register

- Purpose: Register a Student.
- Readiness: PROVISIONAL — Provisional email/password policy.
- Access: PUBLIC.
- Request DTO: {email:string, password:string, fullName:string}.
- Response DTO: Registration.
- Validation: V-PROFILE; email uniqueness/normalization and password policy as approved; reject privileged role/entitlement input; create STUDENT, PENDING_VERIFICATION and default Standard access.
- Success: 201.
- Important errors: 400, 409, 429, 503; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Email uniqueness prevents a second account; an uncertain retry may return 409. No credential is returned in conflict responses.
- References: FR-STU-001; BR-AUTHN-001; BR-AUTHN-004; BR-PROFILE-001; UC-AUTH-REGISTER-01.

#### 2. POST /auth/email-verification

- Purpose: Consume verification credential.
- Readiness: FINAL.
- Access: PUBLIC credential holder.
- Request DTO: {credential:string}.
- Response DTO: Acknowledgement.
- Validation: Account-bound purpose, expiry and consumption checks; do not override lock/disable state.
- Success: 200.
- Important errors: 400, 409, 429; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Atomic consumption; used credential cannot reactivate or bypass current eligibility.
- References: FR-ACC-008; BR-AUTHN-004; BR-AUTHN-006; UC-AUTH-VERIFY-EMAIL-01.

#### 3. POST /auth/email-verification/resend

- Purpose: Request verification message.
- Readiness: PROVISIONAL — Provisional resend policy.
- Access: PUBLIC neutral request; eligibility resolved server-side.
- Request DTO: {email:string}.
- Response DTO: Acknowledgement.
- Validation: Only eligible unverified accounts; neutral response; verification lifetime and resend policy remain open.
- Success: 202.
- Important errors: 400, 429, 503; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Do not automatically retry delivery; replacement/coexisting credential policy unresolved.
- References: FR-ACC-009; BR-AUTHN-005; BR-AUTHN-006; UC-AUTH-VERIFY-EMAIL-01.

#### 4. POST /auth/login

- Purpose: Authenticate existing account.
- Readiness: PROVISIONAL — Provisional AUTH-WIRE.
- Access: PUBLIC.
- Request DTO: {email:string, password:string}.
- Response DTO: AuthSession under AUTH-WIRE.
- Validation: Verify password and current status; pending-user limited access behavior remains constrained by BR-AUTHN-005.
- Success: 200.
- Important errors: 400, 401, 403, 429; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Creates authentication state; concurrent sessions/retry behavior provisional.
- References: FR-STU-002; FR-TEA-001; FR-ADM-001; BR-AUTHN-005; UC-AUTH-LOGIN-01.

#### 5. POST /auth/refresh

- Purpose: Refresh authentication.
- Readiness: PROVISIONAL — Provisional AUTH-WIRE and rotation.
- Access: Valid refresh credential and eligible account/session; not dependent on unexpired access JWT.
- Request DTO: AUTH-WIRE refresh credential; final body/cookie shape unresolved.
- Response DTO: AuthSession under AUTH-WIRE.
- Validation: Expiry, revocation, account and applicable session checks; support approved rotation.
- Success: 200.
- Important errors: 400, 401, 403, 409, 429; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Rotation/replay/race policy unresolved; no automatic retry guarantee.
- References: FR-ACC-003; FR-ACC-004; FR-ACC-005; BR-AUTHN-009; BR-AUTHN-010; UC-AUTH-REFRESH-01.

#### 6. POST /auth/logout

- Purpose: End applicable authentication session.
- Readiness: PROVISIONAL — Provisional AUTH-WIRE.
- Access: Applicable authenticated/refresh session under approved transport.
- Request DTO: AUTH-WIRE session credential; no client-selected User.
- Response DTO: No body.
- Validation: Revoke applicable server refresh state and clear applicable browser credentials; does not promise instant JWT revocation.
- Success: 204.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Repeat has no further business effect; session selection and expired-access behavior provisional.
- References: FR-STU-003; BR-AUTHN-011; UC-AUTH-LOGOUT-01.

#### 7. POST /auth/password/change

- Purpose: Change own password.
- Readiness: PROVISIONAL — Provisional password/session policy.
- Access: CURRENT, SELF; eligible under security rules.
- Request DTO: {currentPassword:string, newPassword:string}.
- Response DTO: No body.
- Validation: Verify required security checks; do not accept password hashes; session invalidation remains open.
- Success: 204.
- Important errors: 400, 401, 403, 429; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: No blind retry; old-password validation can differ after success.
- References: FR-ACC-010; BR-AUTHN-013; UC-AUTH-CHANGE-PASSWORD-01.

#### 8. POST /auth/password/forgot

- Purpose: Request password recovery.
- Readiness: FINAL.
- Access: PUBLIC.
- Request DTO: {email:string}.
- Response DTO: Acknowledgement.
- Validation: Neutral existence response; issue only approved account-bound recovery credential.
- Success: 202.
- Important errors: 400, 429, 503; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Do not automatically repeat delivery; resend/rate details remain open.
- References: FR-ACC-011; BR-AUTHN-014; UC-AUTH-FORGOT-PASSWORD-01.

#### 9. POST /auth/password/reset

- Purpose: Consume recovery credential.
- Readiness: PROVISIONAL — Provisional password/session policy.
- Access: PUBLIC credential holder.
- Request DTO: {credential:string, newPassword:string}.
- Response DTO: No body.
- Validation: Unexpired purpose-bound single-use credential; configured reset lifetime; validate password; session invalidation open.
- Success: 204.
- Important errors: 400, 409, 429; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Atomic single-use consumption; reused credential rejected, never another password change.
- References: FR-ACC-012; FR-ACC-013; BR-AUTHN-015; BR-AUTHN-016; UC-AUTH-RESET-PASSWORD-01.

### 7.2 Current User and personal profile

#### 10. GET /me

- Purpose: Read current permitted account data.
- Readiness: FINAL.
- Access: CURRENT, SELF.
- Request DTO: No body.
- Response DTO: CurrentUser.
- Validation: Expose no hashes/credential state; entitlement is derived and applicable to Student only.
- Success: 200.
- Important errors: 401, 403; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ACC-007; FR-STU-004; FR-TEA-002; BR-AUTHN-012; UC-AUTH-ME-01.

#### 11. PATCH /me/profile

- Purpose: Update own approved profile.
- Readiness: FINAL.
- Access: S or T, SELF.
- Request DTO: {fullName?:string, avatarUrl?:string|null}.
- Response DTO: Profile.
- Validation: V-PROFILE; fullName null/blank rejected; avatar null clears; omitted properties unchanged; reject protected/unknown keys; no Admin self-edit grant.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Identical field assignments are semantically idempotent; concurrent update/version policy not invented.
- References: FR-STU-005; FR-TEA-003; BR-PROFILE-001; UC-AUTH-PROFILE-01.

### 7.3 Course discovery and Student Lesson access

#### 12. GET /courses

- Purpose: Browse discoverable Courses.
- Readiness: PROVISIONAL — Anonymous availability provisional.
- Access: DISCOVERY.
- Request DTO: No body.
- Response DTO: Page<Course>.
- Validation: Only PUBLISHED discovery; no full restricted Lesson content; role/access tier not conflated.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; filter cefrLevel; sort title asc (default), id.
- Retry/idempotency: Read-only.
- References: FR-DIS-001; FR-DIS-002; BR-CSTATUS-002; UC-STU-BROWSE-COURSES-01.

#### 13. GET /courses/{courseId}

- Purpose: View permitted Course summary and access indication.
- Readiness: PROVISIONAL — Anonymous availability provisional.
- Access: DISCOVERY.
- Request DTO: No body.
- Response DTO: DiscoveryCourseDetail.
- Validation: Published discovery and permitted Teacher information; optional anonymous enrolled is null; no fabricated entitlement. Discovery Lesson summaries include only id/title/topic/position, never protected body, vocabulary content, questions or answer keys; summary visibility follows approved Course visibility.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only.
- References: FR-DIS-003; FR-DIS-004; BR-COURSE-005; UC-STU-VIEW-COURSE-01.

#### 14. GET /courses/{courseId}/lessons

- Purpose: List accessible Course Lessons.
- Readiness: FINAL.
- Access: S, LEARN.
- Request DTO: No body.
- Response DTO: Page<Lesson>.
- Validation: Verify Course enrollment/access before full Lesson data.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; sort position asc then id; no filters.
- Retry/idempotency: Read-only; no mutation.
- References: FR-LES-001; BR-ENR-001; BR-ENR-003; UC-STU-VIEW-LESSON-01.

#### 15. GET /lessons/{lessonId}

- Purpose: Open accessible Lesson.
- Readiness: FINAL.
- Access: S, LEARN via parent Course.
- Request DTO: No body.
- Response DTO: Lesson.
- Validation: Backend parent traversal and access checks; repeat permitted; viewing does not complete a Lesson.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-LES-002; FR-LES-004; BR-LCOMP-002; UC-STU-VIEW-LESSON-01.

#### 16. GET /lessons/{lessonId}/vocabulary

- Purpose: Learn selected Lesson senses.
- Readiness: FINAL.
- Access: S, LEARN.
- Request DTO: No body.
- Response DTO: Page<LessonVocabulary>.
- Validation: Return licensed available data; retain selected-sense meaning; audio availability honest.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; sort position asc then id; no filters.
- Retry/idempotency: Read-only; no mutation.
- References: FR-VOC-001; FR-VOC-004; FR-VOC-005; BR-VOC-005; UC-LEARN-VOCABULARY-01.

#### 17. GET /lessons/{lessonId}/exercises

- Purpose: List accessible assessments.
- Readiness: FINAL.
- Access: S, LEARN.
- Request DTO: No body.
- Response DTO: Page<ExerciseSummary>.
- Validation: No answer keys; enforce only approved access restrictions.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; optional type; sort position asc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-FILL-001; FR-LIS-001; FR-QUIZ-001; BR-EX-001; UC-LEARN-FILL-01; UC-LEARN-LISTENING-01; UC-LEARN-QUIZ-01.

### 7.4 Teacher Course management

#### 18. POST /teacher/courses

- Purpose: Create owned Course.
- Readiness: FINAL.
- Access: T; owner from identity.
- Request DTO: {title:string, description?:string|null, cefrLevel:Cefr}.
- Response DTO: Course.
- Validation: V-COURSE; backend DRAFT/STANDARD; reject accessClassification, owner and status input.
- Success: 201.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Not retry-safe creation; reconcile list after ambiguous response.
- References: FR-TCR-001; FR-TCR-002; FR-TCR-003; BR-COURSE-005; BR-CSTATUS-001; UC-TEA-COURSE-01.

#### 19. GET /teacher/courses

- Purpose: List owned Courses.
- Readiness: FINAL.
- Access: T, OWN scope.
- Request DTO: No body.
- Response DTO: Page<Course>.
- Validation: Backend scope; no foreign-owner filter bypass.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; filters cefrLevel, publicationStatus, accessClassification; sort title asc default, createdAt, id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-TCR-004; BR-COURSE-004; UC-TEA-COURSE-01.

#### 20. GET /teacher/courses/{courseId}

- Purpose: Read owned Course for editing.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Course.
- Validation: Ownership required even when draft/archived.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-TCR-004; BR-COURSE-004; UC-TEA-COURSE-01.

#### 21. PATCH /teacher/courses/{courseId}

- Purpose: Edit owned Course details.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: {title?:string, description?:string|null, cefrLevel?:Cefr}.
- Response DTO: Course.
- Validation: V-COURSE; preserve approved content editing; classification and arbitrary status excluded.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Repeat identical assignments is semantically idempotent.
- References: FR-TCR-005; FR-TCR-006; BR-COURSE-004; BR-COURSE-006; UC-TEA-COURSE-01.

#### 22. POST /teacher/courses/{courseId}/publish

- Purpose: Publish owned Course.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Course.
- Validation: Validate approved lifecycle/content conditions; no new restore/unarchive permission inferred.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Already-published same authorized operation returns current state; other transitions follow approved lifecycle.
- References: FR-TCR-005; BR-CSTATUS-001; BR-CSTATUS-002; UC-TEA-PUBLISH-01.

#### 23. POST /teacher/courses/{courseId}/archive

- Purpose: Archive owned Course.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Course.
- Validation: Preserve enrollment/history; no new enrollments while archived.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Already archived returns current state; no destructive cascade.
- References: FR-TCR-005; BR-CSTATUS-003; UC-TEA-PUBLISH-01.

### 7.5 Teacher Lesson management

#### 24. GET /teacher/courses/{courseId}/lessons

- Purpose: List owned content.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Page<Lesson>.
- Validation: Parent ownership; no Student eligibility substitute.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; sort position asc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-TLE-001; FR-TLE-006; BR-LESSON-001; UC-TEA-LESSON-01.

#### 25. POST /teacher/courses/{courseId}/lessons

- Purpose: Create Lesson.
- Readiness: PROVISIONAL — Required-completion configuration provisional.
- Access: T, OWN.
- Request DTO: {title:string, topic?:string|null, content?:string|null, position:integer}.
- Response DTO: Lesson.
- Validation: V-LESSON; backend parent; initial required-section mapping remains P1.
- Success: 201.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Creation not automatically retry-safe.
- References: FR-TLE-001; FR-TLE-003; BR-LESSON-001; BR-LESSON-003; UC-TEA-LESSON-01.

#### 26. PATCH /teacher/lessons/{lessonId}

- Purpose: Edit Lesson content/order.
- Readiness: FINAL.
- Access: T, OWN through Course.
- Request DTO: {title?:string, topic?:string|null, content?:string|null, position?:integer}.
- Response DTO: Lesson.
- Validation: V-LESSON; no parent reassignment or history deletion.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Identical assignments semantically idempotent.
- References: FR-TLE-002; FR-TLE-003; FR-TLE-006; BR-LESSON-003; UC-TEA-LESSON-01.

### 7.6 Shared vocabulary and Teacher dictionary usage

#### 27. GET /vocabulary

- Purpose: Search existing shared vocabulary.
- Readiness: PROVISIONAL — search matching/normalization semantics.
- Access: S or T; basic Student search not Premium-only.
- Request DTO: No body.
- Response DTO: Page<Vocabulary>.
- Validation: q must be nonblank when supplied; no automatic provider import from a GET; licensed fields only.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; q, cefrLevel; sort word asc then id; exact matching semantics provisional.
- Retry/idempotency: Read-only; no mutation.
- References: FR-SEA-001; FR-SEA-005; FR-TVOC-001; BR-SEARCH-001; UC-VOC-SEARCH-01.

#### 28. GET /vocabulary/{vocabularyId}

- Purpose: View word/senses and available audio reference.
- Readiness: FINAL.
- Access: S or T.
- Request DTO: No body.
- Response DTO: Vocabulary.
- Validation: Basic access maintained; approved Premium dataset restrictions only; no audio upload/proxy endpoint.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-SEA-002; FR-SEA-003; FR-VOC-004; BR-SEARCH-002; UC-VOC-DETAIL-01.

#### 29. GET /teacher/dictionary-candidates

- Purpose: Retrieve candidates for missing content.
- Readiness: PROVISIONAL — Provider/license-dependent.
- Access: T.
- Request DTO: No body.
- Response DTO: {items:VocabularyCandidate[]} where VocabularyCandidate={reference:string,word:string,senses:SenseCandidate[],ipa:string|null}; SenseCandidate={reference:string,partOfSpeech:string|null,englishDefinition:string|null,vietnameseMeaning:string|null,exampleSentence:string|null}.
- Validation: Platform-first lookup; provider rights and mapping required; candidate references are not platform IDs.
- Success: 200.
- Important errors: 400, 401, 403, 503; apply the common error model.
- Pagination/filter/sort: q required; bounded result count/provider continuation contract provisional.
- Retry/idempotency: Read-only; no permanent import.
- References: FR-TVOC-003; FR-TVOC-004; BR-DIC-001; BR-DIC-004; UC-TEA-VOCABULARY-01.

#### 30. POST /teacher/vocabulary/imports

- Purpose: Accept reviewed permitted dictionary content.
- Readiness: PROVISIONAL — Provider/license/normalization-dependent.
- Access: T.
- Request DTO: {candidateReference:string, selectedSenseReferences:string[]} - provisional provider-neutral selection.
- Response DTO: Vocabulary.
- Validation: Backend revalidates provider candidate and license; canonical deduplication; no arbitrary provider payload binding.
- Success: 201 new or 200 reused.
- Important errors: 400, 401, 403, 409, 503; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Canonical identity prevents duplicate word import once normalization is approved.
- References: FR-TVOC-004; FR-TVOC-005; BR-DIC-003; BR-DIC-004; UC-TEA-VOCABULARY-01.

### 7.7 Lesson vocabulary assignments

#### 31. GET /teacher/lessons/{lessonId}/vocabulary

- Purpose: Inspect owned Lesson assignments.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Page<LessonVocabulary>.
- Validation: Resolve shared sense without cloning word per Teacher.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; sort position asc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-TLE-004; FR-TLE-005; BR-VOC-001; UC-TEA-VOCABULARY-01.

#### 32. PUT /teacher/lessons/{lessonId}/vocabulary/{senseId}

- Purpose: Assign or reorder a selected sense.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: {position:integer}.
- Response DTO: LessonVocabulary.
- Validation: Existing sense; position nonnegative; duplicate Lesson/sense prohibited; preserve history.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Natural Lesson/sense identity; repeat updates same association.
- References: FR-TLE-004; FR-TLE-005; BR-VOC-005; UC-TEA-VOCABULARY-01.

#### 33. DELETE /teacher/lessons/{lessonId}/vocabulary/{senseId}

- Purpose: Remove owned Lesson association.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: No body.
- Validation: Do not delete canonical word/sense or historical results; referenced live-question consistency must be validated.
- Success: 204.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Already absent returns 204 after parent ownership check.
- References: FR-TLE-002; BR-DATA-002; UC-TEA-VOCABULARY-01.

### 7.8 Exercise and question management

#### 34. GET /teacher/lessons/{lessonId}/exercises

- Purpose: List owned exercises.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Page<ExerciseSummary>.
- Validation: Ownership and supported types.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; filter type; sort position asc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-TEX-001; FR-TEX-002; FR-TEX-003; BR-EX-001; UC-TEA-EXERCISE-01.

#### 35. POST /teacher/lessons/{lessonId}/exercises

- Purpose: Create core exercise.
- Readiness: PROVISIONAL — Required-completion configuration provisional.
- Access: T, OWN.
- Request DTO: {type:ExerciseType, title?:string|null, instructions?:string|null, position:integer}.
- Response DTO: ExerciseSummary.
- Validation: Only three core types; nonnegative position; no arbitrary advanced engine; required-section mapping provisional.
- Success: 201.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Creation not automatically retry-safe.
- References: FR-TEX-001; FR-TEX-002; FR-TEX-003; BR-EX-001; UC-TEA-EXERCISE-01.

#### 36. PATCH /teacher/exercises/{exerciseId}

- Purpose: Edit exercise metadata/order.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: {title?:string|null, instructions?:string|null, position?:integer}.
- Response DTO: ExerciseSummary.
- Validation: Do not change historical type/results; type-change semantics not approved, so not silently exposed.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Identical assignment semantically idempotent.
- References: FR-TEX-005; BR-EX-002; UC-TEA-EXERCISE-01.

#### 37. GET /teacher/exercises/{exerciseId}/questions

- Purpose: Read owned authoring questions.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: Page<AuthorQuestion>.
- Validation: Author-only expectedAnswer; never reuse response DTO for learners.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Page; sort position asc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-TEX-004; FR-TEX-005; BR-EX-003; UC-TEA-EXERCISE-01.

#### 38. POST /teacher/exercises/{exerciseId}/questions

- Purpose: Create core question.
- Readiness: FINAL.
- Access: T, OWN.
- Request DTO: {vocabularyId:Id, vocabularySenseId?:Id|null, format:QuestionFormat, prompt:string, options?:Option[]|null, expectedAnswer:string, audioReference?:string|null, position:integer}.
- Response DTO: AuthorQuestion.
- Validation: V-QUESTION; backend validates parent Lesson relevance and licensed audio.
- Success: 201.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Creation not automatically retry-safe.
- References: FR-TEX-004; FR-TEX-005; BR-EX-003; BR-LIS-001; UC-TEA-EXERCISE-01.

#### 39. PATCH /teacher/questions/{questionId}

- Purpose: Edit owned question content/order.
- Readiness: PROVISIONAL — Depends on issued-question integrity protocol.
- Access: T, OWN.
- Request DTO: Optional versions of the POST question fields; vocabularyId/format/prompt/expectedAnswer/position cannot be null.
- Response DTO: AuthorQuestion.
- Validation: V-QUESTION; completed snapshots immutable; issued-attempt content binding must prevent regrading against changed content.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Identical assignment semantically idempotent; active-delivery consistency unresolved.
- References: FR-TEX-004; FR-TEX-005; BR-DATA-003; UC-TEA-EXERCISE-01.

### 7.9 Enrollment

#### 40. PUT /me/courses/{courseId}/enrollment

- Purpose: Enroll in an accessible Course.
- Readiness: FINAL.
- Access: S, SELF, LEARN enrollment eligibility.
- Request DTO: No body.
- Response DTO: Enrollment.
- Validation: Published/enrollable Course, backend Premium if required; no per-Course purchase.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Natural Student/Course identity; no duplicate enrollment. Existing association remains after expiry; response does not grant renewed content access.
- References: FR-ENR-001; FR-ENR-002; FR-ENR-003; BR-ENR-004; UC-STU-ENROLL-01.

#### 41. GET /me/courses

- Purpose: List enrolled Courses.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Page<CourseDetail>.
- Validation: Preserve enrollment visibility after expiry with truthful access restrictions.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; filter cefrLevel; sort enrolledAt desc default, title, id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ENR-004; BR-ENR-005; UC-STU-MY-COURSES-01.

### 7.10 Attempts, answer submission and results

#### 42. POST /me/attempts

- Purpose: Start Lesson attempt or Review activity.
- Readiness: PROVISIONAL — Issued-question binding and creation retry protocol provisional.
- Access: S, SELF; LEARN for LESSON, approved Review access for REVIEW.
- Request DTO: LESSON: {context:"LESSON",exerciseId:Id}; REVIEW: {context:"REVIEW",type:ExerciseType,source:"SAVED"|"WEAK"|"INCORRECT"|"RECENT",limit?:integer}.
- Response DTO: AttemptDelivery.
- Validation: Discriminated input rejects fake Lesson/Exercise for Review; backend selects question set, total, target words and evaluation data. Review limit proposal 1..50, default 10; selection algorithm and RECENT window open.
- Success: 201.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Creation retry correlation unresolved; no blind duplicate attempt creation.
- References: FR-REV-001; FR-REV-006; BR-EX-002; BR-REV-001; UC-LEARN-FILL-01; UC-LEARN-LISTENING-01; UC-LEARN-QUIZ-01; UC-REV-REVIEW-01.

#### 43. GET /me/attempts/{attemptId}

- Purpose: Read owned attempt state/result summary.
- Readiness: FINAL.
- Access: S, SELF attempt ownership.
- Request DTO: No body.
- Response DTO: AttemptSummary when incomplete; AttemptResult when completed.
- Validation: Never expose another Student's attempt; no mutable answer keys. Completed reads return the same historical result projection as completion, including snapshot labels and counts; no re-evaluation against current content or expected-answer disclosure.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-PRO-001; FR-PRO-008; BR-HIST-001; UC-LEARN-PROGRESS-01.

#### 44. PUT /me/attempts/{attemptId}/answers/{position}

- Purpose: Submit one issued answer.
- Readiness: PROVISIONAL — Issued-question binding and feedback timing provisional.
- Access: S, SELF attempt ownership; applicable learning access.
- Request DTO: {response:{text:string}|{optionId:string}, submissionContext?:string}.
- Response DTO: AnswerReceipt.
- Validation: Exactly one response variant matching issued format; backend verifies issued slot, ownership, question/word/sense and original evaluation content. No accepted score/correctness/expected answer. Context verification cannot trust an opaque string merely because it exists.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Identical accepted slot retry returns receipt; altered retry or new write after completion returns 409. Slot consistency is checked transactionally.
- References: FR-FILL-005; FR-LIS-005; FR-QUIZ-008; BR-EX-003; BR-FILL-003; BR-LIS-003; UC-LEARN-FILL-01; UC-LEARN-LISTENING-01; UC-LEARN-QUIZ-01; UC-REV-REVIEW-01.

#### 45. POST /me/attempts/{attemptId}/completion

- Purpose: Finalize trusted scoring.
- Readiness: PROVISIONAL — Completion eligibility provisional.
- Access: S, SELF attempt ownership.
- Request DTO: No body.
- Response DTO: AttemptResult.
- Validation: Backend uses original total/questions and recorded answers, computes score/accuracy and atomically freezes results; no client totals. Completion eligibility/skipped answers remain open.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Repeat returns original immutable result; concurrent answer/completion cannot duplicate or change accepted result.
- References: FR-PRO-001; FR-PRO-002; BR-SCORE-001; BR-ACC-001; BR-HIST-001; UC-LEARN-PROGRESS-01.

#### 46. GET /me/attempts/{attemptId}/answers

- Purpose: Read owned historical answers.
- Readiness: FINAL.
- Access: S, SELF attempt ownership.
- Request DTO: No body.
- Response DTO: Page<AnswerHistory>.
- Validation: Completed history uses immutable snapshots; never expose expected/correct-answer keys in learner payloads. Own submitted response and backend correctness are result evidence.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: Page; sort position asc; reject history read before completion in this proposal.
- Retry/idempotency: Read-only; no mutation.
- References: FR-PRO-008; BR-HIST-002; BR-DATA-003; UC-LEARN-PROGRESS-01.

### 7.11 Progress and history

#### 47. GET /me/attempts

- Purpose: List own learning history.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Page<AttemptSummary>.
- Validation: Include retained history regardless of current Premium; does not unlock current restricted content.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; context, exerciseId, from, to; sort completedAt desc default, startedAt, id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-PRO-008; BR-HIST-003; UC-LEARN-PROGRESS-01.

#### 48. GET /me/courses/{courseId}/progress

- Purpose: Read derived Course/Lesson progress.
- Readiness: PROVISIONAL — Required-section mapping/content-change effects provisional.
- Access: S, SELF enrollment history.
- Request DTO: No body.
- Response DTO: CourseProgress.
- Validation: Use approved completion rules; preserve history after expiry; no writable percentages.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only.
- References: FR-PRO-003; FR-PRO-004; BR-LCOMP-002; BR-CCOMP-001; BR-CCOMP-002; UC-LEARN-PROGRESS-01.

#### 49. GET /me/vocabulary-performance

- Purpose: Read own mastery and weak-word evidence.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Page<VocabularyPerformance>.
- Validation: Approved minimum three answers and thresholds; aggregate retained trusted evidence.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; category; sort lastAnsweredAt desc default, vocabularyId.
- Retry/idempotency: Read-only; no mutation.
- References: FR-PRO-005; FR-PRO-006; FR-PRO-007; BR-MAST-003; BR-WEAK-001; UC-LEARN-PROGRESS-01.

### 7.12 Saved vocabulary

#### 50. GET /me/saved-vocabulary

- Purpose: List saved words.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Page<SavedVocabulary>.
- Validation: Basic saved association survives Premium expiry.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; sort savedAt desc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-SAV-002; BR-SAVE-004; UC-VOC-MY-01.

#### 51. PUT /me/saved-vocabulary/{vocabularyId}

- Purpose: Save shared word.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: SavedVocabulary.
- Validation: Existing word; no duplicate Student/word association.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Natural association key; repeat returns existing association.
- References: FR-SAV-001; BR-SAVE-002; UC-VOC-SAVE-01.

#### 52. DELETE /me/saved-vocabulary/{vocabularyId}

- Purpose: Remove saved association.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: No body.
- Validation: Do not delete shared vocabulary, answer evidence or history.
- Success: 204.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Repeat returns 204; no global deletion.
- References: FR-SAV-003; BR-SAVE-003; UC-VOC-MY-01.

### 7.13 Plans and current entitlement

#### 53. GET /subscription-plans

- Purpose: View available Premium plans.
- Readiness: FINAL.
- Access: S; anonymous availability not inferred.
- Request DTO: No body.
- Response DTO: {items:Plan[]}.
- Validation: Only available catalog plans; price/currency not proof of later payment.
- Success: 200.
- Important errors: 401, 403; apply the common error model.
- Pagination/filter/sort: Fixed small catalog; no pagination; order plan type then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-SUB-001; BR-SUB-001; UC-PRE-SUBSCRIBE-01.

#### 54. GET /me/entitlement

- Purpose: Read current derived access.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Entitlement.
- Validation: Derive from covering authoritative periods; no mutable premium flag or writable tier. This dedicated current-access projection and the optional /me entitlement summary derive from the same authoritative periods; independent refresh does not create another source of truth.
- Success: 200.
- Important errors: 401, 403; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-SUB-003; FR-SUB-004; FR-SUB-006; BR-SUB-002; UC-PRE-SUBSCRIPTION-01.

#### 55. GET /me/subscriptions

- Purpose: Inspect own entitlement periods.
- Readiness: FINAL.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Page<StudentPeriod>.
- Validation: Return immutable grant history; active/expired/future are time-derived filters, not mutable statuses.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; coverage=ACTIVE|EXPIRED|FUTURE; sort startsAt desc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-SUB-002; FR-SUB-005; BR-SUB-004; UC-PRE-SUBSCRIPTION-01.

### 7.14 Student payment lifecycle

#### 56. POST /me/payments

- Purpose: Initiate approved plan purchase.
- Readiness: PROVISIONAL — provider, allowlisted continuation and initiation deduplication.
- Access: S, SELF.
- Request DTO: {planId:Id}; retry correlation field/header not finalized.
- Response DTO: PaymentInitiation.
- Validation: Server chooses amount/currency from offered plan; no trusted client success/amount/Student; provider approved before activation.
- Success: 201.
- Important errors: 400, 401, 403, 409, 503; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: No automatic retry until durable initiation correlation/provider idempotency contract is approved; no duplicate charges/grants allowed.
- References: FR-PAY-001; FR-PAY-002; FR-PAY-006; BR-PAY-001; UC-PRE-SUBSCRIBE-01.

#### 57. GET /me/payments

- Purpose: Read own transaction history.
- Readiness: PROVISIONAL — Outcome filter vocabulary provisional.
- Access: S, SELF.
- Request DTO: No body.
- Response DTO: Page<Payment>.
- Validation: Historical amounts/currencies; safe public outcome mapping; no secrets/provider payloads.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; outcome, from, to; sort initiatedAt desc then id.
- Retry/idempotency: Read-only.
- References: FR-PAY-002; BR-PAY-003; UC-PRE-SUBSCRIBE-01.

#### 58. GET /me/payments/{paymentId}

- Purpose: Read verified purchase outcome.
- Readiness: PROVISIONAL — shared public Payment outcome mapping.
- Access: S, SELF transaction ownership.
- Request DTO: No body.
- Response DTO: Payment.
- Validation: Read authoritative backend result; browser return URL never activates Premium.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-PAY-003; FR-PAY-004; BR-PAY-002; BR-PAY-004; UC-PRE-SUBSCRIBE-01.

### 7.15 Teacher analytics

#### 59. GET /teacher/courses/{courseId}/analytics

- Purpose: Read owned Course aggregates.
- Readiness: PROVISIONAL — Detailed analytics population/averaging provisional.
- Access: T, OWN.
- Request DTO: No body.
- Response DTO: TeacherAnalytics.
- Validation: Aggregated learning data only; no platform revenue or unnecessary Student personal details; no approved metric silently replaced by invented formula.
- Success: 200.
- Important errors: 400, 401, 403, 404; apply the common error model.
- Pagination/filter/sort: Optional explicit from/to; aggregation/average population provisional.
- Retry/idempotency: Read-only.
- References: FR-TAN-002; FR-TAN-003; FR-TAN-004; FR-TAN-005; FR-TAN-006; BR-TEA-002; BR-TEA-003; UC-TEA-ANALYTICS-01.

### 7.16 Admin users and Teacher administration

#### 60. GET /admin/users

- Purpose: List/filter permitted users, including Teachers.
- Readiness: FINAL.
- Access: A.
- Request DTO: No body.
- Response DTO: Page<UserSummary>.
- Validation: Role filter is over users; no Teacher identity/table; no credentials exposed.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; role, accountStatus; sort createdAt desc default, fullName, id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ADM-002; FR-ADM-004; BR-ADM-001; UC-ADM-USERS-01.

#### 61. GET /admin/users/{userId}

- Purpose: Inspect permitted user details.
- Readiness: FINAL.
- Access: A.
- Request DTO: No body.
- Response DTO: AdminUserDetail.
- Validation: Only approved account administration visibility.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ADM-002; BR-ADM-001; UC-ADM-USERS-01.

#### 62. POST /admin/teachers

- Purpose: Provision Teacher through approved workflow.
- Readiness: PROVISIONAL — Provisioning credentials, verification and role-assignment transitions provisional.
- Access: A with Teacher-provisioning permission.
- Request DTO: New User: {email:string,fullName:string}; credential/verification provisioning inputs unresolved. Existing User role assignment contract remains deferred..
- Response DTO: UserSummary.
- Validation: V-PROFILE; required fullName for creation; existing valid name can be retained for approved role assignment; no self-registration or generic role setter.
- Success: 201.
- Important errors: 400, 401, 403, 409, 503; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Email uniqueness; no repeated invitation/create guarantee until provisioning protocol approved.
- References: FR-TEA-004; FR-ADM-005; BR-AUTHN-022; BR-PROFILE-001; UC-ADM-TEACHERS-01.

#### 63. POST /admin/users/{userId}/account-actions

- Purpose: Apply approved account administration action.
- Readiness: PROVISIONAL — Not implementable until account-action matrix is approved.
- Access: A with permission for the action/target.
- Request DTO: {action:string} with action allowlist deliberately unresolved.
- Response DTO: UserSummary.
- Validation: Not a generic status/role PATCH. Only explicitly approved transitions/actors may be added; no automatic lockout, unlock or enable powers inferred.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Action-specific retry behavior must be finalized with transition matrix.
- References: FR-ADM-003; FR-ADM-004; FR-ADM-006; BR-AUTHN-018; BR-AUTHN-019; UC-ADM-USERS-01.

### 7.17 Admin Course administration

#### 64. GET /admin/courses

- Purpose: Inspect platform Courses and Teacher-owned lists.
- Readiness: FINAL.
- Access: A.
- Request DTO: No body.
- Response DTO: Page<Course>.
- Validation: teacherId filter supplies Teacher-Course administrative view; no new identity resource.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; teacherId, cefrLevel, publicationStatus, accessClassification; sort createdAt desc default, title, id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ACR-001; FR-ACR-002; BR-ADM-002; UC-ADM-COURSES-01.

#### 65. GET /admin/courses/{courseId}

- Purpose: Inspect Course administrative information.
- Readiness: FINAL.
- Access: A.
- Request DTO: No body.
- Response DTO: AdminCourseDetail.
- Validation: Oversight only; no arbitrary content mutation inferred.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ACR-002; BR-ADM-002; UC-ADM-COURSES-01.

#### 66. PUT /admin/courses/{courseId}/access-classification

- Purpose: Set Course classification.
- Readiness: FINAL.
- Access: A with classification authority.
- Request DTO: {accessClassification:"STANDARD"|"PREMIUM"}.
- Response DTO: Course.
- Validation: No role change or Student entitlement mutation; preserve ownership/history and existing content.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Same target classification is idempotent.
- References: FR-ACR-003; BR-COURSE-006; BR-ADM-003; UC-ADM-CLASSIFY-01.

### 7.18 Admin subscription and payment administration

#### 67. GET /admin/subscriptions

- Purpose: Inspect entitlement periods.
- Readiness: FINAL.
- Access: A.
- Request DTO: No body.
- Response DTO: Page<AdminPeriod>.
- Validation: Authoritative immutable grants, not editable entitlement CRUD.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; studentId, planId, coverage=ACTIVE|EXPIRED|FUTURE; sort startsAt desc then id.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ASU-001; BR-ADM-004; UC-ADM-SUBSCRIPTIONS-01.

#### 68. GET /admin/payments

- Purpose: Inspect transaction history.
- Readiness: PROVISIONAL — Provider outcome mapping provisional.
- Access: A.
- Request DTO: No body.
- Response DTO: Page<Payment & {studentId:Id}>.
- Validation: Historical financial values; no secrets; unverified is not failed.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Page; studentId, planId, currency, outcome, from, to; sort initiatedAt desc default, verifiedAt, id.
- Retry/idempotency: Read-only.
- References: FR-ATR-001; FR-ATR-002; FR-ATR-003; BR-ADM-005; UC-ADM-TRANSACTIONS-01.

#### 69. GET /admin/payments/{paymentId}

- Purpose: Inspect permitted transaction details.
- Readiness: PROVISIONAL — shared public Payment outcome mapping.
- Access: A.
- Request DTO: No body.
- Response DTO: Payment & {studentId:Id, entitlementPeriod:AdminPeriod|null}.
- Validation: At most one linked period; verified success required to grant; no manual browser verification action.
- Success: 200.
- Important errors: 400, 401, 403, 404, 409; apply the common error model.
- Pagination/filter/sort: None.
- Retry/idempotency: Read-only; no mutation.
- References: FR-ATR-001; BR-ADM-005; BR-PAY-001; UC-ADM-TRANSACTIONS-01.

### 7.19 Admin Dashboard and approved analytics

#### 70. GET /admin/dashboard

- Purpose: Read operational Overview Dashboard.
- Readiness: PROVISIONAL — Reporting semantics flagged in section 9.
- Access: A.
- Request DTO: No body.
- Response DTO: Overview.
- Validation: Distinct current Premium Students; historical verified revenue per currency; first/subsequent grants across complete Student history; unresolved metrics explicitly unavailable, not zero.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Explicit from/to proposed to avoid inventing default range; no pagination; reporting boundaries/timezone policy provisional.
- Retry/idempotency: Read-only; no report materialization.
- References: FR-AAN-007; FR-AAN-002; FR-AAN-003; FR-AAN-004; FR-ASU-003; BR-ADM-007; BR-REVN-002; UC-ADM-ANALYTICS-01.

#### 71. GET /admin/analytics/subscriptions

- Purpose: Read subscription operational metrics.
- Readiness: PROVISIONAL — Grant-event time and disabled-Student treatment open.
- Access: A.
- Request DTO: No body.
- Response DTO: SubscriptionAnalytics.
- Validation: First granted period is new, later grants renewal including after expiry; count distinct covering Students, never count future starts as purchases. Dedicated analytics and Dashboard use the same aggregate definitions; independent reporting dimensions remain provisional.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: Explicit from/to; no pagination; grant-event reporting timestamp unresolved.
- Retry/idempotency: Read-only.
- References: FR-ASU-002; FR-ASU-003; BR-ADM-004; BR-ADM-007; UC-ADM-SUBSCRIPTIONS-01.

#### 72. GET /admin/analytics/revenue

- Purpose: Read verified revenue by time and plan evidence.
- Readiness: PROVISIONAL — Reporting bucket policy open.
- Access: A.
- Request DTO: No body.
- Response DTO: Revenue.
- Validation: Each verified payment counted once by verified_at; currencies never summed together; group by stored planId or purchased plan type, not current price/name history.
- Success: 200.
- Important errors: 400, 401, 403; apply the common error model.
- Pagination/filter/sort: from/to required proposal; optional currency, planId, groupBy=PLAN|PLAN_TYPE; interval=DAY|MONTH|YEAR; timezone/boundary contract provisional.
- Retry/idempotency: Read-only.
- References: FR-REVN-001; FR-REVN-002; FR-REVN-003; FR-REVN-004; FR-REVN-006; BR-REVN-002; BR-REVN-003; UC-ADM-REVENUE-01.

## 8. Issued-question integrity, scoring and history

The learner-facing LearnerQuestion and AttemptDelivery DTOs intentionally omit
expected answers, correct option markers, correctness and author-only fields.
The Vocabulary DTO is for learning/search; it must not be embedded wholesale into
assessment delivery when that would directly disclose the tested answer.
AuthorQuestion is available only through authorized Teacher ownership operations.

The backend must bind every issued slot to the authenticated Student, attempt,
context, original question content, target word/sense and evaluation rule.
Changing the current question after delivery must not change the meaning or score
of an already-issued/accepted response. For Review, no fake Lesson/Exercise or
stored exercise_question is required; question_id on its answer remains NULL.

The optional submissionContext is a named placeholder for an integrity protocol,
not an approved plaintext answer key, JWT claim set or custom cryptographic scheme.
The following must be decided before finalizing attempt creation/delivery/submission:
how original trusted generated content survives requests, how resume works, what
is bound to the attempt/slot, expiry behavior, and how a client cannot forge/replay
another issued question. Persistent server records or authenticated confidential
delivery are alternatives for review, not decisions made by this draft.
Never assume a signed but readable token safely hides expected answers.
Do not claim ordinary FK/slot uniqueness solves delivery integrity.

The current 18-table model stores answered snapshots, not a fully specified
pre-answer Review question set. No extra table or column is added here. If the
selected integrity protocol actually requires one, that is a separate database
decision requiring approval; do not hide it in the API implementation.

Before completion the proposed response is an acceptance receipt rather than a
correct-answer disclosure. After completion, read results from immutable evidence;
do not recalculate historical results using current text or current answer keys.
Score = correct / original total questions; accuracy = correct / answered questions
under the approved rules. Zero denominators, skipped-question completion eligibility,
feedback timing and progress effects of content edits remain explicit policy gaps.
Repeated completion returns the original record. Repeated identical slot submissions
cannot add evidence, increase mastery counts or mutate results.

Snapshots contain only the approved minimal interpretive data. Canonical references
support aggregation; no exact old UI replay is promised. Dictionary license rights
apply to historical copies as well as current content. Completed history remains
available under approved account/access policy after Premium expiry without granting
access to currently restricted Lesson content.

## 9. Payment verification and Admin reporting

There is no learner/Admin POST payment-success or grant-entitlement endpoint.
Provider notifications/verification are a required integration boundary, not a
guessed public callback route. Their HTTP method/path, signature, payload, event
identity and acknowledgement behavior remain unassigned until a provider is approved.
This omission is explicit contract deferral, not an assertion that polling a Student
GET endpoint verifies payment.

The backend verifies the trusted provider result, historical amount/currency,
Student and selected plan, records the successful outcome and creates exactly one
immutable entitlement period atomically. A unique originating_payment_id and
transactional checks make repeated processing of the same transaction grant once.
Distinct successful renewals must use serialized coverage calculation, preserving
approved active/expired renewal timing. Failed/invalid payments create no entitlement.

Plan availability/current price is checked for purchase; stored transaction amount,
currency and plan_type_snapshot remain historical. Premium is a platform
subscription; there is no Course payment endpoint. Refund rules remain unresolved;
no accounting, expense, salary or net-profit operations are introduced.

Admin overview uses users, courses, plans, periods and transactions. Count distinct
Students covered now; future/expired periods do not inflate current entitlement.
New subscription is the first successfully granted period over complete Student
history; all subsequent grants are renewals, even after expiration. Filtering by
report range must happen after determining first/subsequent, not redefine the first
purchase inside the range as new. Grant creation time versus verification time for
new/renewal reporting remains open, distinct from revenue's approved verified_at.

Revenue uses verified historical successful payment amounts once each, separated
by currency. Do not multiply values through joins. Historical plan grouping uses
stored planId or purchased plan type; current display names are not historical name
snapshots. There is no conversion into a single total across currencies without
an approved conversion policy.

The Overview/SubscriptionAnalytics nullable unavailable metrics are a proposed
honest representation during unresolved reporting policy, not a permanent waiver of
required dashboard functionality. Finalize required metrics before declaring the
Dashboard implementation complete. Unverified is never automatically failed.
Disabled-Student inclusion, failure mapping/time, report boundaries/timezone and
freshness remain open. Explicit from/to avoids silently selecting a default range,
but does not decide bucket timezone or inclusivity. Those contract details must be
approved before report implementations are finalized.

## 10. Database and earlier-document compatibility

No changes are proposed to the 18 tables, their columns, FKs, indexes or ERD.
All existing approval and security boundaries remain authoritative.
Profile writes map only to users.full_name/avatar_url; Course Teacher creation
supplies DRAFT/STANDARD in Spring Boot, not a new classification database default.
Admin remains a User with role ADMIN, without an inferred self-profile update API.

The following are deliberately not CRUD resources: refresh credential records,
verification/reset rows, answer snapshots, subscription grants and payment provider
state. There is no separate Teacher identity, ReviewSession, progress table,
report warehouse, mutable Premium source of truth or generic permissions API.

No published contradiction requires editing earlier documents at this stage.
The issue register below identifies incomplete operation contracts honestly.
Documentation draft readiness does not imply every operation is ready to build.

## 11. Traceability and coverage

Each operation carries requirement, rule and use-case references. Universal
security coverage also includes FR-AUTH-001 through FR-AUTH-010 and NFR-SEC-001
through NFR-SEC-017 as applicable; browser-specific controls remain AUTH-WIRE open.
BR-AUTH-001 through BR-AUTH-004 require backend role/ownership/entitlement checks.

Student account operations are registration/current-user/profile/password flows,
not a duplicate Student CRUD resource. Teacher authentication uses the same login;
Teacher administration uses /admin/users role filtering and /admin/courses?teacherId.
Teacher Dashboard (FR-TAN-001) composes owned Courses and owned-Course analytics.
CEFR discovery (FR-DIS-001) uses the documented fixed Cefr enum with Course filtering;
a lookup-table CRUD endpoint is unnecessary.
Review sources SAVED/WEAK/INCORRECT/RECENT cover FR-REV-001 through FR-REV-006;
FR-SAV-004 uses the same Review attempt operation.
All three exercise types use the shared attempt/submission/result operations.
Student total/history/preservation needs remain separate from current content access.

FR-AAN-001 total users can be obtained from the paginated authorized user list's
totalElements with no filters. FR-AAN-005/006 are existing optional platform
enrollment/learning aggregates, not silently made mandatory or removed by the
Overview requirement. Their detailed standalone reporting contracts remain deferred
until the requested measures are specified; underlying owned learning/history and
Course analytics operations do not imply unrestricted personal-data disclosure.

## 12. Open decisions and contract readiness

| Issue | Established boundary \| Remaining decision / affected contracts |
|---|---|---|
| AUTH-WIRE | Spring Security + JWT; server-side refresh state \| Cookie/header/body transport, CSRF/CORS, browser storage; login/refresh/logout wire shape |
| Refresh lifecycle | Expiry/revocation validation and rotation support \| Replay/concurrency/session selection, limits and retry behavior |
| Password and verification | Dedicated credential flows and approved defaults \| Complexity, verification lifetime/resend and password-change/reset session invalidation |
| Account administration | Authorized Admin only \| Teacher credential/provisioning process, existing-role assignment, unlock/enable and allowed transition matrix |
| Public discovery | Published content only; no restricted Lesson leakage \| Which discovery reads permit anonymous access |
| Content configuration | Approved Teacher ownership and core types \| Required-section mapping, completion thresholds/eligibility and content-edit effects |
| Delivery integrity | Backend-issued/evaluated content, immutable result snapshots \| Original question persistence/confidential integrity protocol, resume and submission correlation |
| Creation retries | No duplicated accepted answers/results/grants \| Attempt creation identity and durable payment initiation retry protocol |
| Provider integration | Trusted verification, no client success authority \| Dictionary rights/candidates, audio strategy, payment callback and continuation contract |
| Normalization | Unique User identity/canonical vocabulary \| Exact comparison, normalization and conflict behavior |
| Premium features | Basic Standard value preserved \| Final advanced features, benefits and Review restrictions; do not infer from tier labels |
| Reporting | Verified_at revenue, distinct Student coverage, complete-history renewal classification \| Buckets/timezone/boundaries, disabled Students, failure/grant-event timing, refunds and freshness |
| Deployment | Supabase-hosted PostgreSQL and authoritative Spring Boot \| Next.js/Spring Boot hosting, pooling/backups/secrets; no contract-specific infrastructure added |

The Course initialization and editable-profile questions are resolved and must not
appear as unresolved blockers. Profile null handling and protected-field rejection
are exact. The open rows above are not authority to choose missing policies silently.
No global contradiction blocks this draft; flagged operations require further
decisions before their contracts can be considered implementation-ready.

## 13. Validation checklist and implementation handoff limits

- Every method/path pair is unique under /api/v1.
- Every operation identifies authentication/role/ownership, request/response DTO,
  validation, source references, statuses and query/retry behavior.
- All source identifiers resolve to current documents.
- Personal operations resolve identity from authentication; Teacher operations
  traverse ownership; Admin operations do not imply generic mutation privileges.
- Profile registration/provisioning includes required fullName; self-edit excludes
  security fields and Admin. Avatar references do not imply upload/media services.
- Learner question delivery omits expected answers; author and learner DTOs differ.
- BIGINT IDs and monetary values are JSON-safe; currencies accompany money.
- LESSON/REVIEW conditional relationships remain compatible with approved history.
- Payment/report metrics never trust client success or duplicate entitlement grants.
- Provisional fields/contracts are explicitly labeled, not disguised as complete.
- Exactly 18 database tables remain; no SQL, code, configuration or dependency added.
- Future implementation tests should cover role escalation, foreign ownership,
  Premium expiry, protected-field injection, name/avatar validation, answer replay,
  completion races, changed-content history, duplicate payment events, mixed-currency
  reporting and full-history first/renewal classification. No such tests have run
  as part of this documentation-only proposal.

This file is a design proposal. No application endpoints or infrastructure exist
merely because a route is specified here.
