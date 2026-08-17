package com.processing.authorization.client;

import com.processing.authorization.dto.BinLookupResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Client for the external BIN Lookup service.
 *
 * <p>Resolves a card BIN (first 6 digits of PAN) to an issuer ID
 * via a synchronous HTTP call. On any failure (timeout, 404, 500),
 * returns {@link Optional#empty()} to allow graceful degradation.
 */
@Component
public class BinLookupClient {

    private static final Logger LOG = LoggerFactory.getLogger(BinLookupClient.class);

    private final RestClient restClient;
    private final String binLookupUrl;

    /**
     * Constructs the client with configured timeouts.
     *
     * @param binLookupUrl   base URL of the bin-lookup service
     * @param connectTimeout connection timeout in milliseconds
     * @param readTimeout    read timeout in milliseconds
     */
    public BinLookupClient(
            @Value("${bin-lookup.url}") String binLookupUrl,
            @Value("${bin-lookup.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${bin-lookup.read-timeout-ms:5000}") int readTimeout) {
        this.binLookupUrl = binLookupUrl;
        this.restClient = RestClient.builder()
                .requestFactory(clientHttpRequestFactory(connectTimeout, readTimeout))
                .build();
    }

    /**
     * Resolves the issuer ID for a given PAN by extracting the BIN (first 6 digits).
     *
     * @param pan full card number (at least 6 digits)
     * @return issuer ID if resolution succeeded, {@link Optional#empty()} otherwise
     */
    public Optional<String> getIssuerId(String pan) {
        if (pan == null || pan.length() < 6) {
            LOG.warn("Bin lookup skipped: PAN too short");
            return Optional.empty();
        }
        String bin = pan.substring(0, 6);
        try {
            BinLookupResponse response = restClient.get()
                    .uri(binLookupUrl + "/api/bin/{bin}", bin)
                    .retrieve()
                    .body(BinLookupResponse.class);
            String issuerId = response != null ? response.issuerId() : null;
            LOG.debug("Bin lookup OK: bin={} -> issuerId={}", bin, issuerId);
            return Optional.ofNullable(issuerId);
        } catch (Exception e) {
            LOG.warn("Bin lookup failed for bin={}: {}", bin, e.getMessage());
            return Optional.empty();
        }
    }

    private static SimpleClientHttpRequestFactory clientHttpRequestFactory(
            int connectTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
