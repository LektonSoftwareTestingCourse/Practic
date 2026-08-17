package com.processing.gateway.logging;

import com.processing.gateway.logging.models.RequestLog;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
public class ResponseLoggingDecorator extends ServerHttpResponseDecorator {
    private final RequestLog.RequestLogBuilder logBuilder;
    private final LogDataMasker dataMasker;

    public ResponseLoggingDecorator(ServerHttpResponse delegate,
                                    RequestLog.RequestLogBuilder logBuilder, LogDataMasker dataMasker) {
        super(delegate);
        this.logBuilder = logBuilder;
        this.dataMasker = dataMasker;
    }

    @Override
    public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        if (body instanceof Flux<? extends DataBuffer> fluxBody) {
            return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                DataBuffer joinedBuffers = bufferFactory().join(dataBuffers);
                var content = new byte[joinedBuffers.readableByteCount()];
                joinedBuffers.read(content);

                DataBufferUtils.release(joinedBuffers);

                String responseBody = new String(content, StandardCharsets.UTF_8);
                String maskedResponseBody = dataMasker.maskData(responseBody);

                logBuilder.responseBody(maskedResponseBody);

                return bufferFactory().wrap(content);
            }).doOnError(e ->
                    log.error("Error reading response body in logging decorator", e)));
        }

        return super.writeWith(body);
    }
}
