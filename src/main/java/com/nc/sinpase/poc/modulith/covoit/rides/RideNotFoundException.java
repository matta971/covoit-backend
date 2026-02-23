package com.nc.sinpase.poc.modulith.covoit.rides;

import java.util.UUID;

public class RideNotFoundException extends RuntimeException {

    public RideNotFoundException(UUID rideId) {
        super("Ride not found: " + rideId);
    }
}
