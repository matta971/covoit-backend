package com.nc.sinpase.poc.modulith.covoit.notifications.adapters.in.events;

import com.nc.sinpase.poc.modulith.covoit.bookings.BookingAcceptedEvent;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingCanceledEvent;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingRejectedEvent;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    @ApplicationModuleListener
    void on(BookingRequestedEvent event) {
        log.info("[NOTIFICATION] Booking requested: bookingId={} rideId={} passenger={}",
                event.bookingRequestId(), event.rideId(), event.passengerId());
    }

    @ApplicationModuleListener
    void on(BookingAcceptedEvent event) {
        log.info("[NOTIFICATION] Booking accepted: bookingId={} rideId={} passenger={}",
                event.bookingRequestId(), event.rideId(), event.passengerId());
    }

    @ApplicationModuleListener
    void on(BookingRejectedEvent event) {
        log.info("[NOTIFICATION] Booking rejected: bookingId={} rideId={} passenger={}",
                event.bookingRequestId(), event.rideId(), event.passengerId());
    }

    @ApplicationModuleListener
    void on(BookingCanceledEvent event) {
        log.info("[NOTIFICATION] Booking canceled: bookingId={} rideId={} passenger={} wasAccepted={}",
                event.bookingRequestId(), event.rideId(), event.passengerId(), event.wasAccepted());
    }
}
