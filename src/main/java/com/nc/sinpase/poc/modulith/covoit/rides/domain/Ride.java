package com.nc.sinpase.poc.modulith.covoit.rides.domain;

import com.nc.sinpase.poc.modulith.covoit.ForbiddenException;
import com.nc.sinpase.poc.modulith.covoit.rides.NoSeatsAvailableException;
import com.nc.sinpase.poc.modulith.covoit.rides.RideNotBookableException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Ride {

    private final UUID id;
    private final UUID driverId;
    private final String fromLocation;
    private final String toLocation;
    private final Instant departureTime;
    private final int totalSeats;
    private int availableSeats;
    private RideStatus status;
    private final Integer version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Ride(UUID id, UUID driverId, String fromLocation, String toLocation,
                 Instant departureTime, int totalSeats, int availableSeats,
                 RideStatus status, Integer version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.driverId = driverId;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.departureTime = departureTime;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Ride publish(UUID driverId, String from, String to, Instant departureTime, int totalSeats) {
        Objects.requireNonNull(driverId);
        if (from == null || from.isBlank()) throw new IllegalArgumentException("from must not be blank");
        if (to == null || to.isBlank()) throw new IllegalArgumentException("to must not be blank");
        Objects.requireNonNull(departureTime);
        if (totalSeats <= 0) throw new IllegalArgumentException("totalSeats must be > 0");
        if (!departureTime.isAfter(Instant.now())) throw new IllegalArgumentException("departureTime must be in the future");

        Instant now = Instant.now();
        return new Ride(UUID.randomUUID(), driverId, from, to, departureTime,
                totalSeats, totalSeats, RideStatus.SCHEDULED, null, now, now);
    }

    public static Ride reconstitute(UUID id, UUID driverId, String fromLocation, String toLocation,
                                    Instant departureTime, int totalSeats, int availableSeats,
                                    RideStatus status, Integer version, Instant createdAt, Instant updatedAt) {
        return new Ride(id, driverId, fromLocation, toLocation, departureTime,
                totalSeats, availableSeats, status, version, createdAt, updatedAt);
    }

    public void cancel(UUID byDriverId) {
        if (!driverId.equals(byDriverId)) {
            throw new ForbiddenException("Only the driver can cancel a ride");
        }
        if (status == RideStatus.CANCELED) {
            throw new RideNotBookableException("already canceled");
        }
        status = RideStatus.CANCELED;
        updatedAt = Instant.now();
    }

    public void reserveSeat() {
        assertBookable();
        if (availableSeats <= 0) {
            throw new NoSeatsAvailableException(id);
        }
        availableSeats--;
        updatedAt = Instant.now();
    }

    public void releaseSeat() {
        if (availableSeats < totalSeats) {
            availableSeats++;
            updatedAt = Instant.now();
        }
    }

    public void assertBookable() {
        if (status == RideStatus.CANCELED) {
            throw new RideNotBookableException("ride is canceled");
        }
        if (!departureTime.isAfter(Instant.now())) {
            throw new RideNotBookableException("departure time has passed");
        }
    }

    public UUID getId() { return id; }
    public UUID getDriverId() { return driverId; }
    public String getFromLocation() { return fromLocation; }
    public String getToLocation() { return toLocation; }
    public Instant getDepartureTime() { return departureTime; }
    public int getTotalSeats() { return totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public RideStatus getStatus() { return status; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
