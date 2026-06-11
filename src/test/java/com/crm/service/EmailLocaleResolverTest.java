package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.i18n.SupportedLocale;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailLocaleResolverTest {

    @Mock UserRepository userRepository;
    @Mock LocaleService localeService;

    private EmailLocaleResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EmailLocaleResolver(userRepository, localeService);
    }

    @Test
    void resolveForEmail_usesRecipientSavedPreference() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLocale("he");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertEquals(SupportedLocale.HEBREW, resolver.resolveForEmail("user@example.com"));
    }

    @Test
    void resolveForEmail_fallsBackToCurrentLocaleWhenNoSavedPreference() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(localeService.getCurrentLocale()).thenReturn(SupportedLocale.HEBREW);

        assertEquals(SupportedLocale.HEBREW, resolver.resolveForEmail("user@example.com"));
    }

    @Test
    void resolveForEmail_fallsBackToEnglishWhenNothingAvailable() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(localeService.getCurrentLocale()).thenReturn(Locale.forLanguageTag("fr"));

        assertEquals(SupportedLocale.DEFAULT, resolver.resolveForEmail("unknown@example.com"));
    }

    @Test
    void resolveForUser_usesUserLocale() {
        User user = new User();
        user.setLocale("he");

        assertEquals(SupportedLocale.HEBREW, resolver.resolveForUser(user));
    }
}
