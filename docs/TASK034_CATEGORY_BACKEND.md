# TASK-034 Category backend — Phase 1

Scope: Admin-only GET/POST/PATCH `/api/v1/admin/categories`; no DELETE, public catalog,
Teacher selection, or Course creation. TASK-034 remains IN_PROGRESS.

## Implementation

- `features/category/controller/CategoryController` and `CategoryExceptionHandler`.
- `features/category/service/CategoryService` and `CategoryFailure`.
- `features/category/repository/CategoryRepository`: parameterized native SQL through
  EntityManager, following existing repository conventions; no extra JPA entity required.
- `features/category/dto/CategoryDto`: safe decimal-string ApiId response.
- Requests use a strict allowlisted map to distinguish absent fields from explicit null.
- Pagination is `{items,page,size,hasNext}`, ordered by ID, default 20, maximum 100.
- Current verified/unlocked ADMIN is checked in security and service. Mutations lock
  the acting account; PATCH locks the category before merging omitted fields.
- ASCII-space trimming, 1–100 Unicode code points, no controls or malformed UTF-16.
  Inactive categories reserve their names. No accent stripping is performed.
- Named PostgreSQL uniqueness violations map to 409 CATEGORY_NAME_CONFLICT;
  unrelated database errors map to safe 503, never SQL or rejected values.

## Migration and deployment gate

V6 adds only `uq_course_categories_normalized_name` on
`public.course_categories(lower(btrim(name)))`. V1–V5 are unchanged.
Existing conflicting names cause migration failure; no rows are rewritten or deleted.
The normal unique-index build blocks writes while building. Schedule a maintenance window.
Database collation/locale governs `lower`; verify intended accent-sensitive behavior on
an approved disposable clone matching the target locale before deployment.

Do not deploy the Category write endpoints before V6 is successfully installed: without
its index, concurrent normalized-name duplicates are not prevented.

Deployment requires separate operator approval:
1. Verify target fingerprint, backup/restore procedure and successful V1–V5 checksums.
2. With read-only bounded transactions, count duplicate groups using
   `SELECT count(*) FROM (SELECT lower(btrim(name)) FROM public.course_categories GROUP BY 1 HAVING count(*) > 1) conflicts`.
3. If any conflicts exist, stop. Obtain an explicit data-resolution decision; do not
   auto-rename, delete, merge, baseline, repair or clean.
4. Test V6 on a disposable clone, including locale behavior and backup restoration.
5. In an approved maintenance window apply only the reviewed pending migration with
   a separately controlled Flyway runner and private environment credentials.
6. Validate history/checksums and index validity, then deploy the backend and perform
   approved Admin API smoke tests. Keep Hibernate schema mutation and SQL init disabled.
7. On application rollback preserve V6 and data; do not automatically drop the index.

## Verification evidence

Previous session logs retained under `backend/target`:
- `category-focused.log`: 74 passed, 0 failures, 0 errors, 0 skipped.
- `category-persistence.log`: 8 passed, 0 failures, 0 errors, 0 skipped.
  Six persistence tests cover pagination, filtering, rollback, concurrent creates,
  referenced-category retention and serialized partial updates. Two migration tests
  cover fresh V1–V6 and conflicting V5 upgrade rejection; persistence setup also checks
  a populated conflict-free V5 upgrade.
- The interrupted full verify had 17 ApiBoundaryTests context errors: its datasource-free
  fixture did not mock the new CategoryRepository. The resume adds that mock only;
  assertions and production behavior remain unchanged.

No Supabase connection or live migration was performed for this implementation.
Frontend integration and Teacher category selection remain separate work.

Resume verification — 2026-10-22:
- Offline Maven verify with one fork, bounded heaps and explicit Mockito agent: BUILD SUCCESS.
- 368 discovered: 279 passed, 0 failures, 0 errors, 89 skipped (opt-in integration suites).
- Includes CategoryHttpTests 5/5 and CategoryServiceTests 4/4; executable JAR repackaged.
- Log: `backend/target/category-resume-verify.log`.
- PostgreSQL tests were not rerun: production Category code and V6 were unchanged;
  the earlier 8 passing disposable tests remain historical evidence, not new execution.
- `git diff --check` passed. Codex review guided by Ponytail Review found no remaining
  blocking defect in this bounded backend slice; live deployment is not verified.

Admin Category UI — 2026-10-22:
- Existing Admin Dashboard Category section now supports explicit list loading,
  All/Active/Inactive filtering, previous/next pagination, creation, renaming and
  activation/deactivation. No DELETE or Teacher selection is added.
- Reuses authorized() with Bearer-only transport and string IDs; its body type now
  accepts boolean values. Duplicate submissions are blocked by the dashboard guard.
  Uncertain mutations block further writes until an explicit list reload succeeds.
- New files: frontend/src/features/admin/admin-categories.tsx and its CSS module;
  frontend/tests/e2e/admin-categories.spec.ts. Dashboard and existing dashboard tests updated.
- TypeScript, ESLint and production build passed. API helper unit tests: 13 passed,
  0 failed, 0 skipped. Final mocked desktop/mobile browser run: 24 passed, 0 failed,
  0 skipped. Initial run had two filter-locator failures; corrected locator and final
  complete run passed. Screenshots reviewed at 1440x900 and 390x844; no overflow.
- No disposable backend was started and no live Category API integration is claimed.
  Supabase/V6 deployment and Teacher selection remain outstanding. TASK-034 IN_PROGRESS.

