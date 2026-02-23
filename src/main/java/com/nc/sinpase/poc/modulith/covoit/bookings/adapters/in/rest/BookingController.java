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
@RequestMapping("/api")
class BookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;

    BookingController(BookingService bookingService, CurrentUserProvider currentUserProvider) {
        this.bookingService = bookingService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/rides/{rideId}/booking-requests")
    @ResponseStatus(HttpStatus.CREATED)
    BookingRequestView requestBooking(@PathVariable UUID rideId) {
        return bookingService.requestBooking(rideId, currentUserProvider.getUserId());
    }

    @GetMapping("/me/bookings")
    List<BookingRequestView> myBookings() {
        return bookingService.findByPassenger(currentUserProvider.getUserId());
    }

    @PostMapping("/booking-requests/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID id) {
        bookingService.cancel(id, currentUserProvider.getUserId());
    }
}
