package com.nc.sinpase.poc.modulith.covoit.events.domain;

import com.nc.sinpase.poc.modulith.covoit.events.adapters.out.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventJpaEntity, Long> {
}