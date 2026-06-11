package com.crm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationServiceTest {

    private TranslationService translationService;
    private LocaleService localeService;
    private ResourceBundleMessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        localeService = mock(LocaleService.class);
        translationService = new TranslationService(messageSource, localeService);
    }

    @Test
    void translate_usesCurrentLocale() {
        when(localeService.getCurrentLocale()).thenReturn(Locale.ENGLISH);
        assertEquals("CRM", translationService.translate("app.name"));
    }

    @Test
    void translateEnum_buildsEnumKey() {
        when(localeService.getCurrentLocale()).thenReturn(Locale.ENGLISH);
        assertEquals("enum.TestEnum.NEW", translationService.translateEnum(TestEnum.NEW));
    }

    private enum TestEnum { NEW }
}
