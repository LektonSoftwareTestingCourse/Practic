package com.processing.apitests.logger;

import com.processing.apitests.support.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Logger contract: InternalTransactionController.java, TransactionService.java and TransactionRequest.java.
class TransactionLoggerInternalApiTest extends ApiTestSupport {

    @Test
    void repeatedIdenticalMessageIsIdempotentAndChangedPayloadConflicts() {
        // GIVEN: one stable message identity and the same payload for the first two deliveries.
        UUID id = UUID.randomUUID();
        String pan = createCard().pan();
        Map<String, Object> body = storedTransactionBody(id, pan, nextStan(), new BigDecimal("250"));

        // WHEN: first delivery creates the record; the second repeats it; the third reuses id with different data.
        Response created = loggerService().contentType(ContentType.JSON).body(body)
                .when().post("/api/internal/log");
        Response duplicate = loggerService().contentType(ContentType.JSON).body(body)
                .when().post("/api/internal/log");

        Map<String, Object> conflictingPayload = new java.util.LinkedHashMap<>(body);
        conflictingPayload.put("amount", new BigDecimal("251"));
        Response conflict = loggerService().contentType(ContentType.JSON).body(conflictingPayload)
                .when().post("/api/internal/log");

        // THEN
        assertThat(created.statusCode(), equalTo(201));
        assertThat(created.jsonPath().getString("id"), equalTo(id.toString()));
        assertThat(created.jsonPath().getString("status"), equalTo("stored"));
        assertThat(duplicate.statusCode(), equalTo(200));
        assertThat(duplicate.jsonPath().getString("id"), equalTo(id.toString()));
        assertThat(conflict.statusCode(), equalTo(409));
        assertThat(conflict.jsonPath().getString("message"), not(blankOrNullString()));
    }

    @Test
    void invalidLoggerMessageReturns400WithValidationDetails() {
        // GIVEN
        UUID id = UUID.randomUUID();
        String pan = createCard().pan();
        Map<String, Object> body = storedTransactionBody(id, pan, nextStan(), new BigDecimal("100"));
        body.put("stan", "ABC");

        // WHEN
        Response response = loggerService().contentType(ContentType.JSON).body(body)
                .when().post("/api/internal/log");

        // THEN
        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("message"), containsString("stan"));
    }
}
