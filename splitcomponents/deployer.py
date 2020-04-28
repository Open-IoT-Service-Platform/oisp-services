import os
from sys import stderr, argv
import time
import base64
import json

import requests
import oisp


URL = "http://flink-jobmanager-rest:8081"
ENTRY_CLASS = "org.oisp.services.ComponentSplitter"

ARGS = ("""--runner=FlinkRunner --streaming=true """)
#        """--JSONConfig={} """).format(CONFIG_SERIALIZED)

response = requests.post(f"{URL}/jars/upload", files={"jarfile": open(argv[1], "rb")})

if response.status_code != 200:
    print("Could not submit jar, server returned:\n",
          response.request.body.decode("utf-8"), file=stderr)
    exit(1)

jar_id = response.json()["filename"].split("/")[-1]

print(f"Submitted jar with id: {jar_id}")
response = requests.post(f"{URL}/jars/{jar_id}/run",
                         json={"entryClass": ENTRY_CLASS,
                               "programArgs": ARGS})
if response.status_code != 200:
    print("Could not run job, server returned:\n",
          response.content.decode("utf-8"), file=stderr)
    exit(1)
