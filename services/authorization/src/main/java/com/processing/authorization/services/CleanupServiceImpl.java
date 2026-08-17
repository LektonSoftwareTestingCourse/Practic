package com.processing.authorization.services;

import com.processing.authorization.repositories.LimitUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final LimitUsageRepository limitUsageRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 2 * ?")
    public void cleanPreviousMonthRecords() {
        log.info("Starting cleanup of limit_usage records for previous month");
        try {
            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            LocalDate firstDayOfPreviousMonth = now.minusMonths(1).withDayOfMonth(1);
            LocalDate lastDayOfPreviousMonth = now.minusMonths(1).withDayOfMonth(
                    now.minusMonths(1).lengthOfMonth()
            );

            log.info("Deleting records from {} to {}", firstDayOfPreviousMonth, lastDayOfPreviousMonth);

            int deleted = limitUsageRepository.deleteByUsageDateBetween(
                    firstDayOfPreviousMonth,
                    lastDayOfPreviousMonth
            );

            log.info("Deleted {} limit_usage records for previous month", deleted);

        } catch (Exception e) {
            log.error("Failed to clean up limit_usage records for previous month", e);
        }
    }
}
