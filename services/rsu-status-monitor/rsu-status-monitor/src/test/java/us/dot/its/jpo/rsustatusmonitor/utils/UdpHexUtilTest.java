package us.dot.its.jpo.rsustatusmonitor.utils;

import org.junit.jupiter.api.Test;
import us.dot.its.jpo.rsustatusmonitor.udp.InvalidPayloadException;

import java.net.DatagramPacket;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

public class UdpHexUtilTest {

    @Test
    public void testGetMapUperHexString_ValidMapMessage() throws Exception {
        byte[] data = hexStringToBytes("00120102030405");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertEquals("00120102030405", result);
    }

    @Test
    public void testGetMapUperHexString_ValidMapMessageWithHeaders() throws Exception {
        byte[] data = hexStringToBytes("aabbccdd00120102030405");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertEquals("00120102030405", result);
    }

    @Test
    public void testGetMapUperHexString_EmptyBuffer() throws Exception {
        byte[] data = new byte[0];
        DatagramPacket packet = new DatagramPacket(data, 0, InetAddress.getLocalHost(), 8080);

        InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, () -> {
            UdpHexUtil.getMapUperHexString(packet);
        });

        assertEquals("Payload does not contain start flag for a Map message", exception.getMessage());
    }

    @Test
    public void testGetMapUperHexString_NoMapStartFlag() throws Exception {
        byte[] data = hexStringToBytes("aabbccddee");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, () -> {
            UdpHexUtil.getMapUperHexString(packet);
        });

        assertEquals("Payload does not contain start flag for a Map message", exception.getMessage());
    }

    @Test
    public void testGetMapUperHexString_SmallPayload() throws Exception {
        byte[] data = hexStringToBytes("001201");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertEquals("001201", result);
    }

    @Test
    public void testGetMapUperHexString_OnlyStartFlag() throws Exception {
        byte[] data = hexStringToBytes("0012");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertEquals("0012", result);
    }

    @Test
    public void testGetMapUperHexString_UppercaseInput() throws Exception {
        byte[] data = hexStringToBytes("AABBCCDD00120102030405");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertEquals("00120102030405", result);
        assertTrue(result.equals(result.toLowerCase()));
    }

    @Test
    public void testGetMapUperHexString_MultipleStartFlags() throws Exception {
        byte[] data = hexStringToBytes("001200120012");
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertEquals("001200120012", result);
    }

    @Test
    public void testGetMapUperHexString_LongPayload() throws Exception {
        StringBuilder sb = new StringBuilder("aabbccddee");
        for (int i = 0; i < 100; i++) {
            sb.append("ff");
        }
        sb.append("0012");
        for (int i = 0; i < 100; i++) {
            sb.append("aa");
        }
        byte[] data = hexStringToBytes(sb.toString());
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 8080);

        String result = UdpHexUtil.getMapUperHexString(packet);

        assertTrue(result.startsWith("0012"));
        assertTrue(result.contains("aa"));
    }

    @Test
    public void testStripHeaders_NoHeaders() {
        String hexString = "0012010203";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("0012010203", result);
    }

    @Test
    public void testStripHeaders_WithHeaders() {
        String hexString = "aabbccdd0012010203";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("0012010203", result);
    }

    @Test
    public void testStripHeaders_NoStartFlag() {
        String hexString = "aabbccddee";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("aabbccddee", result);
    }

    @Test
    public void testStripHeaders_MultipleStartFlags() {
        String hexString = "aabb00120012ccdd";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("00120012ccdd", result);
    }

    @Test
    public void testStripHeaders_StartFlagAtEnd() {
        String hexString = "aabbccdd0012";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("0012", result);
    }

    @Test
    public void testStripHeaders_EmptyString() {
        String hexString = "";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("", result);
    }

    @Test
    public void testStripHeaders_OnlyStartFlag() {
        String hexString = "0012";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("0012", result);
    }

    @Test
    public void testStripHeaders_MultipleStartFlagsFirstAtEvenIndex() {
        String hexString = "ab0012cd0012ef";

        String result = UdpHexUtil.stripHeaders(hexString, "0012");

        assertEquals("0012cd0012ef", result);
    }

    @Test
    public void testGetIntersectionId_ValidSingleId() {
        String mapXer = "<map><id>12345</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(12345, result);
    }

    @Test
    public void testGetIntersectionId_ValidIdWithWhitespace() {
        String mapXer = "<map><id>  67890  </id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(67890, result);
    }

    @Test
    public void testGetIntersectionId_NullInput() {
        Integer result = UdpHexUtil.getIntersectionId(null);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_EmptyString() {
        Integer result = UdpHexUtil.getIntersectionId("");

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_NoIdTag() {
        String mapXer = "<map><name>Test</name></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_NoClosingIdTag() {
        String mapXer = "<map><id>12345</map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_NonNumericId() {
        String mapXer = "<map><id>abc</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_MultipleIds_FirstIsNonNumeric() {
        String mapXer = "<map><id>notanumber</id><id>99999</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(99999, result);
    }

    @Test
    public void testGetIntersectionId_MultipleIds_FirstIsValid() {
        String mapXer = "<map><id>11111</id><id>22222</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(11111, result);
    }

    @Test
    public void testGetIntersectionId_ZeroValue() {
        String mapXer = "<map><id>0</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(0, result);
    }

    @Test
    public void testGetIntersectionId_NegativeValue() {
        String mapXer = "<map><id>-12345</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(-12345, result);
    }

    @Test
    public void testGetIntersectionId_LargeValue() {
        String mapXer = "<map><id>2147483647</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(2147483647, result);
    }

    @Test
    public void testGetIntersectionId_EmptyIdTag() {
        String mapXer = "<map><id></id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_IdTagWithOnlyWhitespace() {
        String mapXer = "<map><id>   </id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_ComplexXmlStructure() {
        String mapXer = "<MapData><timeStamp>123456</timeStamp><msgIssueRevision>1</msgIssueRevision>" +
                "<layerType>intersectionData</layerType><intersections><IntersectionGeometry>" +
                "<id>54321</id><revision>0</revision></IntersectionGeometry></intersections></MapData>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(54321, result);
    }

    @Test
    public void testGetIntersectionId_NestedIdTags() {
        String mapXer = "<map><outer><id>999</id></outer><id>888</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(999, result);
    }

    @Test
    public void testGetIntersectionId_IdWithDecimalPoint() {
        String mapXer = "<map><id>123.45</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertNull(result);
    }

    @Test
    public void testGetIntersectionId_IdWithLeadingZeros() {
        String mapXer = "<map><id>00012345</id></map>";

        Integer result = UdpHexUtil.getIntersectionId(mapXer);

        assertEquals(12345, result);
    }

    @Test
    public void testMapStartFlagConstant() {
        assertEquals("0012", UdpHexUtil.MAP_START_FLAG);
    }

    private byte[] hexStringToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
