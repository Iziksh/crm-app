package com.crm.service;

import com.crm.i18n.SupportedLocale;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
public class LocaleService {

    private static final int COOKIE_MAX_AGE_SECONDS = 365 * 24 * 60 * 60;

    public Locale resolve(HttpServletRequest request) {
        Optional<Locale> fromCookie = readCookie(request, SupportedLocale.COOKIE_NAME)
                .flatMap(SupportedLocale::fromCode);
        if (fromCookie.isPresent()) {
            return fromCookie.get();
        }

        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Locale sessionLocale = (Locale) session.getAttribute(SupportedLocale.SESSION_ATTR);
            if (sessionLocale != null && SupportedLocale.isSupported(sessionLocale)) {
                return sessionLocale;
            }
        }

        return SupportedLocale.fromAcceptLanguage(request.getHeader("Accept-Language"));
    }

    public void resolveAndApply(UI ui) {
        HttpServletRequest request = currentRequest();
        Locale locale = resolve(request);
        storeLocale(locale);
        ui.setLocale(locale);
        applyDirection(ui, locale);
    }

    public void setLocale(Locale locale, UI ui) {
        Locale normalized = SupportedLocale.isSupported(locale) ? locale : SupportedLocale.DEFAULT;
        storeLocale(normalized);
        writeCookie(normalized);
        ui.setLocale(normalized);
        applyDirection(ui, normalized);
    }

    public void switchLocale(Locale locale, UI ui) {
        Locale normalized = SupportedLocale.isSupported(locale) ? locale : SupportedLocale.DEFAULT;
        if (normalized.equals(getCurrentLocale())) {
            return;
        }
        setLocale(normalized, ui);
        ui.getPage().reload();
    }

    public Locale getCurrentLocale() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Locale sessionLocale = (Locale) session.getAttribute(SupportedLocale.SESSION_ATTR);
            if (sessionLocale != null) {
                return sessionLocale;
            }
        }
        Locale contextLocale = LocaleContextHolder.getLocale();
        if (contextLocale != null && SupportedLocale.isSupported(contextLocale)) {
            return contextLocale;
        }
        return SupportedLocale.DEFAULT;
    }

    public boolean isRtl() {
        return SupportedLocale.isRtl(getCurrentLocale());
    }

    public void applyDirection(UI ui, Locale locale) {
        String dir = SupportedLocale.isRtl(locale) ? "rtl" : "ltr";
        String lang = SupportedLocale.toCode(locale);
        ui.getPage().executeJs(
                "document.documentElement.setAttribute('dir', $0);"
                        + "document.documentElement.setAttribute('lang', $1);"
                        + "localStorage.setItem($2, $1);",
                dir, lang, SupportedLocale.LOCAL_STORAGE_KEY);
    }

    public void writeCookie(Locale locale) {
        HttpServletResponse response = currentResponse();
        if (response == null) {
            return;
        }
        Cookie cookie = new Cookie(SupportedLocale.COOKIE_NAME, SupportedLocale.toCode(locale));
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }

    private void storeLocale(Locale locale) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SupportedLocale.SESSION_ATTR, locale);
        }
        LocaleContextHolder.setLocale(locale);
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private HttpServletRequest currentRequest() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request instanceof VaadinServletRequest servletRequest) {
            return servletRequest.getHttpServletRequest();
        }
        throw new IllegalStateException("No active Vaadin servlet request");
    }

    private HttpServletResponse currentResponse() {
        VaadinResponse response = VaadinResponse.getCurrent();
        if (response instanceof VaadinServletResponse servletResponse) {
            return servletResponse.getHttpServletResponse();
        }
        return null;
    }
}
