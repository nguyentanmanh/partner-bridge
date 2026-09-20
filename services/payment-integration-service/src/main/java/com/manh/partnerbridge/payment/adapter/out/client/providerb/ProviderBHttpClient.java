package com.manh.partnerbridge.payment.adapter.out.client.providerb;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;

public interface ProviderBHttpClient {
    @PostExchange(value = "/api/payment/create", contentType = "application/json")
    ResponseEntity<String> create(@RequestHeader("X-Client-Id") String clientId,
                                  @RequestHeader("X-Signature") String signature,
                                  @RequestHeader("Request-ID") String requestId,
                                  @RequestBody ProviderBRequest request);
}
