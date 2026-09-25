# Чек-листы для периферийных сервисов

## Smoke-тестирование (запускать после каждого `docker-compose up`)

- [ ] `docker-compose up -d` завершился без ошибок
- [ ] `docker-compose ps` - все 11 сервисов + PostgreSQL + RabbitMQ в статусе `Up`
- [ ] `GET http://gateway:8080/health` возвращает `status: ok` и все downstream `ok`
- [ ] RabbitMQ Management UI доступен
- [ ] `POST http://card-management:8081/api/cards/generate` с count=10 возвращает 10 карт

## Критический путь (critical path)

### Сценарий 1: Создание карты и простая покупка
- [ ] `POST /api/cards` → создана карта, статус ACTIVE
- [ ] `POST /api/simulator/terminal/run` с scenario=normal, count=5
- [ ] В ответе `approved > 0`
- [ ] `GET /api/transactions/search?pan={pan}` - транзакции найдены в Logger
- [ ] В Web Dashboard (`http://localhost:3000`) транзакция появилась в таблице

### Сценарий 2: Покупка в интернет-магазине
- [ ] `POST /api/simulator/merchant/run` с scenario=electronics, count=3
- [ ] Транзакции прошли, MCC=5732
- [ ] Комиссия эквайрера рассчиталась (можно проверить в логах)

### Сценарий 3: Превышение дневного лимита → decline
- [ ] Создать карту с dailyLimit=100000 (1000 ₽)
- [ ] Провести транзакцию на 1000 ₽ → APPROVED
- [ ] Провести транзакцию на 1 ₽ → DECLINED, responseCode="61"
- [ ] В логах видно причину decline

### Сценарий 4: Недостаточно средств
- [ ] Карта с balance=10000
- [ ] Транзакция на 10001 → DECLINED, responseCode="51"

### Сценарий 5: Заблокированная карта
- [ ] `PATCH /api/cards/{pan}` → status=BLOCKED
- [ ] Любая транзакция → DECLINED, responseCode="CARD_BLOCKED"

### Сценарий 6: Истёкшая карта
- [ ] Карта с expiryDate в прошлом месяце
- [ ] Транзакция → DECLINED, responseCode="54"

### Сценарий 7: Rate limiting в Gateway
- [ ] Отправить 100+ запрос в секунду на `/api/transactions`
- [ ] Понять, что rate limit работает

### Сценарий 8: RabbitMQ unavailable → reversal
- [ ] Остановить RabbitMQ (`docker-compose stop rabbitmq`)
- [ ] Провести транзакцию
- [ ] Switch должен сделать reversal (mti=0400) в Authorization
- [ ] Клиенту вернулся DECLINED с responseCode="96"
- [ ] Запустить RabbitMQ обратно

### Сценарий 9: Publisher Confirms - успешная публикация
- [ ] Провести транзакцию при работающем RabbitMQ
- [ ] В RabbitMQ Management UI видно: сообщение ушло в `transaction-log`, Ack получен
- [ ] В Logger запись появилась

### Сценарий 10: WebSocket real-time в Dashboard
- [ ] Открыть Dashboard в браузере
- [ ] Открыть DevTools → Network → WS, проверить подключение к `/ws/transactions`
- [ ] Провести транзакцию через симулятор
- [ ] В таблице Dashboard запись появилась **без перезагрузки страницы** (< 1 сек)

### Сценарий 11: Поиск и фильтрация в Logger
- [ ] `GET /api/transactions/search?status=DECLINED` → только decline
- [ ] `GET /api/transactions/search?mcc=5411&status=APPROVED` → комбинация фильтров работает
- [ ] Пагинация: `?limit=5&offset=10` возвращает корректную порцию