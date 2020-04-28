import kopf
import kubernetes
import requests
import uuid

@kopf.on.create("oisp.org", "v1", "beamservices")
def create(body, spec, **kwargs):
    kopf.info(body, reason="SomeReason", message="Creating beamservices")
    deploy(body, spec)

@kopf.on.delete("oisp.org", "v1", "beamservices")
def delete(body, spec, **kwargs):
    kopf.info(body, reason="SomeReason", message="Deleting beamservices")
    kopf.info(body, reason="SomeReason", message=f"Body: {body}")
    kopf.info(body, reason="SomeReason", message=f"Spec: {spec}")
    kopf.info(body, reason="SomeReason", message=f"kwargs: {kwargs}")



FLINK_URL = "http://flink-jobmanager-rest.oisp:8081"

def download_file(url):
    """Download the file and return the saved path."""
    response = requests.get(url)
    path = "/tmp/" + str(uuid.uuid4()) + ".jar"
    with open(path, "wb") as f:
        f.write(response.content)
    return path

def deploy(body, spec):
    # TODO Create schema for spec in CRD
    entry_class = spec["entryClass"]
    args = spec["args"]
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
    response = requests.post(f"{FLINK_URL}/jars/{jar_id}/run",
                             json={"entryClass": entry_class,
                                   "programArgs": args})
    if response.status_code != 200:
        kopf.error(body, reason="BeamExecutionFailed",
                   message="Could not run job, server returned:\n" +
                   response.content.decode("utf-8"))
        raise kopf.TemporaryError("Jar execution failed.", delay=60)
