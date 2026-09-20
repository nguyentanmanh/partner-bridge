# Architecture decision records

Các ADR dưới đây ghi **quyết định đang có trong POC**, không phê duyệt thiết kế production. Mỗi ADR dẫn tới bằng chứng source/config/contract; [roadmap](../limitations-and-roadmap.md) tách riêng phần chưa triển khai.

| ADR | Decision |
| --- | --- |
| [001](ADR-001-canonical-payment-contract.md) | Canonical Payment API che provider contract |
| [002](ADR-002-hexagonal-service-architecture.md) | Java service theo hexagonal boundaries |
| [003](ADR-003-gateway-responsibility-boundary.md) | Kong xử lý edge, backend xử lý payment |
| [004](ADR-004-kong-dbless-for-local-poc.md) | Kong DB-less declarative local POC |
| [005](ADR-005-idempotency-and-unknown-outcome.md) | Reserve trước provider call, giữ unknown outcome |
| [006](ADR-006-provider-http-client.md) | Spring HTTP Service Client/RestClient cho provider |
