CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_wallet_user UNIQUE (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_wallet_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallet_currency CHECK (char_length(currency) = 3)
);

CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    sender_wallet_id BIGINT NOT NULL,
    receiver_wallet_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    failure_reason VARCHAR(255),
    initiated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT uk_transfer_reference UNIQUE (reference),
    CONSTRAINT fk_transfer_sender FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transfer_receiver FOREIGN KEY (receiver_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transfer_initiated_by FOREIGN KEY (initiated_by) REFERENCES users(id),
    CONSTRAINT chk_transfer_amount CHECK (amount > 0),
    CONSTRAINT chk_transfer_wallets CHECK (sender_wallet_id <> receiver_wallet_id)
);

CREATE TABLE top_up_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_topup_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_topup_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_topup_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id),
    CONSTRAINT chk_topup_amount CHECK (amount > 0)
);

CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    balance_before NUMERIC(19,2) NOT NULL,
    balance_after NUMERIC(19,2) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    transfer_id BIGINT,
    top_up_request_id BIGINT,
    performed_by BIGINT,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_wallet_tx_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT fk_wallet_tx_topup FOREIGN KEY (top_up_request_id) REFERENCES top_up_requests(id),
    CONSTRAINT fk_wallet_tx_performed_by FOREIGN KEY (performed_by) REFERENCES users(id),
    CONSTRAINT chk_wallet_tx_amount CHECK (amount > 0),
    CONSTRAINT chk_wallet_tx_balance_after CHECK (balance_after >= 0)
);

CREATE INDEX idx_transfer_sender_wallet ON transfers(sender_wallet_id);
CREATE INDEX idx_transfer_receiver_wallet ON transfers(receiver_wallet_id);
CREATE INDEX idx_topup_user ON top_up_requests(user_id);
CREATE INDEX idx_wallet_tx_wallet ON wallet_transactions(wallet_id);