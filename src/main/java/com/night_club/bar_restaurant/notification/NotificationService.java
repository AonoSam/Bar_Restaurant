package com.night_club.bar_restaurant.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String NOTIFICATIONS_TOPIC = "/topic/notifications";

    /**
     * Single entry point for every notification trigger in the system -
     * persists the row AND pushes it live to any connected manager. Callers
     * (OrderPaidNotificationListener, StockDeductionListener, DarajaService)
     * never touch the repository or messaging template directly, so the
     * persist+push pairing can never drift apart at a call site.
     */
    @Transactional
    public void create(NotificationType type, String title, String message, Long relatedEntityId) {
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .build();

        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend(NOTIFICATIONS_TOPIC, NotificationResponse.fromEntity(saved));
    }

    public List<NotificationResponse> getRecent(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable).stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: " + id));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findAll().stream()
                .filter(n -> !n.isRead())
                .toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void delete(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: " + id);
        }
        notificationRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        notificationRepository.deleteAll();
    }
}