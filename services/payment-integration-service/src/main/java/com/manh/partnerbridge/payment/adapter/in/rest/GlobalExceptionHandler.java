package com.manh.partnerbridge.payment.adapter.in.rest;

import com.manh.partnerbridge.payment.application.exception.AccessDeniedException;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.exception.ExternalSystemTimeoutException;
import com.manh.partnerbridge.payment.application.exception.TemporaryIntegrationException;
import com.manh.partnerbridge.payment.application.exception.UnauthorizedException;
import com.manh.partnerbridge.payment.domain.exception.BusinessRuleViolationException;
import com.manh.partnerbridge.payment.domain.exception.CommonErrorCode;
import com.manh.partnerbridge.payment.domain.exception.ErrorCode;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.exception.ResourceNotFoundException;
import com.manh.partnerbridge.payment.infrastructure.error.ApiError;
import com.manh.partnerbridge.payment.infrastructure.error.ApiErrorDetail;
import com.manh.partnerbridge.payment.infrastructure.i18n.MessageResolver;
import com.manh.partnerbridge.payment.infrastructure.observability.RequestIdFilter;
import com.manh.partnerbridge.payment.infrastructure.observability.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageResolver messages;
    private final TraceIdProvider traceIds;

    public GlobalExceptionHandler(MessageResolver messages, TraceIdProvider traceIds) {
        this.messages = messages;
        this.traceIds = traceIds;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ApiErrorDetail(e.getField(), localizeValidation(e.getDefaultMessage(), locale)))
            .sorted(Comparator.comparing(ApiErrorDetail::field).thenComparing(ApiErrorDetail::reason)).toList();
        return response(HttpStatus.BAD_REQUEST, validationCode(request), details, request);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> malformedRequest(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, validationCode(request), List.of(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> invalidParameter(ConstraintViolationException ex, HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        List<ApiErrorDetail> details = ex.getConstraintViolations().stream().map(v -> {
            String path = v.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            return new ApiErrorDetail(field, localizeValidation(v.getMessage(), locale));
        }).sorted(Comparator.comparing(ApiErrorDetail::field).thenComparing(ApiErrorDetail::reason)).toList();
        return response(HttpStatus.BAD_REQUEST, validationCode(request), details, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest r) {
        return response(HttpStatus.NOT_FOUND, ex.errorCode(), List.of(), r);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ResponseEntity<ApiError> business(BusinessRuleViolationException ex, HttpServletRequest r) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ex.errorCode(), List.of(), r);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException ex, HttpServletRequest r) {
        return response(HttpStatus.UNAUTHORIZED, ex.errorCode(), List.of(), r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest r) {
        return response(HttpStatus.FORBIDDEN, ex.errorCode(), List.of(), r);
    }

    @ExceptionHandler(ExternalSystemTimeoutException.class)
    ResponseEntity<ApiError> timeout(ExternalSystemTimeoutException ex, HttpServletRequest r) {
        return response(HttpStatus.GATEWAY_TIMEOUT, ex.errorCode(), List.of(), r);
    }

    @ExceptionHandler(TemporaryIntegrationException.class)
    ResponseEntity<ApiError> unavailable(TemporaryIntegrationException ex, HttpServletRequest r) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, ex.errorCode(), List.of(), r);
    }

    @ExceptionHandler(PaymentException.class)
    ResponseEntity<ApiError> payment(PaymentException ex, HttpServletRequest r) {
        HttpStatus status = switch (ex.errorCode()) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_REFERENCE, IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case UNSUPPORTED_PROVIDER, PROVIDER_REJECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PROVIDER_AUTH, INVALID_PROVIDER_RESPONSE -> HttpStatus.BAD_GATEWAY;
            case PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case PROVIDER_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        var recorded = ex.recordedFailure();
        if (recorded != null) {
            return ResponseEntity.status(status).body(new ApiError(recorded.code().code(), recorded.message(),
                recorded.traceId(), recorded.requestId(), recorded.timestamp(), List.of()));
        }
        return response(status, ex.errorCode(), List.of(), r);
    }

    private ErrorCode validationCode(HttpServletRequest request) {
        return isPaymentPath(request) ? PaymentErrorCode.INVALID_REQUEST : CommonErrorCode.INVALID_REQUEST;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unknown(Exception ex, HttpServletRequest r) {
        LOG.error("Unhandled request failure", ex);
        ErrorCode code = isPaymentPath(r) ? PaymentErrorCode.INTERNAL_ERROR : CommonErrorCode.INTERNAL_ERROR;
        return response(HttpStatus.INTERNAL_SERVER_ERROR, code, List.of(), r);
    }

    private boolean isPaymentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.equals("/api/v1/payments") || path.startsWith("/api/v1/payments/"));
    }

    private ResponseEntity<ApiError> response(HttpStatus status, ErrorCode code, List<ApiErrorDetail> details, HttpServletRequest request) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        ApiError body = new ApiError(code.code(), messages.resolve(code.messageKey(), LocaleContextHolder.getLocale()),
            traceIds.currentTraceId(), "null".equals(requestId) ? "" : requestId, Instant.now(), details);
        return ResponseEntity.status(status).body(body);
    }

    private String localizeValidation(String text, Locale locale) {
        if (text != null && text.startsWith("{") && text.endsWith("}"))
            return messages.resolve(text.substring(1, text.length() - 1), locale);
        return text == null ? "" : text;
    }
}
