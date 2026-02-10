package com.hackhub.pattern.strategy;

import com.hackhub.model.User;
import org.springframework.stereotype.Component;

@Component
public class EmailNotification implements NotificationStrategy {

    @Override
    public void sendNotification(String message, User recipient) {
        // Simulazione invio email
        System.out.println("📧 Invio email a " + recipient.getEmail() + ": " + message);
        // In produzione, qui integreresti un servizio email reale
    }

    @Override
    public String getType() {
        return "EMAIL";
    }
}