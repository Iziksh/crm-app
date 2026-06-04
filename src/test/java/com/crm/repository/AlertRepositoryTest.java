package com.crm.repository;

import com.crm.domain.entity.Alert;
import com.crm.domain.entity.User;
import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.AlertState;
import com.crm.domain.enums.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AlertRepositoryTest {

    @Autowired AlertRepository alertRepository;
    @Autowired UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashed");
        user = userRepository.save(user);
    }

    @Test
    void countByUser_IdAndAlertState_returnsCorrectCount() {
        alertRepository.save(alert(user, AlertState.NEW, null));
        alertRepository.save(alert(user, AlertState.NEW, null));
        alertRepository.save(alert(user, AlertState.READ, null));

        assertThat(alertRepository.countByUser_IdAndAlertState(user.getId(), AlertState.NEW)).isEqualTo(2);
        assertThat(alertRepository.countByUser_IdAndAlertState(user.getId(), AlertState.READ)).isEqualTo(1);
    }

    @Test
    void existsByDedupKey_returnsTrue_whenKeyExists() {
        Alert a = alert(user, AlertState.NEW, "A-05:ACCOUNT:1:1:2026-06-04");
        alertRepository.save(a);

        assertThat(alertRepository.existsByDedupKey("A-05:ACCOUNT:1:1:2026-06-04")).isTrue();
    }

    @Test
    void existsByDedupKey_returnsFalse_whenKeyMissing() {
        assertThat(alertRepository.existsByDedupKey("nonexistent-key")).isFalse();
    }

    @Test
    void findByUser_IdAndAlertStateIn_returnsMatchingAlerts() {
        alertRepository.save(alert(user, AlertState.NEW, null));
        alertRepository.save(alert(user, AlertState.READ, null));
        alertRepository.save(alert(user, AlertState.ACCEPTED, null));

        List<Alert> result = alertRepository.findByUser_IdAndAlertStateInOrderByCreatedAtDesc(
                user.getId(), List.of(AlertState.NEW, AlertState.READ));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Alert::getAlertState)
                .containsExactlyInAnyOrder(AlertState.NEW, AlertState.READ);
    }

    @Test
    void findByAlertStateAndCreatedAtBefore_returnsOldAlerts() {
        Alert old = alert(user, AlertState.READ, null);
        alertRepository.save(old);

        // All saved alerts have createdAt = null (no auditing in test) so we use far future cutoff
        List<Alert> result = alertRepository.findByAlertStateAndCreatedAtBefore(
                AlertState.READ, LocalDateTime.now().plusYears(100));

        assertThat(result).isNotEmpty();
    }

    @Test
    void findFirstByUser_ResendCutoffAtAfter_returnsAlert_withinWindow() {
        Alert a = alert(user, AlertState.NEW, null);
        a.setEntityType("ACCOUNT");
        a.setEntityId(1L);
        a.setResendCutoffAt(LocalDateTime.now().plusMinutes(5));
        alertRepository.save(a);

        var result = alertRepository.findFirstByUser_IdAndEntityTypeAndEntityIdAndResendCutoffAtAfter(
                user.getId(), "ACCOUNT", 1L, LocalDateTime.now());

        assertThat(result).isPresent();
    }

    @Test
    void findFirstByUser_ResendCutoffAtAfter_returnsEmpty_whenWindowExpired() {
        Alert a = alert(user, AlertState.NEW, null);
        a.setEntityType("ACCOUNT");
        a.setEntityId(1L);
        a.setResendCutoffAt(LocalDateTime.now().minusMinutes(5)); // already expired
        alertRepository.save(a);

        var result = alertRepository.findFirstByUser_IdAndEntityTypeAndEntityIdAndResendCutoffAtAfter(
                user.getId(), "ACCOUNT", 1L, LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    private Alert alert(User u, AlertState state, String dedupKey) {
        Alert a = new Alert();
        a.setUser(u);
        a.setName("Test Alert");
        a.setAlertState(state);
        a.setImportance(AlertImportance.NORMAL);
        a.setChannel(NotificationChannel.IN_APP);
        a.setDedupKey(dedupKey);
        a.setExpiresAt(LocalDateTime.now().plusDays(30));
        return a;
    }
}
