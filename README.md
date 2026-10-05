# Development of an English Tutoring Platform

**Xây Dựng Nền Tảng Gia Sư Dạy Tiếng Anh**

## Overview

A responsive, multi-Teacher English tutoring web platform for STUDENT, TEACHER
and ADMIN. Teachers own Courses; Students purchase whole Courses and participate
in Sessions and Assignments through authorized Enrollment.

This README describes onboarding and approved direction. It does not override
[AGENTS.md](AGENTS.md) or the authoritative documents below.

## Current Development Status

Completed technical foundations:

- **TASK-001:** Spring Boot build, context test, packaging and recorded packaged-JAR startup.
- **TASK-002:** Next.js technical shell, reproducible dependencies and technical browser checks.
- **TASK-003:** Environment-bound datasource setup and recorded local/Supabase development PostgreSQL connectivity.

**Tutoring business capabilities are not implemented:** authentication/business
authorization, profiles, discovery, Course management, Enrollment, Sessions/
scheduling, Assignments, Submissions/results, CourseProgress, receiving information,
payments, Meet, Calendar, ParticipantFeedback and Admin features/statistics.
Production deployment and CI are not implemented.

[FEATURE_STATUS.md](docs/FEATURE_STATUS.md) owns live status, evidence and its
limitations. The neutral frontend is not product UI/UX completion. Connectivity
is not application-schema completion.

## Planned Product Scope

These are approved capabilities, not implemented features. Detailed policies and
interactions remain gated.

| Role | Planned direction |
|---|---|
| STUDENT | Account/profile; Teacher/Course discovery; whole-Course registration/purchase; enrolled participation; Sessions; Assignments and Submissions/results; progress; protected Meet access; Calendar support and ParticipantFeedback. |
| TEACHER | Teaching/public profile; owned Courses, Sessions and Assignments; enrolled-Student teaching information; submissions/results/progress; private receiving information and related transactions; manual Course Meet URL. |
| ADMIN | Account/Course oversight; categories/topics; Enrollment and transaction monitoring; operational statistics within approved permissions. |

## Architecture

Approved architecture, with business APIs still unimplemented:

```text
Browser -> Next.js -> Spring Boot REST API -> JPA/Hibernate -> PostgreSQL
```

Spring Boot is a feature-based modular monolith. It owns authentication,
authorization, Teacher ownership, Student participation, business rules, payment
trust and persistence access. Future business contracts follow /api/v1; no
business endpoint exists yet.

Development supports local PostgreSQL and Supabase-hosted PostgreSQL. The frontend
must never access PostgreSQL directly. Supabase supplies development database hosting
and the approved bounded file Storage direction (not yet implemented). No Supabase Auth,
Data API, RLS-based application
authorization, Edge Functions, Realtime or frontend supabase-js is introduced. Production
hosting is unresolved.

## Technology Stack

| Area | Repository baseline |
|---|---|
| Backend | Java target 17; Spring Boot 4.1.1; Spring Web MVC; Spring Data JPA and PostgreSQL driver, with Boot-managed Hibernate/HikariCP. |
| Build | Maven 3.9.16 via Maven Wrapper 3.3.4, only-script distribution; approved Windows null-Target compatibility guard. |
| Frontend | Node baseline 24.21.0; npm 11.19.0; Next.js 16.3.5; React/React DOM 19.3.0; TypeScript 5.9.3. |
| Frontend foundation | React Query 5.103.1 provider; Zustand 5.0.15 dependency only, without stores or business API hooks. |
| Checks | Spring Boot/JUnit context and opt-in connectivity tests; ESLint 9.39.5 / eslint-config-next 16.3.5; @playwright/test 1.63.0 browser-test development tooling. |
| Planned security | Spring Security + JWT; not currently installed/implemented. |
| Planned migrations | Flyway; currently absent. Tutoring application schema is not claimed complete. |

Exact frontend dependencies are in [package.json](frontend/package.json) and its
lockfile. Backend coordinates remain
com.englishlearning:english-learning-backend:0.0.1-SNAPSHOT. Preserve technical
identifiers rather than renaming them merely to match the product title.

## Repository Structure

```text
english-learning-platform/
├── backend/
│   ├── .mvn/wrapper/
│   ├── .env.example
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/ and src/test/
├── frontend/
│   ├── .env.example
│   ├── package.json / package-lock.json
│   ├── playwright.config.ts
│   ├── src/app/
│   └── tests/e2e/
├── docs/
├── .agents/skills/
├── AGENTS.md
└── README.md
```

Business-feature directories will be added only within approved implementation
tasks; they are not scaffolded by the current foundation.

## Prerequisites

- Approved JDK 17. Recorded verification used 17.0.12; JAVA_HOME examples below
  use a machine-specific installation path.
- Node 24.21.0 and npm 11.19.0 selected in the frontend shell; see
  [frontend/.node-version](frontend/.node-version).
- Reachable local PostgreSQL or Supabase development PostgreSQL for application
  startup or intentional live checks, not normal database-independent builds.
- Network/cache access for initial Maven/npm dependencies and Chromium downloads.

