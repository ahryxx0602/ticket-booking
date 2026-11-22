package com.ticket.notification.domain.port;

import java.util.List;
import java.util.Map;

public interface PushNotificationPort {
    List<String> sendMulticast(List<String> deviceTokens, String title, String body, Map<String, String> data);
    void sendToTopic(String topic, String title, String body, Map<String, String> data);
}
