package com.processing.merchantacquirer.client.dto;

public record CardDataResponse(
    String id,
    String pan,
    String bin,
    String cardholderName,
    String expiryDate,
    String status,
    String currencyCode,
    String dailyLimit,
    String monthlyLimit,
    String availableBalance,
    String issuerId,
    String createdAt) {}
