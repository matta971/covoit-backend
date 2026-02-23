-- Module: rides

CREATE TABLE rides (
    id              UUID         NOT NULL PRIMARY KEY,
    driver_id       UUID         NOT NULL,
    from_location   VARCHAR(255) NOT NULL,
    to_location     VARCHAR(255) NOT NULL,
    departure_time  TIMESTAMP    NOT NULL,
    total_seats     INT          NOT NULL,
    available_seats INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    version         INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_rides_driver_id ON rides(driver_id);
CREATE INDEX idx_rides_status    ON rides(status);
