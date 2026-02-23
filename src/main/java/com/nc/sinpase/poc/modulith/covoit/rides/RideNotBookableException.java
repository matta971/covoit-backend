package com.nc.sinpase.poc.modulith.covoit.rides;

public class RideNotBookableException extends RuntimeException {

    public RideNotBookableException(String reason) {
        super("Ride is not bookable: " + reason);
    }
}
