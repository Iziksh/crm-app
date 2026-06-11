package com.crm.config;

import com.crm.i18n.SupportedLocale;
import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CrmI18nProvider implements I18NProvider {

    private final MessageSource messageSource;

    public CrmI18nProvider(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return SupportedLocale.ALL;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        return messageSource.getMessage(key, params, key, locale);
    }
}
