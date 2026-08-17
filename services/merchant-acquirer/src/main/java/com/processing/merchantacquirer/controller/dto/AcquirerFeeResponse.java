package com.processing.merchantacquirer.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Рассчитанная комиссия эквайрера")
public record AcquirerFeeResponse(
        @Schema(description = "Комиссия эквайрера в копейках", example = "1275")
        BigDecimal acquirerFee
) {
}
