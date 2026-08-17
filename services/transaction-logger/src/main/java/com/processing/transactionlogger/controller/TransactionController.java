package com.processing.transactionlogger.controller;

import com.processing.transactionlogger.dto.TransactionSearchResponse;
import com.processing.transactionlogger.service.TransactionService;
import com.processing.transactionlogger.specification.TransactionFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Контроллер публичного API для поиска транзакций.
 * Gateway перенаправляет сюда запросы от Dashboard и CMS.
 */
@Tag(name = "Transactions", description = "Поиск транзакций")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    /**
     * Ищет транзакции по опциональным фильтрам с пагинацией
     *
     * @param filter параметры фильтрации и пагинации из query-параметров
     * @return постраничный результат с общим счётчиком
     */
    @Operation(
            summary = "Поиск транзакций",
            description = "Фильтр с пагинацией. Все параметры опциональны, активные объединяются через AND.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Результаты поиска"),
                    @ApiResponse(responseCode = "400", description = "Невалидные параметры",
                    content = @Content(schema = @Schema(example = "{\"limit\": "
                            + "\"must be greater than 0\"}")))
            }
    )
    @GetMapping("/search")
    public TransactionSearchResponse search(@Valid @ModelAttribute TransactionFilter filter) {
        return transactionService.search(filter);
    }

    /**
     * Экспортирует транзакции, удовлетворяющие фильтру, в CSV-файл
     *
     * @param filter параметры фильтрации
     * @return CSV-документ ({@code text/csv})
     */
    @Operation(
            summary = "Экспорт транзакций в CSV",
            description = "Выгружает все транзакции, удовлетворяющие фильтру, в формате CSV. "
                    + "Параметры limit/offset не применяются.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV-файл с транзакциями"),
                    @ApiResponse(responseCode = "400", description = "Невалидные параметры фильтра")
            }
    )
    @GetMapping("/export")
    public ResponseEntity<String> export(@Valid @ModelAttribute TransactionFilter filter) {
        String csv = transactionService.exportCsv(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
