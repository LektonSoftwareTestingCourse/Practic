# Чек-листы само-приёмки — СМП

>
> **Как использовать:** перед каждым ревью куратора (дни 5, 10, 15) каждая группа самостоятельно проходит чек-лист. Отмечайте пройденные пункты `[x]`. Если что-то не работает — чините до ревью. Куратор проверяет только красные пункты.

---

## Ревью №1 — День 5 (Фундамент)

### Все сервисы (общее)

- [ ] `docker compose up -d` поднимает все 8 сервисов без ошибок
- [ ] `curl http://localhost:{port}/health` каждого сервиса возвращает HTTP 200
- [ ] Все переменные окружения вынесены в `.env` (ни одного хардкода хостов/портов)
- [ ] Dockerfile собирается: `docker build -t {service} .`
- [ ] Swagger UI / OpenAPI docs доступны на `http://localhost:{port}/docs`
- [ ] Код проходит линтер:
  - Java: `mvn checkstyle:check`
  - Go: `golangci-lint run` или `go vet ./...`
  - Python: `flake8 --max-line-length=120`
  - TypeScript: `npm run lint`
- [ ] README.md сервиса заполнен (см. [шаблон](readme-template.md))

### DevOps

- [ ] `docker-compose.yml` содержит все 8 сервисов + PostgreSQL
- [ ] Настроена общая сеть (все сервисы видят друг друга по DNS-именам)
- [ ] `.env` файл со всеми переменными окружения
- [ ] `.github/workflows/ci.yml` настроен: линтер, сборка, тесты
- [ ] Структура репозитория соответствует общему README
- [ ] OpenAPI-спецификация собрана и зафиксирована

### Gateway

- [ ] `GET /health` возвращает статус Gateway и всех downstream-сервисов
- [ ] `POST /api/transactions` принимает запрос, валидирует и возвращает ответ
- [ ] Валидация: все обязательные поля проверяются (pan=16 цифр, amount>0 и т.д.)
- [ ] Rate limiting: не более 100 запросов/сек на `/api/transactions`

### Switch

- [ ] `GET /health` возвращает статус Switch и Authorization
- [ ] `POST /api/internal/route` принимает запрос, извлекает BIN, определяет issuerId
- [ ] Таблица BIN → issuerId содержит 5 записей

### Authorization

- [ ] `GET /health` возвращает статус Authorization и Card Management
- [ ] `POST /api/internal/authorize` принимает запрос (пока может возвращать всегда APPROVED)

### Card Management

- [ ] `POST /api/cards/generate` генерирует 100+ карт с 5 BIN
- [ ] `GET /api/cards/{pan}` возвращает карту по PAN
- [ ] `GET /api/cards` возвращает список карт с пагинацией
- [ ] PAN проходит проверку алгоритмом Луна
- [ ] Карты хранятся в PostgreSQL (таблица `cards`)

### Terminal Simulator

- [ ] `GET /health` работает
- [ ] `POST /api/simulator/terminal/run` с `{count: 1, scenario: "normal"}` отправляет 1 транзакцию в Gateway

### Merchant + Acquirer Simulator

- [ ] `GET /health` работает
- [ ] Список из 20+ мерчантов с MCC-кодами загружен
- [ ] `POST /api/simulator/merchant/run` с `{count: 1}` отправляет 1 транзакцию в Gateway

### Transaction Logger

- [ ] `GET /health` работает
- [ ] `POST /api/internal/log` принимает транзакцию и сохраняет в PostgreSQL

### Web Dashboard

- [ ] `GET /health` (или доступность на порту 3000)
- [ ] Каркас React-приложения запускается
- [ ] KPI-карточки отображаются (пока с хардкод-данными)

---

## Ревью №2 — День 10 (Сквозной прогон)

### Все сервисы

- [ ] `scripts/smoke-test.sh` (или `.ps1`) проходит без ошибок
- [ ] CI/CD зелёный (линтер + сборка + unit-тесты)
- [ ] Unit-тестов: минимум 1 на сервис

### Gateway

- [ ] `POST /api/transactions` → Switch → Authorization → CMS → Logger — полный цикл до ответа
- [ ] Проксирование работает: `/api/cards/**`, `/api/transactions/search`, `/api/dashboard/**`
- [ ] HTTP 503 при недоступности downstream-сервиса

### Switch

- [ ] Маршрутизация по BIN: 5 BIN → 5 разных issuerId
- [ ] Вызов Authorization Service — реальный, не заглушка
- [ ] Синхронная отправка транзакции в Logger (`POST /api/internal/log`)
- [ ] Неизвестный BIN → DECLINED, responseCode="14"
- [ ] Logger недоступен → транзакция не блокируется (graceful degradation)

### Authorization

