FROM python:3.8
RUN pip install kopf kubernetes
COPY beamservicesoperator.py /opt/beamservicesoperator.py
CMD kopf run --standalone /opt/beamservicesoperator.py