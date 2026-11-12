# Test-design ядра СМП

## 1. Классы эквивалентности (Equivalence Partitioning)

### Сервис Authorization
1. **Статус карты**:
   - Позитивный: ACTIVE
   - Негативные: INACTIVE, BLOCKED, EXPIRED
2. **Сумма транзакции относительно лимитов**:
   - Позитивный: amount < dailyLimit, amount < monthlyLimit, amount < availableBalance
   - Негативные: amount >= dailyLimit, amount >= monthlyLimit, amount >= availableBalance
3. **Срок действия карты (expiryDate)**:
   - Позитивный: MMYY в будущем
   - Негативные: MMYY в прошлом, текущий месяц (граница)

### Сервис Card-Management
1. **Формат PAN**:
   - Позитивный: 16-19 цифр, проходит алгоритм Луна
   - Негативные: <16 цифр, >19 цифр, не проходит Луна
2. **Формат expiryDate**:
   - Позитивный: MMYY (01-12)
   - Негативные: MMYY (00, 13-99)

## 2. Граничные значения (Boundary Value Analysis)

| Параметр | Граница | ON (граница) | OFF (за границей) |
|---|---|---|---|
| `dailyLimit` | Равенство суммы и лимита | Сумма = dailyLimit | Сумма = dailyLimit + 1 |
| `monthlyLimit` | Равенство суммы и лимита | Сумма = monthlyLimit | Сумма = monthlyLimit + 1 |
| `availableBalance` | Равенство суммы и баланса | Сумма = availableBalance | Сумма = availableBalance + 1 |
| `expiryDate` | Текущий месяц | MMYY = текущий | MMYY = прошлый, MMYY = следующий |
| Длина PAN | 16 и 19 символов | 16, 19 | 15, 20 |

## 3. Попарное тестирование (Pairwise)
Для генерации наборов используются модели PICT.
- Модель Authorization: `model.txt` -> Результат: `cases.txt` (25 строк)
- Модель Card-Management: `model-card-management.txt` -> Результат: `cases-card-management.txt` (16 строк)

## 4. Тест-кейсы (Test Cases)

### 4.1. Тест-кейсы для Authorization (на основе cases.txt)

