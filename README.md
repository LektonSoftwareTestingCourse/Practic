# СМП — Симулятор процессингового центра

**Образовательный проект для студенческой практики**

---

## Концепция

`СМП` (Система медленных платежей) — это упрощённый симулятор процессингового центра на микросервисной архитектуре. Проект эмулирует путь банковской транзакции от POS-терминала до авторизации на стороне эмитента и обратно — внутри замкнутой системы «своих» карт.

**Ключевые ограничения:**
- Только открытые технологии и фреймворки
- Нет доступа к реальным банковским API, платёжным сетям и внутренним системам компаний
- Все данные — синтетические (сгенерированные)
- ISO 8583 эмулируется упрощённым JSON-форматом
- Обрабатываются только «наши» тестовые карты из Card Management
- Взаимодействие между сервисами — гибридное: синхронное HTTP + асинхронный RabbitMQ
- Инфраструктура: PostgreSQL + RabbitMQ (message broker)

---

## Команда (15 человек)

| # | Роль | Чел. |
|---|------|:---:|
| 1 | DevOps | 1 |
| 2 | Gateway Service | 2 |
| 3 | Switch / Router | 2 |
| 4 | Authorization Service | 2 |
| 5 | Card Management Service | 2 |
| 6 | Terminal Simulator | 1 |
| 7 | Merchant + Acquirer Simulator | 1 |
| 8 | Transaction Logger | 2 |
| 9 | Web Dashboard | 2 |

Команда разделена на 3 рабочие группы по 5 студентов. Transaction Logger расщеплён между группами: один человек отвечает за приём транзакций от Switch (в группе Core), второй — за поиск, статистику и WebSocket (в группе Data).

---

## Технологический стек

| Слой | Технологии |
|------|-----------|
| Языки | Java 21, Go 1.22, Python 3.12, TypeScript 5.x |
| Фреймворки | Spring Boot 3, FastAPI, React 18 |
| База данных | PostgreSQL 16 |
| Message Broker | RabbitMQ 3.13 |
| Инфраструктура | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| API-документация | OpenAPI 3.0 (Swagger) |

Каждая команда выбирает язык и фреймворк самостоятельно в рамках утверждённого стека.

---

## Быстрый старт (для студентов)

> 📖 **Подробная инструкция по работе с Git:** см. [`docs/git-workflow.md`](docs/git-workflow.md)

```bash
# 1. Клонировать репозиторий
git clone https://github.com/LektonSoftwareTestingCourse/Practic.git
cd processing-practice

# 2. Запустить все сервисы (11 сервисов + PostgreSQL + RabbitMQ)
docker compose up -d

# 3. Проверить, что всё работает
curl http://localhost:8080/health

# 4. RabbitMQ Management UI доступен на http://localhost:15672 (логин smp/smp)

# 5. Проверить bin-lookup API (порт 8096)
curl http://localhost:8096/api/bin/400000

# 6. Проверить notification-service API (порт 8097)
curl http://localhost:8097/api/notifications

# 7. Сгенерировать тестовые карты (через Gateway)
curl -X POST http://localhost:8080/api/cards/generate \
  -H "Content-Type: application/json" \
  -d '{"count": 100, "bins": ["400000","400001","400002","400003","400004"]}'

# 8. Запустить симулятор терминалов (50 транзакций, через Gateway)
curl -X POST http://localhost:8080/api/simulator/terminal/run \
  -H "Content-Type: application/json" \
  -d '{"count": 50, "scenario": "normal"}'

# 9. Открыть дашборд
# http://localhost:3000
```

## Быстрая проверка для куратора (одной командой)

```bash
docker compose up -d && sleep 10 && ./scripts/smoke-test.sh
```

Если вывод заканчивается строкой `🎉 ALL CHECKS PASSED` — все 11 сервисов работают, сквозной прогон транзакций успешен, система полностью готова к ревью.


---

## Структура репозитория

```text
processing-practice/
├── README.md
├── .pre-commit-config.yaml         # Pre-commit хуки (линтеры)
├── docker-compose.yml              # DevOps (создаётся в процессе)
├── docs/
│   ├── architecture.md             # Архитектура и диаграммы
│   ├── api-spec.md                 # API-контракт (OpenAPI 3.0)
│   ├── program-overview.md         # Программа практики
│   ├── evaluation-criteria.md      # Критерии оценки
│   ├── checklists.md               # Чек-листы само-приёмки
│   ├── git-workflow.md             # Инструкция по работе с Git
│   └── readme-template.md          # Шаблон README для сервиса
├── scripts/
│   ├── smoke-test.sh               # Авто-приёмка (Linux/Mac)
│   └── smoke-test.ps1              # Авто-приёмка (Windows)
├── starters/                       # Starter kits (готовые скелеты)
│   ├── java/
│   ├── go/
│   ├── python/
│   └── typescript/
├── tz/                             # Технические задания по ролям
│   ├── 01-devops.md
│   ├── 02-gateway.md
│   ├── 03-switch.md
│   ├── 04-authorization.md
│   ├── 05-card-management.md
│   ├── 06-terminal-simulator.md
│   ├── 07-merchant-acquirer.md
│   ├── 08-transaction-logger.md
│   └── 09-web-dashboard.md
└── services/                       # ← Создаётся студентами в процессе
    ├── gateway/                    #    (скопировать из starters/)
    ├── switch/
    ├── authorization/
    ├── card-management/
    ├── terminal-simulator/
    ├── merchant-acquirer/
    ├── transaction-logger/
    ├── bin-lookup/                 #    Внешний API BIN (вариант B)
    ├── notification-service/       #    Уведомления о картах (вариант C)
    └── dashboard/
```

> **Внимание:** директория `services/` отсутствует в starter-репозитории — она создаётся студентами в процессе работы. Каждая команда копирует нужный starter kit из `starters/{язык}/` в `services/{свой-сервис}/`.

**Starter kits** содержат готовый health-check, Dockerfile, unit-тест и структуру папок. Скопируй нужный язык в `services/{твой-сервис}/` и начинай с работающего кода.

**Pre-commit хуки** (`pip install pre-commit && pre-commit install`) автоматически проверяют стиль кода при каждом коммите.

**Smoke-test** (`./scripts/smoke-test.sh`) — куратор и студенты проверяют систему одной командой.

Каждая директория в `services/` содержит:
- `src/` — исходный код
- `Dockerfile` — сборка контейнера
- `README.md` — документация сервиса (см. [шаблон](docs/readme-template.md))

---

## План практики (4 недели)

| Неделя | Тема | Ключевой результат |
|:---:|---|---|
| 1 | Фундамент и проектирование | Docker Compose запущен, все сервисы отвечают на health-check, API-контракты зафиксированы, тестовые данные сгенерированы |
| 2 | Первый сквозной прогон | Полный синхронный цикл транзакции: терминал → gateway → switch → authorization → card-management → logger. Кросс-командное ревью кода |
| 3 | Стабилизация и Dashboard | Dashboard с real-time графиками, продвинутые сценарии симуляторов, 500+ транзакций. Code freeze |
| 4 | Качество и защита | Рефакторинг, unit-тесты, документация, сухая защита, финальная презентация |

---

## Название проекта

**СМП — Система медленных платежей**

Название выбрано командой и зафиксировано куратором в первый день практики.

---

## Контакты

**Куратор практики:** [Имя Фамилия]
**Telegram-чат группы:** [ссылка]
**Репозиторий:** [GitHub URL]
