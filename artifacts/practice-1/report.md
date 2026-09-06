![alt text](telegram-cloud-photo-size-2-5256144457197888275-y.jpg)

![alt text](telegram-cloud-photo-size-2-5256144457197888275-y-1.jpg)

![alt text](telegram-cloud-photo-size-2-5256144457197888277-y.jpg)


### рабит мку
![alt text](image-1.png)

### Курлы:

curl -X POST http://localhost:8080/api/cards/generate \
    -H "Content-Type: application/json" \
    -d '{
      "count": 100,
      "bins": ["400000", "400001", "400002", "400003", "400004"]
    }'



curl -X POST http://localhost:8080/api/simulator/terminal/run \
    -H "Content-Type: application/json" \
    -d '{
      "count": 1000,
      "scenario": "normal",
      "tps": 100
    }'

Видим, что транзакции сгенерились ![alt text](image.png)