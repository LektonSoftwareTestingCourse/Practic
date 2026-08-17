package com.processing.cardmanagement.events;

public record CardServiceDeletionEvent(String pan) implements CardOutboxEvent {}
