package us.dot.its.jpo.rsustatusmonitor.udp;

import j2735ffm.MessageFrameCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.utils.DateJsonMapper;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UdpReceiverTest {

    @Mock
    private MessageFrameCodec mockCodec;

    @Mock
    private KafkaTemplate<String, String> mockKafkaTemplate;

    private UdpReceiver udpReceiver;
    private final String testTopic = "test-topic";
    private final int testPort = 42300;

    @BeforeEach
    public void setUp() {
        udpReceiver = new UdpReceiver(mockCodec, mockKafkaTemplate, testTopic);
    }

    @AfterEach
    public void tearDown() {
        if (udpReceiver != null && udpReceiver.socket != null && !udpReceiver.socket.isClosed()) {
            udpReceiver.socket.close();
        }
    }

    @Test
    public void testConstructor_InitializesFields() {
        assertNotNull(udpReceiver);
        assertNotNull(udpReceiver.socket);
        assertEquals(testPort, udpReceiver.port);
        assertEquals(2000, udpReceiver.bufferSize);
    }

    @Test
    public void testProcessPacket_ValidMapMessage() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>12345</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        verify(mockCodec).uperToXer(any(byte[].class));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertEquals(testTopic, topicCaptor.getValue());
        assertEquals("12345", keyCaptor.getValue());

        String jsonValue = valueCaptor.getValue();
        IntersectionStatusRecord record = DateJsonMapper.getInstance().readValue(jsonValue,
                IntersectionStatusRecord.class);
        assertEquals(12345, record.getIntersectionId());
        assertNotNull(record.getListenerIp());
        assertNotNull(record.getReceivedAt());
    }

    @Test
    public void testProcessPacket_NoIntersectionId() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        lenient().when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        verify(mockCodec).uperToXer(any(byte[].class));
        verify(mockKafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testProcessPacket_InvalidPayloadException() throws Exception {
        byte[] packetData = hexStringToBytes("aabbccddee");
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        Exception thrown = assertThrows(Exception.class, () -> invokeProcessPacket(packet));
        assertTrue(thrown.getCause() instanceof InvalidPayloadException);

        verify(mockCodec, never()).uperToXer(any(byte[].class));
        verify(mockKafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testProcessPacket_CodecThrowsException() throws Exception {
        String mapUperHex = "00120102030405";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        lenient().when(mockCodec.uperToXer(any(byte[].class))).thenThrow(new RuntimeException("Codec error"));

        Exception thrown = assertThrows(Exception.class, () -> invokeProcessPacket(packet));
        assertTrue(thrown.getCause() instanceof RuntimeException);

        verify(mockCodec).uperToXer(any(byte[].class));
        verify(mockKafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testProcessPacket_MultipleIntersectionIds() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><id>11111</id><intersections><IntersectionGeometry><id>22222</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(eq(testTopic), keyCaptor.capture(), anyString());

        assertEquals("11111", keyCaptor.getValue());
    }

    @Test
    public void testProcessPacket_ZeroIntersectionId() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>0</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(eq(testTopic), keyCaptor.capture(), anyString());

        assertEquals("0", keyCaptor.getValue());
    }

    @Test
    public void testProcessPacket_NegativeIntersectionId() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>-5678</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(eq(testTopic), keyCaptor.capture(), anyString());

        assertEquals("-5678", keyCaptor.getValue());
    }

    @Test
    public void testProcessPacket_WithHeaders() throws Exception {
        String mapUperHex = "aabbccdd00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>54321</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        verify(mockCodec).uperToXer(any(byte[].class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(eq(testTopic), keyCaptor.capture(), anyString());

        assertEquals("54321", keyCaptor.getValue());
    }

    @Test
    public void testProcessPacket_UppercaseHex() throws Exception {
        String mapUperHex = "AABBCCDD00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>99999</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        verify(mockCodec).uperToXer(any(byte[].class));
        verify(mockKafkaTemplate).send(eq(testTopic), eq("99999"), anyString());
    }

    @Test
    public void testProcessPacket_LongPayload() throws Exception {
        StringBuilder sb = new StringBuilder("aabbccddee");
        for (int i = 0; i < 50; i++) {
            sb.append("ff");
        }
        sb.append("0012");
        for (int i = 0; i < 50; i++) {
            sb.append("aa");
        }
        String mapUperHex = sb.toString();
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>77777</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        verify(mockCodec).uperToXer(any(byte[].class));
        verify(mockKafkaTemplate).send(eq(testTopic), eq("77777"), anyString());
    }

    @Test
    public void testIsStopped_InitiallyFalse() {
        assertFalse(udpReceiver.isStopped());
    }

    @Test
    public void testSetStopped_ChangesState() {
        assertFalse(udpReceiver.isStopped());

        udpReceiver.setStopped(true);

        assertTrue(udpReceiver.isStopped());
    }

    @Test
    public void testSocket_CreatedOnConstruction() {
        assertNotNull(udpReceiver.socket);
        assertFalse(udpReceiver.socket.isClosed());
    }

    @Test
    public void testRun_StopsWhenFlagSet() {
        udpReceiver.setStopped(true);

        assertDoesNotThrow(() -> {
            udpReceiver.run();
        });

        assertTrue(udpReceiver.isStopped());
    }

    @Test
    public void testProcessPacket_JsonSerializationWorks() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>33333</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());

        String json = valueCaptor.getValue();
        assertTrue(json.contains("intersectionId"));
        assertTrue(json.contains("33333"));
        assertTrue(json.contains("listenerIp"));
        assertTrue(json.contains("receivedAt"));
    }

    @Test
    public void testProcessPacket_ReceivedAtIsISO8601() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><intersections><IntersectionGeometry><id>44444</id></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());

        String jsonValue = valueCaptor.getValue();
        IntersectionStatusRecord record = DateJsonMapper.getInstance().readValue(jsonValue,
                IntersectionStatusRecord.class);

        assertNotNull(record.getReceivedAt());
        assertTrue(record.getReceivedAt().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }

    @Test
    public void testProcessPacket_ComplexXmlStructure() throws Exception {
        String mapUperHex = "00120102030405";
        String mapXer = "<MapData><timeStamp>123456</timeStamp><msgIssueRevision>1</msgIssueRevision>" +
                "<layerType>intersectionData</layerType><intersections><IntersectionGeometry>" +
                "<id>88888</id><revision>0</revision></IntersectionGeometry></intersections></MapData>";

        byte[] packetData = hexStringToBytes(mapUperHex);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getLocalHost(), 8080);

        when(mockCodec.uperToXer(any(byte[].class))).thenReturn(mapXer);

        invokeProcessPacket(packet);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaTemplate).send(anyString(), keyCaptor.capture(), anyString());

        assertEquals("88888", keyCaptor.getValue());
    }

    private void invokeProcessPacket(DatagramPacket packet) throws Exception {
        Method method = UdpReceiver.class.getDeclaredMethod("processPacket", DatagramPacket.class);
        method.setAccessible(true);
        method.invoke(udpReceiver, packet);
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
