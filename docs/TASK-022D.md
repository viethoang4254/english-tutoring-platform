# TASK-022D — Manual Teacher payment reconciliation

## Approved boundary

The owning eligible Teacher checks the actual incoming bank transaction. Submitted
evidence is Teacher attestation, never authenticated provider evidence or proof of
bank ownership. Admin and Student have no confirmation authority. There is no
automatic bank reconciliation, refund, dispute handling, frontend or Calendar change.

## HTTP contract

All routes are under `/api/v1/teacher/courses/{courseId}/payments`, require the
existing bearer authentication and verified, unlocked, approved Teacher eligibility,
enforce Course ownership, and return `Cache-Control: no-store`.

| Method and suffix | Result |
| --- | --- |
| GET (base) | Own Course payment queue/history, newest ID first |
| GET `/{paymentId}` | Original Payment DTO and immutable amount/destination/reference |
| GET `/{paymentId}/attempts` | Append-only attempt history, newest ID first |
| POST `/{paymentId}/confirm-manually` | Confirmation while Payment and reservation remain valid |
| POST `/{paymentId}/reconcile-late` | Explicit reconciliation after Payment or reservation expiry |

Lists use `page=0`, `size=20`, maximum size 100, and `{items,page,size,hasNext}`.
The queue includes historical statuses; pending records include their authoritative
expiry timestamp. Reads do not renew reservations or mutate obligations.

Both POST requests accept exactly:

```json
{
  "bankBin": "970436",
  "accountNumber": "00123456",
  "bankTransactionReference": "BANK-TRANSACTION-REFERENCE",
  "receivedAt": "2026-11-10T09:00:00+07:00",
  "observedAmount": 101,
  "currency": "VND",
  "transferDescription": "ELP0123456789ABCDEF012345",
  "transferReference": "ELP0123456789ABCDEF012345",
  "attested": true
}
```

These are observed claims, not updates to expected Payment fields. Bank BIN and
account number must match the historical Payment snapshot, including leading zeroes.
Amount must match exactly, currency must be VND, and the description must contain
the exact case-sensitive Payment reference with non-alphanumeric token boundaries.
Received time must fall between Payment creation and server attempt time, inclusive.
Offset-bearing timestamps are required and normalized to PostgreSQL microseconds.

Bank references are limited to 128 characters; only surrounding ASCII spaces are
trimmed. Case, internal spaces, punctuation and leading zeroes remain significant.
Control characters are rejected. Description limit is 1,000 characters. Structurally
invalid requests return 400 before a business decision; false attestation is a
recorded business failure. Missing attestation returns 400.

HTTP 200 returns a SUCCESS attempt. HTTP 409 returns the durable FAILED attempt,
including `failureReason`. Attempt responses contain `id`, `paymentId`, `teacherId`,
`evidence`, `late`, `attemptedAt`, `outcome`, `failureReason`, `allocated`, `replayOf`.
IDs use the existing string ID convention. Cross-owner resources return 404;
ineligible roles return 403 (invalid/locked authentication can return 401).
Infrastructure failure returns sanitized 503, without claiming a completed decision.

Business failure codes include receiver/amount/currency/reference/time mismatch,
missing attestation, already allocated transaction, already confirmed Payment,
ineligible Student/Course, non-PENDING Enrollment, conflicting Payment, wrong
normal/late endpoint, unsupported legacy snapshot, and
`MANUAL_RESOLUTION_REQUIRED_NO_CAPACITY`.

## Transactions and durable failure

Lock order follows existing creation/review conventions: Student and Teacher user
rows in ascending ID order, Course, Enrollment, Payment, then one transaction-scoped
PostgreSQL advisory lock on `(bank BIN, account number, normalized bank reference)`.
Attestation and evidence must match the locked Payment before any allocation lock or lookup. Both use the immutable Payment BIN and account number with the normalized bank reference. Hash collisions only serialize unrelated requests. PostgreSQL unique indexes remain
the authoritative allocation guard. Ownership and eligibility are rechecked under locks.

The service returns business failures without throwing; its transaction commits the
FAILED attempt before the controller returns 409. Failed decisions do not mutate
Payment or Enrollment. Success atomically updates Payment CONFIRMED, server
`confirmed_at`, Enrollment ACTIVE and its activation time, then inserts the audit.
Any infrastructure error rolls back all these changes; it is not represented as a
completed business decision. The caller may safely retry after an uncertain response.
Internal callers must not discard the enclosing transaction after a returned decision.

