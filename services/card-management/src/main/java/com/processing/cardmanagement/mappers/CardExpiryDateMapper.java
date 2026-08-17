package com.processing.cardmanagement.mappers;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Component
public final class CardExpiryDateMapper {

    private static final DateTimeFormatter EXPIRY_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMyy");

    public String asString(YearMonth date) {
        return date != null ? date.format(EXPIRY_DATE_FORMATTER) : null;
    }

    public YearMonth asYearMonth(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        return YearMonth.parse(dateStr, EXPIRY_DATE_FORMATTER);
    }
}
