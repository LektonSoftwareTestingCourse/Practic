package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.controller.dto.AcquirerFeeRequest;
import com.processing.merchantacquirer.controller.dto.AcquirerFeeResponse;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.repository.AcquirerFeeRepositoryPort;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class AcquirerProvider {
    private final AcquirerFeeRepositoryPort repository;

    public void saveAll(List<AcquirerFee> fees) {
        repository.saveFees(fees);
        log.info("Saved {} acquiring fees", fees.size());
    }

    public AcquirerFeeResponse getAcquirerFee(AcquirerFeeRequest request) {
        AcquirerFee acquirerFee = repository.findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
                request.transmissionDateTime(), request.stan(), request.terminalId(),
                request.amount(), request.pan());
        if (acquirerFee == null) {
            throw new ResourceNotFoundException("Acquirer fee not found for stan = " + request.stan());
        }


        return new AcquirerFeeResponse(acquirerFee.getAcquirerFee());
    }
}
