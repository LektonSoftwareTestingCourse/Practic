package com.processing.apitests.transactions;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

// Internal route contract: services/switch/src/main/java/com/processing/controller/RouteController.java.
class SwitchInternalApiTest extends ApiTestSupport {

    @Test
    void switchRouteEndpointReturnsAuthorizationResult() {
        // GIVEN
        CardFixture card = createCard();
        String stan = nextStan();

        // WHEN
        Response response = switchService().contentType(ContentType.JSON)
                .body(transactionBody(card.pan(), stan, new BigDecimal("100")))
                .when().post("/api/internal/route");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getString("stan"), equalTo(stan));
        assertThat(response.jsonPath().getString("status"), equalTo("APPROVED"));
        assertThat(response.jsonPath().getString("responseCode"), equalTo("00"));
        assertThat(response.jsonPath().getString("rrn"), matchesPattern("\\d{12}"));
    }
}
