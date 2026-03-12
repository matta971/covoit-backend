package com.nc.sinpase.poc.modulith.covoit.rides.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.ErrorResponse;
import com.nc.sinpase.poc.modulith.covoit.rides.NoSeatsAvailableException;
import com.nc.sinpase.poc.modulith.covoit.rides.RideNotBookableException;
import com.nc.sinpase.poc.modulith.covoit.rides.RideNotFoundException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class RideExceptionHandler {

    @ExceptionHandler(RideNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleRideNotFound(RideNotFoundException ex) {
        return ErrorResponse.of("NOT_FOUND", ex.getMessage(), traceId());
    }

    @ExceptionHandler(RideNotBookableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleRideNotBookable(RideNotBookableException ex) {
        return ErrorResponse.of("RIDE_NOT_BOOKABLE", ex.getMessage(), traceId());
    }

    @ExceptionHandler(NoSeatsAvailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleNoSeats(NoSeatsAvailableException ex) {
        return ErrorResponse.of("NO_SEATS_AVAILABLE", ex.getMessage(), traceId());
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "unknown";
    }
}
