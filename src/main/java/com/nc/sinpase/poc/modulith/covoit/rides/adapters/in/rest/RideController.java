package com.nc.sinpase.poc.modulith.covoit.rides.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.CurrentUserProvider;
import com.nc.sinpase.poc.modulith.covoit.rides.PublishRideCommand;
import com.nc.sinpase.poc.modulith.covoit.rides.RideService;
import com.nc.sinpase.poc.modulith.covoit.rides.RideView;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
class RideController {

    private final RideService rideService;
    private final CurrentUserProvider currentUserProvider;

    RideController(RideService rideService, CurrentUserProvider currentUserProvider) {
        this.rideService = rideService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RideView publish(@RequestBody @Valid PublishRideRequest request) {
        return rideService.publish(new PublishRideCommand(
                currentUserProvider.getUserId(),
                request.from(), request.to(),
                request.departureTime(), request.totalSeats()
        ));
    }

    @GetMapping
    List<RideView> search(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return rideService.search(from, to, date);
    }

    @GetMapping("/{rideId}")
    RideView getById(@PathVariable UUID rideId) {
        return rideService.findById(rideId);
    }
}
