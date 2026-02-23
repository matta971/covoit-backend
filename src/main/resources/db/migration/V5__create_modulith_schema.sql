-- Spring Modulith — Event Publication Registry
-- Tables gérées par spring-modulith-events-jpa

CREATE TABLE event_publication (
    id                     UUID                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    listener_id            TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 VARCHAR(255)             NOT NULL,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INT                      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_event_publication_status ON event_publication (status);

CREATE TABLE event_publication_archive (
    id                     UUID                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    listener_id            TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 VARCHAR(255)             NOT NULL,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INT                      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
