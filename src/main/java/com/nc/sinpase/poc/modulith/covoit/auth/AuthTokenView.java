package com.nc.sinpase.poc.modulith.covoit.auth;

import java.util.UUID;

public record AuthTokenView(
        UUID userId,
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
