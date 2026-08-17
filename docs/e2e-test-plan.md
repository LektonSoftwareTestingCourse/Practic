# План e2e тестирования — СМП

> **Тип:** End-to-End тест-кейсы

> **Фреймворк:** TestNG

> **Предусловие:** все 11 сервисов + PostgreSQL + RabbitMQ запущены

> **PostgreSQL:** `localhost:5432`

> **RabbitMQ:** `localhost:5672` (AMQP), `localhost:15672` (Management UI)
---

## Общие положения

### Стек технологий

| Категория             | Технология               | Maven-координата                                                | Описание                                                                               |
|-----------------------|--------------------------|-----------------------------------------------------------------|----------------------------------------------------------------------------------------|
| **Фреймворк тестов**  | TestNG 7.10.2            | `org.testng:testng:7.10.2`                                      | `@Test`, `@BeforeClass`, `@AfterClass`, `dependsOnMethods`, `priority`                 |
| **HTTP-клиент**       | REST Assured 5.4.0       | `io.rest-assured:rest-assured:5.4.0`                            | API для отправки HTTP-запросов и проверки ответов                                      |
| **JSON-парсинг**      | Jackson Databind 2.17.2  | `com.fasterxml.jackson.core:jackson-databind:2.17.2`            | Десериализация JSON-ответов в `JsonNode` / POJO                                        |
| **Jackson Java Time** | Jackson JSR310 2.17.2    | `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2` | Поддержка `Instant`, `LocalDateTime` в Jackson                                         |
| **Assertions**        | TestNG Assert            | `org.testng.Assert`                                             | `assertEquals`, `assertTrue`, `softAssert`, `assertAll`                                |
| **JDBC-соединение**   | `java.sql.DriverManager` | JDK 21 (stdlib)                                                 | `DriverManager.getConnection(url, user, password)` → `PreparedStatement` → `ResultSet` |
| **PostgreSQL JDBC**   | PostgreSQL Driver 42.x   | `org.postgresql:postgresql` (версия из Spring Boot BOM)         | Драйвер для подключения к `jdbc:postgresql://localhost:5432/postgres`                  |

### Схема верификации в каждом тесте

Каждый тест-кейс следует паттерну:

1. **HTTP-запрос** → проверка HTTP status code и JSON body
2. **DB-запрос** → проверка записей в таблицах PostgreSQL
3. **Сравнение** HTTP-ответа и DB-состояния

---

## Покрытие по сервисам

| Сервис                                            |            Тест-кейсы             |
|---------------------------------------------------|:---------------------------------:|
| Gateway (роутинг, валидация, rate-limit)          |    TC-01, TC-11, TC-12, TC-13     |
| Card Management (CRUD, генерация, резервирование) | TC-02, TC-03, TC-04, TC-05, TC-15 |
| Switch (маршрутизация по BIN)                     |           TC-06, TC-14            |
| Authorization (проверки, decline-коды)            |    TC-07, TC-08, TC-09, TC-10     |
| Terminal Simulator                                |           TC-16, TC-17            |
| Merchant Simulator                                |               TC-18               |
| Transaction Logger (поиск, статистика, WebSocket) |           TC-19, TC-20            |

---

## TC-01 — Health-check всех сервисов

**Цель:** Получение HTTP 200 на запрос `/health`.

**Шаги и assertions:**

1. `GET http://localhost:8080/health` (Gateway)
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.status == "ok"`
2. `GET http://localhost:8081/health` (Card Management)
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.status == "ok"`,
3. `GET http://localhost:8082/health` (Switch)
    - **HTTP assert:** `statusCode == 200`
4. `GET http://localhost:8083/health` (Authorization)
    - **HTTP assert:** `statusCode == 200`
5. `GET http://localhost:8085/health` (Terminal Simulator)
    - **HTTP assert:** `statusCode == 200`
6. `GET http://localhost:8086/health` (Merchant Simulator)
    - **HTTP assert:** `statusCode == 200`
7. `GET http://localhost:8088/health` (Transaction Logger)
    - **HTTP assert:** `statusCode == 200`
8. `GET http://localhost:3000` (Dashboard)
    - **HTTP assert:** `statusCode == 200` (HTML)

**DB assert:** нет (read-only тест)

---

## TC-02 — Создание карты (POST /api/cards)

