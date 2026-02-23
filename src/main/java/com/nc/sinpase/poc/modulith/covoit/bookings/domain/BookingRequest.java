package com.nc.sinpase.poc.modulith.covoit.bookings.domain;

import com.nc.sinpase.poc.modulith.covoit.ForbiddenException;
import com.nc.sinpase.poc.modulith.covoit.bookings.InvalidStatusTransitionException;

import java.time.Instant;
import java.util.UUID;

public class BookingRequest {

    private final UUID id;
    private final UUID rideId;
    private final UUID passengerId;
    private BookingStatus status;
    private final Instant requestedAt;
    private Instant decidedAt;
    private Instant canceledAt;
    private final Integer version;
    private final Instant createdAt;
    private Instant updatedAt;

    private BookingRequest(UUID id, UUID rideId, UUID passengerId, BookingStatus status,
                           Instant requestedAt, Instant decidedAt, Instant canceledAt,
                           Integer version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
        this.canceledAt = canceledAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BookingRequest request(UUID rideId, UUID passengerId) {
        Instant now = Instant.now();
        return new BookingRequest(UUID.randomUUID(), rideId, passengerId, BookingStatus.REQUESTED,
                now, null, null, null, now, now);
    }

    public static BookingRequest reconstitute(UUID id, UUID rideId, UUID passengerId, BookingStatus status,
                                              Instant requestedAt, Instant decidedAt, Instant canceledAt,
                                              Integer version, Instant createdAt, Instant updatedAt) {
        return new BookingRequest(id, rideId, passengerId, status, requestedAt, decidedAt,
                canceledAt, version, createdAt, updatedAt);
    }

    public void accept() {
        if (status != BookingStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(status.name(), "ACCEPTED");
        }
        status = BookingStatus.ACCEPTED;
        decidedAt = Instant.now();
        updatedAt = Instant.now();
    }

    public void reject() {
        if (status != BookingStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(status.name(), "REJECTED");
        }
        status = BookingStatus.REJECTED;
        decidedAt = Instant.now();
        updatedAt = Instant.now();
    }

    public void cancel(UUID byUserId) {
        if (status == BookingStatus.REJECTED || status == BookingStatus.CANCELED) {
            throw new InvalidStatusTransitionException(status.name(), "CANCELED");
        }
        if (!passengerId.equals(byUserId)) {
            throw new ForbiddenException("Only the passenger can cancel their booking request");
        }
        status = BookingStatus.CANCELED;
        canceledAt = Instant.now();
        updatedAt = Instant.now();
    }

    public boolean wasAccepted() {
        return status == BookingStatus.CANCELED && decidedAt != null;
    }

    public UUID getId() { return id; }
    public UUID getRideId() { return rideId; }
    public UUID getPassengerId() { return passengerId; }
    public BookingStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
