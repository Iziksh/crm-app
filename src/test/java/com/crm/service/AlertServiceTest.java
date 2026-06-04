package com.crm.service;

import com.crm.domain.entity.Alert;
import com.crm.domain.entity.User;
import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.AlertState;
import com.crm.dto.response.AlertResponse;
import java.util.NoSuchElementException;
import com.crm.repository.AlertRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock AlertRepository alertRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AlertService alertService;

    @Test
    void sendAlertFull_createsAlert_forExistingUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(alertRepository.findFirstByUser_IdAndEntityTypeAndEntityIdAndResendCutoffAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(alertRepository.existsByDedupKey(any())).thenReturn(false);
        when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        alertService.sendAlertFull("admin", "A-05", "Account Modified", "desc",
                AlertImportance.NORMAL, 60, "ACCOUNT", 1L, "/accounts/1");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Account Modified");
        assertThat(saved.getAlertState()).isEqualTo(AlertState.NEW);
        assertThat(saved.getTopicKey()).isEqualTo("A-05");
    }

    @Test
    void sendAlertFull_skipsAlert_whenUserNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        alertService.sendAlertFull("nobody", null, "Alert", "desc",
                AlertImportance.NORMAL, 0, null, null, null);

        verify(alertRepository, never()).save(any());
    }

    @Test
    void sendAlertFull_deduplicates_whenDedupKeyAlreadyExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        // resendDelaySeconds=0 skips the window check; dedupKey check fires instead
        when(alertRepository.existsByDedupKey(any())).thenReturn(true);

        alertService.sendAlertFull("admin", "A-05", "Alert", "desc",
                AlertImportance.NORMAL, 0, "ACCOUNT", 1L, "/accounts/1");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void sendAlertFull_deduplicates_whenResendWindowActive() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        Alert existing = new Alert();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(alertRepository.findFirstByUser_IdAndEntityTypeAndEntityIdAndResendCutoffAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.of(existing));

        alertService.sendAlertFull("admin", "A-05", "Alert", "desc",
                AlertImportance.NORMAL, 60, "ACCOUNT", 1L, "/accounts/1");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void markAsRead_updatesAlertState() {
        Alert alert = new Alert();
        alert.setAlertState(AlertState.NEW);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AlertResponse response = alertService.markAsRead(1L);

        assertThat(response.alertState()).isEqualTo(AlertState.READ);
    }

    @Test
    void markAsAccepted_updatesAlertState() {
        Alert alert = new Alert();
        alert.setAlertState(AlertState.READ);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AlertResponse response = alertService.markAsAccepted(1L);

        assertThat(response.alertState()).isEqualTo(AlertState.ACCEPTED);
    }

    @Test
    void countUnread_returnsCount_forUser() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(alertRepository.countByUser_IdAndAlertState(1L, AlertState.NEW)).thenReturn(4L);

        assertThat(alertService.countUnread("admin")).isEqualTo(4L);
    }

    @Test
    void countUnread_returnsZero_whenUserNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThat(alertService.countUnread("nobody")).isZero();
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> alertService.delete(99L));
    }

    @Test
    void buildDeepLink_returnsCorrectPath() {
        assertThat(AlertService.buildDeepLink("ACCOUNT", 5L)).isEqualTo("/accounts/5");
        assertThat(AlertService.buildDeepLink("LEAD", 3L)).isEqualTo("/leads/3");
        assertThat(AlertService.buildDeepLink("OPPORTUNITY", 7L)).isEqualTo("/opportunities/7");
        assertThat(AlertService.buildDeepLink(null, 1L)).isNull();
    }

    @Test
    void expireStaleAlerts_updatesOldReadAlerts() {
        Alert stale = new Alert();
        stale.setAlertState(AlertState.READ);
        when(alertRepository.findByAlertStateAndCreatedAtBefore(
                eq(AlertState.READ), any(LocalDateTime.class))).thenReturn(List.of(stale));

        alertService.expireStaleAlerts();

        assertThat(stale.getAlertState()).isEqualTo(AlertState.EXPIRED);
        verify(alertRepository).saveAll(anyList());
    }
}
