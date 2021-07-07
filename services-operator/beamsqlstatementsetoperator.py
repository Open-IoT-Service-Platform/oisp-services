"""A kopf operator that manages SQL jobs on flink."""

import time
import os
from enum import Enum

import requests
import kopf

import flink_util

oisp_namespace = os.getenv("OISP_NAMESPACE", default="oisp")
FLINK_URL = os.getenv("OISP_FLINK_REST",
                      default="http://flink-jobmanager-rest"
                      f".{oisp_namespace}:8081")

default_gateway_url = f"http://flink-sql-gateway.{oisp_namespace}:9000"
FLINK_SQL_GATEWAY = os.getenv("OISP_FLINK_SQL_GATEWAY",
                              default=default_gateway_url)
timer_interval_seconds = int(os.getenv("TIMER_INTERVAL", default="10"))
timer_backoff_seconds = int(os.getenv("TIMER_BACKOFF_INTERVAL", default="10"))
timer_backoff_temporary_failure_seconds = int(
    os.getenv("TIMER_BACKOFF_TEMPORARY_FAILURE_INTERVAL", default="30"))


class States(Enum):
    """SQL Job states as defined by Flink"""
    INITIALIZED = "INITIALIZED"
    DEPLOYING = "DEPLOYING"
    DEPLOYMENT_FAILURE = "DEPLOYMENT_FAILURE"
    RUNNING = "RUNNING"
    FAILED = "FAILED"
    CANCELED = "CANCELED"
    CANCELING = "CANCELING"
    UNKNOWN = "UNKNOWN"


class DeploymentFailedException(Exception):
    """Deployment failed exception."""


JOB_ID = "job_id"
STATE = "state"


@kopf.on.create("oisp.org", "v1alpha1", "beamsqlstatementsets")
# pylint: disable=unused-argument
# Kopf decorated functions match their expectations
def create(body, spec, patch, logger, **kwargs):
    """Handle k8s create event."""
    name = body["metadata"].get("name")
    namespace = body["metadata"].get("namespace")
    kopf.info(body, reason="Creating",
              message=f"Creating beamsqlstatementsets {name}"
              f"in namespace {namespace}")
    logger.info(
        f"Created beamsqlstatementsets {name} in namespace {namespace}")
    patch.status[STATE] = States.INITIALIZED.name
    patch.status[JOB_ID] = None
    return {"createdOn": str(time.time())}


@kopf.on.delete("oisp.org", "v1alpha1", "beamsqlstatementsets", retries=10)
# pylint: disable=unused-argument
# Kopf decorated functions match their expectations
def delete(body, spec, patch, logger, **kwargs):
    """
    Deleting beamsqlstatementsets

    If state is not CANCELING and not CANCELED and job_id defined,
    refresh status and trigger cancelation if needed
    If Canceling, refresh state, when canceled, allow deletion,otherwise wait
    """
    name = body["metadata"].get("name")
    namespace = body["metadata"].get("namespace")

    state = body['status'].get(STATE)
    job_id = body['status'].get(JOB_ID)
    if job_id and state not in [States.CANCELED.name, States.CANCELING.name]:
        try:
            refresh_state(body, patch, logger)
            if patch.status[STATE]:
                state = patch.status[STATE]
            if state in [States.CANCELING.name, States.CANCELED.name]:
                flink_util.cancel_job(logger, job_id)
        except (KeyError, flink_util.CancelJobFailedException,
                kopf.TemporaryError) as err:
            raise kopf.TemporaryError(
                f"Error trying to cancel {namespace}/{name}"
                + f"with message {err}. Trying again later", 10)
        # cancelation went through, delete job_id and update state
        patch.status[JOB_ID] = None
        patch.status[STATE] = States.CANCELING.name
        raise kopf.TemporaryError(
            f"Waiting for confirmation of cancelation for {namespace}/{name}",
            5)

    if state == States.CANCELING.name:
        refresh_state(body, patch, logger)
        if not patch.status[STATE] == States.CANCELED.name:
            raise kopf.TemporaryError(
                "Canceling, waiting for final confirmation of cancelation"
                f"for {namespace}/{name}", 5)
    kopf.info(body, reason="deleting",
              message=f" {namespace}/{name} cancelled and ready for deletion")
    logger.info(f" {namespace}/{name} cancelled and ready for deletion")


@kopf.index('oisp.org', "v1alpha1", "beamsqltables")
# pylint: disable=missing-function-docstring
def beamsqltables(name: str, namespace: str, body: kopf.Body, **_):
    return {(namespace, name): body}


