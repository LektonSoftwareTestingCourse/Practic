package com.processing.transactionlogger.controller;

import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.transactionlogger.dto.ChartBucket;
import com.processing.transactionlogger.dto.DashboardStatsResponse;
import com.processing.transactionlogger.service.TransactionService;
import com.processing.transactionlogger.specification.ChartsFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер Dashboard API — статистика и последние транзакции.
 * Gateway перенаправляет сюда запросы от Web Dashboard.
 */
@Validated
@Tag(name = "Dashboard", description = "Статистика и последние транзакции")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TransactionService transactionService;

    /**
     * Возвращает агрегированную статистику по всем транзакциям.
     *
     * @return счётчики, суммы, процент одобрения, транзакций в минуту
     */
    @Operation(summary = "Агрегированная статистика", responses = {
            @ApiResponse(responseCode = "200", description = "Статистика по всем транзакциям")
    })
    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return transactionService.getStats();
    }

    /**
     * Возвращает последние транзакции, отсортированные по {@code createdAt DESC}
     *
     * @param limit максимальное число записей (1–500, по умолчанию 20)
     * @return список транзакций
     */
    @Operation(summary = "Последние транзакции", responses = {
            @ApiResponse(responseCode = "200", description = "Список транзакций, отсортированных по createdAt DESC")
    })
    @GetMapping("/recent")
    public List<TransactionResponse> getRecent(@Positive(message = "limit must be positive")
                                               @Max(value = 500, message = "limit must not exceed 500")
                                               @RequestParam(defaultValue = "20") int limit) {
        return transactionService.getRecent(limit);
    }

    /**
     * Возвращает агрегированные по времени данные для графиков Dashboard.
     *
     * @param filter гранулярность ({@code hour}/{@code day}) и опциональный диапазон {@code from}/{@code to}
     * @return упорядоченные по времени корзины со счётчиками и суммами
     */
    @Operation(summary = "Агрегация по часам/дням", responses = {
            @ApiResponse(responseCode = "200", description = "Временные корзины со счётчиками и суммами"),
            @ApiResponse(responseCode = "400", description = "Невалидные параметры")
    })
    @GetMapping("/charts")
    public List<ChartBucket> getCharts(@Valid @ModelAttribute ChartsFilter filter) {
        return transactionService.getCharts(filter);
    }
}
