package com.nc.sinpase.poc.modulith.covoit.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class AuthSteps {

    @Autowired private SharedContext ctx;
    @Autowired private TestHelper    helper;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Response register(String email, String password, String displayName,
                              String dialCode, String phoneNumber) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email",         email,
                "password",      password,
                "displayName",   displayName,
                "phoneDialCode", dialCode,
                "phoneNumber",   phoneNumber
            ))
            .post("/api/auth/register");
    }

    // ─── Given ────────────────────────────────────────────────────────────────

    @Given("un utilisateur avec l'email {string} existe déjà")
    public void unUtilisateurAvecEmailExisteDeja(String email) {
        register(email, "default-password-123", "User", "+33", "0600000001");
    }

    @Given("un utilisateur enregistré avec l'email {string} et le mot de passe {string}")
    public void unUtilisateurEnregistreAvecEmailEtPassword(String email, String password) {
        register(email, password, "Bob", "+33", "0600000002");
        ctx.workingEmail    = email;
        ctx.workingPassword = password;
    }

    @Given("un utilisateur enregistré avec l'email {string}")
    public void unUtilisateurEnregistreAvecEmail(String email) {
        String pwd = "default-password-123";
        register(email, pwd, "Bob", "+33", "0600000002");
        ctx.workingEmail    = email;
        ctx.workingPassword = pwd;
    }

    @Given("un utilisateur connecté avec un refreshToken valide")
    public void unUtilisateurConnecteAvecRefreshTokenValide() {
        register("testuser@covoit.com", "test-password-123", "Test", "+33", "0600000003");
        var resp = helper.login("testuser@covoit.com", "test-password-123");
        ctx.workingAccessToken  = resp.jsonPath().getString("accessToken");
        ctx.workingRefreshToken = resp.jsonPath().getString("refreshToken");
        ctx.workingUserId       = UUID.fromString(resp.jsonPath().getString("userId"));
    }

    @Given("un utilisateur s'est déconnecté \\(son refreshToken est révoqué\\)")
    public void unUtilisateurSestDeconnecte() {
        register("testuser@covoit.com", "test-password-123", "Test", "+33", "0600000003");
        var resp = helper.login("testuser@covoit.com", "test-password-123");
        String access  = resp.jsonPath().getString("accessToken");
        String refresh = resp.jsonPath().getString("refreshToken");
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + access)
            .body(Map.of("refreshToken", refresh))
            .post("/api/auth/logout");
        ctx.workingAccessToken  = access;
        ctx.workingRefreshToken = refresh;
    }

    @Given("un refreshToken déjà révoqué")
    public void unRefreshTokenDejaRevoqu() {
        unUtilisateurSestDeconnecte();
    }

    @Given("un utilisateur avec {int} sessions actives \\({int} refreshTokens\\)")
    public void unUtilisateurAvecNSessionsActives(int sessions, int tokenCount) {
        register("multi@covoit.com", "multi-password-123", "Multi", "+33", "0600000004");
        ctx.multipleRefreshTokens.clear();
        for (int i = 0; i < tokenCount; i++) {
            var r = helper.login("multi@covoit.com", "multi-password-123");
            ctx.multipleRefreshTokens.add(r.jsonPath().getString("refreshToken"));
            ctx.workingAccessToken = r.jsonPath().getString("accessToken");
        }
    }

    // ─── When ─────────────────────────────────────────────────────────────────

    @When("un utilisateur s'inscrit avec les données suivantes :")
    public void unUtilisateurSinscritAvecLesDonnees(DataTable table) {
        var row = table.asMaps().get(0);
        ctx.lastResponse = register(
            row.get("email"), row.get("password"), row.get("displayName"),
            row.get("phoneDialCode"), row.get("phoneNumber")
        );
    }

    @When("un second utilisateur s'inscrit avec l'email {string}")
    public void unSecondUtilisateurSinscritAvecEmail(String email) {
        ctx.lastResponse = register(email, "other-password-123", "Other", "+33", "0600000005");
    }

    @When("un utilisateur s'inscrit avec password={string}")
    public void unUtilisateurSinscritAvecPassword(String password) {
        ctx.lastResponse = register("test@covoit.com", password, "Test", "+33", "0600000001");
    }

    @When("un utilisateur s'inscrit sans champ email")
    public void unUtilisateurSinscritSansChampEmail() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("password", "some-password-123", "displayName", "Test",
                         "phoneDialCode", "+33", "phoneNumber", "0600000001"))
            .post("/api/auth/register");
    }

    @When("un utilisateur s'inscrit sans champ displayName")
    public void unUtilisateurSinscritSansDisplayName() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("email", "test2@covoit.com", "password", "some-password-123",
                         "phoneDialCode", "+33", "phoneNumber", "0600000001"))
            .post("/api/auth/register");
    }

    @When("un utilisateur s'inscrit sans phoneDialCode ni phoneNumber")
    public void unUtilisateurSinscritSansPhone() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("email", "test3@covoit.com", "password", "some-password-123",
                         "displayName", "Test"))
            .post("/api/auth/register");
    }

    @When("POST \\/api\\/auth\\/register est appelé sans body")
    public void postRegisterSansBody() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .post("/api/auth/register");
    }

    @When("il se connecte avec ces identifiants")
    public void ilSeConnecteAvecCesIdentifiants() {
        ctx.lastResponse = helper.login(ctx.workingEmail, ctx.workingPassword);
        if (ctx.lastResponse.getStatusCode() == 200) {
            ctx.workingUserId = UUID.fromString(ctx.lastResponse.jsonPath().getString("userId"));
        }
    }

    @When("il se connecte avec le mot de passe {string}")
    public void ilSeConnecteAvecLeMotDePasse(String password) {
        ctx.lastResponse = helper.login(ctx.workingEmail, password);
    }

    @When("un utilisateur tente de se connecter avec l'email {string}")
    public void unUtilisateurTenteDeSeConnecter(String email) {
        ctx.lastResponse = helper.login(email, "some-password-123");
    }

    @When("POST \\/api\\/auth\\/login est appelé sans champ email")
    public void postLoginSansEmail() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("password", "some-password-123"))
            .post("/api/auth/login");
    }

    @When("POST \\/api\\/auth\\/login est appelé sans champ password")
    public void postLoginSansPassword() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("email", "test@covoit.com"))
            .post("/api/auth/login");
    }

    @When("il appelle POST \\/api\\/auth\\/refresh avec ce refreshToken")
    public void ilAppelleRefresh() {
        ctx.savedAccessToken = ctx.workingAccessToken;
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", ctx.workingRefreshToken))
            .post("/api/auth/refresh");
        if (ctx.lastResponse.getStatusCode() == 200) {
            ctx.workingAccessToken  = ctx.lastResponse.jsonPath().getString("accessToken");
            ctx.workingRefreshToken = ctx.lastResponse.jsonPath().getString("refreshToken");
        }
    }

    @When("POST \\/api\\/auth\\/refresh est appelé avec refreshToken={string}")
    public void postRefreshAvecToken(String token) {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", token))
            .post("/api/auth/refresh");
    }

    @When("POST \\/api\\/auth\\/refresh est appelé sans body")
    public void postRefreshSansBody() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .post("/api/auth/refresh");
    }

    @When("il tente de rafraîchir avec ce refreshToken révoqué")
    public void ilTenteDeRafraichirAvecTokenRevoqu() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", ctx.workingRefreshToken))
            .post("/api/auth/refresh");
    }

    @When("il appelle POST \\/api\\/auth\\/logout avec ce refreshToken")
    public void ilAppelleLogout() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + ctx.workingAccessToken)
            .body(Map.of("refreshToken", ctx.workingRefreshToken))
            .post("/api/auth/logout");
    }

    @When("POST \\/api\\/auth\\/logout est appelé sans accessToken")
    public void postLogoutSansToken() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", "some-refresh-token"))
            .post("/api/auth/logout");
    }

    @When("POST \\/api\\/auth\\/logout est appelé avec ce refreshToken")
    public void postLogoutAvecTokenRevoqu() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + ctx.workingAccessToken)
            .body(Map.of("refreshToken", ctx.workingRefreshToken))
            .post("/api/auth/logout");
    }

    @When("il appelle POST \\/api\\/auth\\/logout-all")
    public void ilAppelleLogoutAll() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + ctx.workingAccessToken)
            .post("/api/auth/logout-all");
    }

    @When("POST \\/api\\/auth\\/logout-all est appelé sans accessToken")
    public void postLogoutAllSansToken() {
        ctx.lastResponse = given().contentType(ContentType.JSON)
            .post("/api/auth/logout-all");
    }

    // ─── Then ─────────────────────────────────────────────────────────────────

    @Then("la réponse contient un accessToken, un refreshToken et un userId")
    public void laReponseContientTokensEtUserId() {
        ctx.lastResponse.then()
            .body("accessToken",  notNullValue())
            .body("refreshToken", notNullValue())
            .body("userId",       notNullValue());
    }

    @Then("expiresIn est positif")
    public void expiresInEstPositif() {
        assertThat(ctx.lastResponse.jsonPath().getLong("expiresIn")).isPositive();
    }

    @Then("la réponse contient un accessToken, un refreshToken et le userId correct")
    public void laReponseContientTokensEtUserIdCorrect() {
        ctx.lastResponse.then()
            .body("accessToken",  notNullValue())
            .body("refreshToken", notNullValue())
            .body("userId",       notNullValue());
        assertThat(ctx.lastResponse.jsonPath().getString("userId")).isNotBlank();
    }

    @Then("la réponse contient un nouvel accessToken")
    public void laReponseContientNouvelAccessToken() {
        ctx.lastResponse.then().body("accessToken", notNullValue());
    }

    @Then("le nouvel accessToken est différent de l'ancien")
    public void leNouvelAccessTokenEstDifferent() {
        assertThat(ctx.lastResponse.jsonPath().getString("accessToken"))
            .isNotEqualTo(ctx.savedAccessToken);
    }

    @Then("ce refreshToken n'est plus utilisable pour rafraîchir")
    public void ceRefreshTokenNestPlusUtilisable() {
        int status = given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", ctx.workingRefreshToken))
            .post("/api/auth/refresh")
            .getStatusCode();
        assertThat(status).isEqualTo(401);
    }

    @Then("aucun des {int} refreshTokens ne peut plus être utilisé")
    public void aucunDesRefreshTokensNePeutPlusEtreUtilise(int count) {
        for (String token : ctx.multipleRefreshTokens) {
            int status = given().contentType(ContentType.JSON)
                .body(Map.of("refreshToken", token))
                .post("/api/auth/refresh")
                .getStatusCode();
            assertThat(status).isEqualTo(401);
        }
    }
}
