package com.processing.service;

import com.processing.common.dto.transactionlogger.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggerClientTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldReturnTrueWhenMessageIsPublished() {
        // Arrange
        LoggerClient client = new LoggerClient(rabbitTemplate);
        TransactionRequest transaction = mock(TransactionRequest.class);

        // Act
        boolean result = client.log(transaction);

        // Assert
        assertTrue(result);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(TransactionRequest.class));
    }

    @Test
    void shouldReturnFalseWhenBrokerIsUnavailable() {
        // Arrange
        LoggerClient client = new LoggerClient(rabbitTemplate);
        TransactionRequest transaction = mock(TransactionRequest.class);
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(TransactionRequest.class));

        // Act
        boolean result = client.log(transaction);

        // Assert
        assertFalse(result);
    }
}
