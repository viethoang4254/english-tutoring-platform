# TASK-022B.1 — Receiving-account foundation

Implemented, pending static review and GitHub Actions verification. No live database or Storage execution is authorized by this document.

## Contract

Approved, verified, unlocked Teachers submit exactly bank BIN, holder name, account number and PNG/JPEG bank QR image. The backend maps BIN to canonical code/name. Account numbers are preserved as strings (1–34 digits); this is syntax validation, not ownership verification. Admin approval is a platform review, not independent ownership verification. Uploaded evidence never replaces payment-specific dynamic VietQR.

- POST /api/v1/teacher/payment-accounts: multipart bankBin, holderName, accountNumber, qrImage; UUID Idempotency-Key header. Returns 201 for creation, 200 for an identical retry. Changed content under the same key or another pending submission returns 409.
- GET /api/v1/teacher/payment-accounts?page=0&size=20: items/page/size/hasNext, size 1–100; account numbers masked.
- GET /api/v1/teacher/payment-accounts/{id}: full own details only.
- GET /api/v1/teacher/payment-accounts/{id}/qr-image: own private evidence.
- GET /api/v1/admin/payment-accounts/{id}/qr-image: verified, unlocked Admin read-only evidence access. No approval or payment mutation authority.

New submissions are PENDING / INACTIVE. Foreign Teacher records return 404. Unknown fields, client ownership/status fields, malformed identifiers and invalid images are rejected. Existing JWT/CSRF safeguards remain applicable. Responses are no-store; evidence is PNG with nosniff.

## Directory

Version 1 snapshot: backend/src/main/resources/bank-directory/vietqr-v1.json. Source https://api.vietqr.io/v2/banks, documentation https://www.vietqr.io/api-bank. Retrieval timestamp and raw-response SHA-256 are retained in the snapshot. The validated response contains 76 unique BIN/code records; 48 advertise transfer support and are accepted for new submissions. BINs remain strings. VietQR.io is a third-party directory, not NAPAS or bank ownership verification.

Updates require manual review of provenance, response structure, BIN/code uniqueness, supported flags and changed/removed mappings, then a new versioned snapshot and CI review. Never silently refresh at runtime or infer identity from QR contents. Existing bank_code values are never remapped.

## Private Storage configuration

External environment configuration only:

- APP_BANKQR_STORAGEORIGIN: HTTPS Supabase project origin, without trailing slash.
- APP_BANKQR_BUCKET: dedicated private bucket identifier.
- APP_BANKQR_SERVICEKEY: backend-only service credential.

Provisioning the bucket and credentials is a separate deployment operation. Do not grant anonymous or direct client object access. The adapter verifies bucket privacy before reads/uploads and after upload, rejects redirects/public buckets, and exposes neither service credentials nor signed/public URLs. External bucket-policy changes remain an operational security responsibility.

Uploads are bounded to 5 MiB, 4096 pixels per dimension and 16 megapixels, decoded and re-encoded as PNG without metadata or resizing. Opaque UUID paths contain no bank information. Stored hashes detect changed evidence. Missing storage configuration fails the operation safely; it does not disable unrelated application startup.

## Persistence and cleanup

V7 adds nullable bank_bin and review/evidence/idempotency metadata. Legacy rows become UNREVIEWED / INACTIVE; original bank identifiers, holder/account values and historical Payment foreign keys remain unchanged. No Payment or Enrollment row is updated. Database constraints require approval for ACTIVE selection, complete new submission metadata, one PENDING per Teacher and unique idempotency keys. A trigger preserves submitted account/evidence fields; corrections require new rows.

Submission performs eligibility/idempotency preflight, uploads outside any database transaction, then uses a short transaction with the existing Teacher user-row lock to recheck eligibility, pending submissions and idempotency before insertion. Concurrent callers may upload separate objects; only the winner is referenced. The losing caller reclaims only its own newly uploaded, unused object. V7 uniqueness constraints remain the final safeguard.

The Storage adapter uses JDK asynchronous HTTP with a 15-second overall deadline per operation, shared across bucket checks, body upload and bounded response consumption. Timeout/interruption cancels the exchange; interruption is restored. Cancellation cannot prove whether the remote server stored an upload, so uncertain uploads are retained privately. No remote upload or cleanup runs under the persistence lock.

