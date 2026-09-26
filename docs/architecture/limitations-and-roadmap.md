# Limitations and roadmap

## Current limitations

- Payment snapshots, idempotency response và merchant-reference reservation chỉ ở memory của **một** backend instance. Restart làm mất tất cả; reference set tăng trong vòng đời process. Không có durable/distributed idempotency.
- TTL mặc định 24h chỉ áp dụng response sau khi lần đầu hoàn tất; không tự xóa reference. Timeout/malformed giữ unknown communication outcome nhưng chưa có status inquiry/reconciliation; GET chỉ đọc snapshot đã tạo.
- Kong DB-less dùng declarative YAML mount read-only; Admin API entity CRUD trả `405`. Rate limit `policy: local` chỉ chính xác trên một Kong node. Chưa có workflow plan/diff/generate declarative config bằng TAD.
- Cả hai provider là WireMock stub, không phải third-party integration thật. Local API keys/signature là dữ liệu giả.
- Chưa có TLS/mTLS, secret manager, OAuth2/JWT/JWS, certificate rotation, centralized audit hoặc production network policy. Backend trusted identity header chưa có transport authentication riêng.
- Core, Way4 và IBFT chưa được kết nối. Refund, callback, settlement, Kafka và database cũng không thuộc current MVP.

## TAD status (research, not runtime)

[Kết quả đánh giá TAD](../tad-evaluation.md) ghi nhận build pass, nhưng full test
và lint của baseline vẫn còn lỗi. Không tìm thấy Kong connector;
`packages/api/gateway` là RPC nội bộ của TAD, **không phải** API Gateway adapter.
Kong DB-less Admin API không hỗ trợ entity CRUD; luồng OpenAPI → TAD plan/diff
→ Kong declarative config chưa được triển khai hoặc kiểm chứng. TAD không phải
runtime dependency của PartnerBridge.

## Proposed roadmap — chưa triển khai

1. Định nghĩa TAD gateway-provider abstraction có schema/ownership và quyền read-only mặc định.
2. Thiết kế Kong DB-less validate/plan/diff trên declarative YAML, không dựa vào Admin CRUD.
3. Sinh YAML/diff có thể review và kiểm soát apply/rollback rõ ràng.
4. Thử portability với gateway khác mà không đổi canonical/domain contract.
5. Bổ sung durable persistence, distributed idempotency, status inquiry/reconciliation và chính sách unknown outcome.
6. Thiết kế production security, identity propagation, secrets, telemetry/audit và SLO.

Các bước này là hướng đề xuất cho SA review, không phải cam kết hoặc capability hiện có.
