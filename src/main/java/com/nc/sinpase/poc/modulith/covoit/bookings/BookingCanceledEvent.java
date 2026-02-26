package com.nc.sinpase.poc.modulith.covoit.bookings;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * Voir RidePublishedEvent pour les notes architecture.
 */
@Externalized("bookings.BookingCanceledEvent")
public record BookingCanceledEvent(UUID bookingRequestId, UUID rideId, UUID passengerId, boolean wasAccepted) {}
