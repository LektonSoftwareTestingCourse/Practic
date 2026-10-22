# A3. C4 Level 3 - Component: Authorization и Card Management

## Назначение

Показать внутреннее устройство двух ключевых сервисов

## Диаграмма

```mermaid
flowchart LR
    subgraph AUTH["Authorization Service"]
        AC["AuthControllerImpl<br/>POST /api/internal/authorize<br/>POST /api/internal/rollback"]
        AS["AuthServiceImpl<br/>authorize(), rollback()"]
        DO["DeclineOutcome<br/>reason + response-код"]
        CMC["CardManagementClientImpl<br/>RestClient"]
        BLC["BinLookupClient<br/>RestClient"]
        LUR["LimitUsageRepository<br/>upsert limit_usage"]
        EV["AuthorizationEventNotifier"]
        AC --> AS
        AS --> DO
        AS --> CMC
        AS --> BLC
        AS --> LUR
        AS --> EV
    end

    subgraph CMS["Card Management Service"]
        CC["CardController<br/>CRUD, reserve, rollback, bulk"]
        CGC["CardGeneratorController<br/>POST /api/cards/generate"]
        CS["CardServiceImpl<br/>бизнес-логика карты"]
        CGS["CardGeneratorService<br/>генерация пачки карт"]
        BIS["BinIssuerService<br/>BIN → issuerId"]
        PG["PanGenerator / LuhnValidator"]
        TR["TransactionRunnerImpl<br/>NESTED-транзакции"]
        CR["CardRepository<br/>CardRepositoryPersistenceAdapter"]
        RR["ReservationRepository<br/>ReservationRollbackRepository"]
        CEN["CardEventNotifierImpl"]
        OEP["OutboxEventProcessorImpl<br/>convertAndSend"]
        OP["OutboxProcessor<br/>@Scheduled 1000 ms"]
        OR["OutboxRepository"]
        CRE["Card Reservation<br/>доменные правила"]

        CC --> CS
        CGC --> CGS
        CGS --> CS
        CS --> TR
        CS --> BIS
        CS --> PG
        CS --> CR
        CS --> RR
        CS --> CEN
        CS --> CRE
        CEN --> OEP
        OEP --> OR
        OP --> OR
        OP --> OEP
    end

    CMC --> |"HTTP: GET card<br/>POST reserve / rollback"| CM["Card Management"]
    BLC --> |"HTTP: GET /api/bin/{bin}"| BIN["Bin Lookup Service"]
    LUR --> |"JDBC<br/>limit_usage<br/>rrn_seq"| PGDB[("PostgreSQL")]
    CR --> |"JDBC"|PGDB
    RR --> |"JDBC"|PGDB
    OR --> |"JDBC"|PGDB
    OEP -.->|"AMQP<br/>card.*"| RMQ[("RabbitMQ")]
```

## Чего не хватало в проекте

Компонентного уровня в документации не было вообще: были только названия сервисов и общая схема.

## Что сделано

- Нарисованы два компонентных графа: Authorization и Card Management.
- Отмечены типы вызовов
## Верификация по коду

| Утверждение | Источник |
|---|---|
| Authorization | [`AuthControllerImpl.java`](../../../services/authorization/src/main/java/com/processing/authorization/controller/AuthControllerImpl.java), [`AuthServiceImpl.java`](../../../services/authorization/src/main/java/com/processing/authorization/services/AuthServiceImpl.java) |
| HTTP-клиент CMS | [`CardManagementClientImpl.java`](../../../services/authorization/src/main/java/com/processing/authorization/client/CardManagementClientImpl.java) |
| BIN-клиент и таймауты | [`BinLookupClient.java`](../../../services/authorization/src/main/java/com/processing/authorization/client/BinLookupClient.java) |
| UPSERT лимитов | [`LimitUsageRepository.java`](../../../services/authorization/src/main/java/com/processing/authorization/repositories/LimitUsageRepository.java) |
| Таблица отказов | [`DeclineOutcome.java`](../../../services/authorization/src/main/java/com/processing/authorization/constants/DeclineOutcome.java) |
| CRUD и reserve/rollback | [`CardController.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/controllers/CardController.java), [`CardServiceImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/CardServiceImpl.java) |
| Генерация PAN | [`CardGeneratorService.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/CardGeneratorService.java), [`LuhnValidator.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/LuhnValidator.java) |
| Outbox | [`CardEventNotifierImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/events/CardEventNotifierImpl.java), [`OutboxEventProcessorImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/OutboxEventProcessorImpl.java), [`OutboxProcessor.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/OutboxProcessor.java) |
| Транзакции CMS | [`TransactionRunnerImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/TransactionRunnerImpl.java) |

