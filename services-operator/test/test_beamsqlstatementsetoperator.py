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

    def warning(self, message):
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


class TestMonitoring(TestCase):
    def create_ddl_from_beamsqltables(beamsqltable, logger):
        return "DDL;"

    def submit_statementset_successful(statementset, logger):
        # Keeping normal assert statement as this does not seem to
        # be an object method after mocking
        assert statementset == "SET pipeline.name = 'namespace/name';\nDDL;" \
            "\nBEGIN STATEMENT SET;\nselect;\nEND;"
        return "job_id"

    def submit_statementset_failed(statementset, logger):
        raise target.DeploymentFailedException("Mock submission failed")

    def update_status_not_found(body, patch, logger):
        patch.status["state"] = "NOT_FOUND"

    @patch('beamsqlstatementsetoperator.tables_and_views.create_ddl_from_beamsqltables',
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
        target.monitor(beamsqltables, None, patch,  Logger(),
                       body, body["spec"], body["status"])
        self.assertEqual(patch.status['state'], "DEPLOYING")
        self.assertEqual(patch.status['job_id'], "job_id")

    @patch('beamsqlstatementsetoperator.tables_and_views.create_ddl_from_beamsqltables',
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
            target.monitor(beamsqltables, None, patch,  Logger(),
                           body, body["spec"], body["status"])
        except kopf.TemporaryError:
            pass
        self.assertEqual(patch.status['state'], "DEPLOYMENT_FAILURE")
        self.assertIsNone(patch.status['job_id'])

    @patch('beamsqlstatementsetoperator.tables_and_views.create_ddl_from_beamsqltables',
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
            target.monitor(beamsqltables, None, patch, Logger(),
                           body, body["spec"], body["status"])
            self.assertTrue(str(cm.exception).startswith(
                "Table DDLs could not be created for namespace/name."))

    @patch('beamsqlstatementsetoperator.tables_and_views.create_ddl_from_beamsqltables',
           create_ddl_from_beamsqltables)
    @patch('beamsqlstatementsetoperator.deploy_statementset',
           submit_statementset_failed)
    @patch('beamsqlstatementsetoperator.refresh_state', update_status_not_found)
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

        target.monitor(beamsqltables, None, patch, Logger(),
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
        raise kopf.TemporaryError("Could not cancel job")

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

job_canceled = False

class TestUpdate(TestCase):
    def cancel_job(logger, job_id):
        global job_canceled
        job_canceled = True
        pass
    def get_job_status(logger, job_id):
        return {
            "state": "RUNNING"
        }
    
    def get_job_status_not_running(logger, job_id):
        return {
            "state": "UNKNOWN"
        }

    def cancel_job_and_get_state(logger, body, patch):
        pass

    
    def stop_job(logger, job_id, savepoint_dir):
        return "savepoint_id"


    def get_savepoint_state_successful(logger, job_id, savepoint_id):
        return {
            "status": "SUCCESSFUL",
            "location": "location"
        }


    def get_savepoint_state_in_progress(logger, job_id, savepoint_id):
        return {
            "status": "IN_PROGRESS",
            "location": "location"
        }


    def get_savepoint_state_not_found(logger, job_id, savepoint_id):
        return {
            "status": "NOT_FOUND",
            "location": "location"
        }

    def add_message(logger, body, patch, reason, mtype):
        pass

    @patch('kopf.info', kopf_info)
    @patch('flink_util.cancel_job', cancel_job)
    @patch('flink_util.get_job_status', get_job_status)
    def test_cancel_job_and_get_state_running(self):
        global job_canceled
        body = {           
            "status": {
                "job_id": "job_id"
            } 
        }
        job_canceled = False
        job_state = target.cancel_job_and_get_state(Logger(), body, None)
        self.assertEqual("RUNNING", job_state)
        self.assertEqual(True, job_canceled)


    @patch('kopf.info', kopf_info)
    @patch('flink_util.cancel_job', cancel_job)
    @patch('flink_util.get_job_status', get_job_status_not_running)
    def test_cancel_job_and_get_state_running(self):
        global job_canceled
        body = {           
            "status": {
                "job_id": "job_id"
            } 
        }
        job_canceled = False
        job_state = target.cancel_job_and_get_state(Logger(), body, None)
        self.assertEqual("UNKNOWN", job_state)
        self.assertEqual(False, job_canceled)


    @patch('kopf.info', kopf_info)
    @patch('beamsqlstatementsetoperator.cancel_job_and_get_state', cancel_job_and_get_state)
    def test_update_no_savepoint(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
            },
            "status": {
                "job_id": "job_id",
                "state": "RUNNING",
            } 
        }
        patch = Bunch()
        patch.status = {}
        target.update(body, body["spec"], patch, Logger())
        self.assertEqual(patch.status["state"], "UPDATING")
        self.assertIsNone(patch.status["savepoint_id"])
        self.assertIsNone(patch.status["location"])


    @patch('kopf.info', kopf_info)
    @patch('beamsqlstatementsetoperator.cancel_job_and_get_state', cancel_job_and_get_state)
    def test_update_none_savepoint(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "updateStrategy": None
            },
            "status": {
                "job_id": "job_id",
                "state": "RUNNING",
            } 
        }
        patch = Bunch()
        patch.status = {}
        target.update(body, body["spec"], patch, Logger())
        self.assertEqual(patch.status["state"], "UPDATING")
        self.assertIsNone(patch.status["savepoint_id"])
        self.assertIsNone(patch.status["location"])


    @patch('kopf.info', kopf_info)
    @patch('flink_util.stop_job', stop_job)
    @patch('flink_util.get_savepoint_state', get_savepoint_state_successful)
    @patch('beamsqlstatementsetoperator.add_message', add_message)
    def test_update_savepoint_successful(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "updateStrategy": "savepoint"
            },
            "status": {
                "job_id": "job_id",
                "state": "RUNNING",
            } 
        }
        patch = Bunch()
        patch.status = {}
        target.update(body, body["spec"], patch, Logger())
        self.assertEqual(patch.status["savepoint_id"], "savepoint_id")
        self.assertEqual(patch.status["state"], "UPDATING")
        self.assertEqual(patch.status["location"], "location")


    @patch('kopf.info', kopf_info)
    @patch('flink_util.stop_job', stop_job)
    @patch('flink_util.get_savepoint_state', get_savepoint_state_in_progress)
    @patch('beamsqlstatementsetoperator.add_message', add_message)
    def test_update_savepoint_in_progress(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "updateStrategy": "savepoint"
            },
            "status": {
                "job_id": "job_id",
                "state": "RUNNING",
            } 
        }
        patch = Bunch()
        patch.status = {}
        with self.assertRaises(kopf.TemporaryError) as cm:
            target.update(body, body["spec"], patch, Logger())
        self.assertEqual(patch.status["savepoint_id"], "savepoint_id")
        self.assertEqual(patch.status["state"], "SAVEPOINTING")


    @patch('kopf.info', kopf_info)
    @patch('flink_util.stop_job', stop_job)
    @patch('flink_util.get_savepoint_state', get_savepoint_state_not_found)
    @patch('beamsqlstatementsetoperator.add_message', add_message)
    def test_update_savepoint_not_found(self):
        body = {
            "metadata": {
                "name": "name",
                "namespace": "namespace"
            },
            "spec": {
                "updateStrategy": "savepoint"
            },
            "status": {
                "job_id": "job_id",
                "state": "RUNNING",
            } 
        }
        patch = Bunch()
        patch.status = {}
        target.update(body, body["spec"], patch, Logger())
        self.assertIsNone(patch.status["savepoint_id"])
        self.assertEqual(patch.status["state"], "RUNNING")
        self.assertIsNone(patch.status["location"])

if __name__ == '__main__':
    unittest.main()
