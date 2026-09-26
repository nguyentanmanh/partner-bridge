package com.manh.partnerbridge.education.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.manh.partnerbridge.education.infrastructure.i18n.MessageResolver;
import com.manh.partnerbridge.education.infrastructure.observability.RequestIdFilter;
import com.manh.partnerbridge.education.infrastructure.observability.TraceIdProvider;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {
    private final TraceIdProvider traces = mock(TraceIdProvider.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages(), traces);
    @AfterEach void clearLocale() { LocaleContextHolder.resetLocaleContext(); }

    @Test void validationDetailsAreLocalizedAndSorted() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        var target = new Form("", "");
        var errors = new BeanPropertyBindingResult(target, "form");
        errors.addError(new FieldError("form", "zeta", "{validation.item-id.not-blank}"));
        errors.addError(new FieldError("form", "alpha", "{validation.item-id.not-blank}"));
        var result = handler.invalidBody(new MethodArgumentNotValidException(null, errors), request);
        assertEquals(400, result.getStatusCode().value());
        assertEquals("alpha", result.getBody().details().getFirst().field());
        assertEquals("không được để trống", result.getBody().details().getFirst().reason());
    }

    @Test void unknownExceptionReturnsSafeMessage() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        when(request.getAttribute(RequestIdFilter.ATTRIBUTE)).thenReturn("REQ-1");
        var result = handler.unknown(new IllegalStateException("secret implementation detail"), request);
        assertEquals(500, result.getStatusCode().value());
        assertEquals("Internal server error", result.getBody().message());
        assertFalse(result.getBody().toString().contains("secret implementation detail"));
    }
    private static MessageResolver messages() {
        var source = new ResourceBundleMessageSource(); source.setBasename("messages"); source.setDefaultEncoding("UTF-8"); source.setFallbackToSystemLocale(false);
        return new MessageResolver(source);
    }
    record Form(String alpha, String zeta) { }
}
