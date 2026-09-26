#!/usr/bin/env bash
set -euo pipefail

base_url="${BANKING_URL:-http://localhost:8082}"
request_id="bank-poc-$(date +%s)"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

status="$(curl -sS -o "$tmp_dir/account.json" -w '%{http_code}' \
  "$base_url/api/v1/accounts/ACC-001" -H "Request-ID: $request_id")"
test "$status" = "200"
grep -q '"accountId":"ACC-001"' "$tmp_dir/account.json"

status="$(curl -sS -o "$tmp_dir/transfer.json" -w '%{http_code}' \
  "$base_url/api/v1/transfers" -H "Request-ID: $request_id" -H 'Content-Type: application/json' \
  -d '{"fromAccount":"ACC-001","toAccount":"ACC-002","amount":100000,"currency":"VND","reference":"ORDER-SUCCESS-001"}')"
test "$status" = "201"
grep -q '"transferId":"BANK-TXN-001"' "$tmp_dir/transfer.json"

status="$(curl -sS -o "$tmp_dir/missing.json" -w '%{http_code}' \
  "$base_url/api/v1/accounts/MISSING" -H 'Accept-Language: vi')"
test "$status" = "404"
grep -q 'BNK-ACCOUNT-001' "$tmp_dir/missing.json"

status="$(curl -sS -o "$tmp_dir/rejected.json" -w '%{http_code}' \
  "$base_url/api/v1/transfers" -H 'Content-Type: application/json' \
  -d '{"fromAccount":"ACC-001","toAccount":"ACC-002","amount":100000,"currency":"VND","reference":"ORDER-REJECTED-001"}')"
test "$status" = "422"
grep -q 'BNK-TRANSFER-001' "$tmp_dir/rejected.json"

echo "Banking API and bank WireMock smoke test passed"
