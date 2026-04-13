CREATE TABLE events (
                        id          UUID        NOT NULL PRIMARY KEY,
                        type        VARCHAR(255) NOT NULL,
                        payload     JSONB       NOT NULL,
                        created_at  TIMESTAMP   NOT NULL
);

CREATE INDEX idx_events_type       ON events(type);
CREATE INDEX idx_events_created_at ON events(created_at);