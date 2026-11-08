package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingAndNormalizationTest {

    @Test
    void shouldReturnIssuerForKnownBin() {
        // Arrange
        RoutingService routingService = new RoutingService(properties());

        // Act
        String issuerId = routingService.getIssuerIdByPan("4000001234567890");

        // Assert
        assertEquals("ISS001", issuerId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectNullOrShortPan(String pan) {
        // Arrange
        RoutingService routingService = new RoutingService(properties());

        // Act and Assert
        assertThrows(UnknownBinException.class, () -> routingService.getIssuerIdByPan(pan));
    }

    @Test
    void shouldRejectUnknownBin() {
        // Arrange
        RoutingService routingService = new RoutingService(properties());

        // Act and Assert
        assertThrows(UnknownBinException.class, () -> routingService.getIssuerIdByPan("9999991234567890"));
    }

    @ParameterizedTest(name = "terminal={0}")
    @CsvSource({
            "TERM, TERM0000",
            "TERM0001, TERM0001",
            "TERMINAL99, TERMINAL"
    })
    void shouldNormalizeTerminalId(String source, String expected) {
        // Act
        String result = AuthorizationRequestNormalizer.normalizeTerminalId(source);

        // Assert
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "merchant={0}")
    @CsvSource({
            "SHOP, SHOP00000000000",
            "MERCH00000000000, MERCH0000000000",
            "MERCHANT123456789, MERCHANT1234567",
            "MERCH1234567890, MERCH1234567890"
    })
    void shouldNormalizeMerchantId(String source, String expected) {
        // Act
        String result = AuthorizationRequestNormalizer.normalizeMerchantId(source);

        // Assert
        assertEquals(expected, result);
    }

    private SwitchProperties properties() {
        return new SwitchProperties(
                "1.0", Map.of("400000", "ISS001"), "http://authorization", "http://logger",
                "http://merchant", new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(3, List.of(1L, 2L)),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1000)));
    }
}
