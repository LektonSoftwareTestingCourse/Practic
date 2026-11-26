package com.processing.apitests.simulator;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Simulator paths and responses: TerminalSimulatorController.java and merchant-acquirer/controller/SimulationController.java.
class SimulatorApiTest extends ApiTestSupport {

    @Test
    void terminalSimulatorRunsOneTransactionAndReturnsCounts() {
        // GIVEN: at least one eligible card exists; the simulator itself obtains card data via Gateway.
        createCard();
        Map<String, Object> body = Map.of("count", 1, "scenario", "normal", "tps", 1);

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/terminal/run");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getInt("totalSubmitted"), equalTo(1));
        assertThat(response.jsonPath().getInt("approved") + response.jsonPath().getInt("declined"),
                lessThanOrEqualTo(1));
        assertThat(response.jsonPath().getList("transactions"), hasSize(1));
        assertThat(response.jsonPath().getLong("elapsedMs"), greaterThanOrEqualTo(0L));
    }

    @Test
    void terminalSimulatorRejectsZeroCount() {
        // GIVEN
        Map<String, Object> body = Map.of("count", 0, "scenario", "normal", "tps", 1);

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/terminal/run");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void terminalContinuousSimulationCanBeStartedAndStopped() {
        // GIVEN: one active card is available for the simulator.
        createCard();
        Map<String, Object> body = Map.of("tps", 1, "transactionType", "NORMAL");

        // WHEN
        Response started = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/terminal/start-continuous");
        Response stopped = gateway().when().post("/api/simulator/terminal/stop");

        // THEN: actual controller paths are /start-continuous and /stop (not /start and /status).
        assertThat(started.statusCode(), equalTo(200));
        assertThat(stopped.statusCode(), equalTo(200));
    }

    @Test
    void terminalContinuousStartRejectsZeroTps() {
        // GIVEN
        Map<String, Object> body = Map.of("tps", 0, "transactionType", "NORMAL");

        // WHEN
        Response response = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/terminal/start-continuous");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void merchantSimulatorRunsAndFeeCanBeLookedUpForItsTransaction() {
        // GIVEN
        createCard();
        Map<String, Object> body = Map.of(
                "count", 1,
                "scenario", "grocery",
                "mccCodes", List.of("5411"),
                "tps", 1
        );

        // WHEN
        Response simulation = gateway().contentType(ContentType.JSON).body(body)
                .when().post("/api/simulator/merchant/run");

        // THEN: the simulator response describes one submitted transaction.
        assertThat(simulation.statusCode(), equalTo(200));
        assertThat(simulation.jsonPath().getInt("totalSubmitted"), equalTo(1));
        assertThat(simulation.jsonPath().getList("transactions"), hasSize(1));
        String stan = simulation.jsonPath().getString("transactions[0].stan");
        String rrn = simulation.jsonPath().getString("transactions[0].rrn");
        assertThat(stan, matchesPattern("\\d{6}"));
        assertThat(simulation.jsonPath().getString("transactions[0].status"), equalTo("APPROVED"));
        assertThat(rrn, matchesPattern("\\d{12}"));

        // STAN is terminal-scoped and can repeat across terminals/restarts. RRN is the
        // unique transaction reference returned by Authorization, so use both to isolate this run.
        // The Logger write is asynchronous; Awaitility waits for that exact row to appear.
        await().atMost(15, TimeUnit.SECONDS).pollInterval(300, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            Response search = gateway().queryParam("limit", 500).queryParam("offset", 0)
                    .when().get("/api/transactions/search");
            assertThat(search.statusCode(), equalTo(200));
            List<Map<String, Object>> matches = search.jsonPath().getList(
                    "transactions.findAll { it.stan == '" + stan + "' && it.rrn == '" + rrn + "' }");
            assertThat(matches, hasSize(1));
            Map<String, Object> transaction = matches.get(0);
            Map<String, Object> feeRequest = Map.of(
                    "transmissionDateTime", transaction.get("transmissionDateTime"),
                    "pan", transaction.get("pan"),
                    "stan", transaction.get("stan"),
                    "amount", new BigDecimal(transaction.get("amount").toString()),
                    "terminalId", transaction.get("terminalId")
            );
            Response fee = gateway().contentType(ContentType.JSON).body(feeRequest)
                    .when().post("/api/simulator/merchant/fee");
            assertThat(fee.statusCode(), equalTo(200));
            assertThat(new BigDecimal(fee.jsonPath().getString("acquirerFee")),
                    greaterThanOrEqualTo(BigDecimal.ZERO));
        });
    }

    @Test
    void merchantListIsAccessibleThroughGateway() {
        // GIVEN

        // WHEN
        Response response = gateway().when().get("/api/simulator/merchants");

        // THEN
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getList("$"), not(empty()));
        assertThat(response.jsonPath().getString("[0].id"), not(blankOrNullString()));
        assertThat(response.jsonPath().getString("[0].mcc"), matchesPattern("\\d{4}"));
    }
}
