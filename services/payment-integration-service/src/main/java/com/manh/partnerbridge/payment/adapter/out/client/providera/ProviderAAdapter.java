package com.manh.partnerbridge.payment.adapter.out.client.providera;

import com.manh.partnerbridge.payment.adapter.out.client.common.ProviderCallSupport;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;

public final class ProviderAAdapter implements PaymentProviderPort {
    private final ProviderAHttpClient http;
    private final ProviderAMapper mapper;
    private final ProviderCallSupport calls;
    private final String apiKey;

    public ProviderAAdapter(ProviderAHttpClient http, ProviderAMapper mapper, ProviderCallSupport calls, String apiKey) {
        this.http = http;
        this.mapper = mapper;
        this.calls = calls;
        this.apiKey = apiKey;
    }

    @Override public String providerCode() { return "PROVIDER_A"; }

    @Override public ProviderResult create(CreatePaymentCommand command, String requestId) {
        return calls.call(providerCode(), requestId, () -> http.create(apiKey, requestId, mapper.request(command)),
            raw -> mapper.response(raw, command));
    }
}
