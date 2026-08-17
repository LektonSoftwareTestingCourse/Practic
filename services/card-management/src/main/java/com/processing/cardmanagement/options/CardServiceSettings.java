package com.processing.cardmanagement.options;

/**
 * Настройки сервиса управления картами
 */
public interface CardServiceSettings {

    /**
     * @return срок действия карты
     */
    int cardValidityPeriod();

    /**
     * @return максимальный размер страницы для пагинации
     */
    int maxPageLimit();

    /**
     * @return максимальное количество повторов для создания карты (при коллизии)
     */
    int maxCardCreationRetries();
}
