package com.manh.partnerbridge.payment.application.port.out;

import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import java.util.Locale;

public interface PaymentMessagesPort {
    String resolve(PaymentErrorCode code, Locale locale);
}