**Цель:** Проверить, что создание карты возвращает HTTP 201, корректный JSON и запись в БД соответствует ответу.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards`
    - Body:
      ```json
      {
        "bin": "400000",
        "cardholderName": "TEST USER",
        "currencyCode": "643",
        "dailyLimit": 15000000,
        "monthlyLimit": 300000000,
        "initialBalance": 100000000
      }
      ```
    - **HTTP assert:** `statusCode == 201`
    - **Body assert:**
        - `$.id` — валидный UUID (не null, формат UUID)
        - `$.pan` — строка ровно 16 символов, только цифры
        - `$.pan` — проходит Luhn-алгоритм
        - `$.bin == "400000"`
        - `$.cardholderName == "TEST USER"`
        - `$.status == "ACTIVE"`
        - `$.currencyCode == "643"`
        - `$.dailyLimit == 15000000`
        - `$.monthlyLimit == 300000000`
        - `$.availableBalance == 100000000`
        - `$.expiryDate` — формат MMYY
        - `$.createdAt` — валидный ISO 8601

2. **DB assert** — `SELECT * FROM cards WHERE pan = '{response.pan}'`:
    - `id` == `response.id` (UUID совпадает)
    - `pan` == `response.pan`
    - `bin` == `"400000"`
    - `cardholder_name` == `"TEST USER"`
    - `status` == `"ACTIVE"`
    - `available_balance` == `100000000`
    - `daily_limit` == `15000000`
    - `monthly_limit` == `300000000`
    - `currency_code` == `"643"`
    - `issuer_id` != null (длина > 0)
    - `created_at` != null

---

## TC-03 — Массовая генерация тестовых карт

**Цель:** Проверить массовую генерацию карт, корректность записей в БД и валидность номера карты.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards/generate`
    - Body:
      ```json
      {
        "count": 100,
        "bins": ["400000","400001","400002","400003","400004"]
      }
      ```
    - **HTTP assert:** `statusCode == 201`
    - **Body assert:**
        - `$.generated >= 100`
        - `$.cards` — массив длины == `$.generated`
        - Каждый элемент `$.cards[i]` имеет валидный `id`, `pan` (16 цифр), `status`
        - Все `$.cards[*].pan` — уникальны
        - Все `$.cards[*].pan` — проходят Luhn-алгоритм
        - `$.cards[*].bin` содержит хотя бы по одной записи для каждого из 5 BIN

2. **DB assert** — `SELECT COUNT(*) FROM cards WHERE status <> 'DELETED'`:
    - `count >= 100`

3. **DB assert** — `SELECT DISTINCT bin FROM cards`:
    - Содержит все 5 значений: `400000`, `400001`, `400002`, `400003`, `400004`

4. **DB assert** — проверка Luhn для 5 случайных PAN из БД:
    - Каждый проходит Luhn-валидацию

---

## TC-04 — Получение карты по PAN и 404 для несуществующей

**Цель:** Проверить, что GET /api/cards/{pan} возвращает корректные данные карты или HTTP 404 для несуществующей.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards` — создать карту
    - Body:
      `{ "bin": "400000", "cardholderName": "TEST USER", "currencyCode": "643", "dailyLimit": 15000000, "monthlyLimit": 300000000, "initialBalance": 100000000 }`
    - **HTTP assert:** `statusCode == 201`
    - Сохранить `pan`, `id` из ответа

2. **HTTP GET** `http://localhost:8080/api/cards/{pan}` (PAN из шага 1)
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.pan == "{pan}"`, `$.id == "{id}"`, `$.status == "ACTIVE"`, `$.availableBalance == 100000000`

3. **DB assert** — `SELECT * FROM cards WHERE pan = '{pan}'`:
    - Запись существует
    - `available_balance == response.availableBalance`
    - `status == response.status`

4. **HTTP GET** `http://localhost:8080/api/cards/0000000000000000`
    - **HTTP assert:** `statusCode == 404`

5. **DB assert** — `SELECT COUNT(*) FROM cards WHERE pan = '0000000000000000'`:
    - `count == 0`

---

## TC-05 — PATCH и DELETE карты

**Цель:** Проверить, что обновление статуса карты и удаление отражаются как в HTTP-ответах, так и в БД.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards` — создать карту
    - **HTTP assert:** `statusCode == 201`
    - Сохранить `pan`, `id`

2. **DB assert** — `SELECT status FROM cards WHERE pan = '{pan}'`:
    - `status == "ACTIVE"`

3. **HTTP PATCH** `http://localhost:8080/api/cards/{pan}`
    - Body: `{ "status": "BLOCKED" }`
    - **HTTP assert:** `statusCode == 204`

4. **DB assert** — `SELECT status FROM cards WHERE pan = '{pan}'`:
    - `status == "BLOCKED"`

5. **HTTP GET** `http://localhost:8080/api/cards/{pan}`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.status == "BLOCKED"`
    - Сравнение: `response.status == db.status`

6. **HTTP DELETE** `http://localhost:8080/api/cards/{pan}`
    - **HTTP assert:** `statusCode == 204`

7. **DB assert** — `SELECT status FROM cards WHERE id = '{id}'`:
    - `status == "DELETED"`

8. **HTTP GET** `http://localhost:8080/api/cards/{pan}`
    - **HTTP assert:** `statusCode == 404`

---

## TC-06 — Полный цикл одиночной транзакции (APPROVED)

**Цель:** Проверить полный E2E-цикл транзакции: Gateway → Switch → Authorization → CMS → Logger. Верифицировать
состояние БД на каждом шаге.

**Шаги и assertions:**

0. **Подготовка:** Создать карту через `POST /api/cards` с `initialBalance: 100000000`. Сохранить `pan`.

1. **HTTP GET** `http://localhost:8080/api/cards/{pan}` — получить текущий баланс
    - **HTTP assert:** `statusCode == 200`
    - Сохранить `balanceBefore = $.availableBalance`

2. **DB assert** — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - `available_balance == balanceBefore`
    - Сравнение: DB и HTTP возвращают одинаковый баланс

3. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body:
      ```json
      {
        "mti": "0100",
        "stan": "000001",
        "pan": "{pan}",
        "processingCode": "000000",
        "amount": 150000,
        "currencyCode": "643",
        "transmissionDateTime": "2026-06-01T10:30:00Z",
        "terminalId": "TERM001",
        "merchantId": "MERCH00000000001",
        "mcc": "5411",
        "acquirerId": "ACQ001"
      }
      ```
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.status == "APPROVED"`
        - `$.responseCode == "00"`
        - `$.mti == "0110"`
        - `$.stan == "000001"`
        - `$.rrn` — строка ровно 12 символов, только цифры
        - `$.authCode` — строка ровно 6 символов, алфавитно-цифровая
        - `$.processingTimeMs > 0`

4. **DB assert** (cards) — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - `available_balance == balanceBefore - 150000`
    - Сравнение: баланс уменьшился ровно на `amount`

5. **HTTP GET** `http://localhost:8080/api/cards/{pan}`
    - **HTTP assert:** `$.availableBalance == balanceBefore - 150000`
    - Сравнение: HTTP и DB показывают одинаковый новый баланс

6. **DB assert** (transactions) — `SELECT * FROM transactions WHERE stan = '000001'`:
    - Запись существует (1 строка)
    - `status == "APPROVED"`
    - `pan == "{expected_pan}"`
    - `amount == 150000`
    - `rrn` == `response.rrn`
    - `auth_code` == `response.authCode`
    - `issuer_id` != null
    - `created_at` != null
    - Сравнение: `rrn`, `auth_code`, `status` совпадают с HTTP-ответом

---

## TC-07 — Decline: Недостаточно средств (responseCode 51)

**Цель:** Проверить отказ по причине недостатка средств (INSUFFICIENT_FUNDS). Баланс в БД не изменился.

**Шаги и assertions:**

0. **Подготовка:** Создать карту через `POST /api/cards` с `initialBalance: 1000`. Сохранить `pan`.

1. **DB assert** — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - Сохранить `balanceBefore` (= 1000)

2. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body: валидный запрос, но `amount` > `balanceBefore`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.status == "DECLINED"`
        - `$.responseCode == "51"`
        - `$.declineReason` содержит "INSUFFICIENT_FUNDS" или "insufficient"
        - `$.rrn` == null или отсутствует
        - `$.authCode` == null или отсутствует

3. **DB assert** (cards) — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - `available_balance == balanceBefore` (не изменился)
    - Сравнение: баланс не списан — и HTTP, и DB подтверждают

4. **DB assert** (transactions) — `SELECT * FROM transactions WHERE stan = '{stan}' AND status = 'DECLINED'`:
    - Запись существует
    - `decline_reason` содержит "INSUFFICIENT_FUNDS"
    - `status == "DECLINED"`

---

## TC-08 — Decline: Невалидный PAN / карта не найдена (responseCode 14)

**Цель:** Проверить отказ CARD_NOT_FOUND для несуществующего PAN.

**Шаги и assertions:**

1. **DB assert** — `SELECT COUNT(*) FROM cards WHERE pan = '9999999999999999'`:
    - `count == 0` (подтверждение что карты нет)

2. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body: валидный запрос, `"pan": "9999999999999999"`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.status == "DECLINED"`
        - `$.responseCode == "14"`
        - `$.declineReason` содержит "CARD_NOT_FOUND" или "card not found"

3. **DB assert** (transactions) — `SELECT * FROM transactions WHERE pan = '9999999999999999' AND status = 'DECLINED'`:
    - Запись существует
    - `decline_reason` содержит "CARD_NOT_FOUND"
    - Сравнение: responseCode из HTTP == decline-код в DB

---

## TC-09 — Decline: Заблокированная карта (responseCode 05)

**Цель:** Проверить, что заблокированная карта отклоняется с корректным кодом отказа.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards` — создать карту
    - **HTTP assert:** `statusCode == 201`
    - Сохранить `pan`

2. **DB assert** — `SELECT status FROM cards WHERE pan = '{pan}'`:
    - `status == "ACTIVE"`

3. **HTTP PATCH** `http://localhost:8080/api/cards/{pan}`
    - Body: `{ "status": "BLOCKED" }`
    - **HTTP assert:** `statusCode == 204`

4. **DB assert** — `SELECT status FROM cards WHERE pan = '{pan}'`:
    - `status == "BLOCKED"` (DB подтверждает блокировку)

5. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body: валидный запрос с этим `pan`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.status == "DECLINED"`
        - `$.responseCode == "05"`
        - `$.declineReason` содержит "BLOCKED"

6. **DB assert** (transactions) — `SELECT * FROM transactions WHERE pan = '{pan}' AND status = 'DECLINED'`:
    - Запись существует
    - `decline_reason` содержит "BLOCKED"
    - Сравнение: `responseCode` из HTTP (`"05"`) == код в declineReason DB

---

## TC-10 — Decline: Истёкшая карта (responseCode 54)

**Цель:** Проверить, что истёкшая карта отклоняется с корректным кодом отказа.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards` — создать карту с `expiryDate` в прошлом (через PATCH или напрямую,
   если API позволяет)
    - Альтернатива: создать карту, затем напрямую в БД установить `expiry_date = '0120'`
    - Сохранить `pan`

2. **DB assert** — `SELECT expiry_date, status FROM cards WHERE pan = '{pan}'`:
    - `expiry_date` указывает на дату в прошлом
    - `status == "ACTIVE"` (карта активна, но истекла)

3. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body: валидный запрос с этим `pan`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.status == "DECLINED"`
        - `$.responseCode == "54"`
        - `$.declineReason` содержит "EXPIRED"

4. **DB assert** (transactions) — `SELECT * FROM transactions WHERE pan = '{pan}' AND status = 'DECLINED'`:
    - Запись существует
    - `decline_reason` содержит "EXPIRED"
    - Сравнение: responseCode из HTTP == `"54"` совпадает с DB decline_reason

5. **DB assert** (cards) — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - Баланс не изменился (средства не резервировались)

---

## TC-11 — Валидация: Gateway отклоняет невалидный запрос (400)

**Цель:** Проверить, что Gateway отклоняет невалидные запросы с HTTP 400.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/transactions` — пустой body `{}`
    - **HTTP assert:** `statusCode == 400`

2. **HTTP POST** `http://localhost:8080/api/transactions` — без `pan`
    - Body: `{ "mti":"0100", "stan":"000001", "amount":100, "currencyCode":"643" }` (остальные поля)
    - **HTTP assert:** `statusCode == 400`

3. **HTTP POST** `http://localhost:8080/api/transactions` — `pan: "123"` (не 16 цифр)
    - **HTTP assert:** `statusCode == 400`

4. **HTTP POST** `http://localhost:8080/api/transactions` — `amount: -100`
    - **HTTP assert:** `statusCode == 400`

5. **DB assert** — `SELECT COUNT(*) FROM transactions` до и после:
    - Количество записей не изменилась (невалидные запросы не создают транзакций)

---

## TC-12 — Gateway: Проксирование запросов к Card Management

**Цель:** Проверить, что Gateway корректно проксирует запросы `/api/cards/**` к Card Management.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards` (через Gateway)
    - Body:
      `{ "bin":"400000", "cardholderName":"PROXY TEST", "currencyCode":"643", "dailyLimit":1000000, "monthlyLimit":10000000, "initialBalance":5000000 }`
    - **HTTP assert:** `statusCode == 201`
    - Сохранить `pan` и `id` из ответа

2. **HTTP GET** `http://localhost:8081/api/cards/{pan}` (напрямую в Card Management)
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.cardholderName == "PROXY TEST"`, `$.availableBalance == 5000000`

3. **Сравнение:** ответ Gateway (шаг 1) и ответ CMS (шаг 2) — одинаковые поля `id`, `pan`, `status`, `availableBalance`

4. **HTTP GET** `http://localhost:8080/api/cards/{pan}` (через Gateway)
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** идентичен ответу из шага 2