Use the Maven Wrapper; global Maven is not required. Select tools in the relevant
shell without requiring machine-wide PATH changes.

## Environment Configuration

[backend/.env.example](backend/.env.example) documents DATABASE_URL,
DATABASE_USERNAME and DATABASE_PASSWORD. Spring Boot does **not** automatically
load .env.example or a copied .env file. Supply values to the process environment;
no dotenv loader is installed.

Use a credential-free JDBC URL and private credential input in a dedicated backend
PowerShell session. Never put passwords in URLs, command-line arguments, committed
files or chat. Do not launch frontend processes from that credential-bearing shell.

[frontend/.env.example](frontend/.env.example) documents NEXT_PUBLIC_API_ORIGIN:
a public Spring Boot origin without /api/v1, trailing slash or credentials.
No API client consumes it yet; the neutral frontend requires no environment
variables. Never place database credentials in frontend variables.

## Backend Development

### Build and database-independent checks

From backend/, select your installed JDK 17:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd --version
.\mvnw.cmd clean verify
```

Normal clean verify builds, runs the context test and packages the executable JAR
without live database credentials. The connectivity test is skipped unless
explicitly enabled. Datasource auto-configuration is excluded only by the context
test; this is not a production profile.

Setting JAVA_HOME does not change a bare java command resolved through PATH.
The startup example therefore uses its explicit executable. On Unix, use
sh ./mvnw --version and sh ./mvnw clean verify with an approved JDK.

### Start the packaged application

Application startup requires valid datasource variables and a reachable database.
From backend/, in a dedicated PowerShell session, configure the database, package
the application and then run that JAR. Choose the URL from the database notes below.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$dbSecret = $null
try {
    $env:DATABASE_URL = Read-Host 'Credential-free JDBC URL'
    $env:DATABASE_USERNAME = Read-Host 'Database username'
    $dbSecret = Read-Host 'Database password' -AsSecureString
    $env:DATABASE_PASSWORD = ([System.Net.NetworkCredential]::new('', $dbSecret)).Password

    .\mvnw.cmd clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Build verification failed' }

    # Optional: insert the opt-in connectivity check below here before startup.
    & "$env:JAVA_HOME\bin\java.exe" -jar target/english-learning-backend-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=8080
}
finally {
    Remove-Item Env:DATABASE_URL, Env:DATABASE_USERNAME, Env:DATABASE_PASSWORD -ErrorAction SilentlyContinue
    if ($null -ne $dbSecret) { $dbSecret.Dispose() }
    Remove-Variable dbSecret -ErrorAction SilentlyContinue
}
```

Stop with Ctrl+C; finally clears the session variables. Backend default port is
8080 unless overridden; this example explicitly uses 127.0.0.1:8080.
127.0.0.1:18080 was a historical verification override, not a permanent application
port. Maven test alone does not package a JAR.

## Frontend Development

The technical frontend exists; tutoring screens/navigation do not. From frontend/
in a separate shell:

```powershell
node --version
npm --version
npm ci
npm run typecheck
npm run lint
npm run build
npm run dev -- --hostname 127.0.0.1 --port 3000
```

Prefer npm ci with the committed lockfile for reproducible setup. Stop the dev
server with Ctrl+C. After a production build, this runs that build instead:

```powershell
npm run start -- --hostname 127.0.0.1 --port 3000
```

The App Router page/layout are Server Components; providers.tsx supplies the
React Query client boundary. No Zustand store, generic API client, authentication
flow or frontend database integration exists.

## PostgreSQL / Supabase Development

Select one datasource per backend process:

| Development target | Credential-free JDBC URL shape |
|---|---|
| Local PostgreSQL | `jdbc:postgresql://localhost:5432/english_learning` |
| Supabase Session Pooler | `jdbc:postgresql://<SESSION_POOLER_HOST>:5432/postgres?sslmode=require` |

Use the dashboard's Session Pooler host and username, port 5432 and database
postgres for Supabase development. Recorded direct IPv6 connectivity was unreliable;
Session Pooler connectivity was verified. This is not a transaction-pooler or
production-hosting decision.

The recorded approved sslmode=require configuration requires TLS without plaintext
fallback but does not verify the server certificate/hostname. No custom CA or trust
store was introduced. Do not lower a stronger existing verification setting merely
to pass a check.

Hibernate ddl-auto is none; Spring SQL initialization is never; Flyway is absent.
Connectivity does not establish schema/migration completeness or today's table
state. Historical observations and attribution remain in FEATURE_STATUS.

## Current Verification

### Optional live PostgreSQL connectivity

Only when intentionally checking configured, reachable PostgreSQL, run from
backend/ with its datasource variables available. In the startup example, insert
these lines inside try before the Java command and environment cleanup:

```powershell
.\mvnw.cmd '-DdatabaseConnectivity=true' '-Dtest=DatabaseConnectivityTests' test
if ($LASTEXITCODE -ne 0) { throw 'Database connectivity verification failed' }
```

