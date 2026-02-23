package com.nc.sinpase.poc.modulith.covoit.rides.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.CurrentUserProvider;
import com.nc.sinpase.poc.modulith.covoit.rides.RideService;
import com.nc.sinpase.poc.modulith.covoit.rides.RideView;
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
class DriverRideController {

    private final RideService rideService;
    private final CurrentUserProvider currentUserProvider;

    DriverRideController(RideService rideService, CurrentUserProvider currentUserProvider) {
        this.rideService = rideService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/driver/me/rides")
    List<RideView> myRides() {
        return rideService.findByDriver(currentUserProvider.getUserId());
    }

    @PostMapping("/rides/{rideId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID rideId) {
        rideService.cancel(rideId, currentUserProvider.getUserId());
    }
}
