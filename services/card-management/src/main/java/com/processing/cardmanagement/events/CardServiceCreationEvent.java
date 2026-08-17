package com.processing.cardmanagement.events;

public record CardServiceCreationEvent(long amount) implements CardOutboxEvent {}
