package com.processing.apitests.dashboard;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Dashboard contract: transaction-logger/controller/DashboardController.java and ChartsFilter.java.
class DashboardApiTest extends ApiTestSupport {

    @Test
    void dashboardStatsAreInternallyConsistent() {
        // GIVEN: dashboard statistics are aggregated from the transaction logger.

        // WHEN
        Response response = gateway().when().get("/api/dashboard/stats");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        long total = response.jsonPath().getLong("totalTransactions");
        long approved = response.jsonPath().getLong("approvedCount");
        long declined = response.jsonPath().getLong("declinedCount");
        assertThat(approved + declined, equalTo(total));
        assertThat(response.jsonPath().getDouble("approvalRate"), allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(1.0)));
        assertThat(new java.math.BigDecimal(response.jsonPath().getString("totalAmount")),
                greaterThanOrEqualTo(java.math.BigDecimal.ZERO));
    }

    @Test
    void dashboardRecentHonoursRequestedLimit() {
        // GIVEN
        int limit = 5;

        // WHEN
        Response response = gateway().queryParam("limit", limit)
                .when().get("/api/dashboard/recent");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        List<Map<String, Object>> transactions = response.jsonPath().getList("$");
        assertThat(transactions, hasSize(lessThanOrEqualTo(limit)));
    }

    @Test
    void dashboardRecentRejectsLimitsOutsideOneToFiveHundred() {
        // GIVEN / WHEN
        Response zero = gateway().queryParam("limit", 0).when().get("/api/dashboard/recent");
        Response tooLarge = gateway().queryParam("limit", 501).when().get("/api/dashboard/recent");

        // THEN
        assertThat(zero.statusCode(), equalTo(400));
        assertThat(tooLarge.statusCode(), equalTo(400));
    }

    @Test
    void dashboardChartsRejectInvertedTimeRange() {
        // GIVEN / WHEN
        Response response = gateway().queryParam("granularity", "day")
                .queryParam("from", "2026-11-27T00:00:00Z")
                .queryParam("to", "2026-11-26T00:00:00Z")
                .when().get("/api/dashboard/charts");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void dashboardChartsAcceptDayGranularity() {
        // GIVEN

        // WHEN
        Response response = gateway().queryParam("granularity", "day")
                .when().get("/api/dashboard/charts");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getList("$"), notNullValue());
    }

    @Test
    void dashboardChartsRejectUnsupportedGranularity() {
        // GIVEN

        // WHEN
        Response response = gateway().queryParam("granularity", "month")
                .when().get("/api/dashboard/charts");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }
}
