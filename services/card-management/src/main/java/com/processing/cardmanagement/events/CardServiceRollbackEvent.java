package com.processing.cardmanagement.events;

import java.math.BigDecimal;

public record CardServiceRollbackEvent(
    String pan,
    String rrn,
    BigDecimal amount
) implements CardOutboxEvent {}
