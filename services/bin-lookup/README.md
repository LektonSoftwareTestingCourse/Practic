# Bin Lookup Service

Микросервис для разрешения BIN (Bank Identification Number) в идентификатор эмитента.

## Эндпоинт

```
GET /api/bin/{bin}
```

### Параметры

| Параметр | Тип    | По умолчанию | Описание |
|----------|--------|-------------|----------|
| `delay`  | int    | 0           | Искусственная задержка ответа в миллисекундах |
| `fail`   | bool   | false       | Если true — возвращает HTTP 500 для тестирования отказов |

### Таблица BIN

| BIN    | Issuer ID | Название    |
|--------|-----------|-------------|
| 400000 | ISS001    | Test Bank 1 |
| 400001 | ISS002    | Test Bank 2 |
| 400002 | ISS003    | Test Bank 3 |
| 400003 | ISS004    | Test Bank 4 |
| 400004 | ISS005    | Test Bank 5 |

### Ответы

- **200** — [`BinLookupResponse`](src/main/java/com/processing/binlookup/dto/BinLookupResponse.java)
- **404** — BIN не найден
- **500** — имитация ошибки (`fail=true`)

## Сборка

```bash
cd practic/services
mvn -P bin-lookup -pl bin-lookup -am package
```

## Docker

```bash
cd practic
docker compose build bin-lookup
```
