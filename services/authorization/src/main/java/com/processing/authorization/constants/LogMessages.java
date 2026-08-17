package com.processing.authorization.constants;

/**
 * Константы для логирования операций авторизации и отката.
 * <p>
 * Содержит предопределённые сообщения для единообразного логирования
 * всех бизнес-сценариев и ошибок.
 * <p>
 * Разделены на группы:
 * <ul>
 * <li><b>Авторизация</b> — успешные и отклонённые сценарии</li>
 * <li><b>Резервирование</b> — успешные операции</li>
 * <li><b>Откат</b> — успешные и отклонённые сценарии</li>
 * <li><b>Технические</b> — дублирование ключей, ошибки</li>
 * </ul>
 *
 * @param message сообщение логгирования
 *
 * @see com.processing.authorization.services.AuthService
 * @see com.processing.authorization.controller.AuthControllerImpl
 */
public record LogMessages(String message) {
    public static final String MESSAGE_AUTH_APPROVED = "AUTHORIZATION APPROVED";

    public static final String MESSAGE_AUTH_DECLINED_NO_CARD = "AUTHORIZATION DECLINED WITH CARD NOT FOUND";
    public static final String MESSAGE_AUTH_DECLINED_STATUS = "AUTHORIZATION DECLINED WITH CARD STATUS";
    public static final String MESSAGE_AUTH_DECLINED_EXPIRE = "AUTHORIZATION DECLINED WITH EXPIRY DATE";
    public static final String MESSAGE_AUTH_DECLINED_FUNDS = "AUTHORIZATION DECLINED WITH INSUFFICIENT FUNDS";
    public static final String MESSAGE_AUTH_DECLINED_LIMITS = "AUTHORIZATION DECLINED WITH EXCEEDING DAILY OR MONTHLY LIMIT";
    public static final String MESSAGE_AUTH_DECLINED_UNAVAILABLE_SERVICE =
            "AUTHORIZATION DECLINED WITH UPSTREAM SERVICE UNAVAILABLE";
    public static final String MESSAGE_AUTH_DECLINED_UNKNOWN = "AUTHORIZATION DECLINED WITH UNKNOWN REASON";

    public static final String MESSAGE_RESERVE_APPROVED = "RESERVE APPROVED";
    public static final String MESSAGE_GET_CARD_APPROVED = "MESSAGE_GET_CARD_APPROVED";

    public static final String MESSAGE_ROLLBACK_APPROVED = "ROLLBACK APPROVED";

    public static final String MESSAGE_ROLLBACK_DECLINED_NO_CARD = "ROLLBACK DECLINED WITH TRANSACTION NOT FOUND";
    public static final String MESSAGE_ROLLBACK_DECLINED_CONFLICT = "ROLLBACK DECLINED WITH CONFLICT STATE";
    public static final String MESSAGE_ROLLBACK_DECLINED_UNAVAILABLE_SERVICE =
            "ROLLBACK DECLINED WITH UPSTREAM SERVICE UNAVAILABLE";
    public static final String MESSAGE_ROLLBACK_DECLINED_UNKNOWN = "ROLLBACK DECLINED WITH UNKNOWN REASON";

    public static final String MESSAGE_DUPLICATE_KEY = "KEY DUPLICATION DETECTED, RETRYING...";
}
