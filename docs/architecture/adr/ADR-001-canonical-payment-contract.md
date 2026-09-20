# ADR-001: Canonical Payment API che giấu provider contracts

- **Status:** Accepted for current POC
- **Date:** 2026-09-20

## Context

Provider A/B có endpoint, authentication, amount, reference và status khác nhau. Nếu client dùng từng payload riêng, việc thêm/thay provider sẽ lan vào client và gateway.

## Decision

Public v1 chỉ có `POST /api/v1/payments` và `GET /api/v1/payments/{paymentId}` theo OpenAPI 3.0.3. `Money.value` là string, provider logic chỉ `PROVIDER_A`/`PROVIDER_B`, canonical status `PENDING`/`SUCCEEDED`/`FAILED`, error dùng `ApiError`. Provider-specific DTO và mapping ở outbound adapters. Contract độc lập gateway.

## Consequences

Client dùng một schema; adapter phải kiểm provider response và chuyển status. Mọi đổi public payload/status cần contract review và test. `FAIL` stub trả business error `422`; không suy diễn thành `201/FAILED`.

## Alternatives considered

Expose raw provider APIs hoặc đưa provider payload vào canonical response: ít mapping hơn nhưng buộc client phụ thuộc provider; không chọn. Một API riêng theo provider cũng không đạt mục tiêu canonical.

## Evidence in repository

[OpenAPI](../../../contracts/payment/v1/openapi.yaml), [contract rules](../../../contracts/payment/v1/README.md), [A mapper](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/adapter/out/client/providera/ProviderAMapper.java), [B mapper](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/adapter/out/client/providerb/ProviderBMapper.java), [adapter tests](../../../services/payment-integration-service/src/test/java/com/manh/partnerbridge/payment/adapter/out/client/PaymentProviderAdapterTest.java).
