-- Correction du schéma event_publication : event_type nullable selon le modèle JPA Spring Modulith

ALTER TABLE event_publication
    ALTER COLUMN event_type DROP NOT NULL;

ALTER TABLE event_publication_archive
    ALTER COLUMN event_type DROP NOT NULL;
