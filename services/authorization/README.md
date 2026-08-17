# Authorization
## Назначение
Сервис авторизации — принимает решение одобрить или отклонить транзакцию
Пример:
> Authorization Service проверяет каждую транзакцию: статус карты, лимиты, баланс — и возвращает решение APPROVED или DECLINED.
---
## Технологии
- Язык: Java 21
- Фреймворк: Spring Boot 3
- База данных: PostgreSQL
- Контейнеризация: Docker
---
## Endpoints
| Метод | Путь | Описание |
|-------|------|----------|
| GET | /health | Health-check сервиса |
| POST | /api/internal/authorize | Authorization: проверка |
| POST | /api/internal/rollback | Rollback: откат транзакции |
### Подробно
#### GET /health
Ответ 200:
```
{
  "status": "ok",
  "service": "authservice",
  "dependencies": {
    "{dep1}": "ok"
  }
}
```
#### POST /api/internal/authorize
Тело запроса:
```
{
  "mti": "0100",
  "stan": "123456",
  "pan": "1234567890123456",
  "processingCode": "000000",
  "amount": 0,
  "currencyCode": "810",
  "transmissionDateTime": "2026-06-05T18:12:49.07",
  "terminalId": "T0000001",
  "terminalType": "ATM",
  "merchantId": "M00000000000001",
  "mcc": "5411",
  "acquirerId": "A001",
  "issuerId": "I001"
}
```
Ответ 200:
```
{
  "mti": "0100",
  "stan": "123456",
  "rrn": "616211293600"
  "authCode": "A1B2C3",
  "responseCode": "00",
  "status": "APPROVED",
  "declineReason": "",
  "processingTimeMs": 42
}
```
Ошибки:
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 403 | Карта заблокирована, неактивна или просрочена |
| 404 | Карта не найдена |
| 422 | Недостаточно средств |
| 503 | Downstream-сервис недоступен |

#### POST /api/internal/rollback
Тело запроса:
```
{
  "rrn": "012345678901",
  "pan": "4000001234560001",
  "amount": 1000
}
```
Ответ 200:
```
{
  "rrn": "012345678901",
  "responseCode": "00",
  "status": "APPROVED",
  "declineReason": "",
  "processingTimeMs": 67
}

```
Ошибки:
| Код | Условие |
|-----|---------|
| 400 | Ошибка валидации |
| 404 | Транзакция не найдена |
| 409 | Конфликт: транзакция уже откачена |
| 500 | Внутренние проблемы сервиса |
| 503 | Downstream-сервис недоступен |
---
## Конфигурация
Все параметры через переменные окружения (файл .env в корне проекта):
| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| DB_HOST | postgres | Хост PostgreSQL |
| DB_PORT | 5432 | Порт PostgreSQL |
| DB_NAME | smp_db | Имя базы данных |
| DB_USER | smp_user | Пользователь БД |
| DB_PASSWORD | smp_password | Пароль БД |
| AUTH_PORT | 8083 | Порт сервиса |
| CARD_MGMT_PORT | 8081 | Порт смежного сервиса |
---
## Как запустить
### Локально (без Docker)
```
# Java (Spring Boot)
mvn spring-boot:run
```
### В Docker
```
docker build -t authorization .
docker run -p 8083:8080 --env-file ../.env authorization
```
### В составе Docker Compose
```
# Из корня репозитория
docker compose up -d authorization
```
---
## Тестирование
```
# Java
mvn test
```
---
## Взаимодействие с другими сервисами

| Сервис | Направление | Протокол | Зачем |
|--------|:----------:|----------|-------|
| Switch | ← входящий | HTTP REST | присылает запросы на авторизацию или откат транзакции |
| CardManagment | → исходящий | HTTP REST | запрашиваем данные карты, резервируем и возвращаем средства |

---
## Структура проекта
```text
authorization/src/
├── main/
│   ├── resoursces/
│   └── java.com.processing.authorization/
│       ├── client/            # Клиент для обращения к CMS
│       ├── configs/           # Конфигурация spring
│       ├── constants/         # Константы для логгов и decline ответов
│       ├── controller/        # HTTP-handlers / controllers
│       ├── dto/               # Модели данных / DTO
│       ├── entities/          # Сущности
│       ├── events/            # События
│       ├── exceptions/        # Custom исключения
│       ├── listeners/         # Слушатели, для обработки событий
│       ├── repositories/      # Репозиторий
│       ├── services/          # Сервисы для обработки запросов
│       └── Application.java

├── test/                      # Тесты
│   ├── resoursces/
│   └── java.com.processing.authorization/
│       ├── controller/        # Тесты контроллера
│       ├── integration/       # Интеграционные тесты с БД
│       └── service/           # Тесты сервиса
├── Dockerfile
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
└── {специфичные файлы}
```
---
## Авторы
- Дарья Ермолаева — разработчик
- Алина Клименко — разработчик
Группа: A { Core }
