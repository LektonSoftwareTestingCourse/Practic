# Card Management

## Назначение

Card Management создает карты, управляет балансом, проверяет наличие карты в бд.

---

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** PostgreSQL
- **Контейнеризация:** Docker

---

## Endpoints

| Метод  | Путь                        | Описание                                 |
|--------|-----------------------------|------------------------------------------|
| GET    | `/health`                   | Health-check сервиса                     |
| POST   | `/api/cards`                | Создать карту                            |
| GET    | `/api/cards/{PAN}`          | Получить карту по номеру                 |
| GET    | `/api/cards/`               | Получить карты по фильтрам               |
| PATCH  | `/api/cards/{PAN}`          | Изменить информацию о карте              |
| DELETE | `/api/cards/{PAN}`          | Удалить карту по номеру (soft delete)    |
| POST   | `/api/cards/generate`       | Сгенерировать N карт                     |
| POST   | `/api/cards/{PAN}/reserve`  | Списать Н-ное количество средств с карты |
| POST   | `/api/cards/{PAN}/rollback` | Выполнить возврат средств                |

### Подробно

#### `GET /health`

**Ответ 200:**

```json
{
  "status": "ok",
  "service": "card-management",
  "cardsInDatabase": 100500
}
```

#### `POST /api/cards`

**Тело запроса:**

```json
{
  "bin": "400000",
  "cardholderName": "TYLER DURDEN",
  "concurrencyCode": "683",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "initialBalance": 1000000
}
```

**Ответ 201:**

```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1234",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

#### `GET /api/cards/{PAN}`

**Ответ 200:**

```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1970-01-01",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

#### `GET /api/cards/`

**Тело запроса** (все данные опциональны)

```json
{
  "limit": 10,
  "offset": 0,
  "status": "ACTIVE",
  "bin": "123456",
  "issuerId": "ZZZZZZ",
  "startDate": "1970-01-01T00:00:00",
  "endDate": "2077-01-01T00:00:00"
}
```

**Ответ 200:**

```json
{
  // всего карт в БД, удовлетворяющих фильтрам
  "total": 100,
  "cards": [
    {
      "id": "d3b07384-d113-44a6-a070-658b446a81d4",
      "pan": "1234567891011121",
      "bin": "123456",
      "cardholderName": "TYLER DURDEN",
      "expiryDate": "1970-01-01",
      "status": "ACTIVE",
      "dailyLimit": 15000000,
      "monthlyLimit": 300000000,
      "availableBalance": 1000000,
      "issuerId": "ZZZZZZ",
      "createdAt": "1970-01-01T00:00:00"
    }
    // ...
  ]
}
```

#### `PATCH /api/cards/{pan}`

**Тело запроса** (все данные опциональны)

```json
{
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000
}
```

**Ответ 200**

```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1970-01-01",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

#### `DELETE /api/cards/{pan}`

**Ответ 204 (no content)**

#### `POST /api/cards/generate`

**Тело запроса**

```json
{
  "count": 100,
  "bin": [
    123456,
    222333,
    400000
  ]
  // not empty
}
```

**Ответ 201**

```json
{
  "generated": 100,
  "cards": [
    {
      "id": "d3b07384-d113-44a6-a070-658b446a81d4",
      "pan": "1234567891011121",
      "bin": "123456",
      "cardholderName": "TYLER DURDEN",
      "expiryDate": "1970-01-01",
      "status": "ACTIVE",
      "dailyLimit": 15000000,
      "monthlyLimit": 300000000,
      "availableBalance": 1000000,
      "issuerId": "ZZZZZZ",
      "createdAt": "1970-01-01T00:00:00"
    }
    // ...
  ]
}
```

#### `POST /api/cards/{pan}/reserve`

**Тело запроса**

```json
{
  "amount": 100,
  "rnn": "123412341234"
}
```

**Ответ 200**

```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1970-01-01",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

#### `POST /api/cards/{pan}/rollback`

**Тело запроса**

```json
{
  "rnn": "123412341234",
  "pan": "1234123412341234",
  "amount": 100
}
```

**Ответ 200**

```json
{
  "id": "d3b07384-d113-44a6-a070-658b446a81d4",
  "pan": "1234567891011121",
  "bin": "123456",
  "cardholderName": "TYLER DURDEN",
  "expiryDate": "1970-01-01",
  "status": "ACTIVE",
  "dailyLimit": 15000000,
  "monthlyLimit": 300000000,
  "availableBalance": 1000000,
  "issuerId": "ZZZZZZ",
  "createdAt": "1970-01-01T00:00:00"
}
```

---

**Ошибки:**

| Код | Условие                       |
|-----|-------------------------------|
| 400 | Ошибка валидации              |
| 402 | Недостаточно средств          |
| 404 | Карта не найдена              |
| 409 | Коллизия или ошибка состояния |
| 413 | Слишком большой запрос        |
| 500 | Неизвестная ошибка            |

**Пример**

```json
{
  "error": "com.processing.cardmanagement.exceptions.CardNotFoundException",
  "message": "Card with present PAN was not found",
  "timestamp": "1970-01-01T00:00:00",
  "serviceName": "card-management"
}
```

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

### Основные

