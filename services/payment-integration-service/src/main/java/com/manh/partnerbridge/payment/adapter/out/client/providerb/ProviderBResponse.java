package com.manh.partnerbridge.payment.adapter.out.client.providerb;

public record ProviderBResponse(String responseCode, Data data) {
    public record Data(String referenceNo, String status, String amount, String currencyCode) {}
}
