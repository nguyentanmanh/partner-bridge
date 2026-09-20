# PartnerBridge architecture — current POC

PartnerBridge cho client một Payment API canonical, che khác biệt giữa hai payment provider giả lập. Đây là POC local để thử boundary tích hợp, không phải platform production-ready.

**Implemented:** client gọi Kong → `payment-integration-service` → WireMock `PROVIDER_A` hoặc `PROVIDER_B`. OpenAPI v1 là contract chung; Kong DB-less xử lý edge policy, service xử lý payment, provider mapping và in-memory idempotency. Service vẫn có sample Item API do archetype tạo; nó không nằm trong Payment API.

**POC limitation:** một backend instance, payment/idempotency trong memory, rate limit Kong local, credential giả lập, chưa có reconciliation và production security. **Proposed/roadmap:** persistence, inquiry/reconciliation, gateway portability và TAD plan/diff. TAD chỉ được đánh giá, không nằm trên runtime path.

| Tài liệu | Mục đích |
| --- | --- |
| [System context](system-context.md) | Actor, hệ thống ngoài và ranh giới current state |
| [Container architecture](container-architecture.md) | Trách nhiệm Kong, service, stubs và contract |
| [Payment service](payment-service.md) | Hexagonal packages và bốn sequence thực tế |
| [Runtime deployment](runtime-deployment.md) | Compose topology, port, health và cấu hình |
| [Security and trust](security-and-trust.md) | Identity, trust boundaries, giới hạn bảo mật |
| [Quality attributes](quality-attributes.md) | Scenario, bằng chứng và trade-off |
| [Limitations and roadmap](limitations-and-roadmap.md) | Current gaps và đề xuất tiếp theo |
| [Architecture decisions](adr/README.md) | Sáu quyết định đã triển khai |

Nguồn chuẩn: [OpenAPI](../../contracts/payment/v1/openapi.yaml), [Compose](../../compose.yaml), [Kong declarative config](../../gateways/kong/kong.yml) và [service source](../../services/payment-integration-service/README.md). Các diagram dưới đây mô tả **current state**, trừ nơi ghi rõ roadmap.
