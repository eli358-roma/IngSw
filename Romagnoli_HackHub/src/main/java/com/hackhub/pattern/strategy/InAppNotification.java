package com.hackhub.pattern.strategy;

import com.hackhub.model.User;
import org.springframework.stereotype.Component;

@Component
public class InAppNotification implements NotificationStrategy {

    @Override
    public void sendNotification(String message, User recipient) {
        // Simulazione notifica in-app
        System.out.println("🔔 Notifica in-app per " + recipient.getUsername() + ": " + message);
        // In produzione, qui salveresti la notifica nel DB
    }

    @Override
    public String getType() {
        return "IN_APP";
    }
}