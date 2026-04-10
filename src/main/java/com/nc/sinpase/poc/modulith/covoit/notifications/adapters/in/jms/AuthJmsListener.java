package com.nc.sinpase.poc.modulith.covoit.notifications.adapters.in.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes auth events from JMS as raw JSON strings to avoid importing
 * the auth module (notifications does not declare auth as an allowed dependency).
 * In Phase 2, a standalone auth-service would be the actual consumer.
 */
@Component
class AuthJmsListener {

    private static final Logger log = LoggerFactory.getLogger(AuthJmsListener.class);

    @JmsListener(destination = "auth.UserRegisteredEvent")
    void onUserRegistered(String payload) {
        log.info("[JMS CONSUMER] auth.UserRegisteredEvent received: {}", payload);
    }
}
