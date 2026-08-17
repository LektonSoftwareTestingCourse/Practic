# Техническое задание: Transaction Logger

**Роль:** Логирование всех транзакций и WebSocket-поток для Dashboard
**Количество человек:** 2
**Сложность:** ⭐⭐⭐
**Примерный объём кода:** 200–400 строк/чел (Go/Python/Java)

> **Деление внутри роли:** один человек отвечает за приём транзакций от Switch (`POST /api/internal/log` + сохранение в БД), второй — за поиск, статистику и WebSocket (`GET /api/transactions/search`, `/api/dashboard/*`, `/ws/transactions`). Первый работает в Группе A (Core) — тесная связка со Switch. Второй работает в Группе C (Data) — тесная связка с Dashboard и CMS.

---

## Ваша задача

Transaction Logger — это сервис, разделённый на две зоны ответственности:

**Logger #1 — Приём транзакций (Группа A — Core):**
1. Принимает транзакции от Switch синхронным POST-запросом и сохраняет их в PostgreSQL
2. Отвечает за надёжность записи и работу с БД

**Logger #2 — Поиск и WebSocket (Группа C — Data):**
1. Предоставляет API для поиска транзакций с фильтрацией и пагинацией
2. Отдаёт агрегированную статистику для Dashboard
3. Отдаёт поток транзакций в реальном времени через WebSocket

Вы — единственный источник истины обо всех транзакциях в системе. Тесная связка: Logger#1 со Switch (одна группа Core), Logger#2 с Dashboard (WebSocket-клиент) и CMS (общая БД).

---

## Обязательные требования

### 1. Health-check: `GET /health`

```json
{
  "status": "ok",
  "service": "transaction-logger",
  "transactionsStored": 1250
}
```

### 2. Приём транзакций: `POST /api/internal/log`

**Вызывается Switch после получения ответа от Authorization.**

**Тело запроса:** полный объект [`Transaction`]()

Что нужно сделать:
1. Десериализовать JSON в объект `Transaction`
2. Сохранить в PostgreSQL (таблица `transactions`)
3. Отправить через WebSocket всем подключённым клиентам
4. Вернуть HTTP 201 `{ "id": "uuid", "status": "stored" }`

> **Идемпотентность:** Switch может повторить запрос (retry при таймауте). Если в теле запроса передан `id`, который уже существует в БД — **не создавайте дубликат**. Верните HTTP 200 с существующей записью. Уникальность `id` обеспечивается на стороне Switch (генерация UUID). Уникальность `rrn` обеспечивается на стороне Authorization Service (формат `{YDDD}{HH}{mm}{ss}{seq}` с атомарным счётчиком). Logger — потребитель, а не генератор идентификаторов.

### 3. Поиск транзакций: `GET /api/transactions/search`

**Query-параметры:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `pan` | string | Поиск по номеру карты |
| `status` | string | APPROVED / DECLINED |
| `dateFrom` | date | С даты |
| `dateTo` | date | По дату |
| `merchantId` | string | ID мерчанта |
| `issuerId` | string | ID эмитента |
| `mcc` | string | Код категории |
| `limit` | int | Лимит (по умолчанию 50) |
| `offset` | int | Смещение (по умолчанию 0) |

**Ответ:**

```json
{
  "total": 150,
  "transactions": [ /* массив Transaction */ ]
}
```

Фильтрация должна работать в комбинации (AND-логика). Если параметр не указан — не фильтруем по нему.

### 4. Статистика для Dashboard: `GET /api/dashboard/stats`

**Ответ:**

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

Для расчёта `transactionsPerMinute` — считай транзакции за последнюю минуту:
```sql
SELECT COUNT(*) FROM transactions WHERE created_at > NOW() - INTERVAL '1 minute'
```
При активной симуляции значение может колебаться — это нормально для учебного проекта.

### 5. Последние транзакции: `GET /api/dashboard/recent?limit=20`

**Ответ:** массив последних N [`Transaction`]() (сортировка по `createdAt DESC`).

### 6. WebSocket: `WS /ws/transactions`

WebSocket-эндпоинт, который в реальном времени отправляет каждую новую транзакцию всем подключённым клиентам.

