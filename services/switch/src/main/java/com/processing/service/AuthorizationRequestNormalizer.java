package com.processing.service;

import com.processing.common.dto.authorization.AuthorizationRequest;

/**
 * Нормализует поля {@link AuthorizationRequest} перед отправкой в downstream-сервисы.
 */
final class AuthorizationRequestNormalizer {

    private static final int TERMINAL_ID_LENGTH = 8;
    private static final int MERCHANT_ID_LENGTH = 15;
    private static final String MERCHANT_PREFIX = "MERCH";

    private AuthorizationRequestNormalizer() {
    }

    /**
     * Приводит terminalId и merchantId к формату, ожидаемому Authorization.
     *
     * @param request исходный запрос
     * @return копия с нормализованными идентификаторами
     */
    static AuthorizationRequest normalize(AuthorizationRequest request) {
        return new AuthorizationRequest(
                request.mti(),
                request.stan(),
                request.pan(),
                request.processingCode(),
                request.amount(),
                request.currencyCode(),
                request.transmissionDateTime(),
                normalizeTerminalId(request.terminalId()),
                request.terminalType(),
                normalizeMerchantId(request.merchantId()),
                request.mcc(),
                request.acquirerId(),
                request.issuerId()
        );
    }

    /**
     * Дополняет или обрезает terminalId до 8 символов.
     *
     * @param terminalId исходный идентификатор терминала
     * @return нормализованный terminalId
     */
    static String normalizeTerminalId(String terminalId) {
        if (terminalId == null || terminalId.isBlank()) {
            return terminalId;
        }
        if (terminalId.length() >= TERMINAL_ID_LENGTH) {
            return terminalId.substring(0, TERMINAL_ID_LENGTH);
        }
        return padEnd(terminalId, TERMINAL_ID_LENGTH, '0');
    }

    /**
     * Приводит merchantId к 15 символам (обрезка, удаление лишнего нуля после MERCH, дополнение).
     *
     * @param merchantId исходный идентификатор мерчанта
     * @return нормализованный merchantId
     */
    static String normalizeMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return merchantId;
        }
        if (merchantId.length() == MERCHANT_ID_LENGTH) {
            return merchantId;
        }
        if (merchantId.length() == MERCHANT_ID_LENGTH + 1
                && merchantId.startsWith(MERCHANT_PREFIX)
                && merchantId.charAt(MERCHANT_PREFIX.length()) == '0') {
            return merchantId.substring(0, MERCHANT_PREFIX.length()) + merchantId.substring(MERCHANT_PREFIX.length() + 1);
        }
        if (merchantId.length() > MERCHANT_ID_LENGTH) {
            return merchantId.substring(0, MERCHANT_ID_LENGTH);
        }
        return padEnd(merchantId, MERCHANT_ID_LENGTH, '0');
    }

    /**
     * Дополняет строку символом {@code padChar} до заданной длины.
     *
     * @param value   исходная строка
     * @param length  целевая длина
     * @param padChar символ дополнения
     * @return дополненная строка
     */
    private static String padEnd(String value, int length, char padChar) {
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < length) {
            builder.append(padChar);
        }
        return builder.toString();
    }
}
