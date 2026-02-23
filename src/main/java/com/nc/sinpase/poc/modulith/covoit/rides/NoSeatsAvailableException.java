package com.nc.sinpase.poc.modulith.covoit.rides;

import java.util.UUID;

public class NoSeatsAvailableException extends RuntimeException {

    public NoSeatsAvailableException(UUID rideId) {
        super("No seats available for ride: " + rideId);
    }
}
