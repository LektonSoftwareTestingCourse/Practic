# Test-design ядра: Authorization и Card Management

## 1. Классы эквивалентности

### 1.1 Authorization Service

| Параметр | Позитивные классы | Негативные классы                       |
|----------|-------------------|-----------------------------------------|
| PAN | Валидный (16 цифр, Luhn, есть в БД) | Неверная длина; неверный Luhn; нет в БД |
| Статус карты | ACTIVE | INACTIVE, BLOCKED, EXPIRED              |
| Сумма vs dailyLimit | below, equal | above                                   |
| Сумма vs monthlyLimit | below, equal | above                                   |
| Сумма vs availableBalance | below, equal | above                                   |
| expiryDate (MMYY) | valid (будущий месяц), current_month | expired (прошлый месяц)                 |
| MTI | 0100 (authorization) | 0400 (reversal) - отдельный сценарий    |
| Bin-lookup | доступен | недоступен (fallback на таблицу Switch) |

### 1.2 Card Management Service

| Параметр | Позитивные классы                       | Негативные классы |
|----------|-----------------------------------------|-------------------|
| PAN (GET/PATCH/DELETE) | Существует, ACTIVE                      | Не существует; статус DELETED |
| BIN (создание) | Из утверждённого списка (400000-400004) | Другой BIN |
| `count` в генераторе | 1, 100                                  | 0, отрицательное, >10000 |
| `amount` в `/reserve` | below balance, equal                    | above balance |
| currencyCode | 643                                     | другая валюта |
| dailyLimit / monthlyLimit | корректные значения                     | отрицательные, null |

---

## 2. Граничные значения

### 2.1 Authorization - лимиты и баланс

Для каждой границы берём 3 точки: **ON** (ровно граница), **OFF-** (чуть ниже), **OFF+** (чуть выше).

| Граница | OFF- | ON | OFF+ |
|---------|------|----|----|
| `dailyLimit` = 150 000 ₽ | 149 999 | 150 000 | 150 001 |
| `monthlyLimit` = 3 000 000 ₽ | 2 999 999 | 3 000 000 | 3 000 001 |
| `availableBalance` = 10 000 ₽ | 9 999 | 10 000 | 10 001 |

### 2.2 Authorization - expiryDate (MMYY)

Предположим, текущая дата - **25.09.2026**.

| Значение expiry | Класс | Ожидаемый результат |
|-----------------|-------|---------------------|
| 0826 (август 2026) | expired (OFF-) | DECLINED, responseCode=54 |
| 0926 (сентябрь 2026) | current_month (ON) | APPROVED (карта ещё валидна весь месяц) |
| 1026 (октябрь 2026) | valid (OFF+) | APPROVED |

### 2.3 Card Management - длина PAN

- OFF-: 15 цифр → 400 (invalid)
- ON: 16 цифр → 200 (если Luhn корректен)
- OFF+: 17 цифр → 400

### 2.4 Card Management - `count` в генераторе

- OFF-: 0 → пустой список / 400
- ON: 1 → создаётся 1 карта
- OFF+: 10001 → отказ или ограничение (зависит от реализации)

---

## 3. Попарное тестирование (Pairwise)

Модель и ограничения - в `docs/practice-2/pict/model.txt`.
Сгенерированный набор - в `docs/practice-2/pict/cases.txt`.

**Параметры (7 штук):**
1. `card_status`: ACTIVE, INACTIVE, BLOCKED, EXPIRED
2. `amount_vs_daily`: below, equal, above
3. `amount_vs_monthly`: below, equal, above
4. `amount_vs_balance`: below, equal, above
5. `expiry`: valid, current_month, expired
6. `terminal_type`: pos, atm, ecom
7. `mcc`: grocery, restaurant, electronics, travel

**Ограничения:**
- Если карта не ACTIVE - все amount-параметры = below (иначе тест не имеет смысла)
- Если expiry = expired - amount_vs_balance = below (по истёкшей карте не провести платёж)

**Дополнительно**  
Модель и ограничения - в `docs/practice-2/pict/model-cm.txt`.
Сгенерированный набор - в `docs/practice-2/pict/cases-cm.txt`.

---

## 4. Тест-кейсы

### 4.1 Authorization - базовые сценарии

#### TC-AUTH-001: Успешная авторизация (позитивный)
- **ID:** TC-AUTH-001
- **Требование:** ТЗ Authorization, п.2 (алгоритм, шаги 1-7)
- **Вид:** позитивный
- **Источник:** класс эквивалентности "все параметры валидны"
- **Предусловие:** в CMS есть карта `4000001234560001`, статус ACTIVE, dailyLimit=150000, balance=100000
- **Шаги:**
    1. Switch отправляет `POST /api/internal/authorize` с mti=0100, amount=10000 (100 ₽)
