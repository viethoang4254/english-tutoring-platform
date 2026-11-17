# Teacher-managed Calendar invitations, Batch 1

An approved, unlocked Teacher can request invitations for upcoming scheduled sessions
of an owned published course. Their connected Google account and app-created secondary
calendar are required. Students need only their registered application email. Student
Overview, Schedule and protected session Join remain the website entry points.

## Course API

Use GET or POST on `/api/v1/integrations/google-calendar/courses/{course}/invitations`.
Both require a Teacher bearer token. POST takes no input and uses the existing Google
integration CSRF cookie/header. GET on the integration root supplies the CSRF token.
POST queues a snapshot and returns HTTP 202. Both course responses count pending,
synchronized and failed **session jobs** for the current connection. A successful
write or matching event readback completes a job. These counts do not measure email
delivery or RSVP. The Teacher UI for this action remains pending.

The new `APP_GOOGLE_CALENDAR_INVITATIONS_ENABLED` flag defaults to false. The existing
OAuth and sync flags, usable Teacher grant and V13 schema are prerequisites. No new
scope or migration is needed. Leave invitations disabled in real environments during
Batch 1 review. Installed TEST migration versions have not been checked.

## Processing

The server selects future SCHEDULED sessions from the owned PUBLISHED course. Invitees
are unique normalized registered emails of unlocked, login-eligible Students with
ACTIVE enrollment and a CONFIRMED payment for that enrollment. There is no Student
Google connection join. The worker checks the live snapshot against the claimed
revision and lease before remote mutation. Concurrent changes during an in-flight
Google request still need subsequent reconciliation.

The action reuses V13's destination/session key, generated event ID, attempted and
applied revisions, bounded retry and lease. Repeating an unchanged request preserves
the revision. A retry reads the same remote ID and verifies calendar and event
ownership. An already-applied private invitation with the same revision, fields,
times and guests completes by readback. Conditional PATCH preserves unrelated fields.
When the guest set changes the patch retains provider RSVP data for remaining guests
and requests sendUpdates=all. Matching guest lists are omitted from updates.

Events have private visibility and disable guest invitations, changes and guest-list
visibility. Descriptions contain only the Student website schedule link for the
session. Joining Meet still requires the protected website action. A background
personal sync job encountering an invitation-managed event records a reconciliation
failure instead of overwriting the invitations.

## Verification and remaining work

TASK-009 already runs CalendarTransportTests, CalendarSyncWorkerTests,
GoogleCalendarHttpTests, GooglePersistenceTests and ReconciliationPersistenceTests.
Use the fake HTTP provider and disposable PostgreSQL suites for verification. Never
use real grants for automated tests. Google delivery and calendar appearance depend
on recipient settings.

Automatic enrollment-change, rescheduling, cancellation and disconnect cleanup,
reminders, and the Teacher management UI are deferred. Disconnect immediately disables
the local connection and revokes credentials. Existing remote events may persist.
Before real-provider acceptance, complete lifecycle reconciliation, validate the exact
revision in hosted CI, verify the isolated database schema and use explicitly consenting
Teacher and Student test accounts.
