package com.nc.sinpase.poc.modulith.covoit.rides.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository {

    void save(Ride ride);

    Optional<Ride> findById(UUID id);

    List<Ride> search(String from, String to, Instant dateFrom, Instant dateTo);

    List<Ride> findByDriverId(UUID driverId);
}
