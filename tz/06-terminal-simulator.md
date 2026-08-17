# Техническое задание: Terminal Simulator

**Роль:** Эмулятор POS-терминалов — генератор тестовых транзакций
**Количество человек:** 1
**Сложность:** ⭐⭐⭐
**Примерный объём кода:** 300–500 строк (Python)

---

## Твоя задача

Terminal Simulator эмулирует работу POS-терминалов в магазинах. Ты генерируешь поток транзакций с разными параметрами и отправляешь их в Gateway. Без тебя никто не сможет проверить работоспособность всей системы в действии.

---

## Обязательные требования

### 1. Health-check: `GET /health`

```json
{
  "status": "ok",
  "service": "terminal-simulator"
}
```

### 2. Запуск симуляции: `POST /api/simulator/terminal/run`

**Вход:**

```json
{
  "count": 50,
  "scenario": "normal"
}
```

**Выход:**

```json
{
  "totalSubmitted": 50,
  "approved": 47,
  "declined": 3,
  "elapsedMs": 2300,
  "transactions": [ /* массив AuthorizationResponse */ ]
}
```

### 3. Сценарии симуляции

Реализуй 5 сценариев:

#### `normal` — обычные покупки

- 100% транзакций с небольшими суммами (100–5 000 ₽)
- Все карты ACTIVE
- Дневное время (09:00–22:00)
- MCC: 5411 (супермаркеты)

**Пример генерируемой транзакции:**

```json
{
  "mti": "0100",
  "stan": "000001",
  "pan": "4000001234560001",
  "processingCode": "000000",
  "amount": 125000,
  "currencyCode": "643",
  "transmissionDateTime": "2026-06-01T14:30:00Z",
  "terminalId": "TERM001",
  "terminalType": "POS",
  "merchantId": "MERCH12345678901",
  "mcc": "5411",
  "acquirerId": "ACQ001"
}
```

#### `mixed` — смешанные транзакции

- 70% обычные покупки
- 15% крупные суммы (50 000 – 140 000 ₽)
- 10% на грани лимита (почти дневной лимит)
- 5% с заблокированными картами (ожидаем DECLINED)

#### `high_value` — только крупные суммы

- Суммы 100 000 – 500 000 ₽
- Проверка лимитов и баланса

#### `night_time` — ночные транзакции

- Время: 01:00–05:00
- Смесь малых и крупных сумм
- Крупные суммы могут отклоняться по ночным лимитам (если реализованы в Authorization)

#### `declines_test` — тест отклонений

- 20% с несуществующими PAN (ожидаем DECLINED, responseCode="14")
- 20% с заблокированными картами (ожидаем DECLINED, "CARD_BLOCKED")
- 20% с недостаточным балансом (ожидаем DECLINED, "51")
- 20% с превышением дневного лимита (ожидаем DECLINED, "61")
- 20% нормальные (ожидаем APPROVED)

> **Генерация несуществующих PAN:** сгенерируй случайный PAN с BIN из утверждённого списка (`400000`–`400004`), но с заведомо неверной контрольной цифрой Луна (например, инвертируй последнюю цифру). Такой PAN гарантированно не пройдёт проверку и не будет найден в CMS.

### 4. Получение тестовых карт

При старте симуляции запроси у Card Management список доступных карт:

```
GET /api/cards?limit=200
```

Используй случайные карты из пула для каждой транзакции.

### 5. Генерация STAN

STAN (System Trace Audit Number) — уникальный номер транзакции:

```
STAN = 000001, 000002, ..., 999999 (последовательно, циклично)
```

### 6. Отправка транзакций

Каждую транзакцию отправляй через Gateway:

```
POST http://gateway:8080/api/transactions
Content-Type: application/json
{ AuthorizationRequest }
```

Собирай ответы и формируй итоговую статистику.

### 7. Режимы запуска

- `POST /api/simulator/terminal/run` — синхронный (дождаться всех ответов)
- `[OPTIONAL] POST /api/simulator/terminal/start` + `GET /api/simulator/terminal/status` — асинхронный

---

## Дополнительные задания (бонусные баллы)

- Параллельная отправка транзакций (многопоточно, эмулируя реальную нагрузку)
- Конфигурируемая задержка между транзакциями (rate: transactions per second)
- Непрерывный режим (`POST /api/simulator/terminal/start-continuous`) — шлёт транзакции постоянно
- Визуализация прогресса (progress bar в консоли)

---

## План работы по дням

| День | Задача |
|:---:|---|
| 1 | Изучить архитектуру. Выбрать язык (Python рекомендуется). Настроить проект |
| 2 | Health-check. Скелет `POST /api/simulator/terminal/run` — отправка 1 статической транзакции |
| 3–4 | Сценарий `normal`: генерация случайных карт, сумм, STAN |
| 5–6 | Сценарии `mixed`, `high_value`, `night_time`, `declines_test` |
| 7–8 | Интеграция с Gateway (реальная отправка). Сбор статистики |
| 9–10 | Параллельная отправка. Dockerfile |
| 11–13 | Интеграционное тестирование со всей системой |
| 14–15 | Полировка сервиса. Ревью куратора №3 (день 15). Code freeze |
| 16 | Рефакторинг: читаемость, стиль, комментарии |
| 17 | Unit-тесты (минимум 3). Нагрузочное тестирование: прогон 1000+ транзакций |
| 18 | Документация: README, описание сценариев, примеры вывода |
| 19 | Сухая защита / репетиция. Доработка презентации и демо-сценария |
| 20 | 🎯 Финальная защита |

---

## Что сдаёшь

| Артефакт | Описание |
|---|---|
| Исходный код | Симулятор с 5 сценариями |
| `Dockerfile` | Сборка и запуск в контейнере |
| `README.md` | Описание сценариев, как запустить, примеры вывода |

---

## С какими сервисами взаимодействуешь

- **Gateway** → отправляешь все транзакции
- **Card Management** → запрашиваешь список карт для генерации

---

## Пример кода (Python)

```python
import random
import requests
from datetime import datetime, timedelta

class TerminalSimulator:
    SCENARIOS = {
        "normal": {
            "amount_range": (10000, 500000),     # 100-5000 RUB in kopecks
            "time_range": (9, 22),
            "mcc": "5411",
            "card_filter": lambda c: c["status"] == "ACTIVE"
        },
        "high_value": {
            "amount_range": (10000000, 50000000),  # > 100k RUB
            "time_range": (9, 22),
            "mcc": "5411",
            "card_filter": lambda c: c["status"] == "ACTIVE"
        },
        # ... и т.д.
    }

    def run(self, count: int, scenario: str):
        config = self.SCENARIOS[scenario]
        cards = self._get_cards()

        results = []
        for i in range(count):
            card = random.choice(cards)
            tx = self._generate_transaction(card, config, i+1)
            resp = self._send_to_gateway(tx)
            results.append(resp)

        approved = sum(1 for r in results if r["status"] == "APPROVED")
        declined = count - approved

        return {
            "totalSubmitted": count,
            "approved": approved,
            "declined": declined,
            "transactions": results
        }
```

---

## Рекомендуемые технологии

- **Python:** FastAPI + httpx (async HTTP-клиент) — оптимальный выбор
- **Go:** net/http (горутины для параллельной отправки)
- **Node.js:** Express + axios
