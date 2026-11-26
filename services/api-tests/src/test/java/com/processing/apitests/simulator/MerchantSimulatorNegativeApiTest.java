package com.processing.apitests.simulator;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;

// Merchant simulator negative contract: SimulationController.java and SimulatorRequest/AcquirerFeeRequest validation.
class MerchantSimulatorNegativeApiTest extends ApiTestSupport {

    @Test
    void merchantSimulatorRejectsZeroCount() {
        // GIVEN
        Map<String, Object> body = Map.of("count", 0, "scenario", "grocery");

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/merchant/run");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void merchantSimulatorReturns404WhenNoMerchantMatchesMcc() {
        // GIVEN: an MCC value absent from the seeded merchant catalogue.
        Map<String, Object> body = Map.of("count", 1, "scenario", "grocery", "mccCodes", java.util.List.of("9999"),
                "tps", 1);

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/merchant/run");

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void merchantFeeLookupReturns404ForUnknownFeeKey() {
        // GIVEN
        Map<String, Object> body = Map.of(
                "transmissionDateTime", "2026-11-26T10:00:00Z",
                "pan", "4000001234567893",
                "stan", "123456",
                "amount", 100,
                "terminalId", "TERM0001"
        );

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/merchant/fee");

        // THEN
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void merchantFeeLookupRejectsMissingRequiredFields() {
        // GIVEN: only transmissionDateTime is missing.
        // Keep every other required field valid so this request tests only the missing timestamp.
        Map<String, Object> body = Map.of(
                "pan", unknownPan(),
                "stan", nextStan(),
                "terminalId", "TERM0001"
        );

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/merchant/fee");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }
}
