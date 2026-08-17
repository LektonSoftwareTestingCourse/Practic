package com.processing.merchantacquirer.domain.repository;

import com.processing.merchantacquirer.domain.entity.AcquirerFee;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface AcquirerFeeRepositoryPort {
    AcquirerFee findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
            Instant transmissionDateTime, String stan, String terminalId, BigDecimal amount, String pan);
    void saveFees(List<AcquirerFee> fees);
    List<AcquirerFee> findAll();
    long count();
}
