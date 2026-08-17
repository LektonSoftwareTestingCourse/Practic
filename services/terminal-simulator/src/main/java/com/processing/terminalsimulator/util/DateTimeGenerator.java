package com.processing.terminalsimulator.util;


import com.processing.terminalsimulator.model.PartofDay;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DateTimeGenerator {
    public Instant generate(PartofDay partOfDay) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int year = 2026;
        int month = random.nextInt(1, 13);
        int day = random.nextInt(1, 28);

        int hour;
        if (PartofDay.NIGHT.equals(partOfDay)) {
            hour = random.nextInt(1, 5);
        } else {
            hour = random.nextInt(9, 22);
        }
        int minute = random.nextInt(0, 60);
        int second = random.nextInt(0, 60);

        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second);
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
