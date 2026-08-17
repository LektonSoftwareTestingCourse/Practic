# README — Шаблон для сервиса СМП

>
> Скопируй этот шаблон в `services/{имя-сервиса}/README.md` и заполни разделы.

---

# {Service Name}

## Назначение

{Одно предложение — что делает сервис, его роль в процессинговом центре.}

Пример:
> Authorization Service проверяет каждую транзакцию: статус карты, лимиты, баланс — и возвращает решение APPROVED или DECLINED.

---

## Технологии

- **Язык:** {Java 21 / Go 1.22 / Python 3.12 / TypeScript 5}
- **Фреймворк:** {Spring Boot 3 / FastAPI / net/http / React 18}
- **База данных:** {PostgreSQL / нет}
- **Контейнеризация:** Docker

---

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Health-check сервиса |
| POST | `/api/...` | {Краткое описание} |

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

#### `POST /api/...`

**Тело запроса:**
```json
{
  "field1": "value",
  "field2": 123
}
```

**Ответ 200:**
```json
{
  "result": "ok",
  "data": {}
}
```

**Ошибки:**
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | {Сущность} не найдена |
| 503 | Downstream-сервис недоступен |

---

## Конфигурация

Все параметры через переменные окружения (файл `.env` в корне проекта):

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `{8080}` | Порт сервиса |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `processing` | Имя базы данных |
| `DB_USER` | `postgres` | Пользователь БД |
| `DB_PASSWORD` | `postgres` | Пароль БД |
| `{UPSTREAM}_URL` | `http://localhost:{port}` | URL смежного сервиса |

---

## Как запустить

### Локально (без Docker)

```bash
# Java (Spring Boot)
./mvnw spring-boot:run

# Go
go run ./cmd/main.go

# Python
pip install -r requirements.txt
uvicorn main:app --reload --port {port}

# TypeScript (React)
npm install
npm run dev
```

### В Docker

```bash
docker build -t {service-name} .
docker run -p {port}:{port} --env-file ../.env {service-name}
```

### В составе Docker Compose

```bash
# Из корня репозитория
docker compose up -d {service-name}
```

---

## Тестирование

```bash
# Java
./mvnw test

# Go
go test ./...

# Python
pytest

# TypeScript
npm test
```

---

## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем |
|--------|:----------:|----------|-------|
| {Service A} | ← входящий | HTTP REST | {Кто вызывает и зачем} |
| {Service B} | → исходящий | HTTP REST | {Кого вызываем и зачем} |

---

## Структура проекта

```text
{service-name}/
├── src/
│   ├── main/
│   │   ├── controllers/    # HTTP-handlers / controllers
│   │   ├── services/       # Бизнес-логика
│   │   ├── models/          # Модели данных / DTO
│   │   ├── repositories/   # Доступ к БД
│   │   └── config/         # Конфигурация
│   └── test/               # Тесты
├── Dockerfile
├── README.md
└── {специфичные файлы}
```

---

## Авторы

- {Имя Фамилия} — разработчик
- {Имя Фамилия} — разработчик

**Группа:** {A / B / C} ({Core / Gateway & Frontend / Data & Simulators})
