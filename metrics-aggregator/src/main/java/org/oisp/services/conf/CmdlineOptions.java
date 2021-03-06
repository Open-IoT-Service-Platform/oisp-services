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
package org.oisp.services.conf;

import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;

public interface CmdlineOptions extends PipelineOptions {
    @Description("Kafka topic for metrics")
    @Default.String("")
    String getMetricsTopic();
    void setMetricsTopic(String value);

    @Description("Kafka Bootstrap Servers")
    @Default.String("")
    String getBootstrapServers();
    void setBootstrapServers(String value);

    @Description("Service Name")
    @Default.String("aggregator")
    String getServiceName();
    void setServiceName(String value);
}
