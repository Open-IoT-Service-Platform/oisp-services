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
import org.oisp.services.utils.LogHelper;
import org.slf4j.Logger;


public class AggregateAll extends DoFn<KV<String, Iterable<AggregatedObservation>>, AggregatedObservation> {
    private Aggregator aggregator;

    private static final Logger LOG = LogHelper.getLogger(AggregateAll.class);

    public AggregateAll(Aggregator aggregator) {
        this.aggregator = aggregator;
    }


    private void sendObservation(Aggregator.AggregatorType type, AggregatedObservation immutableAggrObs, ProcessContext c, Object value, Long count) {
        if (aggregator.getType() == type || aggregator.getType() == Aggregator.AggregatorType.ALL) {
            AggregatedObservation newAggrObs = new AggregatedObservation();
            Observation newObs = new Observation(immutableAggrObs.getObservation());
            newObs.setValue(value.toString());
            Aggregator newAggr = new Aggregator(type, aggregator.getUnit());
            newAggrObs.setAggregator(newAggr);
            newAggrObs.setObservation(newObs);
            newAggrObs.setCount(count);

            c.output(newAggrObs);
        }
    }

    private Double sumAggregator(Aggregator.AggregatorType type, Double value, Double sum) {
        if (type == Aggregator.AggregatorType.SUM) {
            return value;
        }
        return sum;
    }

    private Long countAggregator(Aggregator.AggregatorType type, Double value, Long count) {
        if (type == Aggregator.AggregatorType.COUNT) {
            return value.longValue();
        }
        return count;
    }

    private Double minAggregator(Aggregator.AggregatorType type, Double value, Double min) {
        if (type == Aggregator.AggregatorType.MIN) {
            if (value < min) {
                return value;
            }
        }
        return min;
    }

    private Double maxAggregator(Aggregator.AggregatorType type, Double value, Double max) {
        if (type == Aggregator.AggregatorType.MAX) {
            if (value > max) {
                return value;
            }
        }
        return max;
    }

    @ProcessElement
    public void processElement(ProcessContext c, PaneInfo paneInfo) {
        Iterable<AggregatedObservation> itObs  = c.element().getValue();
        AggregatedObservation firstObs = itObs.iterator().next();

        if (!firstObs.getObservation().isNumber()) {
            return;
        }
        Long avgcount = 0L;
        Double min = Double.MAX_VALUE;
        Double max = -Double.MAX_VALUE;
        Double accum = 0.0;
        for (AggregatedObservation aggrobs : itObs) {
            Double value = 0.0;
            try {
                value = Double.parseDouble(aggrobs.getObservation().getValue());
            } catch (NumberFormatException error) {
                LOG.warn("Error: {}, cid: {} and value {} could not be parsed! ", error.getMessage(), firstObs.getObservation().getCid(), aggrobs.getObservation().getValue());
            }

            Aggregator.AggregatorType aggregatorType = aggrobs.getAggregator().getType();
            if (aggregatorType == Aggregator.AggregatorType.COUNT || aggregatorType == Aggregator.AggregatorType.SUM) {
                //these aggregations can be derived from AVG aggregator, should not be stored and ignored in any case
                continue;
            }
            if (aggregatorType == Aggregator.AggregatorType.AVG) {

                accum += value * aggrobs.getCount();

                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
                avgcount += aggrobs.getCount();
            }
            min = minAggregator(aggregatorType, value, min);
            max = maxAggregator(aggregatorType, value, max);

        }

        Double avg = accum / avgcount;

        AggregatedObservation immutableObs = itObs.iterator().next();
        sendObservation(Aggregator.AggregatorType.AVG, immutableObs, c, avg, avgcount);
        sendObservation(Aggregator.AggregatorType.SUM, immutableObs, c, accum, 0L);
        sendObservation(Aggregator.AggregatorType.MIN, immutableObs, c, min, 0L);
        sendObservation(Aggregator.AggregatorType.MAX, immutableObs, c, max, 0L);
        sendObservation(Aggregator.AggregatorType.COUNT, immutableObs, c, avgcount, 0L);
    }

}
