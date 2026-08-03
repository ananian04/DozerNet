package com.dozernet.common.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observer that "sends" an email. For this academic build we log the email to
 * the console instead of configuring a real SMTP server, which keeps the demo
 * self-contained. Swapping in JavaMailSender later requires no caller changes.
 */
@Component
public class EmailNotificationObserver implements NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserver.class);

    @Override
    public void onEvent(NotificationEvent event) {
        log.info("[EMAIL] To: {} | Subject: {} | Body: {}",
                event.recipient().getEmail(), event.title(), event.message());
    }
}
