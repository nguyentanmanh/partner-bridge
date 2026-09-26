# PartnerBridge

- [Canonical Payment API v1](contracts/payment/v1/README.md)
- [Canonical Banking API v1](contracts/banking/v1/README.md)
- [Local payment provider stubs](provider-stubs/README.md)
- [Kong Gateway DB-less POC](gateways/kong/README.md)

## Architecture Documents

- [PartnerBridge architecture](docs/architecture/README.md)

## Onboarding

- [Kong hoạt động thế nào trong PartnerBridge?](docs/onboarding/how-kong-works.md)
- [Lộ trình học Kong trong PartnerBridge](docs/learning/README.md)

## Chạy local

Luồng request mặc định:

```text
Client → Kong (:8000) → payment-integration-service → PROVIDER_A / PROVIDER_B
```

Kong xác thực API key, chuyển identity của consumer xuống backend, gắn request ID
và giới hạn số request. Payment service vẫn chịu trách nhiệm validate canonical
request, xử lý idempotency, chọn provider, chuyển đổi dữ liệu và trả lỗi nghiệp vụ.

```bash
docker compose up -d --build
docker compose ps
provider-stubs/smoke-test.sh
scripts/gateway-smoke-test.sh
docker compose down
```

Kong proxy được publish tại cổng `8000`; Admin API chỉ bind vào
`127.0.0.1:8001`. Hai cổng `9101` và `9102` của provider stub được giữ lại để
chạy smoke test. Payment service không mở cổng HTTP ra host trong cấu hình
Compose mặc định.

Xem [Kong Gateway DB-less POC](gateways/kong/README.md) để biết API key dùng ở
local, ví dụ `curl`, cách reload cấu hình và các giới hạn hiện tại. Đây là môi
trường học và thử nghiệm local, không phải cấu hình production.
