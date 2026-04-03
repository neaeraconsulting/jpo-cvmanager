package us.dot.its.jpo.rsustatusmonitor.models.api;

public record RsuModeResponse(String ipAddress, int mode, String status, String message) {
}
