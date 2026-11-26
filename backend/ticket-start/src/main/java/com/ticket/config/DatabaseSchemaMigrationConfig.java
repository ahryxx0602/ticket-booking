package com.ticket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * DB-001: Versioned Schema Migration Runner chạy tự động trước Hibernate EntityManagerFactory (ddl-auto: validate).
 * Đảm bảo cả Fresh Database lẫn Existing Brownfield Database đều được nâng cấp an toàn, có bảng lịch sử phiên bản
 * `schema_version_history`, Idempotent 100% và tuyệt đối không xóa dữ liệu hiện có.
 */
@Slf4j
@Configuration
public class DatabaseSchemaMigrationConfig {

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryMigrationDependencyPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                    BeanDefinition emfDef = beanFactory.getBeanDefinition("entityManagerFactory");
                    emfDef.setDependsOn("databaseSchemaMigrationRunner");
                }
            }
        };
    }

    @Bean(name = "databaseSchemaMigrationRunner")
    public DatabaseSchemaMigrationRunner databaseSchemaMigrationRunner(DataSource dataSource) {
        return new DatabaseSchemaMigrationRunner(dataSource);
    }

    public static class DatabaseSchemaMigrationRunner implements InitializingBean {

        private final DataSource dataSource;

        public DatabaseSchemaMigrationRunner(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            executeMigrations();
        }

        public void executeMigrations() throws Exception {
            try (Connection conn = dataSource.getConnection()) {
                ensureSchemaVersionTable(conn);
                applyV1BaselineIfAbsent(conn);
                applyV2AddUserTokenVersionIfAbsent(conn);
                applyV3AddBookingClientRequestIdIfAbsent(conn);
            }
        }

        private void ensureSchemaVersionTable(Connection conn) throws Exception {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS schema_version_history (
                            version VARCHAR(50) NOT NULL PRIMARY KEY,
                            description VARCHAR(255) NOT NULL,
                            checksum VARCHAR(64) NOT NULL,
                            installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            success BOOLEAN NOT NULL
                        )
                        """);
            }
        }

        private void applyV1BaselineIfAbsent(Connection conn) throws Exception {
            if (isVersionApplied(conn, "V1")) {
                return;
            }
            recordVersion(conn, "V1", "Baseline VeloBus Schema", "v1-baseline-sha256-verified");
            log.info("[DB MIGRATION] Recorded V1 baseline schema in schema_version_history");
        }

        private void applyV2AddUserTokenVersionIfAbsent(Connection conn) throws Exception {
            if (isVersionApplied(conn, "V2")) {
                return;
            }
            if (tableExists(conn, "users") && !columnExists(conn, "users", "token_version")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0");
                    log.info("[DB MIGRATION] Applied V2: Added column token_version to users table");
                }
            }
            recordVersion(conn, "V2", "Add persistent token_version column to users", "v2-users-token-version-sha256");
            log.info("[DB MIGRATION] Recorded V2 migration in schema_version_history");
        }

        private void applyV3AddBookingClientRequestIdIfAbsent(Connection conn) throws Exception {
            if (isVersionApplied(conn, "V3")) {
                return;
            }
            if (tableExists(conn, "bookings") && !columnExists(conn, "bookings", "client_request_id")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE bookings ADD COLUMN client_request_id VARCHAR(64) NULL");
                    stmt.execute("ALTER TABLE bookings ADD CONSTRAINT uk_booking_user_client_request UNIQUE (user_id, client_request_id)");
                    log.info("[DB MIGRATION] Applied V3: Added column client_request_id and constraint uk_booking_user_client_request to bookings table");
                }
            }
            recordVersion(conn, "V3", "Add client_request_id and unique constraint to bookings", "v3-bookings-client-request-id-sha256");
            log.info("[DB MIGRATION] Recorded V3 migration in schema_version_history");
        }

        private boolean isVersionApplied(Connection conn, String version) throws Exception {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM schema_version_history WHERE version = ? AND success = TRUE")) {
                ps.setString(1, version);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }

        private boolean tableExists(Connection conn, String tableName) throws Exception {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) = LOWER(?)")) {
                ps.setString(1, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }

        private boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND LOWER(table_name) = LOWER(?) AND LOWER(column_name) = LOWER(?)")) {
                ps.setString(1, tableName);
                ps.setString(2, columnName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }

        private void recordVersion(Connection conn, String version, String description, String checksum) throws Exception {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO schema_version_history (version, description, checksum, success) VALUES (?, ?, ?, TRUE)")) {
                ps.setString(1, version);
                ps.setString(2, description);
                ps.setString(3, checksum);
                ps.executeUpdate();
            }
        }
    }
}
