package com.processing.merchantacquirer.domain.service;

public final class MaskerPan {
    private MaskerPan() {}

    public static String mask(String pan) {
        if (pan == null || pan.length() < 10) {
            return "****";
        }
        return pan.substring(0, 6) + "*".repeat(pan.length() - 10) + pan.substring(pan.length() - 4);
    }
}
