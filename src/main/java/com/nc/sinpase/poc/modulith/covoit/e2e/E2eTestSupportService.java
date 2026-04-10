package com.nc.sinpase.poc.modulith.covoit.e2e;

import com.nc.sinpase.poc.modulith.covoit.identity.CreateUserCommand;
import com.nc.sinpase.poc.modulith.covoit.identity.UserService;
import com.nc.sinpase.poc.modulith.covoit.identity.UserView;
import com.nc.sinpase.poc.modulith.covoit.rides.PublishRideCommand;
import com.nc.sinpase.poc.modulith.covoit.rides.RideService;
import com.nc.sinpase.poc.modulith.covoit.rides.RideView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
class E2eTestSupportService {

    private static final String DEFAULT_PASSWORD = "Password1234xxx";
    private static final String DEFAULT_DRIVER_EMAIL = "test@test.com";
    private static final String DEFAULT_PASSENGER_EMAIL = "passager@test.com";
    private static final String DEFAULT_ROLE_NAME = "ROLE_USER";

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RideService rideService;

    E2eTestSupportService(JdbcTemplate jdbcTemplate,
                          UserService userService,
                          PasswordEncoder passwordEncoder,
                          RideService rideService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.rideService = rideService;
    }

    void resetDatabase() {
        // DELETE en ordre FK (enfants avant parents) pour éviter les deadlocks
        // que TRUNCATE CASCADE provoque avec le scheduler de republication de Modulith
        jdbcTemplate.execute("DELETE FROM booking_requests");
        jdbcTemplate.execute("DELETE FROM event_publication");
        jdbcTemplate.execute("DELETE FROM event_publication_archive");
        jdbcTemplate.execute("DELETE FROM rides");
        jdbcTemplate.execute("DELETE FROM refresh_sessions");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM roles");

        jdbcTemplate.update(
                "INSERT INTO roles (id, name) VALUES (?, ?)",
                UUID.randomUUID(),
                DEFAULT_ROLE_NAME
        );
    }

    SeededUsersResponse seedDefaultUsers() {
        UserView driver = ensureUser(
                DEFAULT_DRIVER_EMAIL,
                "Conducteur Test",
                "+33",
                "612345678"
        );
        UserView passenger = ensureUser(
                DEFAULT_PASSENGER_EMAIL,
                "Passager Test",
                "+33",
                "698765432"
        );

        return new SeededUsersResponse(driver.id().toString(), passenger.id().toString(), DEFAULT_PASSWORD);
    }

    RideView seedRide(SeedRideRequest request) {
        UserView driver = userService.findByEmail(request.driverEmail())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + request.driverEmail()));

        return rideService.publish(new PublishRideCommand(
                driver.id(),
                request.from(),
                request.to(),
                request.departureTime(),
                request.totalSeats()
        ));
    }

    void seedDataset(SeedDatasetRequest req) {
        Instant now = Instant.now();

        for (SeedDatasetRequest.UserSeed u : req.users()) {
            jdbcTemplate.update("""
                    INSERT INTO users (id, email, password_hash, display_name, phone_dial_code, phone_number, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                    """,
                    u.id(), u.email(), passwordEncoder.encode(u.password()),
                    u.displayName(), u.phoneDialCode(), u.phoneNumber(),
                    Timestamp.from(now), Timestamp.from(now));

            jdbcTemplate.update("""
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT ?, id FROM roles WHERE name = ?
                    """,
                    u.id(), DEFAULT_ROLE_NAME);
        }

        for (SeedDatasetRequest.RideSeed r : req.rides()) {
            UUID driverId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?", UUID.class, r.driverEmail());

            jdbcTemplate.update("""
                    INSERT INTO rides (id, driver_id, from_location, to_location, departure_time, total_seats, available_seats, status, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'SCHEDULED', 0, ?, ?)
                    """,
                    r.id(), driverId, r.from(), r.to(),
                    Timestamp.from(r.departureTime()), r.totalSeats(), r.totalSeats(),
                    Timestamp.from(now), Timestamp.from(now));
        }

        for (SeedDatasetRequest.BookingSeed b : req.bookings()) {
            UUID passengerId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?", UUID.class, b.passengerEmail());

            jdbcTemplate.update("""
                    INSERT INTO booking_requests (id, ride_id, passenger_id, status, requested_at, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 0, ?, ?)
                    """,
                    b.id(), b.rideId(), passengerId, b.status(),
                    Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        }
    }

    private UserView ensureUser(String email, String displayName, String phoneDialCode, String phoneNumber) {
        return userService.findByEmail(email)
                .orElseGet(() -> userService.createUser(new CreateUserCommand(
                        email,
                        passwordEncoder.encode(DEFAULT_PASSWORD),
                        displayName,
                        phoneDialCode,
                        phoneNumber
                )));
    }

    record SeededUsersResponse(String driverUserId, String passengerUserId, String password) {
    }

    record SeedRideRequest(String driverEmail, String from, String to, Instant departureTime, int totalSeats) {
    }

    record SeedDatasetRequest(
            List<UserSeed> users,
            List<RideSeed> rides,
            List<BookingSeed> bookings
    ) {
        record UserSeed(UUID id, String email, String password, String displayName,
                        String phoneDialCode, String phoneNumber) {}

        record RideSeed(UUID id, String driverEmail, String from, String to,
                        Instant departureTime, int totalSeats) {}

        record BookingSeed(UUID id, UUID rideId, String passengerEmail, String status) {}
    }
}
