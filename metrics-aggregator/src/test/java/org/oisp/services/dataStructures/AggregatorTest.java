package org.oisp.services.dataStructures;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregatorTest {

    private final Aggregator aggregator1 = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.minutes);
    private final Aggregator aggregator2 = new Aggregator(Aggregator.AggregatorType.AVG, Aggregator.AggregatorUnit.hours);


    @Test
    void getWindowStartTimeTest() {
        Instant time = new Instant("2020-01-01T19:45:00.123");
        assertEquals(aggregator1.getWindowStartTime(time), new Instant("2020-01-01T19:45:00"));
        assertEquals(aggregator2.getWindowStartTime(time), new Instant("2020-01-01T19:00:00"));
    }

    @Test
    void getWindowDurationTest() {
        assertEquals(aggregator1.getWindowDuration(), new Duration(Duration.standardMinutes(1)));
        assertEquals(aggregator2.getWindowDuration(), new Duration(Duration.standardHours(1)));
    }
}