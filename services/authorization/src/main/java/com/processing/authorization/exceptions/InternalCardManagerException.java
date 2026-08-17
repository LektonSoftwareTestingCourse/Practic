package com.processing.authorization.exceptions;

/**
 * Исключение, выбрасываемое при внутренней ошибке CMS (Card Management System).
 */
public class InternalCardManagerException extends RuntimeException {
    public InternalCardManagerException(String message) {
        super(message);
    }
}
