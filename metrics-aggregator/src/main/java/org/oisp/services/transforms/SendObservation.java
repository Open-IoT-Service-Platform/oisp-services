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

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.oisp.services.collections.AggregatedObservation;
import org.oisp.services.collections.Observation;
import org.oisp.services.conf.Config;
import org.oisp.services.dataStructures.Aggregator;
import java.util.Map;
import org.joda.time.Instant;

public class SendObservation extends DoFn<AggregatedObservation, KV<String, Observation>> {

    private String serviceName;
    private static final String DOT = ".";
    public SendObservation(Map<String, Object> config) {
        this.serviceName = (String) config.get(Config.SERVICE_NAME);
    }
    @ProcessElement
    public void processElement(ProcessContext c) {
        AggregatedObservation aggrObservation = c.element();
        Observation obs = new Observation(aggrObservation.getObservation());
        Aggregator aggr = aggrObservation.getAggregator();
        String cid = aggrObservation.getObservation().getCid();
        obs.setCid(cid + DOT + serviceName + DOT + aggr.getType() + DOT + aggr.getUnit());
        Long fullTimeMillis = aggr.getWindowStartTime(Instant
                                .ofEpochMilli(aggrObservation.getObservation().getOn())).getMillis();
        Long durationMillis = Math.round(aggr.getWindowDuration().getMillis() / 2.0);
        Long timestampMillis = fullTimeMillis + durationMillis;
        obs.setOn(timestampMillis);
        obs.setSystemOn(timestampMillis);
        c.output(KV.of(obs.getCid(), obs));
    }
}
