package com.nc.sinpase.poc.modulith.covoit.notifications.adapters.in.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
class BookingJmsListener {

    private static final Logger log = LoggerFactory.getLogger(BookingJmsListener.class);

    @JmsListener(destination = "bookings.BookingRequestedEvent")
    void onBookingRequested(String payload) {
        log.info("[JMS CONSUMER] bookings.BookingRequestedEvent received: {}", payload);
    }

    @JmsListener(destination = "bookings.BookingAcceptedEvent")
    void onBookingAccepted(String payload) {
        log.info("[JMS CONSUMER] bookings.BookingAcceptedEvent received: {}", payload);
    }

    @JmsListener(destination = "bookings.BookingRejectedEvent")
    void onBookingRejected(String payload) {
        log.info("[JMS CONSUMER] bookings.BookingRejectedEvent received: {}", payload);
    }

    @JmsListener(destination = "bookings.BookingCanceledEvent")
    void onBookingCanceled(String payload) {
        log.info("[JMS CONSUMER] bookings.BookingCanceledEvent received: {}", payload);
    }
}
