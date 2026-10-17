"""Small offline checks; no services, database connections or subprocesses."""
import importlib.util
import os
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("support", Path(__file__).with_name("run.py"))
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)


class SafetyChecks(unittest.TestCase):
    def test_requires_hosted_runner(self):
        with patch.dict(os.environ, {}, clear=True), self.assertRaises(RuntimeError):
            support.root()

    def test_exact_database_allowlist(self):
        values = {"TASK009_CONTAINER": "task009-0123456789ab", "DATABASE_USERNAME": "task007_test", "DATABASE_URL": support.URL}
        with patch.dict(os.environ, values, clear=True):
            self.assertEqual(support.container(), values["TASK009_CONTAINER"])
            for key, value in [("DATABASE_URL", support.URL + "_other"), ("DATABASE_USERNAME", "postgres"),
                               ("TASK009_CONTAINER", "existing")]:
                with patch.dict(os.environ, {key: value}), self.assertRaises(RuntimeError):
                    support.container()


if __name__ == "__main__":
    unittest.main()
