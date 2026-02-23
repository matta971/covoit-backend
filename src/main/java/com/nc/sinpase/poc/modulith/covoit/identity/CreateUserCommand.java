package com.nc.sinpase.poc.modulith.covoit.identity;

public record CreateUserCommand(
        String email,
        String passwordHash,
        String displayName,
        String phoneDialCode,
        String phoneNumber
) {}
