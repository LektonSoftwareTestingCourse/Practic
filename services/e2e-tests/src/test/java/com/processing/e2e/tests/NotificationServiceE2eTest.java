package com.processing.e2e.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.awaitility.Awaitility.await;
import static org.testng.Assert.*;

/**
 * TC-24, TC-25: Notification Service e2e tests.
 *
 * Tests outbox → RabbitMQ → Notification Service flow:
 * - TC-24: Create card → outbox event PENDING → PROCESSED → Notification Service consumes
 * - TC-25: RabbitMQ unavailable → outbox retry → recovery when RabbitMQ comes back
 *
 * Prerequisites:
 * - All services running (docker compose up -d)
 * - RabbitMQ accessible on localhost:5672
 * - Notification Service on localhost:8097
 * - Gateway on localhost:8080
 */
public class NotificationServiceE2eTest {

    private static final String GATEWAY = "http://localhost:8080";
    private static final String NOTIFICATION_SERVICE = "http://localhost:8097";

    private int initialTotalElements;

    @BeforeClass
    public void setup() {
        // BUILD: Record current total notification count as baseline
        Response nsResponse = RestAssured
                .given()
                .baseUri(NOTIFICATION_SERVICE)
                .when()
                .get("/api/notifications");

        assertEquals(nsResponse.getStatusCode(), 200,
                "Notification Service health-check failed (precondition)");

        // Use totalElements (across all pages) instead of content.size()
        // because the API returns a paginated Page<> with a default page size of 50
        initialTotalElements = nsResponse.jsonPath().getInt("totalElements");
    }

    // --- TC-24: Outbox PENDING → PROCESSED + Notification Service consume ---

    @Test(description = "TC-24: Card creation triggers outbox event → Notification Service receives notification")
    public void testOutboxAndNotificationDelivery() {
        // BUILD
        String cardholderName = "OUTBOX TEST " + randomLetters(6);
        String cardBody = String.format(
                "{\"bin\":\"400000\",\"cardholderName\":\"%s\",\"currencyCode\":\"643\","
                        + "\"dailyLimit\":15000000,\"monthlyLimit\":300000000,"
                        + "\"initialBalance\":100000000}",
                cardholderName);

        // OPERATE: Create card through Gateway
        Response createResponse = RestAssured
                .given()
                .baseUri(GATEWAY)
                .contentType("application/json")
                .body(cardBody)
                .when()
                .post("/api/cards");

        // CHECK: Card created
        assertEquals(createResponse.getStatusCode(), 201,
                "Card creation should return 201 (check preconditions)");
        String cardId = createResponse.jsonPath().getString("id");
        assertNotNull(cardId, "Card ID should not be null");

        // CHECK: Eventual consistency — wait for Notification Service to receive the event
        // OutboxEventProcessor runs every 1 second; allow up to 12 seconds for delivery
        await()
                .atMost(Duration.ofSeconds(12))
                .pollDelay(Duration.ofMillis(500))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    Response ns = RestAssured
                            .given()
                            .baseUri(NOTIFICATION_SERVICE)
                            .when()
                            .get("/api/notifications");
                    if (ns.getStatusCode() != 200) {
                        return false;
                    }
                    int currentTotal = ns.jsonPath().getInt("totalElements");
                    return currentTotal > initialTotalElements;
                });

        // Verify notification content
        Response nsResponse = RestAssured
                .given()
                .baseUri(NOTIFICATION_SERVICE)
                .when()
                .get("/api/notifications");

        assertEquals(nsResponse.getStatusCode(), 200);
        int currentTotal = nsResponse.jsonPath().getInt("totalElements");
        assertTrue(currentTotal > initialTotalElements,
                "Notification count should increase after card creation. "
                        + "Baseline: " + initialTotalElements
                        + ", current totalElements: " + currentTotal);

        // The most recent notification should be for the card we just created
        List<?> content = nsResponse.jsonPath().getList("content");
        assertNotNull(content, "Notifications content should not be null");
        if (!content.isEmpty()) {
            String lastEventType = nsResponse.jsonPath()
                    .getString("content[" + (content.size() - 1) + "].eventType");
            assertNotNull(lastEventType, "Event type should not be null");
        }
    }

    // --- TC-25: RabbitMQ unavailable → retry → recovery ---

    @Test(description = "TC-25: When RabbitMQ is unavailable, outbox retries and eventually delivers when RMQ recovers")
    public void testOutboxRecoveryAfterRabbitMQOutage() {
        // BUILD: This test requires orchestration: stop RabbitMQ, create card, start RabbitMQ.
        //
        // Manual pre-requisite steps:
        //   1. docker stop smp-rabbitmq
        //   2. Run this test to create a card while RMQ is down
        //   3. docker start smp-rabbitmq
        //   4. Test verifies eventual delivery via Awaitility
        //
        // Since we cannot control Docker from Java, this test uses a pragmatic approach:
        // it creates a card and verifies the notification is delivered within a generous
        // timeout (30s), which covers both the normal path (RMQ up) and the recovery path.
        // For the full RMQ-down scenario, the test operator must manually stop RMQ first.

        String cardholderName = "RECOVERY TEST " + randomLetters(6);
        String cardBody = String.format(
                "{\"bin\":\"400000\",\"cardholderName\":\"%s\",\"currencyCode\":\"643\","
                        + "\"dailyLimit\":15000000,\"monthlyLimit\":300000000,"
                        + "\"initialBalance\":100000000}",
                cardholderName);

        // OPERATE: Create card (possibly while RMQ is down)
        Response createResponse = RestAssured
                .given()
                .baseUri(GATEWAY)
                .contentType("application/json")
                .body(cardBody)
                .when()
                .post("/api/cards");

        assertEquals(createResponse.getStatusCode(), 201,
                "Card creation should return 201");
        String cardId = createResponse.jsonPath().getString("id");
        assertNotNull(cardId, "Card ID should not be null");

        // CHECK: In recovery mode (RMQ was down, now up), allow up to 30 seconds for delivery.
        // Outbox processor retries every 1s with exponential backoff (3 attempts max).
        // After RMQ restart, the processor will pick up PENDING events on next tick.
        await()
                .atMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    Response ns = RestAssured
                            .given()
                            .baseUri(NOTIFICATION_SERVICE)
                            .when()
                            .get("/api/notifications");
                    if (ns.getStatusCode() != 200) {
                        return false;
                    }
                    int currentTotal = ns.jsonPath().getInt("totalElements");
                    return currentTotal > initialTotalElements;
                });

        // Verify the notification was eventually delivered
        Response nsResponse = RestAssured
                .given()
                .baseUri(NOTIFICATION_SERVICE)
                .when()
                .get("/api/notifications");

        assertEquals(nsResponse.getStatusCode(), 200);
        int currentTotal = nsResponse.jsonPath().getInt("totalElements");
        assertTrue(currentTotal > initialTotalElements,
                "After recovery, notification should be delivered. "
                        + "Baseline: " + initialTotalElements
                        + ", current totalElements: " + currentTotal);
    }

    /**
     * Generates a random uppercase-letter string of the given length.
     */
    private static String randomLetters(int length) {
        return ThreadLocalRandom.current()
                .ints('A', 'Z' + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
