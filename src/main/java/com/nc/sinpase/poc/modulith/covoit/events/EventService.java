package com.nc.sinpase.poc.modulith.covoit.events;

import com.nc.sinpase.poc.modulith.covoit.events.adapters.out.EventJpaEntity;
import com.nc.sinpase.poc.modulith.covoit.events.domain.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public void saveEvent(String type, String payload) {
        repository.save(new EventJpaEntity(type,payload,Instant.now()));
    }
}
