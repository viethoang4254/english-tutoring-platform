# Stage C3 Google Calendar account connection

Students and approved Teachers can authorize a personal Google account from their calendar or account screen. The application keeps its existing password and JWT login. Connection status names the verified Google email and shows whether reconnection is required. Session synchronization remains future C4 work.

## Manual configuration

1. In a separate Google Cloud development project, enable the Google Calendar API. Configure the Google Auth Platform consent screen, audience and test users. Request only `openid`, `email` and `https://www.googleapis.com/auth/calendar.app.created`.
2. Create a **Web application** OAuth client. Register the backend callback exactly, including scheme, port and path. Local development uses `http://localhost:8081/api/v1/integrations/google-calendar/callback`. Deployment requires HTTPS. Set the intended frontend as the first configured browser allowed origin. Use the same site for frontend and backend cookies.
3. Provide the following values to the backend process through its secure launcher. Never put secret values in Next.js variables, Git, console output or a shared example file.

| Backend variable | Value |
|---|---|
| APP_GOOGLE_CALENDAR_ENABLED | true to enable after configuration |
| APP_GOOGLE_CALENDAR_CLIENT_ID | Web application client ID |
| APP_GOOGLE_CALENDAR_CLIENT_SECRET | Secret from that client |
| APP_GOOGLE_CALENDAR_REDIRECT_URI | Exact registered backend callback |
| APP_GOOGLE_CALENDAR_SCOPES | The three space-separated scopes above |
| APP_GOOGLE_CALENDAR_TOKEN_KEY_ID | Identifier for the currently loaded encryption key |
| APP_GOOGLE_CALENDAR_TOKEN_KEY_FILE | Absolute path outside the repository to a restricted file containing a Base64-encoded independent random 32-byte key |

Generate the key with a cryptographically secure random generator directly into an owner-restricted secret file. Do not reuse JWT or password-reset material. Keep the key backed up securely. The MVP loads one key version. Changing it without re-encrypting stored credentials requires reconnection. Suppress query strings on the callback route in proxy, access, trace and analytics logs. Disable HTTP body and SQL parameter logging for credential flows.

See [Google web-server OAuth setup](https://developers.google.com/identity/protocols/oauth2/web-server), [OpenID Connect reference](https://developers.google.com/identity/openid-connect/reference) and [Calendar scopes](https://developers.google.com/workspace/calendar/api/auth).

## Connection lifecycle

GET status requires the application's JWT. POST connect and DELETE disconnect also require the dedicated CSRF cookie and masked header. Google consent uses offline authorization with PKCE S256. A ten-minute intent stores hashes of state, browser binding and nonce, the local owner and an encrypted verifier. The callback consumes the intent once before the token exchange. It verifies the signed ID token, issuer, audience, expiry, nonce, subject, verified email and actual scopes. Google credentials never establish application identity.

The backend encrypts refresh tokens with AES-256-GCM using owner and subject as associated data. Duplicate active subjects are rejected. A successful reconnection without a new refresh token can retain the previous credential only for the same subject. Disconnect clears local credentials first. If remote revocation fails, remove the remaining grant through the Google account permissions screen. Revocation is detected on credential use. Status reads use the stored state and do not poll Google.

V12 is additive and has not been deployed. Existing event mappings and Meet URLs are outside C3. The UI returns to a fixed account page. A tab-local marker records the initiating local account ID and timestamp. It expires after ten minutes and is consumed before refresh. Only a matching authenticated /users/me identity reaches the session provider. Invalid returns stay signed out without changing shared Remember Me state.


A disconnect commits REVOCATION_PENDING, erases local credentials and retains the unique verified subject. Only the original transition may send a revocation request, outside the transaction. Only a positive acknowledgement releases the subject. A PostgreSQL sequence and subject release record require fresh consent after release. A timeout, process crash or uncertain acknowledgement leaves the claim blocked across restarts. There is no automatic retry, lease expiry or self-service override. To change Google subjects, disconnect successfully before starting new consent.

Finalization reads the current user row under FOR UPDATE. For Teachers it also locks an APPROVED application row FOR SHARE. These locks last through the credential write. Token exchange runs before this transaction. Disposable tests pause exchange or the final save to cover both account-lock orderings and a concurrent Teacher approval change.

## Verification

TASK-009 includes synthetic provider, HTTP security, crypto, service and disposable PostgreSQL suites plus 32 desktop/mobile browser executions. Six synthetic PNGs are expected in `google-calendar-review-screenshots`. Existing artifacts remain registered. Runtime evidence and manual real-provider consent validation are pending.

## Changed files

- `.github/workflows/task009-acceptance.yml`
- `backend/src/main/java/com/englishlearning/features/auth/security/BrowserSecurityFilters.java`
- `backend/src/main/java/com/englishlearning/features/auth/security/GoogleCalendarSecurity.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleController.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleCrypto.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleFailure.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleProvider.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleService.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleSettings.java`
- `backend/src/main/java/com/englishlearning/features/googlecalendar/GoogleStore.java`
- `backend/src/main/resources/db/migration/V12__google_calendar_connections.sql`
- `backend/src/test/java/com/englishlearning/features/auth/security/GoogleCalendarHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryLiveHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryMigrationTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/course/CoursePersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/enrollment/EnrollmentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GoogleCryptoTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GooglePersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GoogleProviderTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GoogleServiceTests.java`
- `backend/src/test/java/com/englishlearning/features/googlecalendar/GoogleTestSettings.java`
- `backend/src/test/java/com/englishlearning/features/payment/PaymentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/ReconciliationPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/paymentaccount/ReceivingAccountPersistenceTests.java`
- `ci/task009/backend_tests.py`
- `ci/task009/run.py`
- `docs/API_DESIGN.md`
- `docs/ARCHITECTURE.md`
- `docs/BUSINESS_RULES.md`
- `docs/DATABASE_DESIGN.md`
- `docs/FEATURE_STATUS.md`
- `docs/STAGE_C3_GOOGLE_OAUTH.md`
- `docs/TASK_BREAKDOWN.md`
- `frontend/playwright.task009.config.ts`
- `frontend/src/features/auth/account-workspace.tsx`
- `frontend/src/features/auth/session-provider.tsx`
- `frontend/src/features/google-calendar/google-connection.tsx`
- `frontend/src/features/google-calendar/google-return.ts`
- `frontend/src/features/student/student-calendar.tsx`
- `frontend/src/features/teacher/teacher-calendar.tsx`
- `frontend/tests/e2e/google-calendar.spec.ts`
- `frontend/tests/support/google-calendar.ts`
- `frontend/tests/support/task009-reporter.ts`
