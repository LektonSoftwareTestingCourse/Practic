package com.processing.cardmanagement.events;

import com.processing.cardmanagement.models.CardStatus;

import java.util.Map;

public record CardsBatchGeneratedEvent(Map<CardStatus, Long> statusCount) implements CardOutboxEvent {}
