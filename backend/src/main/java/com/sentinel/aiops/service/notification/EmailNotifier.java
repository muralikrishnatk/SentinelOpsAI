package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotifier implements Notifier {
    public String channel() { return "email"; }
    public void send(Incident i, String message) {
        // In a real system this integrates with an SMTP/SES client.
        log.info("[EMAIL] -> {} | {}", i.getAssignee(), message);
    }
}
