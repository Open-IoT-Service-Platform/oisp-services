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
 
package org.oisp.services.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.kafka.common.serialization.Deserializer;
import org.oisp.services.collections.Observation;
import org.oisp.services.collections.ObservationList;

import java.util.ArrayList;
import java.util.List;

public class ObservationDeserializer implements Deserializer<ObservationList> {

    public ObservationDeserializer() {
    }

    public void configure(java.util.Map<String, ?> configs, boolean isKey) {
    }

    public void close() {
    }

    public ObservationList deserialize(String topic,
                                       byte[] data) {
        Gson g = new Gson();
        List<Observation> observations = new ArrayList<Observation>();
        try {
            Observation observation = g.fromJson(new String(data), new TypeToken<Observation>() {
            }.getType());
            observations.add(observation);
        } catch (JsonSyntaxException e) {
            observations = g.fromJson(new String(data), new TypeToken<List<Observation>>() {
            }.getType());
        }
        ObservationList obsList = new ObservationList();
        obsList.setObservationList(observations);
        return obsList;
    }
}
