package com.processing.exception;

/**
 * Исключение при отсутствии BIN в таблице маршрутизации или некорректном PAN.
 */
public class UnknownBinException extends RuntimeException {

    /**
     * @param bin BIN из PAN или маркер {@code "null-or-short"} для некорректного PAN
     */
    public UnknownBinException(String bin) {
        super(String.format("Unknown BIN '%s'", bin));
    }
}
