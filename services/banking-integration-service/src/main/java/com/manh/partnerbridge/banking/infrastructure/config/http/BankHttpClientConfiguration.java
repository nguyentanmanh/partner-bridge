package com.manh.partnerbridge.banking.infrastructure.config.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.banking.adapter.out.client.bank.BankHttpClient;
import com.manh.partnerbridge.banking.adapter.out.client.bank.BankProviderAdapter;
import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(BankClientProperties.class)
public class BankHttpClientConfiguration {
    @Bean BankHttpClient bankHttpClient(BankClientProperties properties) {
        HttpClient jdk = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(properties.responseTimeout());
        RestClient rest = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory)
                .defaultStatusHandler(status -> true, (request, response) -> { }).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest)).build()
                .createClient(BankHttpClient.class);
    }

    @Bean BankProviderPort bankProviderPort(BankHttpClient http, ObjectMapper json, BankClientProperties properties) {
        return new BankProviderAdapter(http, json, properties.apiKey());
    }
}
