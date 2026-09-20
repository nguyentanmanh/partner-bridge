# ADR-005: Reserve trước provider call; giữ unknown outcome

- **Status:** Accepted for current POC
- **Date:** 2026-09-20

## Context

Payment create có side effect bên provider. Concurrent request hoặc timeout không được tự động tạo giao dịch thứ hai; timeout không chứng minh provider đã không xử lý.

## Decision

`PaymentService` reserve nguyên tử `(client scope, Idempotency-Key)` và `(client scope, merchantReference)` **trước** outbound call. Cùng key/canonical command dùng chung per-entry future, không gọi provider thêm; khác command hoặc reference trùng với key khác trả `409`. Global lock không giữ trong lúc HTTP. Sau provider call, lưu payment hoặc recorded error để replay cùng HTTP status/body. Timeout `504` và malformed `502` giữ processing state nội bộ `UNKNOWN`, không map sang canonical `FAILED`; business rejection `422` cũng được replay. Không retry tự động.

TTL response mặc định 24h, cấu hình phải >0, bắt đầu sau first processing và không expire `IN_FLIGHT`; reference reservation giữ tới khi process kết thúc. `Clock` injectable để test TTL.

## Consequences

Replay không gây duplicate trong một process; payment độc lập và GET không chờ slow provider. Sau TTL, reference cũ vẫn `409`. Restart làm mất state; nhiều instance không được bảo vệ; unknown result cần reconciliation tương lai. Error body replay giữ request/trace/time lần đầu, response header Request-ID theo request hiện tại.

## Alternatives considered

Thả reservation hoặc retry ngay sau timeout: có nguy cơ double charge, không chọn. Durable/distributed store và reconciliation là hướng production nhưng ngoài POC. Gán `FAILED` cho timeout: không có bằng chứng nghiệp vụ, không chọn.

## Evidence in repository

[PaymentService](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/application/usecase/PaymentService.java), [contract rules](../../../contracts/payment/v1/README.md), [use case tests](../../../services/payment-integration-service/src/test/java/com/manh/partnerbridge/payment/application/usecase/PaymentServiceTest.java), [REST regression tests](../../../services/payment-integration-service/src/test/java/com/manh/partnerbridge/payment/adapter/in/rest/PaymentApiRegressionTest.java).
