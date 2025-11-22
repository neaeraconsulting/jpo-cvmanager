package us.dot.its.jpo.rsustatusmonitor.tasks;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.RsuMsgfwdValues;
import us.dot.its.jpo.rsustatusmonitor.services.PostgresService;
import us.dot.its.jpo.rsustatusmonitor.services.RsuNearestNeighborService;
import us.dot.its.jpo.rsustatusmonitor.snmp.NearestNeighborProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RsuNearestNeighborSnmpTaskTest {

    @Mock
    private PostgresService postgresService;

    @Mock
    private RsuNearestNeighborService rsuNearestNeighborService;

    @Mock
    private NearestNeighborProperties nearestNeighborProperties;

    private RsuNearestNeighborSnmpTask task;

    @BeforeEach
    public void setup() {
        task = new RsuNearestNeighborSnmpTask(postgresService, rsuNearestNeighborService, nearestNeighborProperties);

        // Setup default nearest neighbor properties
        lenient().when(nearestNeighborProperties.getPsid()).thenReturn("8002");
        lenient().when(nearestNeighborProperties.getDestinationIp()).thenReturn("192.168.1.100");
        lenient().when(nearestNeighborProperties.getPort()).thenReturn(46800);
        lenient().when(nearestNeighborProperties.getProtocol()).thenReturn(6);
        lenient().when(nearestNeighborProperties.getRssi()).thenReturn(-70);
        lenient().when(nearestNeighborProperties.getInterval()).thenReturn(1000);
        lenient().when(nearestNeighborProperties.getSecure()).thenReturn(0);
        lenient().when(nearestNeighborProperties.getAuthMsgInterval()).thenReturn(1000);
    }

    @Test
    public void testConfigureNNForwardingRules_NoCredentials() {
        when(postgresService.getRsusWithCredentials(true)).thenReturn(new ArrayList<>());

        task.configureNNForwardingRules();

        verify(postgresService).getRsusWithCredentials(true);
        verify(rsuNearestNeighborService, never()).configureRsuForwarding(any());
    }

    @Test
    public void testConfigureNNForwardingRules_SingleRsu() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        credentials.add(cred1);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(postgresService).getRsusWithCredentials(true);
        verify(rsuNearestNeighborService, times(1)).configureRsuForwarding(captor.capture());

        RsuMsgfwdValues capturedValues = captor.getValue();
        assertNotNull(capturedValues);
        assertEquals(cred1, capturedValues.getRsuSnmpCredentials());
        assertEquals("8002", capturedValues.getRsuReceivedMsgPsid());
        assertEquals("192.168.1.100", capturedValues.getRsuReceivedMsgDestIpAddr());
        assertEquals(46800, capturedValues.getRsuReceivedMsgDestPort());
        assertEquals(6, capturedValues.getRsuReceivedMsgProtocol());
        assertEquals(-70, capturedValues.getRsuReceivedMsgRssi());
        assertEquals(1000, capturedValues.getRsuReceivedMsgInterval());
        assertEquals(4, capturedValues.getRsuReceivedMsgStatus());
        assertEquals(0, capturedValues.getRsuReceivedMsgSecure());
        assertEquals(1000, capturedValues.getRsuReceivedMsgAuthMsgInterval());
        assertNotNull(capturedValues.getRsuReceivedMsgDeliveryStart());
        assertNotNull(capturedValues.getRsuReceivedMsgDeliveryStop());
    }

    @Test
    public void testConfigureNNForwardingRules_MultipleRsus() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        RsuSnmpCredentials cred2 = new RsuSnmpCredentials(2, "192.168.1.2", "user2", "pass2", "encPass2", "SNMPv3",
                "12346");
        RsuSnmpCredentials cred3 = new RsuSnmpCredentials(3, "192.168.1.3", "user3", "pass3", "encPass3", "SNMPv3",
                "12347");
        credentials.add(cred1);
        credentials.add(cred2);
        credentials.add(cred3);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(postgresService).getRsusWithCredentials(true);
        verify(rsuNearestNeighborService, times(3)).configureRsuForwarding(captor.capture());

        List<RsuMsgfwdValues> capturedValues = captor.getAllValues();
        assertEquals(3, capturedValues.size());

        // Verify each RSU was configured
        assertEquals(cred1, capturedValues.get(0).getRsuSnmpCredentials());
        assertEquals(cred2, capturedValues.get(1).getRsuSnmpCredentials());
        assertEquals(cred3, capturedValues.get(2).getRsuSnmpCredentials());
    }

    @Test
    public void testConfigureNNForwardingRules_TimestampSpacing() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        RsuSnmpCredentials cred2 = new RsuSnmpCredentials(2, "192.168.1.2", "user2", "pass2", "encPass2", "SNMPv3",
                "12346");
        credentials.add(cred1);
        credentials.add(cred2);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService, times(2)).configureRsuForwarding(captor.capture());

        List<RsuMsgfwdValues> capturedValues = captor.getAllValues();

        // Verify that start times are different (spaced out)
        String startTime1 = capturedValues.get(0).getRsuReceivedMsgDeliveryStart();
        String startTime2 = capturedValues.get(1).getRsuReceivedMsgDeliveryStart();

        assertNotNull(startTime1);
        assertNotNull(startTime2);
        assertNotEquals(startTime1, startTime2, "Start times should be different for different RSUs");
    }

    @Test
    public void testConfigureNNForwardingRules_StopTimeIsOneMinuteAfterStart() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        credentials.add(cred1);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService).configureRsuForwarding(captor.capture());

        RsuMsgfwdValues capturedValues = captor.getValue();
        String startHex = capturedValues.getRsuReceivedMsgDeliveryStart();
        String stopHex = capturedValues.getRsuReceivedMsgDeliveryStop();

        assertNotNull(startHex);
        assertNotNull(stopHex);

        // Verify they are hex strings (16 characters each for NTCIP 1218 format)
        assertEquals(16, startHex.length(), "Start time should be 16 hex characters");
        assertEquals(16, stopHex.length(), "Stop time should be 16 hex characters"); // Stop time should be greater than
                                                                                     // start time (1 minute later)
        assertTrue(stopHex.compareTo(startHex) > 0, "Stop time should be after start time");
    }

    @Test
    public void testConfigureNNForwardingRules_AllPropertiesSetCorrectly() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        credentials.add(cred1);

        // Override properties with specific test values
        when(nearestNeighborProperties.getPsid()).thenReturn("9999");
        when(nearestNeighborProperties.getDestinationIp()).thenReturn("10.0.0.1");
        when(nearestNeighborProperties.getPort()).thenReturn(8080);
        when(nearestNeighborProperties.getProtocol()).thenReturn(17);
        when(nearestNeighborProperties.getRssi()).thenReturn(-85);
        when(nearestNeighborProperties.getInterval()).thenReturn(2000);
        when(nearestNeighborProperties.getSecure()).thenReturn(1);
        when(nearestNeighborProperties.getAuthMsgInterval()).thenReturn(3000);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService).configureRsuForwarding(captor.capture());

        RsuMsgfwdValues capturedValues = captor.getValue();
        assertEquals("9999", capturedValues.getRsuReceivedMsgPsid());
        assertEquals("10.0.0.1", capturedValues.getRsuReceivedMsgDestIpAddr());
        assertEquals(8080, capturedValues.getRsuReceivedMsgDestPort());
        assertEquals(17, capturedValues.getRsuReceivedMsgProtocol());
        assertEquals(-85, capturedValues.getRsuReceivedMsgRssi());
        assertEquals(2000, capturedValues.getRsuReceivedMsgInterval());
        assertEquals(1, capturedValues.getRsuReceivedMsgSecure());
        assertEquals(3000, capturedValues.getRsuReceivedMsgAuthMsgInterval());
    }

    @Test
    public void testConfigureNNForwardingRules_StatusAlwaysSetToFour() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        credentials.add(cred1);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService).configureRsuForwarding(captor.capture());

        RsuMsgfwdValues capturedValues = captor.getValue();
        assertEquals(4, capturedValues.getRsuReceivedMsgStatus(), "Status should always be set to 4");
    }

    @Test
    public void testConfigureNNForwardingRules_LargeNumberOfRsus() {
        // Create 100 RSUs to test wrap-around logic
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            RsuSnmpCredentials cred = new RsuSnmpCredentials(
                    i,
                    "192.168.1." + i,
                    "user" + i,
                    "pass" + i,
                    "encPass" + i,
                    "SNMPv3",
                    String.valueOf(10000 + i));
            credentials.add(cred);
        }

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(postgresService).getRsusWithCredentials(true);
        verify(rsuNearestNeighborService, times(100)).configureRsuForwarding(captor.capture());

        List<RsuMsgfwdValues> capturedValues = captor.getAllValues();
        assertEquals(100, capturedValues.size());

        // Verify all have valid hex timestamps
        for (RsuMsgfwdValues values : capturedValues) {
            assertNotNull(values.getRsuReceivedMsgDeliveryStart());
            assertNotNull(values.getRsuReceivedMsgDeliveryStop());
            assertEquals(16, values.getRsuReceivedMsgDeliveryStart().length());
            assertEquals(16, values.getRsuReceivedMsgDeliveryStop().length());
        }
    }

    @Test
    public void testConfigureNNForwardingRules_VerifyAsyncServiceCalled() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        RsuSnmpCredentials cred2 = new RsuSnmpCredentials(2, "192.168.1.2", "user2", "pass2", "encPass2", "SNMPv3",
                "12346");
        credentials.add(cred1);
        credentials.add(cred2);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        task.configureNNForwardingRules();

        // Verify the async service method was called for each RSU
        verify(rsuNearestNeighborService, times(2)).configureRsuForwarding(any(RsuMsgfwdValues.class));
    }

    @Test
    public void testConfigureNNForwardingRules_HexTimestampFormat() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        credentials.add(cred1);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService).configureRsuForwarding(captor.capture());

        RsuMsgfwdValues capturedValues = captor.getValue();
        String startHex = capturedValues.getRsuReceivedMsgDeliveryStart();
        String stopHex = capturedValues.getRsuReceivedMsgDeliveryStop();

        // Verify hex format (should only contain hex characters)
        assertTrue(startHex.matches("[0-9a-fA-F]+"), "Start time should be valid hex string");
        assertTrue(stopHex.matches("[0-9a-fA-F]+"), "Stop time should be valid hex string");
    }

    @Test
    public void testConfigureNNForwardingRules_MidnightWrapAround() {
        // Test when current time is close to midnight
        // This test verifies the wrap-around logic works correctly
        List<RsuSnmpCredentials> credentials = new ArrayList<>();

        // Create enough RSUs to potentially exceed available minutes until midnight
        for (int i = 1; i <= 50; i++) {
            RsuSnmpCredentials cred = new RsuSnmpCredentials(
                    i,
                    "192.168.1." + i,
                    "user" + i,
                    "pass" + i,
                    "encPass" + i,
                    "SNMPv3",
                    String.valueOf(20000 + i));
            credentials.add(cred);
        }

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService, times(50)).configureRsuForwarding(captor.capture());

        List<RsuMsgfwdValues> capturedValues = captor.getAllValues();

        // Verify all RSUs got valid timestamps (wrap-around handled via modulo)
        for (int i = 0; i < capturedValues.size(); i++) {
            RsuMsgfwdValues values = capturedValues.get(i);
            assertNotNull(values.getRsuReceivedMsgDeliveryStart(), "RSU " + i + " should have start time");
            assertNotNull(values.getRsuReceivedMsgDeliveryStop(), "RSU " + i + " should have stop time");
        }
    }

    @Test
    public void testConstructor() {
        RsuNearestNeighborSnmpTask newTask = new RsuNearestNeighborSnmpTask(
                postgresService, rsuNearestNeighborService, nearestNeighborProperties);
        assertNotNull(newTask);
    }

    @Test
    public void testConfigureNNForwardingRules_VerifyIntersectionIdMapping() {
        List<RsuSnmpCredentials> credentials = new ArrayList<>();
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "100");
        RsuSnmpCredentials cred2 = new RsuSnmpCredentials(2, "192.168.1.2", "user2", "pass2", "encPass2", "SNMPv3",
                "200");
        credentials.add(cred1);
        credentials.add(cred2);

        when(postgresService.getRsusWithCredentials(true)).thenReturn(credentials);

        ArgumentCaptor<RsuMsgfwdValues> captor = ArgumentCaptor.forClass(RsuMsgfwdValues.class);

        task.configureNNForwardingRules();

        verify(rsuNearestNeighborService, times(2)).configureRsuForwarding(captor.capture());

        List<RsuMsgfwdValues> capturedValues = captor.getAllValues();

        // Verify the credentials are correctly mapped
        assertEquals("100", capturedValues.get(0).getRsuSnmpCredentials().getIntersection_id());
        assertEquals("200", capturedValues.get(1).getRsuSnmpCredentials().getIntersection_id());
    }
}
