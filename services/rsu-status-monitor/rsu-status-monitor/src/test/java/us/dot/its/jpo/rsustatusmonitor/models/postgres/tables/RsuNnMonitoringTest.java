package us.dot.its.jpo.rsustatusmonitor.models.postgres.tables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RsuNnMonitoringTest {

    @Test
    public void testGettersAndSetters() {
        RsuNnMonitoring monitoring = new RsuNnMonitoring();
        
        monitoring.setRsu_nn_monitoring_id(1);
        monitoring.setRsu_id(100);
        monitoring.setActive(true);
        
        assertEquals(1, monitoring.getRsu_nn_monitoring_id());
        assertEquals(100, monitoring.getRsu_id());
        assertTrue(monitoring.isActive());
    }

    @Test
    public void testEqualsAndHashCode() {
        RsuNnMonitoring monitoring1 = new RsuNnMonitoring();
        monitoring1.setRsu_nn_monitoring_id(1);
        monitoring1.setRsu_id(100);
        monitoring1.setActive(true);
        
        RsuNnMonitoring monitoring2 = new RsuNnMonitoring();
        monitoring2.setRsu_nn_monitoring_id(1);
        monitoring2.setRsu_id(100);
        monitoring2.setActive(true);
        
        assertEquals(monitoring1, monitoring2);
        assertEquals(monitoring1.hashCode(), monitoring2.hashCode());
    }

    @Test
    public void testToString() {
        RsuNnMonitoring monitoring = new RsuNnMonitoring();
        monitoring.setRsu_nn_monitoring_id(1);
        
        String result = monitoring.toString();
        assertNotNull(result);
    }
}
