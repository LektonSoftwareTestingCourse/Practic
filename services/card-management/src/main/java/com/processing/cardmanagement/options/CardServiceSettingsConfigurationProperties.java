package com.processing.cardmanagement.options;

import com.processing.common.dto.annotations.NotNegative;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.card-service")
public record CardServiceSettingsConfigurationProperties(

    @Positive
    int cardValidityPeriod,

    @Positive
    int maxPageLimit,

    @NotNegative
    int maxCardCreationRetries
) implements CardServiceSettings {}
