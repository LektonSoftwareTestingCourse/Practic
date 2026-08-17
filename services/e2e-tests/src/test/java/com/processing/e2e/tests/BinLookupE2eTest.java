package com.processing.e2e.tests;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.apache.http.params.CoreConnectionPNames;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC-21: Bin Lookup Service e2e tests.
 *
 * Tests the bin-lookup external API:
 * - Normal response (200, issuerId)
 * - Simulated failure (500)
 * - Timeout scenario (client-side read timeout)
 * - Unknown BIN (404)
 *
 * Prerequisites: bin-lookup service running on localhost:8096.
 */
public class BinLookupE2eTest {

    private static final String BIN_LOOKUP_BASE = "http://localhost:8096";

    @AfterMethod
    public void resetRestAssuredConfig() {
        RestAssured.config = RestAssuredConfig.config();
    }

    // --- TC-21.1: Normal response ---

    @Test(description = "TC-21.1: Normal bin-lookup response returns issuerId")
    public void testBinLookupNormalResponse() {
        // BUILD — BIN 400000 is pre-seeded in bin-lookup controller

        // OPERATE
        Response response = RestAssured
                .given()
                .baseUri(BIN_LOOKUP_BASE)
                .when()
                .get("/api/bin/400000");

        // CHECK
        assertEquals(response.getStatusCode(), 200, "Expected HTTP 200 for known BIN");
        String issuerId = response.jsonPath().getString("issuerId");
        assertEquals(issuerId, "ISS001", "Expected issuerId ISS001 for BIN 400000");
        assertNotNull(response.jsonPath().getString("issuerName"),
                "issuerName should not be null");
    }

    // --- TC-21.2: Simulated failure (fail=true) ---

    @Test(description = "TC-21.2: Simulated failure returns 500")
    public void testBinLookupSimulatedFailure() {
        // BUILD — controller supports ?fail=true to simulate 500

        // OPERATE
        Response response = RestAssured
                .given()
                .baseUri(BIN_LOOKUP_BASE)
                .queryParam("fail", "true")
                .when()
                .get("/api/bin/400000");

        // CHECK
        assertEquals(response.getStatusCode(), 500, "Expected HTTP 500 for simulated failure");
    }

    // --- TC-21.3: Timeout (delay > client read timeout) ---

    @Test(description = "TC-21.3: Timeout when delay exceeds client read timeout")
    public void testBinLookupTimeout() {
        // BUILD — configure a short read timeout (3s) to trigger timeout
        // before the server-side delay (10s) completes
        int readTimeoutMs = 3000;
        int serverDelayMs = 10000;

        RestAssured.config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CoreConnectionPNames.SO_TIMEOUT, readTimeoutMs)
                        .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000));

        // OPERATE — request with ?delay=10000, expecting client-side timeout
        boolean timeoutOccurred = false;
        try {
            RestAssured
                    .given()
                    .baseUri(BIN_LOOKUP_BASE)
                    .queryParam("delay", serverDelayMs)
                    .when()
                    .get("/api/bin/400000");
        } catch (Exception e) {
            // CHECK — timeout-related exception: socket read timeout or connection timeout
            String exceptionName = e.getClass().getSimpleName();
            String exceptionMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            timeoutOccurred = exceptionName.contains("Timeout")
                    || exceptionMessage.contains("timeout")
                    || exceptionMessage.contains("read timed out")
                    || exceptionName.contains("SocketTimeout");
        }

        assertTrue(timeoutOccurred,
                "Expected a timeout exception when server delay (" + serverDelayMs
                        + "ms) exceeds client read timeout (" + readTimeoutMs + "ms)");
    }

    // --- TC-21.4: Unknown BIN (404) ---

    @Test(description = "TC-21.4: Unknown BIN returns 404")
    public void testBinLookupUnknownBin() {
        // BUILD — BIN 999999 is not in the lookup table

        // OPERATE
        Response response = RestAssured
                .given()
                .baseUri(BIN_LOOKUP_BASE)
                .when()
                .get("/api/bin/999999");

        // CHECK
        assertEquals(response.getStatusCode(), 404, "Expected HTTP 404 for unknown BIN");
    }
}
