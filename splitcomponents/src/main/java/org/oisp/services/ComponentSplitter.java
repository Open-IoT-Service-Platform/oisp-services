package org.oisp.services;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.javatuples.Triplet;
import org.joda.time.Duration;
import org.json.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ComponentSplitter {

    public static class MyCustomCoder extends CustomCoder<ProducerRecord<Long, String>> {

        @Override
        public void encode(ProducerRecord<Long, String> value, OutputStream outStream) throws CoderException, IOException {
            Triplet<String, Long, String> triplet = Triplet.with(value.topic(), value.key(), value.value());
            outStream.write(SerializationUtils.serialize(triplet));
        }

        @Override
        public ProducerRecord<Long, String> decode(InputStream inStream) throws CoderException, IOException {
            Triplet triplet = (Triplet) SerializationUtils.deserialize(inStream.readAllBytes());
            return new ProducerRecord((String) triplet.getValue(0),
                    (Long) triplet.getValue(1),
                    (String) triplet.getValue(2));
        }
    }

    public static void main(String[] args) {

        PipelineOptions options = PipelineOptionsFactory.create();
        Pipeline p = Pipeline.create(options);

        p.apply(KafkaIO.<Long, String>read()
                .withBootstrapServers("kafka:9092")
                .withTopic("metrics_test")
                .withKeyDeserializer(LongDeserializer.class)
                .withValueDeserializer(StringDeserializer.class)
                .withoutMetadata() // PCollection<KV<Long, String>>
        )
                .apply(Window.<KV<Long, String>>into(FixedWindows.of(Duration.standardSeconds(1))))
                .apply(Values.<String>create())
                .apply("ExtractJsonObjects", ParDo.of(new DoFn<String, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        try {
                            String input = (String) c.element();
                            if (input.charAt(0) == '[') {
                                System.out.println(input);
                                JSONArray ja = new JSONArray(input);
                                for (int i = 0; i < ja.length(); i++) {
                                    c.output(ja.getJSONObject(i).toString());
                                }
                            } else {
                                c.output(input);
                            }
                        } catch (JSONException e) {
                            return;
                        }
                    }
                }))
                .apply("ExtractTopic", MapElements.via(new SimpleFunction<String, KV<String, String>>() {
                    @Override
                    public KV<String, String> apply(String input) {
                        //System.out.println(input)
                        JSONObject jo = new JSONObject(input);
                        String account_id = jo.getString("aid");
                        String component_id = jo.getString("cid");
                        String topic = "metrics.".concat(account_id).concat(".").concat(component_id);
                        return KV.of(topic, jo.toString());
                    }
                }))
                .apply("FormatResults", MapElements.via(new SimpleFunction<KV<String, String>, ProducerRecord<Long, String>>() {
                    @Override
                    public ProducerRecord<Long, String> apply(KV<String, String> input) {
                        return new ProducerRecord<Long, String>(input.getKey(), 0L, input.getValue());
                    }
                })).setCoder(new MyCustomCoder())
                .apply((KafkaIO.<Long, String>writeRecords()
                        .withBootstrapServers("kafka:9092")
                        .withKeySerializer(LongSerializer.class)
                        .withValueSerializer(StringSerializer.class)
                ));
        p.run().waitUntilFinish();
    }
}
