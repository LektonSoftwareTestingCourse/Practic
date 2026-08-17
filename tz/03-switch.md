# Техническое задание: Switch / Router

**Роль:** Маршрутизатор транзакций — «сердце» платёжной сети
**Количество человек:** 2
**Сложность:** ⭐⭐⭐⭐
**Примерный объём кода:** 500–900 строк (Java/Go)

---

## Ваша задача

Switch — это центральный маршрутизатор СМП. Когда поступает запрос на авторизацию, вы определяете, какому банку-эмитенту принадлежит карта, и направляете запрос в Authorization Service. После получения ответа — отправляете транзакцию в Transaction Logger синхронным HTTP-запросом.

Представьте: вы — «коммутатор» платёжной сети (как Visa/Mastercard). Все транзакции проходят через вас.

---

## Обязательные требования

### 1. Health-check: `GET /health`

Возвращает статус Switch и доступность Authorization Service:

```json
{
  "status": "ok",
  "service": "switch",
  "dependencies": {
    "authorization": "ok"
  }
}
```

### 2. Маршрутизация: `POST /api/internal/route`

**Вход:** [`AuthorizationRequest`]()

**Что нужно сделать:**

1. Извлечь BIN из PAN (первые 6 цифр)
2. Определить `issuerId` по BIN — используйте таблицу соответствия:

```go
// Пример — таблица BIN → issuer
var binTable = map[string]string{
    "400000": "ISS001",  // Банк 1
    "400001": "ISS002",  // Банк 2
    "400002": "ISS003",  // Банк 3
    "400003": "ISS004",  // Банк 4
    "400004": "ISS005",  // Банк 5
}
```

3. Дополнить запрос полем `issuerId`
4. Отправить запрос в Authorization Service (`POST /api/internal/authorize`)
5. Получить [`AuthorizationResponse`]()
6. **Отправить транзакцию в Logger** — **основной способ: асинхронная публикация в RabbitMQ**:
    - Публикация в exchange `smp.transactions` (тип `topic`) с routing key `transaction.log`
    - Тело: полный объект транзакции (запрос + ответ + issuerId + status)
    - **Publisher Confirms:** `RabbitTemplate` с `publisherConfirmType=CORRELATED`. Если брокер не подтверждает публикацию за 2 секунды — откат резервирования через reversal (mti="0400")
    - **Совместимость:** синхронный `POST /api/internal/log` сохранён как fallback

7. Вернуть ответ в Gateway

### 3. Отправка в Transaction Logger (основной способ — RabbitMQ)

После получения ответа от Authorization публикуете транзакцию в RabbitMQ:

**Exchange:** `smp.transactions` (тип `topic`)
**Routing key:** `transaction.log`
**Очередь:** `transaction-log` (consumer — Transaction Logger через `@RabbitListener`)

**Publisher Confirms:**
- Каждое сообщение отправляется с `CorrelationData` (transactionId)
- `RabbitTemplate` настроен с `publisherConfirmType=CORRELATED`
- Если `ConfirmCallback.isAck() == false` или таймаут подтверждения (2 секунды):
  - Для APPROVED транзакций: откат резервирования через reversal (mti="0400") в Authorization
  - Возврат клиенту `DECLINED` с `responseCode: "96"` (System Error)
  - Это гарантирует: **ни одна транзакция не будет APPROVED без записи в очереди**

**Совместимость:** синхронный HTTP-эндпоинт `POST http://logger:8088/api/internal/log` сохранён для обратной совместимости.

Формат тела сообщения (объединение AuthorizationRequest + AuthorizationResponse):

```json
{
  "id": "uuid",
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
  "status": "APPROVED",
  "declineReason": null,
  "authCode": "ABC123",
  "transmissionDateTime": "2026-06-01T10:30:00Z",
  "createdAt": "2026-06-01T10:30:00Z"
}
```

> `mti` сохраняется из исходного запроса (`"0100"`), а не из ответа (`"0110"`). Это поле отражает тип исходного запроса, а статус (`APPROVED`/`DECLINED`) — итог обработки.

