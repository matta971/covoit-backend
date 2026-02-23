package com.nc.sinpase.poc.modulith.covoit.identity;

import java.util.Set;
import java.util.UUID;

/**
 * Projection de l'identité utilisateur pour la vérification des credentials.
 * Réservée au module auth — ne pas exposer le passwordHash à d'autres modules.
 */
public record UserCredentials(
        UUID userId,
        String passwordHash,
        Set<String> roles
) {}
