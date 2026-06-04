package com.crm.repository;

import com.crm.domain.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByEntityType(String entityType);
    Optional<Topic> findByName(String name);
    Optional<Topic> findByTopicKey(String topicKey);
    boolean existsByName(String name);
    boolean existsByTopicKey(String topicKey);
}