- [ ] Проверка статуса карты (ACTIVE/INACTIVE/BLOCKED/EXPIRED)
- [ ] Проверка дневного лимита
- [ ] Проверка месячного лимита
- [ ] Проверка доступного баланса
- [ ] Резервирование средств через CMS при APPROVED
- [ ] Генерация RRN (12 цифр) и authCode (6 символов)
- [ ] Все decline-сценарии: 14, 54, 61, 51, 05

### Card Management

- [ ] CRUD + PATCH: create, read, update (PATCH), delete (мягкое: status=DELETED)
- [ ] `GET /api/cards` — список с пагинацией и фильтрацией по статусу/BIN
- [ ] `POST /api/cards/{pan}/reserve` — списание средств
- [ ] 100+ карт сгенерированы с разными статусами (ACTIVE 95%, INACTIVE 3%, BLOCKED 2%)

### Terminal Simulator

- [ ] Сценарий `normal`: 50 транзакций, все ACTIVE-карты, MCC=5411
- [ ] Сценарий `mixed`: 70/15/10/5 распределение
- [ ] Сценарий `declines_test`: все 5 категорий (несуществующий PAN, BLOCKED, недостаточно средств, лимит, нормальные)

### Merchant + Acquirer Simulator

- [ ] Сценарий `grocery`: 50 транзакций, MCC=5411
- [ ] Сценарий `restaurant`: 50 транзакций, MCC=5812/5814
- [ ] Расчёт комиссии эквайрера для каждой транзакции

### Transaction Logger

- [ ] `POST /api/internal/log` — транзакция сохраняется в БД
- [ ] `GET /api/transactions/search` — поиск с фильтрацией (pan, status, dateFrom/dateTo)
- [ ] `GET /api/dashboard/stats` — агрегированная статистика
- [ ] `GET /api/dashboard/recent?limit=20` — последние N транзакций
- [ ] WebSocket `/ws/transactions` — эндпоинт доступен

### Web Dashboard

- [ ] Таблица последних транзакций (REST)
- [ ] WebSocket-подключение, real-time обновление таблицы
- [ ] KPI-карточки с реальными данными

---

## Ревью №3 — День 15 (Стабилизация и Code Freeze)

### Все сервисы

- [ ] `scripts/smoke-test.sh` проходит без ошибок
- [ ] 500+ транзакций проходят через систему (любой симулятор)
- [ ] Все decline-коды покрыты тестовыми сценариями
- [ ] CI/CD зелёный по всем сервисам
- [ ] Unit-тестов: минимум 3 на сервис

### Gateway

- [ ] Rate limiting: 100 запросов/сек — при превышении HTTP 429
- [ ] Логирование: каждый запрос с method, path, status, responseTime

### Switch

- [ ] Retry при недоступности Authorization (3 попытки)
- [ ] Circuit breaker (бонус)

### Authorization

- [ ] Резервирование средств корректно обновляет availableBalance в CMS
- [ ] Все decline-коды возвращают правильный responseCode и declineReason

### Симуляторы (Terminal + Merchant)

- [ ] Все 5 сценариев Terminal Simulator работают
- [ ] Все 4 сценария Merchant Simulator работают
- [ ] Параллельная отправка транзакций

### Transaction Logger

- [ ] WebSocket: сообщения доходят до Dashboard в реальном времени
- [ ] Пагинация (limit/offset) работает
- [ ] Фильтрация по всем параметрам (AND-логика)

### Web Dashboard

- [ ] Линейный график потока транзакций (обновляется в реальном времени)
- [ ] Круговая диаграмма Approved vs Declined
- [ ] Фильтры: по статусу, дате, BIN, MCC
- [ ] Модальное окно с деталями транзакции
- [ ] Автореконнект WebSocket с exponential backoff
- [ ] PAN маскируется (первые 4 + последние 4 цифры)
- [ ] Индикатор состояния WebSocket-соединения

---

## Перед финальной защитой — День 19 (Сухая защита)

### Все сервисы

- [ ] `scripts/smoke-test.sh` проходит без ошибок
- [ ] README каждого сервиса заполнен и актуален
- [ ] Общий README репозитория актуален
- [ ] Скриншоты Dashboard сохранены
- [ ] Диаграммы архитектуры актуальны
- [ ] Презентация готова (демо-сценарий, слайды)
- [ ] Репетиция демо-сценария пройдена

### Демо-сценарий (рекомендация)

1. `docker compose up -d` — система стартует (10 сек)
2. `POST /api/cards/generate {count: 100}` — генерация тестовых карт
3. `POST /api/simulator/terminal/run {count: 100, scenario: "mixed"}` — запуск симуляции
4. Dashboard в реальном времени: таблица обновляется, графики двигаются
5. Показать детали одной транзакции (модальное окно)
6. Показать поиск с фильтрацией
7. Показать decline-сценарий (`declines_test`)
