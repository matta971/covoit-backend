package com.nc.sinpase.poc.modulith.covoit.auth.adapters.in.rest;

import jakarta.validation.constraints.NotBlank;

record LogoutRequest(@NotBlank String refreshToken) {}
