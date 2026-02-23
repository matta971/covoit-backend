package com.nc.sinpase.poc.modulith.covoit.auth;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Invalid or expired token");
    }
}
