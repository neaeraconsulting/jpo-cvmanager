package us.dot.its.jpo.rsustatusmonitor.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NearestNeighborUnresponsiveNotificationTest {

    @Test
    public void testConstructor() {
        NearestNeighborUnresponsiveNotification notification = new NearestNeighborUnresponsiveNotification();
        assertNotNull(notification);
        assertEquals("NearestNeighborUnresponsive", notification.getNotificationType());
    }

    @Test
    public void testSetEvent() {
        NearestNeighborUnresponsiveNotification notification = new NearestNeighborUnresponsiveNotification();
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        event.setIntersectionID(123);
        event.setRoadRegulatorID(456);
        
        notification.setEvent(event);
        
        assertEquals(event, notification.getEvent());
        assertEquals(123, notification.getIntersectionID());
        assertEquals(456, notification.getRoadRegulatorID());
    }

    @Test
    public void testSetEventWithNull() {
        NearestNeighborUnresponsiveNotification notification = new NearestNeighborUnresponsiveNotification();
        notification.setEvent(null);
        
        assertNull(notification.getEvent());
    }

    @Test
    public void testGetUniqueId() {
        NearestNeighborUnresponsiveNotification notification = new NearestNeighborUnresponsiveNotification();
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        event.setIntersectionID(789);
        
        notification.setEvent(event);
        
        String uniqueId = notification.getUniqueId();
        assertEquals("NearestNeighborUnresponsive_789", uniqueId);
    }
}
