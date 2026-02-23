-- Recréation des tables Spring Modulith avec toutes colonnes nullable
-- (le modèle JPA de spring-modulith-events-jpa ne déclare pas nullable=false)

DROP TABLE IF EXISTS event_publication;
DROP TABLE IF EXISTS event_publication_archive;

CREATE TABLE event_publication (
    id                     UUID PRIMARY KEY,
    publication_date       TIMESTAMP WITH TIME ZONE,
    listener_id            TEXT,
    serialized_event       TEXT,
    event_type             TEXT,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 VARCHAR(255),
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_event_publication_status ON event_publication (status);

CREATE TABLE event_publication_archive (
    id                     UUID PRIMARY KEY,
    publication_date       TIMESTAMP WITH TIME ZONE,
    listener_id            TEXT,
    serialized_event       TEXT,
    event_type             TEXT,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 VARCHAR(255),
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INT NOT NULL DEFAULT 0
);
