# Тест-кейсы и анализ пирамиды

## Новые тест-кейсы

| ID | Связанное требование | Уровень пирамиды | Вид | Предусловие | Шаги | Ожидаемый результат | Источник |
|---|---|---|---|---|---|---|---|
| P3-01 | api-spec §3: валидация запроса, 400 | unit | негативный | экземпляр `TransactionRequestValidator` | Вызвать `validate()` с pan из 15 цифр | `TransactionValidationException("Field 'pan' must be exactly 16 digits")` | класс эквивалентности: невалидный PAN |
| P3-02 | rate-limit 100 req/s на клиента (.env, gateway) | unit | граничный | `InMemoryRateLimiter.forTesting(capacity=2, refill=1/s, testTicker)` | 3 вызова `allowRequest` подряд; сдвинуть тикер на 1с; ещё вызов | `true, true, false`; после 1с - `true` (refill) | граничные значения: ёмкость корзины |
| P3-03 | ТЗ Switch: неизвестный BIN → DECLINED 14 | unit | негативный | `RoutingService` с таблицей из `switch.application.yml` | `getIssuerIdByPan("999999…")` | `UnknownBinException` | класс эквивалентности: неизвестный BIN |
| P3-04 | ТЗ CMS: PAN по алгоритму Луна | unit | позитивный | `LuhnValidator` | `generatePan(bin)` для каждого из 5 BIN | 16 цифр, начинается с BIN, `isValid()==true` | класс эквивалентности: все валидные BIN |
| P3-05 | api-spec: RRN - 12 цифр, уникален | unit | позитивный | мок `LimitUsageRepository.fetchRrnBlock` | Два вызова `generateRRN()` | Две разные 12-значные строки формата `yDDD+8 цифр` | контракт |
| P3-06 | api-spec §3: mti обязан быть 0100 | api | негативный | запущенный gateway | `POST /api/transactions` с `mti="0200"` | 400, `error=VALIDATION_ERROR`, `message` про mti | контракт |
| P3-07 | api-spec §2: создание карты | api | позитивный | запущенный card-management | `POST /api/cards` (bin=400000, лимиты, баланс) | 201; `pan` 16 цифр и валиден по Луну; `status=ACTIVE`; expiry = +3 года | контракт |
| P3-08 | architecture.md, вариант A: eventual consistency | integration | позитивный | полный стек, сгенерирована карта | Отправить транзакцию через gateway; поллинговать `GET /api/transactions/search?stan=…` до 15с | Транзакция появляется; `status` и `rrn` совпадают с ответом gateway | сценарий: асинхронная доставка |
| P3-09 | architecture.md: сбой публикации → откат резерва | integration | негативный | стек поднят, `docker stop smp-rabbitmq` | `POST /api/transactions` на сумму < баланса | Ответ `DECLINED`, `responseCode=96`; `GET /api/cards/{pan}` - баланс **не изменился** | сценарий отказа: брокер недоступен |
| P3-10 | architecture.md, вариант C: outbox → notification | integration | позитивный | полный стек | `POST /api/cards`; поллинговать `GET :8097/api/notifications` до 12с | `totalElements` вырос; последнее событие `eventType=CardServiceCreationEvent` | сценарий: eventual consistency outbox |
| P3-11 | код: повтор при `DuplicateKeyException` в `AuthServiceImpl` | integration | позитивный | карта с большим балансом | Два параллельных `POST /api/internal/authorize` по одной карте в один день | Оба обработаны; в `limit_usage` одна строка `(pan, usage_date)`; `daily_amount` = сумме обеих | состояние БД: конкуренция |
| P3-12 | контракт `POST /api/cards/{pan}/reserve`: идемпотентность по RRN | integration | негативный | карта с балансом | `reserve(amount, rrn)` дважды с одним `rrn` | Первый - 200 (баланс уменьшен); второй - 409 (`ReservationAlreadyExistsException`), баланс не уменьшен повторно | контракт + состояние БД |
| P3-13 | api-spec §3: сквозная покупка | e2e | позитивный | полный стек, карта с балансом | Создать карту → `POST /api/transactions` → дождаться записи в search → проверить баланс | `APPROVED`, `code=00`; транзакция в журнале; баланс уменьшен ровно на `amount` | сквозной сценарий |
| P3-14 | ТЗ terminal-simulator: declines_test | e2e | смешанный | полный стек, пул карт | `POST /api/simulator/terminal/run {count:50, scenario:"declines_test"}` | В ответах присутствуют: 14, 51, 61, `CARD_BLOCKED` и APPROVED (~по 20%) | сквозной сценарий |
| P3-15 | tz/04: graceful degradation bin-lookup | integration | негативный | стек поднят | Авторизация по карте при `GET /api/bin/{bin}?fail=true` | Авторизация завершается по существу (баланс/лимиты), а не отказом из-за bin-lookup; `issuerId` заполнен | сценарий отказа внешнего API |

## Распределение тест-кейсов практики 2

