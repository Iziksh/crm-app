package com.crm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpServiceTest {

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(5, 6);
    }

    @Test
    void generateAndStore_returns6DigitNumericCode() {
        String otp = otpService.generateAndStore("user@example.com");

        assertThat(otp).matches("\\d{6}");
    }

    @Test
    void validate_returnsTrue_forCorrectCode() {
        String otp = otpService.generateAndStore("user@example.com");

        assertThat(otpService.validate("user@example.com", otp)).isTrue();
    }

    @Test
    void validate_returnsFalse_forWrongCode() {
        otpService.generateAndStore("user@example.com");

        assertThat(otpService.validate("user@example.com", "000000")).isFalse();
    }

    @Test
    void validate_isSingleUse_secondCallReturnsFalse() {
        String otp = otpService.generateAndStore("user@example.com");
        otpService.validate("user@example.com", otp); // consume

        assertThat(otpService.validate("user@example.com", otp)).isFalse();
    }

    @Test
    void validate_returnsFalse_forUnknownEmail() {
        assertThat(otpService.validate("nobody@example.com", "123456")).isFalse();
    }

    @Test
    void validate_isCaseInsensitive_onEmail() {
        String otp = otpService.generateAndStore("User@Example.COM");

        assertThat(otpService.validate("user@example.com", otp)).isTrue();
    }

    @Test
    void generateAndStore_overwritesPreviousCode() {
        String first = otpService.generateAndStore("user@example.com");
        String second = otpService.generateAndStore("user@example.com");

        // Only the latest code should be valid
        assertThat(otpService.validate("user@example.com", second)).isTrue();
        assertThat(otpService.validate("user@example.com", first)).isFalse();
    }
}
