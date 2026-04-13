package com.nc.sinpase.poc.modulith.covoit.steps;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

public class Hooks {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    /** Configure RestAssured base URL once per scenario (idempotent). */
    @Before(order = 0)
    public void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /** Wipe all business data before each scenario. Roles are kept (seeded at startup). */
    @Before(order = 1)
    public void cleanDatabase() {
        jdbc.execute("""
            TRUNCATE TABLE
                event_publication,
                event_publication_archive,
                booking_requests,
                refresh_sessions,
                rides,
                user_roles,
                users
            CASCADE
            """);
    }
}
