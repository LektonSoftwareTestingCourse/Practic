package com.processing.transactionlogger.controller;

import com.processing.common.dto.ErrorResponse;
import com.processing.common.dto.transactionlogger.TransactionRequest;
import com.processing.common.dto.transactionlogger.TransactionResponse;
import com.processing.common.dto.transactionlogger.TransactionStoredResponse;
import com.processing.transactionlogger.service.TransactionStoreResult;
import com.processing.transactionlogger.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Внутренний контроллер для приёма транзакций от Switch.
 * Предназначен для внутренних вызовов от Switch.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "Internal transactions", description = "Внутренний API для сохранения транзакций, полученных от Switch")
public class InternalTransactionController {
    private final TransactionService transactionService;

    /**
     * Сохраняет транзакцию, полученную от Switch.
     * Идемпотентен: повторный запрос с тем же {@code id} и теми же данными вернёт HTTP 200.
     *
     * @param request данные транзакции
     * @return HTTP 201 с {@link TransactionStoredResponse} при новой записи,
     *         HTTP 200 с {@link TransactionResponse} при уже существующей
     */
    @PostMapping("/log")
    @Operation(
            summary = "Сохранение транзакции",
            description = "Сохраняет транзакцию, полученную от Switch. Если транзакция с таким id уже существует, "
                    + "возвращает существующую запись без создания дубликата."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Транзакция сохранена",
            content = @Content(schema = @Schema(implementation = TransactionStoredResponse.class))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Транзакция с таким id уже существует и совпадает с запросом",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Тело запроса невалидно",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Транзакция с таким id уже существует с другими данными или нарушает ограничения БД",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "503",
            description = "База данных transaction-logger недоступна",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервиса",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<?> store(@Valid @RequestBody TransactionRequest request) {
        TransactionStoreResult result = transactionService.store(request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.storedTransaction());
        }

        return ResponseEntity.ok(result.existingTransaction());
    }
}
