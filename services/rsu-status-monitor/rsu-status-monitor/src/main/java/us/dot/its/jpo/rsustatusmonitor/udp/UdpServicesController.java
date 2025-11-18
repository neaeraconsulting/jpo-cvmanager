package us.dot.its.jpo.rsustatusmonitor.udp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import j2735ffm.MessageFrameCodec;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.rsustatusmonitor.kafka.KafkaTopics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

/**
 * Centralized UDP service dispatcher.
 */
@Controller
@Slf4j
public class UdpServicesController {

  private final List<ExecutorService> executors = new ArrayList<>();

  /**
   * Constructs a UdpServicesController to manage UDP receiver services.
   *
   * @param codec The J2735 FFM codec
   */
  @Autowired
  public UdpServicesController(MessageFrameCodec codec, KafkaTemplate<String, String> kafkaTemplate,
      KafkaTopics kafkaTopics) {

    log.debug("Starting UDP receiver services...");

    startReceiver(new UdpReceiver(codec, kafkaTemplate, kafkaTopics.getIntersectionStatus()));

    log.debug("UDP receiver services started.");
  }

  /**
   * Starts a receiver in its own executor service and manages its lifecycle.
   *
   * @param receiver The receiver to start
   */
  private void startReceiver(AbstractUdpReceiver receiver) {
    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true); // Makes thread exit when main application exits
      return thread;
    });

    executors.add(executor);

    executor.submit(() -> {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          receiver.run();
        }
      } catch (Exception e) {
        log.error("Error in receiver {}: {}", receiver.getClass().getSimpleName(), e.getMessage(), e);
      }
    });
  }
}