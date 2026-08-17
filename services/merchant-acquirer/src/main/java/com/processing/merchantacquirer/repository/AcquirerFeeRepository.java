package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import com.processing.merchantacquirer.domain.repository.AcquirerFeeRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcquirerFeeRepository extends JpaRepository<AcquirerFee, Long>, AcquirerFeeRepositoryPort {
    @Override
    default void saveFees(List<AcquirerFee> fees) {
        saveAll(fees);
    }
}
