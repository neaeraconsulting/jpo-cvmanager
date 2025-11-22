package us.dot.its.jpo.rsustatusmonitor.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.utils.DateJsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class IntersectionStatusQueryServiceTest {

    @Mock
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Mock
    private KafkaStreams kafkaStreams;

    @Mock
    private ReadOnlyKeyValueStore<String, String> keyValueStore;

    @Mock
    private KeyValueIterator<String, String> keyValueIterator;

    private IntersectionStatusQueryService service;

    @BeforeEach
    public void setup() {
        service = new IntersectionStatusQueryService(streamsBuilderFactoryBean);
    }

    @Test
    public void testGetIntersectionStatus_Success() throws JsonProcessingException {
        String intersectionId = "12345";
        IntersectionStatusRecord expectedRecord = new IntersectionStatusRecord(12345, "192.168.1.1",
                "2025-11-21T10:00:00Z");
        String jsonValue = DateJsonMapper.getInstance().writeValueAsString(expectedRecord);

        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(keyValueStore);
        when(keyValueStore.get(intersectionId)).thenReturn(jsonValue);

        IntersectionStatusRecord result = service.getIntersectionStatus(intersectionId);

        assertNotNull(result);
        assertEquals(12345, result.getIntersectionId());
        assertEquals("192.168.1.1", result.getListenerIp());
        assertEquals("2025-11-21T10:00:00Z", result.getReceivedAt());
    }

    @Test
    public void testGetIntersectionStatus_NotFound() {
        String intersectionId = "99999";

        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(keyValueStore);
        when(keyValueStore.get(intersectionId)).thenReturn(null);

        IntersectionStatusRecord result = service.getIntersectionStatus(intersectionId);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionStatus_KafkaStreamsNotAvailable() {
        String intersectionId = "12345";
        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(null);

        IntersectionStatusRecord result = service.getIntersectionStatus(intersectionId);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionStatus_JsonProcessingException() {
        String intersectionId = "12345";
        String invalidJson = "{ invalid json }";

        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(keyValueStore);
        when(keyValueStore.get(intersectionId)).thenReturn(invalidJson);

        IntersectionStatusRecord result = service.getIntersectionStatus(intersectionId);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionStatus_StoreException() {
        String intersectionId = "12345";

        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenThrow(new RuntimeException("Store error"));

        IntersectionStatusRecord result = service.getIntersectionStatus(intersectionId);

        assertNull(result);
    }

    @Test
    public void testGetAllIntersectionStatuses_Success() throws JsonProcessingException {
        IntersectionStatusRecord record1 = new IntersectionStatusRecord(12345, "192.168.1.1", "2025-11-21T10:00:00Z");
        IntersectionStatusRecord record2 = new IntersectionStatusRecord(67890, "192.168.1.2", "2025-11-21T11:00:00Z");

        String json1 = DateJsonMapper.getInstance().writeValueAsString(record1);
        String json2 = DateJsonMapper.getInstance().writeValueAsString(record2);

        KeyValue<String, String> kv1 = new KeyValue<>("12345", json1);
        KeyValue<String, String> kv2 = new KeyValue<>("67890", json2);

        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(keyValueStore);
        when(keyValueStore.all()).thenReturn(keyValueIterator);

        // Mock forEachRemaining to actually invoke the consumer
        doAnswer(invocation -> {
            java.util.function.Consumer<KeyValue<String, String>> action = invocation.getArgument(0);
            action.accept(kv1);
            action.accept(kv2);
            return null;
        }).when(keyValueIterator).forEachRemaining(any());

        Map<String, IntersectionStatusRecord> result = service.getAllIntersectionStatuses();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("12345"));
        assertTrue(result.containsKey("67890"));
        assertEquals(12345, result.get("12345").getIntersectionId());
        assertEquals(67890, result.get("67890").getIntersectionId());
    }

    @Test
    public void testGetAllIntersectionStatuses_EmptyStore() {
        Map<String, IntersectionStatusRecord> result = service.getAllIntersectionStatuses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAllIntersectionStatuses_KafkaStreamsNotAvailable() {
        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(null);

        Map<String, IntersectionStatusRecord> result = service.getAllIntersectionStatuses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAllIntersectionStatuses_WithInvalidJson() throws JsonProcessingException {
        IntersectionStatusRecord validRecord = new IntersectionStatusRecord(12345, "192.168.1.1",
                "2025-11-21T10:00:00Z");
        String validJson = DateJsonMapper.getInstance().writeValueAsString(validRecord);
        String invalidJson = "{ invalid json }";

        KeyValue<String, String> kv1 = new KeyValue<>("12345", validJson);
        KeyValue<String, String> kv2 = new KeyValue<>("67890", invalidJson);

        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenReturn(keyValueStore);
        when(keyValueStore.all()).thenReturn(keyValueIterator);

        // Mock forEachRemaining to actually invoke the consumer
        doAnswer(invocation -> {
            java.util.function.Consumer<KeyValue<String, String>> action = invocation.getArgument(0);
            action.accept(kv1);
            action.accept(kv2);
            return null;
        }).when(keyValueIterator).forEachRemaining(any());

        Map<String, IntersectionStatusRecord> result = service.getAllIntersectionStatuses();

        assertNotNull(result);
        assertEquals(1, result.size()); // Only valid record should be included
        assertTrue(result.containsKey("12345"));
        assertFalse(result.containsKey("67890"));
    }

    @Test
    public void testGetAllIntersectionStatuses_StoreException() {
        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any(StoreQueryParameters.class))).thenThrow(new RuntimeException("Store error"));

        Map<String, IntersectionStatusRecord> result = service.getAllIntersectionStatuses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
