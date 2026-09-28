package com.manh.partnerbridge.banking.adapter.out.client.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BankProviderAdapterTest {
    @Test void mapsProviderTransactionPayloadToCanonicalModel() {
        BankHttpClient http = mock(BankHttpClient.class);
        String response = """
                {"transactions":[{
                  "transactionId":"TXN-001",
                  "amount":-500000.00,
                  "currency":"VND",
                  "transactionDate":"2026-09-28T10:30:00Z"
                }]}
                """;
        when(http.getTransactions("bank-local-key", "REQ-001", "ACC-001"))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        BankProviderAdapter adapter = new BankProviderAdapter(http,
                new ObjectMapper().findAndRegisterModules(), "bank-local-key");

        var transactions = adapter.getTransactions("ACC-001", "REQ-001");

        assertEquals(1, transactions.size());
        assertEquals("TXN-001", transactions.getFirst().transactionId());
        assertEquals(new BigDecimal("-500000.00"), transactions.getFirst().amount());
        assertEquals("VND", transactions.getFirst().currency());
        verify(http).getTransactions("bank-local-key", "REQ-001", "ACC-001");
    }
}
