package com.processing.transactionlogger.websocket;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Обработчик WebSocket-соединений по адресу {@code /ws/transactions}.
 * Делегирует управление сессиями в {@link WebSocketManager}.
 */
@Component
@RequiredArgsConstructor
public class TransactionWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketManager webSocketManager;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        webSocketManager.addSession(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        webSocketManager.removeSession(session);
    }
}
