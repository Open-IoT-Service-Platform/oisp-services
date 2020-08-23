package org.oisp.services.utils;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import org.oisp.services.collections.Observation;
import org.oisp.services.collections.ObservationList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObservationDeserializerTest {

    @Test
    void deserialize() {
        String testData = "{\"dataType\":\"Boolean\",\"aid\":\"a67e051f-8443-4553-b1b9-de22a15df904\",\"cid\":\"4afd14aa-e56f-4d53-aa22-138121823bee\"," +
                "\"on\":1000000700000,\"value\":\"1\",\"systemOn\":1000000700000,\"attributes\":{\"key5\":\"value6\",\"key1\":\"value1\"},\"loc\":[100.12,300.12]}";
        byte[] testByteArray = testData.getBytes();
        ObservationDeserializer deserializer = new ObservationDeserializer();
        ObservationList observationList = deserializer.deserialize("topic", testByteArray);
        List<Observation> listObs = observationList.getObservationList();
        Observation observation = listObs.get(0);
        assertEquals(observation.getCid(), "4afd14aa-e56f-4d53-aa22-138121823bee");
        assertEquals(observation.getDataType(), "Boolean");
        assertEquals(observation.getAid(), "a67e051f-8443-4553-b1b9-de22a15df904");
        assertEquals(observation.getOn(),1000000700000l);
        assertEquals(observation.getValue(), "1");
        assertEquals(observation.getSystemOn(), 1000000700000l);
        assertEquals(observation.getAttributes().size(), 2);
        assertThat(observation.getAttributes(), IsMapContaining.hasEntry("key5", "value6"));
        assertThat(observation.getAttributes(), IsMapContaining.hasEntry("key1", "value1"));
        assertEquals(observation.getLoc().size(), 2);
        assertThat(observation.getLoc(), hasItem(100.12));
        assertThat(observation.getLoc(), hasItem(300.12));
    }
}