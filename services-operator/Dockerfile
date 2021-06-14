FROM python:3.8
RUN pip install kubernetes kopf==1.32.1 oisp
ADD test/requirements.txt /opt/test/
RUN cd /opt/test && pip install -r requirements.txt
ADD *.py /opt/
ADD test/*.py /opt/test/
RUN PYTHONPATH=/opt python3 /opt/test/test_*.py

FROM python:3.8
RUN pip install kubernetes kopf==1.32.1 oisp
ADD *.py /opt/
WORKDIR /opt
RUN groupadd -r bsoperator && useradd -r -g bsoperator bsoperator

#RUN chown -R operator:operator /opt && chown -R operator:operator /var
USER bsoperator

CMD kopf run -A --standalone /opt/beamservicesoperator.py /opt/beamsqlstatementsetoperator.py