package com.processing.apitests.authorization;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Business outcomes: AuthServiceImpl.java and constants/DeclineOutcome.java; HTTP codes: AuthControllerImpl.java.
class AuthorizationInternalApiTest extends ApiTestSupport {

    @Test
    void authorizationApprovesCardWithSufficientBalanceAndLimits() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> body = authorizationBody(card.pan(), nextStan(), new BigDecimal("100"));

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON).body(body)
                .when().post("/api/internal/authorize");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        response.then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator
                .matchesJsonSchemaInClasspath("schemas/authorization-response.schema.json"));
        assertThat(response.jsonPath().getString("status"), equalTo("APPROVED"));
        assertThat(response.jsonPath().getString("responseCode"), equalTo("00"));
        assertThat(response.jsonPath().getString("rrn"), matchesPattern("\\d{12}"));
        assertThat(response.jsonPath().getString("authCode"), matchesPattern("[A-Z0-9]{6}"));
    }

    @ParameterizedTest(name = "{0} card is declined with code {1} and reason {2}")
    @CsvSource({
            "BLOCKED,05,CARD_BLOCKED",
            "INACTIVE,05,CARD_INACTIVE",
            "EXPIRED,54,CARD_EXPIRED"
    })
    void cardStateProducesItsOwnBusinessDecline(String cardStatus, String responseCode, String declineReason) {
        // GIVEN
        CardFixture card = createCard();
        Response patched = gateway().contentType(ContentType.JSON)
                .body(Map.of("status", cardStatus))
                .when().patch("/api/cards/{pan}", card.pan());
        assertThat(patched.statusCode(), equalTo(200));

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON)
                .body(authorizationBody(card.pan(), nextStan(), new BigDecimal("100")))
                .when().post("/api/internal/authorize");

        // THEN: code and reason are checked together because multiple declines share a code.
        assertThat(response.statusCode(), equalTo(403));
        assertThat(response.jsonPath().getString("status"), equalTo("DECLINED"));
        assertThat(response.jsonPath().getString("responseCode"), equalTo(responseCode));
        assertThat(response.jsonPath().getString("declineReason"), equalTo(declineReason));
    }

    @Test
    void expiredByExpiryDateIsDistinctFromAnExpiredCardStatus() {
        // GIVEN: card status is ACTIVE, but the request is dated after the card's expiry month.
        CardFixture card = createCard();
        // A distant fixed timestamp is guaranteed to be after the default card validity.
        Instant afterExpiry = Instant.parse("2100-01-01T00:00:00Z");
        Map<String, Object> body = authorizationBody(card.pan(), nextStan(), new BigDecimal("100"));
        body.put("transmissionDateTime", afterExpiry.toString());

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON).body(body)
                .when().post("/api/internal/authorize");

        // THEN: the same business outcome (expired card) is reached through the request timestamp,
        // not by changing the card status to EXPIRED.
        assertThat(response.statusCode(), equalTo(403));
        assertThat(response.jsonPath().getString("responseCode"), equalTo("54"));
        assertThat(response.jsonPath().getString("declineReason"), equalTo("CARD_EXPIRED"));
    }

    @Test
    void insufficientFundsProduces422AndCode51() {
        // GIVEN
        CardFixture card = createCard(new BigDecimal("100"), new BigDecimal("500000"),
                new BigDecimal("3000000"));

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON)
                .body(authorizationBody(card.pan(), nextStan(), new BigDecimal("200")))
                .when().post("/api/internal/authorize");

        // THEN
        assertThat(response.statusCode(), equalTo(422));
        assertThat(response.jsonPath().getString("responseCode"), equalTo("51"));
        assertThat(response.jsonPath().getString("declineReason"), equalTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    void exceedingDailyLimitProducesCode61AndExplicitReason() {
        // GIVEN: balance is sufficient, but the configured daily limit is lower than the request.
        CardFixture card = createCard(new BigDecimal("100000"), new BigDecimal("100"),
                new BigDecimal("1000000"));

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON)
                .body(authorizationBody(card.pan(), nextStan(), new BigDecimal("101")))
                .when().post("/api/internal/authorize");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("responseCode"), equalTo("61"));
        assertThat(response.jsonPath().getString("declineReason"), equalTo("EXCEEDS_AMOUNT_LIMIT"));
    }

    @Test
    void rollbackOfUnknownRrnReturns404WithTransactionNotFoundReason() {
        // GIVEN
        CardFixture card = createCard();
        Map<String, Object> body = Map.of("rrn", nextRrn(), "pan", card.pan(), "amount", new BigDecimal("100"));

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON).body(body)
                .when().post("/api/internal/rollback");

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("status"), equalTo("DECLINED"));
        assertThat(response.jsonPath().getString("declineReason"), equalTo("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void rollbackRestoresBalanceAndRejectsASecondRollback() {
        // GIVEN
        CardFixture card = createCard();
        BigDecimal initialBalance = new BigDecimal(
                card.createResponse().jsonPath().getString("availableBalance"));
        BigDecimal amount = new BigDecimal("1000");
        Map<String, Object> request = authorizationBody(card.pan(), nextStan(), amount);

        // WHEN: approve (which reserves funds), then issue the rollback through Authorization API.
        Response approved = authorizationService().contentType(ContentType.JSON).body(request)
                .when().post("/api/internal/authorize");
        assertThat(approved.statusCode(), equalTo(200));
        String rrn = approved.jsonPath().getString("rrn");
        Map<String, Object> rollbackBody = Map.of("rrn", rrn, "pan", card.pan(), "amount", amount);
        Response rollback = authorizationService().contentType(ContentType.JSON).body(rollbackBody)
                .when().post("/api/internal/rollback");

        // THEN: rollback restores the original available balance and cannot be applied twice.
        assertThat(rollback.statusCode(), equalTo(200));
        assertThat(rollback.jsonPath().getString("status"), equalTo("APPROVED"));
        Response cardAfterRollback = gateway().when().get("/api/cards/{pan}", card.pan());
        BigDecimal restoredBalance = new BigDecimal(cardAfterRollback.jsonPath().getString("availableBalance"));
        assertThat(initialBalance.compareTo(restoredBalance), equalTo(0));

        Response duplicateRollback = authorizationService().contentType(ContentType.JSON).body(rollbackBody)
                .when().post("/api/internal/rollback");
        assertThat(duplicateRollback.statusCode(), equalTo(409));
        assertThat(duplicateRollback.jsonPath().getString("declineReason"), equalTo("ALREADY_ROLLED_BACK"));
    }

    @Test
    void authorizationReturns404AndCardNotFoundReasonForUnknownPan() {
        // GIVEN: PAN has the correct structure and a known BIN, but does not identify a stored card.
        String pan = unknownPan();

        // WHEN
        Response response = authorizationService().contentType(ContentType.JSON)
                .body(authorizationBody(pan, nextStan(), new BigDecimal("100")))
                .when().post("/api/internal/authorize");

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("responseCode"), equalTo("14"));
        assertThat(response.jsonPath().getString("declineReason"), equalTo("CARD_NOT_FOUND"));
    }
}
