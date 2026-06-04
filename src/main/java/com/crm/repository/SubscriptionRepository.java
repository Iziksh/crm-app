package com.crm.repository;

import com.crm.domain.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Subscription> findByTopic_IdAndActiveTrue(Long topicId);
    Optional<Subscription> findByIdAndUser_Id(Long id, Long userId);
}
