package us.dot.its.jpo.rsustatusmonitor.controllers;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.dot.its.jpo.rsustatusmonitor.models.api.RsuModeResponse;
import us.dot.its.jpo.rsustatusmonitor.services.RsuModeService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RsuModeControllerTest {

        @Mock
        private RsuModeService rsuModeService;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        public void setup() {
                objectMapper = new ObjectMapper();
                mockMvc = MockMvcBuilders.standaloneSetup(new RsuModeController(rsuModeService))
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                                .build();
        }

        @Test
        public void testSetRsuMode_ReturnsOk() throws Exception {
                when(rsuModeService.setRsuMode("192.168.1.100", 4))
                                .thenReturn(new RsuModeResponse("192.168.1.100", 4, "success",
                                                "RSU mode updated successfully."));

                mockMvc.perform(post("/api/rsus/mode")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ModeRequestBody("192.168.1.100", 4))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ipAddress").value("192.168.1.100"))
                                .andExpect(jsonPath("$.mode").value(4))
                                .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        public void testSetRsuMode_MissingModeReturnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/rsus/mode")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ModeRequestBody("192.168.1.100", null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(status().reason("Mode is required."));
        }

        @Test
        public void testSetRsuMode_RsuNotFoundReturnsNotFound() throws Exception {
                when(rsuModeService.setRsuMode(eq("192.168.1.200"), eq(2)))
                                .thenThrow(new NoSuchElementException(
                                                "No RSU credentials found for IP address 192.168.1.200"));

                mockMvc.perform(post("/api/rsus/mode")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ModeRequestBody("192.168.1.200", 2))))
                                .andExpect(status().isNotFound())
                                .andExpect(status().reason("No RSU credentials found for IP address 192.168.1.200"));
        }

        @Test
        public void testSetRsuMode_SnmpFailureReturnsBadGateway() throws Exception {
                when(rsuModeService.setRsuMode(eq("192.168.1.100"), eq(4)))
                                .thenThrow(new IOException("Timeout"));

                mockMvc.perform(post("/api/rsus/mode")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ModeRequestBody("192.168.1.100", 4))))
                                .andExpect(status().isBadGateway())
                                .andExpect(status().reason("Failed to update RSU mode."));
        }

        @Test
        public void testGetRsuStatus_ReturnsOk() throws Exception {
                when(rsuModeService.getCurrentRsuStatus("192.168.1.100"))
                                .thenReturn(new RsuModeResponse("192.168.1.100", 4, "success",
                                                "RSU status retrieved successfully."));

                mockMvc.perform(get("/api/rsus/status")
                                .param("ipAddress", "192.168.1.100"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ipAddress").value("192.168.1.100"))
                                .andExpect(jsonPath("$.mode").value(4))
                                .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        public void testGetRsuStatus_MissingIpReturnsBadRequest() throws Exception {
                mockMvc.perform(get("/api/rsus/status"))
                                .andExpect(status().isBadRequest())
                                .andExpect(status().reason("Required parameter 'ipAddress' is not present."));
        }

        @Test
        public void testGetRsuStatus_RsuNotFoundReturnsNotFound() throws Exception {
                when(rsuModeService.getCurrentRsuStatus("192.168.1.200"))
                                .thenThrow(new NoSuchElementException(
                                                "No RSU credentials found for IP address 192.168.1.200"));

                mockMvc.perform(get("/api/rsus/status")
                                .param("ipAddress", "192.168.1.200"))
                                .andExpect(status().isNotFound())
                                .andExpect(status().reason("No RSU credentials found for IP address 192.168.1.200"));
        }

        @Test
        public void testGetRsuStatus_SnmpFailureReturnsBadGateway() throws Exception {
                when(rsuModeService.getCurrentRsuStatus("192.168.1.100"))
                                .thenThrow(new IOException("Timeout"));

                mockMvc.perform(get("/api/rsus/status")
                                .param("ipAddress", "192.168.1.100"))
                                .andExpect(status().isBadGateway())
                                .andExpect(status().reason("Failed to retrieve RSU status."));
        }

        private record ModeRequestBody(String ipAddress, Integer mode) {
        }
}
