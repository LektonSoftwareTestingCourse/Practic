package com.processing.service;

import com.processing.config.SwitchProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class MerchantAcquirerClientTest {
    private static final String MERCHANT_URL = "http://merchant.test";

    @Test
    void shouldReturnFeeFromMerchantAcquirer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MerchantAcquirerClient client = new MerchantAcquirerClient(properties(), builder.build());
        server.expect(requestTo(MERCHANT_URL + "/api/simulator/merchant/fee"))
                .andRespond(withSuccess("{\"acquirerFee\":\"2.50\"}", MediaType.APPLICATION_JSON));

        BigDecimal fee = client.fetchAcquiringFee(Instant.parse("2026-11-12T10:00:00Z"), "123456",
                "4000001234567890", "TERM0001", new BigDecimal("100.00"));

        assertEquals(new BigDecimal("2.50"), fee);
        server.verify();
    }

    @Test
    void shouldReturnNullWhenMerchantAcquirerIsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MerchantAcquirerClient client = new MerchantAcquirerClient(properties(), builder.build());
        server.expect(requestTo(MERCHANT_URL + "/api/simulator/merchant/fee"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertNull(client.fetchAcquiringFee(Instant.now(), "123456", "4000001234567890", "TERM0001",
                new BigDecimal("100.00")));
        server.verify();
    }

    @Test
    void shouldReturnNullWhenMerchantAcquirerReturnsEmptyBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MerchantAcquirerClient client = new MerchantAcquirerClient(properties(), builder.build());
        server.expect(requestTo(MERCHANT_URL + "/api/simulator/merchant/fee"))
                .andRespond(withSuccess());

        assertNull(client.fetchAcquiringFee(Instant.now(), "123456", "4000001234567890", "TERM0001",
                new BigDecimal("100.00")));
        server.verify();
    }

    private static SwitchProperties properties() {
        return new SwitchProperties("test", Map.of(), "http://auth", "http://logger", MERCHANT_URL,
                new SwitchProperties.HttpProperties(100, 100, 100),
                new SwitchProperties.RetryProperties(1, List.of(0L)),
                new SwitchProperties.CircuitBreakerSection(
                        new SwitchProperties.CircuitBreakerProperties(50, 10, 5, 1000)));
    }
}
