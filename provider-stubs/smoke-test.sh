#!/usr/bin/env bash
set -euo pipefail

PROVIDER_A_URL="${PROVIDER_A_URL:-http://localhost:9101}"
PROVIDER_B_URL="${PROVIDER_B_URL:-http://localhost:9102}"
PROVIDER_A_KEY="provider-a-local-key"
PROVIDER_B_CLIENT="provider-b-local-client"
PROVIDER_B_SIGNATURE="local-test-signature"

pass() {
  printf 'PASS  %s\n' "$1"
}

fail() {
  printf 'FAIL  %s\n' "$1" >&2
  exit 1
}

assert_response() {
  local name="$1"
  local expected_status="$2"
  local expected_text="$3"
  shift 3

  local response body status
  response="$(curl --silent --show-error --write-out $'\n%{http_code}' "$@")" || fail "$name (curl failed)"
  status="${response##*$'\n'}"
  body="${response%$'\n'*}"

  [[ "$status" == "$expected_status" ]] || fail "$name (expected HTTP $expected_status, got $status; body: $body)"
  [[ "$body" == *"$expected_text"* ]] || fail "$name (response does not contain '$expected_text'; body: $body)"
  pass "$name"
}

printf 'PartnerBridge provider stub smoke tests\n'

assert_response "Provider A health" 200 '"provider":"PROVIDER_A"' \
  "$PROVIDER_A_URL/health"

assert_response "Provider B health" 200 '"provider":"PROVIDER_B"' \
  "$PROVIDER_B_URL/health"

assert_response "Provider A create SUCCESS" 201 '"paymentStatus":"COMPLETED"' \
  --request POST "$PROVIDER_A_URL/v1/payments" \
  --header "X-Api-Key: $PROVIDER_A_KEY" \
  --header 'Content-Type: application/json' \
  --data '{"merchantRef":"ORDER-SUCCESS-001","amount":100000,"currency":"VND","note":"Test payment"}'

assert_response "Provider A create PENDING" 201 '"paymentStatus":"PROCESSING"' \
  --request POST "$PROVIDER_A_URL/v1/payments" \
  --header "X-Api-Key: $PROVIDER_A_KEY" \
  --header 'Content-Type: application/json' \
  --data '{"merchantRef":"ORDER-PENDING-001","amount":100000,"currency":"VND","note":"Test payment"}'

assert_response "Provider B create SUCCESS" 200 '"status":"S"' \
  --request POST "$PROVIDER_B_URL/api/payment/create" \
  --header "X-Client-Id: $PROVIDER_B_CLIENT" \
  --header "X-Signature: $PROVIDER_B_SIGNATURE" \
  --header 'Content-Type: application/json' \
  --data '{"request":{"orderRef":"ORDER-SUCCESS-001","money":{"amount":"100000","currencyCode":"VND"},"content":"Test payment"}}'

assert_response "Provider B create FAIL" 422 '"error":"PAYMENT_REJECTED"' \
  --request POST "$PROVIDER_B_URL/api/payment/create" \
  --header "X-Client-Id: $PROVIDER_B_CLIENT" \
  --header "X-Signature: $PROVIDER_B_SIGNATURE" \
  --header 'Content-Type: application/json' \
  --data '{"request":{"orderRef":"ORDER-FAIL-001","money":{"amount":"100000","currencyCode":"VND"},"content":"Test payment"}}'

assert_response "Provider A authentication failure" 401 '"code":"UNAUTHORIZED"' \
  --request POST "$PROVIDER_A_URL/v1/payments" \
  --header 'Content-Type: application/json' \
  --data '{"merchantRef":"ORDER-SUCCESS-001","amount":100000,"currency":"VND","note":"Test payment"}'

printf 'TEST  Provider A TIMEOUT (curl must time out)\n'
set +e
curl --silent --show-error --max-time 2 \
  --request POST "$PROVIDER_A_URL/v1/payments" \
  --header "X-Api-Key: $PROVIDER_A_KEY" \
  --header 'Content-Type: application/json' \
  --data '{"merchantRef":"ORDER-TIMEOUT-001","amount":100000,"currency":"VND","note":"Test payment"}' \
  >/dev/null
timeout_exit=$?
set -e
[[ "$timeout_exit" -eq 28 ]] || fail "Provider A TIMEOUT (expected curl exit 28, got $timeout_exit)"
pass "Provider A TIMEOUT"

printf 'All smoke tests passed.\n'
