package org.oisp.services.transforms;

import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.oisp.services.collections.AggregatedObservation;
import org.oisp.services.collections.Observation;
import org.oisp.services.conf.Config;
import org.oisp.services.dataStructures.Aggregator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;



class MapKVToKV extends DoFn<KV<String, Observation>, String> implements Serializable {
    @ProcessElement public void processElement(ProcessContext c) {
        String key = c.element().getKey();
        c.output(key);
    }
}

class MapKVToOn extends DoFn<KV<String, Observation>, Long> implements Serializable {
    @ProcessElement public void processElement(ProcessContext c) {
        Observation observation = c.element().getValue();
        c.output(observation.getOn());
    }
}

class SendObservationTest {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create().enableAbandonedNodeEnforcement(false);

    @Test
    void sendObservationTest() {
        Aggregator aggregator = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.hours);
        Observation observation = new Observation();
        observation.setValue("1.2");
        observation.setDataType("Number");
        observation.setOn(new Instant("2020-07-27T23:21:10.568Z").getMillis());
        observation.setSystemOn(new Instant("2020-07-27T23:21:12.568Z").getMillis());
        observation.setCid("4afd14aa-e56f-4d53-aa22-138121823bee");
        AggregatedObservation aggObs = new AggregatedObservation(observation, aggregator);
        Map<String, Object> conf = new HashMap<>();
        conf.put(Config.SERVICE_NAME, "service");

        Observation resultObservation = new Observation();

        PCollection<KV<String, Observation>> out = pipeline.apply("Create Input", Create.of(aggObs))
                .apply("test sendObservation", ParDo.of(new SendObservation(conf)));

        // get key from KV
        PCollection<String> key = out.apply("get key", ParDo.of(new MapKVToKV()));
        // get on timestamp from Observation
        PCollection<Long> on = out.apply("get on", ParDo.of(new MapKVToOn()));

        PAssert.that(key).containsInAnyOrder(observation.getCid() + ".service.AVG.hours");
        PAssert.that(on).containsInAnyOrder(Instant.parse("2020-07-27T23:30:00.000Z").getMillis());
        pipeline.run().waitUntilFinish();
    }
}