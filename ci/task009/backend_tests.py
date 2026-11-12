"""Sequential feature tests; only fresh, labelled GitHub-runner containers."""
from contextlib import contextmanager
import errno
import json
import os
import re
import secrets
import socket
import sys
import time
import xml.etree.ElementTree as ET
import run as support

UNIT = ["AdminRegistrationHttpTests", "AdminUserServiceTests", "ReconciliationHttpTests", "PaymentMoneyTests", "VietQrTests", "PaymentHttpTests", "ReceivingAccountReviewHttpTests", "ReceivingSubmissionTests", "ReceivingImageTests", "ReceivingStorageTests", "ReceivingAccountHttpTests", "PublicCourseHttpTests", "EnrollmentServiceTests", "EnrollmentHttpTests", "CategoryServiceTests", "TeacherCategoryServiceTests", "CategoryHttpTests", "TeacherCategoryHttpTests",
        "CourseInputTests", "CourseServiceTests", "CourseHttpTests", "CalendarRangeTests", "CalendarRepositoryTests",
        "TeacherCalendarHttpTests", "SchedulePlanTests", "SessionInputTests", "RescheduleInputTests", "SessionHttpTests",
        "SecurityBoundaryTests", "StudentProfileHttpTests", "TeacherOnboardingHttpTests", "ApiBoundaryTests"]


GROUPS = {"feature-unit-http", "category-persistence", "category-http", "course-persistence", "enrollment-persistence", "receiving-persistence", "payment-persistence"}
STAGES = {"group", "memory-preflight", "port-availability", "postgres-create",
          "postgres-readiness", "database-create", "empty-database-verification",
          "test-environment", "report-preparation", "maven-execution", "test-report-collection",
          "postgres-cleanup"}


def diagnostic(group, stage, event, failure=None, *, attempt=None, retry_exhausted=False):
    # Allowlisted names and numeric metadata only; never serialize exceptions/commands.
    code, timed_out = None, False
    current = failure
    while current is not None:
        if isinstance(current, support.CommandFailure) and type(current.exit_code) is int:
            code = current.exit_code
        if isinstance(current, (support.subprocess.TimeoutExpired, support.ReadinessTimeout)):
            timed_out = True
        if isinstance(current, support.ResourceFailure) and stage == "maven-execution":
            stage = "memory-preflight"
        current = current.__cause__
    fields = {"group": group if group in GROUPS else "unknown",
              "stage": stage if stage in STAGES else "unknown",
              "event": event if event in {"start", "complete", "failure"} else "unknown",
              "exit_code": code, "timeout": timed_out}
    if attempt is not None:
        fields.update({"socket_errno": failure.errno if isinstance(failure, OSError) and type(failure.errno) is int else None,
                       "attempt": attempt if type(attempt) is int else None,
                       "retry_exhausted": retry_exhausted is True})
    print(json.dumps(fields), flush=True)


@contextmanager
def stage(group, name):
    diagnostic(group, name, "start")
    try:
        yield
    except Exception as failure:
        diagnostic(group, name, "failure", failure)
        raise
    else:
        diagnostic(group, name, "complete")


RECONCILIATION = "ReconciliationHttpTests"
TEST_CLASS = "com.englishlearning.features.auth.security." + RECONCILIATION
METHODS = {"teacherConfirmationUsesAuthenticatedIdentityAndNoStore", "studentAdminAnonymousAndUnverifiedTeacherDenied",
           "wrongTeacherCannotSeeOrReconcilePayment", "businessFailureIsAuditedAndReturnedAsConflict",
           "foreignReceiverCannotProbeAnAllocatedOrUnusedReference", "absentAndFalseAttestationCannotActivate",
           "queueDetailHistoryPaginationAndPrivateFields", "infrastructureErrorsDoNotLeakEvidence"}
EXCEPTIONS = {"java.lang.AssertionError", "org.opentest4j.AssertionFailedError",
              "org.mockito.exceptions.verification.NeverWantedButInvoked",
              "org.mockito.exceptions.verification.WantedButNotInvoked",
              "org.mockito.exceptions.verification.TooManyActualInvocations",
              "org.mockito.exceptions.verification.NoInteractionsWanted"}
SAFE_VALUES = {"SUCCESS", "FAILED", "AMOUNT_MISMATCH", "RECEIVER_MISMATCH", "ATTESTATION_REQUIRED",
               "PAYMENT_UNAVAILABLE", "no-store", "true", "false"}


