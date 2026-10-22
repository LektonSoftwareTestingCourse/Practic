# A2. C4 Level 2 - Container

## Назначение

Показать фактический состав контейнеров и различить синхронные связи и асинхронные

## Диаграмма

```mermaid
flowchart TB
    TERM["Terminal Simulator<br/>:8085"]
    MERCH["Merchant + Acquirer Simulator<br/>:8084"]
    DASH["Dashboard<br/>:3000"]
    GW["API Gateway<br/>:8080"]
    SW["Switch<br/>:8082"]
    AUTH["Authorization<br/>:8083"]
    CMS["Card Management<br/>:8081"]
    BIN["Bin Lookup<br/>:8096"]
    LOG["Transaction Logger<br/>:8088"]
    NS["Notification Service<br/>:8097"]
    PG[("PostgreSQL<br/>:5432")]
    RMQ[("RabbitMQ<br/>:5672 / :15672")]

    TERM -->|"HTTP: POST /api/transactions"| GW
    TERM -->|"HTTP: GET /api/cards"| CMS
    
    MERCH -->|"HTTP: POST /api/transactions<br/>GET /api/cards"| GW
    DASH -->|"HTTP /api"| GW

    GW -->|"HTTP: POST /api/internal/route"| SW
    GW -->|"HTTP: /api/cards/**"| CMS
    GW -->|"HTTP: /api/transactions/**<br/>/api/dashboard/**"| LOG

    GW -->|"HTTP: /api/simulator/terminal/**"| TERM
    GW -->|"HTTP: /api/simulator/merchant/**"| MERCH

    SW -->|"HTTP: /api/internal/authorize<br/>/api/internal/rollback"| AUTH
    SW -->|"HTTP: /api/simulator/merchant/fee"| MERCH
    AUTH -->|"HTTP: GET card<br/>POST reserve / rollback"| CMS
    AUTH -->|"HTTP: GET /api/bin/{bin}"| BIN

    SW -.->|"AMQP: transaction.log"| RMQ
    CMS -.->|"AMQP: smp.card-events<br/>card.*"| RMQ
    RMQ -.->|"AMQP: consume<br/>transaction-log"| LOG
    RMQ -.->|"AMQP: consume<br/>card-notification"| NS

    DASH -->|"WebSocket: /ws/transactions"| LOG

    CMS -->|"JDBC: cards, reservations,<br/> outbox_event"| PG
    AUTH -->|"JDBC: limit_usage, rrn_seq"| PG
    LOG -->|"JDBC: transactions"| PG
    NS -->|"JDBC: card_notifications"| PG
    MERCH -->|"JDBC: merchants, terminals, acquirer_fee"| PG
```

## Порты сервисов

| Сервис | Порт |
|--------|:---:|
| Gateway | 8080 |
| Card Management | 8081 |
| Switch | 8082 |
| Authorization | 8083 |
| Terminal Simulator | 8085 |
| Merchant Simulator | 8084 |
| Transaction Logger | 8088 |
| Bin Lookup | 8096 |
| Notification Service | 8097 |
| Dashboard | 3000 |
| PostgreSQL | 5432 |
| RabbitMQ (AMQP) | 5672 |
| RabbitMQ (Management UI) | 15672 |

## Чего не хватало в проекте

В `docs/architecture.md` есть схема контейнеров, но:
1. связи не размечены на синхронные (HTTP) и асинхронные (AMQP);
2. outbox-публикация не отражена;
3. отсутствуют связи, которые есть в коде: `switch -> merchant-acquirer` (запрос комиссии) и `terminal-simulator -> card-management`;

## Что сделано

- Собрана схема по `docker-compose.yaml`, `application.yml`/`application.properties`.
- Все связи разделены на HTTP, JDBC, AMQP (пунктир) и WebSocket.

## Верификация по коду

| Утверждение | Источник |
|---|---|
| Маршруты Gateway | [`application.yml` gateway](../../../services/gateway/src/main/resources/application.yml) |
| Состав и порты контейнеров | [`docker-compose.yaml`](../../../docker-compose.yaml) |
| AMQP-публикация Switch | [`LoggerClient.java`](../../../services/switch/src/main/java/com/processing/service/LoggerClient.java) |
| Outbox | [`OutboxEventProcessorImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/OutboxEventProcessorImpl.java) |
| AMQP-потребители | [`TransactionLogListener.java`](../../../services/transaction-logger/src/main/java/com/processing/transactionlogger/listener/TransactionLogListener.java), [`CardEventListener.java`](../../../services/notification-service/src/main/java/com/processing/notification/listener/CardEventListener.java) |
| WebSocket Dashboard | [`WebSocketConfig.java`](../../../services/transaction-logger/src/main/java/com/processing/transactionlogger/websocket/WebSocketConfig.java) |

