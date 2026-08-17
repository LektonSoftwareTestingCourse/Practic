# Terminal Simulator

## Назначение

Terminal Simulator генерирует транзакции по различным сценариям.

## Технологии

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3
- **База данных:** нет
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь                                       | Описание                                   |
|-------|--------------------------------------------|--------------------------------------------|
| GET | `/health`                                  | Health-check сервиса                       |
| POST | `/api/simulator/terminal/run`              | Запуск симулятора терминала                |
| POST | `/api/simulator/terminal/start-continuous` | Шлёт транзакции постоянно                  |
| POST | `/api/simulator/terminal/stop`             | Останавливает непрерывную отправку транзакций |

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

#### `POST /api/simulator/terminal/run`

**Тело запроса:**
```json
{
  "count": 50,
  "scenario": "normal"
}
```
scenario enum: normal, mixed, high_value, night_time, declines_test

normal — обычные покупки
* 100% транзакций с небольшими суммами (100–5 000 ₽)
* Все карты ACTIVE
* Дневное время (09:00–22:00)
* MCC: 5411 (супермаркеты)

mixed — смешанные транзакции
* 70% обычные покупки
* 15% крупные суммы (50 000 – 140 000 ₽)
* 10% на грани лимита (почти дневной лимит)
* 5% с заблокированными картами (ожидаем DECLINED)

high_value — только крупные суммы
* Суммы 100 000 – 500 000 ₽
* Проверка лимитов и баланса

night_time — ночные транзакции
* Время: 01:00–05:00
* Смесь малых и крупных сумм
* Крупные суммы могут отклоняться по ночным лимитам (если реализованы в Authorization)

declines_test — тест отклонений
* 20% с несуществующими PAN (ожидаем DECLINED, responseCode="14")
* 20% с заблокированными картами (ожидаем DECLINED, "CARD_BLOCKED")
* 20% с недостаточным балансом (ожидаем DECLINED, "51")
* 20% с превышением дневного лимита (ожидаем DECLINED, "61")
* 20% нормальные (ожидаем APPROVED)

**Ответ 200:**
```json
{
  "totalSubmitted": 50,
  "approved": 47,
  "declined": 3,
  "elapsedMs": 2300,
  "transactions": [ "массив AuthorizationResponse" ]
}
```

#### `POST /api/simulator/terminal/start-continuous`

**Тело запроса:**
```json
{
  "tps": 50,
  "transactionType": "NORMAL"
}
```

enum transactionType:

NORMAL :
*  100% транзакций с небольшими суммами (100–5 000 ₽)
*  Все карты ACTIVE
*  Дневное время (09:00–22:00)
*  MCC: 5411 (супермаркеты)

HIGH_VALUE :
*  Суммы 100 000 – 500 000 ₽

ALMOST_DAILY_LIMIT :
* на грани лимита (почти дневной лимит)

BLOCKED :
* с заблокированными картами

NO_MONEY :
* с недостаточным балансом

MORE_THAN_DAILY_LIMIT :
* с превышением дневного лимита

INVALID_PAN :
* с несуществующими PAN (ожидаем DECLINED, responseCode="14")

**Ответ 200: без тела**

#### `POST /api/simulator/terminal/stop`

**Запрос: без тела**

**Ответ 200: без тела**

```


**Ошибки:**
------------------
| Код | Условие                         |
|-----|---------------------------------|
| 400 | Плохой запрос                   |
| 422 | Не получено BLOCKED/ACTIVE карт |
| 4xx | Остальные http ошибки           |
| 503 | Downstream-сервис недоступен    |
| 500 | Остальные ошибки                |

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию     | Описание |
|------------|---------------------------|----------|
| `PORT` | `{8085}`                  | Порт сервиса |
| `DB_HOST` | -                         | Хост PostgreSQL |
| `DB_PORT` | -                         | Порт PostgreSQL |
| `DB_NAME` | -                         | Имя базы данных |
| `DB_USER` | -                         | Пользователь БД |
| `DB_PASSWORD` | -                         | Пароль БД |
| `{UPSTREAM}_URL` | `http://localhost:{port}` | URL смежного сервиса |

---

## Как запустить

### Локально (без Docker)

```bash
# Java (Spring Boot)
./mvnw spring-boot:run

```

### В Docker

```bash
docker build -t {service-name} .
docker run -p {port}:{port} --env-file ../.env terminal-simulator
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d terminal-simulator
```

---

## Тестирование

```bash
# Java
./mvnw test

```

---

## Взаимодействие с другими сервисами

| Сервис              | Направление | Протокол | Зачем                                        |
|---------------------|:----------:|----------|----------------------------------------------|
| API Gateway         | → исходящий | HTTP REST | Вызов API Gateway для отправки транзакции    |
| API Gateway         | ← входящий | HTTP REST | API Gateway возвращает результаты транзакций |
| API Card-Management | → исходящий | HTTP REST | Вызов API Card-Management получения карточек |

---

## Структура проекта

```text
terminal-simulator/
├── src/
│   ├── main/
│   │   ├── controller/     # controllers
│   │   ├── client/         # HTTP-handlers
│   │   ├── service/        # Бизнес-логика
│   │   ├── model/          # Модели данных
│   │   ├── dto/            # DTO
│   └── test/               # Тесты
├── Dockerfile
├── README.md
└── pom.xml
```

---

## Авторы

- Елена Серебренникова — разработчик

**Группа:** C (Data & Simulators)