Teacher Category read-only slice — 2026-10-24:
- Added TeacherCategoryController and TeacherCategoryService for GET
  /api/v1/teacher/categories. Reuses CategoryRepository, CategoryDto and Page;
  active=true is always bound before SQL pagination. No client filter override.
- Existing TeacherBusinessEligibility checks current role, verified/unlocked account
  and approved onboarding. Security admits only the exact Teacher GET route.
- CategoryExceptionHandler also handles this controller; policy denials become safe
  403 ACCESS_DENIED. Admin code paths and repository SQL are unchanged.
- Seven HTTP tests cover default/explicit pagination, ApiId strings, query/body
  rejection, role/account/cookie boundaries, onboarding denial, forbidden mutations
  and sanitized database failures. Two service/repository tests cover actual policy
  composition and active filtering before pagination.
- Offline memory-bounded Maven verify: BUILD SUCCESS; 377 discovered, 288 passed,
  0 failures, 0 errors, 89 skipped. Executable JAR packaged successfully.
  Log: backend/target/teacher-category-verify.log. No database suites enabled.
- Future Course create/update must validate current Teacher eligibility, ownership
  and Category eligibility transactionally at write time, including deactivation races.
  Deactivated Categories remain referenced; no Course data is changed by this slice.
- No Supabase connection, migrations, frontend changes or Teacher Course selection UI.
  TASK-034 remains IN_PROGRESS: V6 deployment, live integration and Course write-time
  selection remain outstanding. PostgreSQL execution for this new endpoint is unverified.

Disposable PostgreSQL + real HTTP verification — 2026-10-24:
- Added opt-in CategoryLiveHttpTests. Exact guards require user task034_test and
  jdbc:postgresql://127.0.0.1:55436/task034_live, with an empty application schema.
- Created a fresh PostgreSQL 18 cluster (UTF8, ICU und, SCRAM, loopback only,
  shared_buffers=16MB, max_connections=12). No existing cluster/database was changed.
  Verified system identifier: 7700082364190047064.
- Flyway applied exactly V1–V6 and validate passed. Spring Boot ran on random
  loopback HTTP port 56534, with Flyway disabled after explicit migration, ddl-auto=none,
  SQL init disabled, ephemeral RSA keys/HMAC and SMTP restricted to 127.0.0.1:1.
- Real Java HTTP client requests: 3 tests passed, 0 failed, 0 errors, 0 skipped.
  Covers Admin list/create/rename/activate/deactivate; Teacher active-only SQL filtering
  and pagination; anonymous/wrong-role/locked/unverified/unapproved denial; forbidden
  mutations; invalid parameters; safe 404/409 responses; inactive-name reservation;
  simultaneous normalized duplicate creates (one 201, one 409); failed rename rollback;
  unchanged fixture user count.
- Concurrent HTTP creates use the same Admin and therefore also exercise the account
  serialization lock. Cross-Admin races and referenced Course preservation retain
  previous disposable persistence evidence; they were not rerun in this HTTP suite.
- Tokens are issued by the real AccessTokenIssuer for isolated fixture users. JWT
  validation and database-backed authorization run normally. This is not a login or
  frontend browser journey. No mail is sent.
- Initial attempt: 0 assertions completed, 1 setup error, caused by Windows Java
  UnixDomainSockets loopback initialization. Retry passed with the existing process-local
  workaround -Djdk.net.unixdomain.tmpdir=E:/CodexHome/tmp. No OS/network settings changed.
- Logs: backend/target/category-live-http.log and Surefire CategoryLiveHttpTests report.
  Successful disposable data directory retained, stopped:
  C:/Users/NGUYEN~1/AppData/Local/Temp/task034-live-90aebb9786ab45b1a79b9de1c52b8040.
  Spring Boot closed; ephemeral JWT files removed; no reusable passwords are recorded.
- Browser-to-live-backend check deferred due to limited physical memory (~838 MiB after
  cleanup). Previous 24 mocked browser tests remain separate evidence.
- TASK-034 IN_PROGRESS. Supabase V6 deployment, frontend/live acceptance and eventual
  Course write-time Category validation remain outstanding.

Reproduction (isolated infrastructure only):
1. Select a fresh disposable cluster directory; ensure loopback port 55436 is unused.
   Generate a private random SCRAM password and initialize PostgreSQL 18 as task034_test;
   bind only 127.0.0.1 with the bounded settings above. Do not reuse populated targets.
2. Create only task034_live owned by task034_test. Verify cluster identity.
3. Set CATEGORY_LIVE_URL to the exact guarded JDBC URL above; set CATEGORY_TEST_USER
   and CATEGORY_TEST_PASSWORD privately. Override DATABASE_URL/USERNAME/PASSWORD with
   the same isolated values; never inherit a live datasource.
4. From backend run:
   ./mvnw.cmd -B -o test -Dtest=CategoryLiveHttpTests -DcategoryLive=true -DforkCount=1
   "-DargLine=-Xmx384m -XX:MaxMetaspaceSize=256m -Djdk.net.unixdomain.tmpdir=E:/CodexHome/tmp"
   Use MAVEN_OPTS="-Xmx192m -XX:MaxMetaspaceSize=128m". The short socket directory is a
   Windows environment workaround, not a database/application setting.
5. Always stop only the newly created cluster with pg_ctl -D <verified-new-directory>
   -m fast -w stop. Preserve all other instances and databases.
