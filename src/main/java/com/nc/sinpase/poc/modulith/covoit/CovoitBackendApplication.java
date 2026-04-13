package com.nc.sinpase.poc.modulith.covoit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CovoitBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CovoitBackendApplication.class, args);
    }

}
