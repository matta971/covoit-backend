package com.nc.sinpase.poc.modulith.covoit.bookings;

import java.util.UUID;

public record BookingCanceledEvent(UUID bookingRequestId, UUID rideId, UUID passengerId, boolean wasAccepted) {}
