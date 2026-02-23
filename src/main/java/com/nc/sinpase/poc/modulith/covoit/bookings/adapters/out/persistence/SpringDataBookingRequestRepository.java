package com.nc.sinpase.poc.modulith.covoit.bookings.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataBookingRequestRepository extends JpaRepository<BookingRequestJpaEntity, UUID> {

    List<BookingRequestJpaEntity> findByPassengerId(UUID passengerId);

    List<BookingRequestJpaEntity> findByRideId(UUID rideId);
}
