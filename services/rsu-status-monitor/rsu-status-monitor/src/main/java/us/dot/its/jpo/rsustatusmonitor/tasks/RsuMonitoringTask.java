package us.dot.its.jpo.rsustatusmonitor.tasks;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;
import us.dot.its.jpo.rsustatusmonitor.services.PostgresService;
import us.dot.its.jpo.rsustatusmonitor.services.RsuQueryService;

@Component
@ConditionalOnProperty(name = "enable.monitoring", havingValue = "true", matchIfMissing = false)
@Slf4j
public class RsuMonitoringTask {

    private PostgresService postgresService;
    private RsuQueryService rsuQueryService;

    @Autowired
    public RsuMonitoringTask(
            RsuQueryService rsuQueryService,
            PostgresService postgresService) {
        this.rsuQueryService = rsuQueryService;
        this.postgresService = postgresService;

    }

    @Scheduled(fixedRateString = "${monitor.interval}")
    public void queryRSUStats() {
        List<RsuSnmpCredentials> credentials = postgresService.getRsusWithCredentials(false);
        for (RsuSnmpCredentials cred : credentials) {
            rsuQueryService.getRsuInformation(cred);
        }
    }
}