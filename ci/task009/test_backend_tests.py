"""Offline checks for the new CI report gate; executed in GitHub Actions only."""
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
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

    def test_primary_failure_and_cleanup_failure_both_survive(self):
        def work():
            with self.assertRaises(feature.support.CommandFailure):
                with feature.stage("category-http", "group"):
                    try:
                        with feature.stage("category-http", "postgres-readiness"):
                            raise feature.support.ReadinessTimeout("SECRET")
                    finally:
                        with feature.stage("category-http", "postgres-cleanup"):
                            raise feature.support.CommandFailure(9)
        rows = self.events(work)
        failures = [r for r in rows if r["event"] == "failure"]
        self.assertEqual([r["stage"] for r in failures], ["postgres-readiness", "postgres-cleanup", "group"])
        self.assertTrue(failures[0]["timeout"])
        self.assertEqual(failures[1]["exit_code"], 9)

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
