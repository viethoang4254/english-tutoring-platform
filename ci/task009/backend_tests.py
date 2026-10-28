"""Sequential feature tests; only fresh, labelled GitHub-runner containers."""
from contextlib import contextmanager
import json
import os
import secrets
import socket
import sys
import xml.etree.ElementTree as ET
import run as support

UNIT = ["CategoryServiceTests", "TeacherCategoryServiceTests", "CategoryHttpTests", "TeacherCategoryHttpTests",
        "CourseInputTests", "CourseServiceTests", "CourseHttpTests", "CalendarRangeTests", "CalendarRepositoryTests",
        "TeacherCalendarHttpTests", "SchedulePlanTests", "SessionInputTests", "RescheduleInputTests", "SessionHttpTests",
        "SecurityBoundaryTests", "StudentProfileHttpTests", "TeacherOnboardingHttpTests", "ApiBoundaryTests"]


GROUPS = {"feature-unit-http", "category-persistence", "category-http", "course-persistence"}
STAGES = {"group", "memory-preflight", "port-availability", "postgres-create",
          "postgres-readiness", "database-create", "empty-database-verification",
          "test-environment", "report-preparation", "maven-execution", "test-report-collection",
          "postgres-cleanup"}


def diagnostic(group, stage, event, failure=None):
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
    print(json.dumps({"group": group if group in GROUPS else "unknown",
                      "stage": stage if stage in STAGES else "unknown",
                      "event": event if event in {"start", "complete", "failure"} else "unknown",
                      "exit_code": code, "timeout": timed_out}), flush=True)


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
        with stage(label, "test-report-collection"):
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


def database_group(label, port, user, databases, suites, flags, prefix):
    with stage(label, "memory-preflight"):
        support.resources(3072)
    with stage(label, "port-availability"):
        with socket.socket() as check:
            check.bind(("127.0.0.1", port))
    name = "task009-feature-" + secrets.token_hex(6)
    try:
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
    finally:
        with stage(label, "postgres-cleanup"):
            inspection = support.subprocess.run(["docker", "inspect", name], capture_output=True, text=True)
            if inspection.returncode == 0:
                info = json.loads(inspection.stdout)[0]
                support.require(info["Config"]["Labels"].get("task009-feature") == name, "Cleanup ownership mismatch")
                support.run(["docker", "rm", "-f", "-v", name])


def main():
    support.root()
    with stage("feature-unit-http", "group"):
        tests("feature-unit-http", UNIT)
    with stage("category-persistence", "group"):
        database_group("category-persistence", 55436, "task034_test",
                       ["task034_categories", "task034_migration_clean", "task034_migration_conflict"],
                       ["CategoryPersistenceTests", "CategoryMigrationTests"], ["-DcategoryPersistence=true"], "CATEGORY")
    with stage("category-http", "group"):
        database_group("category-http", 55436, "task034_test", ["task034_live"],
                       ["CategoryLiveHttpTests"], ["-DcategoryLive=true"], "CATEGORY")
    with stage("course-persistence", "group"):
        database_group("course-persistence", 55437, "task010_test", ["task010_courses"],
                       ["CoursePersistenceTests"], ["-DcoursePersistence=true"], "COURSE")


if __name__ == "__main__":
    try:
        os.umask(0o077)
        main()
    except Exception:
        print("Feature acceptance failed; inspect sanitized suite counts", file=sys.stderr)
        sys.exit(1)
