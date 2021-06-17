"""Helpers for communicating with Flink"""

import os
import requests

namespace = os.getenv("OISP_NAMESPACE", default="oisp")
FLINK_URL = os.getenv("OISP_FLINK_REST",
                      default=f"http://flink-jobmanager-rest.{namespace}:8081")


class CancelJobFailedException(Exception):
    """Exception for a failed SQL Job cancellation"""


def get_job_status(logger, job_id):
    """Get job status as json dict as returned by Flink"""
    logger.debug(f"Requestion status for {job_id} from flink job-manager")
    job_request = requests.get(
        f"{FLINK_URL}/jobs/{job_id}").json()
    logger.debug(f"Received job status: {job_request}")
    return job_request


def cancel_job(logger, job_id):
    """Cancel job with given id."""
    logger.debug(
        f"Requesting cancelation of job {job_id} from flink job-manager")
    response = requests.patch(f"{FLINK_URL}/jobs/{job_id}")
    if response.status_code != 202:
        raise CancelJobFailedException("Could not cancel job {job_id}")
