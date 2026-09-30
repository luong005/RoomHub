-- Run against the same database configured in spring.datasource.url.
-- Standalone audit table: no foreign keys, triggers or changes to existing tables.
-- If the table already exists, inspect its schema instead of replacing it.
CREATE TABLE IF NOT EXISTS security_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    phone_number VARCHAR(50) NULL,
    ip_address VARCHAR(45) NOT NULL,
    endpoint VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL COMMENT 'UTC',
    message VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_security_events_created_at (created_at),
    INDEX idx_security_events_ip_created (ip_address, created_at),
    INDEX idx_security_events_phone_created (phone_number, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