### 4. Обработка ошибок

- Если BIN не найден в таблице — возвращаем decline с `responseCode: "14"` (Invalid Card Number)
- Если Authorization Service недоступен — retry (3 попытки), затем decline с `responseCode: "05"` (Do Not Honor)
- **Если RabbitMQ недоступен при публикации** (Publisher Confirm не получен за 2 секунды):
  1. Для APPROVED транзакций — **откатить резервирование**: отправить reversal-запрос (`mti="0400"`) в Authorization
  2. Вернуть клиенту `DECLINED` с `responseCode: "96"` (System Error)
  3. Для DECLINED транзакций — можно вернуть успешный ответ (логирование не критично)
  4. Это гарантирует: **ни одна транзакция не будет отмечена как APPROVED без записи в очереди**

> **Почему Publisher Confirms, а не просто «пропустить» очередь:** Если транзакция одобрена, но не опубликована — деньги списаны, а Logger не узнает о транзакции. Publisher Confirms — механизм RabbitMQ, гарантирующий что брокер принял сообщение. Без подтверждения — откат резервирования.

### 5. Логирование

Каждая транзакция логируется:
```
[INFO] TX 000001 | BIN=400000 → ISS001 | Status=APPROVED | 42ms
[WARN] TX 000002 | BIN=999999 → unknown BIN | DECLINED
[ERROR] Logger unavailable for TX 000001 — rolling back reservation
```

---

## Дополнительные задания (бонусные баллы)

- Circuit Breaker для вызова Authorization (Resilience4j)
- Динамическая таблица BIN (загрузка из Card Management Service при старте)
- **Reversal-транзакции (mti="0400")** — ⭐ рекомендуется: Switch должен уметь маршрутизировать reversal в Authorization для разрезервирования средств. Это требуется для отката при недоступности RabbitMQ (Publisher Confirm fail, см. п. 4)
- Idempotency key (защита от дублирования транзакций через `MessageProperties.messageId`)
- Retry с exponential backoff для Authorization (3 попытки, 1s → 2s → 4s)

---

## План работы по дням

| День | Задача |
|:---:|---|
| 1 | Изучить архитектуру и API-контракт. Выбрать язык. Настроить проект |
| 2 | Health-check. Создать таблицу BIN (5 записей). Скелет `POST /api/internal/route` |
| 3–4 | Реализовать маршрутизацию: BIN → issuerId. Интеграция с Authorization (пока заглушка) |
| 5 | Интеграция с Logger: синхронный POST /api/internal/log. Dockerfile |
| 6–8 | Интеграция с реальным Authorization Service. Обработка всех decline-сценариев |
| 9–10 | Retry, обработка ошибок, rollback при недоступности Logger |
| 11–13 | Интеграционное тестирование с Gateway, Authorization и Logger |
| 14–15 | Полировка сервиса. Ревью куратора №3 (день 15). Code freeze |
| 16 | Рефакторинг: читаемость, стиль, комментарии, разделение на слои |
| 17 | Unit-тесты (минимум 3). Нагрузочное тестирование |
| 18 | Документация: README, таблица BIN, схема маршрутизации |
| 19 | Сухая защита / репетиция. Доработка презентации и демо-сценария |
| 20 | 🎯 Финальная защита |

---

## Что сдаёте

| Артефакт | Описание |
|---|---|
| Исходный код | Switch с маршрутизацией и синхронной отправкой в Logger |
| `Dockerfile` | Сборка и запуск в контейнере |
| `README.md` | Таблица BIN, схема маршрутизации, как тестировать |

---

## С какими сервисами взаимодействуете

- **Gateway** → присылает запросы на вход
- **Authorization Service** → вызываете для проверки
- **Transaction Logger (приём)** → отправляете транзакции синхронным POST; Logger#1 в одной группе Core с вами

---

## Рекомендуемые технологии

- **Java:** Spring Boot 3 + RestTemplate / WebClient
- **Go:** net/http
- **Python:** FastAPI + httpx
