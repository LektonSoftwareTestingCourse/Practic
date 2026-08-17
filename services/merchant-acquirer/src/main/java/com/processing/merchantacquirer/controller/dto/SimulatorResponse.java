package com.processing.merchantacquirer.controller.dto;

import com.processing.common.dto.authorization.AuthorizationResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Результат симуляции: статистика отправленных транзакций")
public record SimulatorResponse(
    @Schema(description = "Всего отправлено транзакций", example = "50")
    int totalSubmitted,

    @Schema(description = "Количество одобренных транзакций", example = "47")
    int approved,

    @Schema(description = "Количество отклонённых транзакций", example = "3")
    int declined,

    @Schema(description = "Время выполнения симуляции в миллисекундах", example = "1234")
    int elapsedMs,

    @Schema(description = "Список авторизационных ответов по каждой транзакции")
    List<AuthorizationResponse> transactions) {}
