package com.processing.cardmanagement.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Transactional(propagation = Propagation.NESTED)
public class TransactionRunnerImpl implements TransactionRunner {

    @Override
    public void run(Runnable operation) {
        operation.run();
    }

    @Override
    public <T> T runSupplier(Supplier<T> operation) {
        return operation.get();
    }

    @Override
    public <T> T runSupplierWithNestedQuery(Function<TransactionRunner, T> supplier) {
        return supplier.apply(this);
    }
}
