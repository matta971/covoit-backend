package com.nc.sinpase.poc.modulith.covoit.auth.adapters.in.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, message = "Password must be at least 12 characters") String password,
        @NotBlank String displayName,
        @NotBlank String phoneDialCode,
        @NotBlank String phoneNumber
) {}
