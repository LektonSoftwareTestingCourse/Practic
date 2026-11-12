package com.processing.gateway.downstream;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class DownstreamExceptionUtilsTest {
    @Test
    void shouldDetectConnectionFailuresWrappedInCauses() {
        assertTrue(DownstreamExceptionUtils.isDownstreamUnavailable(
                new IllegalStateException("request failed", new SocketTimeoutException("timeout"))));
        assertTrue(DownstreamExceptionUtils.isDownstreamUnavailable(
                new ResourceAccessException("connection error")));
    }

    @Test
    void shouldNotClassifyBusinessExceptionsAsUnavailable() {
        assertFalse(DownstreamExceptionUtils.isDownstreamUnavailable(new IllegalArgumentException("bad request")));
        assertFalse(DownstreamExceptionUtils.isDownstreamUnavailable(null));
    }

    @Test
    void shouldRethrowIoExceptionWithoutWrapping() {
        IOException original = new IOException("read failed");
        IOException thrown = assertThrows(IOException.class, () -> DownstreamExceptionUtils.rethrow(original));
        assertSame(original, thrown);
    }

    @Test
    void shouldRethrowRuntimeExceptionWithoutWrapping() {
        IllegalStateException original = new IllegalStateException("bad state");
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> DownstreamExceptionUtils.rethrow(original));
        assertSame(original, thrown);
    }

    @Test
    void shouldWrapCheckedNonIoExceptionIntoIoException() {
        IOException thrown = assertThrows(IOException.class,
                () -> DownstreamExceptionUtils.rethrow(new Exception("checked")));
        assertEquals("checked", thrown.getCause().getMessage());
    }
}
