package com.crm.repository;

import com.crm.domain.entity.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

@Repository
public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {
    // Workspace-specific config first, fall back to global (workspaceId = null)
    Optional<NotificationConfig> findByTopicKeyAndWorkspaceId(String topicKey, Long workspaceId);
    Optional<NotificationConfig> findByTopicKeyAndWorkspaceIdIsNull(String topicKey);
    boolean existsByTopicKeyAndWorkspaceIdIsNull(String topicKey);

    @Query("SELECT c.topicKey FROM NotificationConfig c WHERE c.workspaceId IS NULL")
    Set<String> findGlobalTopicKeys();
}
