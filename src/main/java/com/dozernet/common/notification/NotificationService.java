package com.dozernet.common.notification;

import com.dozernet.common.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Subject/publisher in the Observer pattern. Spring injects every
 * {@link NotificationObserver} bean, so publishing an event fans out to all
 * channels (in-app + email) automatically. Also exposes read helpers for the UI.
 */
@Service
public class NotificationService {

    private final List<NotificationObserver> observers;
    private final NotificationRepository notificationRepository;

    public NotificationService(List<NotificationObserver> observers,
                               NotificationRepository notificationRepository) {
        this.observers = observers;
        this.notificationRepository = notificationRepository;
    }

    /** Notify a single user across all channels. */
    public void notify(User recipient, String title, String message) {
        NotificationEvent event = new NotificationEvent(recipient, title, message);
        observers.forEach(observer -> observer.onEvent(event));
    }

    public List<Notification> forUser(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFlagFalse(user);
    }

    public void markAllRead(User user) {
        List<Notification> items = notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
        items.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(items);
    }
}
