# Gateway Service

Gateway Service - точка входа во внешнее REST API процессинговой системы. Сервис
принимает запросы от симуляторов и dashboard-а, валидирует входящие запросы, ограничивает количество запросов, добавляет трассировку, защищает downstream-сервисы circuit breaker'ом
и проксирует запросы в нужные сервисы.

Подробные контракты API, модели запросов/ответов, коды ошибок и правила валидации
описаны в OpenAPI-спецификации.

## Возможности

- Health-check самого gateway и зависимых сервисов.
- Reverse proxy для основных сервисов процессинга.
- Валидация авторизационных транзакций перед отправкой в Switch.
- In-memory rate limiting на token bucket для транзакционного endpoint'а.
- Circuit breaker для downstream-сервисов.
- Кэширование успешных GET-ответов Card Management с инвалидированием при изменениях.
- Единая обработка недоступности downstream-сервисов.
- Request logging с `X-Request-Id` и MDC.
- Graceful shutdown с drain-периодом.
- Метрики Actuator и экспорт в Prometheus.
- Кастомные метрики для Grafana.

## API-документация

После запуска сервиса документация доступна в Swagger UI:

```text
http://localhost:8080/docs
```

Swagger UI показывает спецификации Gateway, Switch, Transaction Logger,
Terminal Simulator, Merchant Acquirer и Card Management.

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Конфигурация

Основные переменные окружения:

| Переменная | Значение по умолчанию | Описание |
|---|---:|---|
| `SERVER_PORT` | `8080` | Порт Gateway. Имеет приоритет над `GATEWAY_PORT`. |
| `GATEWAY_PORT` | `8080` | Альтернативная настройка порта Gateway. |
| `SWITCH_URL` | `http://localhost:8082` | URL Switch Service. |
| `LOGGER_URL` | `http://localhost:8088` | URL Transaction Logger. |
| `AUTH_URL` | `http://localhost:8083` | URL Authorization Service для health-check. |
| `CARD_MGMT_URL` | `http://localhost:8081` | URL Card Management Service. |
| `TERMINAL_SIM_URL` | `http://localhost:8085` | URL Terminal Simulator. |
| `MERCHANT_SIM_URL` | `http://localhost:8086` | URL Merchant Simulator. |
| `TRANSACTIONS_RATE_LIMIT_CAPACITY` | `100` | Размер token bucket для транзакционных запросов на клиента. |
| `TRANSACTIONS_RATE_LIMIT_REFILL_PER_SECOND` | `100` | Скорость пополнения token bucket в токенах в секунду. |
| `TRANSACTIONS_RATE_LIMIT_BUCKET_TTL` | `10m` | Время жизни неактивного rate-limit bucket. |
| `TRANSACTIONS_RATE_LIMIT_MAX_BUCKETS` | `10000` | Максимальное число rate-limit bucket'ов в памяти. |
| `TRANSACTIONS_RATE_LIMIT` | `100` | Legacy-переменная: используется как capacity/refill, если новые rate-limit переменные не заданы. |
| `CIRCUIT_BREAKER_FAILURE_THRESHOLD` | `3` | Количество ошибок до открытия circuit breaker. |
| `CIRCUIT_BREAKER_OPEN_DURATION` | `10s` | Время, на которое circuit breaker остается открытым. |
| `GATEWAY_PUBLIC_URL` | `http://localhost:${GATEWAY_PORT:8080}` | Публичный URL, который подставляется в OpenAPI-документы. |
| `GATEWAY_SHUTDOWN_DRAIN_PERIOD` | `30s` | Drain-период graceful shutdown: новые запросы получают `503`, а уже выполняющиеся запросы могут завершиться в течение этого времени. |
| `GATEWAY_STOP_GRACE_PERIOD` | `35s` | Время, которое Docker Compose дает gateway-контейнеру на завершение после `SIGTERM`; должно быть больше или равно `GATEWAY_SHUTDOWN_DRAIN_PERIOD`. |

