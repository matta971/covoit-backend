package com.nc.sinpase.poc.modulith.covoit.bookings;

import java.util.UUID;

public record BookingRequestedEvent(UUID bookingRequestId, UUID rideId, UUID passengerId) {}
