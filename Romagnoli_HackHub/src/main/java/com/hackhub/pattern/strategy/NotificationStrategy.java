package com.hackhub.pattern.strategy;

import com.hackhub.model.User;

public interface NotificationStrategy {
    void sendNotification(String message, User recipient);
    String getType();
}