package com.processing.merchantacquirer.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.processing.common.dto.ErrorResponse;
import lombok.Getter;
import org.springframework.web.client.HttpStatusCodeException;

public class ExternalServiceException extends RuntimeException {
  @Getter private final String serviceName;
  @Getter private final String retryAfterMs;

  private static final ObjectMapper MAPPER = JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .build();

  public ExternalServiceException(String serviceName, String message, String retryAfterMs) {
    super(message);
    this.serviceName = serviceName;
    this.retryAfterMs = retryAfterMs;
  }

  public static ExternalServiceException fromResponse(HttpStatusCodeException ex) {
    try {
      ErrorResponse response = MAPPER.readValue(ex.getResponseBodyAsString(), ErrorResponse.class);
      return new ExternalServiceException(
          response.serviceName(), response.message(), response.retryAfterMs());
    } catch (JsonProcessingException e) {
      ExternalServiceException fallback = new ExternalServiceException("Merchant acquirer simulator", ex.getMessage(), "0");
      fallback.initCause(e);
      return fallback;
    }
  }
}
