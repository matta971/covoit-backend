package com.nc.sinpase.poc.modulith.covoit.auth;

public record RegisterCommand(
        String email,
        String password,
        String displayName,
        String phoneDialCode,
        String phoneNumber
) {}
