package com.nc.sinpase.poc.modulith.covoit.e2e;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("e2e")
@RestController
@RequestMapping("/api/test-support")
class E2eTestSupportController {

    private final E2eTestSupportService service;

    E2eTestSupportController(E2eTestSupportService service) {
        this.service = service;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset() {
        service.resetDatabase();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    void seedRoles(@RequestBody List<E2eTestSupportService.RoleRow> rows) {
        service.seedRoles(rows);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    void seedUsers(@RequestBody List<E2eTestSupportService.UserRow> rows) {
        service.seedUsers(rows);
    }

    @PostMapping("/user-roles")
    @ResponseStatus(HttpStatus.CREATED)
    void seedUserRoles(@RequestBody List<E2eTestSupportService.UserRoleRow> rows) {
        service.seedUserRoles(rows);
    }

    @PostMapping("/rides")
    @ResponseStatus(HttpStatus.CREATED)
    void seedRides(@RequestBody List<E2eTestSupportService.RideRow> rows) {
        service.seedRides(rows);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    void seedBookings(@RequestBody List<E2eTestSupportService.BookingRow> rows) {
        service.seedBookings(rows);
    }

}
