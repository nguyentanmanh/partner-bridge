package com.manh.partnerbridge.payment.infrastructure.config.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

public final class ProviderClientProperties {
    private ProviderClientProperties() {}

    @ConfigurationProperties(prefix = "partnerbridge.provider-a")
    public record ProviderA(String baseUrl, String apiKey, Duration connectTimeout, Duration responseTimeout) {
        public ProviderA { validate(baseUrl, connectTimeout, responseTimeout); required(apiKey, "api-key"); }
    }

    @ConfigurationProperties(prefix = "partnerbridge.provider-b")
    public record ProviderB(String baseUrl, String clientId, String signature,
                            Duration connectTimeout, Duration responseTimeout) {
        public ProviderB {
            validate(baseUrl, connectTimeout, responseTimeout);
            required(clientId, "client-id");
            required(signature, "signature");
        }
    }

    private static void validate(String baseUrl, Duration connectTimeout, Duration responseTimeout) {
        required(baseUrl, "base-url");
        if (connectTimeout == null || !connectTimeout.isPositive())
            throw new IllegalArgumentException("provider connect-timeout must be positive");
        if (responseTimeout == null || !responseTimeout.isPositive())
            throw new IllegalArgumentException("provider response-timeout must be positive");
    }

    private static void required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("provider " + name + " is required");
    }
}
