-- Module: auth

CREATE TABLE refresh_sessions (
    id                      UUID         NOT NULL PRIMARY KEY,
    user_id                 UUID         NOT NULL REFERENCES users(id),
    refresh_token_hash      VARCHAR(255) NOT NULL,
    created_at              TIMESTAMP    NOT NULL,
    expires_at              TIMESTAMP    NOT NULL,
    revoked_at              TIMESTAMP,
    replaced_by_session_id  UUID,
    device_id               VARCHAR(255),
    user_agent              VARCHAR(512),
    ip                      VARCHAR(64)
);

CREATE INDEX idx_refresh_sessions_token_hash ON refresh_sessions(refresh_token_hash);
CREATE INDEX idx_refresh_sessions_user_id    ON refresh_sessions(user_id);
