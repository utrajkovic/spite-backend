package com.spite.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.spite.backend.repository.FcmTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {

    private final FcmTokenRepository tokenRepo;

    public PushNotificationService(FcmTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    public void sendToUser(String username, String title, String body, String type, String extraData) {
        tokenRepo.findByUsername(username).ifPresent(fcmToken -> {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type);

                if (extraData != null) {
                    builder.putData("from", extraData);
                }

                FirebaseMessaging.getInstance().send(builder.build());
            } catch (Exception e) {
                System.err.println("Push notification failed for " + username + ": " + e.getMessage());
            }
        });
    }
}
