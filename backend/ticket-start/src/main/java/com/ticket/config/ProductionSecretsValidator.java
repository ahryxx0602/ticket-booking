package com.ticket.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * BE-SEC-009: Cưỡng chế kiểm tra toàn bộ khóa bí mật bắt buộc trên môi trường Production (profile 'prod').
 * Từ chối khởi động (Fail-Fast) nếu phát hiện sử dụng giá trị mặc định của môi trường dev hoặc khóa quá ngắn.
 */
@Slf4j
@Component
public class ProductionSecretsValidator {

    private static final Set<String> FORBIDDEN_DEV_SECRETS = Set.of(
            "local-dev-dummy-secret-key-must-be-at-least-32-chars!",
            "defaultsecretkeywithatleast32bytesforhmacsha256!",
            "dev_jwt_secret_key_minimum_32_characters_long",
            "your_jwt_secret_key_minimum_32_characters_for_hmac_sha256",
            "sandbox_vnpay_hash_secret_placeholder",
            "your_vnpay_sandbox_hash_secret_placeholder",
            "default-local-hmac-qr-signing-key-32-chars!",
            "ticket_booking_qr_secret_key_2026",
            "dev_webhook_secret_key_placeholder",
            "telegram_webhook_secret_key",
            "mock_bot_token_placeholder",
            "dummy_telegram_bot_token"
    );

    private final Environment environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.payment.vnpay.hash-secret:}")
    private String vnpayHashSecret;

    @Value("${app.notification.qr.secret-key:}")
    private String qrSecretKey;

    @Value("${app.notification.telegram.enabled:false}")
    private boolean telegramEnabled;

    @Value("${app.notification.telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${app.notification.telegram.webhook-secret:}")
    private String telegramWebhookSecret;

    public ProductionSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    public void setSecrets(String jwtSecret,
                           String vnpayHashSecret,
                           String qrSecretKey,
                           boolean telegramEnabled,
                           String telegramBotToken,
                           String telegramWebhookSecret) {
        this.jwtSecret = jwtSecret;
        this.vnpayHashSecret = vnpayHashSecret;
        this.qrSecretKey = qrSecretKey;
        this.telegramEnabled = telegramEnabled;
        this.telegramBotToken = telegramBotToken;
        this.telegramWebhookSecret = telegramWebhookSecret;
    }

    @PostConstruct
    public void validateProductionSecrets() {
        if (environment == null || !environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }

        requireStrongSecret("JWT_SECRET (app.jwt.secret)", jwtSecret, 32);
        requireStrongSecret("VNPAY_HASH_SECRET (app.payment.vnpay.hash-secret)", vnpayHashSecret, 16);
        requireStrongSecret("QR_SECRET_KEY (app.notification.qr.secret-key)", qrSecretKey, 32);

        if (telegramEnabled) {
            requireStrongSecret("TELEGRAM_BOT_TOKEN (app.notification.telegram.bot-token)", telegramBotToken, 16);
            requireStrongSecret("TELEGRAM_WEBHOOK_SECRET (app.notification.telegram.webhook-secret)", telegramWebhookSecret, 16);
        }

        log.info("[SECURITY VALIDATOR] All mandatory production secrets verified successfully.");
    }

    private void requireStrongSecret(String name, String value, int minLength) {
        if (value == null || value.trim().length() < minLength) {
            throw new IllegalStateException("Production security violation: " + name
                    + " must be configured with at least " + minLength + " characters");
        }
        if (FORBIDDEN_DEV_SECRETS.contains(value.trim().toLowerCase())) {
            throw new IllegalStateException("Production security violation: " + name
                    + " must not use development placeholder or default repository secret");
        }
    }
}
