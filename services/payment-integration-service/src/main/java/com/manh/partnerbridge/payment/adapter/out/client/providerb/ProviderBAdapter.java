package com.manh.partnerbridge.payment.adapter.out.client.providerb;

import com.manh.partnerbridge.payment.adapter.out.client.common.ProviderCallSupport;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;

public final class ProviderBAdapter implements PaymentProviderPort {
    private final ProviderBHttpClient http;
    private final ProviderBMapper mapper;
    private final ProviderCallSupport calls;
    private final String clientId;
    private final String signature;

    public ProviderBAdapter(ProviderBHttpClient http, ProviderBMapper mapper, ProviderCallSupport calls,
                            String clientId, String signature) {
        this.http = http;
        this.mapper = mapper;
        this.calls = calls;
        this.clientId = clientId;
        this.signature = signature;
    }

    @Override public String providerCode() { return "PROVIDER_B"; }

    @Override public ProviderResult create(CreatePaymentCommand command, String requestId) {
        return calls.call(providerCode(), requestId,
            () -> http.create(clientId, signature, requestId, mapper.request(command)),
            raw -> mapper.response(raw, command));
    }
}
