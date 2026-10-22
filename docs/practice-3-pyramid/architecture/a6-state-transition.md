# A6. State-transition

## Card.status

```mermaid
stateDiagram-v2
    [*] --> ACTIVE

    ACTIVE --> INACTIVE : PATCH / bulk-update
    INACTIVE --> ACTIVE : PATCH / bulk-update
    ACTIVE --> BLOCKED : PATCH / bulk-update
    BLOCKED --> ACTIVE : PATCH / bulk-update
    ACTIVE --> EXPIRED : PATCH / bulk-update
    INACTIVE --> EXPIRED : PATCH / bulk-update
    BLOCKED --> EXPIRED : PATCH / bulk-update
    EXPIRED --> ACTIVE : PATCH / bulk-update

    ACTIVE --> DELETED : DELETE /api/cards/{pan}
    INACTIVE --> DELETED : DELETE /api/cards/{pan}
    BLOCKED --> DELETED : DELETE /api/cards/{pan}
    EXPIRED --> DELETED : DELETE /api/cards/{pan}

    DELETED --> [*]

    note right of DELETED
        @SQLRestriction
        скрывает карту из выборок.

    end note
```

## Жизненный цикл Transaction

```mermaid
stateDiagram-v2
    state "Проверка в Authorization" as check
    state "APPROVED: средства зарезервированы, лимиты учтены" as approved
    state "DECLINED: резерва нет" as declined
    state "status = APPROVED" as logA
    state "status = DECLINED" as logD
    state "ROLLED_BACK" as rolled
    state "lost" as lost

    [*] --> check: POST /api/transactions

    check --> approved: все проверки пройдены
    check --> declined: отказ (responseCode="...")

    approved --> logA: публикация удалась
    approved --> rolled: публикация не удалась
    rolled --> [*]: клиенту уходит DECLINED

    declined --> logD: публикация удалась
    declined --> lost: публикация не удалась
    lost --> [*]: клиенту уходит исходный DECLINED

    logA --> [*]
    logD --> [*]
```

## Чего не хватало в проекте

- Не было state-диаграмм.
- Не было переходов статусов карты.

## Что сделано

- Построена диаграмма состояний карты с переходами.
- Построена схема жизненного цикла транзакции

## Верификация по коду

| Утверждение                       | Класс/файл |
|-----------------------------------|---|
| `CardStatus`                      | [`CardStatus.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/models/CardStatus.java) |
| Переходы карты                    | [`Card.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/models/Card.java), [`CardServiceImpl.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/services/CardServiceImpl.java) |
| `@SQLRestriction` для DELETED     | [`CardEntity.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/models/CardEntity.java) |
| `ReservationStatus`               | [`ReservationStatus.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/models/ReservationStatus.java) |
| Переходы резерва                  | [`Reservation.java`](../../../services/card-management/src/main/java/com/processing/cardmanagement/models/Reservation.java) |
| `TransactionStatus`               | [`TransactionStatus.java`](../../../services/common/src/main/java/com/processing/common/dto/transactionlogger/TransactionStatus.java) |
| Сохранение статуса без обновления | [`TransactionService.java`](../../../services/transaction-logger/src/main/java/com/processing/transactionlogger/service/TransactionService.java) |
| Откат             | [`RouteService.java`](../../../services/switch/src/main/java/com/processing/service/RouteService.java) |
