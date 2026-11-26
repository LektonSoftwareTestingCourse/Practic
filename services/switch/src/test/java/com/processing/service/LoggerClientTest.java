package com.processing.service;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class LoggerClientTest {
    @Test
    void shouldReturnTrueWhenRabbitAcceptsMessage() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        assertTrue(new LoggerClient(rabbit).log(transaction()));
    }

    @Test
    void shouldReturnFalseWhenRabbitPublishFails() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        assertFalse(new LoggerClient(rabbit).log(transaction()));
    }

    private static TransactionRequest transaction() {
        return new TransactionRequest(UUID.randomUUID(), "0100", "123456", "123456789012",
                "4000001234567890", "000000", new BigDecimal("100.00"), "643", "TERM0001", "POS",
                "MERCH0000000002", "5045", "ACQ001", "ISS001", BigDecimal.ZERO,
                TransactionStatus.APPROVED, null, "A1B2C3", 1,
                Instant.parse("2026-11-12T10:00:00Z"), Instant.parse("2026-11-12T10:00:01Z"));
    }
}
