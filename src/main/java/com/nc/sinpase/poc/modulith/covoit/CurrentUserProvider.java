package com.nc.sinpase.poc.modulith.covoit;

import java.util.Set;
import java.util.UUID;

/**
 * Port partagé — identité de l'utilisateur courant (SecurityContext).
 * Placé dans le package racine pour être accessible à tous les modules
 * sans déclarer de dépendance vers le module auth.
 * Implémenté par auth/adapters/out/security/CurrentUserProviderImpl.
 */
public interface CurrentUserProvider {

    UUID getUserId();

    Set<String> getRoles();
}
