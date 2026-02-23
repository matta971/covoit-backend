package com.nc.sinpase.poc.modulith.covoit.bookings;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String from, String to) {
        super("Invalid status transition: " + from + " → " + to);
    }
}
