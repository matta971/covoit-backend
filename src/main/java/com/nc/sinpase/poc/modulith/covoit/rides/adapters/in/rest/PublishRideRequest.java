package com.nc.sinpase.poc.modulith.covoit.rides.adapters.in.rest;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

record PublishRideRequest(
        @NotBlank String from,
        @NotBlank String to,
        @NotNull @Future Instant departureTime,
        @Min(1) int totalSeats
) {}
