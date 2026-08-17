# Switch / Router

## Назначение

Switch — центральный маршрутизатор СМП. Принимает запрос на авторизацию от Gateway, определяет банк-эмитент по BIN карты, вызывает Authorization Service и синхронно записывает транзакцию в Transaction Logger.

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **HTTP-клиент:** Spring RestClient
- **Retry:** Resilience4j
- **Контейнеризация:** Docker

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check Switch и доступности Authorization |
| POST | `/api/internal/route` | Маршрутизация транзакции по BIN |

### `GET /health`

```json
{
  "status": "ok",
  "service": "switch",
  "version": "1.0.0",
  "dependencies": {
    "authorization": "ok"
  }
}
```

### `POST /api/internal/route`

Внутренний вызов от Gateway. Switch извлекает BIN, дополняет `issuerId`, вызывает Authorization и Logger, возвращает `AuthorizationResponse`.

## Таблица BIN → issuerId

| BIN | issuerId | Банк |
|-----|----------|------|
| 400000 | ISS001 | Банк 1 |
| 400001 | ISS002 | Банк 2 |
| 400002 | ISS003 | Банк 3 |
| 400003 | ISS004 | Банк 4 |
| 400004 | ISS005 | Банк 5 |

Неизвестный BIN → `DECLINED`, `responseCode: "14"` (без вызовов Authorization и Logger).

## Схема маршрутизации

```
Gateway
   │ POST /api/internal/route
   ▼
Switch (RouteService)
   │ BIN → issuerId (RoutingService)
   ├─ unknown BIN ──────────────────────► DECLINED 14
   │
   ├─ POST /api/internal/authorize ────► Authorization (retry 3×)
   │                                      └─ fail ──► DECLINED 05
   │
   ├─ POST /api/internal/log ──────────► Transaction Logger (retry 3×, timeout 2s)
   │                                      └─ fail after APPROVED:
   │                                         reversal mti=0400 → Auth
   │                                         └──► DECLINED 96
   │
   └─ AuthorizationResponse ───────────► Gateway
```

## Обработка ошибок

| Ситуация | Действие | responseCode |
|----------|----------|--------------|
| BIN не найден | Отказ без downstream | 14 |
| Authorization недоступен (3 попытки) | DECLINED | 05 |
| Logger недоступен после APPROVED | Reversal `mti=0400` + DECLINED | 96 |
| Authorization DECLINED | Запись в Logger, pass-through ответа | 51, 41, … |

> Reversal (`mti=0400`) отправляется в Authorization Service. Полный E2E откат резервирования зависит от реализации reversal в Authorization.

## Payload в Logger

Switch отправляет `com.processing.common.dto.transactionlogger.TransactionRequest` (контракт Logger):

`id`, `mti`, `stan`, `rrn`, `pan`, `processingCode`, `amount`, `currencyCode`, `terminalId`, `merchantId`, `mcc`, `acquirerId`, `issuerId`, `acquiringFee`, `status`, `declineReason`, `authCode`, `processingTimeMs`, `transmissionDateTime`, `createdAt`.

- `mti` берётся из исходного запроса (`0100`), не из ответа Authorization.
- `acquiringFee` — запрашивается у Merchant Acquirer (`GET /api/simulator/merchant/fee`) до записи в Logger; при недоступности — `null`.

## Конфигурация

| Переменная | По умолчанию (локально) | Описание |
|------------|-------------------------|----------|
| `PORT` / `SERVER_PORT` | `8082` / `8080` (Docker) | Порт сервиса |
| `AUTH_URL` | `http://localhost:8083` | URL Authorization Service |
| `LOGGER_URL` | `http://localhost:8088` | URL Transaction Logger |
| `MERCHANT_URL` | `http://localhost:8086` | URL Merchant Acquirer (комиссия) |

Параметры в `application.yml`:

- `switch.http.connect-timeout-ms` — connect timeout HTTP-клиентов (3000 мс)
- `switch.http.auth-read-timeout-ms` — read timeout для Authorization (5000 мс)
- `switch.http.logger-read-timeout-ms` — read timeout для Logger (2000 мс)
- `switch.retry.max-attempts` — число попыток для Auth и Logger (3)
- `switch.retry.backoff-ms` — задержки между попытками Logger (1s, 2s, 4s; при большем числе попыток используется последнее значение)

## Запуск

### Локально

```bash
cd services
mvn spring-boot:run -Pswitch -pl switch
```

### Тесты

```bash
cd services
mvn test -Pswitch
```

E2E (TestNG, нужен `docker compose up -d`):

```bash
cd services
mvn test -Pe2e-tests -pl e2e-tests
```

### Docker

```bash
docker compose up switch
```

В контейнере порт `8080` (`SERVER_PORT=8080`), health-check: `GET /health`.

## Интеграционное тестирование (дни 11–13)

Поднять стек: `docker compose up -d`

### 1. Health-check

```bash
curl http://localhost:8082/health
```

Ожидание: `"status": "ok"`, `"authorization": "ok"` (или `"down"` если Auth не запущен).

### 2. Unknown BIN → код 14

```bash
curl -s -X POST http://localhost:8082/api/internal/route \
  -H "Content-Type: application/json" \
  -d '{
    "mti": "0100",
    "stan": "000099",
    "pan": "9999991234560001",
    "processingCode": "000000",
    "amount": 150000,
    "currencyCode": "643",
    "transmissionDateTime": "2026-06-01T10:30:00",
    "terminalId": "TERM001",
    "merchantId": "MERCH12345678901",
    "mcc": "5411",
    "acquirerId": "ACQ001"
  }'
```

Ожидание: `"responseCode": "14"`, `"status": "DECLINED"`.

### 3. Сквозная транзакция через Gateway

```bash
curl -s -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "mti": "0100",
    "stan": "000001",
    "pan": "4000001234560001",
    "processingCode": "000000",
    "amount": 150000,
    "currencyCode": "643",
    "transmissionDateTime": "2026-06-01T10:30:00",
    "terminalId": "TERM001",
    "merchantId": "MERCH12345678901",
    "mcc": "5411",
    "acquirerId": "ACQ001"
  }'
```

Ожидание: `"responseCode": "00"`, `"status": "APPROVED"`. Транзакция появляется в Logger (`GET /api/transactions` через Gateway).

### 4. Logger недоступен после APPROVED → код 96

1. Остановить Logger: `docker compose stop transaction-logger`
2. Повторить запрос с известным BIN и достаточным балансом
3. Ожидание: `"responseCode": "96"`, `"status": "DECLINED"`
4. В логах Switch: `Logger unavailable for TX ... — rolling back reservation`
5. Запустить Logger: `docker compose start transaction-logger`

> Для полного отката резервирования Authorization должен поддерживать reversal (`mti=0400`).

### 5. Decline-сценарии (pass-through)

При отказе Authorization (например, недостаточно средств — код `51`) Switch возвращает ответ Auth как есть и всё равно пишет транзакцию в Logger со статусом `DECLINED`.

## Взаимодействие с другими сервисами

| Сервис | Направление | Endpoint |
|--------|-------------|----------|
| Gateway | входящий | `POST /api/internal/route` |
| Authorization | исходящий | `POST /api/internal/authorize`, reversal `mti=0400` |
| Transaction Logger | исходящий | `POST /api/internal/log` |

---

## Авторы

- {Кононенко Кирилл} — разработчик
- {Климментий Аниськин} — разработчик

**Группа:** A { Core }
