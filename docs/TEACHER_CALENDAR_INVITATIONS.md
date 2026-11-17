# Teacher-managed Calendar invitations

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
OAuth and sync flags, usable Teacher grant and V13 schema are prerequisites. Batch 2 additionally requires V14 for synchronization. OAuth-only installations can remain on V12. Keep invitations disabled in real environments pending hosted and lifecycle review. Installed TEST migration versions have not been checked.

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

## Batch 2 lifecycle

V14 retains explicit course opt-ins by destination (Teacher and Google subject)
and backfills them from identifiable Batch 1 invitation snapshots. If an old
snapshot was already erased, its former opt-in cannot be recovered from payment
data. The owning Teacher can request synchronization again.

Enrollment, payment, course, session, connection and account changes already record
invalidations in the same transaction through V13 triggers. V14 also watches
registered email changes. The projector recomputes eligible future attendees for
opted-in courses and keeps each original event ID. With no queued invalidations,
each tick revisits up to eight due course destinations, scheduled five minutes apart
per destination. A new revision is written only when the desired data changes.
The existing global invalidation pass still considers the full schedule.

A time change with the same guests sends a conditional time patch with
`sendUpdates=all` while omitting the attendee array. Cancellation removes the
owned event with a conditional DELETE and attendee notification. The local mapping
survives as a tombstone. If eligibility changes after the final local check, a write
can briefly reach Google. The next revision reconciles the same ID.

The first disconnect request records cleanup and queues cancellations while the
grant still exists. The Teacher sees cleanup pending and may retry failed sync.
New invitation requests are blocked. After cleanup, pressing disconnect again
completes the existing local invalidation, remote revocation and subject-release
flow. If Google revoked the grant first, reconnect the same subject to resume
cleanup. Successful final disconnect clears opt-ins, so later invitations require
an explicit course action. Do not enable invitations solely to clear a production
queue without reviewing its destination and pending changes.

Before issuing an invitation insert, the worker records the possibility of an
ambiguous result. A later Google 404 during cancellation cannot prove that a delayed
insert was cancelled. Retain that mapping, retry within the bounded policy, and
show a failure requiring review if uncertainty persists. Credential revocation
stays blocked until cleanup is accounted for. External notification receipt cannot
be verified through these status counts.

Use TASK-009 disposable PostgreSQL and mocked Google fixtures to verify this revision.
The migration has not been applied to TEST or production. Fifteen-minute reminders,
a complete course management UI and real-provider acceptance remain separate.
