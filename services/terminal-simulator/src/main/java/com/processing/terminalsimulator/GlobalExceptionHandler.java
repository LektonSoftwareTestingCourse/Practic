package com.processing.terminalsimulator;
import com.processing.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        ErrorResponse response = new ErrorResponse("Validation failed", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.badRequest().body(response);  // 400
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = new ErrorResponse("Illegal state", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);  // 422
    }
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccess(ResourceAccessException ex) {
        ErrorResponse response = new ErrorResponse("Resource access", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);  // 503
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse response = new ErrorResponse("Invalid request format", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", "0");
        return ResponseEntity.badRequest().body(response);  // 400
    }
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        ErrorResponse response = new ErrorResponse("Http connection error", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(ex.getStatusCode()).body(response);  // 4xx
    }
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClient(RestClientException ex) {
        log.error("Gateway communication error", ex);
        ErrorResponse response = new ErrorResponse("Gateway communication error", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", "0");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);  // 502
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected internal server error", ex);
        ErrorResponse response = new ErrorResponse("Unexpected internal server error", ex.getMessage(),
                (LocalDateTime.now()).toInstant(ZoneOffset.UTC), "terminal-simulator", String.valueOf(0));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500
    }
}
