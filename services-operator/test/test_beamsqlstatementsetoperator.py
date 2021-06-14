from unittest import TestCase
import unittest
from bunch import Bunch
from mock import patch, MagicMock
import kopf
from kopf.testing import KopfRunner

import beamsqlstatementsetoperator as target


"""
Mock functions
"""


class Logger():
    def info(self, message):
        pass

    def debug(self, message):
        pass

    def error(self, message):
        pass

    def warn(self, message):
        pass


def kopf_info(body, reason, message):
    pass


class TestInit(TestCase):
    @patch('kopf.info', kopf_info)
    def test_init(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {}
        }
        patch = Bunch()
        patch.status = {}
        result = target.create(body, body["spec"], patch, Logger())
        self.assertIsNotNone(result['createdOn'])
        self.assertEqual(patch.status['state'], "INITIALIZED")
        self.assertIsNone(patch.status['job_id'])


class TestUpdates(TestCase):
    def create_ddl_from_beamsqltables(body, beamsqltable, logger):
        return "DDL;"

    def submit_statementset_successful(statementset, logger):
        # Keeping normal assert statement as this does not seem to
        # be an object method after mocking
        assert statementset == "SET pipeline.name = 'namespace/name';\nDDL;" \
            "\nBEGIN STATEMENT SET;\nselect;\nEND;"
        return "job_id"

    def submit_statementset_failed(statementset, logger):
        raise target.DeploymentFailedException("Mock submission failed")

    def update_status_unknown(body, patch, logger):
        patch.status["state"] = "UNKNOWN"

    @patch('beamsqlstatementsetoperator.create_ddl_from_beamsqltables',
           create_ddl_from_beamsqltables)
    @patch('beamsqlstatementsetoperator.deploy_statementset',
           submit_statementset_successful)
    def test_update_submission(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "INITIALIZED",
                "job_id": None
            }
        }

        patch = Bunch()
        patch.status = {}

        beamsqltables = {("namespace", "table"): ({}, {})}
        target.updates(beamsqltables, None, patch,  Logger(),
                       body, body["spec"], body["status"])
        self.assertEqual(patch.status['state'], "DEPLOYING")
        self.assertEqual(patch.status['job_id'], "job_id")

    @patch('beamsqlstatementsetoperator.create_ddl_from_beamsqltables',
           create_ddl_from_beamsqltables)
    @patch('beamsqlstatementsetoperator.deploy_statementset',
           submit_statementset_failed)
    def test_update_submission_failure(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "INITIALIZED",
                "job_id": None
            }
        }

        patch = Bunch()
        patch.status = {}

        beamsqltables = {("namespace", "table"): ({}, {})}
        try:
            target.updates(beamsqltables, None, patch,  Logger(),
                           body, body["spec"], body["status"])
        except kopf.TemporaryError:
            pass
        self.assertEqual(patch.status['state'], "DEPLOYMENT_FAILURE")
        self.assertIsNone(patch.status['job_id'])

    @patch('beamsqlstatementsetoperator.create_ddl_from_beamsqltables',
           create_ddl_from_beamsqltables)
    @patch('beamsqlstatementsetoperator.deploy_statementset',
           submit_statementset_failed)
    def test_update_table_failure(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "INITIALIZED",
                "job_id": None
            }
        }

        patch = Bunch()
        patch.status = {}

        beamsqltables = {}
        with self.assertRaises(kopf.TemporaryError) as cm:
            target.updates(beamsqltables, None, patch, Logger(),
                           body, body["spec"], body["status"])
            self.assertTrue(str(cm.exception).startswith(
                "Table DDLs could not be created for namespace/name."))

    @patch('beamsqlstatementsetoperator.create_ddl_from_beamsqltables',
           create_ddl_from_beamsqltables)
    @patch('beamsqlstatementsetoperator.deploy_statementset',
           submit_statementset_failed)
    @patch('beamsqlstatementsetoperator.refresh_state', update_status_unknown)
    def test_update_handle_unknown(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "RUNNING",
                "job_id": "job_id"
            }
        }

        patch = Bunch()
        patch.status = {}

        beamsqltables = {}

        target.updates(beamsqltables, None, patch, Logger(),
                       body, body["spec"], body["status"])
        self.assertEqual(patch.status["state"], "INITIALIZED")
        self.assertIsNone(patch.status["job_id"])


class TestDeletion(TestCase):
    def update_status_nochange(body, patch, logger):
        pass

    def update_status_change(body, patch, logger):
        patch.status["state"] = "CANCELED"

    def cancel_job(logger, job_id):
        pass

    def cancel_job_error(logger, job_id):
        raise Exception("Could not cancel job")

    @patch('kopf.info', kopf_info)
    def test_canceled_delete(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "CANCELED",
                "job_id": "job_id"
            }
        }
        patch = Bunch()
        patch.status = {}

        target.delete(body, body["spec"], patch, Logger())
        self.assertEqual(patch.status, {})

    @patch('kopf.info', kopf_info)
    @patch('beamsqlstatementsetoperator.refresh_state', update_status_nochange)
    def test_canceling_delete(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "CANCELING",
                "job_id": "job_id"
            }
        }
        patch = Bunch()
        patch.status = {"state": ""}

        with self.assertRaises(kopf.TemporaryError) as cm:
            target.delete(body, body["spec"], patch, Logger())
            self.assertTrue(str(cm.exception).startswith("Cancelling,"))

    @patch('kopf.info', kopf_info)
    @patch('beamsqlstatementsetoperator.refresh_state', update_status_change)
    def test_canceling_delete(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "CANCELING",
                "job_id": "job_id"
            }
        }
        patch = Bunch()
        patch.status = {"state": ""}

        target.delete(body, body["spec"], patch, Logger())

    @patch('flink_util.cancel_job', cancel_job)
    @patch('beamsqlstatementsetoperator.refresh_state', update_status_nochange)
    @patch('kopf.info', kopf_info)
    def test_canceling_delete_flink_ok(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "RUNNING",
                "job_id": "job_id"
            }
        }
        patch = Bunch()
        patch.status = {"state": ""}

        with self.assertRaises(kopf.TemporaryError) as cm:
            target.delete(body, body["spec"], patch, Logger())
            self.assertTrue(str(cm.exception).startswith("Waiting for"))
        self.assertEqual(patch.status["state"], "CANCELING")

    @patch('flink_util.cancel_job', cancel_job_error)
    @patch('kopf.info', kopf_info)
    def test_canceling_delete_flink_error(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "sqlstatements": ["select;"],
                "tables": ["table"]
            },
            "status": {
                "state": "RUNNING",
                "job_id": "job_id"
            }
        }
        patch = Bunch()
        patch.status = {"state": ""}

        with self.assertRaises(kopf.TemporaryError) as cm:
            target.delete(body, body["spec"], patch, Logger())
            self.assertTrue(str(cm.exception).startswith("Error trying"))
        self.assertNotEqual(patch.status["state"], "CANCELING")


if __name__ == '__main__':
    unittest.main()
