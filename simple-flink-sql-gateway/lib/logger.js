'use strict';

const {format, createLogger, transports} = require('winston');
const { combine, errors, timestamp} = format;

module.exports = createLogger ({
    level: "debug",
    format: combine(
      format.json(),
      errors({ stack: true }),
      timestamp()),
    transports: [
      new transports.Console({format: format.simple()})
    ]
  });