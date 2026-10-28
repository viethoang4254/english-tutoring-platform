"""Offline checks for the new CI report gate; executed in GitHub Actions only."""
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import call, patch
import backend_tests as feature


class FeatureReportChecks(unittest.TestCase):
    def check_report(self, attributes, accepted):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            reports = root / "backend/target/surefire-reports"
            reports.mkdir(parents=True)
            def maven(_arguments):
                if attributes is not None:
                    (reports / "TEST-example.ExampleTests.xml").write_text(
                        '<testsuite ' + attributes + '/>')
            with patch.object(feature.support, "REPO", root), patch.object(feature.support, "root", return_value=root), patch.object(feature.support, "maven", side_effect=maven):
                if accepted:
                    feature.tests("sample", ["ExampleTests"])
                else:
                    with self.assertRaises(RuntimeError):
                        feature.tests("sample", ["ExampleTests"])
            self.assertTrue((root / "reports/sample.json").exists())

    def test_success(self):
        self.check_report('tests="2" failures="0" errors="0" skipped="0"', True)

    def test_missing_failed_and_skipped_suites_are_rejected(self):
        for attributes in [None, 'tests="1" failures="1" errors="0" skipped="0"',
                           'tests="1" failures="0" errors="1" skipped="0"',
                           'tests="1" failures="0" errors="0" skipped="1"',
                           'tests="0" failures="0" errors="0" skipped="0"']:
            with self.subTest(attributes=attributes):
                self.check_report(attributes, False)

    def test_main_preserves_group_order_and_uses_distinct_database_ports(self):
        executed = []
        with patch.object(feature.support, "root"), patch("sys.stdout", io.StringIO()), \
                patch.object(feature, "tests", side_effect=lambda label, suites: executed.append((label, None))), \
                patch.object(feature, "database_group", side_effect=lambda label, port, *args: executed.append((label, port))):
            feature.main()
        self.assertEqual(executed, [
            ("feature-unit-http", None),
            ("category-persistence", 55436),
            ("category-http", 55438),
            ("course-persistence", 55437),
        ])
        self.assertEqual(len({port for _, port in executed[1:]}), 3)

    def test_main_requires_runner_guard_before_tests(self):
        with patch.object(feature.support, "root", side_effect=RuntimeError("Not CI")), patch.object(feature, "tests") as tests:
            with self.assertRaises(RuntimeError):
                feature.main()
            tests.assert_not_called()


class DiagnosticChecks(unittest.TestCase):
    def events(self, action):
        output = io.StringIO()
        with patch("sys.stdout", output):
            action()
        return [json.loads(line) for line in output.getvalue().splitlines()]

    def test_untrusted_values_and_exception_text_are_not_logged(self):
        rows = self.events(lambda: feature.diagnostic("SECRET", "SECRET", "SECRET", RuntimeError("SECRET")))
        self.assertEqual(rows, [{"group": "unknown", "stage": "unknown", "event": "unknown", "exit_code": None, "timeout": False}])

    def test_exit_code_and_timeout_without_command_or_output(self):
        for failure, code, timeout in [
            (feature.support.CommandFailure(7), 7, False),
            (feature.support.subprocess.TimeoutExpired(["SECRET"], 10, output="SECRET"), None, True),
            (feature.support.ReadinessTimeout("SECRET"), None, True),
        ]:
            rows = self.events(lambda: feature.diagnostic("category-http", "maven-execution", "failure", failure))
            self.assertEqual(rows[0]["exit_code"], code)
            self.assertEqual(rows[0]["timeout"], timeout)
            self.assertNotIn("SECRET", json.dumps(rows))

    def test_maven_memory_failure_is_identified(self):
        rows = self.events(lambda: feature.diagnostic("feature-unit-http", "maven-execution", "failure", feature.support.ResourceFailure("SECRET")))
        self.assertEqual(rows[0]["stage"], "memory-preflight")

    def test_subprocess_failure_retains_only_numeric_exit_metadata(self):
        result = feature.support.subprocess.CompletedProcess(["SECRET"], 3, "SECRET", "SECRET")
        with patch.object(feature.support.subprocess, "run", return_value=result):
            with self.assertRaises(feature.support.CommandFailure) as caught:
                feature.support.run(["SECRET"])
        self.assertEqual(caught.exception.exit_code, 3)
        self.assertNotIn("SECRET", str(caught.exception))


