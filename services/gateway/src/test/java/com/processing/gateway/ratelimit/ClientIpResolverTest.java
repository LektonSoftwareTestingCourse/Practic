package com.processing.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {
    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void shouldPreferFirstAddressFromForwardedForHeader() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Forwarded-For", "198.51.100.10, 10.0.0.2").build());

        assertEquals("198.51.100.10", resolver.resolve(exchange));
    }

    @Test
    void shouldUseRealIpWhenForwardedForIsMissing() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header("X-Real-IP", "203.0.113.7").build());

        assertEquals("203.0.113.7", resolver.resolve(exchange));
    }

    @Test
    void shouldUseRemoteAddressWhenHeadersAreMissing() throws Exception {
        var address = new InetSocketAddress(InetAddress.getByName("192.0.2.15"), 8080);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").remoteAddress(address).build());

        assertEquals("192.0.2.15", resolver.resolve(exchange));
    }

    @Test
    void shouldReturnUnknownWhenNoAddressIsAvailable() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        assertEquals("unknown", resolver.resolve(exchange));
    }
}
