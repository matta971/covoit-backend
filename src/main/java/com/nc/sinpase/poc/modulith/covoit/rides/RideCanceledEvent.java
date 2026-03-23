package com.nc.sinpase.poc.modulith.covoit.rides;

import com.nc.sinpase.poc.modulith.covoit.events.domain.Event;
import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * Voir RidePublishedEvent pour les notes architecture.
 */
@Externalized("rides.RideCanceledEvent")
public record RideCanceledEvent(UUID rideId, UUID driverId) {}
