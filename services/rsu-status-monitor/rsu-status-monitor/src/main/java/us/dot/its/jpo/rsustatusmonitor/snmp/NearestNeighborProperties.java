package us.dot.its.jpo.rsustatusmonitor.snmp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Kafka topics.
 */
@Configuration
@ConfigurationProperties(prefix = "rsu-status-monitor.snmp.nearest-neighbor-monitor")
@Data
public class NearestNeighborProperties {
    private String psid;
    private String destinationIp;
    private int port;
    private int protocol;
    private int rssi;
    private int interval;
    private int secure;
    private int authMsgInterval;
}
