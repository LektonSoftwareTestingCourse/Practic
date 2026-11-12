# А2. C4 Level 2 — Container

## Диаграмма
```mermaid
flowchart TB
  terminal[Terminal Simulator] -->|sync HTTP| gateway[Gateway]
  merchant[Merchant Acquirer] -->|sync HTTP| gateway
  dashboard[Dashboard] -->|HTTP API| gateway
  gateway -->|sync HTTP| switch[Switch]
  gateway -->|sync HTTP / admin| cards[Card Management]
  gateway -->|sync HTTP| logger[Transaction Logger]
  switch -->|sync HTTP| auth[Authorization]
  auth -->|sync HTTP| cards
  auth -->|sync HTTP| bin[BIN Lookup]
  switch -->|async AMQP: smp.transactions / transaction.log| rmq[(RabbitMQ)]
  rmq -->|async AMQP: transaction-log| logger
  cards -->|sync JDBC| pg[(PostgreSQL)]
  auth -->|sync JDBC| pg
  logger -->|sync JDBC| pg
  notify[Notification Service] -->|sync JDBC| pg
  cards -->|outbox publisher, async AMQP: smp.card-events| rmq
  rmq -->|async AMQP: card-notifications| notify
  merchant -->|sync HTTP| gateway
  subgraph obs[Observability]
    prometheus[Prometheus]
    grafana[Grafana]
    loki[Loki]
    promtail[Promtail]
  end
  prometheus -. scrape .-> gateway
  promtail -. logs .-> loki
  grafana -. dashboards .-> prometheus
  grafana -. dashboards .-> loki
```

## Чего не хватало и что добавлено
- **Не хватало:** пометок sync/async, очередей/exchange, outbox и publisher confirms, сверки с Compose.
- **Добавлено:** HTTP и JDBC показаны как синхронные взаимодействия; AMQP — как асинхронные. Отражены оба потока RabbitMQ: `smp.transactions` → `transaction-log` и `smp.card-events` → `card-notifications`.


## Подтверждение в коде
- `docker-compose.yaml` — контейнеры, environment URL, сети и зависимости.
- `services/switch/src/main/java/com/processing/service/RouteService.java` — синхронный маршрут и async-публикация.
- `services/switch/src/main/java/com/processing/service/LoggerClient.java` и `services/switch/src/main/java/com/processing/config/RabbitMQConfig.java` — exchange/queue и публикация.
- `services/transaction-logger/src/main/java/com/processing/transactionlogger/config/RabbitMQConfig.java`, `.../listener/TransactionLogListener.java` — consumer Logger.
- `services/card-management/src/main/java/com/processing/cardmanagement/config/RabbitMQConfig.java`, `.../services/OutboxEventProcessorImpl.java` — outbox publisher и события карт.
- `services/notification-service/src/main/java/com/processing/notification/config/RabbitMQConfig.java`, `.../listener/CardEventListener.java` — очередь уведомлений.
- `services/authorization/src/main/java/com/processing/authorization/client/` — HTTP-клиенты.