def reconciliation_report(files):
    report = {"suite": RECONCILIATION, "status": "missing", "cases": []}
    if len(files) != 1:
        return report
    try:
        root = ET.parse(files[0]).getroot()
    except (ET.ParseError, OSError):
        report["status"] = "unreadable"
        return report
    report["status"] = "collected"
    for case in root.findall("testcase"):
        for node in case:
            if node.tag not in {"failure", "error"} or len(report["cases"]) >= 8:
                continue
            method = case.get("name")
            kind = node.get("type")
            item = {"method": method if method in METHODS else "redacted", "classification": node.tag,
                    "exception": kind if kind in EXCEPTIONS else "redacted", "assertion": "redacted"}
            msg = node.get("message", "")
            status = re.fullmatch(r"Status expected:<(\d{3})> but was:<(\d{3})>", msg, re.ASCII)
            if status and all(100 <= int(v) <= 599 for v in status.groups()):
                item.update(assertion="HTTP status mismatch", expected=status[1], actual=status[2])
            elif kind in EXCEPTIONS and kind.startswith("org.mockito."):
                item["assertion"] = "Mock interaction mismatch"
            elif msg.startswith("JSON path ") or msg.startswith("No value at JSON path "):
                item["assertion"] = "JSON response assertion"
                values = re.search(r" expected:<(\w+)> but was:<(\w+)>$", msg)
                if values and all(v in SAFE_VALUES for v in values.groups()):
                    item.update(expected=values[1], actual=values[2])
            elif msg.startswith("Response header "):
                item["assertion"] = "Response header assertion"
            frame = re.search(r"(?m)^\s*at " + re.escape(TEST_CLASS) +
                              r"\.(\w+)\(ReconciliationHttpTests\.java:([1-9][0-9]{0,3})\)\s*$", node.text or "")
            if frame and frame[1] in METHODS:
                item["frame"] = RECONCILIATION + ".java:" + frame[2]
            report["cases"].append(item)
    return report


def tests(label, suites, flags=()):
    reports = support.REPO / "backend/target/surefire-reports"
    with stage(label, "report-preparation"):
        # Remove only these previous XML reports so absent execution cannot look successful.
        for suite in suites:
            for old in reports.glob("TEST-*." + suite + ".xml"):
                old.unlink()
    try:
        with stage(label, "maven-execution"):
            support.maven(["-Dtest=" + ",".join(suites), *flags, "-DforkCount=1", "-DreuseForks=false",
                           "-Djunit.jupiter.execution.parallel.enabled=false",
                           "-DargLine=-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m", "test"])
    finally:
        primary = sys.exc_info()[1]
        try:
            with stage(label, "test-report-collection"):
                if RECONCILIATION in suites:
                    try:
                        files = list(reports.glob("TEST-*." + RECONCILIATION + ".xml"))
                        details = reconciliation_report(files)
                        destination = support.root() / "reports"
                        destination.mkdir(exist_ok=True)
                        (destination / "reconciliation-diagnostics.json").write_text(json.dumps(details))
                        print("Reconciliation diagnostics: " + json.dumps(details), flush=True)
                    except Exception:
                        print("Reconciliation diagnostics unavailable", flush=True)
                result = []
                for suite in suites:
                    files = list(reports.glob("TEST-*." + suite + ".xml"))
                    counts = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
                    if len(files) == 1:
                        counts = {key: int(ET.parse(files[0]).getroot().attrib[key]) for key in counts}
                    result.append({"suite": suite, **counts})
                destination = support.root() / "reports"
                destination.mkdir(exist_ok=True)
                (destination / (label + ".json")).write_text(json.dumps(result, indent=2))
                print(label + ": " + json.dumps(result))
                support.require(all(r["tests"] > 0 and not (r["failures"] or r["errors"] or r["skipped"]) for r in result),
                                "Required feature suite failed, missing or skipped")
        except Exception:
            if primary is None:
                raise
            # The original Maven exception resumes when this finally block exits.



PORT_ATTEMPTS = 5
PORT_RETRY_DELAY = 1


def check_port(label, port):
    for attempt in range(1, PORT_ATTEMPTS + 1):
        try:
            with socket.socket() as check:
                check.bind(("127.0.0.1", port))
            return
        except OSError as failure:
            exhausted = failure.errno == errno.EADDRINUSE and attempt == PORT_ATTEMPTS
            diagnostic(label, "port-availability", "failure", failure,
                       attempt=attempt, retry_exhausted=exhausted)
            if failure.errno != errno.EADDRINUSE or exhausted:
                support.port_diagnostics(label, port)
                raise
            time.sleep(PORT_RETRY_DELAY)


