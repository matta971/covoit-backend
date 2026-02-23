package com.nc.sinpase.poc.modulith.covoit.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {

    void save(RefreshSession session);

    void saveAll(List<RefreshSession> sessions);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    List<RefreshSession> findActiveByUserId(UUID userId);
}
