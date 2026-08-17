package com.processing.gateway.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.processing.gateway.logging.models.RequestLog;
import com.processing.gateway.properties.SmpGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Adds request correlation and writes a structured log record for every request.
 *
 * <p>If an incoming {@code X-Request-Id} header is present it is reused;
 * otherwise a new UUID is generated and propagated to the response.</p>
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {
    private final ObjectMapper mapper;
    private final SmpGatewayProperties properties;
    private final LogDataMasker dataMasker;

    private static final String ID_HEADER_NAME = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    public RequestLoggingFilter(ObjectMapper mapper, SmpGatewayProperties properties, LogDataMasker dataMasker) {
        this.properties = properties;
        this.mapper = mapper.copy()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.dataMasker = dataMasker;

        if (properties.getLogging().getPretty()) {
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (properties.getLogging().getExcludedRoutes().contains(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();

        String incoming = exchange.getRequest().getHeaders().getFirst(ID_HEADER_NAME);
        String requestId = (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();

        var mutatedRequest = exchange.getRequest().mutate()
                .header(ID_HEADER_NAME, requestId)
                .build();
        exchange.getResponse().getHeaders().add(ID_HEADER_NAME, requestId);

        var requestLogBuilder = RequestLog.builder()
                .requestId(requestId)
                .method(mutatedRequest.getMethod().name())
                .path(mutatedRequest.getPath().value());

        var mutatedExchange = properties.getLogging().getBodies()
                ? logBodies(mutatedRequest, exchange.getResponse(), exchange, requestLogBuilder)
                : exchange.mutate()
                    .request(mutatedRequest)
                    .build();

        return chain.filter(mutatedExchange).doFinally(signalType -> {
            long responseTime = System.currentTimeMillis() - startTime;

            HttpStatusCode statusCode = mutatedExchange.getResponse().getStatusCode();
            int rawStatusCode = (statusCode != null) ? statusCode.value() : 0;

            var requestLog = requestLogBuilder
                    .responseCode(rawStatusCode)
                    .responseTime(responseTime)
                    .build();

            MDC.put(MDC_KEY, requestId);
            try {
                String mappedLog = mapper.writeValueAsString(requestLog);
                log.info(mappedLog);
            } catch (JsonProcessingException e) {
                log.error("Exception occurred while mapping log message", e);
            } finally {
                MDC.remove(MDC_KEY);
            }
        });
    }

    private ServerWebExchange logBodies(ServerHttpRequest request,
                                        ServerHttpResponse response,
                                        ServerWebExchange exchange,
                                        RequestLog.RequestLogBuilder logBuilder) {
        logRequestBody(request, logBuilder);
        return exchange.mutate()
                .request(request)
                .response(new ResponseLoggingDecorator(response, logBuilder, dataMasker))
                .build();
    }

    private void logRequestBody(ServerHttpRequest request, RequestLog.RequestLogBuilder logBuilder) {
        var decoratedRequest = new ServerHttpRequestDecorator(request);

        decoratedRequest.getBody().buffer().map(dataBuffers -> {
            var bufferFactory = new DefaultDataBufferFactory();
            DataBuffer joinedBuffers = bufferFactory.join(dataBuffers);

            var content = new byte[joinedBuffers.readableByteCount()];
            joinedBuffers.read(content);

            DataBufferUtils.release(joinedBuffers);

            String requestBody = new String(content, StandardCharsets.UTF_8);
            String maskedRequestBody = dataMasker.maskData(requestBody);

            logBuilder.requestBody(maskedRequestBody);

            return bufferFactory.wrap(content);
        }).subscribe();
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
