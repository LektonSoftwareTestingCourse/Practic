# API-тесты СМП — отчёт по практике 5

**Область:** синхронные HTTP-контракты публичных маршрутов Gateway и внутренние HTTP-вызовы Switch, Authorization и Transaction Logger, участвующие в цепочке обработки транзакции.

**Стек:** Java 21, Maven, JUnit 5, REST-assured 5.4.0, Hamcrest 2.2, Awaitility 4.2.2, JSON Schema Validator, Docker Compose.

**Объём набора:** 9 тестовых классов, 63 тестовых метода, в том числе 6 параметризованных методов; данные создаются API-запросами и не требуют прямого доступа к БД.


## 1. Покрытие эндпоинтов

| Эндпоинт | Проверяемые ответы в тестах | Класс тестов | Непроверенные / условные ответы |
|---|---|---|---|
| `GET /health` (Gateway) | 200, JSON Schema, `Content-Type` | `GatewayHealthApiTest` | 503 при недоступном downstream — нужен fault injection |
| `POST /api/cards` | 201, 400 | `CardsApiTest` | — |
| `GET /api/cards` | 200, 400; фильтры `bin`, `status`, `issuerId`, `startDate`, `endDate`, `limit`, `offset`; пустая выборка | `CardsApiTest` | — |
| `GET /api/cards/{pan}` | 200, 400, 404 | `CardsApiTest` | — |
| `PATCH /api/cards/{pan}` | 200, 400, 404 | `CardsApiTest` | — |
| `DELETE /api/cards/{pan}` | 204, 400, 404 | `CardsApiTest` | — |
| `POST /api/cards/{pan}/reserve` | 200, 400, 402, 404, 409 | `CardsApiTest` | — |
| `POST /api/cards/{pan}/rollback` | 200, 400, 404, 409 | `CardsApiTest` | — |
| `POST /api/cards/generate` | 201, 400 | `CardsApiTest` | — |
| `POST /api/cards/bulk-update` | 200 | `CardsApiTest` | Endpoint есть в контроллере, но отсутствует в OpenAPI-файле |
| `POST /api/transactions` (Gateway → Switch) | 200 для APPROVED и DECLINED; 400 для ошибок Gateway validation | `TransactionsApiTest` | 429 rate limit и 503 недоступность Switch — нужны отдельные fault/load сценарии |
| `GET /api/transactions/search` | 200, 400; PAN/status/date/merchant/issuer/MCC/declineReason/limit/offset, пустая выборка | `TransactionsApiTest` | — |
| `GET /api/transactions/export` | 200 CSV и заголовки; 400 | `TransactionsApiTest` | — |
| `GET /api/dashboard/stats` | 200, взаимная согласованность счётчиков | `DashboardApiTest` | 5xx при отказе хранилища здесь не моделировались |
| `GET /api/dashboard/recent` | 200, 400 при неверном `limit` | `DashboardApiTest` | — |
| `GET /api/dashboard/charts` | 200, 400 для неподдерживаемой гранулярности и обратного диапазона | `DashboardApiTest` | — |
| `POST /api/simulator/terminal/run` | 200, 400 | `SimulatorApiTest` | 404/5xx при отсутствии карт или недоступном Gateway не моделировались |
| `POST /api/simulator/terminal/start-continuous` | 200, 400 | `SimulatorApiTest` | Реальный путь контроллера; отличается от пути в OpenAPI |
| `POST /api/simulator/terminal/stop` | 200 | `SimulatorApiTest` | Реальный путь контроллера; отличается от пути в OpenAPI |
| `POST /api/simulator/merchant/run` | 200, 400, 404 | `SimulatorApiTest`, `MerchantSimulatorNegativeApiTest` | 500/502 при проблемах БД или внешнего сервиса не моделировались |
| `GET /api/simulator/merchants` | 200 | `SimulatorApiTest` | 500 при проблеме БД не моделировался |
| `POST /api/simulator/merchant/fee` | 200, 400, 404 | `SimulatorApiTest`, `MerchantSimulatorNegativeApiTest` | 500 при проблеме БД не моделировался |
| `POST /api/internal/route` (Switch) | 200 | `SwitchInternalApiTest` | 503 при недоступности Authorization/Logger — нужен fault injection |
| `POST /api/internal/authorize` (Authorization) | 200, 403, 404, 422, 400 | `AuthorizationInternalApiTest` | 503 при недоступности Card Management — нужен fault injection |
| `POST /api/internal/rollback` (Authorization) | 200, 404, 409 | `AuthorizationInternalApiTest` | 400/503 при некорректном запросе или недоступности Card Management не моделировались |
| `POST /api/internal/log` (Transaction Logger) | 201, 200 (идемпотентный повтор), 400, 409 | `TransactionLoggerInternalApiTest` | 503 при недоступности PostgreSQL — нужен fault injection |

