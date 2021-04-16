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
package org.oisp.services.collections;

import org.oisp.services.dataStructures.Aggregator;

import java.io.Serializable;

public class AggregatedObservation implements Serializable {
    private Observation observation;
    private Aggregator aggregator;
    private Long count; //count of samples which contributed to the aggregation

    public AggregatedObservation(Observation observation, Aggregator aggregator) {
        this.observation = observation;
        this.aggregator = aggregator;
    }

    public AggregatedObservation() {
    }
    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public AggregatedObservation withCount(Long count) {
        setCount(count);
        return this;
    }

    public AggregatedObservation withObservation(Observation observation) {
        setObservation(observation);
        return this;
    }

    public AggregatedObservation withAggregator(Aggregator aggregator) {
        setAggregator(aggregator);
        return this;
    }
    public String toString() {
        return "AggregatedObservation" + "\ncount: " + this.count + "\n" + this.observation.toString();
    }
}
