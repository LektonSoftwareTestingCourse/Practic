package com.processing.gateway.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.gateway.metrics.GatewayMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionValidationFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TransactionRequestValidator validator = new TransactionRequestValidator();
    private final GatewayMetrics metrics = mock(GatewayMetrics.class);
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);
    private final TransactionValidationFilter filter = new TransactionValidationFilter(objectMapper, validator, metrics);

    @Test
    void shouldRejectTransactionWhenMtiIsWrong() {
        MockServerWebExchange exchange = transactionExchange(request("0200"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("VALIDATION_ERROR")
                .contains("Field 'mti' must be '0100'");
        verify(metrics).recordValidationRejected("invalid_request");
        verifyNoInteractions(chain);
    }

    @Test
    void shouldRejectTransactionWhenJsonIsInvalid() {
        MockServerWebExchange exchange = transactionExchange("{");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("VALIDATION_ERROR")
                .contains("Request body must be valid JSON");
        verify(metrics).recordValidationRejected("invalid_json");
        verifyNoInteractions(chain);
    }

    @Test
    void shouldPassTransactionToNextFilterWhenRequestIsValid() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = transactionExchange(request("0100"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    void shouldIgnoreRequestWhenPathIsNotTransaction() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/cards").build()
        );

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    private MockServerWebExchange transactionExchange(String body) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );
    }

    private String request(String mti) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .mti(mti)
                .stan("000001")
                .pan("4000000000000002")
                .processingCode("000000")
                .amount(new BigDecimal("1000"))
                .currencyCode("643")
                .transmissionDateTime(Instant.parse("2026-01-01T10:00:00Z"))
                .terminalId("TERM0001")
                .terminalType("POS")
                .merchantId("MERCH0000000002")
                .mcc("5045")
                .acquirerId("ACQ002")
                .issuerId("ISS001")
                .build();

        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
