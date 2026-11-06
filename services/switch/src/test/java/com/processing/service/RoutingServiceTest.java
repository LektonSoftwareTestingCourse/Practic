package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingServiceTest {

    private static final String KNOWN_PAN = "4000001234567890";

    private final RoutingService routingService = routingService();

    @Test
    void shouldResolveIssuerIdWhenBinIsKnown() {
        String issuerId = routingService.getIssuerIdByPan(KNOWN_PAN);

        assertThat(issuerId).isEqualTo("ISS001");
    }

    @Test
    void shouldResolveIssuerIdWhenPanContainsExactlySixDigits() {
        String issuerId = routingService.getIssuerIdByPan("400001");

        assertThat(issuerId).isEqualTo("ISS002");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345"})
    void shouldRejectPanWhenItIsNullOrTooShort(String pan) {
        assertThatThrownBy(() -> routingService.getIssuerIdByPan(pan))
                .isInstanceOf(UnknownBinException.class)
                .hasMessageContaining("null-or-short");
    }

    @Test
    void shouldRejectPanWhenBinIsUnknown() {
        assertThatThrownBy(() -> routingService.getIssuerIdByPan("9999991234567890"))
                .isInstanceOf(UnknownBinException.class)
                .hasMessageContaining("999999");
    }

    private static RoutingService routingService() {
        Map<String, String> binRouting = Map.of(
                "400000", "ISS001",
                "400001", "ISS002"
        );
        SwitchProperties properties = new SwitchProperties(
                "1.0.0", binRouting, null, null, null, null, null, null);
        return new RoutingService(properties);
    }
}
