package us.dot.its.jpo.rsustatusmonitor.udp;

public class InvalidPayloadException extends Exception {
    public InvalidPayloadException(String message) {
        super(message);
    }
}
