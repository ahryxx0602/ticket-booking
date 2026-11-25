package com.ticket.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BE-SEC-009: ProductionSecretsValidator Tests")
class ProductionSecretsValidatorTest {

    @Test
    @DisplayName("BE-SEC-009: Dev profile allows development placeholder secrets")
    void validateProductionSecrets_WhenDevProfile_ShouldSkipStrictProdEnforcement() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        ProductionSecretsValidator validator = new ProductionSecretsValidator(devEnv);
        validator.setSecrets(
                "local-dev-dummy-secret-key-must-be-at-least-32-chars!",
                "sandbox_vnpay_hash_secret_placeholder",
                "default-local-hmac-qr-signing-key-32-chars!",
                false,
                "mock_bot_token_placeholder",
                "dev_webhook_secret_key_placeholder"
        );

        assertThatCode(validator::validateProductionSecrets).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BE-SEC-009: Prod profile rejects default dev placeholder secrets or missing QR/JWT secrets")
    void validateProductionSecrets_WhenProdProfileUsesDevDefaults_ShouldThrowIllegalStateException() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        ProductionSecretsValidator validator = new ProductionSecretsValidator(prodEnv);

        // 1. Default QR secret in prod -> rejected
        validator.setSecrets(
                "prod_unique_jwt_secret_key_minimum_32_bytes_long_2026",
                "prod_vnpay_secret_key_2026",
                "default-local-hmac-qr-signing-key-32-chars!",
                false,
                "",
                ""
        );
        assertThatThrownBy(validator::validateProductionSecrets)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QR_SECRET_KEY");

        // 2. Telegram enabled in prod with default webhook secret -> rejected
        validator.setSecrets(
                "prod_unique_jwt_secret_key_minimum_32_bytes_long_2026",
                "prod_vnpay_secret_key_2026",
                "prod_unique_qr_hmac_signing_key_minimum_32_bytes_2026",
                true,
                "prod_telegram_bot_token_9988776655",
                "dev_webhook_secret_key_placeholder"
        );
        assertThatThrownBy(validator::validateProductionSecrets)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEGRAM_WEBHOOK_SECRET");
    }

    @Test
    @DisplayName("BE-SEC-009: Prod profile succeeds when all mandatory secrets are strong and non-default")
    void validateProductionSecrets_WhenProdProfileHasStrongSecrets_ShouldPass() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        ProductionSecretsValidator validator = new ProductionSecretsValidator(prodEnv);
        validator.setSecrets(
                "prod_unique_jwt_secret_key_minimum_32_bytes_long_2026",
                "prod_vnpay_secret_key_2026",
                "prod_unique_qr_hmac_signing_key_minimum_32_bytes_2026",
                false,
                "",
                ""
        );

        assertThatCode(validator::validateProductionSecrets).doesNotThrowAnyException();
    }
}
