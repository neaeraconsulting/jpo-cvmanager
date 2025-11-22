package us.dot.its.jpo.rsustatusmonitor.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntersectionStatusRecordTest {

    @Test
    public void testNoArgsConstructor() {
        IntersectionStatusRecord record = new IntersectionStatusRecord();
        assertNotNull(record);
    }

    @Test
    public void testAllArgsConstructor() {
        IntersectionStatusRecord record = new IntersectionStatusRecord(123, "192.168.1.1", "2025-11-21T10:00:00Z");
        assertEquals(123, record.getIntersectionId());
        assertEquals("192.168.1.1", record.getListenerIp());
        assertEquals("2025-11-21T10:00:00Z", record.getReceivedAt());
    }

    @Test
    public void testGettersAndSetters() {
        IntersectionStatusRecord record = new IntersectionStatusRecord();
        
        record.setIntersectionId(456);
        record.setListenerIp("10.0.0.1");
        record.setReceivedAt("2025-11-21T11:30:00Z");
        
        assertEquals(456, record.getIntersectionId());
        assertEquals("10.0.0.1", record.getListenerIp());
        assertEquals("2025-11-21T11:30:00Z", record.getReceivedAt());
    }

    @Test
    public void testEqualsAndHashCode() {
        IntersectionStatusRecord record1 = new IntersectionStatusRecord(123, "192.168.1.1", "2025-11-21T10:00:00Z");
        IntersectionStatusRecord record2 = new IntersectionStatusRecord(123, "192.168.1.1", "2025-11-21T10:00:00Z");
        IntersectionStatusRecord record3 = new IntersectionStatusRecord(456, "10.0.0.1", "2025-11-21T11:30:00Z");
        
        assertEquals(record1, record2);
        assertNotEquals(record1, record3);
        assertEquals(record1.hashCode(), record2.hashCode());
    }

    @Test
    public void testToString() {
        IntersectionStatusRecord record = new IntersectionStatusRecord(123, "192.168.1.1", "2025-11-21T10:00:00Z");
        String result = record.toString();
        assertNotNull(result);
        assertTrue(result.contains("123"));
    }
}
