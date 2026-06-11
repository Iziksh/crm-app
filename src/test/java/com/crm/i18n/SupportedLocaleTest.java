package com.crm.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportedLocaleTest {

    @Test
    void fromCode_resolvesEnglishAndHebrew() {
        assertEquals(SupportedLocale.ENGLISH, SupportedLocale.fromCode("en").orElseThrow());
        assertEquals(SupportedLocale.HEBREW, SupportedLocale.fromCode("he").orElseThrow());
    }

    @Test
    void isRtl_onlyForHebrew() {
        assertFalse(SupportedLocale.isRtl(SupportedLocale.ENGLISH));
        assertTrue(SupportedLocale.isRtl(SupportedLocale.HEBREW));
    }

    @Test
    void fromAcceptLanguage_prefersFirstSupportedTag() {
        assertEquals(SupportedLocale.HEBREW,
                SupportedLocale.fromAcceptLanguage("he-IL,en-US;q=0.9"));
        assertEquals(SupportedLocale.ENGLISH,
                SupportedLocale.fromAcceptLanguage("en-US,he-IL;q=0.8"));
    }

    @Test
    void toCode_roundTrips() {
        assertEquals("en", SupportedLocale.toCode(Locale.ENGLISH));
        assertEquals("he", SupportedLocale.toCode(Locale.forLanguageTag("he")));
    }
}
