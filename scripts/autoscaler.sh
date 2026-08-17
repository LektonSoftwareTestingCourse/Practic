#!/bin/bash

SERVICE_NAME="terminal-simulator"

RPS_PER_INSTANCE=200
MIN_CONTAINERS=1
MAX_CONTAINERS=10

echo "Автоскейлер запущен"

while true; do
  RESPONSE=$(curl -s -G "http://prometheus:9090/api/v1/query" --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{application="gateway",uri!~"/docs.*",uri!~"/actuator.*",uri!~"/health.*"}[1m]))')

  RPS=$(echo "$RESPONSE" | jq -r '.data.result[0]?.value[1] // 0 | tonumber | floor')

  echo "Текущая нагрузка на Gateway: $RPS RPS"

  NEEDED_CONTAINERS=$(( (RPS + RPS_PER_INSTANCE - 1) / RPS_PER_INSTANCE ))

  if [ "$NEEDED_CONTAINERS" -lt "$MIN_CONTAINERS" ]; then
    NEEDED_CONTAINERS=$MIN_CONTAINERS
  fi

  if [ "$NEEDED_CONTAINERS" -gt "$MAX_CONTAINERS" ]; then
    NEEDED_CONTAINERS=$MAX_CONTAINERS
  fi

  CURRENT=$(docker compose ps $SERVICE_NAME --format "{{.State}}" 2>/dev/null | grep "running" | wc -l)

  if [ "$NEEDED_CONTAINERS" -ne "$CURRENT" ]; then
    echo "Масштабируем: $CURRENT -> $NEEDED_CONTAINERS"
    docker compose up -d --no-deps --no-recreate --scale "$SERVICE_NAME"="$NEEDED_CONTAINERS"
  else
    echo "Количество контейнеров не изменилось ($CURRENT)"
  fi

  sleep 15

done
