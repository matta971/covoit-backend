package com.nc.sinpase.poc.modulith.covoit.notifications.adapters.in.events;

import com.nc.sinpase.poc.modulith.covoit.rides.RideCanceledEvent;
import com.nc.sinpase.poc.modulith.covoit.rides.RidePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class RideEventListener {

    private static final Logger log = LoggerFactory.getLogger(RideEventListener.class);

    @ApplicationModuleListener
    void on(RidePublishedEvent event) {
        log.info("[NOTIFICATION] Ride published: rideId={} driver={} {}→{} at {}",
                event.rideId(), event.driverId(), event.from(), event.to(), event.departureTime());
    }

    @ApplicationModuleListener
    void on(RideCanceledEvent event) {
        log.info("[NOTIFICATION] Ride canceled: rideId={} driver={}",
                event.rideId(), event.driverId());
    }
}
