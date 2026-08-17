package com.processing.cardmanagement.events;

public sealed interface CardEvent permits CardOutboxEvent, CardsAmountUpdatedEvent {}
