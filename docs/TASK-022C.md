# TASK-022C — Payment request contract

The user approved the following for this batch: server-authoritative tuition and
existing percentage discounts, exact BigDecimal intermediates, one HALF_UP rounding
to whole VND, separate rounding adjustment, and local Java VietQR generation.
These decisions supersede the earlier OPEN rounding/encoder/expiry notes for this
batch only. Confirmation, late reconciliation, refunds and retention remain outside
this implementation.

## API

| Endpoint | Contract |
| --- | --- |
| POST /api/v1/student/enrollments/{enrollmentId}/payments | Empty body/query. 201 for creation, 200 for reuse of a valid pending request. |
| GET /api/v1/student/payments/{paymentId} | Own Payment, including historical status and immutable snapshots; lazily expires overdue pending requests. |
| GET /api/v1/student/payments/{paymentId}/qr-image | Own unexpired pending Payment PNG, generated from its stored payload. |

All require authenticated, unlocked STUDENT identity. Email verification is not
required for Students. Bearer JWT and existing browser authentication/CSRF conventions
are unchanged. All responses use Cache-Control: no-store. IDs and monetary values
are decimal strings. DTO fields: id, enrollmentId, status, currency, originalAmount,
discountPercent, exactDiscountAmount, roundingAdjustment, finalAmount,
transferReference, receivingAccountId, bankBin, bankName, accountNumber,
accountHolderName, qrPayload, expiresAt, createdAt. No private QR object or Meet URL.
Legacy rows do not fabricate missing snapshots; their new optional fields are null.

400 INVALID_REQUEST rejects client fields/amounts. 403 ACCESS_DENIED protects role
and eligibility; 404 PAYMENT_NOT_FOUND/ENROLLMENT_NOT_FOUND hides cross-Student
resources. Domain conflicts are 409: ENROLLMENT_NOT_PAYABLE, RESERVATION_EXPIRED,
COURSE_NOT_PAYABLE, COURSE_FULL, RECEIVING_ACCOUNT_UNAVAILABLE,
PAYMENT_AMOUNT_INVALID, PAYMENT_LEGACY_UNAVAILABLE. Persistence failures return
sanitized 503 PAYMENT_UNAVAILABLE. Unknown commit outcomes are retried through the
same Enrollment endpoint, never reported as confirmed transfers.

## Money and history

An applicable Course discount uses start <= serverNow < end. Snapshot the percentage,
tuition and exact discount (tuition * percent / 100); round only the resulting payable
amount using HALF_UP. Store rounding = payable - (tuition - exact discount).
Reject nonpositive payable amounts and amounts exceeding 13 whole digits supported
by the QR contract. Never round the discount early or invent a discount.

V9 widens discount_amount to NUMERIC(19,6), adds discount_percent and
rounding_adjustment, and distinguishes LEGACY from VND_HALF_UP_V1. Existing values
are not recalculated. Only pre-V9 rows receive LEGACY; new rows default to
VND_HALF_UP_V1 and an INSERT trigger rejects explicit LEGACY values. The immutable
snapshot trigger prevents policy downgrades while allowing historical status updates.
Conditional constraints preserve the old equation for legacy
rows and enforce the new equation/rounding for new snapshots. A trigger protects
financial values, receiving-account identity/details, reference, payload and deadline
from subsequent changes. Historical migrations and foreign keys are unchanged.

## Reservation, concurrency and receiving account

Create only for a PENDING Enrollment with a live reservation, published Course,
generated Sessions before first start, and capacity not exceeded. The reservation
already consumes a seat; Payment creation adds none. Deadline equals the existing
reservation deadline. No automatic reservation renewal.

Lock Student/Teacher user rows in ascending order, then Course, Enrollment and selected
account. Account reviews serialize on the same Teacher user lock. Check eligible
Teacher and APPROVED/ACTIVE receiving account with audit metadata for new requests.
Reuse valid pending snapshots without changing their destination after account
replacement. Database uniqueness allows one pending Payment per Enrollment.
The transfer reference is ELP plus 22 random uppercase hexadecimal digits (88 random
bits), protected by the existing global unique constraint. A collision fails the
transaction; retry is safe and generates no second committed pending obligation.

