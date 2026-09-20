package com.manh.partnerbridge.payment.adapter.out.client.common;

import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort.ProviderResult;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

/** Shared transport outcome handling; provider-specific body mapping stays in each mapper. */
public final class ProviderCallSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ProviderCallSupport.class);
    private final ProviderTransportExceptionMapper transportErrors;

    public ProviderCallSupport(ProviderTransportExceptionMapper transportErrors) {
        this.transportErrors = transportErrors;
    }

    public ProviderResult call(String provider, String requestId, Supplier<ResponseEntity<String>> request,
                               Function<String, ProviderResult> successMapper) {
        long started = System.nanoTime();
        String callOutcome = "UNKNOWN";
        String paymentStatus = "UNKNOWN";
        try {
            ResponseEntity<String> response = request.get();
            int status = response.getStatusCode().value();
            if (status == 401 || status == 403) throw new PaymentException(PaymentErrorCode.PROVIDER_AUTH);
            if (status == 422) throw new PaymentException(PaymentErrorCode.PROVIDER_REJECTED);
            if (status >= 500) throw new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
            if (status < 200 || status >= 300) throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
            ProviderResult result = successMapper.apply(response.getBody());
            callOutcome = "RESPONSE_ACCEPTED";
            paymentStatus = result.status().name();
            return result;
        } catch (PaymentException exception) {
            callOutcome = switch (exception.errorCode()) {
                case PROVIDER_REJECTED -> "BUSINESS_REJECTION";
                case PROVIDER_AUTH -> "AUTHENTICATION_ERROR";
                case INVALID_PROVIDER_RESPONSE -> "INVALID_RESPONSE";
                case PROVIDER_TIMEOUT -> "TIMEOUT";
                case PROVIDER_UNAVAILABLE -> "UNAVAILABLE";
                default -> "UNKNOWN";
            };
            throw exception;
        } catch (ResourceAccessException exception) {
            PaymentException translated = transportErrors.map(exception);
            callOutcome = translated.errorCode() == PaymentErrorCode.PROVIDER_TIMEOUT ? "TIMEOUT" : "UNAVAILABLE";
            throw translated;
        } finally {
            LOG.info("provider={} requestId={} durationMs={} callOutcome={} paymentStatus={}", provider, requestId,
                Duration.ofNanos(System.nanoTime() - started).toMillis(), callOutcome, paymentStatus);
        }
    }
}
