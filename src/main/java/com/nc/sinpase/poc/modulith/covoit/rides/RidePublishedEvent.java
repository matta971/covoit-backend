package com.nc.sinpase.poc.modulith.covoit.rides;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

/**
 * ✅ Avantage Spring Modulith : @Externalized publie l'event vers Artemis
 * automatiquement APRÈS commit de la transaction (via event_publication JPA).
 * Garantie at-least-once delivery sans code infrastructure supplémentaire.
 *
 * ⚠️ Défi : le nom de queue est une string non typée. Renommer cet event
 * sans mettre à jour la cible @Externalized casse le contrat sans erreur
 * de compilation.
 *
 * 🔄 Change : l'event voyage maintenant AUSSI vers la queue JMS
 * "rides.RidePublishedEvent" en plus du dispatch in-process.
 * Les listeners @ApplicationModuleListener (notifications) restent
 * fonctionnels en parallèle — @Externalized n'est pas destructif.
 */
@Externalized("rides.RidePublishedEvent")
public record RidePublishedEvent(UUID rideId, UUID driverId, String from, String to, Instant departureTime) {}
