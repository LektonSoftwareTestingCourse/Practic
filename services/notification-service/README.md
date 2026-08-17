# Notification Service

Сервис-потребитель событий карт из RabbitMQ.

## Назначение

- Подписан на очередь `card-notifications` (exchange `smp.card-events`, routing key `card.*`).
- Получает события карт от Card Management через механизм Outbox → RabbitMQ.
- Сохраняет полученные события в таблицу `card_notifications` для тестовой проверки.

## API

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/notifications` | Список полученных событий (с пагинацией) |
| `GET` | `/actuator/health` | Health-check |

## Таблица БД

`card_notifications`:

| Колонка | Тип | Описание |
|---------|-----|----------|
| `id` | UUID | Первичный ключ |
| `event_type` | VARCHAR(100) | Тип события (напр. `CardServiceCreationEvent`) |
| `routing_key` | VARCHAR(100) | Routing key (напр. `card.CardServiceCreationEvent`) |
| `payload` | JSONB | Сериализованное событие |
| `received_at` | TIMESTAMPTZ | Время получения |

## Изоляция Flyway

Используется отдельная таблица истории миграций `flyway_schema_history_notification`, чтобы не конфликтовать с миграциями card-management (V5.x) и других сервисов.
