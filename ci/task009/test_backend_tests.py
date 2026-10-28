"""Offline checks for the new CI report gate; executed in GitHub Actions only."""
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
