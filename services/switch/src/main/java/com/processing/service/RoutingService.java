package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * Определяет {@code issuerId} эмитента по BIN (первые 6 цифр PAN).
 */
@Service
public class RoutingService {

    private final Map<String, String> binTable;

    /**
     * @param switchProperties конфигурация с таблицей BIN → issuerId
     */
    public RoutingService(SwitchProperties switchProperties) {
        this.binTable = Map.copyOf(switchProperties.binRouting());
    }

    /**
     * Возвращает идентификатор эмитента для заданного PAN.
     *
     * @param pan номер карты (минимум 6 цифр)
     * @return issuerId из таблицы маршрутизации
     * @throws UnknownBinException если PAN короче 6 символов или BIN не найден
     */
    public String getIssuerIdByPan(String pan) {
        if (pan == null || pan.length() < 6) {
            throw new UnknownBinException("null-or-short");
        }
        String bin = pan.substring(0, 6);
        String issuerId = binTable.get(bin);
        if (issuerId == null) {
            throw new UnknownBinException(bin);
        }
        return issuerId;
    }
}
