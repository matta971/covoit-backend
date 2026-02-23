package com.nc.sinpase.poc.modulith.covoit.bookings;

import java.time.Instant;
import java.util.UUID;

public record BookingRequestView(
        UUID bookingRequestId,
        UUID rideId,
        UUID passengerId,
        String status,
        Instant requestedAt,
        Instant decidedAt,
        Instant canceledAt
) {}
