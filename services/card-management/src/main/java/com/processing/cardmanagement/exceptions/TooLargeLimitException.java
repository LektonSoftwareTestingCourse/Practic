package com.processing.cardmanagement.exceptions;

public final class TooLargeLimitException extends CardManagementException {

    public TooLargeLimitException(int actualLimit, int maxPageLimit) {
        super("Page limit (" + actualLimit + " received) can not be more than " + maxPageLimit);
    }
}
