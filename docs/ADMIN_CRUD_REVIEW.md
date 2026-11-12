# Admin CRUD review

Branch: `codex/admin-crud`, baseline `21f9549f5fa9ef94d35f0b6a855ead4e92e3cfa7`.

## Implemented scope

- Admin user creation reuses registration, password encoding and Teacher approval workflows. STUDENT and TEACHER only; ADMIN provisioning is excluded.
- User list/search/role filter/pagination, details, full-name editing and confirmed lock/unlock. Immutable identity, email, role, verification and approval fields cannot be edited.
- Admin accounts cannot be locked or edited through ordinary user management. Locking revokes refresh sessions; existing JWT account checks remain enforced.
- Category search and permanent deletion of unused categories. Referenced categories return 409 CATEGORY_IN_USE and can be deactivated instead. Existing duplicate-name checks and active Teacher selection remain.
- Existing Teacher review and approved/unverified Teacher eligibility are preserved.

## API

- GET/POST /api/v1/admin/users
- GET /api/v1/admin/users/{id}
- PATCH /api/v1/admin/users/{id}/profile (fullName only)
- PATCH /api/v1/admin/users/{id}/lock (locked only)
- GET /api/v1/admin/categories supports search
- DELETE /api/v1/admin/categories/{id} (unused only)

## Verification

106 targeted backend tests passed (including 43 security-boundary and 30 Teacher-review tests). Frontend TypeScript passed. ESLint: zero errors, three existing image warnings. 15 lightweight frontend tests passed. git diff --check passed.

Disposable PostgreSQL lifecycle/deletion regression cases and browser UI regression coverage are prepared, not executed locally. Existing CI groups remain registered; new Admin HTTP/service tests are included in feature-unit-http.

## Boundaries / remaining requirements

Permanent user deletion is unavailable: there is no approved deletion/retention lifecycle for linked financial, teaching and audit records. UI explicitly distinguishes reversible locking from deletion. No destructive substitute is implemented.

Administrative modifications emit actor/target/type operational logs after commit, not durable database audit records. A durable audit table requires separate schema approval. Email changes, role changes and Admin-account provisioning remain outside this workflow.

No production access or data mutation, migration, commit, push or server restart was performed. Original working tree was not edited. Browser and PostgreSQL acceptance must run in CI before release.

## Exact changed/new paths

- `backend/src/main/java/com/englishlearning/features/auth/controller/AdminRegistrationController.java`
- `backend/src/main/java/com/englishlearning/features/auth/controller/VerificationExceptionHandler.java`
- `backend/src/main/java/com/englishlearning/features/auth/security/SecurityConfiguration.java`
- `backend/src/main/java/com/englishlearning/features/auth/service/RegistrationService.java`
- `backend/src/main/java/com/englishlearning/features/auth/service/VerificationFailure.java`
- `backend/src/main/java/com/englishlearning/features/category/controller/CategoryController.java`
- `backend/src/main/java/com/englishlearning/features/category/controller/CategoryExceptionHandler.java`
- `backend/src/main/java/com/englishlearning/features/category/repository/CategoryRepository.java`
- `backend/src/main/java/com/englishlearning/features/category/service/CategoryFailure.java`
- `backend/src/main/java/com/englishlearning/features/category/service/CategoryService.java`
- `backend/src/main/java/com/englishlearning/features/user/controller/AdminUserController.java`
- `backend/src/main/java/com/englishlearning/features/user/dto/AdminUserDto.java`
- `backend/src/main/java/com/englishlearning/features/user/repository/AdminUserRepository.java`
- `backend/src/main/java/com/englishlearning/features/user/service/AdminUserService.java`
- `backend/src/test/java/com/englishlearning/EnglishLearningApplicationTests.java`
- `backend/src/test/java/com/englishlearning/common/api/ApiBoundaryTests.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/AdminRegistrationHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/AuthenticationHttpTestSupport.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/CategoryHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/SecurityBoundaryTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryLiveHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/user/AdminUserServiceTests.java`
- `ci/task009/backend_tests.py`
- `docs/ADMIN_CRUD_REVIEW.md`
- `docs/FEATURE_STATUS.md`
- `frontend/src/features/admin/admin-categories.tsx`
- `frontend/src/features/admin/admin-dashboard.tsx`
- `frontend/src/features/admin/admin-users.module.css`
- `frontend/src/features/admin/admin-users.tsx`
- `frontend/src/features/admin/create-user.tsx`
- `frontend/tests/e2e/admin-dashboard.spec.ts`
