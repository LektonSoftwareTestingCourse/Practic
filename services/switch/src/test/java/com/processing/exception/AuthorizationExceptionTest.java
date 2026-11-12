package com.processing.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationExceptionTest {
    @Test
    void shouldKeepStanAndLastError() {
        AuthorizationException exception = new AuthorizationException("123456", 3, "timeout");

        assertEquals("123456", exception.getStan());
        assertEquals("Authorization service unavailable for STAN=123456 after 3 attempts. Last error: timeout",
                exception.getMessage());
    }

    @Test
    void shouldUseUnknownWhenLastErrorIsNull() {
        AuthorizationException exception = new AuthorizationException("123456", 1, null);

        assertEquals("Authorization service unavailable for STAN=123456 after 1 attempts. Last error: unknown",
                exception.getMessage());
    }
}
