-- Tresor Database Initialization Script
-- This script creates the necessary tables and indexes for the Tresor application

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- For text search optimization

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    total_messages INTEGER NOT NULL DEFAULT 0,
    total_spent_usd DECIMAL(10, 2) NOT NULL DEFAULT 0.00
);

CREATE INDEX idx_users_email ON users(email);

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    delivery_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unlock_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    arweave_id VARCHAR(43),
    deployment_receipt_json TEXT,
    content_hash VARCHAR(64),
    cost_usd DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    unlocked_at TIMESTAMP,
    delivered_at TIMESTAMP,
    next_unlock_check TIMESTAMP,
    unlock_attempts INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,

    -- New fields for encryption modes
    encryption_mode VARCHAR(20) NOT NULL DEFAULT 'PASSWORD_ENCRYPTION',
    deployment_tier VARCHAR(20) DEFAULT 'budget',
    password_salt VARCHAR(64),
    password_hint VARCHAR(500),
    pbkdf2_iterations INTEGER,
    encryption_iv VARCHAR(32),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for messages
CREATE INDEX idx_message_user ON messages(user_id);
CREATE INDEX idx_message_status ON messages(status);
CREATE INDEX idx_message_unlock_date ON messages(unlock_date);
CREATE INDEX idx_message_deployment_tier ON messages(deployment_tier);
CREATE INDEX idx_message_encryption_mode ON messages(encryption_mode);
CREATE INDEX idx_message_status_tier ON messages(status, deployment_tier);

-- Composite index for batch deployment queries
CREATE INDEX idx_messages_batch_deployment ON messages(status, deployment_tier, created_at)
WHERE status = 'PENDING_BATCH';

-- Index for unlock scheduler queries
CREATE INDEX idx_messages_ready_to_unlock ON messages(status, unlock_date, next_unlock_check)
WHERE status = 'LOCKED';

-- Delivery addresses table (for updateable email addresses)
CREATE TABLE IF NOT EXISTS delivery_addresses (
    id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    delivery_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, etc.
    address VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_address_message ON delivery_addresses(message_id);

-- Unlocked messages table (encrypted with user key)
CREATE TABLE IF NOT EXISTS unlocked_messages (
    id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    encrypted_content BYTEA NOT NULL, -- Re-encrypted with user's personal key
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accessed_at TIMESTAMP,
    access_count INTEGER NOT NULL DEFAULT 0,

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_unlocked_message_user ON unlocked_messages(user_id);
CREATE INDEX idx_unlocked_message_accessed ON unlocked_messages(accessed_at);

-- Audit log table (for compliance and debugging)
CREATE TABLE IF NOT EXISTS audit_log (
    id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    message_id VARCHAR(255),
    user_id VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    details JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_message ON audit_log(message_id);
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_created ON audit_log(created_at);

-- Statistics view
CREATE OR REPLACE VIEW message_statistics AS
SELECT
    status,
    deployment_tier,
    encryption_mode,
    COUNT(*) as count,
    SUM(cost_usd) as total_cost,
    AVG(cost_usd) as avg_cost,
    MIN(created_at) as first_created,
    MAX(created_at) as last_created
FROM messages
GROUP BY status, deployment_tier, encryption_mode;

-- Batch deployment statistics view
CREATE OR REPLACE VIEW batch_deployment_stats AS
SELECT
    deployment_tier,
    COUNT(*) as pending_count,
    SUM(cost_usd) as estimated_cost,
    MIN(created_at) as oldest_message,
    MAX(created_at) as newest_message
FROM messages
WHERE status = 'PENDING_BATCH'
GROUP BY deployment_tier;

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO tresor;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO tresor;

-- Insert test user (for development)
INSERT INTO users (id, email, created_at)
VALUES ('test-user-1', 'test@tresor.io', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Tresor database initialized successfully!';
    RAISE NOTICE 'Tables created: users, messages, delivery_addresses, unlocked_messages, audit_log';
    RAISE NOTICE 'Views created: message_statistics, batch_deployment_stats';
END $$;
