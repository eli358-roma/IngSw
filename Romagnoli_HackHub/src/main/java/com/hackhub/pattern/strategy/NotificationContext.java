package com.hackhub.pattern.strategy;

import com.hackhub.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class NotificationContext {

    private final Map<String, NotificationStrategy> strategies;

    @Autowired
    public NotificationContext(Map<String, NotificationStrategy> strategies) {
        this.strategies = strategies;
    }

    public void sendNotification(String type, String message, User recipient) {
        NotificationStrategy strategy = strategies.get(type.toLowerCase() + "Notification");
        if (strategy != null) {
            strategy.sendNotification(message, recipient);
        } else {
            // Default a email
            strategies.get("emailNotification").sendNotification(message, recipient);
        }
    }

    public void sendToAll(String type, String message, Iterable<User> recipients) {
        for (User recipient : recipients) {
            sendNotification(type, message, recipient);
        }
    }
}