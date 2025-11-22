package us.dot.its.jpo.rsustatusmonitor.udp;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.DatagramPacket;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.HexFormat;
import j2735ffm.MessageFrameCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.utils.DateJsonMapper;
import us.dot.its.jpo.rsustatusmonitor.utils.UdpHexUtil;

@Slf4j
public class UdpReceiver extends AbstractUdpReceiver {

    private final MessageFrameCodec codec;
    private final KafkaTemplate<String, String> kafkaPublisher;
    private final String publishTopic;

    public UdpReceiver(MessageFrameCodec codec, KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
        super(42300, 2000);
        this.codec = codec;
        this.kafkaPublisher = kafkaTemplate;
        this.publishTopic = publishTopic;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        do {
            try {
                socket.receive(packet);
                if (packet.getLength() > 0) {
                    processPacket(packet);
                }
            } catch (InvalidPayloadException e) {
                log.error("Error decoding packet", e);
            } catch (Exception e) {
                log.error("Error receiving packet", e);
            }
        } while (!isStopped());
    }

    private void processPacket(DatagramPacket packet) throws InvalidPayloadException {
        String senderIp = packet.getAddress().getHostAddress();
        log.debug("Packet received from {}", senderIp);

        String uperHex = UdpHexUtil.getMapUperHexString(packet);
        log.debug("UPER hex extracted from packet: {}", uperHex);

        byte[] bytes = HexFormat.of().parseHex(uperHex);
        String xer = codec.uperToXer(bytes);
        log.debug("XER representation: {}", xer);

        Integer intersectionId = UdpHexUtil.getIntersectionId(xer);
        log.debug("Intersection ID: '{}' From IP: '{}'", intersectionId, senderIp);

        // Create IntersectionStatusRecord object to hold the status information
        if (intersectionId != null) {
            IntersectionStatusRecord record = new IntersectionStatusRecord();
            // TODO: Build out more information for both listener and sender. Build out data
            // structure
            record.setIntersectionId(intersectionId);
            record.setListenerIp(senderIp);
            record.setReceivedAt(
                    Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // Write result to Kafka
            try {
                kafkaPublisher.send(publishTopic, intersectionId.toString(),
                        DateJsonMapper.getInstance().writeValueAsString(record));
            } catch (JsonProcessingException e) {
                log.error("Error serializing IntersectionStatusRecord to JSON", e);
            }
        } else {
            log.warn("Failed to extract Intersection ID from Map payload received by {} of UPER Hex: {}", senderIp,
                    uperHex);
        }
    }
}
