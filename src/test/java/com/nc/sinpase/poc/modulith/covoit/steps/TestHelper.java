package com.nc.sinpase.poc.modulith.covoit.steps;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Stateless HTTP helper used by all step-definition classes.
 */
@Component
public class TestHelper {

    public Response register(String email, String password, String displayName) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email",         email,
                "password",      password,
                "displayName",   displayName,
                "phoneDialCode", "+33",
                "phoneNumber",   "0600000001"
            ))
            .post("/api/auth/register");
    }

    public Response login(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", password))
            .post("/api/auth/login");
    }

    /**
     * Register + login in one shot; stores token + id into the given fields.
     *
     * @return the access token
     */
    public String registerAndGetToken(String email, String password, String displayName) {
        register(email, password, displayName);
        return login(email, password).jsonPath().getString("accessToken");
    }

    public UUID loginAndGetUserId(String email, String password) {
        return UUID.fromString(login(email, password).jsonPath().getString("userId"));
    }

    /** Fills driverToken + driverId into context. */
    public void setupDriver(SharedContext ctx) {
        register(ctx.DRIVER_EMAIL, ctx.DRIVER_PASSWORD, "Driver");
        Response resp = login(ctx.DRIVER_EMAIL, ctx.DRIVER_PASSWORD);
        ctx.driverToken = resp.jsonPath().getString("accessToken");
        ctx.driverId    = UUID.fromString(resp.jsonPath().getString("userId"));
    }

    /** Fills passengerToken + passengerId into context. */
    public void setupPassenger(SharedContext ctx) {
        register(ctx.PASSENGER_EMAIL, ctx.PASSENGER_PASSWORD, "Passenger");
        Response resp = login(ctx.PASSENGER_EMAIL, ctx.PASSENGER_PASSWORD);
        ctx.passengerToken = resp.jsonPath().getString("accessToken");
        ctx.passengerId    = UUID.fromString(resp.jsonPath().getString("userId"));
    }

    /** Fills passenger2Token + passenger2Id into context. */
    public void setupPassenger2(SharedContext ctx) {
        register(ctx.PASSENGER2_EMAIL, ctx.PASSENGER2_PASSWORD, "Passenger2");
        Response resp = login(ctx.PASSENGER2_EMAIL, ctx.PASSENGER2_PASSWORD);
        ctx.passenger2Token = resp.jsonPath().getString("accessToken");
        ctx.passenger2Id    = UUID.fromString(resp.jsonPath().getString("userId"));
    }

    /** Fills driverBToken + driverBId into context. */
    public void setupDriverB(SharedContext ctx) {
        register(ctx.DRIVER_B_EMAIL, ctx.DRIVER_B_PASSWORD, "DriverB");
        Response resp = login(ctx.DRIVER_B_EMAIL, ctx.DRIVER_B_PASSWORD);
        ctx.driverBToken = resp.jsonPath().getString("accessToken");
        ctx.driverBId    = UUID.fromString(resp.jsonPath().getString("userId"));
    }
}
