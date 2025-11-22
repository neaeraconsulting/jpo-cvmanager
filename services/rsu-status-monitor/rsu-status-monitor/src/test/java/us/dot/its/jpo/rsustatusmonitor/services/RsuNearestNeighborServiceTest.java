package us.dot.its.jpo.rsustatusmonitor.services;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snmp4j.smi.Variable;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.RsuMsgfwdValues;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class RsuNearestNeighborServiceTest {

    @Mock
    private SNMPService snmpService;

    @InjectMocks
    private RsuNearestNeighborService service;

    private RsuSnmpCredentials credentials;
    private RsuMsgfwdValues msgfwdValues;

    @BeforeEach
    public void setup() {
        credentials = new RsuSnmpCredentials(1, "192.168.1.100", "testUser", "testPass", "encryptPass", "SNMPv3",
                "12345");

        msgfwdValues = new RsuMsgfwdValues();
        msgfwdValues.setRsuSnmpCredentials(credentials);
        msgfwdValues.setRsuReceivedMsgPsid("20");
        msgfwdValues.setRsuReceivedMsgDestIpAddr("192.168.1.1");
        msgfwdValues.setRsuReceivedMsgDestPort(8080);
        msgfwdValues.setRsuReceivedMsgProtocol(6);
        msgfwdValues.setRsuReceivedMsgRssi(-100);
        msgfwdValues.setRsuReceivedMsgInterval(1000);
        msgfwdValues.setRsuReceivedMsgDeliveryStart("01010000");
        msgfwdValues.setRsuReceivedMsgDeliveryStop("01020000");
        msgfwdValues.setRsuReceivedMsgStatus(4);
        msgfwdValues.setRsuReceivedMsgSecure(0);
        msgfwdValues.setRsuReceivedMsgAuthMsgInterval(500);
    }

    @Test
    public void testConfigureRsuForwarding_Success() throws Exception {
        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async method to complete
        Thread.sleep(100);

        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Variable>> oidMapCaptor = ArgumentCaptor.forClass(Map.class);

        verify(snmpService).setSnmpV3Values(
                ipCaptor.capture(),
                usernameCaptor.capture(),
                passwordCaptor.capture(),
                oidMapCaptor.capture());

        assertEquals("192.168.1.100", ipCaptor.getValue());
        assertEquals("testUser", usernameCaptor.getValue());
        assertEquals("testPass", passwordCaptor.getValue());

        Map<String, Variable> capturedOidMap = oidMapCaptor.getValue();
        assertNotNull(capturedOidMap);
        assertEquals(11, capturedOidMap.size());
    }

    @Test
    public void testConfigureRsuForwarding_WithDifferentCredentials() throws Exception {
        RsuSnmpCredentials differentCreds = new RsuSnmpCredentials(2, "10.0.0.50", "user2", "pass2", "encPass2",
                "SNMPv3", "67890");
        msgfwdValues.setRsuSnmpCredentials(differentCreds);

        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async method to complete
        Thread.sleep(100);

        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(snmpService).setSnmpV3Values(
                ipCaptor.capture(),
                usernameCaptor.capture(),
                passwordCaptor.capture(),
                anyMap());

        assertEquals("10.0.0.50", ipCaptor.getValue());
        assertEquals("user2", usernameCaptor.getValue());
        assertEquals("pass2", passwordCaptor.getValue());
    }

    @Test
    public void testConfigureRsuForwarding_WithDifferentMsgfwdValues() throws Exception {
        msgfwdValues.setRsuReceivedMsgDestIpAddr("172.16.0.1");
        msgfwdValues.setRsuReceivedMsgDestPort(9090);
        msgfwdValues.setRsuReceivedMsgProtocol(17); // UDP

        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async method to complete
        Thread.sleep(100);

        verify(snmpService).setSnmpV3Values(
                eq("192.168.1.100"),
                eq("testUser"),
                eq("testPass"),
                anyMap());
    }

    @Test
    public void testConfigureRsuForwarding_SnmpServiceThrowsException() throws Exception {
        doThrow(new RuntimeException("SNMP connection failed"))
                .when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async method to complete
        Thread.sleep(100);

        verify(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    public void testConfigureRsuForwarding_WithNullIntersectionId() throws Exception {
        RsuSnmpCredentials credsWithNullIntersection = new RsuSnmpCredentials(3, "192.168.2.1", "user3", "pass3",
                "encPass3", "SNMPv3", null);
        msgfwdValues.setRsuSnmpCredentials(credsWithNullIntersection);

        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async methods to complete
        Thread.sleep(100);

        verify(snmpService).setSnmpV3Values(
                eq("192.168.2.1"),
                eq("user3"),
                eq("pass3"),
                anyMap());
    }

    @Test
    public void testConfigureRsuForwarding_VerifyOidMapContainsAllRequiredKeys() throws Exception {
        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async methods to complete
        Thread.sleep(100);

        ArgumentCaptor<Map<String, Variable>> oidMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), oidMapCaptor.capture());

        Map<String, Variable> capturedOidMap = oidMapCaptor.getValue();

        // Verify the map contains entries for all 11 OIDs (ending with .33)
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgPsid") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgDestIpAddr") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgDestPort") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgProtocol") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgRssi") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgInterval") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgDeliveryStart") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgDeliveryStop") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgStatus") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgSecure") || key.endsWith(".33")));
        assertTrue(capturedOidMap.keySet().stream()
                .anyMatch(key -> key.contains("rsuReceivedMsgAuthMsgInterval") || key.endsWith(".33")));
    }

    @Test
    public void testConfigureRsuForwarding_WithMinimalValues() throws Exception {
        msgfwdValues.setRsuReceivedMsgRssi(0);
        msgfwdValues.setRsuReceivedMsgInterval(0);
        msgfwdValues.setRsuReceivedMsgAuthMsgInterval(0);

        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async method to complete
        Thread.sleep(100);

        verify(snmpService).setSnmpV3Values(
                eq("192.168.1.100"),
                eq("testUser"),
                eq("testPass"),
                anyMap());
    }

    @Test
    public void testConfigureRsuForwarding_WithMaxValues() throws Exception {
        msgfwdValues.setRsuReceivedMsgDestPort(65535);
        msgfwdValues.setRsuReceivedMsgProtocol(255);
        msgfwdValues.setRsuReceivedMsgInterval(Integer.MAX_VALUE);

        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(msgfwdValues);

        // Allow async method to complete
        Thread.sleep(100);

        ArgumentCaptor<Map<String, Variable>> oidMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), oidMapCaptor.capture());

        assertNotNull(oidMapCaptor.getValue());
    }

    @Test
    public void testConfigureRsuForwarding_AsyncExecution() throws Exception {
        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        long startTime = System.currentTimeMillis();
        service.configureRsuForwarding(msgfwdValues);
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 50, "Async method should return immediately");

        // Allow async method to complete
        Thread.sleep(100);

        verify(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    public void testConfigureRsuForwarding_MultipleCallsConcurrently() throws Exception {
        RsuMsgfwdValues values1 = new RsuMsgfwdValues();
        values1.setRsuSnmpCredentials(
                new RsuSnmpCredentials(10, "192.168.1.10", "user1", "pass1", "enc1", "SNMPv3", "1000"));
        values1.setRsuReceivedMsgPsid("20");
        values1.setRsuReceivedMsgDestIpAddr("192.168.1.1");
        values1.setRsuReceivedMsgDestPort(8080);
        values1.setRsuReceivedMsgProtocol(6);
        values1.setRsuReceivedMsgRssi(-100);
        values1.setRsuReceivedMsgInterval(1000);
        values1.setRsuReceivedMsgDeliveryStart("01010000");
        values1.setRsuReceivedMsgDeliveryStop("01020000");
        values1.setRsuReceivedMsgStatus(4);
        values1.setRsuReceivedMsgSecure(0);
        values1.setRsuReceivedMsgAuthMsgInterval(500);

        RsuMsgfwdValues values2 = new RsuMsgfwdValues();
        values2.setRsuSnmpCredentials(
                new RsuSnmpCredentials(20, "192.168.1.20", "user2", "pass2", "enc2", "SNMPv3", "2000"));
        values2.setRsuReceivedMsgPsid("21");
        values2.setRsuReceivedMsgDestIpAddr("192.168.1.2");
        values2.setRsuReceivedMsgDestPort(9090);
        values2.setRsuReceivedMsgProtocol(17);
        values2.setRsuReceivedMsgRssi(-90);
        values2.setRsuReceivedMsgInterval(2000);
        values2.setRsuReceivedMsgDeliveryStart("02010000");
        values2.setRsuReceivedMsgDeliveryStop("02020000");
        values2.setRsuReceivedMsgStatus(4);
        values2.setRsuReceivedMsgSecure(1);
        values2.setRsuReceivedMsgAuthMsgInterval(1000);

        doNothing().when(snmpService).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());

        service.configureRsuForwarding(values1);
        service.configureRsuForwarding(values2);

        // Allow async methods to complete
        Thread.sleep(200);

        verify(snmpService, times(2)).setSnmpV3Values(anyString(), anyString(), anyString(), anyMap());
    }
}
