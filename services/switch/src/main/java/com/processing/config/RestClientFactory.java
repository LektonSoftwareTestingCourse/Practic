package com.processing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Фабрика REST-клиентов с настраиваемыми таймаутами для Authorization и Merchant Acquirer.
 */
@Configuration
public class RestClientFactory {

    /**
     * Общий REST-клиент для Authorization и Merchant Acquirer.
     *
     * @param switchProperties конфигурация таймаутов
     * @return {@link RestClient} с auth read timeout
     */
    @Bean
    public RestClient restClient(SwitchProperties switchProperties) {
        SwitchProperties.HttpProperties http = switchProperties.http();
        return RestClient.builder()
                .requestFactory(requestFactory(http.connectTimeoutMs(), http.authReadTimeoutMs()))
                .build();
    }

    /**
     * Создаёт фабрику HTTP-запросов с заданными таймаутами.
     *
     * @param connectTimeoutMs таймаут подключения (мс)
     * @param readTimeoutMs    таймаут чтения (мс)
     * @return настроенная {@link SimpleClientHttpRequestFactory}
     */
    private static SimpleClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
}
