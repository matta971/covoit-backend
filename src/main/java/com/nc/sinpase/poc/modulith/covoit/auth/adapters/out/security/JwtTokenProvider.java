package com.nc.sinpase.poc.modulith.covoit.auth.adapters.out.security;

import com.nc.sinpase.poc.modulith.covoit.auth.domain.TokenIssuer;
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

@Component
class JwtTokenProvider implements TokenIssuer {

    private final JwtProperties props;

    JwtTokenProvider(JwtProperties props) {
        this.props = props;
    }

    @Override
    public String generateAccessToken(UUID userId, Collection<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
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
