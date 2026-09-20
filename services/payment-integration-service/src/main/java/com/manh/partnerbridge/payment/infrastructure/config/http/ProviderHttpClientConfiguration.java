package com.manh.partnerbridge.payment.infrastructure.config.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.adapter.out.client.common.ProviderCallSupport;
import com.manh.partnerbridge.payment.adapter.out.client.common.ProviderTransportExceptionMapper;
import com.manh.partnerbridge.payment.adapter.out.client.providera.ProviderAAdapter;
import com.manh.partnerbridge.payment.adapter.out.client.providera.ProviderAHttpClient;
import com.manh.partnerbridge.payment.adapter.out.client.providera.ProviderAMapper;
import com.manh.partnerbridge.payment.adapter.out.client.providerb.ProviderBAdapter;
import com.manh.partnerbridge.payment.adapter.out.client.providerb.ProviderBHttpClient;
import com.manh.partnerbridge.payment.adapter.out.client.providerb.ProviderBMapper;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties({ProviderClientProperties.ProviderA.class, ProviderClientProperties.ProviderB.class})
public class ProviderHttpClientConfiguration {
    @Bean
    public ProviderAHttpClient providerAHttpClient(ProviderClientProperties.ProviderA properties) {
        return proxy(ProviderAHttpClient.class, properties.baseUrl(), properties.connectTimeout(), properties.responseTimeout());
    }

    @Bean
    public ProviderBHttpClient providerBHttpClient(ProviderClientProperties.ProviderB properties) {
        return proxy(ProviderBHttpClient.class, properties.baseUrl(), properties.connectTimeout(), properties.responseTimeout());
    }

    private static <T> T proxy(Class<T> type, String baseUrl, Duration connectTimeout, Duration responseTimeout) {
        HttpClient jdk = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(responseTimeout);
        RestClient rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
            .defaultStatusHandler(status -> true, (request, response) -> { /* Preserve raw status and body. */ })
            .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest)).build().createClient(type);
    }

    @Bean
    public ProviderCallSupport providerCallSupport() {
        return new ProviderCallSupport(new ProviderTransportExceptionMapper());
    }

    @Bean
    PaymentProviderPort providerA(ProviderAHttpClient http, ObjectMapper json, ProviderCallSupport calls,
                                  ProviderClientProperties.ProviderA properties) {
        return new ProviderAAdapter(http, new ProviderAMapper(json), calls, properties.apiKey());
    }

    @Bean
    PaymentProviderPort providerB(ProviderBHttpClient http, ObjectMapper json, ProviderCallSupport calls,
                                  ProviderClientProperties.ProviderB properties) {
        return new ProviderBAdapter(http, new ProviderBMapper(json), calls, properties.clientId(), properties.signature());
    }
}
