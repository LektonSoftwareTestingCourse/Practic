package com.processing.cardmanagement.events;

import java.math.BigDecimal;

public record CardServiceReserveEvent(
    String pan,
    String rrn,
    BigDecimal amount
) implements CardOutboxEvent {}
