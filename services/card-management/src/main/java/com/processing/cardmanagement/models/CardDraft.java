package com.processing.cardmanagement.models;

import java.math.BigDecimal;

public record CardDraft(
    String bin,
    String cardholderName,
    CardStatus status,
    String currencyCode,
    BigDecimal dailyLimit,
    BigDecimal monthlyLimit,
    BigDecimal initialBalance
) {}
