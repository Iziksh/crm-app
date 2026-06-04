package com.crm.service;

import com.crm.domain.entity.TrustedDevice;
import com.crm.repository.TrustedDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTrustServiceTest {

    @Mock TrustedDeviceRepository repo;

    private DeviceTrustService service;

    @BeforeEach
    void setUp() {
        service = new DeviceTrustService(repo, 14, "DEVICE_TRUST");
    }

    @Test
    void createTrustToken_savesDeviceAndReturnsRawToken() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        String rawToken = service.createTrustToken("user@example.com");

        assertThat(rawToken).isNotBlank();
        ArgumentCaptor<TrustedDevice> captor = ArgumentCaptor.forClass(TrustedDevice.class);
        verify(repo).save(captor.capture());
        TrustedDevice saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo("user@example.com");
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken); // hash differs from raw
        assertThat(saved.getTokenHash()).hasSize(64);            // SHA-256 hex
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void isTokenValid_returnsTrue_forValidToken() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        String rawToken = service.createTrustToken("user@example.com");

        // Capture the saved device and wire findByTokenHash to return it
        ArgumentCaptor<TrustedDevice> captor = ArgumentCaptor.forClass(TrustedDevice.class);
        verify(repo).save(captor.capture());
        TrustedDevice saved = captor.getValue();
        when(repo.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));

        assertThat(service.isTokenValid(rawToken, "user@example.com")).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forWrongEmail() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        String rawToken = service.createTrustToken("user@example.com");

        ArgumentCaptor<TrustedDevice> captor = ArgumentCaptor.forClass(TrustedDevice.class);
        verify(repo).save(captor.capture());
        TrustedDevice saved = captor.getValue();
        when(repo.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));

        assertThat(service.isTokenValid(rawToken, "other@example.com")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        TrustedDevice expired = new TrustedDevice();
        expired.setUserEmail("user@example.com");
        expired.setTokenHash("somehash");
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repo.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThat(service.isTokenValid("anytoken", "user@example.com")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenTokenNotFound() {
        when(repo.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.isTokenValid("nonexistent", "user@example.com")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forNullInputs() {
        assertThat(service.isTokenValid(null, "user@example.com")).isFalse();
        assertThat(service.isTokenValid("token", null)).isFalse();
    }

    @Test
    void getCookieName_returnsConfiguredName() {
        assertThat(service.getCookieName()).isEqualTo("DEVICE_TRUST");
    }

    @Test
    void getTrustDays_returnsConfiguredDays() {
        assertThat(service.getTrustDays()).isEqualTo(14);
    }
}
