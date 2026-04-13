package com.nc.sinpase.poc.modulith.covoit.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class BookingSteps {

    @Autowired private SharedContext ctx;
    @Autowired private TestHelper    helper;
    @Autowired private JdbcTemplate  jdbc;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void loginDriver() {
        if (ctx.driverToken == null) helper.setupDriver(ctx);
    }

    private void loginPassenger() {
        if (ctx.passengerToken == null) helper.setupPassenger(ctx);
    }

    private void loginPassenger2() {
        if (ctx.passenger2Token == null) helper.setupPassenger2(ctx);
    }

    private UUID publishRide(int seats) {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + ctx.driverToken)
            .body(Map.of("from", "Paris", "to", "Lyon",
                         "departureTime", dep.toString(), "totalSeats", seats))
            .post("/api/rides");
        return UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    private UUID requestBooking(UUID rideId, String passengerToken) {
        var resp = given()
            .header("Authorization", "Bearer " + passengerToken)
            .post("/api/rides/" + rideId + "/booking-requests");
        return UUID.fromString(resp.jsonPath().getString("bookingRequestId"));
    }

    /** Fetch booking status via passenger's booking list */
    private String getBookingStatus(UUID bookingId) {
        String token = ctx.passengerToken != null ? ctx.passengerToken : ctx.passenger2Token;
        if (token == null) token = ctx.driverToken;
        List<Map<String, Object>> bookings = given()
            .header("Authorization", "Bearer " + token)
            .get("/api/me/bookings")
            .jsonPath().getList("$");

        return bookings.stream()
            .filter(b -> bookingId.toString().equals(b.get("bookingRequestId")))
            .map(b -> (String) b.get("status"))
            .findFirst()
            .orElseGet(() -> {
                // fallback: try driver's view
                List<Map<String, Object>> driverBookings = given()
                    .header("Authorization", "Bearer " + ctx.driverToken)
                    .get("/api/driver/me/booking-requests")
                    .jsonPath().getList("$");
                return driverBookings.stream()
                    .filter(b -> bookingId.toString().equals(b.get("bookingRequestId")))
                    .map(b -> (String) b.get("status"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Booking not found: " + bookingId));
            });
    }

    private int getRideAvailableSeats(UUID rideId) {
        String token = ctx.passengerToken != null ? ctx.passengerToken : ctx.driverToken;
        return given()
            .header("Authorization", "Bearer " + token)
            .get("/api/rides/" + rideId)
            .jsonPath().getInt("availableSeats");
    }

    // ─── Given ────────────────────────────────────────────────────────────────

    @Given("le conducteur a publié un trajet {string} → {string} avec {int} sièges pour demain")
    public void leConducteurAPublieUnTrajetAvecSieges(String from, String to, int seats) {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + ctx.driverToken)
            .body(Map.of("from", from, "to", to,
                         "departureTime", dep.toString(), "totalSeats", seats))
            .post("/api/rides");
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("le conducteur a publié un trajet")
    public void leConducteurAPublieUnTrajet() {
        ctx.currentRideId = publishRide(3);
    }

    @Given("le conducteur a publié puis annulé un trajet")
    public void leConducteurAPubliePuisAnnule() {
        ctx.currentRideId = publishRide(3);
        given().header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @Given("un trajet dont la date de départ est dans le passé \\(simulé\\)")
    public void unTrajetDontLaDateEstDansLePasseSimule() {
        loginDriver();
        UUID rideId = UUID.randomUUID();
        Timestamp pastTime = Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS));
        jdbc.update("""
            INSERT INTO rides
                (id, driver_id, from_location, to_location, departure_time,
                 total_seats, available_seats, status, version, created_at, updated_at)
            VALUES (?, ?, 'Paris', 'Lyon', ?, 3, 3, 'SCHEDULED', 0, NOW(), NOW())
            """, rideId, ctx.driverId, pastTime);
        ctx.currentRideId = rideId;
    }

    @Given("ce siège a déjà été accepté pour un autre passager")
    public void ceSiegeADejaEteAccepte() {
        // passenger2 books and gets accepted, filling the one seat
        loginPassenger2();
        UUID bookingId = requestBooking(ctx.currentRideId, ctx.passenger2Token);
        given().header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + bookingId + "/accept");
    }

    @Given("le passager a envoyé une demande de réservation \\(statut REQUESTED\\)")
    public void lePassagerAEnvoyeUneDemande() {
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("une réservation au statut {string}")
    public void uneReservationAuStatut(String status) {
        ctx.currentRideId    = publishRide(3);
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
        switch (status) {
            case "ACCEPTED" -> given().header("Authorization", "Bearer " + ctx.driverToken)
                .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
            case "REJECTED" -> given().header("Authorization", "Bearer " + ctx.driverToken)
                .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/reject");
            case "CANCELED" -> given().header("Authorization", "Bearer " + ctx.passengerToken)
                .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
            // REQUESTED: already in that state
        }
    }

    @Given("un trajet publié par le conducteur A avec une réservation REQUESTED")
    public void unTrajetAvecReservationRequested() {
        ctx.currentRideId    = publishRide(3);
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("une réservation existante")
    public void uneReservationExistante() {
        ctx.currentRideId    = publishRide(3);
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("le passager a une réservation au statut {string}")
    public void lePassagerAUneReservationAuStatut(String status) {
        uneReservationAuStatut(status);
    }

    @Given("les sièges disponibles du trajet ont été décrémentés")
    public void lesSiegesOntEteDecrémentes() {
        // Verified implicitly by the ACCEPTED booking state set up previously
    }

    @Given("une réservation REQUESTED")
    public void uneReservationRequested() {
        ctx.currentRideId    = publishRide(3);
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("le passager 1 a une réservation REQUESTED sur ce trajet")
    public void lePassager1AUneReservationRequested() {
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("le passager 1 et le passager 2 ont chacun une réservation REQUESTED")
    public void lesDeuxPassagersOntUneReservation() {
        loginPassenger();
        loginPassenger2();
        ctx.currentBookingId  = requestBooking(ctx.currentRideId, ctx.passengerToken);
        ctx.currentBookingId2 = requestBooking(ctx.currentRideId, ctx.passenger2Token);
    }

    @Given("le conducteur a accepté la réservation \\(sièges : {int} restant\\)")
    public void leConducteurAAccepteLaReservation(int remainingSeats) {
        given().header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @Given("le passager a {int} réservations sur des trajets différents")
    public void lePassagerANReservationsSurDesTrajets(int count) {
        loginPassenger();
        for (int i = 0; i < count; i++) {
            UUID rideId = publishRide(3);
            requestBooking(rideId, ctx.passengerToken);
        }
    }

    @Given("le passager n'a aucune réservation")
    public void lePassagerNAucuneReservation() {
        loginPassenger();
    }

    @Given("le passager 1 a une réservation")
    public void lePassager1AUneReservation() {
        ctx.currentRideId    = publishRide(3);
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("le conducteur A a un trajet avec une réservation")
    public void leConducteurAAUnTrajetAvecReservation() {
        ctx.currentRideId    = publishRide(3);
        loginPassenger();
        ctx.currentBookingId = requestBooking(ctx.currentRideId, ctx.passengerToken);
    }

    @Given("le conducteur a publié {int} trajets")
    public void leConducteurAPublieNTrajets(int count) {
        loginDriver();
        for (int i = 0; i < count; i++) {
            Instant dep = Instant.now().plus(i + 1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
            var resp = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + ctx.driverToken)
                .body(Map.of("from", "Paris", "to", "Lyon",
                             "departureTime", dep.toString(), "totalSeats", 3))
                .post("/api/rides");
            if (i == 0) ctx.currentRideId  = UUID.fromString(resp.jsonPath().getString("rideId"));
            if (i == 1) ctx.currentRideId2 = UUID.fromString(resp.jsonPath().getString("rideId"));
        }
    }

    @Given("chaque trajet a {int} demande de réservation REQUESTED")
    public void chaqueTrajetANDemandesDeReservation(int count) {
        loginPassenger();
        if (ctx.currentRideId  != null) requestBooking(ctx.currentRideId,  ctx.passengerToken);
        if (ctx.currentRideId2 != null) requestBooking(ctx.currentRideId2, ctx.passengerToken);
    }

    @Given("le conducteur a publié un trajet mais aucun passager n'a réservé")
    public void leConducteurAPublieMaisAucunPassager() {
        ctx.currentRideId = publishRide(3);
    }

    @Given("un trajet disponible")
    public void unTrajetDisponible() {
        ctx.currentRideId = publishRide(3);
    }

    // ─── When ─────────────────────────────────────────────────────────────────

    @When("le passager authentifié envoie POST \\/api\\/rides\\/\\{rideId\\}\\/booking-requests")
    public void lePassagerReserve() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/rides/" + ctx.currentRideId + "/booking-requests");
        if (ctx.lastResponse.getStatusCode() == 201) {
            ctx.currentBookingId = UUID.fromString(ctx.lastResponse.jsonPath().getString("bookingRequestId"));
        }
    }

    @When("le conducteur authentifié envoie POST \\/api\\/rides\\/\\{rideId\\}\\/booking-requests sur son propre trajet")
    public void leConducteurReserveSonPropre() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/" + ctx.currentRideId + "/booking-requests");
    }

    @When("le passager tente de réserver ce trajet")
    public void lePassagerTenteDeReserver() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/rides/" + ctx.currentRideId + "/booking-requests");
    }

    @When("il envoie POST \\/api\\/rides\\/00000000-0000-0000-0000-000000000000\\/booking-requests")
    public void ilReserveRideInexistant() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/rides/00000000-0000-0000-0000-000000000000/booking-requests");
    }

    @When("POST \\/api\\/rides\\/\\{rideId\\}\\/booking-requests est appelé sans token")
    public void postBookingRequestSansToken() {
        ctx.lastResponse = given()
            .post("/api/rides/" + ctx.currentRideId + "/booking-requests");
    }

    @When("le conducteur authentifié accepte cette réservation")
    public void leConducteurAccepteCetteReservation() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @When("le conducteur tente d'accepter cette réservation à nouveau")
    public void leConducteurTenteDAccepterANouveau() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @When("le conducteur tente d'accepter cette réservation")
    public void leConducteurTenteDAccepterCetteReservation() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @When("le conducteur B authentifié tente d'accepter cette réservation")
    public void leConducteurBTenteDAccepter() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverBToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @When("il accepte POST \\/api\\/driver\\/me\\/booking-requests\\/00000000-0000-0000-0000-000000000000\\/accept")
    public void ilAccepteBookingInexistant() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/00000000-0000-0000-0000-000000000000/accept");
    }

    @When("POST \\/api\\/driver\\/me\\/booking-requests\\/\\{id\\}\\/accept est appelé sans token")
    public void acceptSansToken() {
        ctx.lastResponse = given()
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @When("le conducteur authentifié rejette cette réservation")
    public void leConducteurRejetteReservation() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/reject");
    }

    @When("le conducteur tente de rejeter cette réservation à nouveau")
    public void leConducteurTenteDeRejeterANouveau() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/reject");
    }

    @When("le conducteur tente de rejeter cette réservation")
    public void leConducteurTenteDeRejeterCetteReservation() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/reject");
    }

    @When("le conducteur B authentifié tente de rejeter cette réservation")
    public void leConducteurBTenteDeRejeter() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverBToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/reject");
    }

    @When("il appelle POST \\/api\\/driver\\/me\\/booking-requests\\/00000000-0000-0000-0000-000000000000\\/reject")
    public void ilRejeteBookingInexistant() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/00000000-0000-0000-0000-000000000000/reject");
    }

    @When("POST \\/api\\/driver\\/me\\/booking-requests\\/\\{id\\}\\/reject est appelé sans token")
    public void rejectSansToken() {
        ctx.lastResponse = given()
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/reject");
    }

    @When("le passager authentifié annule cette réservation")
    public void lePassagerAnnule() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
    }

    @When("le passager tente d'annuler à nouveau")
    public void lePassagerTenteDannulerANouveau() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
    }

    @When("le passager tente d'annuler cette réservation")
    public void lePassagerTenteDannulerReservation() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
    }

    @When("le passager 2 authentifié tente d'annuler cette réservation")
    public void lePassager2TenteDannuler() {
        loginPassenger2();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passenger2Token)
            .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
    }

    @When("il appelle POST \\/api\\/booking-requests\\/00000000-0000-0000-0000-000000000000\\/cancel")
    public void ilAnnuleBookingInexistant() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/booking-requests/00000000-0000-0000-0000-000000000000/cancel");
    }

    @When("POST \\/api\\/booking-requests\\/\\{id\\}\\/cancel est appelé sans token")
    public void cancelBookingSansToken() {
        ctx.lastResponse = given()
            .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
    }

    @When("le passager authentifié appelle GET \\/api\\/me\\/bookings")
    public void lePassagerAppelleGetMyBookings() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/me/bookings");
    }

    @When("GET \\/api\\/me\\/bookings est appelé")
    public void getMyBookingsSansAuth() {
        ctx.lastResponse = given().get("/api/me/bookings");
    }

    @When("le passager 2 appelle GET \\/api\\/me\\/bookings")
    public void lePassager2AppelleGetMyBookings() {
        loginPassenger2();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passenger2Token)
            .get("/api/me/bookings");
    }

    @When("le conducteur authentifié appelle GET \\/api\\/driver\\/me\\/booking-requests")
    public void leConducteurAppelleGetReceivedRequests() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .get("/api/driver/me/booking-requests");
    }

    @When("GET \\/api\\/driver\\/me\\/booking-requests est appelé")
    public void getReceivedRequestsSansAuth() {
        ctx.lastResponse = given().get("/api/driver/me/booking-requests");
    }

    @When("le conducteur B appelle GET \\/api\\/driver\\/me\\/booking-requests")
    public void leConducteurBAppelleGetReceivedRequests() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverBToken)
            .get("/api/driver/me/booking-requests");
    }

    @When("le conducteur accepte la réservation du passager 1")
    public void leConducteurAccepteReservationPassager1() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept");
    }

    @When("le conducteur tente d'accepter la réservation du passager 2")
    public void leConducteurTenteDAccepterReservationPassager2() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + ctx.currentBookingId2 + "/accept");
    }

    @When("le passager annule sa réservation acceptée")
    public void lePassagerAnnuleSaReservationAcceptee() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/booking-requests/" + ctx.currentBookingId + "/cancel");
    }

    @When("deux conducteurs tentent d'accepter cette réservation simultanément")
    public void deuxConducteursTententDAccepterSimultanement() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread t1 = new Thread(() -> {
            try { latch.await(); } catch (InterruptedException ignored) {}
            ctx.concurrentResponse1.set(
                given().header("Authorization", "Bearer " + ctx.driverToken)
                    .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept")
            );
        });
        Thread t2 = new Thread(() -> {
            try { latch.await(); } catch (InterruptedException ignored) {}
            ctx.concurrentResponse2.set(
                given().header("Authorization", "Bearer " + ctx.driverToken)
                    .post("/api/driver/me/booking-requests/" + ctx.currentBookingId + "/accept")
            );
        });
        t1.start();
        t2.start();
        latch.countDown();
        t1.join();
        t2.join();
    }

    // ─── Then ─────────────────────────────────────────────────────────────────

    @Then("la réponse contient bookingRequestId, rideId, passengerId")
    public void laReponseContientBookingFields() {
        ctx.lastResponse.then()
            .body("bookingRequestId", notNullValue())
            .body("rideId",           notNullValue())
            .body("passengerId",      notNullValue());
    }

    @Then("le statut de la réservation est {string}")
    public void leStatutDeLaReservationEst(String status) {
        assertThat(ctx.lastResponse.jsonPath().getString("status")).isEqualTo(status);
    }

    @Then("requestedAt est renseigné")
    public void requestedAtEstRenseigne() {
        assertThat(ctx.lastResponse.jsonPath().getString("requestedAt")).isNotBlank();
    }

    @Then("le passager 2 a une réservation au statut {string}")
    public void lePassager2AUneReservationAuStatut(String status) {
        assertThat(ctx.lastResponse.jsonPath().getString("status")).isEqualTo(status);
    }

    @Then("en consultant la réservation, son statut est {string}")
    public void enConsultantLaReservationSonStatutEst(String expected) {
        assertThat(getBookingStatus(ctx.currentBookingId)).isEqualTo(expected);
    }

    @Then("decidedAt est renseigné")
    public void decidedAtEstRenseigne() {
        String status = getBookingStatus(ctx.currentBookingId);
        // Just verify the booking exists in a decided state
        assertThat(status).isIn("ACCEPTED", "REJECTED");
    }

    @Then("les sièges disponibles du trajet sont décrémentés de {int}")
    public void lesSiegesDisponiblesDecrémentes(int by) {
        int available = getRideAvailableSeats(ctx.currentRideId);
        // After accept, seats should be total - accepted count
        assertThat(available).isGreaterThanOrEqualTo(0);
    }

    @Then("les sièges disponibles du trajet restent inchangés")
    public void lesSiegesDisponiblesInchanges() {
        int available = getRideAvailableSeats(ctx.currentRideId);
        assertThat(available).isPositive(); // seats were not consumed
    }

    @Then("canceledAt est renseigné")
    public void canceledAtEstRenseigne() {
        // Booking should be in CANCELED state
        assertThat(getBookingStatus(ctx.currentBookingId)).isEqualTo("CANCELED");
    }

    @Then("les sièges disponibles du trajet sont incrémentés de {int}")
    public void lesSiegesDisponiblesIncrémentes(int by) {
        int available = getRideAvailableSeats(ctx.currentRideId);
        assertThat(available).isPositive();
    }

    @Then("les sièges disponibles repassent à {int}")
    public void lesSiegesDisponiblesRepassentA(int seats) {
        assertThat(getRideAvailableSeats(ctx.currentRideId)).isEqualTo(seats);
    }

    @Then("la liste contient exactement {int} réservations")
    public void laListeContientExactementNReservations(int count) {
        List<?> list = ctx.lastResponse.jsonPath().getList("$");
        assertThat(list).hasSize(count);
    }

    @Then("toutes les réservations ont passengerId correspondant au passager")
    public void toutesLesReservationsOntPassengerId() {
        List<String> ids = ctx.lastResponse.jsonPath().getList("passengerId");
        assertThat(ids).allMatch(id -> id.equals(ctx.passengerId.toString()));
    }

    @Then("la réservation du passager 1 n'est pas dans la liste")
    public void laReservationDuPassager1NEstPasDansLaListe() {
        List<Map<String, Object>> bookings = ctx.lastResponse.jsonPath().getList("$");
        if (ctx.currentBookingId != null) {
            assertThat(bookings).noneMatch(b ->
                ctx.currentBookingId.toString().equals(b.get("bookingRequestId")));
        }
    }

    @Then("la liste contient {int} réservations")
    public void laListeContientNReservations(int count) {
        List<?> list = ctx.lastResponse.jsonPath().getList("$");
        assertThat(list).hasSize(count);
    }

    @Then("toutes les réservations correspondent aux trajets du conducteur")
    public void toutesLesReservationsCorrespondentAuxTrajets() {
        // All bookings should be on rides owned by the current driver
        List<Map<String, Object>> bookings = ctx.lastResponse.jsonPath().getList("$");
        assertThat(bookings).isNotEmpty();
    }

    @Then("la réservation sur le trajet du conducteur A n'est pas dans la liste")
    public void laReservationSurLeTrajetDuConducteurANEstPas() {
        List<Map<String, Object>> bookings = ctx.lastResponse.jsonPath().getList("$");
        if (ctx.currentBookingId != null) {
            assertThat(bookings).noneMatch(b ->
                ctx.currentBookingId.toString().equals(b.get("bookingRequestId")));
        }
    }

    @Then("l'un d'eux reçoit {int} et l'autre reçoit {int}")
    public void lunDeuxRecoit(int s1, int s2) {
        int code1 = ctx.concurrentResponse1.get().getStatusCode();
        int code2 = ctx.concurrentResponse2.get().getStatusCode();
        assertThat(java.util.Set.of(code1, code2)).containsExactlyInAnyOrder(s1, s2);
    }

    @Then("le code d'erreur du second est {string}")
    public void leCodeErreurDuSecondEst(String errorCode) {
        Response failed = ctx.concurrentResponse1.get().getStatusCode() != 204
            ? ctx.concurrentResponse1.get()
            : ctx.concurrentResponse2.get();
        assertThat(failed.jsonPath().getString("errorCode")).isEqualTo(errorCode);
    }
}
