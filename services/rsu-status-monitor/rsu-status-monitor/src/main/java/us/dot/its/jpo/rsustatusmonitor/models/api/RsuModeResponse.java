package us.dot.its.jpo.rsustatusmonitor.models.api;

public record RsuModeResponse(String ipAddress, Integer mode, String status, String message) {
}
