# First real Google Calendar consent on Windows TEST

Use Windows x64, PowerShell 7.2+, Node 24 x64, Java 17 or newer and psql. Start in the calendar-google-integration worktree. The reviewed application baseline is 31e0942003a783c6fc507d209ef77a4e3445381f. The package needs acceptance on the commit containing these scripts.

## Obtain the matching application

After approval, commit and push the setup files. Run TASK-009 on that commit with the optional package input blank. After success, dispatch Google OAuth TEST Windows package on the same revision with the successful TASK-009 run ID. If the new workflow is not selectable on the default branch, dispatch the already registered TASK-009 workflow on the feature branch with that prior run ID in package_after_run. The latter also runs normal acceptance. Check both run URLs, repository, commit, and successful conclusions on GitHub.

Download google-oauth-test-windows-<commit> from the successful package run. Use the GitHub download from that run as the source of the receipt. A locally authored receipt cannot prove its own origin. CI builds a Spring Boot JAR and a Windows Next.js production build with the localhost API origin. The package includes .next, installed production dependencies and public assets if present. The launcher invokes next start.

## Local secrets and TLS

Run the following in PowerShell 7. The secret generator refuses to overwrite an existing directory. For previously configured secrets, retain the existing key and skip generation. Enter only TEST credentials at the masked prompts. DPAPI binds them to this Windows user and machine. The script also generates a 32-byte Base64 AES key, a separate RSA JWT pair and reset HMAC under a private NTFS ACL. Protect a recoverable backup of the AES key.

```powershell
$private = 'E:\SupabaseSecrets\google-calendar-test'
$manifest = Join-Path $private 'manifest.json'
./tools/google-test/Configure-Secrets.ps1
Copy-Item ./tools/google-test/manifest.example.json $manifest -Confirm
notepad $manifest
```