| ID (пр. 2) | Название | Что проверяет тест | Уровень | Обоснование |
|---|---|---|---|---|
| AUTH-001 | Успешная авторизация | HTTP-контракт `authorize`: `200`, `APPROVED`, `00`, длины `authCode` и `RRN` | api | Проверяется контракт входа/выхода и маппинг ответа; CMS, Bin Lookup и БД заменяются стабами |
| AUTH-002 | Карта не найдена | `404` + `DECLINED` + `responseCode=14` | api | Проверяется контракт ошибки при ответе CMS `404`; бизнес-логика БД не нужна |
| AUTH-003 | Карта заблокирована | `403` + `DECLINED` + `05` + `CARD_BLOCKED` | api | Проверяется контракт внутреннего endpoint и маппинг `DeclineOutcome`; CMS стабится |
| AUTH-004 | Превышен дневной лимит | Ветка `dailyLimit OFF+`; `reserve` не вызывается | unit | Чистое решение `AuthServiceImpl` на моках; реальный SQL и HTTP не проверяются. Баланс в precondition должен быть не меньше 150001, иначе сработает 51 |
| AUTH-005 | Ровно на границе дневного лимита | `dailyLimit ON`: 150000 проходит | unit | Граница проверяется на уровне решения сервиса; БД и CMS замоканы |
| AUTH-006 | Недостаточно средств | `amount > availableBalance` → `51` | unit | Простое сравнение в сервисе; внешние зависимости не нужны |
| AUTH-007 | Истёк срок действия карты | `expiryDate` в прошлом → `54` | unit | Проверка даты и кода отказа изолирована в сервисе |
| AUTH-008 | Bin-lookup недоступен | Отказ Bin Lookup не влияет на выдачу | unit | Клиент замокан; проверяется graceful degradation и логирование, а не сетевой контракт |
| AUTH-009 | Card Management недоступен | `96` + `SERVICE_UNAVAILABLE` | api | Проверяется контракт ошибки на границе Auth/CMS; CMS стабится `503` |
| CM-001 | Создание карты с валидным BIN | `201`, 16 цифр, Luhn, `ACTIVE`, expiry +3 года | api | Проверяется HTTP-контракт создания и DTO-ответа; алгоритм PAN отдельно покрывается unit-тестами |
| CM-002 | Генерация тестовых карт | 100 карт, распределение по BIN, 95/3/2, ограничения БД | integration | Нужны реальные `saveAll`, уникальность PAN и проверка состояния БД |
| CM-003 | Генерация с `count=0` | `400` из-за `@Min(1)` | unit | Валидация DTO не требует БД и HTTP-контура |
| CM-004 | Резервирование `amount < balance` | Баланс уменьшается, создаётся `Reservation` | integration | Проверяются транзакция, pessimistic lock и фактическое состояние `cards`/`reservation` |
| CM-005 | Резервирование `amount > balance` | `402`, баланс не изменён | unit | Доменный guard `Card.startReservation` и `InsufficientFundsException` проверяются без БД |
| CM-006 | Получение несуществующей карты | `404 Not Found` | api | Контракт ошибки репозитория/контроллера; БД можно заменить стабом |
| CM-007 | Мягкое удаление карты | `DELETED`, `@SQLRestriction`, GET → `404` | integration | Нужно проверить фактическую запись статуса и поведение Hibernate-ограничения в БД |
| PW-001 | ACTIVE, ниже всех лимитов, valid, POS, grocery | Базовый happy-path решения | unit | Все параметры валидны; проверяется ветка `APPROVED` на моках, без сети и БД |
| PW-002 | ACTIVE, ровно на границах daily/monthly/balance | Три равенства одновременно; `UPSERT` и reserve | integration | Граничные равенства нужно проверить на реальном SQL `limit_usage` и состоянии резерва |
| PW-003 | ACTIVE, выше daily и monthly, ниже balance | Отказ по лимитам, отсутствие резерва | integration | Проверяются SQL-условия и отсутствие побочных изменений в БД |
| PW-004 | INACTIVE, всё below, valid, POS, travel | `05` + `CARD_INACTIVE` | unit | Ветка статуса в `AuthServiceImpl`; CMS и БД не нужны |
| PW-005 | BLOCKED, всё below, current_month, ATM, grocery | `05` + `CARD_BLOCKED` | unit | Ветка статуса в `AuthServiceImpl`; проверяется на моке карты |
| PW-006 | EXPIRED, всё below, expired, ecom, restaurant | `54` по статусу/дате | integration | Нужна карта из реального CMS/БД с `EXPIRED` и истёкшей датой |
| PW-007 | ACTIVE, expired карта, ниже balance, POS, travel | `54` несмотря на валидные лимиты | integration | Проверяется приоритет проверки `expiryDate` на реальной карте |
| PW-008 | ACTIVE, above daily, below monthly, above balance | Приоритет проверок: баланс раньше лимитов → `51` | integration | Нужно подтвердить фактический порядок условий и отсутствие резерва в БД |

## Анализ пропорций

### Сводная таблица

| Уровень | Старые (пр. 2) | Новые (часть б) |  Итого |      Доля |
|---|---:|----------------:|-------:|----------:|
| unit | 10 |               5 |     15 |     38.5% |
| api | 6 |               2 |      8 |     20.5% |
| integration | 8 |               6 |     14 |     35.9% |
| e2e | 0 |               2 |      2 |      5.1% |
| **api + integration (справочно)** | **14** |           **8** | **22** | **56.4%** |
| **Итого** | **24** |          **15** | **39** |  **100%** |

### Объяснение отклонений от 70/20/10

Пирамида сбалансирована и соответствует микросервисной архитектуре. Доминируют unit-тесты (38.5%) - в проекте много чистой изолированной логики 
(валидация, лимиты, Luhn). Доля integration (35.9%) высока из-за специфики: нативные SQL-запросы (upsertLimitUsage), pessimistic lock, 
RabbitMQ, Outbox-паттерн и DLQ требуют проверки на реальной инфраструктуре. Api-тесты (20.5%) покрывают контракты без БД, а E2E (5.1%) ограничен двумя
ключевыми сквозными сценариями. Перекоса в "ice-cream cone" нет