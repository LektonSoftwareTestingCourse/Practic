package com.processing.authorization.events;

import com.processing.common.utils.events.Event;

/**
 * Иерархия событий для логирования и мониторинга операций авторизации и отката.
 * <p>
 * Использует sealed-интерфейс для строгого контроля над иерархией событий.
 * Все события разделены на логические группы:
 * <ul>
 * <li><b>Авторизация</b> — успешные и отклонённые сценарии</li>
 * <li><b>Откат</b> — успешные и отклонённые сценарии</li>
 * <li><b>Инфраструктура</b> — дублирование ключей, технические события</li>
 * </ul>
 * <p>
 * События публикуются через {@link AuthorizationEventNotifier}
 * и обрабатываются слушателями.
 *
 * @see AuthorizationEventNotifier
 */
public sealed interface AuthorizationEvent extends Event permits
        AuthServiceAuthorizeEvent,
        AuthServiceAuthDeclinedCardStatusEvent,
        AuthServiceAuthDeclinedCardExpiryDateEvent,
        AuthServiceAuthDeclinedFundsEvent,
        AuthServiceAuthDeclinedLimitsEvent,
        AuthServiceAuthDeclineNoCardEvent,
        AuthServiceAuthDeclineNoServiceEvent,
        AuthServiceAuthDeclineUnknownEvent,

        AuthServiceRollbackEvent,
        AuthServiceRollbackDeclineNoCardEvent,
        AuthServiceRollbackDeclineConflictEvent,
        AuthServiceRollbackDeclineNoServiceEvent,
        AuthServiceRollbackDeclineUnknownEvent,

        AuthServiceDuplicateKeyEvent,

        CmsClientGetCardEvent,
        CmsClientReserveEvent,
        CmsClientRollbackEvent {
}
