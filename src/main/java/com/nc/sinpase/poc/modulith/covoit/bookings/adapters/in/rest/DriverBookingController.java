package com.nc.sinpase.poc.modulith.covoit.bookings.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.CurrentUserProvider;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingRequestView;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/driver/me")
class DriverBookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;

    DriverBookingController(BookingService bookingService, CurrentUserProvider currentUserProvider) {
        this.bookingService = bookingService;
        this.currentUserProvider = currentUserProvider;
    }

    // Note: returns all requests received across all rides of this driver.
    // For per-ride requests, use /api/rides/{rideId}/booking-requests (not yet exposed).
    @GetMapping("/booking-requests")
    List<BookingRequestView> myReceivedRequests() {
        return bookingService.findByDriver(currentUserProvider.getUserId());
    }

    @PostMapping("/booking-requests/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void accept(@PathVariable UUID id) {
        bookingService.accept(id, currentUserProvider.getUserId());
    }

    @PostMapping("/booking-requests/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reject(@PathVariable UUID id) {
        bookingService.reject(id, currentUserProvider.getUserId());
    }
}
