package com.nc.sinpase.poc.modulith.covoit.rides.adapters.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UpdateRideRequest(
    @NotBlank(message = "Origin is required")
    String from,
    
    @NotBlank(message = "Destination is required")
    String to,
    
    @Positive(message = "Total seats must be positive")
    Integer totalSeats
) {}