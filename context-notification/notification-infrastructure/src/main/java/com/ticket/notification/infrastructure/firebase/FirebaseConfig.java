package com.ticket.notification.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final FirebaseProperties properties;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("[FIREBASE CONFIG] FCM is disabled by configuration (app.notification.fcm.enabled=false). Running in Mock mode.");
            return;
        }

        if (properties.getCredentialsPath() == null || properties.getCredentialsPath().trim().isEmpty()) {
            log.warn("[FIREBASE CONFIG] FCM enabled but credentialsPath is empty. Fallback to mock mode.");
            return;
        }

        File credFile = new File(properties.getCredentialsPath());
        if (!credFile.exists()) {
            log.warn("[FIREBASE CONFIG] FCM credential file not found at: {}. Fallback to mock mode.", credFile.getAbsolutePath());
            return;
        }

        try (InputStream serviceAccount = new FileInputStream(credFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(properties.getProjectId())
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("[FIREBASE CONFIG] FirebaseApp initialized successfully with project: {}", properties.getProjectId());
            }
        } catch (Exception ex) {
            log.error("[FIREBASE CONFIG ERROR] Failed to initialize FirebaseApp", ex);
        }
    }
}
