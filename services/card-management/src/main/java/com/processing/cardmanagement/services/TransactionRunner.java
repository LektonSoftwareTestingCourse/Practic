package com.processing.cardmanagement.services;

import java.util.function.Function;
import java.util.function.Supplier;

public interface TransactionRunner {

    void run(Runnable operation);

    <T> T runSupplier(Supplier<T> operation);

    <T> T runSupplierWithNestedQuery(Function<TransactionRunner, T> supplier);
}
