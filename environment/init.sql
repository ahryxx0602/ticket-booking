-- Database Initialization Script for Ticket Booking
CREATE DATABASE IF NOT EXISTS ticket_booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_booking_db;

-- 1. Identity Module Tables
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Catalog Module Tables
CREATE TABLE IF NOT EXISTS stations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_code VARCHAR(20) NOT NULL UNIQUE,
    departure_station_id BIGINT NOT NULL,
    arrival_station_id BIGINT NOT NULL,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,
    transport_type VARCHAR(20) NOT NULL, -- FLIGHT, TRAIN, BUS
    total_seats INT NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (departure_station_id) REFERENCES stations(id),
    FOREIGN KEY (arrival_station_id) REFERENCES stations(id)
);

CREATE TABLE IF NOT EXISTS seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD', -- VIP, STANDARD, ECONOMY
    price_multiplier DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_trip_seat (trip_id, seat_number),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

-- 3. Booking Module Tables
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    phone VARCHAR(20),
    trip_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, HELD, CONFIRMED, CANCELLED, EXPIRED
    total_amount DECIMAL(12,2) NOT NULL,
    expires_at DATETIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_booking_user (user_id),
    INDEX idx_booking_ref (booking_reference),
    INDEX idx_booking_status_exp (status, expires_at),
    INDEX idx_booking_trip_status_exp (trip_id, status, expires_at)
);

CREATE TABLE IF NOT EXISTS booking_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- 4. Payment Module Tables
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(50) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    payment_method VARCHAR(20) NOT NULL, -- VNPAY, MOMO
    transaction_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED
    payment_time DATETIME,
    refund_amount DECIMAL(12,2),
    refund_reason VARCHAR(255),
    refunded_at DATETIME,
    reconciliation_required BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pay_booking (booking_id),
    INDEX idx_pay_status_created (status, created_at),
    INDEX idx_pay_ref (payment_reference)
);

-- 5. Notification Module Tables
CREATE TABLE IF NOT EXISTS user_device_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(512) NOT NULL UNIQUE,
    device_type VARCHAR(20) NOT NULL DEFAULT 'WEB',
    last_active_at DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_tokens (user_id),
    INDEX idx_fcm_token (fcm_token)
);

CREATE TABLE IF NOT EXISTS telegram_account_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    telegram_chat_id BIGINT NOT NULL UNIQUE,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tele_user (user_id),
    INDEX idx_tele_chat (telegram_chat_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    recipient VARCHAR(512) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NULL,
    content TEXT NOT NULL,
    metadata_json TEXT NULL,
    idempotency_key VARCHAR(128) NULL UNIQUE,
    error_message VARCHAR(500) NULL,
    sent_at DATETIME NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_noti_user_created (user_id, created_at),
    INDEX idx_noti_status (status)
);

-- Seed Data (Initial Stations & Sample Data)
INSERT INTO stations (code, name, city) VALUES 
('HAN', 'Sân bay Quốc tế Nội Bài', 'Hà Nội'),
('SGN', 'Sân bay Quốc tế Tân Sơn Nhất', 'Hồ Chí Minh'),
('DAD', 'Sân bay Quốc tế Đà Nẵng', 'Đà Nẵng')
ON DUPLICATE KEY UPDATE name=VALUES(name);
