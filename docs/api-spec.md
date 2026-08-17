# API-контракт СМП

>
> **Исходный формат:** OpenAPI 3.0 YAML
> **Версия:** 1.0.0
> **Базовый URL:** `http://localhost:8080`

---

## Правила

- Все эндпоинты, помеченные `[REQUIRED]`, обязательны к реализации
- Эндпоинты `[OPTIONAL]` — дополнительный функционал, бонусные баллы
- Формат дат: ISO 8601 (`2026-06-01T10:30:00Z`)
- Все суммы в минорных единицах (копейки/центы)

---

## 1. Health

### `GET /health` — `[REQUIRED]` Проверка работоспособности Gateway

**Ответ 200:**

```json
{
  "status": "ok",
  "service": "gateway",
  "version": "1.0.0",
  "services": {
    "switch": "ok",
    "authorization": "ok",
    "cardManagement": "ok",
    "logger": "ok"
  }
}
```

---

## 2. Cards (Card Management Service)

### `POST /api/cards` — `[REQUIRED]` Создать карту

**Тело запроса:**

```json
{
  "bin": "400000",
  "cardholderName": "IVAN IVANOV",
  "currencyCode": "643",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "initialBalance": 100000000
}
```

**Ответ 201:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "pan": "4000001234560001",
  "bin": "400000",
  "cardholderName": "IVAN IVANOV",
  "expiryDate": "1228",
  "status": "ACTIVE",
  "currencyCode": "643",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 100000000,
  "issuerId": "ISS001",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

Ошибки: `409` — карта с таким PAN уже существует.

### `POST /api/cards/generate` — `[REQUIRED]` Сгенерировать тестовые карты

**Тело запроса:**

```json
{
  "count": 100,
  "bins": ["400000", "400001", "400002", "400003", "400004"]
}
```

**Ответ 201:**

```json
{
  "generated": 100,
  "cards": [ /* массив Card */ ]
}
```

### `GET /api/cards/{pan}` — `[REQUIRED]` Получить карту по PAN

**Параметры пути:** `pan` — 16 цифр

