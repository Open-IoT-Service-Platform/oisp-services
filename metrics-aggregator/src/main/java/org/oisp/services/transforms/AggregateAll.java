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
import org.apache.beam.sdk.transforms.windowing.PaneInfo;
import org.apache.beam.sdk.values.KV;
import org.oisp.services.collections.AggregatedObservation;
import org.oisp.services.collections.Observation;
import org.oisp.services.dataStructures.Aggregator;

public class AggregateAll extends DoFn<KV<String, Iterable<Observation>>, AggregatedObservation> {
    private Aggregator aggregator;

    public AggregateAll(Aggregator aggregator) {
        this.aggregator = aggregator;
    }
    private void sendObservation(Aggregator.AggregatorType type, Observation immutableObs, ProcessContext c, Object value) {
        if (aggregator.getType() == type || aggregator.getType() == Aggregator.AggregatorType.ALL) {
            Observation newObs = new Observation(immutableObs);
            newObs.setValue(value.toString());
            Aggregator newAggr = new Aggregator(type, aggregator.getUnit());
            c.output(new AggregatedObservation(newObs, newAggr));
        }
    }
    @ProcessElement
    public void processElement(ProcessContext c, PaneInfo paneInfo) {
        Iterable<Observation> itObs  = c.element().getValue();
        Observation firstObs = itObs.iterator().next();
        if (firstObs.isNumber()) {
            Long count = 0L;
            Double min = Double.MAX_VALUE;
            Double max = -Double.MAX_VALUE;
            Double accum = 0.0;
            for (Observation obs : itObs) {
                Double value = Double.parseDouble(obs.getValue());
                accum += value;
                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
                count++;
            }

            Double avg = accum / count;
            Observation immutableObs = itObs.iterator().next();
            sendObservation(Aggregator.AggregatorType.AVG, immutableObs, c, avg);
            sendObservation(Aggregator.AggregatorType.SUM, immutableObs, c, accum);
            sendObservation(Aggregator.AggregatorType.MIN, immutableObs, c, min);
            sendObservation(Aggregator.AggregatorType.MAX, immutableObs, c, max);
            sendObservation(Aggregator.AggregatorType.COUNT, immutableObs, c, count);
        }
    }
}
