# A1. C4 Level 1 - Context

## Что было дополнено

В изначальной документации было непонятно где проведена сама СМП, а где внешние системамы. На диаграмме СМП показала единым чёрным ящиком, а симуляторы - как эмуляторы внешних систем. BIN Lookup развёрнут в составе Docker Compose, но для Authorization является внешним HTTP API.

```mermaid
flowchart LR
    terminal[POS-терминал<br/>эмулируется Terminal Simulator]
    merchant[Мерчант / эквайер<br/>эмулируется Merchant Acquirer]
    user[Пользователь Dashboard]
    bin[Внешний BIN Lookup]

    subgraph boundary[СМП — система микросервисного процессинга]
        smp[Приём, маршрутизация и авторизация транзакций;<br/>управление картами; журналирование и уведомления]
    end

    terminal -->|HTTPS/REST<br/>транзакции| smp
    merchant -->|HTTPS/REST<br/>транзакции| smp
    smp -->|HTTP<br/>запрос комиссии| merchant
    user -->|HTTP/REST| smp
    smp -->|WebSocket<br/>поток транзакций| user
    smp -->|"HTTP GET /api/bin/{bin}"| bin
```

## Верификация по коду

- Вход `/api/transactions` и маршрутизация через Gateway: [`services/gateway/src/main/resources/application.yml`](../../../services/gateway/src/main/resources/application.yml).
- Terminal Simulator вызывает Gateway: [`GatewayClient.java`](../../../services/terminal-simulator/src/main/java/com/processing/terminalsimulator/client/GatewayClient.java).
- Merchant Acquirer вызывает Gateway и предоставляет endpoint комиссии: [`SimulationController.java`](../../../services/merchant-acquirer/src/main/java/com/processing/merchantacquirer/controller/SimulationController.java).
- Dashboard использует REST и WebSocket: [`nginx.conf`](../../../services/dashboard/nginx.conf), [`useWebSocket.ts`](../../../services/dashboard/src/hooks/useWebSocket.ts).
- Authorization обращается к BIN Lookup: [`BinLookupClient.java`](../../../services/authorization/src/main/java/com/processing/authorization/client/BinLookupClient.java).
