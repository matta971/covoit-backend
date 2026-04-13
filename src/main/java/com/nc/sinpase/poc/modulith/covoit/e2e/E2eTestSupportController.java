package com.nc.sinpase.poc.modulith.covoit.e2e;

import com.nc.sinpase.poc.modulith.covoit.rides.RideView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Profile("e2e")
@Validated
@RestController
@RequestMapping("/api/test-support")
class E2eTestSupportController {

    private final E2eTestSupportService e2eTestSupportService;

    E2eTestSupportController(E2eTestSupportService e2eTestSupportService) {
        this.e2eTestSupportService = e2eTestSupportService;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset() {
        e2eTestSupportService.resetDatabase();
    }

    @PostMapping("/seed/default-users")
    E2eTestSupportService.SeededUsersResponse seedDefaultUsers() {
        return e2eTestSupportService.seedDefaultUsers();
    }

    @PostMapping("/seed/ride")
    @ResponseStatus(HttpStatus.CREATED)
    RideView seedRide(@RequestBody @Valid SeedRideRequest request) {
        return e2eTestSupportService.seedRide(new E2eTestSupportService.SeedRideRequest(
                request.driverEmail(),
                request.from(),
                request.to(),
                request.departureTime(),
                request.totalSeats()
        ));
    }

    @PostMapping("/seed/dataset")
    @ResponseStatus(HttpStatus.CREATED)
    void seedDataset(@RequestBody @Valid SeedDatasetRequest request) {
        e2eTestSupportService.seedDataset(new E2eTestSupportService.SeedDatasetRequest(
                request.users().stream().map(u -> new E2eTestSupportService.SeedDatasetRequest.UserSeed(
                        u.id(), u.email(), u.password(), u.displayName(), u.phoneDialCode(), u.phoneNumber()
                )).toList(),
                request.rides().stream().map(r -> new E2eTestSupportService.SeedDatasetRequest.RideSeed(
                        r.id(), r.driverEmail(), r.from(), r.to(), r.departureTime(), r.totalSeats()
                )).toList(),
                request.bookings().stream().map(b -> new E2eTestSupportService.SeedDatasetRequest.BookingSeed(
                        b.id(), b.rideId(), b.passengerEmail(), b.status()
                )).toList()
        ));
    }

    record SeedRideRequest(
            @NotBlank @Email String driverEmail,
            @NotBlank String from,
            @NotBlank String to,
            @NotNull @Future Instant departureTime,
            @Min(1) int totalSeats
    ) {
    }

    record SeedDatasetRequest(
            @NotNull List<@Valid UserSeed> users,
            @NotNull List<@Valid RideSeed> rides,
            @NotNull List<@Valid BookingSeed> bookings
    ) {
        record UserSeed(
                @NotNull UUID id,
                @NotBlank @Email String email,
                @NotBlank String password,
                @NotBlank String displayName,
                @NotBlank String phoneDialCode,
                @NotBlank String phoneNumber
        ) {}

        record RideSeed(
                @NotNull UUID id,
                @NotBlank @Email String driverEmail,
                @NotBlank String from,
                @NotBlank String to,
                @NotNull Instant departureTime,
                @Min(1) int totalSeats
        ) {}

        record BookingSeed(
                @NotNull UUID id,
                @NotNull UUID rideId,
                @NotBlank @Email String passengerEmail,
                @NotBlank String status
        ) {}
    }
}
