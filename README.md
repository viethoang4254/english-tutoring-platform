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
