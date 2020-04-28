import kopf
import kubernetes
import yaml

@kopf.on.create("oisp.org", "v1", "beamservices")
def create(body, spec, **kwargs):
    print("Creating beamservices")
    print("Body:", body)
    print("Spec:", spec)
    print("kwargs:", kwargs)

@kopf.on.delete("oisp.org", "v1", "beamservices")
def delete(body, spec, **kwargs):
    print("Creating beamservices")
    print("Body:", body)
    print("Spec:", spec)
    print("kwargs:", kwargs)
