package us.dot.its.jpo.rsustatusmonitor.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IntersectionStatusTopology {

    public static final String INTERSECTION_STATUS_STORE = "intersection-status-store";

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder, KafkaTopics kafkaTopics) {
        log.info("Building intersection status topology");

        // Create Global KTable from the compacted intersection status topic
        streamsBuilder.globalTable(
                kafkaTopics.getIntersectionStatus(),
                Consumed.with(Serdes.String(), Serdes.String()),
                Materialized.as(INTERSECTION_STATUS_STORE));

        log.info("Intersection status topology built successfully");
    }
}
