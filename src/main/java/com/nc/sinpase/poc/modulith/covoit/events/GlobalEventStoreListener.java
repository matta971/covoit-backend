package com.nc.sinpase.poc.modulith.covoit.events;

import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.Externalized;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class GlobalEventStoreListener {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public GlobalEventStoreListener(EventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(Object event) {
        if (!event.getClass().isAnnotationPresent(Externalized.class)) {
            return;
        }
        Externalized ext = event.getClass().getAnnotation(Externalized.class);
        eventService.saveEvent(
                ext.value(),
                toJson(event)
        );
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}