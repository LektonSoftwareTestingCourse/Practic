# Техническое задание: Authorization Service

**Роль:** Сервис авторизации — принимает решение одобрить или отклонить транзакцию
**Количество человек:** 2
**Сложность:** ⭐⭐⭐⭐
**Примерный объём кода:** 600–900 строк (Java/Go/Python)

---

## Ваша задача

Authorization Service — это «мозг» процессинга. Вы получаете запрос от Switch, проверяете карту, лимиты, баланс и возвращаете решение: APPROVED или DECLINED.

Это самый насыщенный бизнес-логикой сервис. Именно здесь живут правила, которые определяют судьбу каждой транзакции. Вас двое — разделите работу: один берёт на себя проверку карты и лимитов (шаги 1–5), второй — резервирование, генерацию RRN/authCode и обработку ошибок (шаги 6–7).

---

## Обязательные требования

### 1. Health-check: `GET /health`

```json
{
  "status": "ok",
  "service": "authorization",
  "dependencies": {
    "cardManagement": "ok"
  }
}
```

### 2. Авторизация: `POST /api/internal/authorize`

**Вход:** [`AuthorizationRequest`]() (от Switch, уже с `issuerId`)

**Обогащение issuerId через bin-lookup (вариант B):**
Перед проверкой карты Authorization вызывает внешний API Bin Lookup Service для обогащения данных эмитента:
- `GET http://bin-lookup:8096/api/bin/{bin}` → `BinLookupResponse { issuerId, bankName, countryCode }`
- Timeout: 3s connect, 5s read
- Retry: 2 попытки при 5xx ошибках
- Fallback: если bin-lookup недоступен — используется `issuerId` из таблицы BIN Switch (graceful degradation)

**Алгоритм проверки (строго в этом порядке):**

```
1. Получить карту из Card Management (GET /api/cards/{pan})
   ↓ если карта не найдена → DECLINED, responseCode="14"

2. Проверить статус карты:
   - INACTIVE → DECLINED, "CARD_INACTIVE"
   - BLOCKED  → DECLINED, "CARD_BLOCKED"
   - EXPIRED  → DECLINED, responseCode="54"
   ↓ если не ACTIVE → DECLINED

3. Проверить срок действия:
   expiryDate (MMYY) < текущая дата → DECLINED, responseCode="54"

4. Проверить дневной лимит:
   - Сумма всех одобренных транзакций за сегодня + текущая сумма ≤ dailyLimit
   - Превышен → DECLINED, responseCode="61"

5. Проверить месячный лимит:
   - Аналогично дневному, но за текущий месяц

6. Проверить доступный баланс:
   - amount ≤ availableBalance
   - Недостаточно → DECLINED, responseCode="51"

7. ВСЁ ОК → APPROVED:
   - Зарезервировать средства (вычесть из availableBalance через CMS)
   - Сгенерировать RRN (12 цифр)
   - Сгенерировать authCode (6 символов A-Z0-9)
   - Вернуть APPROVED, responseCode="00"
```

### 3. Резервирование средств

При успешной авторизации нужно зарезервировать (списать) сумму с `availableBalance` карты. Вызывайте Card Management:

```
POST /api/cards/{pan}/reserve
Body: { "amount": 150000, "rrn": "012345678901" }
```

### 4. Отслеживание использованных лимитов

Храните счётчики использованных лимитов в PostgreSQL. Таблица `limit_usage`:

```sql
CREATE TABLE limit_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL REFERENCES cards(id),
    usage_date DATE NOT NULL,
    daily_amount BIGINT NOT NULL DEFAULT 0,
    monthly_amount BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(card_id, usage_date)
);
CREATE INDEX idx_limit_usage_card ON limit_usage(card_id);
CREATE INDEX idx_limit_usage_date ON limit_usage(usage_date);
```

Логика работы:
- При каждой успешной авторизации обновляйте сумму потраченных средств за день и месяц
- При проверке лимита сравнивайте: `daily_amount + текущая сумма ≤ dailyLimit` (из CMS)
- `monthly_amount` агрегируется как сумма `daily_amount` за все дни текущего месяца

> **Важно:** значения `dailyLimit` и `monthlyLimit` хранятся в CMS (таблица `cards`). Authorization получает их через `GET /api/cards/{pan}` и сравнивает с usage из своей таблицы `limit_usage`.

