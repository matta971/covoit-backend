package com.nc.sinpase.poc.modulith.covoit.rides;

import java.util.UUID;

public record RideCanceledEvent(UUID rideId, UUID driverId) {}
