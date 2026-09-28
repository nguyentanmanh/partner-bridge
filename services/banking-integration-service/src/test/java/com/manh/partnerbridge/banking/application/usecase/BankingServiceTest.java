package com.manh.partnerbridge.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.RecentTransaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BankingServiceTest {
    @Test void combinesAccountAndTransactionsIntoSummary() {
        BankProviderPort provider = mock(BankProviderPort.class);
        BankAccount account = new BankAccount("ACC-001", "Nguyen Van A",
                new BigDecimal("2500000.00"), "VND", "ACTIVE");
        RecentTransaction transaction = new RecentTransaction("TXN-001", new BigDecimal("-500000.00"),
                "VND", OffsetDateTime.parse("2026-09-28T10:30:00Z"));
        when(provider.getAccount("ACC-001", "REQ-001")).thenReturn(account);
        when(provider.getTransactions("ACC-001", "REQ-001")).thenReturn(List.of(transaction));

        var summary = new BankingService(provider).getAccountSummary("ACC-001", "REQ-001");

        assertEquals(account, summary.account());
        assertEquals(List.of(transaction), summary.recentTransactions());
        verify(provider).getAccount("ACC-001", "REQ-001");
        verify(provider).getTransactions("ACC-001", "REQ-001");
    }
}