### 5. Обработка ошибок

- Если Card Management недоступен → DECLINED, responseCode="05", причина: "ISSUER_TIMEOUT"

### 6. Генерация идентификаторов

- **RRN** (Retrieval Reference Number): 12 цифр, **уникальный в рамках системы**. Формат: `{YDDD}{HH}{mm}{ss}{seq}` — год + день года + час + минута + секунда + 1 цифра последовательности
  - **Обеспечение уникальности:** sequence-компонент (`seq`) должен инкрементироваться при совпадении timestamp в пределах секунды. Используйте атомарный счётчик (например, `AtomicInteger` в Java, `atomic.Int32` в Go) или генерацию на стороне БД
  - **Почему это важно:** Switch, Logger и Dashboard полагаются на RRN как на уникальный идентификатор транзакции. Дублирование RRN приведёт к перезаписи данных или некорректному поиску
- **authCode**: 6 символов (буквы+цифры), случайный. Пример: `A1B2C3`
  - Для учебного проекта допустима случайная генерация, но проверяйте отсутствие коллизий среди активных транзакций

> ⚠️ **Атомарность резервирования:** Операция «проверить баланс → зарезервировать» (шаги 6–7 в алгоритме) **не гарантированно атомарна**. Между `GET /api/cards/{pan}` и `POST /api/cards/{pan}/reserve` другая параллельная транзакция может изменить баланс. Для учебного проекта это допустимо, но учитывайте при нагрузочном тестировании — возможны race conditions.

---

## Дополнительные задания (бонусные баллы)

- PIN verification stub (запрос принимает PIN, проверяет хэш — всегда `1234`)
- CVV verification stub (всегда принимает `123`)
- **Reversal (mti="0400")** — разрезервирование средств. ⭐ рекомендуется: Switch использует этот механизм для отката при недоступности Logger (см. ТЗ Switch, п. 4). Алгоритм: найти транзакцию по `rrn` → вернуть сумму в `availableBalance` → снять запись из `limit_usage`
- 3-D Secure stub
- Кэширование данных карт в памяти для ускорения (TTL 60 секунд)

---

## План работы по дням

| День | Задача |
|:---:|---|
| 1 | Изучить архитектуру и API-контракт. Выбрать язык. Настроить проект |
| 2 | Health-check. Скелет `POST /api/internal/authorize` (всегда APPROVED) |
| 3–4 | Интеграция с Card Management. Проверка статуса карты и expiry |
| 5–6 | Проверка лимитов (дневной/месячный) и баланса |
| 7–8 | Резервирование средств через CMS. Генерация RRN и authCode |
| 9–10 | Обработка всех decline-сценариев. Dockerfile |
| 11–13 | Интеграционное тестирование со Switch и CMS |
| 14–15 | Полировка сервиса. Ревью куратора №3 (день 15). Code freeze |
| 16 | Рефакторинг: читаемость, стиль, комментарии, разделение на слои |
| 17 | Unit-тесты (минимум 3). Нагрузочное тестирование |
| 18 | Документация: README, алгоритм авторизации, RRN-формат, response codes |
| 19 | Сухая защита / репетиция. Доработка презентации и демо-сценария |
| 20 | 🎯 Финальная защита |

---

## Что сдаёте

| Артефакт | Описание |
|---|---|
| Исходный код | Полный Authorization Engine |
| `Dockerfile` | Сборка и запуск в контейнере |
| `README.md` | Алгоритм авторизации, RRN-формат, response codes |

---

## С какими сервисами взаимодействуете

- **Switch** → присылает запросы на авторизацию
- **Bin Lookup Service** → вызываете для обогащения issuerId через `GET /api/bin/{bin}` (timeout 3s/5s, retry, graceful degradation)
- **Card Management** → запрашиваете данные карты и резервируете средства
- **Transaction Logger (приём)** → в одной группе Core; через Switch отправляются транзакции в Logger

---

## Рекомендуемые технологии

- **Java:** Spring Boot 3 + RestTemplate / WebClient + Spring Data JPA
- **Go:** net/http + database/sql
- **Python:** FastAPI + SQLAlchemy + httpx
