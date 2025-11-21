package us.dot.its.jpo.rsustatusmonitor.tasks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.rsustatusmonitor.kafka.KafkaTopics;
import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.models.NearestNeighborUnresponsiveEvent;
import us.dot.its.jpo.rsustatusmonitor.models.NearestNeighborUnresponsiveNotification;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuData;
import us.dot.its.jpo.rsustatusmonitor.services.IntersectionStatusQueryService;
import us.dot.its.jpo.rsustatusmonitor.services.PostgresService;
import us.dot.its.jpo.rsustatusmonitor.utils.DateJsonMapper;

/*
 * This Task is responsible for checking what RSU units have not been heard from in the last 24 hours, and issuing events and notifications for RSU units that are missing.
 */
@Component
@ConditionalOnProperty(name = "enable.nearest-neighbor", havingValue = "true", matchIfMissing = false)
@Slf4j
public class NearestNeighborStatusCheckTask {

    private IntersectionStatusQueryService queryService;
    private KafkaTemplate<String, String> kafkaPublisher;
    private KafkaTopics kafkaTopics;
    private PostgresService postgresService;

    @Autowired
    public NearestNeighborStatusCheckTask(IntersectionStatusQueryService queryService,
            PostgresService postgresService, KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopics kafkaTopics) {
        this.queryService = queryService;
        this.postgresService = postgresService;
        this.kafkaPublisher = kafkaTemplate;
        this.kafkaTopics = kafkaTopics;
    }

    // Every 30 seconds run this
    @Scheduled(fixedRate = 30000)
    public void checkNearestNeighborStatus() {
        log.debug("Checking nearest neighbor status at {}", LocalDateTime.now());

        // Retrieve all RSUs with nearest neighbor monitoring enabled with an
        // intersection id
        List<RsuData> rsus = postgresService.getNNMonitoredRsus();
        if (rsus.isEmpty()) {
            log.warn("No RSUs found matching the requirements");
            return;
        }

        for (RsuData rsu : rsus) {
            String intersectionId = rsu.getIntersection_id();
            if (intersectionId == null || intersectionId.isEmpty()) {
                log.warn("RSU ID {} does not have a valid intersection ID, skipping", rsu.getRsu_id());
                continue;
            }

            // Query the latest status for the intersection
            IntersectionStatusRecord status = queryService.getIntersectionStatus(intersectionId);
            if (status == null) {
                log.warn("No intersection status found for intersection ID: {}", intersectionId);
                alertNNUnresponsiveEvent(intersectionId);
                continue;
            }

            // Verify status was received within the last 24 hours
            if (!isTimestampRecent(status.getReceivedAt(), 24 * 60)) {
                log.warn("Intersection ID {} has not received status updates in the last 24 hours", intersectionId);
                alertNNUnresponsiveEvent(intersectionId);
            } else {
                log.info("Intersection ID {} has received a status update in the last 24 hours", intersectionId);
            }
        }
    }

    private boolean isTimestampRecent(String receivedAtString, long thresholdMinutes) {
        try {
            // Parse the ISO_LOCAL_DATE_TIME string (assumes it's in UTC)
            LocalDateTime receivedAt = LocalDateTime.parse(receivedAtString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Calculate the duration between the two times
            Duration duration = Duration.between(receivedAt, LocalDateTime.now(ZoneOffset.UTC));

            // Check if the absolute difference is within the threshold
            return Math.abs(duration.toMinutes()) <= thresholdMinutes;

        } catch (Exception e) {
            log.error("Error parsing timestamp: {}", receivedAtString, e);
            return false;
        }
    }

    private void alertNNUnresponsiveEvent(String intersectionId) {
        NearestNeighborUnresponsiveEvent event = new NearestNeighborUnresponsiveEvent();
        event.setIntersectionID(Integer.valueOf(intersectionId));
        NearestNeighborUnresponsiveNotification notification = new NearestNeighborUnresponsiveNotification();
        notification.setEvent(event);
        try {
            kafkaPublisher.send(kafkaTopics.getNearestNeighborUnresponsiveEvent(), intersectionId.toString(),
                    DateJsonMapper.getInstance().writeValueAsString(event));
            kafkaPublisher.send(kafkaTopics.getNearestNeighborUnresponsiveNotification(), intersectionId.toString(),
                    DateJsonMapper.getInstance().writeValueAsString(notification));
        } catch (Exception e) {
            log.error("Error serializing NNUnresponsiveEvent to JSON", e);
        }
    }
}