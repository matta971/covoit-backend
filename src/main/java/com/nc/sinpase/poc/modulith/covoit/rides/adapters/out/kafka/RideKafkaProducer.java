/*
package com.nc.sinpase.poc.modulith.covoit.rides.adapters.out.kafka;

import com.nc.sinpase.poc.modulith.covoit.rides.RidePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class RideKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(RideKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    RideKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @ApplicationModuleListener
    void on(RidePublishedEvent event) {
        log.info("[KAFKA] Ride published");
        kafkaTemplate.send("ride-published", event.rideId().toString(), event);
    }

}
*/
