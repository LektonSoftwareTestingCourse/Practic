package com.processing.cardmanagement.events;

public record CardServicePatchEvent(String pan) implements CardOutboxEvent {}
