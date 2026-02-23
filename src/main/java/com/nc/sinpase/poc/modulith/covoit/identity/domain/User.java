package com.nc.sinpase.poc.modulith.covoit.identity.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final PhoneNumber phoneNumber;
    private UserStatus status;
    private final Set<Role> roles = new HashSet<>();
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;

    private User(UUID id, String email, String passwordHash, String displayName,
                 PhoneNumber phoneNumber, UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Factory — création d'un nouvel utilisateur */
    public static User create(String email, String passwordHash, String displayName, PhoneNumber phoneNumber) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        if (email.isBlank()) throw new IllegalArgumentException("email must not be blank");
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");

        Instant now = Instant.now();
        return new User(UUID.randomUUID(), email, passwordHash, displayName, phoneNumber, UserStatus.ACTIVE, now, now);
    }

    /** Reconstitution depuis la persistence */
    public static User reconstitute(UUID id, String email, String passwordHash, String displayName,
                                    PhoneNumber phoneNumber, UserStatus status, Set<Role> roles,
                                    Instant createdAt, Instant updatedAt, Instant lastLoginAt) {
        User user = new User(id, email, passwordHash, displayName, phoneNumber, status, createdAt, updatedAt);
        if (roles != null) user.roles.addAll(roles);
        user.lastLoginAt = lastLoginAt;
        return user;
    }

    public void assignRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        roles.add(role);
        updatedAt = Instant.now();
    }

    public void recordLogin(Instant at) {
        this.lastLoginAt = at;
        this.updatedAt = at;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public PhoneNumber getPhoneNumber() { return phoneNumber; }
    public UserStatus getStatus() { return status; }
    public Set<Role> getRoles() { return Collections.unmodifiableSet(roles); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
