package com.processing.cardmanagement.options;

import java.math.BigDecimal;

/**
 * Значения по умолчанию для CardService
 */
public interface CardServiceDefaults {

    /**
     * @return значение лимита для пагинации
     */
    int pageLimit();

    /**
     * @return значение сдвига для пагинации
     */
    long pageOffset();

    /**
     * @return код валюты
     */
    String currencyCode();

    /**
     * @return дневной лимит карты
     */
    BigDecimal dailyLimit();

    /**
     * @return месячный лимит карты
     */
    BigDecimal monthlyLimit();

    /**
     * @return баланс карты
     */
    BigDecimal balance();
}
