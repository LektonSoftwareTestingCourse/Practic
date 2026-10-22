# A4. Sequence - путь транзакции

## Happy path

```mermaid
sequenceDiagram
    autonumber
    actor TERM as POS / Terminal Simulator
    participant GW as Gateway
    participant SW as Switch
    participant AUTH as Authorization
    participant CMS as Card Management
    participant BIN as Bin Lookup
    participant PG as PostgreSQL
    participant MERCH as Merchant Acquirer
    participant MQ as RabbitMQ
    participant LOG as Transaction Logger
    participant WS as Dashboard

    TERM->>GW: POST /api/transactions
    GW->>GW: TransactionValidationFilter<br/>проверка mti, pan, amount, mcc
    GW->>SW: POST /api/internal/route
    SW->>SW: RoutingService.getIssuerIdByPan(pan)
    SW->>AUTH: POST /api/internal/authorize
    AUTH->>CMS: GET /api/cards/{pan}
    CMS->>PG: SELECT cards
    PG-->>CMS:  card
    CMS-->>AUTH: card
    AUTH->>BIN: GET /api/bin/{bin}
    BIN-->>AUTH: issuerId
    AUTH->>AUTH: status = ACTIVE?<br/>expiryDate не истёк?<br/>amount <= availableBalance?
    AUTH->>PG: upsertLimitUsage
    PG-->>AUTH: 1 row (лимиты не превышены)
    AUTH->>PG: выделение RRN
    AUTH->>CMS: POST /api/cards/{pan}/reserve
    CMS->>PG: блокировка, reservation, balance, outbox
    PG-->>CMS: OK
    CMS-->>AUTH: 200
    AUTH-->>SW: 200 APPROVED / 00 / RRN / authCode
    SW->>MERCH: POST /api/simulator/merchant/fee
    MERCH-->>SW: acquiringFee или null
    SW->>MQ: convertAndSend(smp.transactions, transaction.log)
    Note over SW,MQ: AMQP-публикация без ожидания confirm
    SW-->>GW: 200 APPROVED
    GW-->>TERM: 200 APPROVED

    MQ-->>LOG: transaction-log
    LOG->>PG: insert transactions
    LOG->>WS: WebSocket broadcast
```

## Declined: карта BLOCKED

```mermaid
sequenceDiagram
    autonumber
    actor TERM as POS / Terminal Simulator
    participant GW as Gateway
    participant SW as Switch
    participant AUTH as Authorization
    participant CMS as Card Management
    participant BIN as Bin Lookup
    participant MA as Merchant Acquirer
    participant MQ as RabbitMQ
    participant LOG as Transaction Logger
    

    TERM->>GW: POST /api/transactions
    GW->>SW: POST /api/internal/route
    SW->>AUTH: POST /api/internal/authorize
    AUTH->>CMS: GET /api/cards/{pan}
    CMS-->>AUTH: 200 card (status=BLOCKED)
    AUTH->>BIN: GET /api/bin/{bin}
    BIN-->>AUTH: issuerId
    AUTH->>AUTH: status != ACTIVE
    Note over AUTH: reserve не вызывается,<br/>limit_usage не изменяется
    AUTH-->>SW: 403 DECLINED / 05 / CARD_BLOCKED
    SW->>MA: POST /api/simulator/merchant/fee
    MA-->>SW: acquirer fee
    SW->>MQ: transaction.log (DECLINED)
    SW-->>GW: 200 DECLINED
    GW-->>TERM: 200 DECLINED
    MQ-->>LOG: запись DECLINED-транзакции
```

## Reversal (сбой публикации в очередь после APPROVED)

```mermaid
sequenceDiagram
    participant GW as gateway
    participant SW as switch
    participant AU as authorization
    participant CM as card-management
    participant PG as PostgreSQL
    participant MQ as RabbitMQ

    GW->>SW: POST /api/internal/route
    SW->>AU: POST /api/internal/authorize
    AU->>CM: reserve выполнен (баланс уменьшен)
    CM->>PG: изменение баланса
    AU-->>SW: 200 APPROVED (rrn)
    SW-xMQ: convertAndSend (брокер недоступен)
    Note over SW: LoggerClient вернул false,<br/>а статус был APPROVED
    SW->>AU: POST /api/internal/rollback (rrn, pan, amount)
    AU->>CM: POST /api/cards/{pan}/rollback
    CM->>PG: reservation RESERVED -> ROLLED_BACK,<br/>баланс восстановлен
    CM-->>AU: OK
    AU-->>SW: RollbackResponse APPROVED
    SW-->>GW: DECLINED, 96
```

## Чего не хватало в проекте

В `docs/architecture.md` был только happy path. Не было видно:

- где именно синхронная цепочка обрывается при `declined`;
- как фактически устроен откат резерва при сбое публикации в RabbitMQ;
- в какой момент транзакция уходит в асинхронный канал и что при этом не ждётся подтверждение брокера.

## Что сделано

- Построены три sequence-диаграммы: happy path, declined и reversal.
- Выделена асинхронная часть

## Верификация по коду

| Утверждение | Класс/файл |
|---|---|
| Маршрут `/api/transactions` → `/api/internal/route` | [`application.yml` gateway](../../../services/gateway/src/main/resources/application.yml) |
| Оркестрация, BIN, публикация, rollback | [`RouteService.java`](../../../services/switch/src/main/java/com/processing/service/RouteService.java), [`AuthorizationClient.java`](../../../services/switch/src/main/java/com/processing/service/AuthorizationClient.java) |
| Проверки карты, лимитов, reserve, rollback | [`AuthServiceImpl.java`](../../../services/authorization/src/main/java/com/processing/authorization/services/AuthServiceImpl.java) |
| HTTP-клиент CMS | [`CardManagementClientImpl.java`](../../../services/authorization/src/main/java/com/processing/authorization/client/CardManagementClientImpl.java) |
| UPSERT `limit_usage` | [`LimitUsageRepository.java`](../../../services/authorization/src/main/java/com/processing/authorization/repositories/LimitUsageRepository.java) |
| Публикация в RabbitMQ | [`LoggerClient.java`](../../../services/switch/src/main/java/com/processing/service/LoggerClient.java) |
| Потребление Logger | [`TransactionLogListener.java`](../../../services/transaction-logger/src/main/java/com/processing/transactionlogger/listener/TransactionLogListener.java), [`TransactionService.java`](../../../services/transaction-logger/src/main/java/com/processing/transactionlogger/service/TransactionService.java) |
| Rollback в CMS | [`CardServiceImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/CardServiceImpl.java), [`Reservation.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/models/Reservation.java) |
