package us.dot.its.jpo.rsustatusmonitor.rest;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import us.dot.its.jpo.rsustatusmonitor.TestConfig;
import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.services.IntersectionStatusQueryService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = IntersectionStatusController.class)
@Import(TestConfig.class)
public class IntersectionStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntersectionStatusQueryService queryService;
    private IntersectionStatusRecord testRecord;

    @BeforeEach
    public void setup() {
        testRecord = new IntersectionStatusRecord(12345, "192.168.1.1", "2025-11-21T10:00:00Z");
    }

    @Test
    public void testGetIntersectionStatus_Found() throws Exception {
        String intersectionId = "12345";
        when(queryService.getIntersectionStatus(intersectionId)).thenReturn(testRecord);

        mockMvc.perform(get("/api/intersection-status/{intersectionId}", intersectionId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.intersectionId", is(12345)))
                .andExpect(jsonPath("$.listenerIp", is("192.168.1.1")))
                .andExpect(jsonPath("$.receivedAt", is("2025-11-21T10:00:00Z")));
    }

    @Test
    public void testGetIntersectionStatus_NotFound() throws Exception {
        String intersectionId = "99999";
        when(queryService.getIntersectionStatus(intersectionId)).thenReturn(null);

        mockMvc.perform(get("/api/intersection-status/{intersectionId}", intersectionId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAllIntersectionStatuses_Success() throws Exception {
        Map<String, IntersectionStatusRecord> statuses = new HashMap<>();
        statuses.put("12345", testRecord);
        statuses.put("67890", new IntersectionStatusRecord(67890, "192.168.1.2", "2025-11-21T11:00:00Z"));

        when(queryService.getAllIntersectionStatuses()).thenReturn(statuses);

        mockMvc.perform(get("/api/intersection-status/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.['12345'].intersectionId", is(12345)))
                .andExpect(jsonPath("$.['12345'].listenerIp", is("192.168.1.1")))
                .andExpect(jsonPath("$.['67890'].intersectionId", is(67890)))
                .andExpect(jsonPath("$.['67890'].listenerIp", is("192.168.1.2")));
    }

    @Test
    public void testGetAllIntersectionStatuses_EmptyMap() throws Exception {
        Map<String, IntersectionStatusRecord> emptyStatuses = new HashMap<>();
        when(queryService.getAllIntersectionStatuses()).thenReturn(emptyStatuses);

        mockMvc.perform(get("/api/intersection-status/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", anEmptyMap()));
    }

    @Test
    public void testGetIntersectionStatus_WithDifferentIntersectionIds() throws Exception {
        String[] testIds = { "123", "99999", "abc123" };

        for (String id : testIds) {
            IntersectionStatusRecord record = new IntersectionStatusRecord(
                    Integer.parseInt(id.replaceAll("[^0-9]", "0")),
                    "192.168.1.100",
                    "2025-11-21T12:00:00Z");
            when(queryService.getIntersectionStatus(id)).thenReturn(record);

            mockMvc.perform(get("/api/intersection-status/{intersectionId}", id))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
