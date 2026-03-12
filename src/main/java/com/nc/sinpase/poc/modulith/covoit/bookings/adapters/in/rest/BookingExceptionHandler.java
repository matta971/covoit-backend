package com.nc.sinpase.poc.modulith.covoit.bookings.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.ErrorResponse;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingNotFoundException;
import com.nc.sinpase.poc.modulith.covoit.bookings.InvalidStatusTransitionException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class BookingExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBookingNotFound(BookingNotFoundException ex) {
        return ErrorResponse.of("NOT_FOUND", ex.getMessage(), traceId());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidTransition(InvalidStatusTransitionException ex) {
        return ErrorResponse.of("INVALID_STATUS_TRANSITION", ex.getMessage(), traceId());
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "unknown";
    }
}
