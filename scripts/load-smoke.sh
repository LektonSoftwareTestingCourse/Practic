#!/bin/bash
# Load smoke test — студент дорабатывает под свою практику 6
#
# ЛОКАЛЬНО (студент):
#   LOAD_COUNT=500 ./scripts/load-smoke.sh
#   Собирает метрики (p50/p95/p99, throughput, error rate) и пишет отчёт.
#
# В CI (автопроверка):
#   CI запускает скрипт с LOAD_COUNT=10 — smoke-проверка, что скрипт не сломан.
#   Реальные нагрузочные метрики — только локально.
#
# Студент сам определяет: сценарий, количество транзакций, собираемые метрики.
# Если скрипт отсутствует в ветке — CI выведет предупреждение, но не зафейлит билд.

LOAD_COUNT="${LOAD_COUNT:-500}"

echo "Running load smoke test (count=${LOAD_COUNT})..."

# Пример: N транзакций через terminal simulator
curl -X POST http://localhost:8080/api/simulator/terminal/run \
  -H "Content-Type: application/json" \
  -d "{\"count\": ${LOAD_COUNT}, \"scenario\": \"mixed\"}"

echo "Load smoke completed."
