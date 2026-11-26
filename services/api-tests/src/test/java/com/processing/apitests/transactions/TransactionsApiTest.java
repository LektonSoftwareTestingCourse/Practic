package com.processing.apitests.transactions;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// Gateway/logger contract: docs/api/openapi.yaml, Gateway routes and TransactionController/TransactionSpecification.
class TransactionsApiTest extends ApiTestSupport {

    @Test
    void gatewayAcceptsTransactionAndLoggerEventuallyMakesItSearchable() {
        // GIVEN: a card created through the public API and a unique STAN.
        CardFixture card = createCard();
        String stan = nextStan();
        Map<String, Object> body = transactionBody(card.pan(), stan, new BigDecimal("1000"));

        // WHEN: submit the transaction through Gateway.
        Response authorization = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/transactions");

        // THEN: check the synchronous authorization response and generated fields.
        assertThat(authorization.statusCode(), equalTo(200));
        authorization.then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator
                .matchesJsonSchemaInClasspath("schemas/authorization-response.schema.json"));
        assertThat(authorization.jsonPath().getString("stan"), equalTo(stan));
        assertThat(authorization.jsonPath().getString("status"), equalTo("APPROVED"));
        assertThat(authorization.jsonPath().getString("responseCode"), equalTo("00"));
        assertThat(authorization.jsonPath().getString("rrn"), matchesPattern("\\d{12}"));
        assertThat(authorization.jsonPath().getString("authCode"), matchesPattern("[A-Z0-9]{6}"));
        assertThat(authorization.jsonPath().getInt("processingTimeMs"), greaterThanOrEqualTo(0));
        assertThat(authorization.jsonPath().get("declineReason"), nullValue());

