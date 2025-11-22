package us.dot.its.jpo.rsustatusmonitor.tasks;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.rsustatusmonitor.kafka.KafkaTopics;
import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuData;
import us.dot.its.jpo.rsustatusmonitor.services.IntersectionStatusQueryService;
import us.dot.its.jpo.rsustatusmonitor.services.PostgresService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NearestNeighborStatusCheckTaskTest {

    @Mock
    private IntersectionStatusQueryService queryService;

    @Mock
    private PostgresService postgresService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopics kafkaTopics;

    private NearestNeighborStatusCheckTask task;

    @BeforeEach
    public void setup() {
        task = new NearestNeighborStatusCheckTask(queryService, postgresService, kafkaTemplate, kafkaTopics);

        // Setup default kafka topics (lenient as not all tests trigger Kafka messages)
        lenient().when(kafkaTopics.getNearestNeighborUnresponsiveEvent()).thenReturn("nn-unresponsive-event");
        lenient().when(kafkaTopics.getNearestNeighborUnresponsiveNotification())
                .thenReturn("nn-unresponsive-notification");
    }

    @Test
    public void testCheckNearestNeighborStatus_NoRsusFound() {
        when(postgresService.getNNMonitoredRsus()).thenReturn(new ArrayList<>());

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService, never()).getIntersectionStatus(anyString());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_RsuWithNullIntersectionId() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(1, "192.168.1.1", null);
        rsus.add(rsu);

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService, never()).getIntersectionStatus(anyString());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_RsuWithEmptyIntersectionId() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(2, "192.168.1.2", "");
        rsus.add(rsu);

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService, never()).getIntersectionStatus(anyString());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_NoIntersectionStatusFound() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(3, "192.168.1.3", "12345");
        rsus.add(rsu);

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("12345")).thenReturn(null);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("12345");
        verify(kafkaTemplate, times(2)).send(anyString(), eq("12345"), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_RecentTimestamp() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(4, "192.168.1.4", "54321");
        rsus.add(rsu);

        IntersectionStatusRecord status = new IntersectionStatusRecord();
        // Set timestamp to 1 hour ago
        LocalDateTime oneHourAgo = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        status.setReceivedAt(oneHourAgo.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("54321")).thenReturn(status);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("54321");
        // Should not send alert for recent timestamp
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_OldTimestamp() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(5, "192.168.1.5", "99999");
        rsus.add(rsu);

        IntersectionStatusRecord status = new IntersectionStatusRecord();
        // Set timestamp to 25 hours ago (beyond 24 hour threshold)
        LocalDateTime twentyFiveHoursAgo = LocalDateTime.now(ZoneOffset.UTC).minusHours(25);
        status.setReceivedAt(twentyFiveHoursAgo.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("99999")).thenReturn(status);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("99999");
        // Should send alert for old timestamp
        verify(kafkaTemplate, times(2)).send(anyString(), eq("99999"), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_ExactlyAtThreshold() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(6, "192.168.1.6", "11111");
        rsus.add(rsu);

        IntersectionStatusRecord status = new IntersectionStatusRecord();
        // Set timestamp to exactly 24 hours ago
        LocalDateTime exactlyTwentyFourHours = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);
        status.setReceivedAt(exactlyTwentyFourHours.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("11111")).thenReturn(status);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("11111");
        // At exactly 24 hours (1440 minutes), should still be within threshold
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_MultipleRsus() {
        List<RsuData> rsus = new ArrayList<>();

        RsuData rsu1 = new RsuData(7, "192.168.1.7", "10001");
        rsus.add(rsu1);

        RsuData rsu2 = new RsuData(8, "192.168.1.8", "10002");
        rsus.add(rsu2);

        RsuData rsu3 = new RsuData(9, "192.168.1.9", "10003");
        rsus.add(rsu3);

        IntersectionStatusRecord recentStatus = new IntersectionStatusRecord();
        recentStatus.setReceivedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        IntersectionStatusRecord oldStatus = new IntersectionStatusRecord();
        oldStatus.setReceivedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(30)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("10001")).thenReturn(recentStatus);
        when(queryService.getIntersectionStatus("10002")).thenReturn(oldStatus);
        when(queryService.getIntersectionStatus("10003")).thenReturn(null);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("10001");
        verify(queryService).getIntersectionStatus("10002");
        verify(queryService).getIntersectionStatus("10003");
        // Should send alerts for 10002 and 10003 (4 messages total: 2 per intersection)
        verify(kafkaTemplate, times(4)).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_InvalidTimestampFormat() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(10, "192.168.1.10", "20001");
        rsus.add(rsu);

        IntersectionStatusRecord status = new IntersectionStatusRecord();
        status.setReceivedAt("invalid-timestamp-format");

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("20001")).thenReturn(status);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("20001");
        // Invalid timestamp should be treated as false (not recent), triggering alert
        verify(kafkaTemplate, times(2)).send(anyString(), eq("20001"), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_KafkaMessageContent() throws Exception {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(11, "192.168.1.11", "30001");
        rsus.add(rsu);

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("30001")).thenReturn(null);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        task.checkNearestNeighborStatus();

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());

        List<String> topics = topicCaptor.getAllValues();
        List<String> keys = keyCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();

        // Verify both messages sent
        assertEquals(2, topics.size());
        assertEquals("nn-unresponsive-event", topics.get(0));
        assertEquals("nn-unresponsive-notification", topics.get(1));

        // Verify keys
        assertEquals("30001", keys.get(0));
        assertEquals("30001", keys.get(1));

        // Verify messages contain intersection ID
        assertTrue(messages.get(0).contains("30001"));
        assertTrue(messages.get(1).contains("30001"));
    }

    @Test
    public void testCheckNearestNeighborStatus_FutureTimestamp() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(12, "192.168.1.12", "40001");
        rsus.add(rsu);

        IntersectionStatusRecord status = new IntersectionStatusRecord();
        // Set timestamp to 1 hour in the future (should still be considered recent)
        LocalDateTime oneHourFuture = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        status.setReceivedAt(oneHourFuture.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("40001")).thenReturn(status);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("40001");
        // Future timestamp should be within threshold due to Math.abs()
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testCheckNearestNeighborStatus_JustOverThreshold() {
        List<RsuData> rsus = new ArrayList<>();
        RsuData rsu = new RsuData(13, "192.168.1.13", "50001");
        rsus.add(rsu);

        IntersectionStatusRecord status = new IntersectionStatusRecord();
        // Set timestamp to 24 hours and 1 minute ago (just over threshold)
        LocalDateTime justOverThreshold = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1441);
        status.setReceivedAt(justOverThreshold.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        when(postgresService.getNNMonitoredRsus()).thenReturn(rsus);
        when(queryService.getIntersectionStatus("50001")).thenReturn(status);

        task.checkNearestNeighborStatus();

        verify(postgresService).getNNMonitoredRsus();
        verify(queryService).getIntersectionStatus("50001");
        // Should send alert as it's over the 1440 minute threshold
        verify(kafkaTemplate, times(2)).send(anyString(), eq("50001"), anyString());
    }

    @Test
    public void testConstructor() {
        NearestNeighborStatusCheckTask newTask = new NearestNeighborStatusCheckTask(
                queryService, postgresService, kafkaTemplate, kafkaTopics);
        assertNotNull(newTask);
    }
}
