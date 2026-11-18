"""Sanitized Maven failure regression checks, executed by hosted TASK-009."""
import io
import json
import subprocess
import unittest
from unittest.mock import patch
import run as support


class MavenDiagnosticsTests(unittest.TestCase):
    def test_phase_categories_and_redaction(self):
        for output, phase, category in [
            ("Could not resolve dependencies SECRET", "dependency-resolution", "dependency-resolution"),
            ("Could not create the Java Virtual Machine SECRET", "jvm-startup", "jvm-startup"),
            ("Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.14.0:testCompile (default-testCompile) SECRET\nCOMPILATION ERROR", "test-compilation", "compilation-error"),
            ("Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.14.0:compile (default-compile) SECRET\nCOMPILATION ERROR", "java-compilation", "compilation-error"),
            ("Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test (default-test) SECRET\nNo tests matching pattern", "surefire", "test-discovery"),
            ("There are test failures SECRET", "unknown", "test-failures"),
            ("The forked VM terminated without properly saying goodbye SECRET", "unknown", "forked-jvm-termination"),
            ("SECRET", "unknown", "unclassified-maven-failure"),
        ]:
            with self.subTest(category=category):
                result = support.maven_diagnostics(output)
                self.assertEqual((result["phase"], result["category"]), (phase, category))
                self.assertNotIn("SECRET", json.dumps(result))

    def test_compiler_locations_reuse_repository_allowlist(self):
        output = "\x1b[31m[ERROR] src/main/java/com/englishlearning/features/auth/security/AdminManagementSecurity.java:[35,12] incompatible types SECRET\x1b[0m\n[ERROR] /private/SECRET.java:[1,1] SECRET"
        result = support.maven_diagnostics(output)
        self.assertEqual(result["recognized_diagnostics"], 1)
        self.assertEqual(result["unrecognized_error_records"], 1)
        self.assertEqual(result["locations"][0]["category"], "incompatible-types")
        self.assertEqual(result["locations"][0]["line"], 35)
        self.assertNotIn("SECRET", json.dumps(result))

    def test_command_failure_exit_code_is_preserved_without_raw_output(self):
        stream = io.StringIO()
        result = subprocess.CompletedProcess([], 7, "SECRET", "SECRET")
        with patch.object(support.subprocess, "run", return_value=result), patch("sys.stdout", stream):
            with self.assertRaises(support.CommandFailure) as raised:
                support.run(["private-command"], maven_diagnostic=True)
        self.assertEqual(raised.exception.exit_code, 7)
        self.assertEqual(json.loads(stream.getvalue())["exit_code"], 7)
        self.assertNotIn("SECRET", stream.getvalue())
        self.assertNotIn("private-command", stream.getvalue())
