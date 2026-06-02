package com.crm.repository;

import com.crm.domain.entity.AppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndReadFalse(Long userId);
    Page<AppNotification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<AppNotification> findByUser_IdAndReadFalse(Long userId);
}
