package com.nc.sinpase.poc.modulith.covoit.rides.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

interface SpringDataRideRepository extends JpaRepository<RideJpaEntity, UUID>, JpaSpecificationExecutor<RideJpaEntity> {

    List<RideJpaEntity> findByDriverId(UUID driverId);
}
