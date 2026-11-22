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
            // Thử kiểm tra tương đối từ thư mục cha nếu app chạy từ submodule (vd: ticket-start)
            File fallbackFile = new File(".." + File.separator + properties.getCredentialsPath());
            if (fallbackFile.exists()) {
                credFile = fallbackFile;
            } else {
                // Thử tìm trong thư mục environment của project root
                File envRootFile = new File("environment", "firebase-service-account.json");
                File parentEnvRootFile = new File(".." + File.separator + "environment", "firebase-service-account.json");
                if (envRootFile.exists()) {
                    credFile = envRootFile;
                } else if (parentEnvRootFile.exists()) {
                    credFile = parentEnvRootFile;
                } else {
                    log.warn("[FIREBASE CONFIG] FCM credential file not found at: {}. Fallback to mock mode.", credFile.getAbsolutePath());
                    return;
                }
            }
        }

        try (InputStream serviceAccount = new FileInputStream(credFile)) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(properties.getProjectId());

            if (properties.getStorageBucket() != null && !properties.getStorageBucket().trim().isEmpty()) {
                builder.setStorageBucket(properties.getStorageBucket());
            }

            FirebaseOptions options = builder.build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("[FIREBASE CONFIG] FirebaseApp initialized successfully with project: {} and bucket: {}",
                        properties.getProjectId(), properties.getStorageBucket());
            }
        } catch (Exception ex) {
            log.error("[FIREBASE CONFIG ERROR] Failed to initialize FirebaseApp", ex);
        }
    }
}
