package com.manh.partnerbridge.payment.infrastructure.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class MessageResolverTest {
    private final MessageResolver resolver = resolver();

    @Test
    void resolvesEnglish() {
        assertEquals("Item not found", resolver.resolve("error.item.not-found", Locale.ENGLISH));
    }

    @Test
    void resolvesVietnamese() {
        assertEquals("Không tìm thấy mặt hàng", resolver.resolve("error.item.not-found", Locale.forLanguageTag("vi")));
    }

    @Test
    void missingKeyIsSafeAndStable() {
        assertEquals("missing.key", resolver.resolve("missing.key", Locale.forLanguageTag("vi")));
    }

    private static MessageResolver resolver() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return new MessageResolver(source);
    }
}
