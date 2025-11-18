package us.dot.its.jpo.rsustatusmonitor;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@Slf4j
public class RsuStatusMonitorProperties {

    final BuildProperties buildProperties;
    // private String rsuStatusKafkaTopic;
    // private String rsuNearestNeighborUnresponsiveEventTopic;
    // private String rsuNearestNeighborUnresponsiveNotificationTopic;

    @Autowired
    public RsuStatusMonitorProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @PostConstruct
    void initialize() {
        log.info("groupId: {}", buildProperties.getGroup());
        log.info("artifactId: {}", buildProperties.getArtifact());
        log.info("version: {}", buildProperties.getVersion());
    }

    public String getVersion() {
        return buildProperties.getVersion();
    }

    // @Value("${rsu-status-monitor.kafka.topics.intersection-status}")
    // public void setRsuStatusKafkaTopic(String rsuStatusKafkaTopic) {
    // this.rsuStatusKafkaTopic = rsuStatusKafkaTopic;
    // }

    // public String getRsuStatusKafkaTopic() {
    // return rsuStatusKafkaTopic;
    // }

    // @Value("${rsu-status-monitor.kafka.topics.nearest-neighbor-unresponsive-event}")
    // public void setRsuNearestNeighborUnresponsiveEventKafkaTopic(String
    // rsuNearestNeighborUnresponsiveEventTopic) {
    // this.rsuNearestNeighborUnresponsiveEventTopic =
    // rsuNearestNeighborUnresponsiveEventTopic;
    // }

    // public String getRsuNearestNeighborUnresponsiveEventKafkaTopic() {
    // return rsuNearestNeighborUnresponsiveEventTopic;
    // }

    // @Value("${rsu-status-monitor.kafka.topics.nearest-neighbor-unresponsive-notification}")
    // public void setRsuNearestNeighborUnresponsiveNotificationKafkaTopic(
    // String rsuNearestNeighborUnresponsiveNotificationTopic) {
    // this.rsuNearestNeighborUnresponsiveEventTopic =
    // rsuNearestNeighborUnresponsiveNotificationTopic;
    // }

    // public String getRsuNearestNeighborUnresponsiveNotificationKafkaTopic() {
    // return rsuNearestNeighborUnresponsiveNotificationTopic;
    // }

}