- **Ожидаемый результат:** status=APPROVED, responseCode="00", есть authCode (6 символов) и RRN (12 цифр)

#### TC-AUTH-002: Карта не найдена
- **ID:** TC-AUTH-002
- **Требование:** ТЗ Authorization, шаг 1 алгоритма
- **Вид:** негативный
- **Источник:** негативный класс "PAN нет в БД"
- **Предусловие:** PAN `4000009999999999` отсутствует в CMS
- **Шаги:** отправить авторизацию с этим PAN
- **Ожидаемый результат:** DECLINED, responseCode="14" (Invalid Card Number)

#### TC-AUTH-003: Карта заблокирована
- **ID:** TC-AUTH-003
- **Требование:** ТЗ Authorization, шаг 2 алгоритма
- **Вид:** негативный
- **Источник:** класс "статус BLOCKED"
- **Предусловие:** карта со статусом BLOCKED
- **Шаги:** отправить авторизацию
- **Ожидаемый результат:** DECLINED, responseCode="CARD_BLOCKED"

#### TC-AUTH-004: Превышен дневной лимит (граница ON+)
- **ID:** TC-AUTH-004
- **Требование:** ТЗ Authorization, шаг 4
- **Вид:** негативный
- **Источник:** BVA, dailyLimit OFF+
- **Предусловие:** dailyLimit=150000, уже потрачено 0
- **Шаги:** авторизация на сумму 150001
- **Ожидаемый результат:** DECLINED, responseCode="61"

#### TC-AUTH-005: Ровно на границе дневного лимита (ON)
- **ID:** TC-AUTH-005
- **Требование:** ТЗ Authorization, шаг 4
- **Вид:** позитивный
- **Источник:** BVA, dailyLimit ON
- **Предусловие:** dailyLimit=150000, потрачено 0
- **Шаги:** авторизация на сумму 150000
- **Ожидаемый результат:** APPROVED

#### TC-AUTH-006: Недостаточно средств
- **ID:** TC-AUTH-006
- **Требование:** ТЗ Authorization, шаг 6
- **Вид:** негативный
- **Источник:** класс "amount > balance"
- **Предусловие:** balance=10000
- **Шаги:** авторизация на 10001
- **Ожидаемый результат:** DECLINED, responseCode="51"

#### TC-AUTH-007: Истёк срок действия карты
- **ID:** TC-AUTH-007
- **Требование:** ТЗ Authorization, шаг 3
- **Вид:** негативный
- **Источник:** BVA, expiryDate expired
- **Предусловие:** expiryDate=0826 (текущая дата 25.09.2026)
- **Шаги:** авторизация
- **Ожидаемый результат:** DECLINED, responseCode="54"

#### TC-AUTH-008: Bin-lookup недоступен (graceful degradation)
- **ID:** TC-AUTH-008
- **Требование:** ТЗ Authorization, "Обогащение issuerId через bin-lookup"
- **Вид:** негативный (сценарий отказа)
- **Источник:** класс "bin-lookup недоступен"
- **Предусловие:** bin-lookup не отвечает (например, выключен контейнер)
- **Шаги:** отправить авторизацию
- **Ожидаемый результат:** APPROVED (fallback на issuerId из таблицы Switch), не DECLINED

#### TC-AUTH-009: Card Management недоступен
- **ID:** TC-AUTH-009
- **Требование:** ТЗ Authorization, п.5 "Обработка ошибок"
- **Вид:** негативный
- **Предусловие:** Card Management выключен
- **Шаги:** отправить авторизацию
- **Ожидаемый результат:** DECLINED, responseCode="05", причина "ISSUER_TIMEOUT"

### 4.2 Card Management - базовые сценарии

#### TC-CM-001: Создание карты с валидным BIN
- **ID:** TC-CM-001
- **Требование:** ТЗ CM, POST /api/cards
- **Вид:** позитивный
- **Предусловие:** -
- **Шаги:**
    1. `POST /api/cards` с bin=400000, dailyLimit=150000, monthlyLimit=300000, initialBalance=100000
- **Ожидаемый результат:** 201 Created, PAN 16 цифр, первая цифра 4, Luhn корректен, status=ACTIVE, expiryDate = текущая дата + 3 года