After a fully successful upload, transaction completion records COMMITTED, ROLLED_BACK or UNKNOWN. Cleanup is attempted once only after confirmed rollback, or committed idempotent reuse that never referenced this new object. Successful referenced commits and UNKNOWN outcomes are never deleted. Cleanup has its own 15-second deadline and no retry; its failure emits only a static diagnostic and never replaces the original persistence error. Storage and database operations are not atomic.

Uncertain objects and failed cleanup require a separately authorized reconciliation operation: quiesce submissions, ensure no in-flight transactions, inspect at most 100 candidate objects in the dedicated bucket, compare all database references and storage creation metadata, recheck before deletion, and retain ambiguity. Historical/referenced evidence is never removed by request cleanup. Long-term retention and revocation remain open approval gates.

## CI acceptance

The existing TASK-009 backend runner includes ReceivingSubmissionTests, ReceivingImageTests, ReceivingStorageTests and ReceivingAccountHttpTests. ReceivingAccountPersistenceTests uses a separate empty PostgreSQL 18 database task022_receiving on loopback 55441, role task022_test and a generated CI-only password. It applies V1–V6, seeds historical account/payment fixtures, then applies V7 and verifies compatibility, ownership, persisted eligibility, idempotency and concurrent submissions. Storage tests use an isolated mock; no Supabase access is performed.

Admin approve/reject operations, Payment creation/confirmation, Enrollment activation, student VietQR generation and frontend screens remain outside this batch. Tests and builds have not been run locally.


## TASK-022B.2 — Admin review and account selection

Approved lifecycle: a PENDING / INACTIVE submission becomes APPROVED / ACTIVE when
an eligible Admin approves it. Any previous ACTIVE account of that Teacher becomes
INACTIVE in the same transaction. Rejection requires a reason and produces
REJECTED / INACTIVE without changing the current selected account. Restoring an old
account requires a new submission and approval; no switching/revocation API exists.
Approval is platform review, not independent ownership or bank-transaction verification.

This batch extends the B.1 read-only Admin image permission with:

- GET /api/v1/admin/payment-accounts/pending?page=0&size=20 — oldest-first queue,
  deterministic created_at/id order, items/page/size/hasNext, size 1–100. Queue items
  contain id, teacherId, bankName and submittedAt, not bank account numbers.
- GET /api/v1/admin/payment-accounts/{id} — teacherId, account (existing DTO),
  reviewedBy, reviewedAt and rejectionReason. Private no-store response.
- GET /api/v1/admin/payment-accounts/{id}/qr-image — existing authorized private read.
- POST /api/v1/admin/payment-accounts/{id}/approve — no body/query, 204 on success.
- POST /api/v1/admin/payment-accounts/{id}/reject — JSON {"reason":"..."}, 204.
  Reason must be nonblank, at most 2000 characters, and contain no control characters.
  Other fields, including client reviewer, timestamp and status, are rejected.

Only authenticated, verified, unlocked ADMIN accounts can review. Both the reviewer
and Teacher user rows are locked in ascending ID order, followed by the submission.
Teacher eligibility is rechecked under the shared submission lock. Only PENDING may
transition; duplicate or losing concurrent decisions return 409
RECEIVING_ACCOUNT_ALREADY_REVIEWED. Missing resources return 404; invalid input 400;
forbidden access 403 (missing/invalid authentication 401). Database failures roll back
both selection changes and the decision. No remote Storage request runs in review.

V8 adds reviewed_by (restricted User FK), reviewed_at and rejection_reason. The
existing review_status stores the decision; each immutable submission retains its
terminal decision as history, without a new table. Database checks require complete
decision metadata; a trigger prevents rewriting decisions or reactivating an old
INACTIVE account. Existing V1–V7 and bank/evidence fields remain unchanged. If an
out-of-band APPROVED/REJECTED V7 row lacks authentic audit metadata, V8 fails rather
than fabricating a reviewer or timestamp; resolve such data through separate approval.
Historical Payments retain their original account references. No Payment or Enrollment
state changes occur. No evidence deletion or retention policy is introduced.

B.1 was accepted at b671328cf6d668bc13483f86b0c1376d8b844d58 and integrated into dev.
B.2 implementation remains IN_PROGRESS, awaiting static review and GitHub Actions.
CI applies V8 in isolated PostgreSQL only; no local builds/tests or live writes performed.
