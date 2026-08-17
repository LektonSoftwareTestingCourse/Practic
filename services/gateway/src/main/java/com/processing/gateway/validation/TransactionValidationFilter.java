package com.processing.gateway.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processing.common.dto.ErrorResponse;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.gateway.metrics.GatewayMetrics;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;

/**
 * Validates transaction authorization requests before they are proxied to Switch.
 */
@Component
@RequiredArgsConstructor
public class TransactionValidationFilter implements GlobalFilter, Ordered {

    private static final String TRANSACTIONS_PATH = "/api/transactions";

    private final ObjectMapper objectMapper;
    private final TransactionRequestValidator validator;
    private final GatewayMetrics gatewayMetrics;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isTransactionRequest(exchange)) {
            return chain.filter(exchange);
        }

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .switchIfEmpty(Mono.fromSupplier(() -> exchange.getResponse()
                        .bufferFactory()
                        .wrap(new byte[0])))
                .flatMap(dataBuffer -> validateAndContinue(exchange, chain, dataBuffer));
    }

    private Mono<Void> validateAndContinue(ServerWebExchange exchange,
                                           GatewayFilterChain chain,
                                           DataBuffer dataBuffer) {
        byte[] requestBody = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(requestBody);
        DataBufferUtils.release(dataBuffer);

        try {
            AuthorizationRequest authorizationRequest = objectMapper.readValue(requestBody, AuthorizationRequest.class);
            validator.validate(authorizationRequest);
        } catch (IOException e) {
            gatewayMetrics.recordValidationRejected("invalid_json");
            return writeValidationError(exchange, "Request body must be valid JSON");
        } catch (TransactionValidationException e) {
            gatewayMetrics.recordValidationRejected("invalid_request");
            return writeValidationError(exchange, e.getMessage());
        }

        ServerHttpRequest decoratedRequest = decorateRequest(exchange, requestBody);
        return chain.filter(exchange.mutate().request(decoratedRequest).build());
    }

    private boolean isTransactionRequest(ServerWebExchange exchange) {
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && TRANSACTIONS_PATH.equals(exchange.getRequest().getPath().value());
    }

    private ServerHttpRequest decorateRequest(ServerWebExchange exchange, byte[] requestBody) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            @NonNull
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.setContentLength(requestBody.length);
                return headers;
            }

            @Override
            @NonNull
            public Flux<DataBuffer> getBody() {
                return Flux.defer(() -> {
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(requestBody);
                    return Flux.just(buffer);
                });
            }
        };
    }

    private Mono<Void> writeValidationError(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return writeJson(exchange, new ErrorResponse(
                "VALIDATION_ERROR",
                message,
                Instant.now(),
                "gateway",
                null
        ));
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, Object body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (IOException e) {
            return Mono.error(e);
        }
    }
}
