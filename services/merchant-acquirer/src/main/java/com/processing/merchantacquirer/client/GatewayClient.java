package com.processing.merchantacquirer.client;

import com.processing.merchantacquirer.client.dto.CardsRequest;
import com.processing.merchantacquirer.client.dto.CardsResponse;
import com.processing.merchantacquirer.exception.ExternalServiceException;
import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class GatewayClient {
  private static final int MAX_RETRIES = 3;
  private static final long DEFAULT_RETRY_AFTER_MS = 1000L;

  private final RestClient restClient;

  public GatewayClient(RestClient.Builder builder, @Value("${gateway.url}") String gatewayUrl) {
      var settings = ClientHttpRequestFactorySettings.defaults()
              .withConnectTimeout(Duration.ofSeconds(5))
              .withReadTimeout(Duration.ofSeconds(30));
      this.restClient = builder
              .baseUrl(gatewayUrl)
              .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
              .messageConverters(GatewayClient::allowOctetStreamAsJson)
              .build();
  }

  private static void allowOctetStreamAsJson(List<HttpMessageConverter<?>> converters) {
    for (var converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
        var types = new ArrayList<>(jackson.getSupportedMediaTypes());
        if (!types.contains(MediaType.APPLICATION_OCTET_STREAM)) {
          types.add(MediaType.APPLICATION_OCTET_STREAM);
          jackson.setSupportedMediaTypes(types);
        }
      }
    }
  }

  public CardsResponse getCards(CardsRequest request) {
    try {
      return restClient.get()
              .uri(uriBuilder -> {
                uriBuilder.path("/api/cards");
                if (request.limit() > 0) {
                    uriBuilder.queryParam("limit", request.limit());
                }
                if (request.offset() > 0) {
                    uriBuilder.queryParam("offset", request.offset());
                }
                if (request.status() != null) {
                  uriBuilder.queryParam("status", request.status());
                }
                if (request.bin() != null) {
                  uriBuilder.queryParam("bin", request.bin());
                }
                return uriBuilder.build();
              })
              .retrieve()
              .body(CardsResponse.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.warn("Card management returned HTTP error: status={}, body={}",
              ex.getStatusCode(), ex.getResponseBodyAsString());
      throw ExternalServiceException.fromResponse(ex);
    } catch (Exception ex) {
      log.warn("Card management call failed ({}): {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
      throw new ExternalServiceException("Card management", ex.getMessage(), "0");
    }
  }

  public AuthorizationResponse processAuthorize(AuthorizationRequest authorizationRequest) {
    for (int attempt = 1; ; attempt++) {
      try {
        return restClient
            .post()
                .uri("/api/transactions")
                .body(authorizationRequest)
                .retrieve()
                .body(AuthorizationResponse.class);

      } catch (HttpClientErrorException.TooManyRequests ex) {
        if (attempt >= MAX_RETRIES) {
          log.warn("Gateway rate-limited STAN {}, TerminalID {} after {} attempts, giving up",
                  authorizationRequest.stan(), authorizationRequest.terminalId(), attempt);
          throw ExternalServiceException.fromResponse(ex);
        }
        long delay = retryAfterMs(ex);
        sleepBeforeRetry(delay);
      } catch (HttpClientErrorException | HttpServerErrorException ex) {
        log.warn("Gateway returned HTTP error for STAN {}: status={}, body={}",
                authorizationRequest.stan(), ex.getStatusCode(), ex.getResponseBodyAsString());
        throw ExternalServiceException.fromResponse(ex);
      } catch (Exception ex) {
        log.warn("Gateway call failed for STAN {} ({}): {}",
                authorizationRequest.stan(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        throw new ExternalServiceException("API Gateway", ex.getMessage(), "0");
      }
    }
  }

  private long retryAfterMs(HttpClientErrorException ex) {
    try {
      String raw = ExternalServiceException.fromResponse(ex).getRetryAfterMs();
      long value = raw == null ? 0 : Long.parseLong(raw.trim());
      if (value > 0) {
        return value;
      }
    } catch (Exception ignore) { }
    return DEFAULT_RETRY_AFTER_MS;
  }

  private void sleepBeforeRetry(long baseMs) {
    long jitter = (long) (baseMs * 0.2 * ThreadLocalRandom.current().nextDouble());
    try {
      Thread.sleep(baseMs + jitter);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to retry gateway call", e);
    }
  }
}