**Формат сообщения:** JSON-объект [`Transaction`]()

Базовый пример (Python FastAPI + WebSocket):

```python
from fastapi import WebSocket
from typing import List

class WebSocketManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            try:
                await connection.send_json(message)
            except:
                self.disconnect(connection)

ws_manager = WebSocketManager()

@app.websocket("/ws/transactions")
async def websocket_endpoint(websocket: WebSocket):
    await ws_manager.connect(websocket)
    try:
        while True:
            # Keep connection alive
            await websocket.receive_text()
    except:
        ws_manager.disconnect(websocket)
```

### 7. База данных

```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mti VARCHAR(4) NOT NULL DEFAULT '0100',
    stan VARCHAR(6) NOT NULL,
    rrn VARCHAR(12),
    pan VARCHAR(16) NOT NULL,
    processing_code VARCHAR(6) NOT NULL DEFAULT '000000',
    amount BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT '643',
    terminal_id VARCHAR(8) NOT NULL,
    merchant_id VARCHAR(15) NOT NULL,
    mcc VARCHAR(4) NOT NULL,
    acquirer_id VARCHAR(10) NOT NULL,
    issuer_id VARCHAR(10),
    acquiring_fee BIGINT,
    status VARCHAR(20) NOT NULL,
    decline_reason VARCHAR(100),
    auth_code VARCHAR(6),
    transmission_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_pan ON transactions(pan);
CREATE INDEX idx_transactions_merchant ON transactions(merchant_id);
```

---

## Дополнительные задания (бонусные баллы)

- Экспорт транзакций в CSV
- Агрегация по часам/дням (отдельный эндпоинт `GET /api/dashboard/charts`)
- Поиск по тексту (полнотекстовый поиск по `declineReason`)
- Сохранение в Elasticsearch вместо PostgreSQL (для production-like подхода)

---

## План работы по дням

| День | Задача |
|:---:|---|
| 1 | Изучить архитектуру. Настроить проект. Health-check |
| 2 | `POST /api/internal/log` — приём и сохранение транзакций в PostgreSQL |
| 3–4 | `GET /api/transactions/search` — поиск с фильтрацией и пагинацией |
| 5–6 | `GET /api/dashboard/stats` + `GET /api/dashboard/recent` |
| 7–8 | WebSocket: `/ws/transactions` — real-time поток |
| 9–10 | Dockerfile. Тестирование |
| 11–13 | Интеграционное тестирование со Switch и Dashboard |
| 14–15 | Полировка сервиса. Ревью куратора №3 (день 15). Code freeze |
| 16 | Рефакторинг: читаемость, стиль, комментарии, разделение на слои |
| 17 | Unit-тесты (минимум 3). Нагрузочное тестирование |
| 18 | Документация: README, API-эндпоинты, формат WebSocket-сообщений, схема БД |
| 19 | Сухая защита / репетиция. Доработка презентации и демо-сценария |
| 20 | 🎯 Финальная защита |

---

## Что сдаёте

| Артефакт | Кто | Описание |
|---|---|---|
| Исходный код | Оба | REST-приём (Logger#1) + поиск/статистика/WebSocket (Logger#2) |
| `Dockerfile` | Оба (общий) | Сборка и запуск в контейнере |
| `README.md` | Оба (общий) | API-эндпоинты, формат WebSocket-сообщений, схема БД |

---

## С какими сервисами взаимодействуете

- **Logger #1 (приём):** Switch → публикует транзакции в RabbitMQ; Logger потребляет через `@RabbitListener` из очереди `transaction-log`. REST-эндпоинт `POST /api/internal/log` сохранён для обратной совместимости. В одной группе Core — самая тесная связка.
- **Logger #2 (поиск/WS):** Gateway → проксирует `/api/transactions/search`, `/api/dashboard/*`. Web Dashboard → WebSocket-клиент. CMS → общая БД.

---

## Рекомендуемые технологии

- **Go:** net/http + gorilla/websocket (оптимально для WebSocket)
- **Python:** FastAPI + websockets
- **Java:** Spring Boot 3 + Spring WebSocket
