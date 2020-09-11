package org.oisp.services;

import avro.shaded.com.google.common.collect.ImmutableMap;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.javatuples.Triplet;
import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public abstract class ComponentSplitter {

    private static final String KAFKAURL = "oisp-kafka-headless:9092";

    public static void main(String[] args) {
        PipelineOptions options = PipelineOptionsFactory.fromArgs(args).create();
        //FlinkPipelineOptions flinkPipelineOptions = options.as(FlinkPipelineOptions.class);
        // In order to run your pipeline, you need to make following runner specific changes:
        //
        // CHANGE 1/3: Select a Beam runner, such as BlockingDataflowRunner
        // or FlinkRunner.
        // CHANGE 2/3: Specify runner-required options.
        // For BlockingDataflowRunner, set project and temp location as follows:
        //   DataflowPipelineOptions dataflowOptions = options.as(DataflowPipelineOptions.class);
        //   dataflowOptions.setRunner(BlockingDataflowRunner.class);
        //   dataflowOptions.setProject("SET_YOUR_PROJECT_ID_HERE");
        //   dataflowOptions.setTempLocation("gs://SET_YOUR_BUCKET_NAME_HERE/AND_TEMP_DIRECTORY");
        // For FlinkRunner, set the runner as follows. See {@code FlinkPipelineOptions}
        // for more details.
        //   options.as(FlinkPipelineOptions.class)
        //      .setRunner(FlinkRunner.class);

        // Create the Pipeline object with the options we defined above

        // PipelineOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().create();
        Pipeline p = Pipeline.create(options);
        p.apply(KafkaIO.<String, String>read()
                .withBootstrapServers(KAFKAURL)
                .withTopic("metrics")
                .withKeyDeserializer(StringDeserializer.class)
                .withValueDeserializer(StringDeserializer.class)
                .withConsumerConfigUpdates(ImmutableMap.of("group.id", "my_beam_app_1"))
                .withoutMetadata() // PCollection<KV<Long, String>
        )
                .apply(Window.<KV<String, String>>into(FixedWindows.of(Duration.standardSeconds(1))))
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
                        String accountId = jo.getString("aid");
                        String componentId = jo.getString("cid");
                        String topic = "metrics.".concat(accountId).concat(".").concat(componentId);
                        System.out.println(topic);
                        return KV.of(topic, jo.toString());
                    }
                }))
                .apply("FormatResults", MapElements.via(new SimpleFunction<KV<String, String>, ProducerRecord<Long, String>>() {
                //.apply("FormatResults", MapElements.via(new SimpleFunction<String, ProducerRecord<Long, String>>() {
                    @Override
                    public ProducerRecord<Long, String> apply(KV<String, String> input) {
                        return new ProducerRecord<Long, String>(input.getKey(), 0L, input.getValue());
                    }
                })).setCoder(new MyCustomCoder())
                .apply((KafkaIO.<Long, String>writeRecords()
                        .withBootstrapServers(KAFKAURL)
                        .withKeySerializer(LongSerializer.class)
                        .withValueSerializer(StringSerializer.class)
                ));
        p.run().waitUntilFinish();
    }

    public static class MyCustomCoder extends CustomCoder<ProducerRecord<Long, String>> {

        @Override
        public void encode(ProducerRecord<Long, String> value, OutputStream outStream) throws CoderException, IOException {
            Triplet<String, Long, String> triplet = Triplet.with(value.topic(), value.key(), value.value());
            outStream.write(SerializationUtils.serialize(triplet));
        }

        @Override
        public ProducerRecord<Long, String> decode(InputStream inStream) throws CoderException, IOException {
            byte[] targetArray = new byte[inStream.available()];
            inStream.read(targetArray);
            Triplet triplet = (Triplet) SerializationUtils.deserialize(targetArray);
            return new ProducerRecord((String) triplet.getValue(0),
                    (Long) triplet.getValue(1),
                    (String) triplet.getValue(2));
        }
    }

}
