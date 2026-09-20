# ADR-002: Hexagonal Architecture cho Java payment service

- **Status:** Accepted for current POC
- **Date:** 2026-09-20

## Context

Payment service được generate từ HexaForge. Tích hợp nhiều provider và gateway cần giữ business flow độc lập HTTP/provider framework.

## Decision

`PaymentController` là inbound REST adapter, chỉ dùng `PaymentUseCase`; `PaymentService` thực hiện use case và dùng `PaymentProviderPort`; A/B outbound adapters triển khai port. Domain model không phụ thuộc Spring/HTTP/provider DTO. Infrastructure tạo beans, HTTP clients, i18n/error/observability. Kong là process ngoài service.

## Consequences

Routing, reservation và canonical behavior có thể test với fake port/Clock. Mỗi provider cần adapter/mapper/client riêng và thêm wiring. Rule ArchUnit bảo vệ boundary; không đổi rule để hợp thức hóa dependency sai.

## Alternatives considered

Controller gọi RestClient trực tiếp hoặc domain chứa provider DTO: ngắn hơn lúc đầu nhưng tạo coupling và khó test; không chọn. Tách nhiều deployable services chưa cần cho POC.

## Evidence in repository

[Package map](../payment-service.md), [PaymentController](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/adapter/in/rest/PaymentController.java), [PaymentService](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/application/usecase/PaymentService.java), [ports](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/application/port/), [ArchitectureTest](../../../services/payment-integration-service/src/test/java/com/manh/partnerbridge/payment/architecture/ArchitectureTest.java).
