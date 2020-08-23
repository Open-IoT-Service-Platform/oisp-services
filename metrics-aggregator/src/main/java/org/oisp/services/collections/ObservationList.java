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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ObservationList implements Serializable {
    // needed to workaround the fact that get class of List<Observation> fails
    public List<Observation> getObservationList() {
        return observationList;
    }

    public ObservationList() {
        observationList = new ArrayList<Observation>();
    }
    public void setObservationList(List<Observation> observationList) {
        this.observationList = observationList;
    }

    private List<Observation> observationList;
}
