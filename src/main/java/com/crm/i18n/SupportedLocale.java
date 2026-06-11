package com.crm.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SupportedLocale {

    public static final String COOKIE_NAME = "CRM_LOCALE";
    public static final String SESSION_ATTR = "crm.locale";
    public static final String LOCAL_STORAGE_KEY = "crm.locale";

    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale HEBREW = Locale.forLanguageTag("he");
    public static final Locale DEFAULT = ENGLISH;

    public static final List<Locale> ALL = List.of(ENGLISH, HEBREW);

    private SupportedLocale() {}

    public static boolean isSupported(Locale locale) {
        if (locale == null) {
            return false;
        }
        return ALL.stream().anyMatch(supported -> supported.getLanguage().equals(locale.getLanguage()));
    }

    public static boolean isRtl(Locale locale) {
        return locale != null && HEBREW.getLanguage().equals(locale.getLanguage());
    }

    public static Optional<Locale> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "en" -> Optional.of(ENGLISH);
            case "he" -> Optional.of(HEBREW);
            default -> Optional.empty();
        };
    }

    public static String toCode(Locale locale) {
        if (locale == null) {
            return "en";
        }
        return HEBREW.getLanguage().equals(locale.getLanguage()) ? "he" : "en";
    }

    public static Locale fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return DEFAULT;
        }
        for (String part : header.split(",")) {
            String tag = part.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (tag.startsWith("he")) {
                return HEBREW;
            }
            if (tag.startsWith("en")) {
                return ENGLISH;
            }
        }
        return DEFAULT;
    }
}
