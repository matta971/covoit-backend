package com.nc.sinpase.poc.modulith.covoit.auth.adapters.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
class JwtProperties {

    private String secret;
    private long accessTokenExpirationSeconds = 900;
    private long refreshTokenExpirationSeconds = 2592000;
    private String issuer = "covoit-backend";
    private String audience = "covoit-app";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenExpirationSeconds() { return accessTokenExpirationSeconds; }
    public void setAccessTokenExpirationSeconds(long v) { this.accessTokenExpirationSeconds = v; }

    public long getRefreshTokenExpirationSeconds() { return refreshTokenExpirationSeconds; }
    public void setRefreshTokenExpirationSeconds(long v) { this.refreshTokenExpirationSeconds = v; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
}
