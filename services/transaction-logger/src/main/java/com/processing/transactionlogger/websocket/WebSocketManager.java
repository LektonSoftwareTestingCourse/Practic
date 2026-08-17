package com.processing.transactionlogger.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * Менеджер WebSocket-сессий.
 * Хранит активные подключения и рассылает сообщения всем клиентам.
 */
public interface WebSocketManager {

    /**
     * Регистрирует новую WebSocket-сессию
     *
     * @param session открытая сессия клиента
     */
    void addSession(WebSocketSession session);

    /**
     * Удаляет WebSocket-сессию при разрыве соединения
     *
     * @param session закрытая сессия клиента
     */
    void removeSession(WebSocketSession session);

    /**
     * Отправляет сообщение всем активным клиентам.
     * Недоступные сессии удаляются из пула автоматически.
     *
     * @param message текст сообщения в формате JSON
     */
    void broadcast(String message);
}
