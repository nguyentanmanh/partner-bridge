package com.manh.partnerbridge.banking.adapter.in.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.manh.partnerbridge.banking.application.port.in.BankingUseCase;
import com.manh.partnerbridge.banking.domain.model.AccountSummary;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.RecentTransaction;
import com.manh.partnerbridge.banking.infrastructure.observability.RequestIdFilter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BankingControllerTest {
    private final BankingUseCase useCase = mock(BankingUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BankingController(useCase)).build();

    @Test void returnsRecentTransactionsFromInboundPort() throws Exception {
        RecentTransaction transaction = transaction();
        when(useCase.getTransactions("ACC-001", "REQ-001")).thenReturn(List.of(transaction));

        mvc.perform(get("/api/v1/accounts/ACC-001/transactions")
                        .requestAttr(RequestIdFilter.ATTRIBUTE, "REQ-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("TXN-001"))
                .andExpect(jsonPath("$[0].currency").value("VND"));

        verify(useCase).getTransactions("ACC-001", "REQ-001");
    }

    @Test void returnsAggregatedAccountSummaryFromInboundPort() throws Exception {
        BankAccount account = new BankAccount("ACC-001", "Nguyen Van A",
                new BigDecimal("2500000.00"), "VND", "ACTIVE");
        when(useCase.getAccountSummary("ACC-001", "REQ-002"))
                .thenReturn(new AccountSummary(account, List.of(transaction())));

        mvc.perform(get("/api/v1/accounts/ACC-001/summary")
                        .requestAttr(RequestIdFilter.ATTRIBUTE, "REQ-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.accountNo").value("ACC-001"))
                .andExpect(jsonPath("$.account.balance").value(2500000.00))
                .andExpect(jsonPath("$.recentTransactions[0].transactionId").value("TXN-001"));

        verify(useCase).getAccountSummary("ACC-001", "REQ-002");
    }

    private RecentTransaction transaction() {
        return new RecentTransaction("TXN-001", new BigDecimal("-500000.00"), "VND",
                OffsetDateTime.parse("2026-09-28T10:30:00Z"));
    }
}
