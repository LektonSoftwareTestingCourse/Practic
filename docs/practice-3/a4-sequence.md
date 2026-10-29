# А4. Sequence — happy path, declined, reversal (MTI 0400)

## Диаграмма
```mermaid

sequenceDiagram
    autonumber
    participant T as Terminal/Merchant
    participant G as Gateway
    participant S as Switch
    participant B as BIN Lookup
    participant A as Authorization
    participant C as Card Management
    participant R as RabbitMQ
    participant L as Transaction Logger

    Note over T,L: Happy path — sync authorization, async logging
    T->>G: HTTP authorization request
    G->>S: HTTP request
    S->>B: HTTP lookup BIN
    B-->>S: issuerId
    S->>A: HTTP authorize
    A->>C: HTTP get/reserve card
    C-->>A: card/reservation result
    A-->>S: APPROVED + RRN/authCode
    S->>R: AMQP publish smp.transactions / transaction.log
    R-->>S: publisher confirm (broker ack)
    S-->>G: APPROVED response
    G-->>T: HTTP response
    R->>L: deliver transaction-log
    L->>L: persist transaction

    Note over T,L: Declined — business decline
    T->>G: HTTP authorization request
    G->>S: HTTP request
    S->>B: HTTP lookup BIN
    B-->>S: issuerId / unknown BIN
    S->>A: HTTP authorize (known BIN)
    A->>C: HTTP card/status/balance/limits
    C-->>A: decline condition or card data
    A-->>S: DECLINED (or Switch declines unknown BIN)
    S->>R: AMQP publish DECLINED transaction for log
    S-->>G: DECLINED response
    G-->>T: HTTP response
    R->>L: deliver transaction-log
    L->>L: persist DECLINED

    Note over T,L: Reversal MTI 0400 / rollback
    T->>G: HTTP rollback request (reversal / MTI 0400)
    G->>S: HTTP request
    S->>A: HTTP rollback
    A->>C: HTTP rollback reservation
    C-->>A: rollback result
    A-->>S: rollback APPROVED/DECLINED
    S->>R: AMQP publish reversal/log event when flow produces transaction record
    S-->>G: reversal response
    G-->>T: HTTP response

```

## Чего не хватало и что добавлено
- **Не хватало:** только happy path; не были сопоставлены отказ и reversal.
- **Добавлено:** три сценария, граница sync/async, publisher confirm, rollback при недоступности публикации.

## Подтверждение в коде
- `services/switch/src/main/java/com/processing/service/RouteService.java` — последовательность routing → authorize → logging и rollback при неуспешной публикации.
- `services/switch/src/main/java/com/processing/service/AuthorizationClient.java` — HTTP authorize/rollback (в коде rollback формируется отдельным `RollbackRequest`; литерал MTI 0400 не найден).
- `services/switch/src/main/java/com/processing/service/LoggerClient.java` — AMQP publish.
- `services/switch/src/main/resources/application.yml` — `publisher-confirm-type: correlated`.
- `services/authorization/src/main/java/com/processing/authorization/controller/AuthController.java` — authorize/rollback endpoints.
- `services/authorization/src/main/java/com/processing/authorization/services/AuthServiceImpl.java` — логика rollback.
- `services/common/src/main/java/com/processing/common/dto/transactionlogger/TransactionStatus.java` — только `APPROVED`, `DECLINED`.
- Проверка фактического reversal-контракта: `docs/api-spec.md` и `services/common/src/main/java/com/processing/common/dto/authorization/RollbackRequest.java`.
