package com.nc.sinpase.poc.modulith.covoit.bookings;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * Voir RidePublishedEvent pour les notes architecture.
 */
@Externalized("bookings.BookingAcceptedEvent")
public record BookingAcceptedEvent(UUID bookingRequestId, UUID rideId, UUID passengerId) {}
