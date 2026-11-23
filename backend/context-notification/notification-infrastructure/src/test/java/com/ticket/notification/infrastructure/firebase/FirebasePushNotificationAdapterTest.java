package com.ticket.notification.infrastructure.firebase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Firebase Push Notification Adapter Tests")
class FirebasePushNotificationAdapterTest {

    @Test
    @DisplayName("When FCM is disabled, sendMulticast should return empty list without throwing exception")
    void testSendMulticast_DisabledMode() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setEnabled(false);

        FirebasePushNotificationAdapter adapter = new FirebasePushNotificationAdapter(properties);

        List<String> invalidTokens = adapter.sendMulticast(
                List.of("tok1", "tok2"),
                "Title",
                "Body",
                Map.of("key", "val")
        );

        assertNotNull(invalidTokens);
        assertTrue(invalidTokens.isEmpty());
    }

    @Test
    @DisplayName("When tokens list is empty, sendMulticast should return immediately")
    void testSendMulticast_EmptyTokens() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setEnabled(true);

        FirebasePushNotificationAdapter adapter = new FirebasePushNotificationAdapter(properties);

        List<String> invalidTokens = adapter.sendMulticast(Collections.emptyList(), "T", "B", null);
        assertNotNull(invalidTokens);
        assertTrue(invalidTokens.isEmpty());
    }
}
