package com.processing.apitests.health;

import com.processing.apitests.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

// Contract: docs/api/openapi.yaml; implementation: gateway/health/HealthController.java.
class GatewayHealthApiTest extends ApiTestSupport {

    @Test
    void healthReturnsGatewayAndDownstreamStatuses() {
        // GIVEN: the full stack has been started by the practice-5 runner.

        // WHEN
        var response = gateway().when().get("/health");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.header("Content-Type"), startsWith("application/json"));
        response.then().assertThat().body(matchesJsonSchemaInClasspath("schemas/health-response.schema.json"));
        assertThat(response.jsonPath().getString("service"), equalTo("gateway"));
        assertThat(response.jsonPath().getString("status"), equalTo("ok"));
    }
}
