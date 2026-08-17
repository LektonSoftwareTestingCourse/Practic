package com.processing.authorization.events;

import com.processing.common.utils.events.EventNotifier;

/**
 * Нотификатор событий авторизации.
 * <p>
 * Рассылает события всем зарегистрированным слушателям.
 * Используется для:
 * <ul>
 * <li>Логирования бизнес-операций</li>
 * </ul>
 *
 * @see AuthorizationEvent
 */
public interface AuthorizationEventNotifier extends EventNotifier<AuthorizationEvent> {
}
