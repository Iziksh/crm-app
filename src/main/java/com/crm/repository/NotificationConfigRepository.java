package com.crm.repository;

import com.crm.domain.entity.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {
    // Workspace-specific config first, fall back to global (workspaceId = null)
    Optional<NotificationConfig> findByTopicKeyAndWorkspaceId(String topicKey, Long workspaceId);
    Optional<NotificationConfig> findByTopicKeyAndWorkspaceIdIsNull(String topicKey);
    boolean existsByTopicKeyAndWorkspaceIdIsNull(String topicKey);
}
