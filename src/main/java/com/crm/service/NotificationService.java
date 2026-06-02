package com.crm.service;

import com.crm.domain.entity.AppNotification;
import com.crm.dto.response.NotificationResponse;
import com.crm.repository.AppNotificationRepository;
import com.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(AppNotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notify(Long userId, String message, String entityType, Long entityId) {
        userRepository.findById(userId).ifPresent(user -> {
            AppNotification n = new AppNotification();
            n.setUser(user);
            n.setMessage(message);
            n.setEntityType(entityType);
            n.setEntityId(entityId);
            notificationRepository.save(n);
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(String username) {
        return userRepository.findByUsername(username).map(user ->
                notificationRepository.findByUser_IdAndReadFalseOrderByCreatedAtDesc(user.getId())
                        .stream().map(NotificationResponse::from).toList()
        ).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return userRepository.findByUsername(username)
                .map(u -> notificationRepository.countByUser_IdAndReadFalse(u.getId()))
                .orElse(0L);
    }

    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllRead(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            List<AppNotification> unread = notificationRepository.findByUser_IdAndReadFalse(user.getId());
            unread.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(unread);
        });
    }
}