class PortChecks(unittest.TestCase):
    def probe(self, failures, expected=None):
        output = io.StringIO()
        with patch.object(feature.socket, "socket") as sockets, patch.object(feature.time, "sleep") as sleep, patch("sys.stdout", output):
            bind = sockets.return_value.__enter__.return_value.bind
            bind.side_effect = failures
            if expected is None:
                feature.check_port("category-http", 55436)
            else:
                with self.assertRaises(OSError) as caught:
                    feature.check_port("category-http", 55436)
                self.assertIs(caught.exception, expected)
            self.assertEqual(bind.call_args_list, [call(("127.0.0.1", 55436))] * len(failures))
            self.assertEqual(sockets.return_value.__exit__.call_count, len(failures))
        self.assertNotIn("SECRET", output.getvalue())
        return [json.loads(line) for line in output.getvalue().splitlines()], sleep.call_args_list

    def test_available_first_attempt(self):
        rows, delays = self.probe([None])
        self.assertEqual(rows, [])
        self.assertEqual(delays, [])

    def test_transient_address_in_use(self):
        rows, delays = self.probe([OSError(feature.errno.EADDRINUSE, "SECRET"), None])
        self.assertEqual(rows, [{"group": "category-http", "stage": "port-availability", "event": "failure",
                                 "exit_code": None, "timeout": False, "socket_errno": feature.errno.EADDRINUSE,
                                 "attempt": 1, "retry_exhausted": False}])
        self.assertEqual(delays, [call(1)])

    def test_persistent_address_in_use(self):
        failure = OSError(feature.errno.EADDRINUSE, "SECRET")
        rows, delays = self.probe([failure] * 5, failure)
        self.assertEqual([r["attempt"] for r in rows], [1, 2, 3, 4, 5])
        self.assertEqual([r["retry_exhausted"] for r in rows], [False] * 4 + [True])
        self.assertEqual(delays, [call(1)] * 4)

    def test_other_socket_errors_fail_immediately(self):
        for number in [feature.errno.EACCES, None]:
            with self.subTest(errno=number):
                failure = OSError(number, "SECRET")
                rows, delays = self.probe([failure], failure)
                self.assertEqual(rows[0]["socket_errno"], number)
                self.assertFalse(rows[0]["retry_exhausted"])
                self.assertEqual(delays, [])


class CleanupChecks(unittest.TestCase):
    name = "task009-feature-test"

    def test_removal_verified_after_ownership_check(self):
        inspection = json.dumps([{"Config": {"Labels": {"task009-feature": self.name}}}])
        with patch.object(feature.support, "run", side_effect=[self.name, inspection, "", ""]) as run, patch("sys.stdout", io.StringIO()):
            with feature.container_cleanup("category-http", self.name):
                pass
        listing = ["docker", "ps", "--all", "--filter", "name=^/" + self.name + "$", "--format", "{{.Names}}"]
        self.assertEqual(run.call_args_list, [call(listing), call(["docker", "inspect", self.name]),
                                            call(["docker", "rm", "-f", "-v", self.name]), call(listing)])

    def test_missing_container_is_confirmed_without_removal(self):
        with patch.object(feature.support, "run", return_value="") as run, patch("sys.stdout", io.StringIO()):
            with feature.container_cleanup("category-http", self.name):
                pass
        self.assertEqual(run.call_count, 2)
        self.assertTrue(all(c.args[0][:2] == ["docker", "ps"] for c in run.call_args_list))

    def test_wrong_owner_is_not_removed(self):
        inspection = json.dumps([{"Config": {"Labels": {"task009-feature": "other"}}}])
        with patch.object(feature.support, "run", side_effect=[self.name, inspection]) as run, patch("sys.stdout", io.StringIO()):
            with self.assertRaises(RuntimeError):
                with feature.container_cleanup("category-http", self.name):
                    pass
        self.assertEqual(run.call_count, 2)

    def test_remaining_container_fails_verification(self):
        inspection = json.dumps([{"Config": {"Labels": {"task009-feature": self.name}}}])
        with patch.object(feature.support, "run", side_effect=[self.name, inspection, "", self.name]), patch("sys.stdout", io.StringIO()):
            with self.assertRaises(RuntimeError):
                with feature.container_cleanup("category-http", self.name):
                    pass

    def test_cleanup_failure_fails_successful_group_and_preserves_primary(self):
        for primary in [None, feature.support.ReadinessTimeout("SECRET")]:
            with self.subTest(primary=primary is not None):
                cleanup = feature.support.CommandFailure(9)
                output = io.StringIO()
                with patch.object(feature.support, "run", side_effect=cleanup), patch("sys.stdout", output):
                    with self.assertRaises(Exception) as caught:
                        with feature.stage("category-http", "group"):
                            with feature.container_cleanup("category-http", self.name):
                                if primary is not None:
                                    with feature.stage("category-http", "postgres-readiness"):
                                        raise primary
                self.assertIs(caught.exception, primary if primary is not None else cleanup)
                rows = [json.loads(line) for line in output.getvalue().splitlines()]
                failures = [r for r in rows if r["event"] == "failure"]
                expected = ["postgres-cleanup", "group"] if primary is None else ["postgres-readiness", "postgres-cleanup", "group"]
                self.assertEqual([r["stage"] for r in failures], expected)
                self.assertEqual(failures[-2]["exit_code"], 9)
                self.assertEqual(failures[-1]["timeout"], primary is not None)
                self.assertNotIn("SECRET", output.getvalue())
