package com.processing.gateway.shutdown;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "gateway.shutdown")
public class ShutdownProperties {
    private Duration drainPeriod = Duration.ofSeconds(30);

    public long retryAfterSeconds() {
        return Math.max(1, drainPeriod.toSeconds());
    }
}
