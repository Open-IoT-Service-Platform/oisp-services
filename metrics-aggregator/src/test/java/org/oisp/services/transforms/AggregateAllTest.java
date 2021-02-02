package org.oisp.services.transforms;

import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.joda.time.Duration;
import org.junit.Rule;
import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.oisp.services.collections.AggregatedObservation;
import org.oisp.services.collections.Observation;
import org.oisp.services.dataStructures.Aggregator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Double.parseDouble;


class MapAggregatedObservationToValue extends DoFn<AggregatedObservation, KV<Aggregator.AggregatorType, Double>> implements Serializable {
    void testAssert(Aggregator aggregator, Aggregator.AggregatorType type, Observation observation, Double value) {
        if (type == aggregator.getType()) {
            assertEquals((Double)parseDouble(observation.getValue()), value);
        }
    }
    @ProcessElement public void processElement(ProcessContext c) {
        Observation obs = c.element().getObservation();
        Aggregator aggregator = c.element().getAggregator();
        testAssert(aggregator, Aggregator.AggregatorType.AVG, obs, 2.0);
        testAssert(aggregator, Aggregator.AggregatorType.SUM, obs, 6.0);
        testAssert(aggregator, Aggregator.AggregatorType.MIN, obs, 1.0);
        testAssert(aggregator, Aggregator.AggregatorType.MAX, obs, 3.0);
        testAssert(aggregator, Aggregator.AggregatorType.COUNT, obs, 3.0);
        //c.output(KV.of(aggregatorType, parseDouble(obs.getValue())));
    }
    public MapAggregatedObservationToValue(){}
}


class MapAggregatedObservationToValue2 extends DoFn<AggregatedObservation, KV<Aggregator.AggregatorType, Double>> implements Serializable {
    void testAssert(Aggregator aggregator, Aggregator.AggregatorType type, Observation observation, Double value) {
        if (type == aggregator.getType()) {
            assertEquals((Double)parseDouble(observation.getValue()), value);
        }
    }
    @ProcessElement public void processElement(ProcessContext c) {
        Observation obs = c.element().getObservation();
        Aggregator aggregator = c.element().getAggregator();
        testAssert(aggregator, Aggregator.AggregatorType.AVG, obs, 6.0);
        testAssert(aggregator, Aggregator.AggregatorType.SUM, obs, 30.0);
        testAssert(aggregator, Aggregator.AggregatorType.MIN, obs, 1.0);
        testAssert(aggregator, Aggregator.AggregatorType.MAX, obs, 10.0);
        testAssert(aggregator, Aggregator.AggregatorType.COUNT, obs, 5.0);
        //c.output(KV.of(aggregatorType, parseDouble(obs.getValue())));
    }
    public MapAggregatedObservationToValue2(){}
}

class MapAggregatedObservationToValue3 extends DoFn<AggregatedObservation, KV<Aggregator.AggregatorType, Double>> implements Serializable {
    void testAssert(Aggregator aggregator, Aggregator.AggregatorType type, Observation observation, Double value) {
        if (type == aggregator.getType()) {
            assertEquals((Double)parseDouble(observation.getValue()), value);
        }
    }
    @ProcessElement public void processElement(ProcessContext c) {
        Observation obs = c.element().getObservation();
        Aggregator aggregator = c.element().getAggregator();
        testAssert(aggregator, Aggregator.AggregatorType.AVG, obs, 6.0);
        testAssert(aggregator, Aggregator.AggregatorType.SUM, obs, 30.0);
        testAssert(aggregator, Aggregator.AggregatorType.MIN, obs, 5.0);
        testAssert(aggregator, Aggregator.AggregatorType.MAX, obs, 10.0);
        testAssert(aggregator, Aggregator.AggregatorType.COUNT, obs, 5.0);
        //c.output(KV.of(aggregatorType, parseDouble(obs.getValue())));
    }
    public MapAggregatedObservationToValue3(){}
}

class AggregateAllTest {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create().enableAbandonedNodeEnforcement(false);

    @Test
    void aggregateAvgTest() {

        Aggregator aggregatorAVG = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorALL = new Aggregator(Aggregator.AggregatorType.ALL, Aggregator.AggregatorUnit.minutes);
        Observation observation1 = new Observation();
        observation1.setValue("1.0");
        observation1.setDataType("Number");
        Observation observation2 = new Observation();
        observation2.setValue("2.0");
        observation2.setDataType("Number");
        Observation observation3 = new Observation();
        observation3.setValue("3.0");
        observation3.setDataType("Number");
        AggregatedObservation aggregatedObservation1 = new AggregatedObservation()
                .withObservation(observation1)
                .withAggregator(aggregatorAVG)
                .withCount(1L);
        AggregatedObservation aggregatedObservation2 = new AggregatedObservation()
                .withObservation(observation2)
                .withAggregator(aggregatorAVG)
                .withCount(1L);
        AggregatedObservation aggregatedObservation3 = new AggregatedObservation()
                .withObservation(observation3)
                .withAggregator(aggregatorAVG)
                .withCount(1L);

        AggregatedObservation aggObs = new AggregatedObservation(observation2, aggregatorAVG);
        Iterable<AggregatedObservation> itObs = Arrays.asList(aggregatedObservation1, aggregatedObservation2, aggregatedObservation3);
        KV<String, Iterable<AggregatedObservation>> input = KV.of("key", itObs);
        PCollection<AggregatedObservation> out = pipeline.apply("Create Input", Create.of(input))
                .apply("test AggregateAvg", ParDo.of(new AggregateAll(aggregatorALL)));
        PCollection<KV<Aggregator.AggregatorType, Double>> out2 = (PCollection<KV<Aggregator.AggregatorType, Double>>) out
                .apply("extract aggregation value", ParDo.of(
                        new MapAggregatedObservationToValue())).apply(
                        Window.<KV<Aggregator.AggregatorType, Double>>into(new GlobalWindows()).withAllowedLateness(Duration.standardHours(1L)));;
        ;

        //PAssert.that(out2).containsInAnyOrder(KV.of(Aggregator.AggregatorType.MIN, 5.0));

        pipeline.run().waitUntilFinish();
    }

