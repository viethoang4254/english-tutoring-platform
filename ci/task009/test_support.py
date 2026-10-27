"""Small offline checks; no services, database connections or subprocesses."""
import importlib.util
import os
import io
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("support", Path(__file__).with_name("run.py"))
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)


class SafetyChecks(unittest.TestCase):
    def test_calendar_rejects_invalid_or_shared_owners_before_sql(self):
        for owners in ({"owner": "1;DROP", "other": "2"}, {"owner": "1", "other": "1"}):
            with patch.object(support.sys, "stdin", io.StringIO(json.dumps(owners))), patch.object(support, "sql") as sql:
                with self.assertRaises(RuntimeError):
                    support.calendar_fixture()
                sql.assert_not_called()

    def test_calendar_requires_approved_fresh_ci_owners(self):
        for answers in (["0"], ["1", "1"], ["1", "0", "0"], ["1", "0", "1", "1"]):
            with patch.object(support.sys, "stdin", io.StringIO('{"owner":"1","other":"2"}')), patch.object(support, "sql", side_effect=answers) as sql:
                with self.assertRaises(RuntimeError):
                    support.calendar_fixture()
                self.assertTrue(all("INSERT" not in call.args[0] for call in sql.call_args_list))

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