5. **HTTP GET** `http://localhost:8080/api/cards?limit=5` (список через Gateway)
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.cards` — массив, `$.total >= 1`

6. **DB assert** — карта из шага 1 существует в `cards`:
    - `SELECT * FROM cards WHERE pan = '{pan}'` — запись найдена

---

## TC-13 — Gateway: Проксирование запросов к Transaction Logger

**Цель:** Проверить, что Gateway корректно проксирует запросы `/api/transactions/search` и `/api/dashboard/**`.

**Шаги и assertions:**

0. **Подготовка:** Создать карту через `POST /api/cards`. Отправить `POST /api/transactions` для создания хотя бы одной
   транзакции.

1. **HTTP GET** `http://localhost:8080/api/transactions/search?limit=5`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.total > 0`, `$.transactions` — массив, `$.transactions.length <= 5`

2. **HTTP GET** `http://localhost:8088/api/transactions/search?limit=5` (напрямую в Logger)
    - **HTTP assert:** `statusCode == 200`
    - **Сравнение:** `$.total` идентичен шагу 1, `$.transactions` — тот же набор данных

3. **HTTP GET** `http://localhost:8080/api/dashboard/stats`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.totalTransactions > 0`
        - `$.approvedCount >= 0`
        - `$.declinedCount >= 0`
        - `$.totalTransactions == $.approvedCount + $.declinedCount`

4. **HTTP GET** `http://localhost:8080/api/dashboard/recent?limit=10`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** массив длины <= 10, каждый элемент содержит `pan`, `status`, `amount`

5. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - `count == stats.totalTransactions` (DB и HTTP согласованы)

---

## TC-14 — Switch: Маршрутизация по BIN (5 BIN → 5 issuerId)

**Цель:** Проверить, что Switch маршрутизирует транзакции в корректный issuerId на основе BIN.

**Шаги и assertions:**

0. **Подготовка:** Сгенерировать карты через `POST /api/cards/generate` с
   `bins: ["400000","400001","400002","400003","400004"]`, `count: 50`.

Для каждого BIN из `["400000", "400001", "400002", "400003", "400004"]`:

1. Найти ACTIVE карту: `SELECT pan FROM cards WHERE bin = '{bin}' AND status = 'ACTIVE' LIMIT 1`

2. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body: валидный запрос с `pan` найденной карты
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.status` == `"APPROVED"` или `"DECLINED"` (оба допустимы)

3. **DB assert** (transactions) — `SELECT issuer_id FROM transactions WHERE pan = '{pan}'`:
    - `issuer_id` != null
    - `issuer_id` соответствует ожидаемому для данного BIN (например, `ISS001` для `400000`, `ISS002` для `400001`, и
      т.д.)

4. **Сравнение:** `issuerId` из HTTP-ответа == `issuer_id` из DB

**Итоговый assert:**

- 5 транзакций с 5 разными `issuerId`
- Каждый BIN → свой issuerId (маппинг `400000→ISS001`, `400001→ISS002`, ..., `400004→ISS005`)

---

## TC-15 — Резервирование средств (POST /api/cards/{pan}/reserve)

**Цель:** Проверить, что резервирование средств корректно уменьшает баланс как в HTTP-ответах, так и в БД.

**Шаги и assertions:**

1. **HTTP POST** `http://localhost:8080/api/cards` — создать карту с `initialBalance: 100000000`
    - **HTTP assert:** `statusCode == 201`
    - Сохранить `pan`

2. **DB assert** — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - `available_balance == 100000000`

3. **HTTP POST** `http://localhost:8080/api/cards/{pan}/reserve`
    - Body: `{ "amount": 500000, "rrn": "123456789012" }`
    - **HTTP assert:** `statusCode == 200`

4. **DB assert** — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - `available_balance == 99500000` (100000000 - 500000)
    - Сравнение: ожидаемое значение точно совпадает

5. **HTTP GET** `http://localhost:8080/api/cards/{pan}`
    - **HTTP assert:** `$.availableBalance == 99500000`
    - Сравнение: HTTP GET и DB возвращают одинаковый баланс

6. Повторить резервирование: **HTTP POST** `http://localhost:8080/api/cards/{pan}/reserve`
    - Body: `{ "amount": 300000, "rrn": "123456789013" }`
    - **HTTP assert:** `statusCode == 200`

7. **DB assert** — `SELECT available_balance FROM cards WHERE pan = '{pan}'`:
    - `available_balance == 99200000` (99500000 - 300000)

---

## TC-16 — Terminal Simulator: Сценарий "normal"

**Цель:** Проверить, что симулятор терминала генерирует транзакции в сценарии "normal" и результаты отражаются в БД.

**Шаги и assertions:**

0. **Подготовка:** Сгенерировать карты через `POST /api/cards/generate` с
   `bins: ["400000","400001","400002","400003","400004"]`, `count: 50`.

1. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - Сохранить `countBefore`

2. **HTTP POST** `http://localhost:8080/api/simulator/terminal/run`
    - Body: `{ "count": 50, "scenario": "normal" }`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.totalSubmitted == 50`
        - `$.approved + $.declined == 50`
        - `$.approved > 0` (при normal-сценарии большинство APPROVED)
        - `$.transactions` — массив длины 50
        - Каждый `$.transactions[i]` содержит `status`, `stan`, `mti`

3. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - `count - countBefore == 50` (ровно 50 новых записей)

4. **DB assert** — `SELECT status, COUNT(*) FROM transactions WHERE created_at > '{test_start}' GROUP BY status`:
    - `SUM(count) == 50`
    - APPROVED-count + DECLINED-count == 50
    - Сравнение: `APPROVED-count == response.approved`, `DECLINED-count == response.declined`

5. **DB assert** — проверить что транзакции содержат MCC `5411` (normal-сценарий):
    - `SELECT DISTINCT mcc FROM transactions WHERE created_at > '{test_start}'`
    - Содержит `"5411"`

---

## TC-17 — Terminal Simulator: Сценарий "declines_test"

**Цель:** Проверить, что сценарий "declines_test" генерирует транзакции с разнообразными кодами отказа.

**Шаги и assertions:**

1. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - Сохранить `countBefore`

2. **HTTP POST** `http://localhost:8080/api/simulator/terminal/run`
    - Body: `{ "count": 50, "scenario": "declines_test" }`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.totalSubmitted == 50`
        - `$.declined > 0` (сценарий генерирует decline-транзакции)
        - `$.transactions` — массив длины 50

3. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE created_at > '{test_start}'`:
    - `count - countBefore == 50`

4. **DB assert** —
   `SELECT DISTINCT decline_reason FROM transactions WHERE created_at > '{test_start}' AND status = 'DECLINED'`:
    - Содержит多种 типы decline (CARD_NOT_FOUND, INSUFFICIENT_FUNDS, EXPIRED, BLOCKED, и т.д.)

5. **DB assert** — проверить что в `transactions` присутствуют транзакции с разными decline-кодами:
    - Есть записи с `decline_reason` содержащим "CARD_NOT_FOUND" (responseCode 14)
    - Есть записи с `decline_reason` содержащим "EXPIRED" (responseCode 54)
    - Есть записи с `decline_reason` содержащим "INSUFFICIENT_FUNDS" (responseCode 51)
    - Есть записи с `decline_reason` содержащим "BLOCKED" (responseCode 05)

6. **Сравнение:** сумма `$.approved + $.declined` из HTTP == количество записей в DB за период теста

---

## TC-18 — Merchant Simulator: Сценарий "grocery"

**Цель:** Проверить, что симулятор мерчанта генерирует транзакции с корректным MCC и рассчитанной комиссией.

**Шаги и assertions:**

1. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - Сохранить `countBefore`

2. **HTTP POST** `http://localhost:8080/api/simulator/merchant/run`
    - Body: `{ "count": 20, "mccCodes": ["5411"], "scenario": "grocery" }`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.totalSubmitted == 20`
        - `$.transactions` — массив длины 20
        - Каждый `$.transactions[i]` содержит `status`, `stan`

3. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE created_at > '{test_start}'`:
    - `count - countBefore == 20`

4. **DB assert** — `SELECT mcc, COUNT(*) FROM transactions WHERE created_at > '{test_start}' GROUP BY mcc`:
    - MCC `"5411"` — основной (100% или подавляющее большинство)

5. **DB assert** — `SELECT acquiring_fee FROM transactions WHERE created_at > '{test_start}' LIMIT 5`:
    - `acquiring_fee > 0` (комиссия рассчитана)
    - Каждый `acquiring_fee` == ~1-3% от `amount` (допуск ±1%)

6. **Сравнение:** `$.approved + $.declined` из HTTP == количество записей в DB

---

## TC-19 — Поиск транзакций с фильтрами (AND-логика)

**Цель:** Проверить поиск транзакций с фильтрами, пагинацией и AND-логикой. Верифицировать соответствие HTTP-результатов
данным в БД.

**Шаги и assertions:**

0. **Подготовка:** Создать карту через `POST /api/cards`. Отправить `POST /api/transactions` для создания хотя бы 3
   транзакций (2 APPROVED, 1 DECLINED). Сохранить `pan`.

1. **HTTP GET** `http://localhost:8080/api/transactions/search?limit=10&offset=0`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.total > 0`, `$.transactions.length <= 10`

2. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - `count == response.total`

3. **HTTP GET** `http://localhost:8080/api/transactions/search?status=APPROVED`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** все `$.transactions[*].status == "APPROVED"`

4. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED'`:
    - `count == response.total`

5. **HTTP GET** `http://localhost:8080/api/transactions/search?status=DECLINED`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** все `$.transactions[*].status == "DECLINED"`

6. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE status = 'DECLINED'`:
    - `count == response.total`

7. **HTTP GET** `http://localhost:8080/api/transactions/search?pan={known_pan}`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** все `$.transactions[*].pan == "{known_pan}"`

8. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE pan = '{known_pan}'`:
    - `count == response.total`

9. **HTTP GET** `http://localhost:8080/api/transactions/search?status=APPROVED&pan={known_pan}`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** все `$.transactions[*].status == "APPROVED"` и `$.transactions[*].pan == "{known_pan}"`

10. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED' AND pan = '{known_pan}'`:
    - `count == response.total` (AND-логика работает)

11. **Пагинация:**
    - **HTTP GET** `http://localhost:8080/api/transactions/search?limit=2&offset=0`
        - **HTTP assert:** `$.transactions.length == 2`
        - Сохранить `id1 = $.transactions[0].id`, `id2 = $.transactions[1].id`
    - **HTTP GET** `http://localhost:8080/api/transactions/search?limit=2&offset=2`
        - **HTTP assert:** `$.transactions.length == 2`
        - **Assert:** `$.transactions[0].id != id1` и `$.transactions[0].id != id2` (нет дубликатов)

---

## TC-20 — Dashboard статистика и агрегация

**Цель:** Проверить, что статистика Dashboard непротиворечива и соответствует данным в БД.

**Шаги и assertions:**

0. **Подготовка:** Создать карту через `POST /api/cards`. Отправить `POST /api/transactions` для создания нескольких
   транзакций с разными суммами (APPROVED + DECLINED).

1. **HTTP GET** `http://localhost:8080/api/dashboard/stats`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - `$.totalTransactions > 0`
        - `$.totalTransactions == $.approvedCount + $.declinedCount` (внутренняя непротиворечивость)
        - `$.approvalRate == $.approvedCount / $.totalTransactions` (±0.01)
        - `$.totalAmount > 0`
        - `$.averageAmount > 0`
        - `$.averageAmount == $.totalAmount / $.totalTransactions` (±1)
        - `$.avgProcessingTimeMs > 0`

2. **DB assert** — `SELECT COUNT(*) FROM transactions`:
    - `count == stats.totalTransactions`

3. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE status = 'APPROVED'`:
    - `count == stats.approvedCount`

4. **DB assert** — `SELECT COUNT(*) FROM transactions WHERE status = 'DECLINED'`:
    - `count == stats.declinedCount`

5. **DB assert** — `SELECT SUM(amount) FROM transactions`:
    - `sum == stats.totalAmount`

6. **DB assert** — `SELECT AVG(amount) FROM transactions`:
    - `avg ≈ stats.averageAmount` (±1)

7. **HTTP GET** `http://localhost:8080/api/dashboard/recent?limit=20`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:**
        - Массив длины <= 20
        - Каждый элемент содержит: `id`, `pan`, `status`, `amount`, `createdAt`
        - Элементы отсортированы по `createdAt DESC` (каждый следующий `createdAt` <= предыдущего)

8. **DB assert** — `SELECT * FROM transactions ORDER BY created_at DESC LIMIT 20`:
    - Первый элемент DB `id == response.recent[0].id` (самая новая транзакция)
    - Последний элемент DB `id == response.recent[last].id`
    - Сравнение: HTTP recent и DB top-20 идентичны по `id` и порядку

---

## Матрица покрытия decline-кодов

| responseCode | Причина                           |      Тест-кейс      |
|:------------:|-----------------------------------|:-------------------:|
|      00      | Approved                          | TC-06, TC-14, TC-16 |
|      05      | Do Not Honor (BLOCKED/INACTIVE)   |        TC-09        |
|      14      | Invalid Card Number               |    TC-08, TC-17     |
|      51      | Insufficient Funds                |    TC-07, TC-17     |
|      54      | Expired Card                      |    TC-10, TC-17     |
|      61      | Exceeds Amount Limit              |        TC-17        |
|      96      | System Error / Reservation Failed |        TC-17        |

---

## Матрица покрытия функциональности

| Функция                                  | Тест-кейсы |           HTTP assert            |                  DB assert                  |
|------------------------------------------|:----------:|:--------------------------------:|:-------------------------------------------:|
| Health-check всех сервисов               |   TC-01    |           ✅ status=200           |                      —                      |
| Создание карты                           |   TC-02    |    ✅ status=201, body fields     |                ✅ cards table                |
| Массовая генерация карт                  |   TC-03    |    ✅ status=201, count, Luhn     |       ✅ cards count, BIN distribution       |
| Получение карты по PAN                   |   TC-04    |         ✅ status=200/404         |              ✅ cards existence              |
| PATCH и DELETE карты                     |   TC-05    |     ✅ status=204, get verify     |         ✅ cards status transitions          |
| Полный цикл транзакции                   |   TC-06    | ✅ status=APPROVED, rrn, authCode |      ✅ cards balance, transactions row      |
| Decline: недостаточно средств            |   TC-07    |    ✅ status=DECLINED, code=51    | ✅ cards balance unchanged, transactions row |
| Decline: невалидный PAN                  |   TC-08    |    ✅ status=DECLINED, code=14    |             ✅ transactions row              |
| Decline: заблокированная карта           |   TC-09    |    ✅ status=DECLINED, code=05    |  ✅ cards status=BLOCKED, transactions row   |
| Decline: истёкшая карта                  |   TC-10    |    ✅ status=DECLINED, code=54    |      ✅ cards expiry, transactions row       |
| Gateway валидация                        |   TC-11    |           ✅ status=400           |       ✅ transactions count unchanged        |
| Gateway проксирование (cards)            |   TC-12    |        ✅ proxy == direct         |                 ✅ cards row                 |
| Gateway проксирование (search/dashboard) |   TC-13    |        ✅ proxy == direct         |         ✅ transactions count match          |
| Маршрутизация по BIN                     |   TC-14    |        ✅ issuerId per BIN        |          ✅ transactions.issuer_id           |
| Резервирование средств                   |   TC-15    |       ✅ balance decrement        |          ✅ cards.available_balance          |
| Terminal Simulator (normal)              |   TC-16    |       ✅ totalSubmitted=50        |          ✅ transactions count, MCC          |
| Terminal Simulator (declines_test)       |   TC-17    |           ✅ declined>0           |    ✅ transactions decline_reason variety    |
| Merchant Simulator (grocery)             |   TC-18    |       ✅ totalSubmitted=20        |      ✅ transactions MCC, acquiring_fee      |
| Поиск + фильтры + пагинация              |   TC-19    |   ✅ filter results, pagination   |          ✅ COUNT match, AND-logic           |
| Dashboard статистика                     |   TC-20    |       ✅ consistency checks       |           ✅ SUM, COUNT, AVG match           |

---

## Структура проекта E2E-тестов

```text
services/e2e-tests/
├── src/
│   ├── main/java/com/processing/e2e/
│   ├── test/java/com/processing/e2e/
│   │   ├── E2EBaseTest.java         # Базовый класс (URL-ы, DB, HTTP)
│   │   └── tests/
│   │       ├── HealthCheckTest.java
│   │       └── ...
│   │   └── utility/
│   │       ├── HttpUtils.java       # HTTP-хелперы (REST Assured)
│   │       └── DBUtils.java         # JDBC-хелперы
│   └── test/resources/
│       └── testng.xml                # TestNG suite
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Асинхронные сценарии (RabbitMQ + bin-lookup)

### Стек (дополнительно)

| Категория | Технология | Maven-координата | Описание |
|-----------|------------|-----------------|----------|
| **Awaitility** | Awaitility 4.2.2 | `org.awaitility:awaitility:4.2.2` | Ожидание eventual consistency в async-тестах |

---

### Покрытие по новым сервисам

| Сервис | Тест-кейсы |
|--------|:---:|
| Bin Lookup (8096) | TC-21 |
| Switch → RabbitMQ → Transaction Logger | TC-22, TC-23 |
| Outbox → RabbitMQ → Notification Service | TC-24, TC-25 |

---

## TC-21 — Bin Lookup: нормальный ответ, timeout, ошибки

**Цель:** Проверить bin-lookup API: успешный ответ, таймаут, ошибки, неизвестный BIN.

**Шаги и assertions:**

1. **HTTP GET** `http://localhost:8096/api/bin/400000`
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.issuerId == "ISS001"`, `$.bankName` != null, `$.countryCode` != null

2. **HTTP GET** `http://localhost:8096/api/bin/400000?fail=true`
    - **HTTP assert:** `statusCode == 500`
    - **Body assert:** `$.error` содержит "simulated failure"

3. **HTTP GET** `http://localhost:8096/api/bin/400000?delay=10000`
    - **HTTP assert:** timeout через 5 секунд (клиентский read timeout)

4. **HTTP GET** `http://localhost:8096/api/bin/999999`
    - **HTTP assert:** `statusCode == 404`
    - **Body assert:** `$.error` содержит "BIN not found"

---

## TC-22 — Async: Switch → RabbitMQ → Logger (eventual consistency)

**Цель:** Проверить асинхронную публикацию транзакции через RabbitMQ и её сохранение в
Transaction Logger с eventual consistency.

**Предусловие:** RabbitMQ запущен, очередь `transaction-log` создана.

**Шаги и assertions:**

0. **Подготовка:** Создать карту через `POST /api/cards`. Сохранить `pan`. Запомнить `countBefore` = `COUNT(*) FROM transactions`.

1. **HTTP POST** `http://localhost:8080/api/transactions`
    - Body: валидный запрос с `pan` из шага 0
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.status` == `"APPROVED"` или `"DECLINED"`
    - Сохранить `stan`, `rrn` из ответа

2. **Awaitility (eventual consistency):**
    - `await().atMost(10, SECONDS).until(() -> { ... })`
    - Проверка через `GET http://localhost:8088/api/transactions/search?stan={stan}`
    - **Assert:** транзакция найдена в Logger (HTTP 200, `total >= 1`)

3. **DB assert** (transactions):
    - `SELECT COUNT(*) FROM transactions WHERE stan = '{stan}'`
    - `count == 1`
    - Сравнение: поля `status`, `amount`, `rrn` совпадают с HTTP-ответом из шага 1

---

## TC-23 — Async: Очередь недоступна → rollback + DLQ

**Цель:** Проверить поведение при недоступности RabbitMQ (rollback для APPROVED).

**Шаги и assertions:**

1. **Остановить RabbitMQ** (docker stop rabbitmq)

2. **HTTP POST** `http://localhost:8080/api/transactions` — с APPROVED-картой
    - **HTTP assert:** `statusCode == 200`
    - **Body assert:** `$.status == "DECLINED"`, `$.responseCode == "96"` (System Error)
    - **Логика:** Switch получает отказ в Publisher Confirm → откат резервирования → возврат 96

3. **DB assert** (cards):
    - `SELECT available_balance FROM cards WHERE pan = '{pan}'`
    - Баланс **не** изменился (средства разрезервированы)

4. **Запустить RabbitMQ** (docker start rabbitmq)

5. Повторить транзакцию — должна пройти APPROVED

---

## TC-24 — Outbox: PENDING → PROCESSED + Notification Service consume

**Цель:** Проверить outbox-паттерн: создание карты → событие PENDING → публикация → PROCESSED →
Notification Service сохраняет уведомление.

**Шаги и assertions:**

0. **Подготовка:** Запомнить `notificationCountBefore` = через `GET http://localhost:8097/api/notifications`.

1. **HTTP POST** `http://localhost:8080/api/cards` — создать карту
    - **HTTP assert:** `statusCode == 201`
    - Сохранить `pan` и `id`

2. **Awaitility (outbox processed):**
    - `await().atMost(10, SECONDS).until(() -> { ... })`
    - Проверка через `GET http://localhost:8097/api/notifications`
    - **Assert:** количество уведомлений увеличилось (`size > notificationCountBefore`)

3. **Body assert** (notification):
    - `$.notifications[*].eventType` содержит "CARD_CREATED"
    - `$.notifications[*].cardId` == id созданной карты

---

## TC-25 — Outbox: RabbitMQ недоступен → retry → recovery

**Цель:** Проверить устойчивость outbox-процессора при недоступности RabbitMQ.

**Шаги и assertions:**

1. **Остановить RabbitMQ** (docker stop rabbitmq)

2. **HTTP POST** `http://localhost:8080/api/cards` — создать карту
    - **HTTP assert:** `statusCode == 201`

3. **Подождать 5 секунд** — outbox-процессор должен зафиксировать ошибку публикации

4. **Запустить RabbitMQ** (docker start rabbitmq)

5. **Awaitility (recovery):**
    - `await().atMost(15, SECONDS).until(() -> { ... })`
    - Проверка через `GET http://localhost:8097/api/notifications`
    - **Assert:** уведомление о создании карты доставлено

---

## Обновлённая матрица покрытия

### Асинхронные тест-кейсы

| Функция | Тест-кейсы | HTTP assert | DB assert |
|----------|:---:|:---:|:---:|
| Bin Lookup: нормальный ответ | TC-21 | ✅ status=200, issuerId | — |
| Bin Lookup: timeout / 500 / 404 | TC-21 | ✅ status=500/404 | — |
| Async Switch→Logger (eventual consistency) | TC-22 | ✅ awaitility + Logger search | ✅ transactions row |
| Async: очередь недоступна → rollback | TC-23 | ✅ DECLINED, responseCode=96 | ✅ cards balance unchanged |
| Outbox: PENDING→PROCESSED | TC-24 | ✅ awaitility + notifications | ✅ card_notifications |
| Outbox: RMQ недоступен → recovery | TC-25 | ✅ awaitility after recovery | ✅ card_notifications |

### Обновлённая структура проекта E2E-тестов

```text
services/e2e-tests/
├── src/
│   ├── main/java/com/processing/e2e/
│   ├── test/java/com/processing/e2e/
│   │   ├── E2EBaseTest.java         # Базовый класс (URL-ы, DB, HTTP)
│   │   └── tests/
│   │       ├── HealthCheckTest.java
│   │       ├── BinLookupE2eTest.java          # TC-21
│   │       ├── RabbitMQAsyncE2eTest.java       # TC-22, TC-23
│   │       ├── NotificationServiceE2eTest.java # TC-24, TC-25
│   │       └── ...
│   │   └── utility/
│   │       ├── HttpUtils.java       # HTTP-хелперы (REST Assured)
│   │       └── DBUtils.java         # JDBC-хелперы
│   └── test/resources/
│       └── testng.xml                # TestNG suite
├── Dockerfile
├── pom.xml
└── README.md
```
