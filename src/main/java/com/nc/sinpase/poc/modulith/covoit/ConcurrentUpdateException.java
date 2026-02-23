package com.nc.sinpase.poc.modulith.covoit;

public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException() {
        super("Concurrent update conflict — please retry");
    }
}
