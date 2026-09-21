# English Learning Platform

## Backend foundation

TASK-001 provides one Spring Boot modular-monolith skeleton. Implementation status
and evidence are maintained only in docs/FEATURE_STATUS.md.

- Java language/bytecode target: 17; local JDK verified: 17.0.12.
- Spring Boot: 4.1.1.
- Maven: 3.9.16, provisioned by Maven Wrapper 3.3.4 (only-script).
  The Windows script has a one-line compatibility fix: check the Maven cache
  directory Target before indexing it, since ordinary directories can return null.
- Coordinates: com.englishlearning:english-learning-backend:0.0.1-SNAPSHOT.

A JDK is required; global Maven is not. The first wrapper run downloads Maven and
build dependencies from Maven Central, requiring network access and a writable
user Maven cache. Set JAVA_HOME to the approved JDK 17 for the current shell;
an existing JAVA_HOME may select a different JDK than java on PATH. Verification
used `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'` without changing machine settings.

From backend/ on Windows:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean verify
java -jar target/english-learning-backend-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=18080
```

On Unix, use `sh ./mvnw --version` and `sh ./mvnw clean verify`.
Stop the running application with Ctrl+C. The verification build compiles, runs
the standard context-load test without a listening server, and packages the
executable JAR. No application API endpoint is implemented by the skeleton.

## Structure and boundaries

- backend/pom.xml: pinned build and minimal web/test dependencies.
- backend/.mvn/wrapper/, mvnw and mvnw.cmd: reproducible build-tool bootstrap.
- backend/src/main/java/com/englishlearning/: application root; add feature-owned
  packages only when their implementation tasks begin, not empty CRUD layers.
- backend/src/main/resources/application.yml: application identity only.
- backend/src/test/java/com/englishlearning/: context-load test.
- docs/: approved design, task specifications and implementation-status evidence.

The future Next.js frontend is a separate application. Core data access remains
Next.js -> Spring Boot REST API -> JPA/Hibernate -> Supabase-hosted PostgreSQL.
Future application routes use /api/v1; no route is created here. Database access,
Flyway migrations, security and application features belong to later tasks.

## Configuration and local files

Use Spring Boot's standard environment-variable/command-line configuration.
For example, SERVER_PORT overrides the server port; no custom dotenv loader is
installed. The startup command above binds locally for verification.

Never commit real secrets or local environment files. Root .gitignore protects
.env/.env.* files, private-key/keystore files, backend build output and local IDE
artifacts; placeholder-only .env.example files remain eligible for tracking.
No environment example or profile is needed by TASK-001. Database, JWT, provider
and frontend configuration are deferred to their owning tasks. Backend secrets
must never be exposed through frontend NEXT_PUBLIC_* variables.

## Frontend technical foundation

The frontend/ application uses Node 24.21.0 and npm 11.19.0. Its exact Node
version is recorded in frontend/.node-version; direct packages and package-lock.json
pin the approved dependency baseline. Select that Node installation in the shell
before running commands. No global npm upgrade or machine PATH change is required.

From frontend/:

```powershell
node --version
npm --version
npm ci
npm run typecheck
npm run lint
npm run build
npx playwright install chromium
npm run test:e2e
npm run dev -- --hostname 127.0.0.1 --port 3000
```

Stop the development server with Ctrl+C. To run a previously built production
application manually, use `npm run start -- --hostname 127.0.0.1 --port 3000`.
The E2E command starts its own production server on port 3000; stop a manually
started server first. It refuses to reuse an unrelated server. Initial dependency
resolution uses npm install; subsequent reproducible installs use npm ci.

The App Router layout/page are Server Components; providers.tsx is the React Query
Client Component boundary with a stable QueryClient and unchanged library defaults.
Zustand is installed only: no store or application query/API call is implemented.
Future feature implementations belong under src/features/ when their tasks begin.
No placeholder feature directories or generic API client are created here.

The sole page is neutral technical startup content, not approved product UI/UX.
Global CSS provides basic sizing and wrapping only. The Playwright smoke test uses
Chromium at 390x844 and 1440x900 to verify rendering, no horizontal overflow and no
uncaught browser page errors. It does not establish product flows or replace
backend tests; cross-feature E2E remains TASK-028. Chromium installation downloads
browser artifacts to Playwright's user cache outside this repository.

No frontend environment variables or .env files are required. Backend/API-origin
integration belongs to later tasks. No authentication, database/Supabase client,
product navigation, deployment or CI configuration is included.

Track package-lock.json. node_modules/, .next/, next-env.d.ts, TypeScript build
cache and Playwright output are generated and ignored. next typegen (included in
typecheck) regenerates Next.js types before the TypeScript check.
Next.js may also generate frontend/AGENTS.md and frontend/CLAUDE.md in an AI-agent
session; these generated guidance files are ignored, leaving root AGENTS.md unchanged.
