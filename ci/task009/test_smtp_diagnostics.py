"""Sanitized SMTP pipeline diagnostics; run by hosted CI only."""
import io
import json
import subprocess
import unittest
from unittest.mock import patch
import run as support


class SmtpDiagnosticChecks(unittest.TestCase):
    def test_fixed_failure_categories_never_expose_exception_details(self):
        for error, category in [
            (support.CommandFailure(1), "command-failed"),
            (subprocess.TimeoutExpired("secret-command", 10, output="secret-token"), "timeout"),
            (support.ResourceFailure("secret-environment"), "resource-limit"),
            (FileNotFoundError("secret-path"), "missing-file"),
            (RuntimeError("secret-password"), "validation-failed"),
        ]:
            output = io.StringIO()
            with patch("sys.stdout", output), self.assertRaises(type(error)):
                with support.smtp_stage("smtp-test-execution"):
                    raise error
            records = [json.loads(line) for line in output.getvalue().splitlines()]
            self.assertEqual(records[0], {"stage": "smtp-test-execution", "event": "start"})
            self.assertEqual(records[1]["category"], category)
            self.assertNotIn("secret", output.getvalue())
            self.assertEqual(records[1].get("exit_code"), 1 if isinstance(error, support.CommandFailure) else None)

    def test_completion_and_unknown_stage(self):
        output = io.StringIO()
        with patch("sys.stdout", output):
            with support.smtp_stage("dependency-classpath"):
                pass
            with self.assertRaises(RuntimeError):
                with support.smtp_stage("secret-stage"):
                    self.fail("Invalid stage was accepted")
        self.assertEqual([json.loads(line)["event"] for line in output.getvalue().splitlines()], ["start", "completion"])
        self.assertNotIn("secret", output.getvalue())

    def test_compiler_output_is_reduced_to_known_source_and_fixed_category(self):
        source = "backend/src/test/java/com/englishlearning/features/course/CoursePersistenceTests.java"
        raw = (f"[ERROR] /runner/repo/{source}:[410,26] cannot find symbol secret-token\n"
               "[ERROR] /private/secret.java:[1,2] secret-password\n"
               "[ERROR] backend/src/secret.java:[3,4] secret-key\n"
               "symbol: secret-value\n")
        locations = support.compiler_locations(raw)
        self.assertEqual(locations, [{"file": source, "line": 410, "column": 26, "category": "unresolved-symbol"}])
        self.assertNotIn("secret", json.dumps(locations))

    def test_compilation_failure_stops_before_classpath_or_smtp(self):
        output = io.StringIO()
        with patch.object(support, "maven", side_effect=support.CommandFailure(1)) as maven, patch("sys.stdout", output):
            with self.assertRaises(support.CommandFailure):
                support.smtp_test()
        maven.assert_called_once_with(["test-compile"])
        self.assertEqual(json.loads(output.getvalue().splitlines()[-1])["stage"], "test-compilation")
