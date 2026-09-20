# ADR-004: Kong DB-less declarative YAML cho local POC

- **Status:** Accepted for current local POC; not a production decision
- **Date:** 2026-09-20

## Context

POC cần route, key-auth, correlation và rate limit có thể khởi động bằng Compose mà chưa vận hành thêm database.

## Decision

Dùng `kong:3.9.3`, `KONG_DATABASE=off`, mount read-only `gateways/kong/kong.yml`. Service/Route/Consumers/Plugins được khai báo trong YAML, áp dụng khi Kong startup/restart; `retries: 0` trên payment upstream. Admin API bind host loopback để quan sát. Đây chỉ là topology local.

## Consequences

Config review được bằng file diff, không cần Kong database. DB-less Admin API không hỗ trợ entity CRUD; POST thử tạo service giả đã trả `405` và service đó không tồn tại sau thử nghiệm. Không có workflow TAD plan/diff/apply. Rate limiting local không chia sẻ counter nhiều node; thay đổi YAML cần reload/restart theo quy trình POC. Không kết luận DB-less là lựa chọn production cuối cùng.

## Alternatives considered

Kong database mode/PostgreSQL: hỗ trợ quản lý entity động nhưng tăng hạ tầng và nằm ngoài POC. Admin CRUD trên DB-less: không khả thi (`405`). TAD tự apply config: chưa có connector/schema và không chọn trong current state.

## Evidence in repository

[Compose](../../../compose.yaml), [Kong YAML](../../../gateways/kong/kong.yml), [gateway runbook](../../../gateways/kong/README.md), [Step 7 DB-less 405 observation](../../tad-evaluation.md).