WebSocket `/ws/transactions` в этот набор не включён: задание ограничивает работу синхронными HTTP-контрактами REST API.

## 2. Бизнес-отказы авторизации

Код и причина проверяются одновременно. Это важно, поскольку разные причины могут иметь общий ISO response code.

| Исход | Код | Причина | Путь достижения | Подготовка данных |
|---|---:|---|---|---|
| Карта заблокирована | `05` | `CARD_BLOCKED` | Статус карты | Создать карту через `POST /api/cards`, затем `PATCH` со статусом `BLOCKED` |
| Карта неактивна | `05` | `CARD_INACTIVE` | Статус карты | Создать карту и установить `INACTIVE` |
| Карта просрочена | `54` | `CARD_EXPIRED` | Статус карты | Создать карту и установить `EXPIRED` |
| Карта просрочена | `54` | `CARD_EXPIRED` | Дата операции после срока действия | Оставить карту `ACTIVE`, передать `transmissionDateTime` в 2100 году |
| Недостаточно средств | `51` | `INSUFFICIENT_FUNDS` | Сумма больше остатка | Создать карту с остатком 100, запросить 200 |
| Превышен дневной лимит | `61` | `EXCEEDS_AMOUNT_LIMIT` | Сумма больше лимита | Создать карту с дневным лимитом 100 и достаточным балансом, запросить 101 |
| Карта не найдена | `14` | `CARD_NOT_FOUND` | PAN корректной длины и контрольной суммы отсутствует в системе | Сгенерировать уникальный неизвестный PAN с корректной Luhn-суммой |
| Повторный откат | `05` | `ALREADY_ROLLED_BACK` | Повторить rollback с тем же RRN | Сначала одобрить запрос, затем выполнить rollback дважды |

Для `CARD_EXPIRED` проверяются оба пути: поле статуса карты и дата запроса. Для `CARD_BLOCKED` и `CARD_INACTIVE` проверяются одинаковые коды при разных доменных причинах.

## 3. Валидация и классы эквивалентности

| Область | Поле / параметр | Проверяемые классы и границы | Где проверяется |
|---|---|---|---|
| Card Management: создание | `bin` | Некорректная длина / нецифровой BIN | `CardsApiTest` |
| Card Management: создание | `cardholderName` | Строчные символы вместо разрешённых заглавных | `CardsApiTest` |
| Card Management: создание | `currencyCode` | Длина и только цифры | `CardsApiTest` |
| Card Management: создание | `dailyLimit` | Отрицательное значение | `CardsApiTest` |
| Card Management: PAN | `pan` | Неверный формат против корректно сформированного, но неизвестного PAN | `CardsApiTest` |
| Card Management: список | `limit`, `offset` | Ноль, отрицательное значение; допустимые значения в позитивном тесте | `CardsApiTest` |
| Card Management: список | `bin`, `issuerId`, `status`, `startDate` | Неверный формат, enum и дата | `CardsApiTest` |
| Card Management: PATCH | `dailyLimit` | Отрицательное значение | `CardsApiTest` |
| Card Management: reserve/rollback | `amount`, `rrn`, PAN | Нулевой/некорректный параметр и неизвестная карта/операция | `CardsApiTest` |
| Card Management: генерация | `count`, `bins` | Ноль, пустой список, некорректный BIN | `CardsApiTest` |
| Gateway: транзакция | `mti`, `pan`, `amount`, `currencyCode`, `mcc` | Неверное значение по одному полю за запрос, включая отрицательную и отсутствующую сумму | `TransactionsApiTest` |
| Gateway: транзакция | обязательные поля | Отсутствуют `stan`, `processingCode`, `transmissionDateTime`, `terminalId`, `merchantId`, `acquirerId` | `TransactionsApiTest` |
| Logger: поиск | `limit`, `offset`, `dateFrom`, `dateTo` | Ноль/отрицательное/выход за максимум, отрицательный offset, обратный диапазон дат | `TransactionsApiTest` |
| Dashboard | `limit`, `granularity`, `from`, `to` | Недопустимая граница, неизвестная гранулярность, обратный диапазон | `DashboardApiTest` |
| Terminal Simulator | `count`, `tps` | Ноль для количества и TPS | `SimulatorApiTest` |
| Merchant Simulator | `count`, `scenario`, `mccCodes`, fee fields | Ноль, неизвестный MCC, отсутствующие обязательные поля | `MerchantSimulatorNegativeApiTest` |

