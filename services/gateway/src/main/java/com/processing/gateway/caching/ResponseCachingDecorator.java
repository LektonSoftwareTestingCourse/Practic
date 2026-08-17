package com.processing.gateway.caching;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import org.springframework.cache.Cache;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ResponseCachingDecorator extends ServerHttpResponseDecorator {
    private final Cache cache;
    private final String key;

    public ResponseCachingDecorator(ServerHttpResponse delegate, Cache cache, String key) {
        super(delegate);
        this.cache = cache;
        this.key = key;
    }

    @Override
    public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        if ((getStatusCode() == null || getStatusCode() == HttpStatus.OK)
                && body instanceof Flux<? extends DataBuffer> fluxBody) {
            return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                DataBuffer joinedBuffers = bufferFactory().join(dataBuffers);
                var content = new byte[joinedBuffers.readableByteCount()];
                joinedBuffers.read(content);

                DataBufferUtils.release(joinedBuffers);

                cache.put(key, content);

                return bufferFactory().wrap(content);
            }).doOnError(e ->
                    log.error("Error reading response body in caching decorator", e)));
        }

        return super.writeWith(body);
    }
}
