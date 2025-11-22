package us.dot.its.jpo.rsustatusmonitor.kafka;

import java.util.Properties;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class IntersectionStatusTopologyTest {

    private IntersectionStatusTopology topology;
    private KafkaTopics kafkaTopics;
    private StreamsBuilder streamsBuilder;

    @BeforeEach
    public void setup() {
        topology = new IntersectionStatusTopology();
        kafkaTopics = new KafkaTopics();
        kafkaTopics.setIntersectionStatus("test-intersection-status-topic");
        streamsBuilder = new StreamsBuilder();
    }

    @Test
    public void testBuildPipeline() {
        // Test that the topology builds without throwing an exception
        assertDoesNotThrow(() -> topology.buildPipeline(streamsBuilder, kafkaTopics));

        // Verify the topology was built
        assertNotNull(streamsBuilder.build());
    }

    @Test
    public void testBuildPipelineCreatesGlobalTable() {
        // Build the pipeline
        topology.buildPipeline(streamsBuilder, kafkaTopics);

        // Create a test driver to validate the topology
        Properties props = new Properties();
        props.put("application.id", "test-app");
        props.put("bootstrap.servers", "dummy:1234");

        try (TopologyTestDriver testDriver = new TopologyTestDriver(streamsBuilder.build(), props)) {
            // Verify that the global store exists
            assertNotNull(testDriver.getKeyValueStore(IntersectionStatusTopology.INTERSECTION_STATUS_STORE));
        }
    }
}
