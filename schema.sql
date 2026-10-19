-- Digital Steganography - schema (MySQL 8+)
CREATE DATABASE IF NOT EXISTS stego_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stego_db;

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT          NOT NULL,
    operation     ENUM('EMBED','EXTRACT') NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    payload_size  INT          NOT NULL DEFAULT 0,   -- bytes
    psnr          DOUBLE       NULL,                 -- only for EMBED
    success       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_history_user_date (user_id, created_at DESC)
) ENGINE=InnoDB;
