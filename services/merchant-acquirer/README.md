# Merchant acquirer simulator

## Назначение

Сервис отвечает за генерацию транзакций, которые отправляет на авторизацию в API Gateway. В отличие от Terminal Simulator, данный сервис генерирует правдоподобные транзакции на основе множества мерчантов и экваеров.

---

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** PostgreSQL
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь                          | Описание                              |
|-------|-------------------------------|---------------------------------------|
| GET   | `/health`                     | Health-check сервиса                  |
| GET   | `/api/simulator/merchants`    | Получение доступных мерчантов         |
| POST  | `/api/simulator/merchant/run` | Запуск симулятора отправки транзакций |
| POST  | `/api/simulator/merchant/fee` | Получение комиссии эквайера по транзакции |

### Подробно

#### `GET /health`

**Ответ 200:**
```json
{
  "status": "ok",
  "service": "{service-name}",
  "dependencies": {
    "{dep1}": "ok"
  }
}
```

**Ошибки:**

| Код | `error` | Условие |
|-----|---------|---------|
| 500 | Internal service error | Ошибка обращения к БД при подсчёте мерчантов |

#### `GET /api/simulator/merchants`

**Ответ 200:**
```json
{
  "id": "MERCH00000000005",
  "name": "Перекрёсток #042",
  "mcc": "5411",
  "category": "grocery",
  "acquirerId": "ACQ001",
  "acquiringFee": 0,
  "averageCheck": 120000
}
```

**Ошибки:**

| Код | `error` | Условие |
|-----|---------|---------|
| 500 | Internal service error | Ошибка обращения к БД при выборке мерчантов |

#### `POST /api/simulator/merchant/run`

**Тело запроса:**
```json
{
  "count": "value",
  "mccCodes": "massive",
  "scenario": "enum: grocery, electronics, restaurant, travel"
}
```

**Ответ 200:**
```json
{
  "totalSubmitted": 50,
  "approved": 47,
  "declined": 3,
  "elapsedMs": 2300,
  "transactions": ["AuthorizationResponse"]
}
```

**Ошибки:**

| Код | `error` | Условие |
|-----|---------|---------|
| 400 | Invalid request | `count < 1`, не указан `scenario` или некорректное JSON-тело |
| 404 | Resource not found | Нет мерчантов по заданным MCC, Card Management не вернул карт, либо у мерчанта нет терминалов |
| 502 | External service error | Недоступен Card Management или API Gateway (ошибка/таймаут вызова) |
| 500 | Internal service error | Непредвиденная внутренняя ошибка при построении/отправке транзакций |

#### `POST /api/simulator/merchant/fee`

**Тело запроса:**
```json
{
  "transmissionDateTime": "dataTime",
  "stan": "string",
  "pan": "string",
  "terminalId": "string",
  "amount": "BigDecimal"
}
```

**Ответ 200:**
```json
{
  "acquirerFee": "long"
}
```

---

**Ошибки:**

| Код | `error` | Условие |
|-----|---------|---------|
| 400 | Invalid request | Некорректное тело запроса |
| 404 | Resource not found | Комиссия по заданным `transmissionDateTime` / `stan` / `terminalId` не найдена |
| 500 | Internal service error | Непредвиденная внутренняя ошибка |


### Формат ошибок

Все ошибки возвращаются в едином формате `ErrorResponse`:

```json
{
  "error": "Resource not found",
  "message": "Merchants with given mcc ([0000]) not found",
  "timestamp": "2026-06-17T10:03:48.512",
  "serviceName": "Merchant acquirer simulator",
  "retryAfterMs": "0"
}
```

| Поле | Описание |
|------|----------|
| `error` | Краткая категория ошибки |
| `message` | Детали (поле, MCC, STAN и т.п.) |
| `serviceName` | Источник ошибки. При `502` — имя **вышестоящего** сервиса (`Card management`, `API Gateway`) |
| `retryAfterMs` | Через сколько мс повторять: `"0"` — повтор бессмыслен; `>0` — повторить через N мс (приходит из ответа upstream при `502`) |

**Ошибки: **

| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | {Сущность} не найдена |
| 500 | Непредвиденная внутрення ошибка сервиса |
| 502 | Внешний сервис не доступен |

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная          | Значение по умолчанию     | Описание                                                 |
|---------------------|---------------------------|----------------------------------------------------------|
| `PORT`              | `{8080}`                  | Порт сервиса                                             |
| `POSTGRES_HOST`     | `postgres`                | Хост PostgreSQL                                          |
| `POSTGRES_PORT`     | `5432`                    | Порт PostgreSQL                                          |
| `POSTGRES_DB`       | `smp_db`                  | Имя базы данных                                          |
| `POSTGRES_USER`     | `smp_user`                | Пользователь БД                                          |
| `POSTGRES_PASSWORD` | `smp_password`            | Пароль БД                                                |
| `GATEWAY_URL`       | `http://localhost:{port}` | URL смежного сервиса                                     |
| `CONCURRENCY`       | `50`                      | Количество потоков при параллельной отправки авторизаций |

---

## Как запустить

### Локально (без Docker)

```bash
./mvnw spring-boot:run
```

### В Docker

```bash
docker build -t merchant-aqcuirer .
docker run -p {port}:{port} --env-file ../.env merchant-aqcuirer
```

### В составе Docker Compose

```bash
docker compose up -d merchant-aqcuirer
```

---

## Тестирование

```bash
./mvnw test
```

---

## Взаимодействие с другими сервисами

| Сервис      | Направление | Протокол | Зачем                                                     |
|-------------|:----------:|----------|-----------------------------------------------------------|
| API Gateway | → исходящий | HTTP REST | Вызов API Gateway для отправки транзакции                 |
| API Gateway | ← входящий | HTTP REST | API Gateway возвращает результаты транзакций              |
| API Gateway | ← входящий | HTTP REST | Внутренний сервис обращается за получение комиссии экваера |
| API Gateway | → исходящий | HTTP REST | Возвращаю комиссию экэваера                               |

---

## Структура проекта

```text
merchant-aqcuirer/
├── src/
│   ├── main/
│   │   ├── client/         #Клиент для отправки HTTP запросов
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── domain/         # Доменные сущности
│   │   │   ├── entity/
│   │   │   ├── factory/
│   │   │   └── model/
│   │   ├── repositories/   # Доступ к БД
│   │   ├── services/       # Бизнес-логика
│   └── test/               # Тесты
├── Dockerfile
├── README.md
└── .env
```

---

## Авторы

- Козлов Богдан — разработчик

**Группа:** C (Data & Simulators)