The test obtains the application's DataSource connection, checks PostgreSQL
metadata without printing URL/user, marks it read-only and executes SELECT 1.
It closes resources and performs no schema/data mutation. It does not verify
tables, migrations, application schema or production readiness. A successful psql
connection alone is not JDBC verification. No live check is needed for documentation
reconciliation.

### Technical browser smoke checks

After npm ci, from frontend/, install Chromium if needed and build before E2E:

```powershell
npx playwright install chromium
npm run build
npm run test:e2e
```

Port 3000 must be free: the suite starts its own production server and refuses to
reuse another one. It checks the neutral page at 390x844 and 1440x900 for rendering,
no horizontal overflow and no uncaught page errors. These are technical checks,
not tutoring workflows or security verification. Playwright does not replace
Spring Boot/JUnit/backend integration tests.

Build/test artifacts are ignored: backend/target/, frontend/node_modules/, .next/,
next-env.d.ts, TypeScript cache and Playwright output. Keep package-lock.json
tracked. Generated frontend/AGENTS.md and frontend/CLAUDE.md are also ignored;
root AGENTS.md remains authoritative.

## Security and Business Boundaries

These are future implementation requirements, not claims of implemented protections:

- Spring Boot owns role/eligibility, nested Teacher ownership, Student Enrollment and
  trusted payment effects. No frontend claim establishes authority.
- Separate Student/Teacher registration; Teacher authority requires TEACHER,
  verified email, unlocked account and approved onboarding. Application snapshots
  remain separate from the current public profile.
- users.locked is the only blocking mechanism; no disabled/account_status lifecycle.
  Verify new email before replacing old; reset revokes all refresh sessions and
  logged-in password change revokes others while preserving current.
- Whole-Course Enrollment is unique per Student/Course. Capacity/cutoff enforcement
  is transactional; free Courses create no fake Payments.
- VietQR is the direct-to-Teacher direction; actual trustworthy bank evidence is
  required. No platform custody, wallets, payouts, commission or accounting.
  Full refunds use Teacher transfer/proof and Admin completion verification.
- One manual protected Course Meet URL serves all Sessions; no Meet API.
- Google Calendar uses a system/organization account with Session event mapping;
  Session remains authoritative and sync failure cannot roll back core changes.
- Supabase Storage is approved for Teacher avatars, Course thumbnails, Assignment/
  Submission files and refund proof; database stores paths/metadata only.
- No integration, application schema or business feature is implemented by these
  decisions. Keep credentials, hashes, private bank evidence and signed URLs out
  of public projections/logs and secrets outside Git.


## Deferred Decisions

The approved Physical V1 contains exactly 22 tables. G-PHYSICAL is RESOLVED;
TASK-004 is TODO and awaits explicit implementation approval. Adapter, Calendar
and Storage implementation remain separate work. No migration is created.

Concrete API/UI contracts, token transport/security parameters, provider authenticity/
retry details, Storage bucket/validation contracts, scheduling edges, progress/reporting
formulas, moderation and production operations remain open. Consult
[TASK_BREAKDOWN.md](docs/TASK_BREAKDOWN.md) and
[DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md); do not infer policy from examples.


## Documentation

README is descriptive only. Authority flows from AGENTS through specification,
requirements, rules, use cases and their respective designs. TASK_BREAKDOWN owns
work scope, dependencies, planning readiness and decision gates. FEATURE_STATUS
owns live implementation status and completion evidence.

| Document | Purpose |
|---|---|
| [AGENTS.md](AGENTS.md) | Project instructions, authority and development workflow. |
| [PROJECT_SPEC.md](docs/PROJECT_SPEC.md) | Product identity, approved scope and high-level boundaries. |
| [REQUIREMENTS.md](docs/REQUIREMENTS.md) | Functional/non-functional requirements and open decisions. |
| [BUSINESS_RULES.md](docs/BUSINESS_RULES.md) | Ownership, privacy, payment trust and governing rules. |
| [USE_CASES.md](docs/USE_CASES.md) | Actor workflows and authorization boundaries. |
| [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md) | Domain concepts and invariants, not table mandates. |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Approved system boundaries and technical direction. |
| [DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | Persistence direction and gated physical-design choices. |
| [API_DESIGN.md](docs/API_DESIGN.md) | API responsibilities and candidate/deferred contracts. |
| [TASK_BREAKDOWN.md](docs/TASK_BREAKDOWN.md) | Work, acceptance boundaries, dependencies and readiness. |
| [FEATURE_STATUS.md](docs/FEATURE_STATUS.md) | Actual implementation/verification and supporting evidence. |

## Contribution Workflow

Read authoritative documents, relevant project-local skills and the current task
before changing behavior. The current domain skill is
[english-tutoring-domain](.agents/skills/english-tutoring-domain/SKILL.md).
Do not invent unresolved requirements or skip dependent design/UI decisions.

Implement only approved task scope, preserve backend ownership/authorization and
payment trust, and add appropriate tests/evidence. Record verified completion in
FEATURE_STATUS; design documentation alone is not completion. Preserve completed
foundations and unrelated work. Do not rename technical packages, artifacts,
databases or paths solely for product-title consistency.
