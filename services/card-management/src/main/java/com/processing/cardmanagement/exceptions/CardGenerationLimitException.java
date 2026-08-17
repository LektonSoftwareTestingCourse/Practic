package com.processing.cardmanagement.exceptions;

public final class CardGenerationLimitException extends RuntimeException {

    public CardGenerationLimitException(int maxCount) {
        super("Count exceeds maximum value: " + maxCount);
    }
}