@contextmanager
def container_cleanup(label, name):
    try:
        yield
    finally:
        primary = sys.exc_info()[1]
        try:
            with stage(label, "postgres-cleanup"):
                listing = ["docker", "ps", "--all", "--filter", "name=^/" + name + "$", "--format", "{{.Names}}"]
                if support.run(listing):
                    info = json.loads(support.run(["docker", "inspect", name]))[0]
                    support.require(info["Config"]["Labels"].get("task009-feature") == name, "Cleanup ownership mismatch")
                    support.run(["docker", "rm", "-f", "-v", name])
                support.require(not support.run(listing), "Container removal not confirmed")
        except Exception:
            # Cleanup is already reported; preserve the original failure when both fail.
            if primary is None:
                raise


def database_group(label, port, user, databases, suites, flags, prefix):
    with stage(label, "memory-preflight"):
        support.resources(3072)
    with stage(label, "port-availability"):
        try:
            support.require_non_ephemeral(port)
        except Exception:
            support.port_diagnostics(label, port)
            raise
        check_port(label, port)
    name = "task009-feature-" + secrets.token_hex(6)
    with container_cleanup(label, name):
        with stage(label, "postgres-create"):
            support.run(["docker", "run", "--detach", "--name", name, "--label", "task009-feature=" + name,
                         "--memory", "512m", "--publish", f"127.0.0.1:{port}:5432",
                         "--env", "POSTGRES_USER=" + user, "--env", "POSTGRES_DB=" + databases[0],
                         "--env", "POSTGRES_PASSWORD_FILE=/run/secrets/password", "--mount",
                         "type=bind,src=" + str(support.root() / "pg-password") + ",dst=/run/secrets/password,readonly",
                         "postgres:18", "-c", "shared_buffers=64MB", "-c", "max_connections=20"], timeout=180)
        def query(db, statement):
            return support.run(["docker", "exec", "-i", name, "psql", "-X", "-U", user, "-d", db,
                                "-At", "-v", "ON_ERROR_STOP=1"], data=statement)
        with stage(label, "postgres-readiness"):
            support.wait_for(lambda: support.require(query(databases[0], "SELECT current_database()||'|'||current_user")
                                                    == databases[0] + "|" + user, "Test identity mismatch"))
        with stage(label, "database-create"):
            for db in databases[1:]:
                query(databases[0], "CREATE DATABASE " + db)
        with stage(label, "empty-database-verification"):
            for db in databases:
                support.require(query(db, "SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')") == "0", "Fresh database required")
        with stage(label, "test-environment"):
            os.environ[prefix + "_TEST_USER"] = user
            os.environ[prefix + "_TEST_PASSWORD"] = os.environ["DATABASE_PASSWORD"]
            key = "CATEGORY_LIVE_URL" if label == "category-http" else prefix + "_TEST_URL"
            os.environ[key] = f"jdbc:postgresql://127.0.0.1:{port}/{databases[0]}"
        tests(label, suites, flags)


def main():
    support.root()
    with stage("feature-unit-http", "group"):
        tests("feature-unit-http", UNIT)
    with stage("category-persistence", "group"):
        database_group("category-persistence", 15436, "task034_test",
                       ["task034_categories", "task034_migration_clean", "task034_migration_conflict"],
                       ["CategoryPersistenceTests", "CategoryMigrationTests"], ["-DcategoryPersistence=true"], "CATEGORY")
    with stage("category-http", "group"):
        database_group("category-http", 15438, "task034_test", ["task034_live"],
                       ["CategoryLiveHttpTests"], ["-DcategoryLive=true"], "CATEGORY")
    with stage("course-persistence", "group"):
        database_group("course-persistence", 15437, "task010_test", ["task010_courses"],
                       ["CoursePersistenceTests"], ["-DcoursePersistence=true"], "COURSE")
    with stage("enrollment-persistence", "group"):
        database_group("enrollment-persistence", 15440, "task016_test", ["task016_enrollments"],
                       ["EnrollmentPersistenceTests"], ["-DenrollmentPersistence=true"], "ENROLLMENT")

    with stage("receiving-persistence", "group"):
        database_group("receiving-persistence", 15441, "task022_test",
                       ["task022_receiving", "task022_upgrade_pending", "task022_upgrade_approved", "task022_upgrade_rejected"],
                       ["ReceivingAccountPersistenceTests", "ReceivingAccountMigrationTests"], ["-DreceivingPersistence=true"], "RECEIVING")

    with stage("payment-persistence", "group"):
        database_group("payment-persistence", 15442, "task022c_test", ["task022c_payments", "task022d_reconciliation"],
                       ["PaymentPersistenceTests", "ReconciliationPersistenceTests"], ["-DpaymentPersistence=true"], "PAYMENT")


if __name__ == "__main__":
    try:
        os.umask(0o077)
        main()
    except Exception:
        print("Feature acceptance failed; inspect sanitized suite counts", file=sys.stderr)
        sys.exit(1)
