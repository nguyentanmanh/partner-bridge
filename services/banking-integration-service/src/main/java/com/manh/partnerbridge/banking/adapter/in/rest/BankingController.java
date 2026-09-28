package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.application.port.in.BankingUseCase;
import com.manh.partnerbridge.banking.infrastructure.observability.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
public class BankingController {
    private final BankingUseCase useCase;
    public BankingController(BankingUseCase useCase) { this.useCase = useCase; }

    @Operation(summary = "Get a bank account from the external bank")
    @GetMapping("/accounts/{accountId}")
    public BankAccountResponse getAccount(@PathVariable @NotBlank String accountId,
            HttpServletRequest servletRequest) {
        return BankAccountResponse.from(useCase.getAccount(accountId, requestId(servletRequest)));
    }

    @Operation(summary = "Get recent transactions from the external bank")
    @GetMapping("/accounts/{accountId}/transactions")
    public List<RecentTransactionResponse> getTransactions(@PathVariable @NotBlank String accountId,
            HttpServletRequest servletRequest) {
        return useCase.getTransactions(accountId, requestId(servletRequest)).stream()
                .map(RecentTransactionResponse::from)
                .toList();
    }

    @Operation(summary = "Get an account summary from the external bank")
    @GetMapping("/accounts/{accountId}/summary")
    public AccountSummaryResponse getAccountSummary(@PathVariable @NotBlank String accountId,
            HttpServletRequest servletRequest) {
        return AccountSummaryResponse.from(useCase.getAccountSummary(accountId, requestId(servletRequest)));
    }

    @Operation(summary = "Create a transfer at the external bank")
    @PostMapping("/transfers")
    public ResponseEntity<BankTransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BankTransferResponse.from(useCase.createTransfer(request.toCommand(), requestId(servletRequest))));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        return value == null ? "" : value.toString();
    }
}