Некоторые ограничения DTO, которые не создают отдельный HTTP-тест в этой итерации (например, полный набор ограничений всех полей внутреннего `TransactionRequest` Logger), оставлены в списке дальнейшего расширения.

## 4. Расхождения спецификации и реализации

| Декларировано | Наблюдается в исходном коде | Источник декларации / реализации | Классификация |
|---|---|---|---|
| Terminal Simulator: `POST /api/simulator/terminal/start` и `GET /api/simulator/terminal/status` | Контроллер реализует `POST /api/simulator/terminal/start-continuous` и `POST /api/simulator/terminal/stop`; endpoint `status` отсутствует | `docs/api/openapi.yaml`; `services/terminal-simulator/.../TerminalSimulatorController.java` | Несовпадение контракта и реализации; тесты используют пути контроллера |
| Ответ авторизации содержит MTI `0110` | `AuthorizationResponse.approved()` возвращает `request.mti()`; для входного `0100` ответ также содержит `0100` | `docs/api/openapi.yaml` schema `AuthorizationResponse`; `services/common/.../AuthorizationResponse.java` | Несовпадение формата ответа; зафиксировать как дефект или явно изменить контракт |
| OpenAPI описывает `POST /api/internal/authorize` преимущественно как ответ 200 | `AuthControllerImpl` возвращает 403, 404, 422 и 503 для соответствующих бизнес- и технических исходов, а также 400 для остальных отказов | `docs/api/openapi.yaml`; `services/authorization/.../AuthControllerImpl.java` | Неполное описание статус-кодов в OpenAPI |
| `docs/api-spec.md` описывает logger `/api/internal/log` как создание с ответом 201 | Реализация возвращает 201 для новой записи, 200 для идентичного повторного сообщения и конфликт 409 обрабатывается при совпавшем id с отличающимися данными | `docs/api-spec.md`; `InternalTransactionController.java`, `TransactionService.java` | Старая спецификация неполная; OpenAPI содержит 200/201, конфликт нужно уточнить |
| OpenAPI не перечисляет `POST /api/simulator/merchant/fee` | Endpoint реализован в `SimulationController` и доступен через Gateway route `/api/simulator/merchant/**` | `docs/api/openapi.yaml`; `SimulationController.java`, Gateway `application.yml` | Недокументированный endpoint |
| OpenAPI не перечисляет `POST /api/cards/bulk-update` | Endpoint реализован в `CardController` и доступен через Gateway route `/api/cards/**` | `docs/api/openapi.yaml`; `CardController.java`, Gateway `application.yml` | Недокументированный endpoint |
| В старой E2E-спецификации/примерных запросах встречается фильтр `stan` для `/api/transactions/search` | `TransactionFilter` не содержит `stan`; реализованы `pan`, `status`, `dateFrom`, `dateTo`, `merchantId`, `issuerId`, `mcc`, `declineReason`, `limit`, `offset` | `docs/api-spec.md` и E2E-код; `TransactionFilter.java` | Пример контракта не соответствует модели фильтра |
| Модель карточки перечисляет статусы `ACTIVE/INACTIVE/BLOCKED/EXPIRED` | `DELETE /api/cards/{pan}` переводит запись в `DELETED`; `CardStatusMapper` не отображает `DELETED` в `CardModelStatus` для обычной выдачи DTO | `CardController.java`; `CardStatusMapper.java`; `CardModelStatus.java` | Кандидат на дефект при чтении удалённой карты; поведение после DELETE требует отдельной проверки на поднятом стеке |

