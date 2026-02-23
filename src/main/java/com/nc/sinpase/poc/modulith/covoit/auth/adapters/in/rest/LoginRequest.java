package com.nc.sinpase.poc.modulith.covoit.auth.adapters.in.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
