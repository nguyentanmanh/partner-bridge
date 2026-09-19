# PartnerBridge payment provider stubs

Hai WireMock stub này mô phỏng hai payment provider có contract khác canonical
Payment API. Chúng dùng cho phát triển adapter và E2E test của POC, không dùng
cho production.

## Khác biệt giữa hai provider

| Đặc điểm | PROVIDER_A | PROVIDER_B |
| --- | --- | --- |
| Port local | `9101` | `9102` |
| Authentication | `X-Api-Key` | `X-Client-Id` và `X-Signature` |
| Create endpoint | `POST /v1/payments` | `POST /api/payment/create` |
| Query endpoint | `GET /v1/payments/{transactionId}` | `POST /api/payment/query` |
| Amount | JSON number | String trong object `money` |
| Reference | `merchantRef` | `request.orderRef` |
| Description | `note` | `request.content` |
| Status | `COMPLETED`, `PROCESSING`, `REJECTED` | `S`, `P`, `F` |

Health endpoint là `GET /health` trên mỗi provider và không yêu cầu header.

## Header bắt buộc

PROVIDER_A yêu cầu:

```text
X-Api-Key: provider-a-local-key
```

PROVIDER_B yêu cầu:

```text
X-Client-Id: provider-b-local-client
X-Signature: local-test-signature
```

Các giá trị trên chỉ là test data local. PROVIDER_B chỉ kiểm tra `X-Signature`
tồn tại và gồm các ký tự hợp lệ; stub không cryptographically verify HMAC.

## Scenario xác định

Create mapping chọn response theo chuỗi có trong `merchantRef` của PROVIDER_A
hoặc `request.orderRef` của PROVIDER_B:

- `SUCCESS`: trả trạng thái thành công (`COMPLETED` hoặc `S`).
- `PENDING`: trả trạng thái đang xử lý (`PROCESSING` hoặc `P`).
- `FAIL`: trả HTTP `422` với provider-specific business error.
- `TIMEOUT`: giữ response khoảng 5,5 giây.
- `MALFORMED`: trả HTTP `200` nhưng thiếu field bắt buộc.

Request có authentication hợp lệ nhưng không khớp cấu trúc cơ bản trả HTTP
`400`. Thiếu hoặc sai authentication trả HTTP `401`. Mapping scenario có
priority cao hơn mapping mặc định; mapping authentication có priority cao nhất.

Query fixtures có sẵn:

- PROVIDER_A: `PA-TXN-001`, `PA-TXN-PENDING`, `PA-TXN-FAILED`.
- PROVIDER_B: `PB-REF-001`, `PB-REF-PENDING`, `PB-REF-FAILED`.

## Chạy bằng Docker Compose

Từ root repository:

```bash
docker compose up -d provider-a provider-b
docker compose ps
docker compose logs provider-a
docker compose logs provider-b
docker compose down
```

Compose pin image `wiremock/wiremock:3.13.2`, mount mapping bằng relative path và
đặt hai container trong network `partner-bridge`.

## Ví dụ curl

Tạo payment với PROVIDER_A:

```bash
curl --request POST http://localhost:9101/v1/payments \
  --header 'X-Api-Key: provider-a-local-key' \
  --header 'Content-Type: application/json' \
  --data '{"merchantRef":"ORDER-SUCCESS-001","amount":100000,"currency":"VND","note":"Test payment"}'
```

Query PROVIDER_A:

```bash
curl http://localhost:9101/v1/payments/PA-TXN-001 \
  --header 'X-Api-Key: provider-a-local-key'
```

Tạo payment với PROVIDER_B:

```bash
curl --request POST http://localhost:9102/api/payment/create \
  --header 'X-Client-Id: provider-b-local-client' \
  --header 'X-Signature: local-test-signature' \
  --header 'Content-Type: application/json' \
  --data '{"request":{"orderRef":"ORDER-SUCCESS-001","money":{"amount":"100000","currencyCode":"VND"},"content":"Test payment"}}'
```

Query PROVIDER_B:

```bash
curl --request POST http://localhost:9102/api/payment/query \
  --header 'X-Client-Id: provider-b-local-client' \
  --header 'X-Signature: local-test-signature' \
  --header 'Content-Type: application/json' \
  --data '{"referenceNo":"PB-REF-001"}'
```

## Smoke test

Khi hai container đã healthy:

```bash
./provider-stubs/smoke-test.sh
```

Script kiểm tra health, success/pending/fail, authentication failure và xác nhận
scenario timeout kết thúc bằng curl exit code `28`. Có thể override base URL bằng
`PROVIDER_A_URL` và `PROVIDER_B_URL`.

## Request journal

WireMock lưu request journal trong memory. Xem journal qua admin API:

```bash
curl http://localhost:9101/__admin/requests
curl http://localhost:9102/__admin/requests
```

Reset journal mà không đổi mapping:

```bash
curl --request DELETE http://localhost:9101/__admin/requests
curl --request DELETE http://localhost:9102/__admin/requests
```

Admin API chỉ dành cho môi trường local test. Stub không mô phỏng persistence,
HMAC verification, retry, rate limiting hoặc hành vi production khác.
