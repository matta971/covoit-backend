-- Module: bookings

CREATE TABLE booking_requests (
    id           UUID         NOT NULL PRIMARY KEY,
    ride_id      UUID         NOT NULL,
    passenger_id UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    requested_at TIMESTAMP    NOT NULL,
    decided_at   TIMESTAMP,
    canceled_at  TIMESTAMP,
    version      INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_booking_requests_ride_id      ON booking_requests(ride_id);
CREATE INDEX idx_booking_requests_passenger_id ON booking_requests(passenger_id);
