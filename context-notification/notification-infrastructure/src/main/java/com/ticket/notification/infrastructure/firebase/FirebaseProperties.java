package com.ticket.notification.infrastructure.firebase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.notification.fcm")
public class FirebaseProperties {
    private boolean enabled = false;
    private String credentialsPath;
    private String projectId = "ticket-booking-dev";
}
