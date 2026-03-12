package com.nc.sinpase.poc.modulith.covoit.auth.adapters.in.rest;

import com.nc.sinpase.poc.modulith.covoit.ErrorResponse;
import com.nc.sinpase.poc.modulith.covoit.auth.InvalidCredentialsException;
import com.nc.sinpase.poc.modulith.covoit.auth.InvalidTokenException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return ErrorResponse.of("UNAUTHENTICATED", ex.getMessage(), traceId());
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidTokenException ex) {
        return ErrorResponse.of("UNAUTHENTICATED", ex.getMessage(), traceId());
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "unknown";
    }
}
