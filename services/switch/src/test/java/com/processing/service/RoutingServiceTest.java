package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingServiceTest {
    private final RoutingService service = new RoutingService(properties());

    @Test
    void shouldResolveIssuerForKnownBin() {
        assertEquals("ISS001", service.getIssuerIdByPan("4000001234567890"));
    }

    @Test
    void shouldRejectNullPan() {
        assertThrows(UnknownBinException.class, () -> service.getIssuerIdByPan(null));
    }

    @Test
    void shouldRejectPanShorterThanSixDigits() {
        assertThrows(UnknownBinException.class, () -> service.getIssuerIdByPan("12345"));
    }

    @Test
    void shouldRejectUnknownBin() {
        assertThrows(UnknownBinException.class, () -> service.getIssuerIdByPan("9999991234567890"));
    }

    private static SwitchProperties properties() {
        return new SwitchProperties(
                "test", Map.of("400000", "ISS001"), "http://auth", "http://logger", "http://merchant",
                new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(1, List.of(0L)),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1_000)));
    }
}
