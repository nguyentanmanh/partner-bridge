package com.manh.partnerbridge.banking.adapter.out.client.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.banking.application.command.CreateTransferCommand;
import com.manh.partnerbridge.banking.application.exception.ExternalSystemException;
import com.manh.partnerbridge.banking.application.exception.ExternalSystemTimeoutException;
import com.manh.partnerbridge.banking.application.exception.TemporaryIntegrationException;
import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import com.manh.partnerbridge.banking.domain.exception.BankingErrorCode;
import com.manh.partnerbridge.banking.domain.exception.BusinessRuleViolationException;
import com.manh.partnerbridge.banking.domain.exception.CommonErrorCode;
import com.manh.partnerbridge.banking.domain.exception.ResourceNotFoundException;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.BankTransfer;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

public final class BankProviderAdapter implements BankProviderPort {
    private final BankHttpClient http;
    private final ObjectMapper json;
    private final String apiKey;

    public BankProviderAdapter(BankHttpClient http, ObjectMapper json, String apiKey) {
        this.http = http; this.json = json; this.apiKey = apiKey;
    }

    @Override public BankAccount getAccount(String accountId, String requestId) {
        ResponseEntity<String> response = call(() -> http.getAccount(apiKey, requestId, accountId));
        if (response.getStatusCode().value() == 404)
            throw new ResourceNotFoundException(BankingErrorCode.ACCOUNT_NOT_FOUND);
        requireSuccess(response);
        BankAccountPayload body = read(response.getBody(), BankAccountPayload.class);
        if (body.accountNumber() == null || body.availableBalance() == null)
            throw invalidResponse(null);
        return new BankAccount(body.accountNumber(), body.customerName(), body.availableBalance(),
                body.currency(), body.status());
    }

    @Override public BankTransfer createTransfer(CreateTransferCommand command, String requestId) {
        var request = new BankTransferRequest(command.fromAccount(), command.toAccount(), command.amount(),
                command.currency(), command.reference());
        ResponseEntity<String> response = call(() -> http.createTransfer(apiKey, requestId, request));
        if (response.getStatusCode().value() == 422)
            throw new BusinessRuleViolationException(BankingErrorCode.TRANSFER_REJECTED);
        requireSuccess(response);
        BankTransferPayload body = read(response.getBody(), BankTransferPayload.class);
        if (body.transactionId() == null || body.transactionStatus() == null)
            throw invalidResponse(null);
        return new BankTransfer(body.transactionId(), body.transactionStatus());
    }

    private ResponseEntity<String> call(Call call) {
        try { return call.invoke(); }
        catch (ResourceAccessException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause())
                if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException)
                    throw new ExternalSystemTimeoutException(exception);
            throw new TemporaryIntegrationException(exception);
        }
    }

    private void requireSuccess(ResponseEntity<String> response) {
        int status = response.getStatusCode().value();
        if (status >= 500) throw new TemporaryIntegrationException(new IllegalStateException("bank status " + status));
        if (status < 200 || status >= 300) throw invalidResponse(null);
    }

    private <T> T read(String body, Class<T> type) {
        try { return json.readValue(body == null ? "" : body, type); }
        catch (JsonProcessingException exception) { throw invalidResponse(exception); }
    }

    private ExternalSystemException invalidResponse(Throwable cause) {
        return new ExternalSystemException(CommonErrorCode.EXTERNAL_INVALID_RESPONSE, cause);
    }

    @FunctionalInterface private interface Call { ResponseEntity<String> invoke(); }
}
