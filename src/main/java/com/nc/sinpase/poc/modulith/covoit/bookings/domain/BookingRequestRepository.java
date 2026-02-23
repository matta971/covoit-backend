package com.nc.sinpase.poc.modulith.covoit.bookings.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRequestRepository {

    void save(BookingRequest request);

    Optional<BookingRequest> findById(UUID id);

    List<BookingRequest> findByPassengerId(UUID passengerId);

    List<BookingRequest> findByRideId(UUID rideId);
}