        // AND: RabbitMQ delivery is asynchronous, so wait for the record rather than sleeping.
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(300, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Response search = gateway().queryParam("pan", card.pan())
                            .queryParam("limit", 20).queryParam("offset", 0)
                            .when().get("/api/transactions/search");
                    assertThat(search.statusCode(), equalTo(200));
                    assertThat(search.jsonPath().getLong("total"), greaterThanOrEqualTo(1L));
                    List<Map<String, Object>> ownTransaction = search.jsonPath().getList(
                            "transactions.findAll { it.stan == '" + stan + "' }");
                    assertThat(ownTransaction, hasSize(1));
                    assertThat(ownTransaction.get(0).get("status").toString(), equalTo("APPROVED"));
                });
    }

    @ParameterizedTest(name = "Gateway rejects invalid transaction field {0}")
    @MethodSource("invalidTransactionRequests")
    void gatewayRejectsOneInvalidFieldAndReturnsGatewayError(String field, Map<String, Object> body,
                                                               String expectedMessage) {
        // GIVEN: every request is valid except for the single field named in the case.

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/transactions");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("error"), equalTo("VALIDATION_ERROR"));
        assertThat(response.jsonPath().getString("serviceName"), equalTo("gateway"));
        assertThat(response.jsonPath().getString("message"), containsString(expectedMessage));
    }

    static Stream<Arguments> invalidTransactionRequests() {
        return Stream.of(
                invalidTransaction("mti", "0200", "Field 'mti' must be '0100'"),
                invalidTransaction("pan", "not-a-pan", "Field 'pan' must be exactly 16 digits"),
                invalidTransaction("amount", new BigDecimal("-1"), "Field 'amount' must be > 0"),
                invalidTransaction("amount", null, "Field 'amount' is required"),
                invalidTransaction("currencyCode", "64", "Field 'currencyCode' must be exactly 3 characters"),
                invalidTransaction("mcc", "ABCD", "Field 'mcc' must be exactly 4 digits"),
                missingTransactionField("stan"),
                missingTransactionField("processingCode"),
                missingTransactionField("transmissionDateTime"),
                missingTransactionField("terminalId"),
                missingTransactionField("merchantId"),
                missingTransactionField("acquirerId")
        );
    }

    private static Arguments invalidTransaction(String field, Object value, String expectedMessage) {
        Map<String, Object> body = transactionBody(unknownPan(), nextStan(), new BigDecimal("100"));
        body.put(field, value);
        return Arguments.of(field, body, expectedMessage);
    }

    private static Arguments missingTransactionField(String field) {
        Map<String, Object> body = transactionBody(unknownPan(), nextStan(), new BigDecimal("100"));
        body.remove(field);
        return Arguments.of(field, body, "Field '" + field + "' is required");
    }

    @Test
    void rejectedGatewayRequestDoesNotCreateAStoredTransaction() {
        // GIVEN
        CardFixture card = createCard();
        String stan = nextStan();
        Map<String, Object> invalid = transactionBody(card.pan(), stan, new BigDecimal("100"));
        invalid.put("mti", "0200");

        // WHEN
        Response rejected = gateway().contentType(ContentType.JSON).body(invalid)
                .when().post("/api/transactions");

        // THEN: the Gateway validation filter rejects the request before it reaches Switch.
        assertThat(rejected.statusCode(), equalTo(400));
        await().during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Response search = gateway().queryParam("pan", card.pan())
                            .queryParam("limit", 20).queryParam("offset", 0)
                            .when().get("/api/transactions/search");
                    assertThat(search.statusCode(), equalTo(200));
                    List<Map<String, Object>> matching = search.jsonPath().getList(
                            "transactions.findAll { it.stan == '" + stan + "' }");
                    assertThat(matching, empty());
                });
    }

    @ParameterizedTest(name = "transaction search rejects invalid limit {0}")
    @MethodSource("invalidSearchLimits")
    void searchRejectsOutOfRangePaginationLimit(int limit) {
        // GIVEN: a limit outside the supported positive range.

        // WHEN
        Response response = gateway().queryParam("limit", limit)
                .when().get("/api/transactions/search");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    static Stream<Arguments> invalidSearchLimits() {
        return Stream.of(Arguments.of(0), Arguments.of(-1), Arguments.of(501));
    }

    @Test
    void transactionSearchReturnsTotalAndPaginatesTheMatchingRecords() {
        // GIVEN
        CardFixture card = createCard();
        String stan = nextStan();
        Response authorization = gateway().contentType(ContentType.JSON)
                .body(transactionBody(card.pan(), stan, new BigDecimal("100")))
                .when().post("/api/transactions");
        assertEquals(200, authorization.statusCode(), authorization.asString());
        String secondStan = nextStan();
        Response secondAuthorization = gateway().contentType(ContentType.JSON)
                .body(transactionBody(card.pan(), secondStan, new BigDecimal("150")))
                .when().post("/api/transactions");
        assertEquals(200, secondAuthorization.statusCode(), secondAuthorization.asString());

        // WHEN / THEN: wait for both writes, then verify that offset 0 and 1 return different rows.
        await().atMost(15, TimeUnit.SECONDS).pollInterval(300, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Response allMatching = gateway().queryParam("pan", card.pan()).queryParam("limit", 20)
                    .queryParam("offset", 0).when().get("/api/transactions/search");
            assertThat(allMatching.statusCode(), equalTo(200));
            assertThat(allMatching.jsonPath().getLong("total"), greaterThanOrEqualTo(2L));
            List<Map<String, Object>> selected = allMatching.jsonPath().getList(
                    "transactions.findAll { it.stan == '" + stan + "' || it.stan == '" + secondStan + "' }");
            assertThat(selected, hasSize(2));

            Response firstPage = gateway().queryParam("pan", card.pan()).queryParam("limit", 1)
                    .queryParam("offset", 0).when().get("/api/transactions/search");
            Response secondPage = gateway().queryParam("pan", card.pan()).queryParam("limit", 1)
                    .queryParam("offset", 1).when().get("/api/transactions/search");
            assertThat(firstPage.jsonPath().getList("transactions"), hasSize(1));
            assertThat(secondPage.jsonPath().getList("transactions"), hasSize(1));
            assertThat(firstPage.jsonPath().getString("transactions[0].stan"),
                    not(equalTo(secondPage.jsonPath().getString("transactions[0].stan"))));
        });
    }

    @Test
    void searchCombinesTransactionFiltersIncludingDeclineReason() {
        // GIVEN: a blocked card creates a declined transaction with a known domain reason.
        CardFixture card = createCard();
        Response blocked = gateway().contentType(ContentType.JSON).body(Map.of("status", "BLOCKED"))
                .when().patch("/api/cards/{pan}", card.pan());
        assertEquals(200, blocked.statusCode(), blocked.asString());
        String stan = nextStan();
        Map<String, Object> body = transactionBody(card.pan(), stan, new BigDecimal("100"));
        Instant transmittedAt = Instant.parse(body.get("transmissionDateTime").toString());

        // WHEN: Switch returns the decline and Logger stores it asynchronously.
        Response authorization = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/transactions");
        assertThat(authorization.statusCode(), equalTo(200));
        assertThat(authorization.jsonPath().getString("status"), equalTo("DECLINED"));
        assertThat(authorization.jsonPath().getString("responseCode"), equalTo("05"));
        assertThat(authorization.jsonPath().getString("declineReason"), equalTo("CARD_BLOCKED"));

        // THEN: each supplied filter applies to the same record (AND semantics in TransactionSpecification).
        await().atMost(15, TimeUnit.SECONDS).pollInterval(300, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Response search = gateway().queryParam("pan", card.pan())
                    .queryParam("status", "DECLINED")
                    .queryParam("dateFrom", LocalDate.ofInstant(transmittedAt, ZoneOffset.UTC).toString())
                    .queryParam("dateTo", LocalDate.ofInstant(transmittedAt, ZoneOffset.UTC).toString())
                    .queryParam("merchantId", "MERCH0000000005")
                    .queryParam("issuerId", "ISS001")
                    .queryParam("mcc", "5411")
                    .queryParam("declineReason", "CARD_BLOCKED")
                    .queryParam("limit", 10).queryParam("offset", 0)
                    .when().get("/api/transactions/search");
            assertThat(search.statusCode(), equalTo(200));
            List<Map<String, Object>> selected = search.jsonPath().getList(
                    "transactions.findAll { it.stan == '" + stan + "' }");
            assertThat(selected, hasSize(1));
            assertThat(selected.get(0).get("declineReason"), equalTo("CARD_BLOCKED"));
        });
    }

    @Test
    void searchRejectsInvertedDateRange() {
        // GIVEN: dateFrom is later than dateTo.

        // WHEN
        Response response = gateway().queryParam("dateFrom", "2026-11-27")
                .queryParam("dateTo", "2026-11-26")
                .when().get("/api/transactions/search");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void searchRejectsNegativeOffset() {
        // GIVEN / WHEN
        Response response = gateway().queryParam("offset", -1).when().get("/api/transactions/search");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void searchReturnsAnEmptyPageForAWellFormedUnknownPan() {
        // GIVEN: a syntactically valid PAN that has no transactions.
        String pan = unknownPan();

        // WHEN
        Response response = gateway().queryParam("pan", pan).queryParam("limit", 10)
                .queryParam("offset", 0).when().get("/api/transactions/search");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getLong("total"), equalTo(0L));
        assertThat(response.jsonPath().getList("transactions"), empty());
    }

    @Test
    void transactionExportRejectsInvalidPaginationFilter() {
        // GIVEN

        // WHEN
        // Request JSON for the validation error; text/csv is only appropriate for successful exports.
        Response response = gateway().queryParam("limit", 0).accept("application/json")
                .when().get("/api/transactions/export");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void transactionExportReturnsCsvWithContractHeaders() {
        // GIVEN: export is a read-only operation and also returns the header for an empty result.

        // WHEN
        Response response = gateway().accept("text/csv")
                .when().get("/api/transactions/export");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.header("Content-Type"), startsWith("text/csv"));
        assertThat(response.header("Content-Disposition"), containsString("transactions.csv"));
        assertTrue(response.asString().startsWith("id,mti,stan,rrn,pan"),
                "CSV must begin with the documented column header");
    }
}
