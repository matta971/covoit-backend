package com.nc.sinpase.poc.modulith.covoit.e2e;

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
public class E2eTestSupportService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public E2eTestSupportService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public void resetDatabase() {
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
    }

    public void seedRoles(List<RoleRow> rows) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO roles (id, name) VALUES (?, ?)",
                rows, rows.size(),
                (ps, row) -> {
                    ps.setObject(1, row.id());
                    ps.setString(2, row.name());
                });
    }

    public void seedUsers(List<UserRow> rows) {
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO users (id, email, password_hash, display_name, phone_dial_code, phone_number, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """,
                rows, rows.size(),
                (ps, row) -> {
                    ps.setObject(1, row.id());
                    ps.setString(2, row.email());
                    ps.setString(3, passwordEncoder.encode(row.password()));
                    ps.setString(4, row.displayName());
                    ps.setString(5, row.phoneDialCode());
                    ps.setString(6, row.phoneNumber());
                    ps.setTimestamp(7, Timestamp.from(now));
                    ps.setTimestamp(8, Timestamp.from(now));
                });
    }

    public void seedUserRoles(List<UserRoleRow> rows) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                rows, rows.size(),
                (ps, row) -> {
                    ps.setObject(1, row.userId());
                    ps.setObject(2, row.roleId());
                });
    }

    public void seedRides(List<RideRow> rows) {
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO rides (id, driver_id, from_location, to_location, departure_time, total_seats, available_seats, status, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'SCHEDULED', 0, ?, ?)
                """,
                rows, rows.size(),
                (ps, row) -> {
                    ps.setObject(1, row.id());
                    ps.setObject(2, row.driverId());
                    ps.setString(3, row.from());
                    ps.setString(4, row.to());
                    ps.setTimestamp(5, Timestamp.from(row.departureTime()));
                    ps.setInt(6, row.totalSeats());
                    ps.setInt(7, row.totalSeats()); // available_seats = total_seats on initial seed
                    ps.setTimestamp(8, Timestamp.from(now));
                    ps.setTimestamp(9, Timestamp.from(now));
                });
    }

    public void seedBookings(List<BookingRow> rows) {
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO booking_requests (id, ride_id, passenger_id, status, requested_at, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, ?)
                """,
                rows, rows.size(),
                (ps, row) -> {
                    ps.setObject(1, row.id());
                    ps.setObject(2, row.rideId());
                    ps.setObject(3, row.passengerId());
                    ps.setString(4, row.status());
                    ps.setTimestamp(5, Timestamp.from(now));
                    ps.setTimestamp(6, Timestamp.from(now));
                    ps.setTimestamp(7, Timestamp.from(now));
                });
    }

    public record RoleRow(UUID id, String name) {}

    public record UserRow(UUID id, String email, String password,
                          String displayName, String phoneDialCode, String phoneNumber) {}

    public record UserRoleRow(UUID userId, UUID roleId) {}

    public record RideRow(UUID id, UUID driverId, String from, String to,
                          Instant departureTime, int totalSeats) {}

    public record BookingRow(UUID id, UUID rideId, UUID passengerId, String status) {}


}
