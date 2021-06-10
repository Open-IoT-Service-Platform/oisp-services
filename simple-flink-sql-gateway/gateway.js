"use strict";
const express = require('express');
const { exec } = require("child_process");
const uuid = require("uuid");
const fs = require('fs');
const app = express();
const logger = require("./lib/logger.js");
const port = process.env.SIMPLE_FLINK_SQL_GATEWAY_PORT || 9000;
const flink_root = process.env.SIMPLE_FLINK_SQL_GATEWAY_ROOT || "./flink-1.13.0";
const sql_jars = process.env.SIMPLE_FLINK_SQL_GATEWAY_JARS || './jars';


app.use(express.json());

app.get('/health', function(_, response) {
  response.status(200).send("OK");
  logger.debug("Health Endpoint was requested.");
});

app.post('/v1/sessions/:session_id/statements', function (request, response) {
  //for now ignore session_id
  var body = request.body;
  if (body.statement === undefined) {
    response.status(500);
    response.send("Wrong format! No statement field in body");
    return;
  }
  var id = uuid.v4();
  var filename = "/tmp/script_" + id + ".sql";
  fs.writeFileSync(filename, body.statement.toString());
  var command = flink_root + "/bin/sql-client.sh -l " + sql_jars + " -f " + filename;
  logger.debug("Now executing " + command);
  exec(command, (error, stdout, stderr) => {
    fs.unlinkSync(filename);
    if (error) {
      response.status(500);
      response.send("Error while executing sql-client: " + error);
      return;
    }
    //find Job ID ind stdout, e.g.
    //Job ID: e1ebb6b314c82b27cf81cbc812300d97
    var regexp = /Job ID: ([0-9a-f]*)/i;
    var found = stdout.match(regexp);
    logger.debug("Server output: " + stdout);
    if (found !== null && found !== undefined) {
      var jobId = found[1];
      logger.debug("jobId found:" + jobId);
      response.status(200).send('{ "jobid": "' + jobId + '" }');
    } else { // no JOB ID found, unsuccessful
      response.status(500);
      response.send("Not successfully submitted. No JOB ID found in server reply.");
      return;
    }
  });
});



app.listen(port, function () {
  console.log('Listening on port ' + port);
});