Set approvedCommit and approvedPackageRunId from the successful package run. Use the TEST dashboard Connect panel to independently verify the project zobxcixrxsazmngcthvg, session pooler hostname, port 5432, project-qualified user and database postgres against the separately saved TEST metadata. Shared pooler hostname alone does not identify a project. Obtain the trusted CA bundle for this endpoint through the selected TEST project's Database SSL settings or Supabase's authenticated support if the pooler uses another chain. Save it at databaseCaFile. Never use an unverified server leaf as a trust anchor. See [Supabase connection instructions](https://supabase.com/docs/guides/database/connecting-to-postgres) and [pgJDBC SSL configuration](https://jdbc.postgresql.org/documentation/ssl/).

psql and the default pgJDBC SSL factory use an explicit CA with verify-full. The optional Java DefaultJavaSSLFactory uses JVM trust anchors instead. This launcher uses the explicit bundle for consistent checks in both clients. A TLS-only probe of the configured pooler did not establish a verified handshake with this JDK. That result does not establish that the trust store lacks the issuer. Resolve endpoint/CA verification before using credentials.

## Install and verify the artifact

Extract the GitHub artifact's outer archive to a fresh download directory. Set $download to that directory. Keep receipts and runtime in the private directory, separate from source. Use a fresh extraction destination.

```powershell
$download = Read-Host 'Directory containing receipt.json, SHA256SUMS and runtime.zip'
$archive = Join-Path $download 'runtime.zip'
$expected = (Get-Content (Join-Path $download 'SHA256SUMS') -Raw).Trim()
if ($expected -notmatch '^([a-f0-9]{64})  runtime\.zip$') { throw 'Invalid checksum record' }
if ((Get-FileHash $archive -Algorithm SHA256).Hash -ine $Matches[1]) { throw 'Checksum mismatch' }
Copy-Item (Join-Path $download 'receipt.json') $private -Confirm
Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $private 'runtime')
./tools/google-test/Start-TestEnvironment.ps1 -Manifest $manifest -DryRun
./tools/google-test/Start-TestFrontend.ps1 -Manifest $manifest
./tools/google-test/Start-TestEnvironment.ps1 -Manifest $manifest -VerifyDatabase
```

The confirmed read-only preflight reports aggregate account availability and checks successful migration filenames, ordering and Flyway checksums from the same source revision. V12 is required. V13 is optional with sync disabled. Stop if either the TEST identity or certificate cannot be verified. Missing eligible accounts require separate TEST provisioning. Prepare-Demo is a plan only and has no part in first consent.

After explicit startup approval, with both ports available, run the backend launcher. Verify the CSRF endpoint's HTTP status without displaying its body. Start the frontend only after the backend is responding.

```powershell
./tools/google-test/Start-TestEnvironment.ps1 -Manifest $manifest -StartBackend
(Invoke-WebRequest http://localhost:8081/api/v1/auth/csrf).StatusCode
./tools/google-test/Start-TestFrontend.ps1 -Manifest $manifest -StartFrontend
```

The backend process enables OAuth, disables sync and disables migrations. The frontend origin is http://localhost:3000. Application login uses the isolated JWT keys. Existing eligible accounts can use password login. SMTP-dependent registration and recovery need separate TEST mail configuration.

### Browser consent steps

1. Open http://localhost:3000/login in a normal browser and log in as an existing TEST Student. Verify the intended application account before connecting.
2. Open the account page and expand Kết nối Google Calendar. The disconnected panel should be available; synchronization must say it is not active.
3. Click Kết nối Google Calendar. **This is the point where the user personally selects their allowlisted Google test account and grants consent on Google's real website.** Do not automate consent or share Google passwords.
4. Google returns through http://localhost:8081/api/v1/integrations/google-calendar/callback. The application should return to /account with its allowlisted result query, restore only the initiating application account and show the connected Google email.
5. Reload connection status to verify persistence. No secondary calendar or events should be created with sync disabled.
6. Log out and repeat in a separate browser profile for the approved TEST Teacher, using their own Google test account. Do not reuse a Google subject already connected to another local user.
7. Consent denial should display an error and not install a connection. Invalid/replayed return state must not restore another account. Do not open the callback manually as a substitute for the flow.


## Demo bootstrap design requiring completion

Use deterministic synthetic emails under example.invalid and an external private run journal. Hold an exclusive journal lock; record IDs after each successful API result. On uncertain POST outcomes, reconcile by exact identity and backend state before retrying. Never retry course creation blindly. Do not infer identity from the display name alone.

Provision a TEST Admin through a separately approved isolated fixture authority; register Teacher/Student via existing registration APIs, capture verification using an isolated SMTP sink where required, and approve Teacher through Admin review. No manual user creation should be required once that fixture authority is implemented. Current TASK-009 fixture is explicitly restricted to disposable localhost PostgreSQL and must not be repointed to Supabase TEST.

Provide a synthetic receiving-account submission through the existing submission/review workflow with TEST-only evidence, using TEST Storage only. No real receiving details. Resolve a genuine Teacher-owned Meet URL through secure local input: publication requires it, and generating a syntactically valid fictional URL is forbidden. These prerequisites cannot be omitted to obtain a published course.

Then create/recover category and owned course Tiếng Anh giao tiếp thực tế, tuition amount 1000000 VND, Asia/Ho_Chi_Minh, three sessions, start at least seven days ahead. Set weekly schedule, generate exactly three future sessions and publish using existing APIs. Student POST /student/courses/{id}/enrollment; POST /student/enrollments/{id}/payments. Recover existing pending identifiers rather than inserting new ones. Owning Teacher POST /teacher/courses/{courseId}/payments/{paymentId}/confirm-manually with snapshot-derived receiver, amount, reference and a stable synthetic transaction identifier. Keep attestation explicitly TEST-only; never execute it against real money or production records. Assert CONFIRMED/ACTIVE through owner APIs. No direct SQL activation.

## Controlled acceptance procedure (not executed)

1. Read Teacher and Student calendar endpoints over a bounded 62-day range, follow pagination, and verify the same three session IDs. Before payment confirmation verify Student exclusion and denied Join access.
2. Manually consent in separate Student/Teacher browser sessions; scripts must never automate consent or collect Google passwords.
3. Only after a separate enablement approval set sync true in the TEST launcher configuration. The prepared launcher intentionally hardcodes false.
4. Verify separate app-managed calendars, exactly one event per session, safe application links, no raw Meet URLs, no attendee/payment secrets. Google UI or an approved server-side verifier must inspect actual event IDs; frontend status alone is insufficient.
5. Save event IDs, reschedule one session using expected timestamps, verify same event IDs/new times. Cancel another and verify removal. Repeat status reads to check no duplicates.
6. Verify unrelated/unauthorized accounts cannot Join; cancelled sessions cannot Join. Revoke enrollment access and verify removal while credentials remain usable.
7. Exercise disconnect/reconnect and document residual remote calendar limitations. Do not reset UNCERTAIN provisioning blindly.
