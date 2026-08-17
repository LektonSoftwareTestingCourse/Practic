# Техническое задание: Card Management Service

**Роль:** Управление картами — CRUD и генерация тестовых данных
**Количество человек:** 2
**Сложность:** ⭐⭐⭐
**Примерный объём кода:** 500–800 строк (Java/Python/Go)

---

## Ваша задача

Card Management Service — это источник данных о картах. Все сервисы, которым нужна информация о карте, обращаются к вам. Вы также генерируете тестовые карты, без которых вся система не сможет работать.

Критически важная роль: без генератора карт никто не сможет тестировать свои сервисы. Вас двое — разделите работу: один берёт CRUD и алгоритм Луна, второй — генератор тестовых карт и резервирование средств.

---

## Обязательные требования

### 1. Health-check: `GET /health`

```json
{
  "status": "ok",
  "service": "card-management",
  "cardsInDatabase": 100
}
```

### 2. CRUD карт

#### `POST /api/cards` — Создание карты

**Вход:**

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

**Что нужно сделать:**
1. Сгенерировать полный PAN: `bin` + 9 случайных цифр + 1 контрольная цифра (алгоритм Луна)
2. Сгенерировать `expiryDate` = текущая дата + 3 года (MMYY)
3. Присвоить `status = ACTIVE`
4. Сохранить в PostgreSQL
5. Вернуть созданную карту с полным PAN (включая незамаскированный — это же тестовая среда)

#### `GET /api/cards/{pan}` — Получение карты

Возвращает карту по PAN. Если не найдена — 404.

#### `GET /api/cards` — `[REQUIRED]` Список карт с пагинацией

**Query-параметры:**

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|:---:|---|
| `limit` | int | 50 | Количество карт на странице |
| `offset` | int | 0 | Смещение |
| `status` | string | — | Фильтр: ACTIVE, INACTIVE, BLOCKED, EXPIRED |
| `bin` | string | — | Фильтр по BIN |

**Ответ 200:**
```json
{
  "total": 100,
  "cards": [ /* массив Card */ ]
}
```

> **Примечание:** этот эндпоинт обязателен, т.к. Terminal Simulator использует его для получения пула карт.

#### `PATCH /api/cards/{pan}` — Обновление карты

Частичное обновление статуса, лимитов, баланса. В теле запроса передаются только изменяемые поля. Используйте HTTP-метод **PATCH** (не PUT).

#### `DELETE /api/cards/{pan}` — Удаление карты

Мягкое удаление: `status = DELETED`. Карты со статусом `DELETED` не возвращаются в `GET /api/cards/{pan}` и `GET /api/cards`, не участвуют в транзакциях. Статус `INACTIVE` зарезервирован для карт, сгенерированных как неактивные (3% при генерации).

### 3. Генератор тестовых карт: `POST /api/cards/generate`

**Вход:**

```json
{
  "count": 100,
  "bins": ["400000", "400001", "400002", "400003", "400004"]
}
```

**Что нужно сделать:**
1. Сгенерировать `count` карт, распределённых равномерно по BIN
2. Для каждой карты:
   - Случайное имя держателя из списка: `["IVAN IVANOV", "PETR PETROV", "ANNA SMIRNOVA", "ELENA VOLKOVA", "DMITRY SOKOLOV", ...]`
   - Случайный баланс: 10 000 – 500 000 ₽ (в копейках)
   - Случайный дневной лимит: 50 000 – 300 000 ₽
   - Месячный лимит = дневной × 30
   - Статус: ACTIVE (95%), INACTIVE (3%), BLOCKED (2%)
3. Вернуть список созданных карт

### 4. Алгоритм Луна (Luhn)

Проверка и генерация контрольной цифры PAN:

```python
def luhn_checksum(card_number: str) -> bool:
    """Проверяет корректность номера карты по алгоритму Луна."""
    digits = [int(d) for d in card_number]
    odd_digits = digits[-1::-2]
    even_digits = digits[-2::-2]
    total = sum(odd_digits)
    for d in even_digits:
        total += sum(divmod(d * 2, 10))
    return total % 10 == 0

def generate_pan(bin_prefix: str) -> str:
    """Генерирует полный PAN с BIN и контрольной цифрой."""
    base = bin_prefix + str(random.randint(100000000, 999999999))
    # Вычисляем контрольную цифру
    for check_digit in range(10):
        pan = base + str(check_digit)
        if luhn_checksum(pan):
            return pan
    raise Exception("Failed to generate valid PAN")
```