@kopf.timer("oisp.org", "v1alpha1", "beamsqlstatementsets",
            interval=timer_interval_seconds, backoff=timer_backoff_seconds)
# pylint: disable=too-many-arguments unused-argument redefined-outer-name
# Kopf decorated functions match their expectations
def updates(beamsqltables: kopf.Index, stopped, patch, logger, body, spec,
            status, **kwargs):
    """
    Managaging the main lifecycle of the beamsqlstatementset crd
    Current state is stored under
    status:
        state: STATE
        job_id: string
    STATE can be
        - INITIALIZED - resource is ready to deploy job. job_id: None
        - DEPLOYING - resource has been deployed, job_id: flink_id
        - RUNNING - resource is running job_id: flink_id
        - FAILED - resource is in failed state
        - DEPLOYMENT_FAILURE - something went wrong while operator tried to
          deploy (e.g. server returned 500)
        - CANCELED - resource has been canceled
        - CANCELING - resource is in cancelling process
        - UNKNOWN - resource cannot be monitored

    Transitions:
        - undefined/INITIALIZED => DEPLOYING
        - INITIALIZED => DEPLOYMENT_FAILURE
        - DEPLOYMENT_FAILURE => DEPLOYING
        - DEPLOYING/UNKNOWN =>FLINK_STATES(RUNNING/FAILED/CANCELLED/CANCELLING)
        - delete resource => CANCELLING
        - delete done => CANCELLED
    Currently, cancelled state is not recovered
    """
    namespace = body['metadata'].get("namespace")
    name = body['metadata'].get("name")
    state = body['status'].get(STATE)
    logger.debug(
        f"Triggered updates for {namespace}/{name} with state {state}")

    if state in [States.INITIALIZED.name, States.DEPLOYMENT_FAILURE.name]:
        # deploying
        logger.debug(f"Deyploying {namespace}/{name}")

        # get first all table ddls
        # get inputTable and outputTable
        ddls = f"SET pipeline.name = '{namespace}/{name}';\n"
        try:
            ddls += "\n".join(create_ddl_from_beamsqltables(
                body,
                list(beamsqltables[(namespace, table_name)])[0],
                logger) for table_name in spec.get("tables")) + "\n"

        except (KeyError, TypeError) as exc:
            logger.error("Table DDLs could not be created for "
                         f"{namespace}/{name}."
                         "Check the table definitions and references.")
            raise kopf.TemporaryError("Table DDLs could not be created for "
                                      f"{namespace}/{name}. Check the table"
                                      "definitions and references: "f"{exc}",
                                      timer_backoff_temporary_failure_seconds)\
                from exc

        # now create statement set
        statementset = ddls
        statementset += "BEGIN STATEMENT SET;\n"
        statementset += "\n".join(spec.get("sqlstatements")) + "\n"
        # TODO: check for "insert into" prefix and terminating ";"
        # in sqlstatement
        statementset += "END;"
        logger.debug(f"Now deploying statementset {statementset}")
        try:
            patch.status[JOB_ID] = deploy_statementset(statementset, logger)
            patch.status[STATE] = States.DEPLOYING.name
            return
        except DeploymentFailedException as err:
            # Temporary error as we do not know the reason
            patch.status[STATE] = States.DEPLOYMENT_FAILURE.name
            patch.status[JOB_ID] = None
            logger.error(f"Could not deploy statementset {err}")
            raise kopf.TemporaryError(
                f"Could not deploy statement: {err}",
                timer_backoff_temporary_failure_seconds)

    # If state is not INITIALIZED, DEPLOYMENT_FAILURE nor CANCELED,
    # the state is monitored
    if state not in [States.CANCELED.name, States.CANCELING.name]:
        refresh_state(body, patch, logger)
        if patch.status[STATE] == States.UNKNOWN.name:
            patch.status[STATE] = States.INITIALIZED.name
            patch.status[JOB_ID] = None


def refresh_state(body, patch, logger):
    """Refrest patch.status.state"""
    job_id = body['status'].get(JOB_ID)
    job_info = None
    try:
        job_info = flink_util.get_job_status(logger, job_id)
    except requests.HTTPError as exc:
        pass
    except requests.exceptions.RequestException as exc:
        patch.status[STATE] = States.UNKNOWN.name
        raise kopf.TemporaryError(
            f"Could not monitor task {job_id}: {exc}",
            timer_backoff_temporary_failure_seconds) from exc
    if job_info is not None:
        patch.status[STATE] = job_info.get("state")
    else:
        # API etc works but no job found. Can happen for instance
        # after restart of job manager
        # In this case, we need to signal that stage
        patch.status[STATE] = States.UNKNOWN.name


