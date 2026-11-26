package com.processing.apitests.support;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared request defaults and API-only test-data builders. No direct database access is used. */
public abstract class ApiTestSupport {
    private static final String GATEWAY_URL = setting(
            "smp.gateway.url", "SMP_GATEWAY_URL", "http://localhost:8080");
    private static final RequestSpecification GATEWAY_SPEC = new RequestSpecBuilder()
            .setBaseUri(GATEWAY_URL)
            .setAccept("*/*")
            .build();
    private static final RequestSpecification SWITCH_SPEC = serviceSpec(
            "smp.switch.url", "SMP_SWITCH_URL", "http://localhost:8082");
    private static final RequestSpecification AUTHORIZATION_SPEC = serviceSpec(
            "smp.authorization.url", "SMP_AUTHORIZATION_URL", "http://localhost:8083");
    private static final RequestSpecification LOGGER_SPEC = serviceSpec(
            "smp.logger.url", "SMP_LOGGER_URL", "http://localhost:8088");
    private static final AtomicInteger STAN_SEQUENCE = new AtomicInteger(
            ThreadLocalRandom.current().nextInt(100_000, 900_000));

    protected static RequestSpecification gateway() {
        return RestAssured.given().spec(GATEWAY_SPEC);
    }

    protected static RequestSpecification switchService() {
        return RestAssured.given().spec(SWITCH_SPEC);
    }

    protected static RequestSpecification authorizationService() {
        return RestAssured.given().spec(AUTHORIZATION_SPEC);
    }

    protected static RequestSpecification loggerService() {
        return RestAssured.given().spec(LOGGER_SPEC);
    }

    private static RequestSpecification serviceSpec(String property, String environment, String fallback) {
        return new RequestSpecBuilder()
                .setBaseUri(setting(property, environment, fallback))
                .setAccept("*/*")
                .build();
    }

    protected static CardFixture createCard() {
        return createCard(new BigDecimal("100000000"),
                new BigDecimal("50000000"), new BigDecimal("300000000"));
    }

    protected static CardFixture createCard(BigDecimal balance, BigDecimal dailyLimit, BigDecimal monthlyLimit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bin", "400000");
        body.put("cardholderName", "API TEST USER");
        body.put("currencyCode", "643");
        body.put("dailyLimit", dailyLimit);
        body.put("monthlyLimit", monthlyLimit);
        body.put("initialBalance", balance);

        Response response = gateway()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/cards");
        Assertions.assertEquals(201, response.statusCode(),
                "Card fixture must be created through the API. Body: " + response.asString());
        String pan = response.jsonPath().getString("pan");
        Assertions.assertNotNull(pan, "Card creation response must contain PAN");
        return new CardFixture(pan, response);
    }

    protected static String nextStan() {
        return String.format(Locale.ROOT, "%06d", STAN_SEQUENCE.getAndIncrement() % 1_000_000);
    }

    protected static String nextRrn() {
        return String.format(Locale.ROOT, "%012d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
    }

    /** Returns a syntactically valid, Luhn-checksummed PAN that was not created by this test. */
    protected static String unknownPan() {
        String body = "400000" + String.format(Locale.ROOT, "%09d",
                ThreadLocalRandom.current().nextLong(1_000_000_000L));
        int sum = 0;
        boolean doubleDigit = true;
        for (int index = body.length() - 1; index >= 0; index--) {
            int digit = body.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        int checkDigit = (10 - sum % 10) % 10;
        return body + checkDigit;
    }

    protected static Map<String, Object> transactionBody(String pan, String stan, BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mti", "0100");
        body.put("stan", stan);
        body.put("pan", pan);
        body.put("processingCode", "000000");
        body.put("amount", amount);
        body.put("currencyCode", "643");
        body.put("transmissionDateTime", Instant.now().minusSeconds(2).toString());
        body.put("terminalId", "TERM0001");
        body.put("terminalType", "POS");
        body.put("merchantId", "MERCH0000000005");
        body.put("mcc", "5411");
        body.put("acquirerId", "ACQ001");
        return body;
    }

    protected static Map<String, Object> authorizationBody(String pan, String stan, BigDecimal amount) {
        Map<String, Object> body = transactionBody(pan, stan, amount);
        body.put("issuerId", "ISS001");
        return body;
    }

    protected static Map<String, Object> storedTransactionBody(UUID id, String pan, String stan,
                                                                 BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id.toString());
        body.put("mti", "0110");
        body.put("stan", stan);
        body.put("rrn", nextRrn());
        body.put("pan", pan);
        body.put("processingCode", "000000");
        body.put("amount", amount);
        body.put("currencyCode", "643");
        body.put("terminalId", "TERM0001");
        body.put("terminalType", "POS");
        body.put("merchantId", "MERCH0000000005");
        body.put("mcc", "5411");
        body.put("acquirerId", "ACQ001");
        body.put("issuerId", "ISS001");
        body.put("acquiringFee", BigDecimal.ZERO);
        body.put("status", "APPROVED");
        body.put("declineReason", null);
        body.put("authCode", "TST001");
        body.put("processingTimeMs", 4);
        body.put("transmissionDateTime", Instant.now().minusSeconds(2).toString());
        body.put("createdAt", Instant.now().toString());
        return body;
    }

    protected record CardFixture(String pan, Response createResponse) {
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : value;
    }
}
