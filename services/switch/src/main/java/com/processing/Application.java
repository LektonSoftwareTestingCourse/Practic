package com.processing;

import com.processing.config.SwitchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Точка входа Spring Boot-приложения Switch — маршрутизатора транзакций СМП.
 */
@SpringBootApplication
@EnableConfigurationProperties(SwitchProperties.class)
public class Application {

    /**
     * Запускает приложение Switch.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
