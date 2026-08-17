# Техническое задание: Gateway Service

**Роль:** API Gateway — точка входа для всех внешних запросов
**Количество человек:** 2
**Сложность:** ⭐⭐⭐
**Примерный объём кода:** 500–1000 строк (Java/Go)

---

## Ваша задача

Gateway — это единственная точка входа в систему СМП. Все запросы от симуляторов и дашборда проходят через вас. Вы отвечаете за валидацию, маршрутизацию (проксирование) и rate-limiting.

Представьте: вы — «дверь» в процессинговый центр. Всё, что заходит в систему, проходит через вас.

---

## Обязательные требования

### 1. Health-check: `GET /health`

Возвращает статус самого Gateway и всех downstream-сервисов:

```json
{
  "status": "ok",
  "service": "gateway",
  "version": "1.0.0",
  "services": {
    "switch": "ok",
    "authorization": "ok",
    "cardManagement": "ok",
    "logger": "ok"
  }
}
```

Для проверки downstream-сервисов дёргайте их `/health`.

### 2. Приём транзакций: `POST /api/transactions`

**Это главный эндпоинт.** Получаете [`AuthorizationRequest`](), валидируете его и отправляете в Switch.

**Валидация:**
- Все обязательные поля присутствуют (см. API-контракт)
- `mti` = "0100" (основной сценарий авторизации)
- `pan` — ровно 16 цифр
- `amount` > 0
- `currencyCode` — 3 символа
- `mcc` — 4 цифры
- `stan` — не пустой

> **Примечание:** MTI `"0200"` (финансовая транзакция) и `"0400"` (reversal) — бонусные сценарии. Если они реализованы в Authorization и Switch, расширьте валидацию соответствующим образом.

При ошибке валидации — HTTP 400 с понятным сообщением:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Field 'pan' must be exactly 16 digits",
  "timestamp": "2026-06-01T10:30:00Z"
}
```

После валидации — проксируете запрос в Switch (`POST /api/internal/route`). Ответ от Switch возвращаете клиенту как есть.

### 3. Проксирование (reverse proxy)

Вы проксируете запросы к соответствующим сервисам:

| Путь | Проксируется в |
|------|---------------|
| `/api/cards/**` | Card Management Service |
| `/api/transactions` | Switch |
| `/api/transactions/search` | Transaction Logger |
| `/api/simulator/terminal/**` | Terminal Simulator |
| `/api/simulator/merchant/**` | Merchant Simulator |
| `/api/dashboard/**` | Transaction Logger (stats/recent) |

### 4. Rate Limiting

Не более **100 запросов в секунду** на эндпоинт `/api/transactions`. Используйте in-memory счётчик (concurrent map). При превышении — HTTP 429:

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Try again later.",
  "retryAfterMs": 1000
}
```

### 5. Обработка ошибок

Если downstream-сервис недоступен — HTTP 503:

```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "Authorization service is temporarily unavailable",
  "serviceName": "authorization"
}
```

### 6. Логирование

- Каждый запрос логируется: метод, путь, статус, время ответа
- Добавляйте `X-Request-Id` в заголовки для трассировки

### 7. Swagger UI

Документация API доступна на `GET /docs` (Swagger UI).

---

## Дополнительные задания (бонусные баллы)

- Circuit Breaker: при недоступности сервиса не дёргать его N секунд
- Метрики в Prometheus (количество запросов, latency, статусы)
- Кэширование ответов от Card Management

---

## План работы по дням

| День | Задача |
|:---:|---|
| 1 | Изучить архитектуру. Выбрать язык/фреймворк. Настроить проект |
| 2 | Утвердить API-контракт. Health-check |
| 3 | Скелет: `POST /api/transactions` с заглушкой. Dockerfile |
| 4–5 | Валидация, rate-limiting (in-memory), проксирование |
| 6–8 | Интеграция со Switch (реальная, не заглушка) |
| 9–10 | Обработка ошибок, логирование, swagger |
| 11–13 | Интеграционное тестирование, исправление багов |
| 14–15 | Полировка сервиса. Ревью куратора №3 (день 15). Code freeze |
| 16 | Рефакторинг: читаемость, стиль, комментарии, разделение на слои |
| 17 | Unit-тесты (минимум 3). Нагрузочное тестирование |
| 18 | Документация: README, JSDoc/JavaDoc, описание endpoints |
| 19 | Сухая защита / репетиция. Доработка презентации и демо-сценария |
| 20 | 🎯 Финальная защита |

---

## Что сдаёте

| Артефакт | Описание |
|---|---|
| Исходный код | Полный Gateway с валидацией, проксированием, rate-limiting |
| `Dockerfile` | Сборка и запуск в контейнере |
| `README.md` | Как запустить, какие endpoints доступны, конфигурация |

---

## С какими сервисами взаимодействуете

- **Switch** — основной downstream, отправляете транзакции на маршрутизацию
- **Все остальные** — проксируете запросы
- **Dashboard** — обслуживаете фронтенд

---

## Рекомендуемые технологии

- **Java:** Spring Boot 3 + Spring Cloud Gateway
- **Go:** net/http + gorilla/mux или chi
- **Node.js:** Express + http-proxy-middleware
