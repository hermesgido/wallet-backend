CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    endpoint VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_code INTEGER,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_idempotency_record UNIQUE (idempotency_key, endpoint, user_id)
);

CREATE INDEX idx_idempotency_lookup ON idempotency_records(idempotency_key, endpoint, user_id);
