package com.processing.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Контракт получения комиссии эквайринга из Merchant Acquirer Simulator.
 */
public interface AcquiringFeeClient {

    /**
     * Запрашивает комиссию эквайринга для транзакции.
     *
     * @param transmissionDateTime время передачи транзакции
     * @param stan                 системный номер операции
     * @param pan                  номер карты
     * @param terminalId           идентификатор терминала
     * @param amount               сумма транзакции
     * @return комиссия или {@code null}, если сервис недоступен
     */
    BigDecimal fetchAcquiringFee(Instant transmissionDateTime, String stan, String pan, String terminalId, BigDecimal amount);
}
