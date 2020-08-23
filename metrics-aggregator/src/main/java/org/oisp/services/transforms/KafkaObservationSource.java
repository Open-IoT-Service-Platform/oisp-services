/*
 * Copyright (c) 2016-2020 Intel Corporation
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
import org.apache.beam.sdk.io.kafka.TimestampPolicy;
import org.apache.beam.sdk.io.kafka.TimestampPolicyFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.joda.time.Instant;
import org.oisp.services.collections.Observation;
import org.oisp.services.collections.ObservationList;
import org.oisp.services.conf.Config;
import org.oisp.services.utils.LogHelper;
import org.oisp.services.utils.ObservationDeserializer;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.*;


public class KafkaObservationSource implements Serializable {

    private KafkaIO.Read<String, ObservationList> transform = null;

    public KafkaIO.Read<String, ObservationList> getTransform() {
        return transform;
    }

    public KafkaObservationSource(Map<String, Object> userConfig, String topic) {

        String serverUri = userConfig.get(Config.KAFKA_BOOTSTRAP_SERVERS).toString();

        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put("group.id", "aggregator");
        consumerProperties.put("enable.auto.commit", "true");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        transform = KafkaIO.<String, ObservationList>read()
                .withBootstrapServers(serverUri)
                .withTopic(topic)
                .withKeyDeserializer(StringDeserializer.class)
                .withValueDeserializer(ObservationDeserializer.class)
                .withConsumerConfigUpdates(consumerProperties)
                .withTimestampPolicyFactory(new CustomTimestampPolicyFactory())
                .withReadCommitted()
                .commitOffsetsInFinalize();

    }


    public class CustomTimestampPolicy extends TimestampPolicy<String, ObservationList> implements Serializable {
        private final Logger log = LogHelper.getLogger(KafkaObservationSource.class);

        private Instant watermark = new Instant(0);

        public Instant	getTimestampForRecord(TimestampPolicy.PartitionContext ctx, KafkaRecord<String, ObservationList> record) {
            List<Observation> obsList = record.getKV().getValue().getObservationList();
            Long minTimestamp = obsList.stream().map((obs) -> obs.getOn()).reduce(Long.MAX_VALUE, (minimum, element) -> minimum > element ? element : minimum);

            if (minTimestamp > watermark.getMillis() && minTimestamp <= Instant.now().getMillis()) {
                watermark = new Instant().withMillis(minTimestamp);
                log.debug("New watermark: {}, key {}", watermark, obsList.get(0).getCid());
            }
            return Instant.ofEpochMilli(minTimestamp);

        }
        public Instant getWatermark(TimestampPolicy.PartitionContext ctx) {

            return watermark;
        }
    }

    public class CustomTimestampPolicyFactory implements TimestampPolicyFactory<String, ObservationList>, Serializable {
        public TimestampPolicy createTimestampPolicy(TopicPartition tp, Optional<Instant> previousWatermark) {
            return new CustomTimestampPolicy();
        }
    }

}
