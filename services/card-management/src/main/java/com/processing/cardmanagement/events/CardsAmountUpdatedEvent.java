package com.processing.cardmanagement.events;

public record CardsAmountUpdatedEvent(long amount) implements CardEvent {}
