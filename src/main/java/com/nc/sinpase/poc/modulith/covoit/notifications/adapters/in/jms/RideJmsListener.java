package com.nc.sinpase.poc.modulith.covoit.notifications.adapters.in.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
class RideJmsListener {

    private static final Logger log = LoggerFactory.getLogger(RideJmsListener.class);

    @JmsListener(destination = "rides.RidePublishedEvent")
    void onRidePublished(String payload) {
        log.info("[JMS CONSUMER] rides.RidePublishedEvent consumed: {}", payload);
    }

    @JmsListener(destination = "rides.RideCanceledEvent")
    void onRideCanceled(String payload) {
        log.info("[JMS CONSUMER] rides.RideCanceledEvent consumed: {}", payload);
    }
}