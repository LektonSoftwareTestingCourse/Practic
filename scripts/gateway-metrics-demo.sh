#!/bin/bash

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
CARD_CACHE_PATH="${CARD_CACHE_PATH:-/api/cards}"
RATE_LIMIT_REQUESTS="${RATE_LIMIT_REQUESTS:-160}"
RATE_LIMIT_CONCURRENCY="${RATE_LIMIT_CONCURRENCY:-16}"
RATE_LIMIT_CLIENT_IP="${RATE_LIMIT_CLIENT_IP:-198.51.100.$((RANDOM % 200 + 1))}"

VALID_TRANSACTION='{
  "mti": "0100",
  "stan": "000001",
  "pan": "4000003458730237",
  "processingCode": "000000",
  "amount": 1000,
  "currencyCode": "643",
  "transmissionDateTime": "2026-06-05T18:12:49.07Z",
  "terminalId": "TERM001",
  "terminalType": "POS",
  "merchantId": "MERCH00000000029",
  "mcc": "5045",
  "acquirerId": "ACQ002"
}'

INVALID_TRANSACTION='{
  "mti": "0100",
  "stan": "000001",
  "pan": "400000345873023",
  "processingCode": "000000",
  "amount": 1000,
  "currencyCode": "643",
  "transmissionDateTime": "2026-06-05T18:12:49.07Z",
  "terminalId": "TERM001",
  "terminalType": "POS",
  "merchantId": "MERCH00000000029",
  "mcc": "5045",
  "acquirerId": "ACQ002"
}'

INVALID_JSON='{"mti": "0100",'

post_transaction() {
  local body="$1"
  local client_ip="${2:-}"

  if [ -n "$client_ip" ]; then
    curl -s -o /dev/null -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -H "X-Forwarded-For: $client_ip" \
      -X POST \
      --data "$body" \
      "$GATEWAY_URL/api/transactions"
    return
  fi

  curl -s -o /dev/null -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -X POST \
    --data "$body" \
    "$GATEWAY_URL/api/transactions"
}

get_card_path() {
  curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL$CARD_CACHE_PATH"
}

print_rejection_metric() {
  local metrics="$1"
  local reason="$2"
  local metric_line

  metric_line="$(grep -E "^gateway_requests_rejected_total\\{.*reason=\"$reason\".*\\}" <<< "$metrics" || true)"

  if [ -n "$metric_line" ]; then
    echo "$metric_line"
    return
  fi

  echo "gateway_requests_rejected_total{reason=\"$reason\"} not exposed"
}

print_gateway_metrics() {
  local metrics

  echo
  echo "Gateway custom metrics from $GATEWAY_URL/actuator/prometheus:"
  metrics="$(curl -fsS "$GATEWAY_URL/actuator/prometheus" 2>/dev/null || true)"

  if [ -z "$metrics" ]; then
    echo "Cannot read actuator metrics. Check that gateway is running and /actuator/prometheus is exposed."
    return
  fi

  case "$metrics" in
    *gateway_requests_rejected*) ;;
    *)
      echo "No custom gateway metrics exposed yet."
      echo "Most likely the running gateway was not rebuilt/restarted after adding GatewayMetrics."
      echo "Rebuild and restart the gateway container/application, then run this script again."
      return
      ;;
  esac

  echo "Gateway rejection metrics:"
  print_rejection_metric "$metrics" "rate_limit"
  print_rejection_metric "$metrics" "shutting_down"
  print_rejection_metric "$metrics" "validation_invalid_json"
  print_rejection_metric "$metrics" "validation_invalid_request"

  echo
  echo "All gateway custom metrics:"
  echo "$metrics" \
    | grep -E 'gateway_(requests_rejected|downstream_unavailable|cache_requests|cache_invalidations)'
}

echo "Gateway metrics demo"
echo "Gateway URL: $GATEWAY_URL"
echo

echo "1. Trigger invalid JSON rejection"
invalid_json_status="$(post_transaction "$INVALID_JSON")"
echo "Invalid JSON request status: $invalid_json_status"

echo
echo "2. Trigger invalid request rejection"
invalid_request_status="$(post_transaction "$INVALID_TRANSACTION")"
echo "Invalid request status: $invalid_request_status"

echo
echo "3. Try to trigger rate-limit rejection"
echo "Sending $RATE_LIMIT_REQUESTS fast transaction requests with X-Forwarded-For=$RATE_LIMIT_CLIENT_IP."
echo "For a deterministic demo, run gateway with TRANSACTIONS_RATE_LIMIT=1."
rate_limited=0
rate_limit_statuses="$(
  seq 1 "$RATE_LIMIT_REQUESTS" \
    | xargs -n 1 -P "$RATE_LIMIT_CONCURRENCY" sh -c '
        curl -s -o /dev/null -w "%{http_code}\n" \
          -H "Content-Type: application/json" \
          -H "X-Forwarded-For: $2" \
          -X POST \
          --data "$1" \
          "$0/api/transactions"
      ' "$GATEWAY_URL" "$INVALID_JSON" "$RATE_LIMIT_CLIENT_IP"
)"
rate_limited="$(grep -c '^429$' <<< "$rate_limit_statuses" || true)"
echo "Rate-limited responses: $rate_limited"

echo
echo "4. Trigger cards cache events"
echo "Using CARD_CACHE_PATH=$CARD_CACHE_PATH"
first_cache_status="$(get_card_path)"
second_cache_status="$(get_card_path)"
echo "First cards request status: $first_cache_status"
echo "Second cards request status: $second_cache_status"
if [ "$first_cache_status" != "200" ] || [ "$second_cache_status" != "200" ]; then
  echo "Cards cache counters require a successful 200 GET response from Card Management."
  echo "Override CARD_CACHE_PATH if your local card endpoint differs."
fi

print_gateway_metrics

echo
echo "Grafana panels use Prometheus rates, so allow one scrape interval before expecting charts to move."
