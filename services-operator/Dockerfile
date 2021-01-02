FROM python:3.8
RUN pip install kubernetes kopf==0.28.3 oisp
COPY beamservicesoperator.py /opt/beamservicesoperator.py
COPY util.py /opt/util.py
CMD kopf run --standalone /opt/beamservicesoperator.py