Расхождения не замалчиваются подстройкой тестов под желаемый ответ. Для исправления нужно согласовать, что является источником истины: OpenAPI, старый Markdown-контракт или поведение сервиса.

## 5. Обоснование достаточности набора

Набор применяет несколько техник тест-дизайна:

1. **Классы эквивалентности:** корректный PAN, некорректный PAN и синтаксически корректный, но отсутствующий PAN; карта с достаточным балансом и карта с недостаточным балансом; валидный и неизвестный MCC.
2. **Граничные значения:** нулевой и отрицательный amount/limit/TPS, максимальная граница пагинации, пустой результат, повторная операция.
3. **Комбинирование параметров:** поиск транзакции проверяет одновременное применение фильтров через AND; пагинация проверяет ограничение размера массива и разные записи при `offset=0` и `offset=1` для списков карт и транзакций.
4. **Уникальные данные:** тесты генерируют свои PAN через API, STAN и UUID; не используют заранее созданную тестовую запись и не обращаются к БД напрямую.
5. **Асинхронность:** `Awaitility` ожидает появление записи в поиске после транзакции; отдельная проверка ожидает, что невалидный запрос Gateway не создаёт запись.
6. **Контракт ответа:** проверяются статус, доменный код, причина, тело, JSON Schema, форматы RRN/authCode, `null` для необязательного `declineReason`, CSV и заголовки ответа.

Оставлены вне проверок этой части: WebSocket, прямые API сервисов BIN Lookup/Notification, отказоустойчивость при отключении зависимостей, rate limit 429 в контролируемом нагрузочном сценарии, 5xx PostgreSQL/RabbitMQ и полные негативные варианты каждого поля внутренних DTO. Эти сценарии требуют отдельной fault-injection/интеграционной настройки и не должны имитироваться фиктивным ожиданием.

## 6. Доказательства способности тестов падать (мутационная проверка)

Минимум три мутации подготовлены как сценарии ручной проверки, но **не запускались** в среде подготовки, так как здесь нет Docker/Maven.

| Тест | Намеренное изменение | Ожидаемый результат |
|---|---|---|
| `gatewayRejectsOneInvalidFieldAndReturnsGatewayError` | В `TransactionRequestValidator` убрать проверку `mti != 0100` | Соответствующий параметризованный кейс должен стать красным: вместо 400 запрос пройдёт дальше |
| `cardStateProducesItsOwnBusinessDecline` | Для статуса `BLOCKED` вернуть `CARD_INACTIVE` вместо `CARD_BLOCKED` | Параметр `BLOCKED` должен упасть по проверке `declineReason`, даже если code остаётся 05 |
| `repeatedIdenticalMessageIsIdempotentAndChangedPayloadConflicts` | Убрать сравнение существующей записи с payload при совпавшем UUID / всегда создавать запись | Проверка статуса повтора или конфликта должна упасть |
| `gatewayAcceptsTransactionAndLoggerEventuallyMakesItSearchable` | Отключить сохранение сообщения consumer-ом Logger | Awaitility не дождётся записи и тест завершится ошибкой по таймауту |

После каждой мутации нужно запускать только соответствующий тест, зафиксировать красный результат и немедленно вернуть исходный код. Нельзя коммитить мутацию в рабочую ветку.

## 7. Порядок запуска

### Windows PowerShell

```powershell
Copy-Item .env.example .env   # только если .env ещё нет
$env:PRACTICE5_REPETITIONS = 3
.\scripts\run-practice-5.ps1
```



Скрипт запускает Compose с тестовым override, ожидает HTTP 200 от агрегированного `/health` и отдельно ждёт `/health` терминального и merchant-симуляторов (они не входят в агрегированную проверку Gateway), выполняет профиль `api-tests` три раза и останавливает контейнеры. При ошибке печатает последние логи. Чтобы оставить контейнеры включёнными для диагностики, установите `KEEP_SMP_STACK=1`.

Отдельный запуск тестов по уже поднятому стеку:

```bash
mvn -B -f services/pom.xml -P api-tests verify
```

