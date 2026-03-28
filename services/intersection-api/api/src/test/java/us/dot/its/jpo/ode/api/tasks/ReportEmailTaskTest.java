package us.dot.its.jpo.ode.api.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.dot.its.jpo.ode.api.services.EmailService;
import us.dot.its.jpo.ode.api.accessors.reports.ReportRepository;
import us.dot.its.jpo.ode.api.models.ReportDocument;
import us.dot.its.jpo.ode.api.accessors.map.ProcessedMapRepository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportEmailTaskTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ReportRepository reportRepo;

    @Mock
    private ProcessedMapRepository processedMapRepo;

    @InjectMocks
    private ReportEmailTask reportEmailTask;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendEmailsForReportsInRange() {
        // Arrange
        Instant startTime = Instant.now().minusSeconds(604800); // 1 week ago
        Instant stopTime = Instant.now();

        List<String> mockUsers = List.of("user1@example.com");
        when(emailService.getUsersForConflictMonitorReports()).thenReturn(mockUsers);

        List<Integer> mockIntersectionIds = List.of(1);
        when(emailService.getAllowedIntersectionIdsByEmail("user1@example.com")).thenReturn(mockIntersectionIds);

        when(reportRepo.findByIntersectionAndExactTime(
                eq(null),
                eq(1),
                eq(startTime.toEpochMilli()),
                eq(stopTime.toEpochMilli()),
                eq(true))).thenReturn(new ReportDocument());

        // Act
        reportEmailTask.sendEmailsForReportsInRange(startTime, stopTime);

        // Assert
        verify(emailService, times(1)).getAllowedIntersectionIdsByEmail("user1@example.com");
        verify(emailService, times(1)).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void testSendWeeklyReportEmails() {
        // Arrange
        List<String> mockUsers = List.of("user1@example.com", "user2@example.com");
        when(emailService.getUsersForConflictMonitorReports()).thenReturn(mockUsers);

        List<Integer> mockIntersectionIds = List.of(1);
        when(emailService.getAllowedIntersectionIdsByEmail(anyString())).thenReturn(mockIntersectionIds);

        when(reportRepo.findByIntersectionAndExactTime(
                eq(null),
                eq(1),
                anyLong(),
                anyLong(),
                eq(true))).thenReturn(new ReportDocument());

        // Act
        reportEmailTask.sendWeeklyReportEmails();

        // Assert
        verify(emailService, times(2)).getAllowedIntersectionIdsByEmail(anyString());
        verify(emailService, times(2)).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void testFetchReportsForIntersections() {
        // Arrange
        Instant startTime = Instant.now().minusSeconds(604800); // 1 week ago
        Instant stopTime = Instant.now();
        Map<Integer, ReportDocument> reportCache = new HashMap<>();

        List<Integer> intersectionIds = List.of(1, 2);

        // Mock the repository method
        ReportDocument mockReport = new ReportDocument();
        when(reportRepo.findByIntersectionAndExactTime(
                eq(null),
                anyInt(),
                eq(startTime.toEpochMilli()),
                eq(stopTime.toEpochMilli()),
                eq(true)))
                .thenReturn(mockReport);

        // Act
        List<Integer> result = reportEmailTask.fetchReportsForIntersections(intersectionIds, startTime, stopTime,
                reportCache);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Integer> intersectionIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> startTimeCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endTimeCaptor = ArgumentCaptor.forClass(Long.class);

        verify(reportRepo, times(intersectionIds.size())).findByIntersectionAndExactTime(
                eq(null),
                intersectionIdCaptor.capture(),
                startTimeCaptor.capture(),
                endTimeCaptor.capture(),
                eq(true));

        // Verify captured arguments
        assertEquals(intersectionIds, intersectionIdCaptor.getAllValues());
        assertTrue(startTimeCaptor.getAllValues().stream().allMatch(time -> time.equals(startTime.toEpochMilli())));
        assertTrue(endTimeCaptor.getAllValues().stream().allMatch(time -> time.equals(stopTime.toEpochMilli())));

        // Verify the cache contains the mock report
        for (Integer intersectionId : intersectionIds) {
            assertEquals(mockReport, reportCache.get(intersectionId));
        }
    }

    @Test
    void testConstructEmailBody() {
        // Arrange
        ReportDocument report1 = new ReportDocument();
        ReportDocument report2 = new ReportDocument();
        Map<Integer, ReportDocument> reportCache = Map.of(1, report1, 2, report2);
        List<Integer> validIntersectionIds = List.of(1, 2);
        Instant startTime = Instant.now().minusSeconds(604800); // 1 week ago
        Instant stopTime = Instant.now();

        // Act
        String emailBody = reportEmailTask.constructEmailBody(validIntersectionIds, reportCache, startTime, stopTime);

        // Assert
        assertTrue(emailBody.contains("intersectionId"));
        assertTrue(emailBody.contains("report"));
    }
}