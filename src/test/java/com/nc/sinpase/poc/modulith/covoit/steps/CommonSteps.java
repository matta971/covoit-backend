package com.nc.sinpase.poc.modulith.covoit.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps shared across all feature files:
 *   - Background registration steps (rides + bookings)
 *   - Common Then assertions
 */
public class CommonSteps {

    @Autowired private SharedContext ctx;
    @Autowired private TestHelper    helper;

    // ─── Background: registration ─────────────────────────────────────────────

    @Given("un conducteur enregistré avec l'email {string} et le mot de passe {string}")
    public void unConducteurEnregistre(String email, String password) {
        helper.register(email, password, "Driver");
        var resp = helper.login(email, password);
        ctx.driverToken = resp.jsonPath().getString("accessToken");
        ctx.driverId    = java.util.UUID.fromString(resp.jsonPath().getString("userId"));
    }

    @Given("un passager enregistré avec l'email {string} et le mot de passe {string}")
    public void unPassagerEnregistre(String email, String password) {
        helper.register(email, password, "Passenger");
        var resp = helper.login(email, password);
        ctx.passengerToken = resp.jsonPath().getString("accessToken");
        ctx.passengerId    = java.util.UUID.fromString(resp.jsonPath().getString("userId"));
    }

    @Given("un autre passager enregistré avec l'email {string} et le mot de passe {string}")
    public void unAutrePassagerEnregistre(String email, String password) {
        helper.register(email, password, "Passenger2");
        var resp = helper.login(email, password);
        ctx.passenger2Token = resp.jsonPath().getString("accessToken");
        ctx.passenger2Id    = java.util.UUID.fromString(resp.jsonPath().getString("userId"));
    }

    @Given("aucune authentification")
    public void aucuneAuthentification() {
        // No-op: steps that need "no auth" simply omit the Authorization header
    }

    // ─── Common assertions ────────────────────────────────────────────────────

    @Then("la réponse a le statut {int}")
    public void laReponseALeStatut(int expected) {
        assertThat(ctx.lastResponse.getStatusCode())
            .as("HTTP status")
            .isEqualTo(expected);
    }

    @Then("le code d'erreur est {string}")
    public void leCodeErreurEst(String errorCode) {
        assertThat(ctx.lastResponse.jsonPath().getString("errorCode"))
            .as("errorCode")
            .isEqualTo(errorCode);
    }

    @Then("la liste est vide")
    public void laListeEstVide() {
        assertThat(ctx.lastResponse.jsonPath().getList("$")).isEmpty();
    }

    @Then("toutes les réponses ont le statut {int}")
    public void toutesLesReponsesOntLeStatut(int expected) {
        for (var resp : ctx.allResponses) {
            assertThat(resp.getStatusCode()).as("HTTP status").isEqualTo(expected);
        }
    }
}
