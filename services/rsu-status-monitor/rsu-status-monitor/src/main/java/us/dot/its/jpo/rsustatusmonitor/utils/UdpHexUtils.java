package us.dot.its.jpo.rsustatusmonitor.utils;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import us.dot.its.jpo.rsustatusmonitor.udp.InvalidPayloadException;

@Slf4j
public class UdpHexUtils {

    public static final String MAP_START_FLAG = "0012";

    /**
     * Extracts the Map UPER hex from the given {@link DatagramPacket} and converts
     * it into a {@link String} object. The method validates that the payload
     * contains the necessary start flag for the specified message type.
     *
     * @param packet the DatagramPacket containing the data
     * @return the extracted String from the packet
     * @throws InvalidPayloadException if the payload is null or does not contain
     *                                 the expected start flag
     */
    public static String getMapUperHexString(DatagramPacket packet) throws InvalidPayloadException {
        // retrieve the buffer from the packet
        byte[] buffer = packet.getData();
        if (buffer == null) {
            throw new InvalidPayloadException("Buffer is null, no payload to extract");
        }

        // retrieve the payload from the buffer
        int lengthOfReceivedPacket = packet.getLength();
        int offsetOfReceivedPacket = packet.getOffset();
        byte[] payload = retrieveRelevantBytes(lengthOfReceivedPacket, buffer, offsetOfReceivedPacket);

        // convert bytes to hex string and verify identity as a Map message
        String payloadHexString = HexUtils.toHexString(payload).toLowerCase();
        if (!payloadHexString.contains(MAP_START_FLAG)) {
            throw new InvalidPayloadException("Payload does not contain start flag for a Map message");
        }

        payloadHexString = stripHeaders(payloadHexString, MAP_START_FLAG).toLowerCase();
        return payloadHexString;
    }

    /**
     * Given a buffer containing the full payload, this method retrieves and returns
     * only the relevant bytes of the message, excluding any padded bytes.
     *
     * @param length The length of the message
     * @param buffer The buffer containing the full message and possibly padded
     *               bytes
     * @param offset The position in the buffer where the message starts
     * @return The relevant bytes of the message
     */
    private static byte[] retrieveRelevantBytes(int length, byte[] buffer, int offset) {
        byte[] relevantPayload = new byte[length];
        System.arraycopy(buffer, offset, relevantPayload, 0, length);
        return relevantPayload;
    }

    /**
     * Strips the IEEE 1609.3 and 1609.2 headers from an ASN.1 hex payload if they
     * are present.
     * 
     * @return The payload without headers
     */
    public static String stripHeaders(String hexString, String payloadStartFlag) {
        int index = hexString.indexOf(payloadStartFlag);

        // If there are no headers, simply return the original payload value
        if (index == 0 || index == -1) {
            return hexString;
        }

        // Make sure start flag is on an even numbered byte
        while (index != -1 && index % 2 != 0) {
            index = hexString.indexOf(payloadStartFlag, index + 1);
        }

        return hexString.substring(index);
    }

    public static Integer getIntersectionId(String mapXer) {
        if (mapXer == null || mapXer.isEmpty()) {
            return null;
        }

        int index = 0;
        while (true) {
            index = mapXer.indexOf("<id>", index);
            if (index == -1) {
                log.warn("No <id> tag found");
                break;
            }
            int endIndex = mapXer.indexOf("</id>", index);
            if (endIndex == -1) {
                log.warn("No </id> tag found");
                break;
            }
            int valueStart = index + 4; // length of <id>
            String value = mapXer.substring(valueStart, endIndex).trim();
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                index = valueStart;
            }
        }
        return null;
    }
}
