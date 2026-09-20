# ADR-003: Gateway xử lý edge; backend xử lý payment

- **Status:** Accepted for current POC
- **Date:** 2026-09-20

## Context

Kong là gateway POC hiện tại nhưng canonical contract và payment logic phải tồn tại độc lập với sản phẩm gateway.

## Decision

| Concern | Kong | Payment service |
| --- | --- | --- |
| Routing | Owner | Không |
| API key authentication | Owner | Nhận trusted identity |
| Rate limiting | Owner | Không |
| Correlation ID | Tạo/truyền | Log/sử dụng |
| Canonical validation | Không | Owner |
| Idempotency | Không | Owner |
| Provider routing | Không | Owner |
| Provider mapping | Không | Owner |
| Business error | Không | Owner |
| Gateway-generated `401`/`429` | Owner | Không |
| Provider timeout classification | Không | Owner |

Kong không biến đổi canonical JSON request/response body. `Idempotency-Key` đi qua Kong; backend giữ replay. Gateway-generated error body không bị giả làm canonical `ApiError`.

## Consequences

Thay gateway phải tái tạo edge policy và trusted identity propagation, nhưng không đổi provider mapping/use case. Backend gateway profile không fallback default client khi thiếu trusted header. Error ownership phải rõ trong E2E test.

## Alternatives considered

Đưa provider routing hoặc idempotency vào Kong: phụ thuộc plugin/state gateway và khó giữ behavior khi đổi gateway; không chọn. Để backend xử lý API key/rate limit trong POC: làm mờ edge boundary; không chọn.

## Evidence in repository

[Kong YAML](../../../gateways/kong/kong.yml), [gateway README](../../../gateways/kong/README.md), [PaymentController](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/adapter/in/rest/PaymentController.java), [gateway smoke](../../../scripts/gateway-smoke-test.py), [Compose](../../../compose.yaml).
