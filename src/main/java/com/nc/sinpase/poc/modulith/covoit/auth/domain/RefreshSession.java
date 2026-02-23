package com.nc.sinpase.poc.modulith.covoit.auth.domain;

import java.time.Instant;
import java.util.UUID;

public class RefreshSession {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedBySessionId;
    private final String deviceId;
    private final String userAgent;
    private final String ip;

    private RefreshSession(UUID id, UUID userId, String tokenHash, Instant createdAt,
                           Instant expiresAt, Instant revokedAt, UUID replacedBySessionId,
                           String deviceId, String userAgent, String ip) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedBySessionId = replacedBySessionId;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.ip = ip;
    }

    public static RefreshSession create(UUID userId, String tokenHash, long expiresInSeconds,
                                        String deviceId, String userAgent, String ip) {
        Instant now = Instant.now();
        return new RefreshSession(UUID.randomUUID(), userId, tokenHash, now,
                now.plusSeconds(expiresInSeconds), null, null, deviceId, userAgent, ip);
    }

    public static RefreshSession reconstitute(UUID id, UUID userId, String tokenHash,
                                              Instant createdAt, Instant expiresAt,
                                              Instant revokedAt, UUID replacedBySessionId,
                                              String deviceId, String userAgent, String ip) {
        return new RefreshSession(id, userId, tokenHash, createdAt, expiresAt,
                revokedAt, replacedBySessionId, deviceId, userAgent, ip);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public void revokeAndReplace(UUID newSessionId) {
        this.revokedAt = Instant.now();
        this.replacedBySessionId = newSessionId;
    }

    public boolean isValid() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getReplacedBySessionId() { return replacedBySessionId; }
    public String getDeviceId() { return deviceId; }
    public String getUserAgent() { return userAgent; }
    public String getIp() { return ip; }
}
