/*
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
 *
 */

package org.oisp.services;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.oisp.services.conf.CmdlineOptions;
import org.oisp.services.conf.Config;
import org.oisp.services.pipelines.FullPipelineBuilder;
import org.oisp.services.utils.LogHelper;
import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;


/**
 * RuleEngineBuild - creates different pipelines for Rule-engine Example
 */



public abstract class MetricsAggregator {
    private static final Logger LOG = LogHelper.getLogger(MetricsAggregator.class);
    public static void main(String[] args) {


        PipelineOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(CmdlineOptions.class);

        PipelineOptionsFactory.register(CmdlineOptions.class);
        Pipeline fullPipeline;


        //read json config from ENVIRONMENT - needed because stupid mvn cannot read JSON from cmdline. Unbelievable, but true.
        String metricsTopic = ((CmdlineOptions) options).getMetricsTopic();
        String bootstrapServers = ((CmdlineOptions) options).getBootstrapServers();
        String serviceName = ((CmdlineOptions) options).getServiceName();

        Map<String, Object> config = new HashMap<>();

        config.put(Config.KAFKA_METRICS_TOPIC, metricsTopic);
        config.put(Config.KAFKA_BOOTSTRAP_SERVERS, bootstrapServers);
        config.put(Config.SERVICE_NAME, serviceName);

        LOG.debug("Debug enabled");

        fullPipeline = FullPipelineBuilder.build(options, config);
        fullPipeline.run().waitUntilFinish();
    }
}
