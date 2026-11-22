package com.ticket.notification.infrastructure.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.ticket.notification.domain.port.PushNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebasePushNotificationAdapter implements PushNotificationPort {

    private final FirebaseProperties properties;

    @Override
    public List<String> sendMulticast(List<String> deviceTokens, String title, String body, Map<String, String> data) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return Collections.emptyList();
        }

        if (!properties.isEnabled() || FirebaseApp.getApps().isEmpty()) {
            log.info("[MOCK FCM] Mock send push to {} tokens. Title: '{}', Body: '{}'", deviceTokens.size(), title, body);
            return Collections.emptyList();
        }

        List<String> invalidTokens = new ArrayList<>();
        try {
            List<Message> messages = deviceTokens.stream()
                    .map(token -> Message.builder()
                            .setToken(token)
                            .setNotification(com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .putAllData(data != null ? data : Collections.emptyMap())
                            .build())
                    .toList();

            BatchResponse response = FirebaseMessaging.getInstance()
                    .sendEachAsync(messages)
                    .get(10, TimeUnit.SECONDS);

            log.info("[FCM SENT] Batch send completed. Success: {}, Failure: {}",
                    response.getSuccessCount(), response.getFailureCount());

            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (!r.isSuccessful()) {
                    FirebaseMessagingException ex = r.getException();
                    if (ex != null) {
                        MessagingErrorCode code = ex.getMessagingErrorCode();
                        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                            invalidTokens.add(deviceTokens.get(i));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("[FCM ERROR] Failed to send multicast messages", ex);
        }

        return invalidTokens;
    }

    @Override
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (!properties.isEnabled() || FirebaseApp.getApps().isEmpty()) {
            log.info("[MOCK FCM] Mock send push to topic '{}'. Title: '{}', Body: '{}'", topic, title, body);
            return;
        }

        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : Collections.emptyMap())
                    .build();

            String response = FirebaseMessaging.getInstance()
                    .sendAsync(message)
                    .get(10, TimeUnit.SECONDS);

            log.info("[FCM TOPIC SENT] Successfully sent topic message. Response: {}", response);
        } catch (Exception ex) {
            log.error("[FCM TOPIC ERROR] Failed to send topic message to '{}'", topic, ex);
        }
    }
}
