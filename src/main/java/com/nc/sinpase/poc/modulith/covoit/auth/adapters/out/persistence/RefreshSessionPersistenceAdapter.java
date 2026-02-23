package com.nc.sinpase.poc.modulith.covoit.auth.adapters.out.persistence;

import com.nc.sinpase.poc.modulith.covoit.auth.domain.RefreshSession;
import com.nc.sinpase.poc.modulith.covoit.auth.domain.RefreshSessionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class RefreshSessionPersistenceAdapter implements RefreshSessionRepository {

    private final SpringDataRefreshSessionRepository repo;

    RefreshSessionPersistenceAdapter(SpringDataRefreshSessionRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(RefreshSession session) {
        RefreshSessionJpaEntity entity = repo.findById(session.getId()).orElseGet(RefreshSessionJpaEntity::new);
        entity.setId(session.getId());
        entity.setUserId(session.getUserId());
        entity.setRefreshTokenHash(session.getTokenHash());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setRevokedAt(session.getRevokedAt());
        entity.setReplacedBySessionId(session.getReplacedBySessionId());
        entity.setDeviceId(session.getDeviceId());
        entity.setUserAgent(session.getUserAgent());
        entity.setIp(session.getIp());
        repo.save(entity);
    }

    @Override
    public void saveAll(List<RefreshSession> sessions) {
        sessions.forEach(this::save);
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return repo.findByRefreshTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public List<RefreshSession> findActiveByUserId(UUID userId) {
        return repo.findActiveByUserId(userId).stream().map(this::toDomain).toList();
    }

    private RefreshSession toDomain(RefreshSessionJpaEntity e) {
        return RefreshSession.reconstitute(
                e.getId(), e.getUserId(), e.getRefreshTokenHash(),
                e.getCreatedAt(), e.getExpiresAt(), e.getRevokedAt(),
                e.getReplacedBySessionId(), e.getDeviceId(), e.getUserAgent(), e.getIp()
        );
    }
}
