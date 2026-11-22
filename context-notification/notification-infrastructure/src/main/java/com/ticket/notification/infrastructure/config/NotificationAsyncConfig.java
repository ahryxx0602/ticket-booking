package com.ticket.notification.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@EnableAsync
public class NotificationAsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        log.info("[NOTIFICATION CONFIG] Initializing Virtual Thread Executor for notificationExecutor");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
