# OISP Services
# NOTE: This project reached end of support and is no longer maintained. Parts of the project are continued in https://github.com/IndustryFusion/DigitalTwin.

This repository contains official OISP Services. All functionality implemented here is experimental and due to change.
**In this document, service is used in sense of a `microservice` and not a Kubernetes svc.**

## OISP Services Operator

The OISP Services Operator is the preferred way of managing services.

### Installation

OISP itself should be up and running before installing the services operator. See [platform launcher](https://github.com/Open-IoT-Service-Platform/platform-launcher) for details.

The operator assumes OISP is running in the `oisp` namespace. In the future, this will be configurable.

From the the root of this repository, run the following:
```bash
cd services-operator/kubernetes
kubectl apply -f sa.yml
kubectl apply -f binding.yml
kubectl apply -f crd.yml
kubectl apply -f operator.yml
```

You can verify the operator is running with:
```bash
kubectl get pods
```

### Deploying services

Currently, the only supported type of service is BeamService, which deploys a Beam application on the internal flink cluster.

#### BeamService
A BeamService is a `jar` file to be deployed on Flink, and is defined as follows:

example_service.yaml:
```yaml
apiVersion: oisp.org/v1
kind: BeamService
metadata:
  name: beam-service
spec:
  entryClass: "org.oisp.services.ComponentSplitter"
  args: "--runner=FlinkRunner --streaming=true"
  url: "https://arkocal.rocks/csb.jar"
```

Currently, the spec is very simple, it is likely to be extended and possibly changed. All the parameters are mandatory. The `url` field describes where the `jar` can be pulled.

Deploy the service with:
```bash
kubectl apply -f example_service.yaml
```

You can verify the service is deployed/running with the following commands:
```bash
kubectl get bs
kubectl describe bs beam-service #or
kubectl get bs beam-service -o yaml
```
