package com.crm.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TranslationService {

    private final MessageSource messageSource;
    private final LocaleService localeService;

    public TranslationService(MessageSource messageSource, LocaleService localeService) {
        this.messageSource = messageSource;
        this.localeService = localeService;
    }

    public String translate(String key, Object... args) {
        return translate(localeService.getCurrentLocale(), key, args);
    }

    public String translate(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    public String translateEnum(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String key = "enum." + value.getClass().getSimpleName() + "." + value.name();
        return translate(key);
    }

    public Locale getCurrentLocale() {
        return localeService.getCurrentLocale();
    }

    public boolean isRtl() {
        return localeService.isRtl();
    }
}