| Переменная    | Значение по умолчанию | Описание        |
|---------------|-----------------------|-----------------|
| `SERVER_PORT` | `8080`                | Порт сервиса    |
| `DB_HOST`     | `localhost`           | Хост PostgreSQL |
| `DB_PORT`     | `5432`                | Порт PostgreSQL |
| `DB_NAME`     | `postgres`            | Имя базы данных |
| `DB_USER`     | `postgres`            | Пользователь БД |
| `DB_PASSWORD` | `postgres`            | Пароль БД       |

### Дополнительные

| Переменная                         | Значение по умолчанию | Описание                                                                      |
|------------------------------------|-----------------------|-------------------------------------------------------------------------------|
| `SERVICE_NAME`                     | `card-management`     | Название сервиса (для ответов ошибок)                                         |
| `CARD_VALIDITY_PERIOD`             | `3`                   | Срок действия карт (в годах)                                                  |
| `MAX_PAGE_LIMIT`                   | `10000`               | Максимальный размер LIMIT в GET /api/cards                                    |
| `MAX_CARD_CREATION_RETRIES`        | `3`                   | Максимальное количество попыток создания карты за сессию                      |
| `DEFAULT_PAGE_OFFSET`              | `0`                   | OFFSET по умолчанию в GET /api/cards (менять не рекомендуется)                |
| `DEFAULT_PAGE_LIMIT`               | `50`                  | LIMIT по умолчанию в GET /api/cards                                           |
| `DEFAULT_CURRENCY_CODE`            | `643`                 | Код валюты по умолчанию (`643` - рубли)                                       |
| `DEFAULT_DAILY_LIMIT`              | `15000000`            | Размер дневного лимита по умолчанию                                           |
| `DEFAULT_MONTHLY_LIMIT`            | `300000000`           | Размер месячного лимита по умолчанию                                          |
| `DEFAULT_BALANCE`                  | `1000000`             | Баланс по умолчанию                                                           |
| `GENERATOR_MIN_BALANCE`            | `1000000`             | Минимальный баланс при генерации карт                                         |
| `GENERATOR_MAX_BALANCE`            | `50000000`            | Максимальный баланс при генерации карт                                        |
| `GENERATOR_MIN_DAILY_LIMIT`        | `5000000`             | Минимальный дневной лимит при генерации карт                                  |
| `GENERATOR_MAX_DAILY_LIMIT`        | `30000000`            | Максимальный дневной лимит при генерации карт                                 |
| `GENERATOR_CURRENCY_CODE`          | `643`                 | Код валюты при генерации карт                                                 |
| `GENERATOR_MAX_COUNT`              | `10000`               | Максимальное количество генерируемых карт                                     |
| `LOAD_TESTS_MAX_PARALLEL_REQUESTS` | `50`                  | Максимальное количество параллельных операций при нагрузочном тестировании    |
| `OUTBOX_INTERVAL_MS`               | `1000`                | Частота обработки информации у паттерна Outbox в Event-системе                |
| `OUTBOX_MAX_RETRY_COUNT`           | `3`                   | Максимальное количество повторных попыток обработки события у паттерна Outbox |
| `METRICS_SCHEDULER_ENABLED`        | `true`                | Включить сбор обновляемых метрик у CardService                                |
| `METRICS_SHEDULER_UPDATE_RATE_MS`  | `5000`                | Как часто обновлять метрики (в милисекундах)                                  |

---

## Как запустить

### Локально

```bash
docker compose -f docker-compose.local.yml up -d # Если нужен Postgres
mvn spring-boot:run
```

### В Docker

```bash
docker build -t card-management .
docker run -p 8083:8080 --env-file ../.env card-management
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d card-management
```

---

## Тестирование

`(нужен контейнер с PostgreSQL)`

### Стандартные тесты

```bash
mvn test
```

### Нагрузочное тестирование

```bash
mvn test -P load
```

---

## Взаимодействие с другими сервисами

| Сервис                        | Направление | Протокол  | Зачем                                             |
|-------------------------------|:-----------:|-----------|---------------------------------------------------|
| Authorization Service         | ← входящий  | HTTP REST | Валидация, создание карт, работа с балансом счета |
| Terminal Simulator            | ← входящий  | HTTP REST | Генерация и получение карт                        |
| Merchant + Acquirer Simulator | ← входящий  | HTTP REST | Генерация и получение карт                        |
| Gateway                       | ← входящий  | HTTP REST | Все операции                                      |

---

## Структура проекта

```text
card-management/
├── src/
│   ├── main/
│   │   ├── configuration/  # Конфигурационные файлы Spring
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── events/         # Работа с Event-ами
│   │   ├── exceptions/     # Бизнес-исключения
│   │   ├── mappers/        # Приведение типов
│   │   ├── metrics/        # Сбор метрик
│   │   ├── models/         # Модели данных / DTO
│   │   ├── options/        # Настройки для компонентов
│   │   ├── repositories/   # Доступ к БД
│   │   ├── services/       # Бизнес-логика
│   └── test/               # Тесты
│       ├── components/     # Тестирование внутренних компонентов
│       ├── controllers/    # Интеграционное тестирование
│       ├── load/           # Нагрузочное тестирование
│       ├── outbox/         # Тестирование Outbox-системы
│       ├── services/       # Юнит-тесты сервисов
│       └── utils/          # Утилиты для написания тестов
├── Dockerfile
└── README.md
```

---

## Авторы

- Владимир Рубахин — разработчик
- Даниил Станкевич — разработчик

**Группа:** C (Data & Simulators)