    @Test
    void aggregateAllTest() {

        Aggregator aggregatorAVG = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorSUM = new Aggregator(Aggregator.AggregatorType.SUM, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorCOUNT = new Aggregator(Aggregator.AggregatorType.COUNT, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorALL = new Aggregator(Aggregator.AggregatorType.ALL, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorMIN = new Aggregator(Aggregator.AggregatorType.MIN, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorMAX = new Aggregator(Aggregator.AggregatorType.MAX, Aggregator.AggregatorUnit.minutes);
        Observation observation1 = new Observation();
        observation1.setValue("1.0");
        observation1.setDataType("Number");
        Observation observation2 = new Observation();
        observation2.setValue("5.0");
        observation2.setDataType("Number");
        Observation observation3 = new Observation();
        observation3.setValue("10.0");
        observation3.setDataType("Number");
        Observation observation4 = new Observation();
        observation4.setValue("30.0");
        observation4.setDataType("Number");
        AggregatedObservation aggregatedObservation1 = new AggregatedObservation()
                .withObservation(observation1)
                .withAggregator(aggregatorMIN)
                .withCount(2L);
        AggregatedObservation aggregatedObservation2 = new AggregatedObservation()
                .withObservation(observation2)
                .withAggregator(aggregatorAVG)
                .withCount(4L);
        AggregatedObservation aggregatedObservation3 = new AggregatedObservation()
                .withObservation(observation3)
                .withAggregator(aggregatorAVG)
                .withCount(1L);
        AggregatedObservation aggregatedObservation4 = new AggregatedObservation()
                .withObservation(observation4)
                .withAggregator(aggregatorSUM)
                .withCount(1L);
        AggregatedObservation aggregatedObservation5 = new AggregatedObservation()
                .withObservation(observation2)
                .withAggregator(aggregatorCOUNT)
                .withCount(1L);
        AggregatedObservation aggregatedObservation6 = new AggregatedObservation()
                .withObservation(observation3)
                .withAggregator(aggregatorMAX)
                .withCount(1L);

        AggregatedObservation aggObs = new AggregatedObservation(observation2, aggregatorAVG);
        Iterable<AggregatedObservation> itObs = Arrays.asList(aggregatedObservation1, aggregatedObservation2,
                aggregatedObservation3, aggregatedObservation4, aggregatedObservation5, aggregatedObservation6);
        KV<String, Iterable<AggregatedObservation>> input = KV.of("key", itObs);
        PCollection<AggregatedObservation> out = pipeline.apply("Create Input", Create.of(input))
                .apply("test AggregateAvg", ParDo.of(new AggregateAll(aggregatorALL)));
        PCollection<KV<Aggregator.AggregatorType, Double>> out2 = (PCollection<KV<Aggregator.AggregatorType, Double>>) out
                .apply("extract aggregation value", ParDo.of(
                        new MapAggregatedObservationToValue2()));

        pipeline.run().waitUntilFinish();
    }
    @Test
    void aggregateMissingTest() {

        Aggregator aggregatorAVG = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.minutes);
        Aggregator aggregatorALL = new Aggregator(Aggregator.AggregatorType.ALL, Aggregator.AggregatorUnit.minutes);
        Observation observation1 = new Observation();
        observation1.setValue("1.0");
        observation1.setDataType("Number");
        Observation observation2 = new Observation();
        observation2.setValue("5.0");
        observation2.setDataType("Number");
        Observation observation3 = new Observation();
        observation3.setValue("10.0");
        observation3.setDataType("Number");
        Observation observation4 = new Observation();
        observation4.setValue("30.0");
        observation4.setDataType("Number");
        AggregatedObservation aggregatedObservation1 = new AggregatedObservation()
                .withObservation(observation2)
                .withAggregator(aggregatorAVG)
                .withCount(4L);
        AggregatedObservation aggregatedObservation2 = new AggregatedObservation()
                .withObservation(observation3)
                .withAggregator(aggregatorAVG)
                .withCount(1L);

        AggregatedObservation aggObs = new AggregatedObservation(observation2, aggregatorAVG);
        Iterable<AggregatedObservation> itObs = Arrays.asList(aggregatedObservation1, aggregatedObservation2);
        KV<String, Iterable<AggregatedObservation>> input = KV.of("key", itObs);
        PCollection<AggregatedObservation> out = pipeline.apply("Create Input", Create.of(input))
                .apply("test AggregateAvg", ParDo.of(new AggregateAll(aggregatorALL)));
        PCollection<KV<Aggregator.AggregatorType, Double>> out2 = (PCollection<KV<Aggregator.AggregatorType, Double>>) out
                .apply("extract aggregation value", ParDo.of(
                        new MapAggregatedObservationToValue3()));


        pipeline.run().waitUntilFinish();
    }
}