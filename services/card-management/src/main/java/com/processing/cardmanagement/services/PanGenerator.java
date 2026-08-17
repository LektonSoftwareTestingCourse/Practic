package com.processing.cardmanagement.services;

/**
 * Интерфейс для валидации и генерации номеров банковских карт (PAN)
 */
public interface PanGenerator {

    /**
     * Проверяет корректность номера карты по алгоритму Луна
     * @param pan номер карты из 16 цифр
     * @return {@code true} если PAN валиден, иначе {@code false}
     */
    boolean isValid(String pan);

    /**
     * Генерирует валидный номер карты по заданному BIN-префиксу
     *
     * @param bin 6-значный банковский индетификационный номер
     * @return валидный 16-значный PAN
     */
    String generatePan(String bin);
}
