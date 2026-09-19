package com.manh.partnerbridge.payment.architecture.fixtures.adapter.out;

import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;

public final class AdapterFixtures {
    private AdapterFixtures() {}
    public abstract static class MissingPortAdapter {}
    public abstract static class ValidAbstractAdapter implements PaymentProviderPort {}
}
