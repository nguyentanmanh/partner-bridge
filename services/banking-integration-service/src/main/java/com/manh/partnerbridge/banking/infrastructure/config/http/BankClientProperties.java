package com.manh.partnerbridge.banking.infrastructure.config.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "partnerbridge.bank")
public record BankClientProperties(String baseUrl, String apiKey, Duration connectTimeout, Duration responseTimeout) {
    public BankClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("bank base-url is required");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("bank api-key is required");
        if (connectTimeout == null || !connectTimeout.isPositive()) throw new IllegalArgumentException("bank connect-timeout must be positive");
        if (responseTimeout == null || !responseTimeout.isPositive()) throw new IllegalArgumentException("bank response-timeout must be positive");
    }
}
