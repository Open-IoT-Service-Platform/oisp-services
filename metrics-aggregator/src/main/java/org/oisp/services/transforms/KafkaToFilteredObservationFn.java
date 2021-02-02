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
 
package org.oisp.services.transforms;


import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.oisp.services.collections.AggregatedObservation;
import org.oisp.services.collections.ObservationList;
import org.oisp.services.conf.Config;
import org.oisp.services.dataStructures.Aggregator;

import java.util.Map;

// Distribute elements with cid key
// Filter out already aggregated values
public class KafkaToFilteredObservationFn extends DoFn<KafkaRecord<String, ObservationList>, KV<String, AggregatedObservation>> {
    private String serviceName;
    public KafkaToFilteredObservationFn(Map<String, Object> conf) {
        serviceName = (String) conf.get(Config.SERVICE_NAME);
    }
    @ProcessElement
    public void processElement(ProcessContext c) {
        ObservationList observations = c.element().getKV().getValue();

        observations.getObservationList().forEach((obs) -> {
            if (!obs.getCid().contains(serviceName)) {
                Aggregator aggregator = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.minutes);
                AggregatedObservation aggregatedObservation = new AggregatedObservation();
                aggregatedObservation.setAggregator(aggregator);
                aggregatedObservation.setObservation(obs);
                aggregatedObservation.setCount(1L);
                c.output(KV.of(obs.getCid(), aggregatedObservation));
            }
        });
    }
}
