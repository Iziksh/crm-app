package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.i18n.SupportedLocale;
import com.crm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves the locale to use when sending an email.
 * Priority: recipient saved preference → current request locale → English.
 */
@Service
public class EmailLocaleResolver {

    private final UserRepository userRepository;
    private final LocaleService localeService;

    public EmailLocaleResolver(UserRepository userRepository, LocaleService localeService) {
        this.userRepository = userRepository;
        this.localeService = localeService;
    }

    public Locale resolveForEmail(String recipientEmail) {
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            Optional<Locale> fromUser = userRepository.findByEmail(recipientEmail.trim())
                    .map(User::getLocale)
                    .flatMap(this::toSupportedLocale);
            if (fromUser.isPresent()) {
                return fromUser.get();
            }
        }
        return resolveCurrentOrDefault();
    }

    public Locale resolveForUser(User user) {
        if (user != null) {
            Optional<Locale> fromUser = toSupportedLocale(user.getLocale());
            if (fromUser.isPresent()) {
                return fromUser.get();
            }
        }
        return resolveCurrentOrDefault();
    }

    private Optional<Locale> toSupportedLocale(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return SupportedLocale.fromCode(code);
    }

    public Locale resolveCurrentOrDefault() {
        Locale current = localeService.getCurrentLocale();
        return SupportedLocale.isSupported(current) ? current : SupportedLocale.DEFAULT;
    }
}
