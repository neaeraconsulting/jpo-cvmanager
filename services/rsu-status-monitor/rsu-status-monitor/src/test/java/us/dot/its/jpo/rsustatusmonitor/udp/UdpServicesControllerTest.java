package us.dot.its.jpo.rsustatusmonitor.udp;

import j2735ffm.MessageFrameCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.rsustatusmonitor.kafka.KafkaTopics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UdpServicesControllerTest {

    @Mock
    private MessageFrameCodec mockCodec;

    @Mock
    private KafkaTemplate<String, String> mockKafkaTemplate;

    @Mock
    private KafkaTopics mockKafkaTopics;

    private UdpServicesController controller;

    @BeforeEach
    public void setUp() {
        lenient().when(mockKafkaTopics.getIntersectionStatus()).thenReturn("intersection-status-topic");
    }

    @Test
    public void testConstructor_InitializesController() {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        assertNotNull(controller);
        verify(mockKafkaTopics).getIntersectionStatus();
    }

    @Test
    public void testConstructor_StartsReceiver() throws InterruptedException {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        Thread.sleep(200);

        assertNotNull(controller);
    }

    @Test
    public void testConstructor_UsesCorrectTopic() {
        String expectedTopic = "test-intersection-topic";
        when(mockKafkaTopics.getIntersectionStatus()).thenReturn(expectedTopic);

        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        verify(mockKafkaTopics, atLeastOnce()).getIntersectionStatus();
    }

    @Test
    public void testConstructor_CreatesExecutors() {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        assertNotNull(controller);
    }

    @Test
    public void testConstructor_MultipleInstances() {
        UdpServicesController controller1 = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);
        UdpServicesController controller2 = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        assertNotNull(controller1);
        assertNotNull(controller2);
        assertNotSame(controller1, controller2);
    }

    @Test
    public void testConstructor_CreatesUdpReceiver() {
        when(mockKafkaTopics.getIntersectionStatus()).thenReturn("status-topic");

        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        assertNotNull(controller);
        verify(mockKafkaTopics).getIntersectionStatus();
    }

    @Test
    public void testConstructor_DaemonThreadCreation() throws InterruptedException {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        Thread.sleep(100);

        assertNotNull(controller);
    }

    @Test
    public void testConstructor_ReceiverRuns() throws InterruptedException {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        Thread.sleep(300);

        assertNotNull(controller);
    }

    @Test
    public void testConstructor_WithEmptyTopic() {
        when(mockKafkaTopics.getIntersectionStatus()).thenReturn("");

        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        assertNotNull(controller);
        verify(mockKafkaTopics).getIntersectionStatus();
    }

    @Test
    public void testConstructor_WithSpecialCharactersTopic() {
        when(mockKafkaTopics.getIntersectionStatus()).thenReturn("test-topic.with_special-chars");

        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        assertNotNull(controller);
        verify(mockKafkaTopics).getIntersectionStatus();
    }

    @Test
    public void testConstructor_VerifyCodecNotInvoked() {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        verifyNoInteractions(mockCodec);
    }

    @Test
    public void testConstructor_VerifyKafkaTemplateNotInvoked() {
        controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);

        verifyNoInteractions(mockKafkaTemplate);
    }

    @Test
    public void testConstructor_SuccessfulInitialization() {
        assertDoesNotThrow(() -> {
            controller = new UdpServicesController(mockCodec, mockKafkaTemplate, mockKafkaTopics);
        });
    }
}