Renewal/cancellation expire overdue PENDING Payments atomically before their existing
checks. Payment reads also materialize expiry. A failed renewal transaction rolls back
its expiration update as well; it does not extend a hold or lose history. Expired
Payments keep their original references. Explicit renewal followed by creation gets a
new Payment/reference. No scheduler or bank-side cancellation guarantee is claimed.

## Offline QR

Local ASCII EMV TLV: 00=01, 01=12; tag 38 contains A000000727, bank BIN/account and
QRIBFTTA; currency 704, exact whole-VND amount, country VN and reference in 62/08.
CRC-16/CCITT-FALSE uses initial FFFF and polynomial 1021 over the payload through
6304. ZXing Core 3.5.4 encodes PNG locally using Java ImageIO; no javase dependency,
runtime credentials, public image URL or external generation request.

Reference: https://www.vietqr.io/generate — Casso's VietQR developer documentation,
not independent bank verification or the official bank directory. Tests retain its
published payload/CRC vector, parse destination/amount/reference independently and
decode PNG with ZXing's decoder. The direct NAPAS document URL was unavailable during
inspection; no bank-app interoperability or NAPAS certification is claimed.
The supported payment encoder limits account numbers to 6-19 digits; out-of-range B.1 account
submissions remain intact but fail closed as unavailable for this encoder. Uploaded
Teacher QR evidence is never used to infer bank identity or generate Payment QR.

QR creation is transfer instruction only. A bank may accept a transfer after expiry.
No endpoint confirms Payment or activates Enrollment; Admin has no such authority.

## Verification

Prepared PaymentMoneyTests, VietQrTests, PaymentHttpTests and isolated
PaymentPersistenceTests are registered in ci/task009/backend_tests.py. The latter
requires paymentPersistence=true and the exact disposable loopback database on port
55442. CI must compile and execute all suites before acceptance. V9 expectations are
updated without removing previous suites or changing browser execution counts.

Only the ZXing dependency hunk belongs to this task in backend/pom.xml; the existing
Spring Boot mainClass configuration is unrelated and must remain unstaged.

## Scoped file inventory

36 paths are involved; shared POM and FEATURE_STATUS require selective staging later.
- `backend/src/main/java/com/englishlearning/features/payment/controller/PaymentController.java`
- `backend/src/main/java/com/englishlearning/features/payment/controller/PaymentExceptionHandler.java`
- `backend/src/main/java/com/englishlearning/features/payment/dto/PaymentDto.java`
- `backend/src/main/java/com/englishlearning/features/payment/repository/PaymentRepository.java`
- `backend/src/main/java/com/englishlearning/features/payment/service/PaymentFailure.java`
- `backend/src/main/java/com/englishlearning/features/payment/service/PaymentMoney.java`
- `backend/src/main/java/com/englishlearning/features/payment/service/PaymentService.java`
- `backend/src/main/java/com/englishlearning/features/payment/service/VietQr.java`
- `backend/src/main/resources/db/migration/V9__payment_monetary_snapshot.sql`
- `backend/src/test/java/com/englishlearning/features/auth/security/PaymentHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/PaymentMoneyTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/PaymentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/payment/VietQrTests.java`
- `docs/TASK-022C.md`
- `backend/pom.xml`
- `backend/src/main/java/com/englishlearning/features/auth/security/SecurityConfiguration.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/repository/EnrollmentRepository.java`
- `backend/src/main/java/com/englishlearning/features/enrollment/service/EnrollmentService.java`
- `backend/src/test/java/com/englishlearning/EnglishLearningApplicationTests.java`
- `backend/src/test/java/com/englishlearning/common/api/ApiBoundaryTests.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/AuthenticationHttpTestSupport.java`
- `backend/src/test/java/com/englishlearning/features/auth/security/SecurityBoundaryTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryLiveHttpTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryMigrationTests.java`
- `backend/src/test/java/com/englishlearning/features/category/CategoryPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/course/CoursePersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/enrollment/EnrollmentPersistenceTests.java`
- `backend/src/test/java/com/englishlearning/features/paymentaccount/ReceivingAccountMigrationTests.java`
- `backend/src/test/java/com/englishlearning/features/paymentaccount/ReceivingAccountPersistenceTests.java`
- `ci/task009/README.md`
- `ci/task009/backend_tests.py`
- `ci/task009/run.py`
- `ci/task009/test_backend_tests.py`
- `docs/API_DESIGN.md`
- `docs/BUSINESS_RULES.md`
- `docs/FEATURE_STATUS.md`
