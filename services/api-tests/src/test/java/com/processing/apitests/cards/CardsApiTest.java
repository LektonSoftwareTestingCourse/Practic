package com.processing.apitests.cards;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Contract: docs/api/openapi.yaml; implementation: card-management/controllers/CardController.java.
class CardsApiTest extends ApiTestSupport {

    @Test
    void createAndReadCardFollowTheCardContract() {
        // GIVEN
        CardFixture card = createCard();

        // WHEN
        Response response = gateway().when().get("/api/cards/{pan}", card.pan());

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        response.then().assertThat().body(matchesJsonSchemaInClasspath("schemas/card-response.schema.json"));
        assertThat(response.jsonPath().getString("pan"), equalTo(card.pan()));
        assertThat(response.jsonPath().getString("status"), equalTo("ACTIVE"));
        assertThat(response.jsonPath().getString("currencyCode"), equalTo("643"));
    }

    @ParameterizedTest(name = "card creation rejects invalid {0}")
    @MethodSource("invalidCardRequests")
    void createCardRejectsEachInvalidInputClass(String invalidField, Map<String, Object> body,
                                                 String expectedMessage) {
        // GIVEN: a valid payload with exactly one invalid field.

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("serviceName"), equalTo("card-management"));
        assertThat(response.jsonPath().getString("message"), containsString(expectedMessage));
    }

    static Stream<Arguments> invalidCardRequests() {
        return Stream.of(
                invalidCard("bin", "123", "BIN number must contain exactly 6 digits"),
                invalidCard("cardholderName", "lowercase name", "Cardholder name can contain only uppercase"),
                invalidCard("currencyCode", "64A", "Value can contain digits only"),
                invalidCard("dailyLimit", new BigDecimal("-1"), "Value can not be negative")
        );
    }

    private static Arguments invalidCard(String field, Object value, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bin", "400000");
        body.put("cardholderName", "API TEST USER");
        body.put("currencyCode", "643");
        body.put("dailyLimit", new BigDecimal("50000000"));
        body.put("monthlyLimit", new BigDecimal("300000000"));
        body.put("initialBalance", new BigDecimal("100000000"));
        body.put(field, value);
        return Arguments.of(field, body, message);
    }

    @ParameterizedTest(name = "invalid PAN path value: {0}")
    @ValueSource(strings = {"bad-pan", "123"})
    void getCardRejectsInvalidPanFormat(String pan) {
        // GIVEN: a malformed path parameter.

        // WHEN
        Response response = gateway().when().get("/api/cards/{pan}", pan);

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("serviceName"), equalTo("card-management"));
        assertThat(response.jsonPath().getString("message"), containsString("PAN"));
    }

    @Test
    void getCardReturns404ForUnknownButWellFormedPan() {
        // GIVEN
        String pan = unknownPan();

        // WHEN
        Response response = gateway().when().get("/api/cards/{pan}", pan);

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("serviceName"), equalTo("card-management"));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void listCardsAppliesFiltersAndPagination() {
        // GIVEN
        Instant startDate = Instant.now().minusSeconds(86_400);
        CardFixture card = createCard();
        createCard();
        Instant endDate = Instant.now().plusSeconds(86_400);

        // WHEN: narrow date bounds isolate cards created by this test from previous runs.
        Response response = gateway().queryParam("bin", "400000")
                .queryParam("status", "ACTIVE")
                .queryParam("issuerId", "ISS001")
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("limit", 1000)
                .queryParam("offset", 0)
                .when().get("/api/cards");

        // THEN: use a broad page because the service does not define a stable sort order;
        // the separate one-row requests below verify pagination. Every returned row must match filters.
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getInt("total"), greaterThanOrEqualTo(2));
        List<Map<String, Object>> ownCard = response.jsonPath().getList(
                "cards.findAll { it.pan == '" + card.pan() + "' }");
        assertThat(ownCard, hasSize(1));
        List<Map<String, Object>> page = response.jsonPath().getList("cards");
        assertThat(page, hasSize(lessThanOrEqualTo(1000)));
        for (Map<String, Object> item : page) {
            assertThat(item.get("bin"), equalTo("400000"));
            assertThat(item.get("status"), equalTo("ACTIVE"));
            assertThat(item.get("issuerId"), equalTo("ISS001"));
            Instant createdAt = Instant.parse(item.get("createdAt").toString());
            assertThat(createdAt, greaterThanOrEqualTo(startDate));
            assertThat(createdAt, lessThanOrEqualTo(endDate));
        }

        Response firstPage = gateway().queryParam("bin", "400000").queryParam("status", "ACTIVE")
                .queryParam("limit", 1).queryParam("offset", 0).when().get("/api/cards");
        Response secondPage = gateway().queryParam("bin", "400000").queryParam("status", "ACTIVE")
                .queryParam("limit", 1).queryParam("offset", 1).when().get("/api/cards");
        assertThat(firstPage.statusCode(), equalTo(200));
        assertThat(secondPage.statusCode(), equalTo(200));
        assertThat(firstPage.jsonPath().getList("cards"), hasSize(1));
        assertThat(secondPage.jsonPath().getList("cards"), hasSize(1));
        assertThat(firstPage.jsonPath().getString("cards[0].pan"),
                not(equalTo(secondPage.jsonPath().getString("cards[0].pan"))));
    }

    @Test
    void listCardsReturnsEmptyPageForUnknownButWellFormedIssuer() {
        // GIVEN: issuerId is syntactically valid but does not exist in the catalogue.

        // WHEN
        Response response = gateway().queryParam("issuerId", "NOISSUER")
                .queryParam("limit", 10).queryParam("offset", 0)
                .when().get("/api/cards");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getLong("total"), equalTo(0L));
        assertThat(response.jsonPath().getList("cards"), empty());
    }

    @ParameterizedTest(name = "card list rejects invalid query parameter {0}={1}")
    @MethodSource("invalidCardListParameters")
    void listCardsRejectsInvalidQueryParameter(String parameter, String value) {
        // GIVEN: one invalid query parameter; the other parameters keep their defaults.

        // WHEN
        Response response = gateway().queryParam(parameter, value).when().get("/api/cards");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    static Stream<Arguments> invalidCardListParameters() {
        return Stream.of(
                Arguments.of("limit", "0"),
                Arguments.of("offset", "-1"),
                Arguments.of("bin", "12"),
                Arguments.of("issuerId", "lowercase"),
                Arguments.of("status", "UNKNOWN"),
                Arguments.of("startDate", "not-a-date")
        );
    }

    @Test
    void patchUnknownCardReturns404() {
        // GIVEN
        String pan = unknownPan();

        // WHEN
        Response response = gateway().contentType(ContentType.JSON)
                .body(Map.of("status", "BLOCKED"))
                .when().patch("/api/cards/{pan}", pan);

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void patchCardChangesTheStatus() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> patch = Map.of("status", "BLOCKED");

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(patch)
                .when().patch("/api/cards/{pan}", card.pan());

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getString("status"), equalTo("BLOCKED"));
        Response fetched = gateway().when().get("/api/cards/{pan}", card.pan());
        assertThat(fetched.statusCode(), equalTo(200));
        assertThat(fetched.jsonPath().getString("status"), equalTo("BLOCKED"));
    }

    @Test
    void patchCardRejectsNegativeLimitWithFieldError() {
        // GIVEN
        CardFixture card = createCard();

        // WHEN
        Response response = gateway().contentType(ContentType.JSON)
                .body(Map.of("dailyLimit", new BigDecimal("-1")))
                .when().patch("/api/cards/{pan}", card.pan());

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("serviceName"), equalTo("card-management"));
        assertThat(response.jsonPath().getString("message"), containsString("dailyLimit"));
    }

    @Test
    void deleteCardReturns404ForUnknownPan() {
        // GIVEN
        String pan = unknownPan();

        // WHEN
        Response response = gateway().when().delete("/api/cards/{pan}", pan);

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void deleteCardRejectsMalformedPan() {
        // GIVEN

        // WHEN
        Response response = gateway().when().delete("/api/cards/{pan}", "not-a-pan");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), containsString("PAN"));
    }

    @Test
    void bulkUpdateChangesOnlyCardsNamedInRequest() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> body = Map.of("pans", List.of(card.pan()), "status", "BLOCKED");

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/bulk-update");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getInt("updated"), equalTo(1));
        Response fetched = gateway().when().get("/api/cards/{pan}", card.pan());
        assertThat(fetched.statusCode(), equalTo(200));
        assertThat(fetched.jsonPath().getString("status"), equalTo("BLOCKED"));
    }

    @Test
    void deleteCardReturns204AndRemovesItFromActiveLookup() {
        // GIVEN
        CardFixture card = createCard();

        // WHEN
        Response deleted = gateway().when().delete("/api/cards/{pan}", card.pan());

        // THEN
        assertThat(deleted.statusCode(), equalTo(204));
        Response activeCards = gateway().queryParam("bin", "400000")
                .queryParam("status", "ACTIVE").queryParam("limit", 500)
                .when().get("/api/cards");
        List<Map<String, Object>> stillActive = activeCards.jsonPath().getList(
                "cards.findAll { it.pan == '" + card.pan() + "' }");
        assertThat(stillActive, empty());
    }

    @Test
    void reserveAndRollbackRestoreTheOriginalBalance() {
        // GIVEN
        CardFixture card = createCard();
        BigDecimal before = new BigDecimal(card.createResponse().jsonPath().getString("availableBalance"));
        String rrn = nextRrn();
        Map<String, Object> reserveBody = Map.of("amount", new BigDecimal("1000"), "rrn", rrn);

        // WHEN: reserve funds, then send a separate rollback request through the API.
        Response reserved = gateway().contentType(ContentType.JSON).body(reserveBody)
                .when().post("/api/cards/{pan}/reserve", card.pan());
        assertThat(reserved.statusCode(), equalTo(200));
        Response rolledBack = gateway().contentType(ContentType.JSON)
                .body(Map.of("rrn", rrn, "pan", card.pan(), "amount", new BigDecimal("1000")))
                .when().post("/api/cards/{pan}/rollback", card.pan());

        // THEN
        assertThat(rolledBack.statusCode(), equalTo(200));
        BigDecimal after = new BigDecimal(rolledBack.jsonPath().getString("availableBalance"));
        assertEquals(0, before.compareTo(after), "Rollback must restore the card balance");
    }

    @Test
    void rollbackWithoutReservationReturns404() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> body = Map.of("rrn", nextRrn(), "pan", card.pan(),
                "amount", new BigDecimal("100"));

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/{pan}/rollback", card.pan());

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void repeatedRollbackReturns409() {
        // GIVEN
        CardFixture card = createCard();
        String rrn = nextRrn();
        BigDecimal amount = new BigDecimal("100");
        Response reserved = gateway().contentType(ContentType.JSON)
                .body(Map.of("amount", amount, "rrn", rrn))
                .when().post("/api/cards/{pan}/reserve", card.pan());
        assertThat(reserved.statusCode(), equalTo(200));
        Map<String, Object> rollback = Map.of("rrn", rrn, "pan", card.pan(), "amount", amount);

        // WHEN
        Response first = gateway().contentType(ContentType.JSON).body(rollback)
                .when().post("/api/cards/{pan}/rollback", card.pan());
        Response second = gateway().contentType(ContentType.JSON).body(rollback)
                .when().post("/api/cards/{pan}/rollback", card.pan());

        // THEN
        assertThat(first.statusCode(), equalTo(200));
        assertThat(second.statusCode(), equalTo(409));
    }

    @Test
    void reservationRejectsInsufficientFundsWith402() {
        // GIVEN
        CardFixture card = createCard(new BigDecimal("100"), new BigDecimal("500000"),
                new BigDecimal("3000000"));

        // WHEN
        Response response = gateway().contentType(ContentType.JSON)
                .body(Map.of("amount", new BigDecimal("200"), "rrn", nextRrn()))
                .when().post("/api/cards/{pan}/reserve", card.pan());

        // THEN
        assertThat(response.statusCode(), equalTo(402));
        assertThat(response.jsonPath().getString("serviceName"), equalTo("card-management"));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void duplicateReservationReturns409() {
        // GIVEN
        CardFixture card = createCard();
        String rrn = nextRrn();
        Map<String, Object> body = Map.of("amount", new BigDecimal("100"), "rrn", rrn);

        // WHEN
        Response first = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/{pan}/reserve", card.pan());
        Response duplicate = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/{pan}/reserve", card.pan());

        // THEN
        assertThat(first.statusCode(), equalTo(200));
        assertThat(duplicate.statusCode(), equalTo(409));
        assertThat(duplicate.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void reserveRejectsInvalidAmountAndUnknownCardSeparately() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> invalidAmount = Map.of("amount", BigDecimal.ZERO, "rrn", nextRrn());
        Map<String, Object> validRequest = Map.of("amount", new BigDecimal("100"), "rrn", nextRrn());

        // WHEN
        Response invalid = gateway().contentType(ContentType.JSON).body(invalidAmount)
                .when().post("/api/cards/{pan}/reserve", card.pan());
        Response missing = gateway().contentType(ContentType.JSON).body(validRequest)
                .when().post("/api/cards/{pan}/reserve", unknownPan());

        // THEN
        assertThat(invalid.statusCode(), equalTo(400));
        assertThat(invalid.jsonPath().getString("message"), not(blankOrNullString()));
        assertThat(missing.statusCode(), equalTo(404));
        assertThat(missing.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void rollbackRejectsNonPositiveAmount() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> body = Map.of("rrn", nextRrn(), "pan", card.pan(), "amount", BigDecimal.ZERO);

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/{pan}/rollback", card.pan());

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void generateCardsReturnsRequestedCount() {
        // GIVEN
        Map<String, Object> body = Map.of("count", 2, "bins", List.of("400000"));

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/generate");

        // THEN
        assertThat(response.statusCode(), equalTo(201));
        assertThat(response.jsonPath().getInt("generated"), equalTo(2));
        assertThat(response.jsonPath().getList("cards"), hasSize(2));
        assertNotNull(response.jsonPath().getString("cards[0].pan"));
    }

    @Test
    void generateCardsRejectsEmptyAndInvalidBins() {
        // GIVEN
        Map<String, Object> emptyBins = Map.of("count", 1, "bins", List.of());
        Map<String, Object> invalidBin = Map.of("count", 1, "bins", List.of("123"));

        // WHEN
        Response emptyResponse = gateway().contentType(ContentType.JSON).body(emptyBins)
                .when().post("/api/cards/generate");
        Response invalidResponse = gateway().contentType(ContentType.JSON).body(invalidBin)
                .when().post("/api/cards/generate");

        // THEN
        assertThat(emptyResponse.statusCode(), equalTo(400));
        assertThat(invalidResponse.statusCode(), equalTo(400));
    }

    @Test
    void generateCardsRejectsZeroCount() {
        // GIVEN
        Map<String, Object> body = Map.of("count", 0, "bins", List.of("400000"));

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/cards/generate");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

}
