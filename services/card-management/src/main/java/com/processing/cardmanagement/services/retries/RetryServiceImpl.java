package com.processing.cardmanagement.services.retries;

import com.processing.cardmanagement.exceptions.OutOfRetriesException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class RetryServiceImpl implements RetryService {

    public <T> T supply(int maxRetries, Supplier<T> supplier) {
        int retries = 0;
        while (retries <= maxRetries) {
            try {
                return supplier.get();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                if (++retries > maxRetries) {
                    throw new OutOfRetriesException(maxRetries, ex);
                }
            }
        }
        throw new RuntimeException("Unexpected loop end");
    }
}
