package com.nc.sinpase.poc.modulith.covoit.auth.domain;

import io.jsonwebtoken.Claims;

import java.util.Optional;

/**
 * Port sortant — validation des tokens JWT.
 *
 * Sépare explicitement la validation (reste dans le monolithe)
 * de la génération (TokenIssuer, partira dans auth-service).
 *
 * ✅ Avantage Spring Modulith : ce contrat explicite délimite précisément
 * ce qui peut être déplacé vers un microservice et ce qui doit rester.
 * JwtAuthenticationFilter et SecurityConfig dépendent de cette interface
 * — pas de la classe concrète — ils n'auront AUCUNE modification lors
 * de la suppression de TokenIssuer en Phase 2.
 *
 * ⚠️ Défi HMAC-SHA256 : la même clé secrète sert à signer ET à valider.
 * En Phase 2, auth-service et le monolithe partagent app.jwt.secret
 * en configuration — couplage implicite par configuration.
 * En Phase 3 (optionnel) : migrer vers RS256 pour une vraie séparation.
 *   - auth-service détient la clé privée (signe les JWT)
 *   - Le monolithe n'a que la clé publique via JWKS endpoint
 *   - Plus aucun secret partagé entre les deux services
 */
public interface TokenValidator {
    Optional<Claims> validate(String token);
}