#### TC-CM-002: Генерация тестовых карт (нормальный count)
- **ID:** TC-CM-002
- **Требование:** ТЗ CM, POST /api/cards/generate
- **Вид:** позитивный
- **Предусловие:** -
- **Шаги:** `POST /api/cards/generate` с count=100, bins=[400000..400004]
- **Ожидаемый результат:** создано 100 карт, распределены по BIN равномерно, статусы примерно 95% ACTIVE / 3% INACTIVE / 2% BLOCKED

#### TC-CM-003: Генерация с count=0 (граница)
- **ID:** TC-CM-003
- **Требование:** ТЗ CM
- **Вид:** негативный (граница)
- **Шаги:** `POST /api/cards/generate` с count=0
- **Ожидаемый результат:** 400 Bad Request или пустой список

#### TC-CM-004: Резервирование средств, amount < balance
- **ID:** TC-CM-004
- **Требование:** ТЗ CM, POST /api/cards/{pan}/reserve
- **Вид:** позитивный
- **Предусловие:** карта с balance=100000
- **Шаги:** `POST /api/cards/{pan}/reserve` с amount=50000
- **Ожидаемый результат:** 200 OK, balance стал 50000

#### TC-CM-005: Резервирование, amount > balance
- **ID:** TC-CM-005
- **Требование:** ТЗ CM
- **Вид:** негативный
- **Предусловие:** balance=10000
- **Шаги:** reserve с amount=10001
- **Ожидаемый результат:** 400 или 402, баланс не изменился

#### TC-CM-006: Получение несуществующей карты
- **ID:** TC-CM-006
- **Требование:** ТЗ CM, GET /api/cards/{pan}
- **Вид:** негативный
- **Шаги:** `GET /api/cards/9999999999999999`
- **Ожидаемый результат:** 404 Not Found

#### TC-CM-007: Мягкое удаление карты
- **ID:** TC-CM-007
- **Требование:** ТЗ CM, DELETE /api/cards/{pan}
- **Вид:** позитивный
- **Предусловие:** карта ACTIVE
- **Шаги:**
    1. `DELETE /api/cards/{pan}`
    2. `GET /api/cards/{pan}`
- **Ожидаемый результат:** после удаления GET возвращает 404 (или статус DELETED, но в выдачу не попадает)

### 4.3 Тест-кейсы из pairwise-набора

Ниже - проекция строк из `cases.txt` в тест-кейсы.

#### TC-PW-001: ACTIVE, ниже всех лимитов, валидная expiry, POS, grocery
- **Источник:** строка 1 из cases.txt
- **Вид:** позитивный
- **Шаги:** авторизация на небольшую сумму в супермаркете
- **Ожидаемый результат:** APPROVED

#### TC-PW-002: ACTIVE, на границе daily, на границе monthly, на границе balance, current_month expiry, ATM, restaurant
- **Источник:** строка 2
- **Вид:** позитивный (стресс-тест границ)
- **Шаги:** сумма одновременно равна daily, monthly и balance
- **Ожидаемый результат:** APPROVED (если все три равенства выполняются)

#### TC-PW-003: ACTIVE, выше daily, выше monthly, ниже balance, valid, ecom, electronics
- **Источник:** строка 3
- **Вид:** негативный
- **Шаги:** крупная покупка в интернет-магазине электроники
- **Ожидаемый результат:** DECLINED, responseCode="61" (превышен лимит)

#### TC-PW-004: INACTIVE, всё below, valid, POS, travel
- **Источник:** строка 4
- **Вид:** негативный
- **Шаги:** любая сумма по неактивной карте
- **Ожидаемый результат:** DECLINED, responseCode="CARD_INACTIVE"

#### TC-PW-005: BLOCKED, всё below, current_month, ATM, grocery
- **Источник:** строка 5
- **Вид:** негативный
- **Ожидаемый результат:** DECLINED, responseCode="CARD_BLOCKED"

#### TC-PW-006: EXPIRED, всё below, expired, ecom, restaurant
- **Источник:** строка 6
- **Вид:** негативный
- **Ожидаемый результат:** DECLINED, responseCode="54"

#### TC-PW-007: ACTIVE, expired карта, ниже balance, POS, travel
- **Источник:** строка 18
- **Вид:** негативный
- **Ожидаемый результат:** DECLINED, responseCode="54" (несмотря на то, что лимиты не превышены)

#### TC-PW-008: ACTIVE, above daily, below monthly, above balance, current_month, ecom, grocery
- **Источник:** строка 11
- **Вид:** негативный (сработает первое упавшее условие - dailyLimit)
- **Ожидаемый результат:** DECLINED, responseCode="61"