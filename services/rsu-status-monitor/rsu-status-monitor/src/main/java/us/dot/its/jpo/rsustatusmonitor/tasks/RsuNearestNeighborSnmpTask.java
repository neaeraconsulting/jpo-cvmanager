package us.dot.its.jpo.rsustatusmonitor.tasks;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.RsuMsgfwdValues;
import us.dot.its.jpo.rsustatusmonitor.services.PostgresService;
import us.dot.its.jpo.rsustatusmonitor.services.RsuNearestNeighborService;
import us.dot.its.jpo.rsustatusmonitor.snmp.NearestNeighborProperties;
import us.dot.its.jpo.rsustatusmonitor.utils.SnmpHelperUtil;

/*
 * This Task is responsible for compiling a list of RSU's that are configured for nearest neighbor evaluation and configuring them all for daily forwarding.
 * Configuring the actual forwarding is performed asynchronously by the RsuNearestNeighborService
 */
@Component
@Slf4j
public class RsuNearestNeighborSnmpTask {

    private PostgresService postgresService;
    private RsuNearestNeighborService rsuNearestNeighborService;
    private NearestNeighborProperties nearestNeighborProperties;

    @Autowired
    public RsuNearestNeighborSnmpTask(PostgresService postgresService,
            RsuNearestNeighborService rsuNearestNeighborService,
            NearestNeighborProperties nearestNeighborProperties) {
        this.postgresService = postgresService;
        this.rsuNearestNeighborService = rsuNearestNeighborService;
        this.nearestNeighborProperties = nearestNeighborProperties;
    }

    // Every 30 seconds run this
    @Scheduled(fixedRate = 30000)
    public void configureNNForwardingRules() {
        // Retrieve all RSU SNMP Credentials for devices with nearest neighbor
        // monitoring enabled that have intersection IDs
        List<RsuSnmpCredentials> credentials = postgresService.getRsusWithCredentials(true);
        if (credentials.isEmpty()) {
            log.warn("No RSU SNMP Credentials found matching the requirements");
            return;
        }

        // Determine all configuration values based on number of RSUs to reduce
        // concurrent UDP traffic ingest at any one point in time
        Map<String, RsuMsgfwdValues> msgfwdConfigValues = new HashMap<>();

        // Starts 5 minutes from now ~ TODO: Change this to 1 hour
        // Round to the minute (zero out seconds and nanoseconds)
        LocalDateTime baseStartTime = LocalDateTime.now(java.time.ZoneOffset.UTC)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(5);
        // Calculate available minutes until midnight UTC
        int availableMinutesUntilMidnight = (24 * 60)
                - (baseStartTime.getHour() * 60 + baseStartTime.getMinute());

        int rsuIndex = 0;
        for (RsuSnmpCredentials cred : credentials) {
            // Calculate start time with wrap-around logic for midnight
            int minutesOffset = rsuIndex % availableMinutesUntilMidnight;
            LocalDateTime startTime = baseStartTime.plusMinutes(minutesOffset);

            // Calculate stop time: start time + 1 minute
            LocalDateTime stopTime = startTime.plusMinutes(1);

            // Generate hex timestamps
            String startHex = SnmpHelperUtil.generateNtcip1218HexDateTimeString(startTime);
            String stopHex = SnmpHelperUtil.generateNtcip1218HexDateTimeString(stopTime);

            // Build out the RsuReceivedMsg Values object
            RsuMsgfwdValues msgfwdValues = new RsuMsgfwdValues();
            msgfwdValues.setRsuSnmpCredentials(cred);
            msgfwdValues.setRsuReceivedMsgPsid(nearestNeighborProperties.getPsid());
            msgfwdValues.setRsuReceivedMsgDestIpAddr(nearestNeighborProperties.getDestinationIp());
            msgfwdValues.setRsuReceivedMsgDestPort(nearestNeighborProperties.getPort());
            msgfwdValues.setRsuReceivedMsgProtocol(nearestNeighborProperties.getProtocol());
            msgfwdValues.setRsuReceivedMsgRssi(nearestNeighborProperties.getRssi());
            msgfwdValues.setRsuReceivedMsgInterval(nearestNeighborProperties.getInterval());
            msgfwdValues.setRsuReceivedMsgDeliveryStart(startHex);
            msgfwdValues.setRsuReceivedMsgDeliveryStop(stopHex);
            msgfwdValues.setRsuReceivedMsgStatus(4);
            msgfwdValues.setRsuReceivedMsgSecure(nearestNeighborProperties.getSecure());
            msgfwdValues.setRsuReceivedMsgAuthMsgInterval(nearestNeighborProperties.getAuthMsgInterval());

            log.debug("Configuring RSU {} with forwarding values: {}", cred.getIntersection_id(),
                    msgfwdValues);

            msgfwdConfigValues.put(cred.getIntersection_id(), msgfwdValues);
            rsuIndex++;

            // Run Asynchronous Task to Actually setup the message forward
            rsuNearestNeighborService.configureRsuForwarding(msgfwdValues);
        }
    }
}
