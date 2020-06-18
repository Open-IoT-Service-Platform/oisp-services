import kopf
import kubernetes
import requests
import uuid

import time

FLINK_URL = "http://flink-jobmanager-rest.oisp:8081"

@kopf.on.create("oisp.org", "v1", "beamservices")
def create(body, spec, **kwargs):
    kopf.info(body, reason="Creating", message="Creating beamservices")
    return {"createdOn": str(time.time())}

# TODO make this async
@kopf.timer("oisp.org", "v1", "beamservices", interval=1)
def updates(stopped, patch, logger, body, spec, status, **kwargs):
    update_status = status.get("updates")
    if update_status is None:
        return {"deployed": False, "jobCreated": False, "jobStatus": {}}
    if not update_status.get("deployed"):
        jar_id = deploy(body, spec)
        return {"deployed": True, "jarId": jar_id}
    elif not update_status.get("jobCreated"):
        job_id = create_job(body, spec, update_status["jarId"])
        if job_id is not None:
            return {"jobCreated": True, "jobId": job_id}
        else:
            return
    job_status = requests.get(f"{FLINK_URL}/jobs/{update_status['jobId']}").json()
    return {"jobStatus": job_status}

@kopf.on.delete("oisp.org", "v1", "beamservices")
def delete(body, spec, **kwargs):
    update_status = body["status"].get("updates")
    if not update_status:
        return
    if update_status.get("jobId"):
        resp = requests.patch(f"{FLINK_URL}/jobs/{update_status['jobId']}", params={"mode": "cancel"})


def download_file(url):
    """Download the file and return the saved path."""
    response = requests.get(url)
    path = "/tmp/" + str(uuid.uuid4()) + ".jar"
    with open(path, "wb") as f:
        f.write(response.content)
    return path

def deploy(body, spec):
    # TODO Create schema for spec in CRD
    url = spec["url"]
    kopf.info(body, reason="Jar download started",
              message=f"Downloadin from {url}")
    jarfile_path = download_file(url)
    response = requests.post(f"{FLINK_URL}/jars/upload", files={"jarfile": open(jarfile_path, "rb")})
    if response.status_code != 200:
        kopf.error(body, reason="BeamDeploymentFailed",
                   message="Could not submit jar, server returned:"+
                   response.request.body.decode("utf-8"))
        raise kopf.TemporaryError("Jar submission failed.", delay=10)

    jar_id = response.json()["filename"].split("/")[-1]

    kopf.info(body, reason="BeamDeploymentSuccess",
              message=f"Submitted jar with id: {jar_id}")
    return jar_id

def create_job(body, spec, jar_id):
    entry_class = spec["entryClass"]
    args = spec["args"]
    response = requests.post(f"{FLINK_URL}/jars/{jar_id}/run",
                             json={"entryClass": entry_class,
                                   "programArgs": args})
    if response.status_code != 200:
        kopf.error(body, reason="BeamExecutionFailed",
                   message="Could not run job, server returned:\n" +
                   response.content.decode("utf-8"))
        return None
    job_id = response.json().get("jobid")
    kopf.info(body, reason="Job created", message=f"Job id: {job_id}")
    return job_id