| ID | Требование | Вид | Предусловие | Шаги | Ожидаемый результат | Источник |
|---|---|---|---|---|---|---|
| TC-AUTH-01 | tz/04 | Негативный | card_status=EXPIRED, expiry=valid | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=pos, mcc=restaurant | Отказ (DECLINED) | PICT row 1 |
| TC-AUTH-02 | tz/04 | Негативный | card_status=BLOCKED, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=atm, mcc=grocery | Отказ (DECLINED) | PICT row 2 |
| TC-AUTH-03 | tz/04 | Негативный | card_status=INACTIVE, expiry=expired | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=ecom, mcc=grocery | Отказ (DECLINED) | PICT row 3 |
| TC-AUTH-04 | tz/04 | Негативный | card_status=INACTIVE, expiry=valid | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=atm, mcc=travel | Отказ (DECLINED) | PICT row 4 |
| TC-AUTH-05 | tz/04 | Негативный | card_status=EXPIRED, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=ecom, mcc=electronics | Отказ (DECLINED) | PICT row 5 |
| TC-AUTH-06 | tz/04 | Негативный | card_status=BLOCKED, expiry=expired | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=pos, mcc=electronics | Отказ (DECLINED) | PICT row 6 |
| TC-AUTH-07 | tz/04 | Негативный | card_status=EXPIRED, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=pos, mcc=grocery | Отказ (DECLINED) | PICT row 7 |
| TC-AUTH-08 | tz/04 | Негативный | card_status=EXPIRED, expiry=expired | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=atm, mcc=travel | Отказ (DECLINED) | PICT row 8 |
| TC-AUTH-09 | tz/04 | Негативный | card_status=ACTIVE, expiry=valid | Транзакция: amount_vs_daily=equal, monthly=above, balance=above, terminal=ecom, mcc=travel | Отказ (DECLINED) — превышен месячный лимит и баланс | PICT row 9 |
| TC-AUTH-10 | tz/04 | Негативный | card_status=ACTIVE, expiry=current_month | Транзакция: amount_vs_daily=above, monthly=equal, balance=equal, terminal=pos, mcc=travel | Отказ (DECLINED) — превышен дневной лимит | PICT row 10 |
| TC-AUTH-11 | tz/04 | Негативный | card_status=ACTIVE, expiry=expired | Транзакция: amount_vs_daily=equal, monthly=equal, balance=below, terminal=atm, mcc=electronics | Отказ (DECLINED) — истек срок действия | PICT row 11 |
| TC-AUTH-12 | tz/04 | Негативный | card_status=ACTIVE, expiry=valid | Транзакция: amount_vs_daily=above, monthly=above, balance=equal, terminal=atm, mcc=grocery | Отказ (DECLINED) — превышены лимиты | PICT row 12 |
| TC-AUTH-13 | tz/04 | Негативный | card_status=ACTIVE, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=equal, balance=above, terminal=atm, mcc=restaurant | Отказ (DECLINED) — превышен баланс | PICT row 13 |
| TC-AUTH-14 | tz/04 | Негативный | card_status=ACTIVE, expiry=valid | Транзакция: amount_vs_daily=above, monthly=above, balance=above, terminal=pos, mcc=electronics | Отказ (DECLINED) — превышены все лимиты и баланс | PICT row 14 |
| TC-AUTH-15 | tz/04 | Негативный | card_status=INACTIVE, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=ecom, mcc=restaurant | Отказ (DECLINED) | PICT row 15 |
| TC-AUTH-16 | tz/04 | Негативный | card_status=ACTIVE, expiry=current_month | Транзакция: amount_vs_daily=equal, monthly=above, balance=equal, terminal=ecom, mcc=restaurant | Отказ (DECLINED) — превышен месячный лимит | PICT row 16 |
| TC-AUTH-17 | tz/04 | Негативный | card_status=ACTIVE, expiry=expired | Транзакция: amount_vs_daily=below, monthly=above, balance=below, terminal=pos, mcc=electronics | Отказ (DECLINED) — истек срок и превышен лимит | PICT row 17 |
| TC-AUTH-18 | tz/04 | Негативный | card_status=BLOCKED, expiry=valid | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=ecom, mcc=travel | Отказ (DECLINED) | PICT row 18 |
| TC-AUTH-19 | tz/04 | Негативный | card_status=ACTIVE, expiry=valid | Транзакция: amount_vs_daily=above, monthly=equal, balance=above, terminal=ecom, mcc=grocery | Отказ (DECLINED) — превышен дневной лимит и баланс | PICT row 19 |
| TC-AUTH-20 | tz/04 | Позитивный | card_status=ACTIVE, expiry=current_month | Транзакция: amount_vs_daily=equal, monthly=below, balance=equal, terminal=pos, mcc=grocery | Одобрено (APPROVED) — все в пределах границ | PICT row 20 |
| TC-AUTH-21 | tz/04 | Негативный | card_status=ACTIVE, expiry=expired | Транзакция: amount_vs_daily=above, monthly=below, balance=below, terminal=pos, mcc=restaurant | Отказ (DECLINED) — истек срок и превышен дневной лимит | PICT row 21 |
| TC-AUTH-22 | tz/04 | Негативный | card_status=BLOCKED, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=pos, mcc=restaurant | Отказ (DECLINED) | PICT row 22 |
| TC-AUTH-23 | tz/04 | Негативный | card_status=ACTIVE, expiry=valid | Транзакция: amount_vs_daily=below, monthly=below, balance=above, terminal=pos, mcc=electronics | Отказ (DECLINED) — превышен баланс | PICT row 23 |
| TC-AUTH-24 | tz/04 | Позитивный | card_status=ACTIVE, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=equal, balance=equal, terminal=pos, mcc=electronics | Одобрено (APPROVED) — все в пределах границ | PICT row 24 |
| TC-AUTH-25 | tz/04 | Негативный | card_status=INACTIVE, expiry=current_month | Транзакция: amount_vs_daily=below, monthly=below, balance=below, terminal=pos, mcc=electronics | Отказ (DECLINED) | PICT row 25 |

