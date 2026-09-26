package com.manh.partnerbridge.banking.adapter.out.client.bank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface BankHttpClient {
    @GetExchange("/bank/v1/accounts/{accountId}")
    ResponseEntity<String> getAccount(@RequestHeader("X-Bank-Api-Key") String apiKey,
                                      @RequestHeader("Request-ID") String requestId,
                                      @PathVariable String accountId);

    @PostExchange(value = "/bank/v1/transfers", contentType = "application/json")
    ResponseEntity<String> createTransfer(@RequestHeader("X-Bank-Api-Key") String apiKey,
                                          @RequestHeader("Request-ID") String requestId,
                                          @RequestBody BankTransferRequest request);
}
