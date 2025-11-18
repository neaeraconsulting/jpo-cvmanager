package us.dot.its.jpo.rsustatusmonitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for the J2735 FFM Library API used for on demand
 * message decoding. This class holds configuration properties related to the
 * J2735 API buffer sizes and allocation parameters. The properties are
 * automatically bound from the application configuration using the "j2735.api"
 * prefix.
 */
@Component
@ConfigurationProperties(prefix = "j2735.api")
@Data
public class FFMLibApiConfig {
    long textBufferSize;
    long uperBufferSize;
    long messageFrameAllocateSize;
    long asnCodecCtxMaxStackSize;
}
