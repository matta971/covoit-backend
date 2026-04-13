package com.nc.sinpase.poc.modulith.covoit.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.SecretKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class RideSteps {

    @Autowired private SharedContext ctx;
    @Autowired private TestHelper    helper;
    @Autowired private JdbcTemplate  jdbc;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Instant resolveDate(String label) {
        return switch (label.trim()) {
            case "demain"       -> Instant.now().plus(1,  ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
            case "après-demain" -> Instant.now().plus(2,  ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
            case "hier"         -> Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
            default             -> Instant.now().plus(1,  ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        };
    }

    private Instant parseDepartureTime(String text) {
        if (text.contains("à")) {
            String[] parts = text.split("à");
            String datePart = parts[0].trim();
            String timePart = parts[1].trim(); // e.g. "09:00"
            String[] hm = timePart.split(":");
            LocalDate date = switch (datePart) {
                case "demain"       -> LocalDate.now().plusDays(1);
                case "après-demain" -> LocalDate.now().plusDays(2);
                default             -> LocalDate.now().plusDays(1);
            };
            return date.atTime(LocalTime.of(Integer.parseInt(hm[0]), Integer.parseInt(hm[1])))
                       .toInstant(ZoneOffset.UTC);
        }
        return resolveDate(text);
    }

    private Response publishRide(String from, String to, Instant departure, int seats, String token) {
        Map<String, Object> body = Map.of(
            "from",          from,
            "to",            to,
            "departureTime", departure.toString(),
            "totalSeats",    seats
        );
        RequestSpecification req = given().contentType(ContentType.JSON).body(body);
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        return req.post("/api/rides");
    }

    private void loginDriver() {
        if (ctx.driverToken == null) helper.setupDriver(ctx);
    }

    private void loginPassenger() {
        if (ctx.passengerToken == null) helper.setupPassenger(ctx);
    }

    // ─── Given ────────────────────────────────────────────────────────────────

    @Given("le conducteur est authentifié")
    public void leConducteurEstAuthentifie() {
        loginDriver();
    }

    @Given("le conducteur est authentifié et n'a publié aucun trajet")
    public void leConducteurEstAuthentifieEtNAPasPublie() {
        loginDriver();
    }

    @Given("le passager est authentifié")
    public void lePassagerEstAuthentifie() {
        loginPassenger();
    }

    @Given("un trajet {string} → {string} publié pour demain")
    public void unTrajetPubliePourDemain(String from, String to) {
        loginDriver();
        Instant departure = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Response resp = publishRide(from, to, departure, 3, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("un trajet {string} → {string} publié pour après-demain")
    public void unTrajetPubliePourApressDemain(String from, String to) {
        loginDriver();
        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide(from, to, departure, 3, ctx.driverToken);
        ctx.currentRideId2 = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("un trajet {string} → {string} publié pour dans 7 jours")
    public void unTrajetPubliePourDans7Jours(String from, String to) {
        loginDriver();
        Instant departure = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        publishRide(from, to, departure, 3, ctx.driverToken);
    }

    @Given("un trajet {string} → {string} publié avec {int} sièges pour demain")
    public void unTrajetPublieAvecSiegesPourDemain(String from, String to, int seats) {
        loginDriver();
        Instant departure = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide(from, to, departure, seats, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("plusieurs trajets publiés")
    public void plusieursTrajetsPublies() {
        loginDriver();
        Instant departure = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        publishRide("Paris",    "Lyon",   departure, 3, ctx.driverToken);
        publishRide("Bordeaux", "Nantes", departure.plus(1, ChronoUnit.DAYS), 2, ctx.driverToken);
    }

    @Given("il a publié un trajet {string} → {string} pour demain")
    public void ilAPublieUnTrajet(String from, String to) {
        loginDriver();
        Instant departure = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide(from, to, departure, 3, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("il a publié {int} trajets")
    public void ilAPublieNTrajets(int count) {
        loginDriver();
        for (int i = 0; i < count; i++) {
            Instant dep = Instant.now().plus(i + 1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
            publishRide("Paris", "Lyon", dep, 3, ctx.driverToken);
        }
    }

    @Given("le conducteur a déjà annulé son trajet")
    public void leConducteurADejaAnnuleSonTrajet() {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide("Paris", "Lyon", dep, 3, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
        given().header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @Given("un trajet publié par le conducteur")
    public void unTrajetPublieParLeConducteur() {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide("Paris", "Lyon", dep, 3, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("un trajet existant")
    public void unTrajetExistant() {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide("Paris", "Lyon", dep, 3, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("le conducteur A a publié un trajet")
    public void leConducteurAAPublieUnTrajet() {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide("Paris", "Lyon", dep, 3, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("le conducteur B est authentifié")
    public void leConducteurBEstAuthentifie() {
        helper.setupDriverB(ctx);
    }

    @Given("un passager a une réservation acceptée sur ce trajet")
    public void unPassagerAUneReservationAcceptee() {
        loginPassenger();
        var bookResp = given().header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/rides/" + ctx.currentRideId + "/booking-requests");
        UUID bookingId = UUID.fromString(bookResp.jsonPath().getString("bookingRequestId"));
        ctx.currentBookingId = bookingId;
        given().header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/driver/me/booking-requests/" + bookingId + "/accept");
    }

    @Given("le conducteur a publié un trajet avec {int} sièges")
    public void leConducteurAPublieUnTrajetAvecSieges(int seats) {
        loginDriver();
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        var resp = publishRide("Paris", "Lyon", dep, seats, ctx.driverToken);
        ctx.currentRideId = UUID.fromString(resp.jsonPath().getString("rideId"));
    }

    @Given("un token JWT expiré")
    public void unTokenJwtExpire() {
        loginDriver();
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        String expired = Jwts.builder()
            .subject(ctx.driverId.toString())
            .expiration(new Date(System.currentTimeMillis() - 60_000L))
            .signWith(key)
            .compact();
        ctx.driverToken    = expired;
        ctx.passengerToken = expired;
    }

    // ─── When ─────────────────────────────────────────────────────────────────

    @When("il publie un trajet avec les données suivantes :")
    public void ilPublieUnTrajetAvecLesDonnees(DataTable table) {
        var row = table.asMaps().get(0);
        String from   = row.get("from");
        String to     = row.get("to");
        int    seats  = Integer.parseInt(row.get("totalSeats"));
        Instant departure = parseDepartureTime(row.get("departureTime"));
        ctx.lastResponse = publishRide(from, to, departure, seats, ctx.driverToken);
        if (ctx.lastResponse.getStatusCode() == 201) {
            ctx.currentRideId = UUID.fromString(ctx.lastResponse.jsonPath().getString("rideId"));
        }
    }

    @When("il publie un trajet vers {string} avec départ demain")
    public void ilPublieUnTrajetVersSansAuth(String to) {
        Instant dep = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ctx.lastResponse = publishRide("Paris", to, dep, 3, null);
    }

    @When("il publie un trajet avec from={string} to={string} departureTime={word} totalSeats={int}")
    public void ilPublieUnTrajetAvecParametres(String from, String to, String depLabel, int seats) {
        Instant dep = resolveDate(depLabel);
        ctx.lastResponse = publishRide(from, to, dep, seats, ctx.driverToken);
    }

    @When("il envoie POST \\/api\\/rides sans body")
    public void ilEnvoiePostRidesSansBody() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides");
    }

    @When("le passager est authentifié et cherche GET \\/api\\/rides")
    public void lePassagerChercheRides() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/rides");
    }

    @When("le passager cherche les trajets avec from={string}")
    public void lePassagerChercheAvecFrom(String from) {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .queryParam("from", from)
            .get("/api/rides");
    }

    @When("le passager cherche les trajets avec to={string}")
    public void lePassagerChercheAvecTo(String to) {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .queryParam("to", to)
            .get("/api/rides");
    }

    @When("le passager cherche les trajets avec date=demain")
    public void lePassagerChercheAvecDate() {
        loginPassenger();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .queryParam("date", tomorrow)
            .get("/api/rides");
    }

    @When("le passager cherche avec from={string} to={string} date=demain")
    public void lePassagerChercheAvecFromToDate(String from, String to) {
        loginPassenger();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .queryParam("from", from)
            .queryParam("to",   to)
            .queryParam("date", tomorrow)
            .get("/api/rides");
    }

    @When("GET \\/api\\/rides est appelé")
    public void getRidesSansAuth() {
        ctx.lastResponse = given().get("/api/rides");
    }

    @When("le passager consulte ce trajet par son rideId")
    public void lePassagerConsulteLeTrajet() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/rides/" + ctx.currentRideId);
    }

    @When("le passager consulte GET \\/api\\/rides\\/00000000-0000-0000-0000-000000000000")
    public void lePassagerConsulteRideInexistant() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/rides/00000000-0000-0000-0000-000000000000");
    }

    @When("le passager consulte GET \\/api\\/rides\\/not-a-uuid")
    public void lePassagerConsulteRideMauvaisUUID() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/rides/not-a-uuid");
    }

    @When("GET \\/api\\/rides\\/\\{rideId\\} est appelé sans token")
    public void getRideParIdSansToken() {
        ctx.lastResponse = given().get("/api/rides/" + ctx.currentRideId);
    }

    @When("le conducteur annule le trajet")
    public void leConducteurAnnuleLeTrajet() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @When("il annule ce trajet")
    public void ilAnnuleCeTrajet() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @When("le passager tente d'annuler ce trajet")
    public void lePassagerTenteDannulerCeTrajet() {
        loginPassenger();
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @When("il tente d'annuler le même trajet à nouveau")
    public void ilTenteDannulerLeMemeTrajetANouveau() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @When("il tente d'annuler GET \\/api\\/rides\\/00000000-0000-0000-0000-000000000000\\/cancel")
    public void ilTenteDannulerRideInexistant() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .post("/api/rides/00000000-0000-0000-0000-000000000000/cancel");
    }

    @When("POST \\/api\\/rides\\/\\{rideId\\}\\/cancel est appelé sans token")
    public void cancelRideSansToken() {
        ctx.lastResponse = given().post("/api/rides/" + ctx.currentRideId + "/cancel");
    }

    @When("il appelle GET \\/api\\/driver\\/me\\/rides")
    public void ilAppelleGetMyRides() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverToken)
            .get("/api/driver/me/rides");
    }

    @When("GET \\/api\\/driver\\/me\\/rides est appelé")
    public void getMyRidesSansAuth() {
        ctx.lastResponse = given().get("/api/driver/me/rides");
    }

    @When("le conducteur B appelle GET \\/api\\/driver\\/me\\/rides")
    public void leConducteurBAppelleGetMyRides() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.driverBToken)
            .get("/api/driver/me/rides");
    }

    @When("il appelle GET \\/api\\/me")
    public void ilAppelleGetMe() {
        ctx.lastResponse = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/me");
    }

    @When("GET \\/api\\/me est appelé")
    public void getMeSansAuth() {
        ctx.lastResponse = given().get("/api/me");
    }

    @When("il appelle successivement GET \\/api\\/rides, POST \\/api\\/rides, GET \\/api\\/me")
    public void ilAppelleSuccessivementPlusieursEndpoints() {
        String token = ctx.driverToken; // expired token
        ctx.allResponses.clear();
        ctx.allResponses.add(given().header("Authorization", "Bearer " + token).get("/api/rides"));
        ctx.allResponses.add(given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("from", "Paris", "to", "Lyon",
                         "departureTime", Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                         "totalSeats", 3))
            .post("/api/rides"));
        ctx.allResponses.add(given().header("Authorization", "Bearer " + token).get("/api/me"));
        ctx.lastResponse = ctx.allResponses.get(0);
    }

    // ─── Then ─────────────────────────────────────────────────────────────────

    @Then("la réponse contient un rideId non nul")
    public void laReponseContientUnRideIdNonNul() {
        assertThat(ctx.lastResponse.jsonPath().getString("rideId")).isNotBlank();
    }

    @Then("le trajet a le statut {string}")
    public void leTrajetALeStatut(String status) {
        assertThat(ctx.lastResponse.jsonPath().getString("status")).isEqualTo(status);
    }

    @Then("le statut du trajet est {string}")
    public void leStatutDuTrajetEst(String status) {
        ctx.lastResponse.then().body("status", equalTo(status));
    }

    @Then("les sièges disponibles sont {int}")
    public void lesSiegesDisponiblesSont(int seats) {
        assertThat(ctx.lastResponse.jsonPath().getInt("availableSeats")).isEqualTo(seats);
    }

    @Then("le driverId correspond à l'id du conducteur")
    public void leDriverIdCorrespondAuConducteur() {
        assertThat(ctx.lastResponse.jsonPath().getString("driverId"))
            .isEqualTo(ctx.driverId.toString());
    }

    @Then("la réponse contient une liste d'au moins {int} trajets")
    public void laReponseContientUneListeDAuMoinsNTrajets(int min) {
        List<?> rides = ctx.lastResponse.jsonPath().getList("$");
        assertThat(rides.size()).isGreaterThanOrEqualTo(min);
    }

    @Then("tous les trajets retournés ont from={string}")
    public void tousLesTrajetsOntFrom(String from) {
        List<String> froms = ctx.lastResponse.jsonPath().getList("from");
        assertThat(froms).allMatch(f -> f.equalsIgnoreCase(from));
    }

    @Then("le trajet {string} → {string} n'est pas dans les résultats")
    public void leTrajetNEstPasDansLesResultats(String from, String to) {
        List<Map<String, Object>> rides = ctx.lastResponse.jsonPath().getList("$");
        assertThat(rides).noneMatch(r ->
            from.equals(r.get("from")) && to.equals(r.get("to")));
    }

    @Then("tous les trajets retournés ont to={string}")
    public void tousLesTrajetsOntTo(String to) {
        List<String> tos = ctx.lastResponse.jsonPath().getList("to");
        assertThat(tos).allMatch(t -> t.equalsIgnoreCase(to));
    }

    @Then("la liste contient uniquement les trajets du jour demain")
    public void laListeContientUniquementLesTrajetsDeMainDate() {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        List<String> departures = ctx.lastResponse.jsonPath().getList("departureTime");
        assertThat(departures).allMatch(d -> d.startsWith(tomorrow));
    }

    @Then("tous les résultats correspondent aux trois critères")
    public void tousLesResultatsCorrespondentAuxTroisCriteres() {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        List<Map<String, Object>> rides = ctx.lastResponse.jsonPath().getList("$");
        assertThat(rides).allMatch(r ->
            "Paris".equals(r.get("from"))
            && "Lyon".equals(r.get("to"))
            && ((String) r.get("departureTime")).startsWith(tomorrow)
        );
    }

    @Then("la réponse contient rideId, driverId, from={string}, to={string}, totalSeats={int}")
    public void laReponseContientRideDetails(String from, String to, int seats) {
        ctx.lastResponse.then()
            .body("rideId",     notNullValue())
            .body("driverId",   notNullValue())
            .body("from",       equalTo(from))
            .body("to",         equalTo(to))
            .body("totalSeats", equalTo(seats));
    }

    @Then("en consultant le trajet, son statut est {string}")
    public void enConsultantLeTrajetSonStatutEst(String expected) {
        loginPassenger();
        var resp = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/rides/" + ctx.currentRideId);
        assertThat(resp.jsonPath().getString("status")).isEqualTo(expected);
    }

    @Then("le siège libéré est reflété dans les sièges disponibles")
    public void leSiegeLiberEEstReflete() {
        var resp = given()
            .header("Authorization", "Bearer " + ctx.passengerToken)
            .get("/api/rides/" + ctx.currentRideId);
        assertThat(resp.jsonPath().getInt("availableSeats")).isPositive();
    }

    @Then("la liste contient exactement {int} trajets")
    public void laListeContientExactementNTrajets(int count) {
        List<?> list = ctx.lastResponse.jsonPath().getList("$");
        assertThat(list).hasSize(count);
    }

    @Then("tous les trajets ont driverId correspondant au conducteur")
    public void tousLesTrajetsOntDriverIdCorrespondant() {
        List<String> driverIds = ctx.lastResponse.jsonPath().getList("driverId");
        assertThat(driverIds).allMatch(id -> id.equals(ctx.driverId.toString()));
    }

    @Then("le trajet du conducteur A n'est pas dans la liste")
    public void leTrajetDuConducteurANEstPasDansLaListe() {
        List<Map<String, Object>> rides = ctx.lastResponse.jsonPath().getList("$");
        assertThat(rides).noneMatch(r -> ctx.currentRideId.toString().equals(r.get("rideId")));
    }

    @Then("la réponse contient id, email={string}, displayName, roles")
    public void laReponseContientProfilUtilisateur(String email) {
        ctx.lastResponse.then()
            .body("id",          notNullValue())
            .body("email",       equalTo(email))
            .body("displayName", notNullValue())
            .body("roles",       notNullValue());
    }
}