def create_ddl_from_beamsqltables(body, beamsqltable, logger):
    """
    creates an sql ddl object out of a beamsqltables object
    Works only with kafka connector
    column names (fields) are not expected to be escaped with ``.
    This is inserted explicitly.
    The value of the fields are supposed to be prepared to pass SQL parsing
    value: STRING # will be translated into `value` STRING,
    value is an SQL keyword (!)
    dvalue: AS CAST(`value` AS DOUBLE) # will b translated into `dvalue`
        AS CAST(`value` AS DOUBLE), `value` here is epected to be masqued

    Parameters
    ----------
    body: dict
        Beamsqlstatementset which is currently processed
    beamsqltable: dict
        Beamsqltable object
    logger: log object
        Local log object provided from framework
    """
    name = beamsqltable.metadata.name
    ddl = f"CREATE TABLE `{name}` ("
    ddl += ",".join(f"{k} {v}" if k == "watermark" else f"`{k}` {v}"
                    for k, v in beamsqltable.spec.get("fields").items())
    ddl += ") WITH ("
    if beamsqltable.spec.get("connector") != "kafka":
        message = f"Beamsqltable {name} has not supported connector."
        kopf.warn(body, reason="invalid CRD", message=message)
        logger.warn(message)
        return None
    ddl += "'connector' = 'kafka'"
    # loop through the value structure
    # value.format is mandatory
    value = beamsqltable.spec.get("value")
    if not value:
        message = f"Beamsqltable {name} has no value description."
        kopf.warn(body, reason="invalid CRD", message=message)
        logger.warn(message)
        return None
    if not value.get("format"):
        message = f"Beamsqltable {name} has no value.format description."
        kopf.warn(body, reason="invalid CRD", message=message)
        logger.warn(message)

    ddl += "," + ",".join(f"'{k}' = '{v}'" for k, v in value.items())
    # loop through the kafka structure
    # map all key value pairs to 'key' = 'value',
    # except properties
    kafka = beamsqltable.spec.get("kafka")
    if not kafka:
        message = f"Beamsqltable {name} has no Kafka connector descriptor."
        kopf.warn(body, reason="invalid CRD", message=message)
        logger.warn(message)
        return None
    # check mandatory fields in Kafka, topic, bootstrap.server
    if not kafka.get("topic"):
        message = f"Beamsqltable {name} has no kafka topic."
        kopf.warn(body, reason="invalid CRD", message=message)
        logger.warn(message)
        return None

    try:
        _ = kafka["properties"]["bootstrap.servers"]
    except KeyError:
        message = f"Beamsqltable {name} has no kafka bootstrap servers found"
        kopf.warn(body, reason="invalid CRD", message=message)
        logger.warn(message)
        return None
    # the other fields are inserted, there is not a check for valid fields yet
    for kafka_key, kafka_value in kafka.items():
        # properties are iterated separately
        if kafka_key == 'properties':
            for property_key, property_value in kafka_value.items():
                ddl += f",'properties.{property_key}' = '{property_value}'"
        else:
            ddl += f", '{kafka_key}' = '{kafka_value}'"
    ddl += ");"
    logger.debug(f"Created table ddl for table {name}: {ddl}")
    return ddl


def deploy_statementset(statementset, logger):
    """
    deploy statementset to flink SQL gateway

    A statementset is deployed to the flink sql gateway.
    It is expected that all statements in a set are inserting
    the result into a table.
    The table schemas are defined at the beginning

    Parameter
    ---------
    statementset: string
        The fully defined statementset including table ddl
    logger: obj
        logger obj
    returns: jobid: string or exception if deployment was not successful
        id of the deployed job

    """
    request = f"{FLINK_SQL_GATEWAY}/v1/sessions/session/statements"
    logger.debug(f"Deployment request to SQL Gateway {request}")
    try:
        response = requests.post(request,
                                 json={"statement": statementset})
    except requests.RequestException as err:
        raise DeploymentFailedException(
            f"Could not deploy job to {request}, server unreachable ({err})")\
            from err
    if response.status_code != 200:
        raise DeploymentFailedException(
            f"Could not deploy job to {request}, server returned:"
            f"{response.status_code}")
    logger.debug(f"Response: {response.json()}")
    job_id = response.json().get("jobid")
    return job_id
