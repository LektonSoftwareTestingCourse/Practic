package com.processing.transactionlogger.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация {@link WebSocketManager} на основе {@link ConcurrentHashMap}.
 * Каждая сессия оборачивается в {@link ConcurrentWebSocketSessionDecorator},
 * который сериализует отправку через внутреннюю очередь без блокировки вызывающих потоков.
 * Периодически отправляет ping-сообщения для поддержания соединений живыми.
 */
@Slf4j
@Component
public class DefaultWebSocketManager implements WebSocketManager {
    private static final int SEND_TIME_LIMIT_MS = 1_000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void addSession(WebSocketSession session) {
        sessions.add(new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
        log.info("WebSocket connected: {}, total: {}", session.getId(), sessions.size());
    }

    @Override
    public void removeSession(WebSocketSession session) {
        sessions.removeIf(s -> Objects.equals(s.getId(), session.getId()));
        log.info("WebSocket disconnected: {}, total: {}", session.getId(), sessions.size());
    }

    /**
     * Рассылает текстовое сообщение всем открытым сессиям.
     * Закрытые и недоступные сессии удаляются из пула по ходу обхода.
     */
    @Override
    public void broadcast(String message) {
        Set<WebSocketSession> deadSessions = new HashSet<>();
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                } else {
                    deadSessions.add(session);
                }
            } catch (Exception ex) {
                log.error("Failed to send message to session: {}", session.getId(), ex);
                deadSessions.add(session);
            }
        });
        sessions.removeAll(deadSessions);
    }

    /**
     * Отправляет ping всем сессиям для проверки статуса соединений.
     * Период задаётся свойством {@code websocket.ping-interval-ms}.
     */
    @Scheduled(fixedDelayString = "${websocket.ping-interval-ms}")
    public void ping() {
        Set<WebSocketSession> deadSessions = new HashSet<>();
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new PingMessage());
                } else {
                    deadSessions.add(session);
                }
            } catch (Exception ex) {
                log.warn("Ping failed for session {}, removing", session.getId());
                deadSessions.add(session);
            }
        });
        sessions.removeAll(deadSessions);
    }
}
