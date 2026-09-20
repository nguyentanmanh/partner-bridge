package com.manh.partnerbridge.payment.adapter.out.client.providerb;

public record ProviderBRequest(Details request) {
    public record Details(String orderRef, Money money, String content) {}
    public record Money(String amount, String currencyCode) {}
}
