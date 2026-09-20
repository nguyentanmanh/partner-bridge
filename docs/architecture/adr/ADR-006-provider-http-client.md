# ADR-006: Spring HTTP Service Client và RestClient cho providers

- **Status:** Accepted for current POC
- **Date:** 2026-09-20

## Context

Hai provider giả lập có path, auth header, JSON shape và status khác nhau. HTTP transport phải có timeout cấu hình được, còn canonical application không phụ thuộc transport framework.

## Decision

Mỗi provider có Spring HTTP Service interface (`ProviderAHttpClient`/`ProviderBHttpClient`) với `@PostExchange`. Infrastructure tạo proxy bằng `HttpServiceProxyFactory` trên `RestClientAdapter`, `RestClient` và JDK `HttpClient` HTTP/1.1. Typed properties bind base URL, credential, connect timeout và response/read timeout. Adapter gọi HTTP interface và mapper riêng; `ProviderCallSupport` phân loại HTTP/transport error, log safe outcome. Không retry tự động.

## Consequences

Provider path/header/DTO tách rõ, dễ kiểm tra mapping và credentials trong tests; application chỉ thấy `PaymentProviderPort`. Có thêm Spring wiring/proxy layer và phải giữ timeout của backend thấp hơn gateway read timeout. Provider A numeric amount được tạo từ canonical string qua `BigDecimal`; provider B giữ string. Raw provider body/secret không trả cho client hoặc log.

## Alternatives considered

OpenFeign không được chọn vì source hiện dùng Spring HTTP Service Client/RestClient, phù hợp bộ Spring Web dependency đã có và giữ HTTP interface nhỏ cho hai stub. [Spring Cloud OpenFeign documentation](https://docs.spring.io/spring-cloud-openfeign/reference/index.html) hiện gợi ý chuyển sang Spring HTTP Service Clients cho hướng phát triển tiếp; đó là hướng dẫn của Spring Cloud, **không** có nghĩa Feign không còn chạy hoặc không phù hợp với mọi hệ thống. Tự dùng JDK `HttpClient` trực tiếp trong adapter ít proxy hơn nhưng tăng transport code lặp; source đã thay thế cách đó.

## Evidence in repository

[HTTP client configuration](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/infrastructure/config/http/ProviderHttpClientConfiguration.java), [typed properties](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/infrastructure/config/http/ProviderClientProperties.java), [A interface](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/adapter/out/client/providera/ProviderAHttpClient.java), [B interface](../../../services/payment-integration-service/src/main/java/com/manh/partnerbridge/payment/adapter/out/client/providerb/ProviderBHttpClient.java), [transport tests](../../../services/payment-integration-service/src/test/java/com/manh/partnerbridge/payment/adapter/out/client/PaymentProviderAdapterTest.java).
