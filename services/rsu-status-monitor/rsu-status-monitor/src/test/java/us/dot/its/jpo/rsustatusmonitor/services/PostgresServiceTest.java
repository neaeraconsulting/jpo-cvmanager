package us.dot.its.jpo.rsustatusmonitor.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuData;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostgresServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<RsuSnmpCredentials> rsuCredentialsQuery;

    @Mock
    private TypedQuery<RsuData> rsuDataQuery;

    @InjectMocks
    private PostgresService service;

    @Test
    public void testGetRsusWithCredentials_AllRsus_Success() {
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(1, "192.168.1.1", "user1", "pass1", "encPass1", "SNMPv3",
                "12345");
        RsuSnmpCredentials cred2 = new RsuSnmpCredentials(2, "192.168.1.2", "user2", "pass2", "encPass2", "SNMPv2c",
                "67890");
        List<RsuSnmpCredentials> expectedCredentials = Arrays.asList(cred1, cred2);

        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenReturn(expectedCredentials);

        List<RsuSnmpCredentials> result = service.getRsusWithCredentials(false);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRsu_id());
        assertEquals("192.168.1.1", result.get(0).getIpv4_address());
        assertEquals("12345", result.get(0).getIntersection_id());
        verify(entityManager).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery).getResultList();
    }

    @Test
    public void testGetRsusWithCredentials_NearestNeighborOnly_Success() {
        RsuSnmpCredentials cred1 = new RsuSnmpCredentials(100, "192.168.2.1", "nnUser", "nnPass", "nnEncPass", "SNMPv3",
                "11111");
        List<RsuSnmpCredentials> expectedCredentials = Collections.singletonList(cred1);

        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenReturn(expectedCredentials);

        List<RsuSnmpCredentials> result = service.getRsusWithCredentials(true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getRsu_id());
        assertEquals("192.168.2.1", result.get(0).getIpv4_address());
        assertEquals("nnUser", result.get(0).getUsername());
        assertEquals("11111", result.get(0).getIntersection_id());
        verify(entityManager).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery).getResultList();
    }

    @Test
    public void testGetRsusWithCredentials_AllRsus_EmptyResult() {
        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenReturn(Collections.emptyList());

        List<RsuSnmpCredentials> result = service.getRsusWithCredentials(false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(entityManager).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery).getResultList();
    }

    @Test
    public void testGetRsusWithCredentials_NearestNeighborOnly_EmptyResult() {
        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenReturn(Collections.emptyList());

        List<RsuSnmpCredentials> result = service.getRsusWithCredentials(true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(entityManager).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery).getResultList();
    }

    @Test
    public void testGetRsusWithCredentials_AllRsus_WithNullIntersectionId() {
        RsuSnmpCredentials credWithNullIntersection = new RsuSnmpCredentials(3, "192.168.1.3", "user3", "pass3",
                "encPass3", "SNMPv3", null);
        List<RsuSnmpCredentials> expectedCredentials = Collections.singletonList(credWithNullIntersection);

        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenReturn(expectedCredentials);

        List<RsuSnmpCredentials> result = service.getRsusWithCredentials(false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getRsu_id());
        assertNull(result.get(0).getIntersection_id());
        verify(entityManager).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery).getResultList();
    }

    @Test
    public void testGetRsusWithCredentials_QueryExecutionException() {
        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> service.getRsusWithCredentials(false));
        verify(entityManager).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery).getResultList();
    }

    @Test
    public void testGetNNMonitoredRsus_Success() {
        RsuData rsuData1 = new RsuData(200, "192.168.3.1", "22222");
        RsuData rsuData2 = new RsuData(201, "192.168.3.2", "33333");
        List<RsuData> expectedData = Arrays.asList(rsuData1, rsuData2);

        when(entityManager.createQuery(anyString(), eq(RsuData.class))).thenReturn(rsuDataQuery);
        when(rsuDataQuery.getResultList()).thenReturn(expectedData);

        List<RsuData> result = service.getNNMonitoredRsus();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(200, result.get(0).getRsu_id());
        assertEquals("192.168.3.1", result.get(0).getIpv4_address());
        assertEquals("22222", result.get(0).getIntersection_id());
        assertEquals(201, result.get(1).getRsu_id());
        verify(entityManager).createQuery(anyString(), eq(RsuData.class));
        verify(rsuDataQuery).getResultList();
    }

    @Test
    public void testGetNNMonitoredRsus_EmptyResult() {
        when(entityManager.createQuery(anyString(), eq(RsuData.class))).thenReturn(rsuDataQuery);
        when(rsuDataQuery.getResultList()).thenReturn(Collections.emptyList());

        List<RsuData> result = service.getNNMonitoredRsus();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(entityManager).createQuery(anyString(), eq(RsuData.class));
        verify(rsuDataQuery).getResultList();
    }

    @Test
    public void testGetNNMonitoredRsus_SingleResult() {
        RsuData rsuData = new RsuData(300, "192.168.4.1", "44444");
        List<RsuData> expectedData = Collections.singletonList(rsuData);

        when(entityManager.createQuery(anyString(), eq(RsuData.class))).thenReturn(rsuDataQuery);
        when(rsuDataQuery.getResultList()).thenReturn(expectedData);

        List<RsuData> result = service.getNNMonitoredRsus();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(300, result.get(0).getRsu_id());
        assertEquals("192.168.4.1", result.get(0).getIpv4_address());
        assertEquals("44444", result.get(0).getIntersection_id());
        verify(entityManager).createQuery(anyString(), eq(RsuData.class));
        verify(rsuDataQuery).getResultList();
    }

    @Test
    public void testGetNNMonitoredRsus_QueryExecutionException() {
        when(entityManager.createQuery(anyString(), eq(RsuData.class))).thenReturn(rsuDataQuery);
        when(rsuDataQuery.getResultList()).thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> service.getNNMonitoredRsus());
        verify(entityManager).createQuery(anyString(), eq(RsuData.class));
        verify(rsuDataQuery).getResultList();
    }

    @Test
    public void testGetRsusWithCredentials_VerifyDifferentQueriesForNearestNeighbor() {
        when(entityManager.createQuery(anyString(), eq(RsuSnmpCredentials.class))).thenReturn(rsuCredentialsQuery);
        when(rsuCredentialsQuery.getResultList()).thenReturn(Collections.emptyList());

        service.getRsusWithCredentials(false);
        service.getRsusWithCredentials(true);

        // Verify that createQuery was called twice (once for each query type)
        verify(entityManager, times(2)).createQuery(anyString(), eq(RsuSnmpCredentials.class));
        verify(rsuCredentialsQuery, times(2)).getResultList();
    }
}
