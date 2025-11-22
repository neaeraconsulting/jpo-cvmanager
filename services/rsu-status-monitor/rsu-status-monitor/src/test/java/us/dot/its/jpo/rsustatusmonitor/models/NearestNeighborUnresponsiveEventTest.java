package us.dot.its.jpo.rsustatusmonitor.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NearestNeighborUnresponsiveEventTest {

    @Test
    public void testConstructor() {
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        assertNotNull(event);
        assertEquals("NearestNeighborUnresponsive", event.getEventType());
    }

    @Test
    public void testGetKey() {
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        event.setIntersectionID(12345);

        String key = event.getKey();
        assertEquals("12345", key);
    }

    @Test
    public void testGetKeyWithNullIntersectionId() {
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        String key = event.getKey();
        assertEquals("-1", key);
    }

    @Test
    public void testEqualsAndHashCode() {
        NearestNeighborUnresponsiveEvent event1 = new NearestNeighborUnresponsiveEvent();
        event1.setIntersectionID(123);

        NearestNeighborUnresponsiveEvent event2 = new NearestNeighborUnresponsiveEvent();
        event2.setIntersectionID(123);

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    public void testGettersAndSetters() {
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        event.setIntersectionID(999);
        event.setRoadRegulatorID(888);

        assertEquals(999, event.getIntersectionID());
        assertEquals(888, event.getRoadRegulatorID());
    }
}
