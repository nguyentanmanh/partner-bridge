package com.manh.partnerbridge.education.adapter.in.rest;

import com.manh.partnerbridge.education.application.exception.AccessDeniedException;
import com.manh.partnerbridge.education.application.exception.ApplicationException;
import com.manh.partnerbridge.education.application.exception.ExternalSystemTimeoutException;
import com.manh.partnerbridge.education.application.exception.TemporaryIntegrationException;
import com.manh.partnerbridge.education.application.exception.UnauthorizedException;
import com.manh.partnerbridge.education.domain.exception.BusinessRuleViolationException;
import com.manh.partnerbridge.education.domain.exception.CommonErrorCode;
import com.manh.partnerbridge.education.domain.exception.DomainException;
import com.manh.partnerbridge.education.domain.exception.ErrorCode;
import com.manh.partnerbridge.education.domain.exception.ResourceNotFoundException;
import com.manh.partnerbridge.education.infrastructure.error.ApiError;
import com.manh.partnerbridge.education.infrastructure.error.ApiErrorDetail;
import com.manh.partnerbridge.education.infrastructure.i18n.MessageResolver;
import com.manh.partnerbridge.education.infrastructure.observability.RequestIdFilter;
import com.manh.partnerbridge.education.infrastructure.observability.TraceIdProvider;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageResolver messages; private final TraceIdProvider traceIds;
    public GlobalExceptionHandler(MessageResolver messages, TraceIdProvider traceIds) { this.messages = messages; this.traceIds = traceIds; }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiErrorDetail(e.getField(), localizeValidation(e.getDefaultMessage(), locale)))
                .sorted(Comparator.comparing(ApiErrorDetail::field).thenComparing(ApiErrorDetail::reason)).toList();
        return response(HttpStatus.BAD_REQUEST, CommonErrorCode.INVALID_REQUEST, details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> invalidParameter(ConstraintViolationException ex, HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        List<ApiErrorDetail> details = ex.getConstraintViolations().stream().map(v -> {
            String path = v.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            return new ApiErrorDetail(field, localizeValidation(v.getMessage(), locale));
        }).sorted(Comparator.comparing(ApiErrorDetail::field).thenComparing(ApiErrorDetail::reason)).toList();
        return response(HttpStatus.BAD_REQUEST, CommonErrorCode.INVALID_REQUEST, details, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest r) { return response(HttpStatus.NOT_FOUND, ex.errorCode(), List.of(), r); }
    @ExceptionHandler(BusinessRuleViolationException.class)
    ResponseEntity<ApiError> business(BusinessRuleViolationException ex, HttpServletRequest r) { return response(HttpStatus.UNPROCESSABLE_ENTITY, ex.errorCode(), List.of(), r); }
    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException ex, HttpServletRequest r) { return response(HttpStatus.UNAUTHORIZED, ex.errorCode(), List.of(), r); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest r) { return response(HttpStatus.FORBIDDEN, ex.errorCode(), List.of(), r); }
    @ExceptionHandler(ExternalSystemTimeoutException.class)
    ResponseEntity<ApiError> timeout(ExternalSystemTimeoutException ex, HttpServletRequest r) { return response(HttpStatus.GATEWAY_TIMEOUT, ex.errorCode(), List.of(), r); }
    @ExceptionHandler(TemporaryIntegrationException.class)
    ResponseEntity<ApiError> unavailable(TemporaryIntegrationException ex, HttpServletRequest r) { return response(HttpStatus.SERVICE_UNAVAILABLE, ex.errorCode(), List.of(), r); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unknown(Exception ex, HttpServletRequest r) {
        LOG.error("Unhandled request failure", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_ERROR, List.of(), r);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, ErrorCode code, List<ApiErrorDetail> details, HttpServletRequest request) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        ApiError body = new ApiError(code.code(), messages.resolve(code.messageKey(), LocaleContextHolder.getLocale()),
                traceIds.currentTraceId(), "null".equals(requestId) ? "" : requestId, Instant.now(), details);
        return ResponseEntity.status(status).body(body);
    }
    private String localizeValidation(String text, Locale locale) {
        if (text != null && text.startsWith("{") && text.endsWith("}")) return messages.resolve(text.substring(1, text.length() - 1), locale);
        return text == null ? "" : text;
    }
}