### 5. Резервирование средств: `POST /api/cards/{pan}/reserve`

Вызывается Authorization Service при успешной авторизации.

**Вход:** `{ "amount": 150000, "rrn": "012345678901" }`

**Что сделать:**
- `availableBalance -= amount`
- Сохранить в БД
- Вернуть 200 OK

### 6. База данных

Таблица `cards` в PostgreSQL:

```sql
CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pan VARCHAR(16) UNIQUE NOT NULL,
    bin VARCHAR(6) NOT NULL,
    cardholder_name VARCHAR(255) NOT NULL,
    expiry_date VARCHAR(4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    currency_code VARCHAR(3) NOT NULL DEFAULT '643',
    daily_limit BIGINT NOT NULL DEFAULT 15000000,
    monthly_limit BIGINT NOT NULL DEFAULT 300000000,
    available_balance BIGINT NOT NULL DEFAULT 100000000,
    issuer_id VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

При старте сервиса — создать таблицу, если её нет (миграция).

---

## Дополнительные задания (бонусные баллы)

- Валидация PAN при создании (проверка Луна)
- Расширенная фильтрация: по `issuerId`, диапазону дат создания

---

## План работы по дням

| День | Задача |
|:---:|---|
| 1 | Изучить архитектуру. Выбрать язык/фреймворк. Настроить PostgreSQL |
| 2 | Health-check. `POST /api/cards` — создание карты с алгоритмом Луна |
| 3 | `GET /api/cards/{pan}`, `PATCH /api/cards/{pan}`, `DELETE /api/cards/{pan}` |
| 4–5 | Генератор тестовых карт (`POST /api/cards/generate`). Сгенерировать 100 карт |
| 6–7 | `POST /api/cards/{pan}/reserve` — резервирование средств |
| 8–9 | Интеграционное тестирование с Authorization |
| 10 | Dockerfile. Миграция БД при старте |
| 11–13 | Оптимизация запросов. Интеграционное тестирование |
| 14–15 | Полировка сервиса. Ревью куратора №3 (день 15). Code freeze |
| 16 | Рефакторинг: читаемость, стиль, комментарии, разделение на слои |
| 17 | Unit-тесты (минимум 3). Нагрузочное тестирование |
| 18 | Документация: README, API-эндпоинты, формат PAN, примеры карт |
| 19 | Сухая защита / репетиция. Доработка презентации и демо-сценария |
| 20 | 🎯 Финальная защита |

---

## Что сдаёте

| Артефакт | Кто | Описание |
|---|---|---|
| Исходный код | Оба | CRUD + генератор карт + резервирование |
| `Dockerfile` | Оба (общий) | Сборка и запуск в контейнере |
| `README.md` | Оба (общий) | API-эндпоинты, формат PAN, примеры карт |

---

## С какими сервисами взаимодействуете

- **Authorization Service** → основной потребитель (запрос карт, резервирование)
- **Terminal Simulator** → запрашивает список карт для генерации транзакций
- **Notification Service** → асинхронный потребитель карточных событий через RabbitMQ (outbox-паттерн)

---

## Асинхронная публикация событий (Outbox → RabbitMQ, вариант C)

Card Management публикует карточные события (создание, обновление, удаление) через **outbox-паттерн**:

1. При изменении карты — запись в таблицу `outbox_event` со статусом `PENDING`
2. `OutboxEventProcessor` (scheduled, раз в 1 сек) читает `PENDING`-события
3. Публикует событие в exchange `smp.card-events` (тип `topic`) с routing key `card.*`
4. Обновляет статус на `PROCESSED` после успешной публикации
5. При недоступности RabbitMQ — retry с exponential backoff (3 попытки, затем `FAILED`)
6. Consumer — Notification Service (очередь `card-notifications`)

---

## Рекомендуемые технологии

- **Python:** FastAPI + SQLAlchemy + asyncpg
- **Java:** Spring Boot 3 + Spring Data JPA + PostgreSQL
- **Go:** database/sql + pgx
