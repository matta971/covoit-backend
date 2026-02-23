package com.nc.sinpase.poc.modulith.covoit.bookings;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking request not found: " + bookingId);
    }
}
