package com.nc.sinpase.poc.modulith.covoit.rides;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * Voir RidePublishedEvent pour les notes architecture.
 */
@Externalized("rides.RideCanceledEvent")
public record RideCanceledEvent(UUID rideId, UUID driverId) {}
