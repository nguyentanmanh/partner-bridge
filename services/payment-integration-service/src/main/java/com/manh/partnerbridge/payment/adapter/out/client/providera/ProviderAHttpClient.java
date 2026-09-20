package com.manh.partnerbridge.payment.adapter.out.client.providera;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;

public interface ProviderAHttpClient {
    @PostExchange(value = "/v1/payments", contentType = "application/json")
    ResponseEntity<String> create(@RequestHeader("X-Api-Key") String apiKey,
                                  @RequestHeader("Request-ID") String requestId,
                                  @RequestBody ProviderARequest request);
}
