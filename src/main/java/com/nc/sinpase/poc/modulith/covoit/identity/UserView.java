package com.nc.sinpase.poc.modulith.covoit.identity;

import java.util.Set;
import java.util.UUID;

public record UserView(
        UUID id,
        String email,
        String displayName,
        String phoneDialCode,
        String phoneNumber,
        String status,
        Set<String> roles
) {}
