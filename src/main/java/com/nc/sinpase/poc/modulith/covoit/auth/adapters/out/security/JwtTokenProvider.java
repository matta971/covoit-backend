package com.nc.sinpase.poc.modulith.covoit.auth.adapters.out.security;

import com.nc.sinpase.poc.modulith.covoit.auth.domain.TokenIssuer;
import com.nc.sinpase.poc.modulith.covoit.auth.domain.TokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * ÉTAT ACTUEL (Phase 1 — monolithe) : implémente TokenIssuer ET TokenValidator.
 *
 * FUTUR Phase 2 — après extraction de auth-service :
 *   - TokenIssuer (generateAccessToken, getXxxExpirationSeconds) :
 *     SUPPRIMÉ de ce fichier, déplacé dans auth-service
 *   - TokenValidator (validate) :
 *     RESTE ici, utilisé par JwtAuthenticationFilter
 *
 * ✅ Avantage : JwtAuthenticationFilter et SecurityConfig injectent
 * TokenValidator (interface) — ils n'ont AUCUNE modification à faire
 * quand TokenIssuer disparaît de cette classe en Phase 2.
 *
 * 🔄 Change par rapport au monolithe : SecurityConfig injecte maintenant
 * TokenValidator au lieu de JwtTokenProvider directement.
 */
@Component
class JwtTokenProvider implements TokenIssuer, TokenValidator {

    private final JwtProperties props;

    JwtTokenProvider(JwtProperties props) {
        this.props = props;
    }

    // ── TokenIssuer ── restera dans auth-service en Phase 2 ──────────────────

    @Override
    public String generateAccessToken(UUID userId, Collection<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.getAccessTokenExpirationSeconds() * 1000))
                .issuer(props.getIssuer())
                .audience().add(props.getAudience()).and()
                .signWith(secretKey())
                .compact();
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return props.getAccessTokenExpirationSeconds();
    }

    @Override
    public long getRefreshTokenExpirationSeconds() {
        return props.getRefreshTokenExpirationSeconds();
    }

    // ── TokenValidator ── restera dans le monolithe en Phase 2 ───────────────

    @Override
    public Optional<Claims> validate(String token) {
        return parseToken(token);
    }

    // Méthode package-private conservée pour compatibilité interne
    Optional<Claims> parseToken(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }
}
