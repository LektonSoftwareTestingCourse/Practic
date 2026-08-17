package com.processing.authorization.controller;

import org.springframework.http.ResponseEntity;

import com.processing.authorization.dto.HealthResponse;
import com.processing.authorization.services.HealthServiceImpl;

/**
 * REST-контроллер для проверки работоспособности сервиса авторизации и его
 * зависимостей.
 * <p>
 * Предоставляет эндпоинт для мониторинга состояния сервиса и всех внешних
 * систем,
 * от которых он зависит. Используется для автоматического определения
 * доступности
 * сервиса в системах оркестрации и балансировки нагрузки.
 * </p>
 * <p>
 * Возвращает HTTP 200, если все зависимости здоровы, или HTTP 503,
 * если хотя бы одна зависимость недоступна.
 * </p>
 *
 * @author core-auth-team
 */
public interface HealthController {
    /**
     * Выполняет проверку работоспособности сервиса авторизации и всех его
     * зависимостей.
     *
     * <p>
     * Метод агрегирует результаты проверки всех внешних сервисов и формирует
     * итоговый статус. Если все сервисы возвращают статус "ok", то общий статус
     * считается "ok" и возвращается HTTP 200. В противном случае статус "degraded"
     * и HTTP 503.
     * </p>
     *
     * @return {@link ResponseEntity} с объектом {@link HealthResponse}, содержащим:
     *         <ul>
     *         <li><b>status</b> - общий статус ("ok" или "degraded")</li>
     *         <li><b>service</b> - имя текущего сервиса ("authorization")</li>
     *         <li><b>dependencies</b> - карта статусов всех зависимых сервисов,
     *         где ключ - URL сервиса, значение - его статус</li>
     *         </ul>
     *
     * @see HealthServiceImpl#healthCheckAllServices()
     * @see HealthResponse
     */
    ResponseEntity<HealthResponse> health();
}
