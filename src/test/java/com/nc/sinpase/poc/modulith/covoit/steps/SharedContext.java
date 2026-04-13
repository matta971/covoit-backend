package com.nc.sinpase.poc.modulith.covoit.steps;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scenario-scoped bean that holds all state shared between step definitions.
 * A fresh instance is created per Cucumber scenario.
 */
@Component
@ScenarioScope
public class SharedContext {

    // ─── Last HTTP response ───────────────────────────────────────────────────
    public Response lastResponse;

    // ─── DRIVER ──────────────────────────────────────────────────────────────
    public final String DRIVER_EMAIL    = "driver@covoit.com";
    public final String DRIVER_PASSWORD = "password-secret-123";
    public String driverToken;
    public UUID   driverId;

    // ─── DRIVER B (multi-driver scenarios) ───────────────────────────────────
    public final String DRIVER_B_EMAIL    = "driverb@covoit.com";
    public final String DRIVER_B_PASSWORD = "password-secret-123";
    public String driverBToken;
    public UUID   driverBId;

    // ─── PASSENGER ───────────────────────────────────────────────────────────
    public final String PASSENGER_EMAIL    = "passenger@covoit.com";
    public final String PASSENGER_PASSWORD = "password-secret-123";
    public String passengerToken;
    public UUID   passengerId;

    // ─── PASSENGER 2 ─────────────────────────────────────────────────────────
    public final String PASSENGER2_EMAIL    = "passenger2@covoit.com";
    public final String PASSENGER2_PASSWORD = "password-secret-123";
    public String passenger2Token;
    public UUID   passenger2Id;

    // ─── Working user (for auth-specific scenarios) ───────────────────────────
    public String workingEmail;
    public String workingPassword;
    public String workingAccessToken;
    public String workingRefreshToken;
    public UUID   workingUserId;
    public String savedAccessToken;   // kept to compare before/after refresh

    // ─── Current resources ────────────────────────────────────────────────────
    public UUID currentRideId;
    public UUID currentRideId2;
    public UUID currentBookingId;    // passenger's booking
    public UUID currentBookingId2;   // passenger2's booking

    // ─── Multi-step helpers ───────────────────────────────────────────────────
    /** Used by the logout-all scenario */
    public List<String> multipleRefreshTokens = new ArrayList<>();

    /** Used by the expired-token multi-call scenario */
    public List<Response> allResponses = new ArrayList<>();

    /** Used by the concurrent-booking scenario */
    public AtomicReference<Response> concurrentResponse1 = new AtomicReference<>();
    public AtomicReference<Response> concurrentResponse2 = new AtomicReference<>();
}
