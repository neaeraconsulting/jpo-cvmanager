package us.dot.its.jpo.ode.api.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;
import us.dot.its.jpo.ode.api.models.IntersectionReferenceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.dot.its.jpo.ode.api.models.ReportDocument;
import us.dot.its.jpo.ode.api.services.ReportService;
import us.dot.its.jpo.ode.api.services.EmailService;

@Component
@ConditionalOnProperty(name = "enable.report-email", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class ReportEmailTask {

    private static final Logger log = LoggerFactory.getLogger(ReportEmailTask.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final us.dot.its.jpo.ode.api.accessors.reports.ReportRepository reportRepo;
    private final us.dot.its.jpo.ode.api.accessors.map.ProcessedMapRepository processedMapRepo;
    private final EmailService emailService;

    private static final String WEEKLY_EMAIL_CRON = "0 0 1 ? * SUN"; // Runs weekly at 1 am on Sundays

    @Scheduled(cron = WEEKLY_EMAIL_CRON)
    public void sendWeeklyReportEmails() {
        log.info("Sending Weekly Report Emails at {}", dateFormat.format(new Date()));
        ZonedDateTime now = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime expectedStartTime = now.minusWeeks(1);
        ZonedDateTime expectedStopTime = now;
        sendEmailsForReportsInRange(expectedStartTime.toInstant(), expectedStopTime.toInstant());
    }

    void sendEmailsForReportsInRange(Instant expectedStartTime, Instant expectedStopTime) {
        log.info("Fetching users subscribed to Conflict Monitor Reports...");
        List<String> users = emailService.getUsersForConflictMonitorReports();

        Map<Integer, ReportDocument> reportCache = new HashMap<>();

        for (String user : users) {
            log.info("Processing user: {}", user);

            List<Integer> allowedIntersections = emailService.getAllowedIntersectionIdsByEmail(user);

            List<Integer> validIntersectionIds = fetchReportsForIntersections(allowedIntersections, expectedStartTime,
                    expectedStopTime, reportCache);

            if (validIntersectionIds.isEmpty()) {
                continue;
            }

            String emailBody = constructEmailBody(validIntersectionIds, reportCache, expectedStartTime,
                    expectedStopTime);

            String subject = String.format("Weekly Conflict Monitor Reports (%s)",
                    ZonedDateTime.now(ZoneOffset.UTC).toLocalDate());
            emailService.sendSimpleMessage(user, subject, emailBody);
        }
    }

    List<Integer> fetchReportsForIntersections(List<Integer> intersectionIds, Instant startTime,
            Instant stopTime, Map<Integer, ReportDocument> reportCache) {
        long startRange = startTime.toEpochMilli();
        long endRange = stopTime.toEpochMilli();

        List<Integer> processedIntersectionIds = new ArrayList<>();

        for (Integer intersectionId : intersectionIds) {
            if (reportCache.containsKey(intersectionId)) {
                processedIntersectionIds.add(intersectionId);
                continue;
            }

            ReportDocument report = reportRepo.findByIntersectionAndExactTime(
                    null,
                    intersectionId,
                    startRange,
                    endRange,
                    true);

            if (report != null) {
                reportCache.put(intersectionId, report);
                processedIntersectionIds.add(intersectionId);
            }
        }

        return processedIntersectionIds;
    }

    String constructEmailBody(List<Integer> validIntersectionIds, Map<Integer, ReportDocument> reportCache,
            Instant startTime, Instant stopTime) {
        List<Map<String, Object>> reports = new ArrayList<>();

        for (Integer intersectionId : validIntersectionIds) {
            ReportDocument report = reportCache.get(intersectionId);

            if (report != null) {
                Map<String, Object> reportEntry = new HashMap<>();
                reportEntry.put("intersectionId", intersectionId);
                reportEntry.put("report", serializeReportToJson(report));
                reports.add(reportEntry);
            }
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(reports);
        } catch (JsonProcessingException e) {
            log.error("Error serializing reports to JSON array", e);
            return "[]"; // Return an empty JSON array in case of error
        }
    }

    private String serializeReportToJson(ReportDocument report) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            log.error("Error serializing report to JSON", e);
            return "{}";
        }
    }
}