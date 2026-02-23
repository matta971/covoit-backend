package com.nc.sinpase.poc.modulith.covoit.auth;

public record LoginCommand(
        String email,
        String password
) {}