An identical confirmed-transaction retry appends a SUCCESS replay pointing to the
original allocated attempt, without updating Payment/Enrollment again. Different
evidence cannot reuse an allocated transaction. Failed attempts never allocate it.
Uniqueness uses physical bank/account identity rather than the account submission ID,
so replacing a submission for the same bank account does not reset duplicate protection.

## Late reconciliation

Both transfers received before expiry but confirmed afterward and transfers received
after expiry use the separate late endpoint. Neither extends the original deadline.
Recheck PUBLISHED Course, first Session strictly in the future, eligible unlocked
Student (email verification is not required), PENDING Enrollment and capacity.
An unexpired existing seat counts once; an expired seat must be reacquired under the
Course lock. Successful activation clears the hold rather than renewing it.

Any other PENDING or CONFIRMED Payment on the Enrollment blocks confirmation. Newer
obligations are never cancelled or rewritten here. Existing expiry handling can mark
an overdue pending obligation EXPIRED; a subsequent explicit reconciliation retry
then reevaluates current conditions. No seat means a durable failed record and manual
resolution, not automatic refund or activation. CANCELLED/COMPLETED Enrollment and
CANCELLED Payment remain denied. Legacy Payments without trustworthy snapshots are
not silently reconstructed. Account replacement never redirects a historical Payment.

## V10 and acceptance

V10 adds `payment_reconciliation_attempts`; V1–V9 and existing rows remain unchanged.
Foreign keys preserve Payment/Teacher references. Check constraints restrict outcomes;
partial unique indexes restrict first successful allocation per Payment and bank
transaction. Successful retries reference the original attempt. UPDATE/DELETE triggers
and a statement-level BEFORE TRUNCATE trigger protect the history. Restrict database privileges separately: privileged administrators can disable triggers. No rows are inserted into `payment_transactions`.

`ReconciliationHttpTests` and `ReconciliationPersistenceTests` join existing TASK-009
gates: 8 HTTP methods and 21 PostgreSQL methods, plus one CI registration helper test.
PostgreSQL uses the separate disposable `task022d_reconciliation` database in
the existing payment group on port 15442. Historical TASK-022C suites remain enabled.
Migration expectations become ten migrations and 24 application tables. No local
tests or builds were run; compilation, migration and race behavior await CI.

## Release prerequisites

Evidence is retained without automatic deletion. Final privacy, retention/deletion,
dispute handling and operational access/logging policy require explicit production
review. Do not enable SQL parameter, request-body or bank-evidence logging. No
regulatory compliance, independent bank verification or automatic settlement is claimed.

## Scoped files (24 paths)

New files:
- `backend/src/main/java/com/englishlearning/features/payment/controller/ReconciliationController.java`
- `backend/src/main/java/com/englishlearning/features/payment/dto/ReconciliationAttempt.java`
- `backend/src/main/java/com/englishlearning/features/payment/dto/ReconciliationEvidence.java`
- `backend/src/main/java/com/englishlearning/features/payment/service/ReconciliationService.java`
- `backend/src/main/resources/db/migration/V10__manual_payment_reconciliation.sql`
- `backend/src/test/java/com/englishlearning/features/auth/security/ReconciliationHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/ReconciliationPersistenceTests.java`
- `docs/TASK-022D.md`

Existing files with scoped changes:
- `backend/src/main/java/com/englishlearning/features/auth/security/SecurityConfiguration.java`
- `backend/src/main/java/com/englishlearning/features/payment/controller/PaymentExceptionHandler.java`
- `backend/src/main/java/com/englishlearning/features/payment/repository/PaymentRepository.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryLiveHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryMigrationTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/course/CoursePersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/enrollment/EnrollmentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/PaymentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/paymentaccount/ReceivingAccountPersistenceTests.java`
- `ci/task009/backend_tests.py`
- `ci/task009/run.py`
- `ci/task009/test_backend_tests.py`
- `ci/task009/README.md`
- `docs/API_DESIGN.md` (TASK-022D appendix only)
- `docs/FEATURE_STATUS.md` (TASK-022D appendix only; preserve unrelated changes)

The POM, historical migrations, frontend and pre-existing untracked work are excluded.