**Ответ 200:** объект [`Card`](#card)
**Ответ 404:** карта не найдена

---

## 3. Transactions

### `POST /api/transactions` — `[REQUIRED]` Отправить запрос авторизации

**Тело запроса:**

```json
{
  "mti": "0100",
  "stan": "000001",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 150000,
  "currencyCode": "643",
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "terminalId": "TERM001",
  "terminalType": "POS",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001"
}
```

**Ответ 200:**

```json
{
  "mti": "0110",
  "stan": "000001",
  "rrn": "012345678901",
  "authCode": "ABC123",
  "responseCode": "00",
  "status": "APPROVED",
  "processingTimeMs": 42
}
```

**Ошибки:** `400` — ошибка валидации, `503` — внутренний сервис недоступен

### `GET /api/transactions/search` — `[REQUIRED]` Поиск транзакций

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|:---:|---|
| `pan` | string | Нет | Номер карты |
| `status` | string | Нет | APPROVED / DECLINED |
| `dateFrom` | date | Нет | Дата начала |
| `dateTo` | date | Нет | Дата окончания |
| `merchantId` | string | Нет | ID мерчанта |
| `declineReason` | string | Нет | Полнотекстовый поиск по причине отказа |
| `limit` | int | Нет | Лимит (по умолчанию 50) |
| `offset` | int | Нет | Смещение (по умолчанию 0) |

**Ответ 200:**

```json
{
  "total": 150,
  "transactions": [ /* массив Transaction */ ]
}
```

---

## 4. Simulator

### `POST /api/simulator/terminal/run` — `[REQUIRED]` Запуск симулятора терминалов

**Тело запроса:**

```json
{
  "count": 50,
  "scenario": "normal"
}
```

`scenario` enum: `normal`, `mixed`, `high_value`, `night_time`, `declines_test`

**Ответ 200:**

```json
{
  "totalSubmitted": 50,
  "approved": 47,
  "declined": 3,
  "elapsedMs": 2300,
  "transactions": [ /* массив AuthorizationResponse */ ]
}
```

### `POST /api/simulator/merchant/run` — `[REQUIRED]` Запуск симулятора мерчантов

**Тело запроса:**

```json
{
  "count": 50,
  "mccCodes": ["5411", "5812"],
  "scenario": "grocery"
}
```

`scenario` enum: `grocery`, `electronics`, `restaurant`, `travel`

**Ответ 200:** аналогично терминальному симулятору

---

## 5. Dashboard

### `GET /api/dashboard/stats` — `[REQUIRED]` Агрегированная статистика

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
  "transactionsPerMinute": 12.3
}
```

### `GET /api/dashboard/recent?limit=20` — `[REQUIRED]` Последние N транзакций

**Ответ 200:** массив [`Transaction`](#transaction)

---

## 6. WebSocket

### `WS /ws/transactions` — `[REQUIRED]` Поток транзакций в реальном времени

Каждое сообщение — JSON-объект [`Transaction`](#transaction).

---

## 7. Внутренние эндпоинты (Inter-Service)

### `POST /api/internal/route` — `[REQUIRED]` Switch: маршрутизация

**Тело запроса:** [`AuthorizationRequest`](#authorizationrequest)
**Ответ 200:** [`AuthorizationResponse`](#authorizationresponse)

### `POST /api/internal/authorize` — `[REQUIRED]` Authorization: проверка

**Тело запроса:** [`AuthorizationRequest`](#authorizationrequest)
**Ответ 200:** [`AuthorizationResponse`](#authorizationresponse)

### `POST /api/internal/log` — `[REQUIRED]` Logger: сохранение транзакции (вызывается Switch)

**Тело запроса:** полный объект [`Transaction`](#transaction)
**Ответ 201:** `{ "id": "uuid", "status": "stored" }`

---

## Модели данных

### Card

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор |
| `pan` | String(16) | Номер карты |
| `bin` | String(6) | BIN |
| `cardholderName` | String | Имя держателя |
| `expiryDate` | String(4) | MMYY |
| `status` | Enum | ACTIVE, INACTIVE, BLOCKED, EXPIRED |
| `currencyCode` | String(3) | 643 = RUB |
| `dailyLimit` | Integer | Дневной лимит (в копейках) |
| `monthlyLimit` | Integer | Месячный лимит (в копейках) |
| `availableBalance` | Integer | Доступный остаток (в копейках) |
| `issuerId` | String | ID банка-эмитента |
| `createdAt` | DateTime | Дата создания |

### AuthorizationRequest

| Поле | Тип        | Обязательное | Описание                                          |
|------|------------|:---:|---------------------------------------------------|
| `mti` | String(4)  | Да | "0100" = Auth Request                             |
| `stan` | String(6)  | Да | System Trace Audit Number                         |
| `pan` | String(16) | Да | Номер карты                                       |
| `processingCode` | String(6)  | Да | "000000" = покупка                                |
| `amount` | BigDecimal | Да | Сумма                                             |
| `currencyCode` | String(3)  | Да | "643"                                             |
| `transmissionDateTime` | DateTime   | Да | Время отправки                                    |
| `terminalId` | String(8)  | Да | ID терминала                                      |
| `terminalType` | Enum       | Нет | POS, ATM, ECOM                                    |
| `merchantId` | String(15) | Да | ID мерчанта                                       |
| `mcc` | String(4)  | Да | Merchant Category Code                            |
| `acquirerId` | String     | Да | ID эквайрера                                      |
| `issuerId` | String     | Нет | ID эмитента (заполняется Switch после маршрутизации) |

### AuthorizationResponse

| Поле | Тип | Описание |
|------|-----|----------|
| `mti` | String | "0110" = Auth Response |
| `stan` | String | STAN из запроса |
| `rrn` | String(12) | Retrieval Reference Number |
| `authCode` | String(6) | Код авторизации |
| `responseCode` | String(2) | 00 = Approved, 51 = Insufficient Funds, ... |
| `status` | Enum | APPROVED / DECLINED |
| `declineReason` | String | Причина отказа (опционально) |
| `processingTimeMs` | Integer | Время обработки в Authorization (от получения запроса до формирования ответа, не включает сетевые задержки Gateway↔Switch) |

### Transaction

Включает все поля `AuthorizationRequest` + `AuthorizationResponse`, а также:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор |
| `issuerId` | String | ID эмитента (определяется Switch) |
| `acquiringFee` | BigDecimal | Комиссия эквайрера (справочно, заполняется Merchant Simulator) |
| `createdAt` | DateTime | Время записи |

### Коды ответов (responseCode)

| Код | Значение |
|:---:|---|
| 00 | Approved |
| 05 | Do Not Honor |
| 12 | Invalid Transaction |
| 14 | Invalid Card Number |
| 30 | Format Error |
| 41 | Lost Card |
| 43 | Stolen Card |
| 51 | Insufficient Funds |
| 54 | Expired Card |
| 61 | Exceeds Amount Limit |
