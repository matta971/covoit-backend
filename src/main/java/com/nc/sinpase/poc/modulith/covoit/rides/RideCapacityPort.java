package com.nc.sinpase.poc.modulith.covoit.rides;

import java.util.List;
import java.util.UUID;

/**
 * Port exposé par le module rides, consommé par le module bookings.
 * Permet de réserver/libérer une place et de vérifier la réservabilité.
 */
public interface RideCapacityPort {

    void reserveSeat(UUID rideId, UUID passengerId);

    void releaseSeat(UUID rideId);

    void assertRideBookable(UUID rideId);

    UUID getDriverId(UUID rideId);

    List<UUID> findRideIdsByDriver(UUID driverId);
}
