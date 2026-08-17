package com.processing.gateway.logging;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogDataMasker {

    private static final Map<Pattern, String> PATTERN_MASK_MAP = Map.of(
            // json pan
            Pattern.compile("(?i)\"pan\"\\s*:\\s*\"(\\d{6})\\d{6,9}(\\d{4})\""), "\"pan\":\"$1****$2\"",

            // json cvv
            Pattern.compile("(?i)\"cvv\"\\s*:\\s*\"\\d{3,4}\""), "\"cvv\":\"***\""
    );

    public String maskData(String data) {
        String maskedData = data;

        for (Map.Entry<Pattern, String> entry : PATTERN_MASK_MAP.entrySet()) {
            Matcher matcher = entry.getKey().matcher(maskedData);

            if (matcher.find()) {
                maskedData = matcher.replaceAll(entry.getValue());
            }
        }

        return maskedData;
    }
}
