# Transaction Logger

## Назначение

Transaction Logger сохраняет все транзакции процессинга, предоставляет API для поиска и фильтрации, агрегированную статистику для Dashboard и real-time поток через WebSocket.

---

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** PostgreSQL
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check сервиса |
| POST | `/api/internal/log` | Приём транзакции от Switch и сохранение в PostgreSQL |
| GET | `/api/transactions/search` | Поиск транзакций с фильтрацией и пагинацией |
| GET | `/api/transactions/export` | Экспорт транзакций по фильтру в CSV |
| GET | `/api/dashboard/stats` | Агрегированная статистика |
| GET | `/api/dashboard/recent` | Последние N транзакций |
| GET | `/api/dashboard/charts` | Агрегация по часам/дням для графиков |
| WS | `/ws/transactions` | Real-time поток транзакций |

Swagger UI: `http://localhost:8080/docs` при локальном запуске или `http://localhost:8088/docs`
при запуске через Docker Compose.

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "ok",
  "service": "transaction-logger",
  "transactionsStored": 0
}
```

---

#### `POST /api/internal/log`

Endpoint принимает транзакцию от Switch и сохраняет её в PostgreSQL.

Если запрос повторяется с уже существующим `id` и все поля совпадают с сохранённой транзакцией, новая запись не создаётся, а сервис возвращает существующую транзакцию.

**Тело запроса:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mti": "0100",
  "stan": "000001",
  "rrn": "012345678901",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 150000,
  "currencyCode": "643",
  "terminalId": "TERM001",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001",
  "issuerId": "ISS001",
  "acquiringFee": 2250,
  "status": "APPROVED",
  "declineReason": null,
  "authCode": "ABC123",
  "processingTimeMs": 42,
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "createdAt": "2026-06-01T10:30:01Z"
}
```

**Ответ 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "stored"
}
```

**Ответ 200:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mti": "0100",
  "stan": "000001",
  "rrn": "012345678901",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 150000,
  "currencyCode": "643",
  "terminalId": "TERM001",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001",
  "issuerId": "ISS001",
  "acquiringFee": 2250,
  "status": "APPROVED",
  "declineReason": null,
  "authCode": "ABC123",
  "processingTimeMs": 42,
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "createdAt": "2026-06-01T10:30:01Z"
}
```

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | Невалидные поля запроса или некорректное JSON-тело |
| 409 | Транзакция с таким `id` уже существует, но поля отличаются |
| 503 | Ошибка доступа к PostgreSQL |
| 500 | Внутренняя ошибка сервиса |

**Пример ошибки:**
```json
{
  "error": "Transaction conflict",
  "message": "Transaction with id 550e8400-e29b-41d4-a716-446655440000 already exists with different data",
  "timestamp": "2026-06-05T08:30:00Z",
  "serviceName": "transaction-logger",
  "retryAfterMs": "0"
}
```

---

#### `GET /api/transactions/search`

**Query-параметры:**

| Параметр | Тип | Описание                        | По умолчанию |
|----------|-----|---------------------------------|--------------|
| `pan` | string | Номер карты (точное совпадение) | — |
| `status` | string | `APPROVED` / `DECLINED`         | — |
| `dateFrom` | date | С даты                          | — |
| `dateTo` | date | По дату                         | — |
| `merchantId` | string | ID мерчанта                     | — |
| `issuerId` | string | ID эмитента                     | — |
| `mcc` | string | Код категории                   | — |
| `declineReason` | string | Полнотекстовый поиск по причине отказа | — |
| `limit` | int | Лимит                           | `50` |
| `offset` | int | Смещение                        | `0` |

Все параметры опциональны, активные объединяются через AND.

Для `declineReason` используется полнотекстовый поиск PostgreSQL. Перед поиском строка нормализуется:
- `_` и `-` заменяются на пробелы
- регистр игнорируется

Поэтому запросы `CARD_NOT_FOUND`, `card_not_found`, `card not found`, `card` и `found`
матчат одну и ту же причину отказа.

**Ответ 200:**
```json
{
  "total": 150,
  "transactions": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "stan": "000001",
      "rrn": "012345678901",
      "pan": "4000000000000001",
      "amount": 150000,
      "currencyCode": "643",
      "status": "APPROVED",
      "authCode": "ABC123",
      "merchantId": "MERCHANT001",
      "mcc": "5411",
      "transmissionDateTime": "2024-01-15T14:30:01Z"
    }
  ]
}
```

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | Невалидные параметры (`limit <= 0`, `offset < 0`) |

---

#### `GET /api/transactions/export`

Экспорт транзакций, удовлетворяющих фильтру, в CSV-файл.

**Query-параметры:** те же, что у `/api/transactions/search`, **кроме `limit`/`offset` — они не применяются.**

| Параметр | Тип | Описание | По умолчанию |
|----------|-----|----------|--------------|
| `pan` | string | Номер карты | — |
| `status` | string | `APPROVED` / `DECLINED` | — |
| `dateFrom` | date | С даты | — |
| `dateTo` | date | По дату | — |
| `merchantId` | string | ID мерчанта | — |
| `issuerId` | string | ID эмитента | — |
| `mcc` | string | Код категории | — |

Все параметры опциональны, активные объединяются через AND.

**Ответ 200:**
- `Content-Type: text/csv; charset=UTF-8`
- `Content-Disposition: attachment; filename="transactions.csv"`
- Тело - CSV: первая строка - заголовок, далее по строке на транзакцию (сортировка `createdAt DESC`). Поля со спецсимволами экранируются кавычками, `null` → пустое поле.

