# Quality attributes — scenarios and evidence

| Attribute / scenario | Current design and evidence | POC limit |
| --- | --- | --- |
| Extensibility: thêm provider C mà không đổi canonical client payload | `PaymentProviderPort` và `PaymentService` route bằng `providerCode`; A/B có adapter, HTTP interface, DTO, mapper riêng trong `adapter.out.client`. [ADR-002](adr/ADR-002-hexagonal-service-architecture.md) | Enum/validation và OpenAPI vẫn phải được version/review; chưa có plugin discovery tự động. |
| Portability: thay Kong nhưng giữ Payment API | [OpenAPI](../../contracts/payment/v1/openapi.yaml), domain và application không import Kong; Kong chỉ ở Compose/YAML. [ADR-003](adr/ADR-003-gateway-responsibility-boundary.md) | Edge authentication/identity/correlation/rate policy phải tái tạo và kiểm thử trên gateway mới. |
| Reliability: concurrent replay cùng key không tạo hai provider calls | `PaymentService` reserve cả key/reference dưới lock ngắn, chờ per-entry future, lưu success/error; `PaymentServiceTest` và `PaymentApiRegressionTest` kiểm call count, TTL và conflict. [ADR-005](adr/ADR-005-idempotency-and-unknown-outcome.md) | Chỉ một process; restart mất state, unknown outcome chưa reconciliation, response TTL 24h. |
| Security: client không tự chọn scope | Kong key-auth + transformer ghi trusted ID; backend gateway profile bắt buộc header; gateway smoke thử spoof và consumer isolation. [Security](security-and-trust.md) | Không có mTLS/secret manager, chỉ dummy keys và Compose boundary. |
| Observability: lần gọi timeout không bị log như payment thất bại | `RequestIdFilter` gắn MDC; `ProviderCallSupport` log provider, ID, duration, call outcome và payment status riêng; adapter test kiểm `TIMEOUT`/`INVALID_RESPONSE`, không log secret. | Chưa có centralized audit/telemetry export hay production tracing pipeline. |
| Testability: thay provider và Clock để chứng minh edge cases | `PaymentServiceTest` inject fake port/MutableClock; `PaymentApiRegressionTest` kiểm REST/concurrency; `ArchitectureTest` giữ boundary; provider/gateway smoke chạy qua WireMock/Kong. | Smoke phụ thuộc Docker local; tests không chứng minh distributed behavior hoặc provider thật. |
| Maintainability: sửa payload B không đổi A/domain | `providera` và `providerb` cô lập DTO/client/mapper; `PaymentProviderAdapterTest` kiểm mapping và transport errors. [ADR-006](adr/ADR-006-provider-http-client.md) | Shared `ProviderCallSupport` vẫn cần review khi thêm loại lỗi/provider mới. |

Các scenario trên là thuộc tính được **hỗ trợ trong phạm vi POC**, không phải SLO hay bảo đảm production.
