package com.processing.cardmanagement.services.retries;

import com.processing.cardmanagement.exceptions.OutOfRetriesException;

import java.util.function.Supplier;

/**
 * Сервис для выполнения каких-либо операций с определенным количеством повторений
 */
public interface RetryService {

    /**
     * Выполняет операцию с возвращаемым значением
     *
     * @param maxRetries максимальное количество повторений
     * @param supplier   опреация
     * @param <T>        возвращаемый орерацией тип
     * @return созданный в результате операции объект
     * @throws OutOfRetriesException если закончилось количество попыток
     */
    <T> T supply(int maxRetries, Supplier<T> supplier);
}
