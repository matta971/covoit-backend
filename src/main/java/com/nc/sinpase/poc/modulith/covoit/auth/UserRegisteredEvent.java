package com.nc.sinpase.poc.modulith.covoit.auth;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * Publié après inscription réussie d'un utilisateur.
 *
 * ✅ Avantage : d'autres services pourront réagir à l'inscription
 * (ex: envoyer un email de bienvenue, initialiser un profil)
 * sans appeler auth directement — couplage zéro.
 *
 * ⚠️ Défi — inversion de responsabilité en Phase 2 :
 * Aujourd'hui c'est le MONOLITHE qui publie cet event.
 * Quand auth deviendra un microservice séparé, c'est
 * auth-SERVICE qui sera la source de vérité et publiera
 * cet event. Le monolithe devra alors RECEVOIR l'event
 * au lieu de le publier — c'est le principal refactoring
 * de Phase 2.
 *
 * 🔄 Change par rapport au monolithe : l'inscription génère
 * maintenant un event tracé dans event_publication JPA et
 * transmis sur la queue JMS "auth.UserRegisteredEvent".
 */
@Externalized("auth.UserRegisteredEvent")
public record UserRegisteredEvent(UUID userId, String email, String displayName) {}
