package com.manh.partnerbridge.payment.adapter.out.client.common;

import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.springframework.web.client.ResourceAccessException;

public final class ProviderTransportExceptionMapper {
    public PaymentException map(ResourceAccessException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException)
                return new PaymentException(PaymentErrorCode.PROVIDER_TIMEOUT);
            if (cause instanceof ConnectException)
                return new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
        }
        return new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
    }
}
