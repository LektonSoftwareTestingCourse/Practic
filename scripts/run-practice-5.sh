#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE=(docker compose -f docker-compose.yaml -f docker-compose.practice5.yaml)
KEEP_SMP_STACK="${KEEP_SMP_STACK:-0}"
ENV_FILE=".env"

if [[ ! -f "$ENV_FILE" ]]; then
  cp .env.example "$ENV_FILE"
  echo "Created .env from .env.example. Review credentials and ports before production use."
fi

env_value() {
  local key="$1" fallback="$2" value
  value="$(grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d '=' -f 2- || true)"
  printf '%s' "${value:-$fallback}"
}

GATEWAY_PORT="$(env_value GATEWAY_PORT 8080)"
SWITCH_PORT="$(env_value SWITCH_PORT 8082)"
AUTH_PORT="$(env_value AUTH_PORT 8083)"
LOGGER_PORT="$(env_value LOGGER_PORT 8088)"
TERMINAL_PORT="$(env_value TERMINAL_PORT 8085)"
MERCHANT_PORT="$(env_value MERCHANT_PORT 8084)"

export SMP_GATEWAY_URL="${SMP_GATEWAY_URL:-http://localhost:${GATEWAY_PORT}}"
export SMP_SWITCH_URL="${SMP_SWITCH_URL:-http://localhost:${SWITCH_PORT}}"
export SMP_AUTHORIZATION_URL="${SMP_AUTHORIZATION_URL:-http://localhost:${AUTH_PORT}}"
export SMP_LOGGER_URL="${SMP_LOGGER_URL:-http://localhost:${LOGGER_PORT}}"
HEALTH_URL="${SMP_GATEWAY_URL%/}/health"

cleanup() {
  local code=$?
  trap - EXIT
  if (( code != 0 )); then
    echo "Practice 5 failed. Recent container logs:"
    "${COMPOSE[@]}" logs --no-color --tail=160 || true
  fi
  if [[ "$KEEP_SMP_STACK" != "1" ]]; then
    "${COMPOSE[@]}" down --remove-orphans || true
  else
    echo "KEEP_SMP_STACK=1: containers left running."
  fi
  exit "$code"
}
trap cleanup EXIT

command -v docker >/dev/null || { echo "Docker is required." >&2; exit 1; }
command -v mvn >/dev/null || { echo "Maven is required." >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required." >&2; exit 1; }

"${COMPOSE[@]}" up -d --build

echo "Waiting for complete Gateway health: $HEALTH_URL"
ready=0
for attempt in $(seq 1 120); do
  status="$(curl -sS -o /tmp/smp-practice5-health.json -w '%{http_code}' "$HEALTH_URL" || true)"
  if [[ "$status" == "200" ]]; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "$ready" != "1" ]]; then
  echo "Gateway did not become healthy within 240 seconds. Last response:"
  cat /tmp/smp-practice5-health.json 2>/dev/null || true
  exit 1
fi

# Gateway's aggregate health omits the two simulator services; wait for them explicitly.
for service in "terminal:${TERMINAL_PORT}" "merchant:${MERCHANT_PORT}"; do
  name="${service%%:*}"
  port="${service##*:}"
  url="http://localhost:${port}/health"
  service_ready=0
  echo "Waiting for ${name} simulator: ${url}"
  for attempt in $(seq 1 120); do
    status="$(curl -sS -o "/tmp/smp-practice5-${name}-health.json" -w '%{http_code}' "$url" || true)"
    if [[ "$status" == "200" ]]; then
      service_ready=1
      break
    fi
    sleep 2
  done
  if [[ "$service_ready" != "1" ]]; then
    echo "${name} simulator did not become healthy within 240 seconds."
    cat "/tmp/smp-practice5-${name}-health.json" 2>/dev/null || true
    exit 1
  fi
done

REPETITIONS="${PRACTICE5_REPETITIONS:-3}"
if ! [[ "$REPETITIONS" =~ ^[1-9][0-9]*$ ]]; then
  echo "PRACTICE5_REPETITIONS must be a positive integer." >&2
  exit 1
fi
for run in $(seq 1 "$REPETITIONS"); do
  echo "Practice 5 API test run ${run}/${REPETITIONS}"
  mvn -B -f services/pom.xml -P api-tests verify
done

echo "Practice 5 API tests passed. Surefire reports: services/api-tests/target/surefire-reports/"
