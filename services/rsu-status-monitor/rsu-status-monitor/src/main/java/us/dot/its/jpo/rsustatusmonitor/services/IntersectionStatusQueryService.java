package us.dot.its.jpo.rsustatusmonitor.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.StoreQueryParameters;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.utils.DateJsonMapper;

@Slf4j
@Service
public class IntersectionStatusQueryService {

    public static final String INTERSECTION_STATUS_STORE = "intersection-status-store";

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public IntersectionStatusQueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    /**
     * Query the KTable to get the latest status for a specific intersection ID
     *
     * @param intersectionId the intersection ID to query
     * @return the latest IntersectionStatusRecord or null if not found
     */
    public IntersectionStatusRecord getIntersectionStatus(String intersectionId) {
        try {
            if (streamsBuilderFactoryBean.getKafkaStreams() == null) {
                log.warn("KafkaStreams is not available");
                return null;
            }

            ReadOnlyKeyValueStore<String, String> store = streamsBuilderFactoryBean
                    .getKafkaStreams()
                    .store(StoreQueryParameters.fromNameAndType(INTERSECTION_STATUS_STORE,
                            QueryableStoreTypes.keyValueStore()));

            String jsonValue = store.get(intersectionId);
            if (jsonValue != null) {
                return DateJsonMapper.getInstance().readValue(jsonValue,
                        IntersectionStatusRecord.class);
            }
        } catch (Exception e) {
            log.error("Error querying intersection status for ID: {}", intersectionId,
                    e);
        }
        return null;
    }

    public java.util.Map<String, IntersectionStatusRecord> getAllIntersectionStatuses() {
        java.util.Map<String, IntersectionStatusRecord> result = new java.util.HashMap<>();
        try {
            if (streamsBuilderFactoryBean.getKafkaStreams() == null) {
                log.warn("KafkaStreams is not available");
                return result;
            }

            ReadOnlyKeyValueStore<String, String> store = streamsBuilderFactoryBean
                    .getKafkaStreams()
                    .store(StoreQueryParameters.fromNameAndType(INTERSECTION_STATUS_STORE,
                            QueryableStoreTypes.keyValueStore()));

            store.all().forEachRemaining(keyValue -> {
                try {
                    IntersectionStatusRecord record = DateJsonMapper.getInstance()
                            .readValue(keyValue.value, IntersectionStatusRecord.class);
                    result.put(keyValue.key, record);
                } catch (JsonProcessingException e) {
                    log.error("Error deserializing intersection status for key: {}",
                            keyValue.key, e);
                }
            });
        } catch (Exception e) {
            log.error("Error querying all intersection statuses", e);
        }
        return result;
    }
}
