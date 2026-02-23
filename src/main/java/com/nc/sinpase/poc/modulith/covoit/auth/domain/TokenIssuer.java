package com.nc.sinpase.poc.modulith.covoit.auth.domain;

import java.util.Collection;
import java.util.UUID;

/**
 * Port sortant — génération des tokens JWT.
 * Implémenté dans adapters/out/security.
 */
public interface TokenIssuer {

    String generateAccessToken(UUID userId, Collection<String> roles);

    long getAccessTokenExpirationSeconds();

    long getRefreshTokenExpirationSeconds();
}
