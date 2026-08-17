package com.processing.merchantacquirer.exception;

import com.processing.common.dto.ErrorResponse;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.processing.merchantacquirer.controller")
public class GlobalExceptionHandler {
  private static final String SERVICE_NAME = "Merchant acquirer simulator";
  private static final String NO_RETRY = "0";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException ex) {
    String message = ex.getFieldErrors().stream()
            .map(error -> error.getField() + " : " + error.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "Invalid request",
                message,
                Instant.now(),
                SERVICE_NAME,
                NO_RETRY));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ErrorResponse(
                            "Invalid request",
                            ex.getMessage(),
                            Instant.now(),
                            SERVICE_NAME,
                            NO_RETRY));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ErrorResponse(
                            "Invalid request",
                            ex.getMessage(),
                            Instant.now(),
                            SERVICE_NAME,
                            NO_RETRY));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                    new ErrorResponse(
                            "Resource not found",
                            ex.getMessage(),
                            Instant.now(),
                            SERVICE_NAME,
                            NO_RETRY));
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(
                    new ErrorResponse(
                            "External service error",
                            ex.getMessage(),
                            Instant.now(),
                            ex.getServiceName(),
                            ex.getRetryAfterMs()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                    new ErrorResponse(
                            "Internal service error",
                            "Unexpected error in merchant acquirer simulator service" + ex,
                            Instant.now(),
                            SERVICE_NAME,
                            NO_RETRY));
  }
}