Маршруты, metadata downstream-сервисов и rewrite-правила находятся в
`src/main/resources/application.yml`.

`GATEWAY_SHUTDOWN_DRAIN_PERIOD` используется приложением и пробрасывается в
`gateway.shutdown.drain-period` и `spring.lifecycle.timeout-per-shutdown-phase`.
`GATEWAY_STOP_GRACE_PERIOD` используется только `docker-compose.yaml` как
`stop_grace_period`.

Дополнительные настройки health-check:

| Свойство | Значение по умолчанию | Описание |
|---|---:|---|
| `gateway.health.connection-timeout` | `5` | Timeout подключения health HTTP-клиента, секунды. |
| `gateway.health.request-timeout` | `3` | Timeout health-запроса, секунды. |
| `gateway.health.url` | `/health` | Путь health-check на downstream-сервисах. |

## Локальный запуск

Из папки `services`:

```bash
mvn -P gateway -pl gateway -am spring-boot:run
```

Сборка jar:

```bash
mvn -P gateway -pl gateway -am package
```

Запуск собранного jar:

```bash
java -jar gateway/target/gateway-*.jar
```

Пример запуска с явными адресами downstream-сервисов:

```bash
SWITCH_URL=http://localhost:8082 \
LOGGER_URL=http://localhost:8088 \
AUTH_URL=http://localhost:8083 \
CARD_MGMT_URL=http://localhost:8081 \
mvn -P gateway -pl gateway -am spring-boot:run
```

## Docker

Сборка образа выполняется из корня репозитория:

```bash
docker build -f services/gateway/Dockerfile -t processing-gateway .
```

Запуск контейнера:

```bash
docker run --rm -p 8080:8080 \
  -e SWITCH_URL=http://host.docker.internal:8082 \
  -e LOGGER_URL=http://host.docker.internal:8088 \
  -e AUTH_URL=http://host.docker.internal:8083 \
  -e CARD_MGMT_URL=http://host.docker.internal:8081 \
  -e TERMINAL_SIM_URL=http://host.docker.internal:8085 \
  -e MERCHANT_SIM_URL=http://host.docker.internal:8086 \
  processing-gateway
```

## Проверка и тесты

Быстрая проверка запущенного сервиса:

```bash
curl http://localhost:8080/health
```

Тесты:

```bash
mvn -P gateway -pl gateway -am test
```

Prometheus metrics:

```text
http://localhost:8080/actuator/prometheus
```

Локальная генерация событий для кастомных gateway-метрик:

```bash
GATEWAY_URL=http://localhost:8080 ./scripts/gateway-metrics-demo.sh
```

Скрипт отправляет невалидную транзакцию, делает burst транзакционных запросов
для проверки rate-limit и дважды дергает Card Management endpoint для cache
hit/miss. Для стабильной проверки rate-limit удобно временно запустить gateway с
низким лимитом, например `TRANSACTIONS_RATE_LIMIT_CAPACITY=1` и
`TRANSACTIONS_RATE_LIMIT_REFILL_PER_SECOND=0`. Legacy-вариант
`TRANSACTIONS_RATE_LIMIT=1` тоже поддерживается. Если локальный endpoint карт
отличается, передайте `CARD_CACHE_PATH`, например
`CARD_CACHE_PATH=/api/cards/4000003458730237`.

## Структура модуля

```text
src/main/java/com/processing/gateway
├── caching/        # response cache for Card Management routes
├── circuitbreaker/ # circuit breaker state and filter
├── config/         # общие Spring beans
├── downstream/     # downstream service resolution and error handling
├── health/         # health-check API, client, service and models
├── logging/        # request logging, request id wrapper and log model
├── metrics/        # custom Micrometer gateway metrics
├── openapi/        # OpenAPI aggregation and rewriting
├── properties/     # binding application.yml properties
├── ratelimit/      # client IP resolver and token bucket limiter
├── shutdown/       # graceful shutdown state, listener and filter
└── validation/     # validation rules for AuthorizationRequest
```
