package com.nc.sinpase.poc.modulith.covoit.auth.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataRefreshSessionRepository extends JpaRepository<RefreshSessionJpaEntity, UUID> {

    Optional<RefreshSessionJpaEntity> findByRefreshTokenHash(String tokenHash);

    @Query("SELECT s FROM RefreshSessionJpaEntity s WHERE s.userId = :userId AND s.revokedAt IS NULL AND s.expiresAt > CURRENT_TIMESTAMP")
    List<RefreshSessionJpaEntity> findActiveByUserId(@Param("userId") UUID userId);
}
