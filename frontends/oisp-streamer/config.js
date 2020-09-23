/**
 * Copyright (c) 2020 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

var winston = require('winston'),
    os = require('os');

// Gets the config for the configName from the OISP_WEBSOCKET_STREAMER_CONFIG environment variable
// Returns empty object if the config can not be found
var getOISPConfig = (function () {
    if (!process.env.OISP_STREAMER_CONFIG) {
        console.log("Root config environment variable (OISP_STREAMER_CONFIG) is missing...");
        return function () { return {}; };
    }
    var streamerConfig = JSON.parse(process.env.OISP_STREAMER_CONFIG);

    var resolveConfig = function (config, stack) {
        if (!stack) {
            stack = ["OISP_STREAMER_CONFIG"];
        }
        for (var property in config) {
            if (typeof config[property] === "string" &&
					(config[property].substring(0,2) === "@@" || config[property].substring(0,2) === "%%")) {
                var configName = config[property].substring(2, config[property].length);
                if (!process.env[configName]) {
                    console.log("Config environment variable (" + configName + ") is missing...");
                    config[property] = {};
                } else if (stack.indexOf(configName) !== -1) {
                    console.log("Detected cyclic reference in config decleration: " + configName + ", stopping recursion...");
                    config[property] = {};
                } else {
                    config[property] = JSON.parse(process.env[configName]);
                    stack.push(configName);
                    resolveConfig(config[property], stack);
                    stack.pop();
                }
            }
        }
    };

    resolveConfig(streamerConfig);

    return function(configName) {
        if (!streamerConfig[configName])
        {return {};}
        else {
            console.log(configName + " is set to: " + JSON.stringify(streamerConfig[configName]));
            return streamerConfig[configName];
        }
    };
})();

var keycloakConfig = getOISPConfig("keycloakConfig"),
    kafkaConfig = getOISPConfig("kafkaConfig"),
    hostname = getOISPConfig("hostname"),
    port = getOISPConfig("port");

var config = {
    kafka: {
        uri: kafkaConfig.uri,
    },
    "logger": {
        format : winston.format.combine(
        	        winston.format.colorize(),
        	        winston.format.simple(),
        	        winston.format.timestamp(),
        	        winston.format.printf(info => { return `${info.timestamp}-${info.level}: ${info.message}`; })
        	     ),
        transports : [new winston.transports.Console()]
    },
    streamer: {
	hostname: hostname,
	port: port
    }
};

module.exports = config;
