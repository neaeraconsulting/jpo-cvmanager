package us.dot.its.jpo.rsustatusmonitor.services;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snmp4j.smi.Integer32;

import us.dot.its.jpo.rsustatusmonitor.models.api.RsuModeResponse;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.OIDMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RsuModeServiceTest {

    @Mock
    private PostgresService postgresService;

    @Mock
    private SNMPService snmpService;

    @InjectMocks
    private RsuModeService service;

    private RsuSnmpCredentials credentials;

    @BeforeEach
    public void setup() {
        credentials = new RsuSnmpCredentials(1, "192.168.1.100", "testUser", "authPass", "privPass", "SNMPv3",
                "12345");
    }

    @Test
    public void testSetRsuMode_Success() throws Exception {
        when(postgresService.getRsuCredentialsByIp("192.168.1.100")).thenReturn(Optional.of(credentials));

        RsuModeResponse response = service.setRsuMode("192.168.1.100", 4);

        assertEquals("192.168.1.100", response.ipAddress());
        assertEquals(4, response.mode());
        assertEquals("success", response.status());
        verify(snmpService).setSnmpV3Value("192.168.1.100",
                "testUser",
                "authPass",
                OIDMap.oids.get("rsuMode").getOid(),
                4);
    }

    @Test
    public void testSetRsuMode_RsuNotFound() {
        when(postgresService.getRsuCredentialsByIp("192.168.1.200")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> service.setRsuMode("192.168.1.200", 4));

        assertTrue(exception.getMessage().contains("192.168.1.200"));
    }

    @Test
    public void testSetRsuMode_IncompleteCredentials() {
        credentials = new RsuSnmpCredentials(1, "192.168.1.100", null, "authPass", "privPass", "SNMPv3", "12345");
        when(postgresService.getRsuCredentialsByIp("192.168.1.100")).thenReturn(Optional.of(credentials));

        assertThrows(IllegalStateException.class, () -> service.setRsuMode("192.168.1.100", 4));
    }

    @Test
    public void testGetCurrentRsuStatus_Success() throws Exception {
        when(postgresService.getRsuCredentialsByIp("192.168.1.100")).thenReturn(Optional.of(credentials));
        when(snmpService.getSnmpV3Value("192.168.1.100",
                "testUser",
                "authPass",
                "privPass",
                OIDMap.oids.get("rsuModeStatus").getOid())).thenReturn(new Integer32(3));

        RsuModeResponse response = service.getCurrentRsuStatus("192.168.1.100");

        assertEquals("192.168.1.100", response.ipAddress());
        assertEquals(3, response.mode());
        assertEquals("success", response.status());
    }

    @Test
    public void testGetCurrentRsuStatus_NoValueReturned() throws Exception {
        when(postgresService.getRsuCredentialsByIp("192.168.1.100")).thenReturn(Optional.of(credentials));
        when(snmpService.getSnmpV3Value("192.168.1.100",
                "testUser",
                "authPass",
                "privPass",
                OIDMap.oids.get("rsuModeStatus").getOid())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.getCurrentRsuStatus("192.168.1.100"));
    }
}
