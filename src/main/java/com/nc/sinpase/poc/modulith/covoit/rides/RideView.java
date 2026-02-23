package com.nc.sinpase.poc.modulith.covoit.rides;

import java.time.Instant;
import java.util.UUID;

public record RideView(
        UUID rideId,
        UUID driverId,
        String from,
        String to,
        Instant departureTime,
        int totalSeats,
        int availableSeats,
        String status
) {}
