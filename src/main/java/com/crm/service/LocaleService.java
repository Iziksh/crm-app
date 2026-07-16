package com.crm.service;

import com.crm.i18n.SupportedLocale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Locale resolution for REST requests. {@link com.crm.config.LocaleCookieFilter} sets
 * {@link LocaleContextHolder} per-request from the locale cookie/Accept-Language header;
 * this service reads that back for callers (email templates, translations) that need the
 * caller's current locale outside the filter itself.
 */
@Service
public class LocaleService {

    public Locale getCurrentLocale() {
        Locale contextLocale = LocaleContextHolder.getLocale();
        if (contextLocale != null && SupportedLocale.isSupported(contextLocale)) {
            return contextLocale;
        }
        return SupportedLocale.DEFAULT;
    }

    public boolean isRtl() {
        return SupportedLocale.isRtl(getCurrentLocale());
    }
}