Пример:
```csv
id,mti,stan,rrn,pan,processingCode,amount,currencyCode,terminalId,terminalType,merchantId,mcc,acquirerId,issuerId,acquiringFee,status,declineReason,authCode,processingTimeMs,transmissionDateTime,createdAt
550e8400-e29b-41d4-a716-446655440000,0100,000001,012345678901,4000000000000001,000000,150000,643,TERM0001,POS,MERCHANT001,5411,ACQ001,ISS001,300,APPROVED,,ABC123,38,2024-01-15T14:30:01Z,2024-01-15T14:30:01Z
```

> Экспорт ограничен `50 000` строк. Если под фильтр попадает больше — выгружаются первые 50 000 (по `createdAt DESC`), в лог пишется предупреждение.

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | Невалидные параметры фильтра (например, `status=UNKNOWN`, `dateFrom > dateTo`) |

---

#### `GET /api/dashboard/stats`

**Ответ 200:**
```json
{
  "totalTransactions": 1250,
  "approvedCount": 1100,
  "declinedCount": 150,
  "approvalRate": 0.88,
  "totalAmount": 187500000,
  "averageAmount": 150000,
  "avgProcessingTimeMs": 38.5,
  "transactionsPerMinute": 12
}
```

---

#### `GET /api/dashboard/recent`

**Query-параметры:**

| Параметр | Тип | Описание | По умолчанию |
|----------|-----|----------|--------------|
| `limit` | int | Количество записей | `20` |

**Ответ 200:** массив транзакций, отсортированных по `createdAt DESC`.

---

#### `GET /api/dashboard/charts`

Агрегация транзакций по временным корзинам (час/день) для графиков Dashboard. Группировка и фильтрация — по `createdAt` в UTC; возвращаются **только непустые** корзины, упорядоченные по времени.

**Query-параметры:**

| Параметр | Тип    | Описание | По умолчанию |
|----------|--------|----------|--------------|
| `granularity` | string | Шаг агрегации: `hour` или `day` | `hour` |
| `from` | date   | Нижняя граница `createdAt`, включительно | — (с начала) |
| `to` | date   | Верхняя граница `createdAt`, исключительно | — (до конца) |

Диапазон - полуинтервал `[from, to)`. Если границы не заданы — агрегируются все транзакции.

**Ответ 200:** массив корзин.
```json
[
  {
    "timestamp": "2026-06-16T10:00:00Z",
    "total": 2,
    "approved": 1,
    "declined": 1,
    "amount": 150000
  },
  {
    "timestamp": "2026-06-16T11:00:00Z",
    "total": 1,
    "approved": 1,
    "declined": 0,
    "amount": 200000
  }
]
```

| Поле        | Описание                                       |
|-------------|------------------------------------------------|
| `timestamp` | Начало интервала (усечено до часа/дня, UTC)    |
| `total`     | Всего транзакций в интервале                   |
| `approved`  | Одобренных                                     |
| `declined`  | Отклонённых                                    |
| `amount`    | Суммарный объём в минорных единицах (копейках) |

**Ошибки:**

| Код | Условие |
|-----|---------|
| 400 | `granularity` не `hour`/`day`, либо `from > to` |

---

#### `WS /ws/transactions`

WebSocket-эндпоинт для получения транзакций в реальном времени.

**Подключение:** `ws://localhost:8080/ws/transactions`

Каждая новая транзакция рассылается всем подключённым клиентам в виде JSON-объекта транзакции.

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `{8080}` | Порт сервиса |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `processing` | Имя базы данных |
| `DB_USER` | `postgres` | Пользователь БД |
| `DB_PASSWORD` | `postgres` | Пароль БД |
| `{UPSTREAM}_URL` | `http://localhost:{port}` | URL смежного сервиса |

---

## Как запустить

### Локально (без Docker)

```bash
cd ../..
mvn -f services/pom.xml -Ptransaction-logger spring-boot:run -pl transaction-logger -am
```

### В Docker

```bash
docker build -t transaction-logger -f services/transaction-logger/Dockerfile .
docker run -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e POSTGRES_HOST=host.docker.internal \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=<db-name> \
  -e POSTGRES_USER=<db-user> \
  -e POSTGRES_PASSWORD=<db-password> \
  transaction-logger
```

Для обычной локальной проверки используйте Docker Compose: он поднимает PostgreSQL
и настраивает сеть между сервисами.

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose build transaction-logger
docker compose up -d transaction-logger
```

---

## Тестирование

```bash
mvn -f services/pom.xml -Ptransaction-logger test
```

---

## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем                                                  |
|--------|:-----------:|----------|--------------------------------------------------------|
| Switch | ← входящий | HTTP REST | Присылает транзакции для сохранения                    |
| Gateway | ← входящий | HTTP REST | Проксирует запросы поиска и статистики                 |
| Web Dashboard | ← входящий | WebSocket | Подключается для получения real-time потока транзакций |

---

## Структура проекта

```text
transaction-logger/
├── src/
│   ├── main/
│   │   ├── java/com/processing/transactionlogger/
│   │   │   ├── config/           # Конфигурация
│   │   │   ├── controller/       # HTTP-контроллеры
│   │   │   ├── dto/              # DTO поиска, health и dashboard
│   │   │   ├── exception/        # Обработка ошибок
│   │   │   ├── mapper/           # Маппинг entity <-> DTO
│   │   │   ├── model/            # JPA-сущности
│   │   │   ├── repository/       # Доступ к БД
│   │   │   ├── service/          # Бизнес-логика
│   │   │   ├── specification/    # JPA Specification (фильтрация)
│   │   │   └── websocket/        # WebSocket-конфигурация и хендлер
│   │   └── resources/
│   └── test/
├── Dockerfile
└── README.md
```

---

## Авторы

- Капусткин Никита — разработчик Logger #2 (Поиск и WebSocket)
- Сидякин Ярослав — разработчик Logger #1 (Приём транзакций)

**Группа:** C (Data), A (Core)
