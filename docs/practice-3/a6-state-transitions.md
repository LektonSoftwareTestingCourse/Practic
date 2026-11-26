# А6. State transitions — Card и Transaction

## Диаграмма
```mermaid
stateDiagram-v2
  direction LR
  state "Card.status" as Card {
    [*] --> ACTIVE: создана активная карта
    ACTIVE --> INACTIVE: деактивация
    INACTIVE --> ACTIVE: активация
    ACTIVE --> BLOCKED: блокировка
    INACTIVE --> BLOCKED: блокировка
    BLOCKED --> ACTIVE: разблокировка (если разрешено)
    ACTIVE --> EXPIRED: истёк срок действия
    INACTIVE --> EXPIRED: истёк срок действия
    BLOCKED --> EXPIRED: истёк срок действия
    ACTIVE --> DELETED: удаление/soft-delete
    INACTIVE --> DELETED: удаление/soft-delete
    BLOCKED --> DELETED: удаление/soft-delete
  }
```

```mermaid
stateDiagram-v2
    state "Обработка транзакции" as Transaction {
[*] --> Authorization
state Authorization <<choice>>
Authorization --> APPROVED: Авторизация успешна
Authorization --> DECLINED: Авторизация отклонена
APPROVED --> Rollback: Ошибка публикации
state Rollback {
[*] --> Reversal
Reversal: Откат резерва средств
}
Rollback --> DECLINED: Системный отказ, код 96
}
```

## Чего не хватало и что добавлено
- **Не хватало:** диаграммы состояний карты и жизненного цикла транзакции.
- **Добавлено:** состояния карты, статусы транзакции и отдельно статусы Outbox.


## Подтверждение в коде
- `services/card-management/src/main/java/com/processing/cardmanagement/models/CardStatus.java` — enum статусов карты.
- `services/common/src/main/java/com/processing/common/dto/cardmanagement/CardModelStatus.java` — статусы в API DTO.
- `services/card-management/src/main/java/com/processing/cardmanagement/services/CardServiceImpl.java` — операции над картами.
- `services/common/src/main/java/com/processing/common/dto/transactionlogger/TransactionStatus.java` — статусы транзакции.
- `services/transaction-logger/src/main/resources/db/migration/V1__create_transactions_table.sql` — ограничение БД `APPROVED`, `DECLINED`.
- `services/card-management/src/main/java/com/processing/cardmanagement/models/OutboxEventDataStatus.java` и `CardOutboxEventData.java` — состояния outbox.