### 4.2. Тест-кейсы для Card-Management (на основе cases-card-management.txt)

| ID | Требование | Вид | Предусловие | Шаги | Ожидаемый результат | Источник |
|---|---|---|---|---|---|---|
| TC-CM-01 | tz/05 | Негативный | user_role=user, channel=mobile | operation_type=update, pan_validity=too_short, expiry_format=past_date, card_status=inactive | Ошибка валидации (400 Bad Request) | PICT row 1 |
| TC-CM-02 | tz/05 | Негативный | user_role=admin, channel=api | operation_type=delete, pan_validity=too_long, expiry_format=invalid_month, card_status=active | Ошибка валидации (400 Bad Request) | PICT row 2 |
| TC-CM-03 | tz/05 | Позитивный | user_role=guest, channel=web | operation_type=read, pan_validity=too_short, expiry_format=invalid_month, card_status=blocked | Успех (200 OK) — гость может только читать | PICT row 3 |
| TC-CM-04 | tz/05 | Негативный | user_role=user, channel=mobile | operation_type=delete, pan_validity=valid, expiry_format=valid_mmyy, card_status=blocked | Ошибка доступа (403 Forbidden) — user не может удалять | PICT row 4 |
| TC-CM-05 | tz/05 | Негативный | user_role=admin, channel=web | operation_type=delete, pan_validity=luhn_fail, expiry_format=past_date, card_status=inactive | Ошибка валидации (400 Bad Request) | PICT row 5 |
| TC-CM-06 | tz/05 | Негативный | user_role=user, channel=web | operation_type=update, pan_validity=too_long, expiry_format=valid_mmyy, card_status=active | Ошибка валидации (400 Bad Request) | PICT row 6 |
| TC-CM-07 | tz/05 | Позитивный | user_role=admin, channel=api | operation_type=create, pan_validity=valid, expiry_format=valid_mmyy, card_status=inactive | Успех (201 Created) | PICT row 7 |
| TC-CM-08 | tz/05 | Негативный | user_role=admin, channel=mobile | operation_type=create, pan_validity=too_long, expiry_format=past_date, card_status=blocked | Ошибка валидации (400 Bad Request) | PICT row 8 |
| TC-CM-09 | tz/05 | Негативный | user_role=user, channel=api | operation_type=delete, pan_validity=too_short, expiry_format=past_date, card_status=active | Ошибка валидации (400 Bad Request) | PICT row 9 |
| TC-CM-10 | tz/05 | Позитивный | user_role=guest, channel=mobile | operation_type=read, pan_validity=luhn_fail, expiry_format=invalid_month, card_status=active | Успех (200 OK) | PICT row 10 |
| TC-CM-11 | tz/05 | Негативный | user_role=admin, channel=api | operation_type=update, pan_validity=valid, expiry_format=invalid_month, card_status=blocked | Ошибка валидации (400 Bad Request) | PICT row 11 |
| TC-CM-12 | tz/05 | Позитивный | user_role=guest, channel=api | operation_type=read, pan_validity=valid, expiry_format=past_date, card_status=inactive | Успех (200 OK) | PICT row 12 |
| TC-CM-13 | tz/05 | Негативный | user_role=user, channel=api | operation_type=read, pan_validity=too_long, expiry_format=invalid_month, card_status=inactive | Ошибка валидации (400 Bad Request) | PICT row 13 |
| TC-CM-14 | tz/05 | Позитивный | user_role=admin, channel=web | operation_type=create, pan_validity=too_short, expiry_format=valid_mmyy, card_status=active | Ошибка валидации (400 Bad Request) | PICT row 14 |
| TC-CM-15 | tz/05 | Позитивный | user_role=admin, channel=web | operation_type=read, pan_validity=luhn_fail, expiry_format=valid_mmyy, card_status=blocked | Успех (200 OK) — админ может читать | PICT row 15 |
| TC-CM-16 | tz/05 | Негативный | user_role=user, channel=api | operation_type=read, pan_validity=luhn_fail, expiry_format=invalid_month, card_status=inactive | Ошибка валидации (400 Bad Request) | PICT row 16 |
