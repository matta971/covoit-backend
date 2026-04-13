package com.nc.sinpase.poc.modulith.covoit.rides;

import java.util.UUID;

// Événement de suppression de ride
public record RideDeletedEvent(UUID rideId, UUID driverId) {
}