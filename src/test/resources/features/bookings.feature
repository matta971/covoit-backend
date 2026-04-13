Feature: Gestion des réservations (Bookings)
  En tant que passager ou conducteur
  Je veux pouvoir demander, accepter, rejeter et annuler des réservations
  Afin de gérer efficacement les places dans les trajets

  Background:
    Given un conducteur enregistré avec l'email "driver@covoit.com" et le mot de passe "password-secret-123"
    And un passager enregistré avec l'email "passenger@covoit.com" et le mot de passe "password-secret-123"
    And un autre passager enregistré avec l'email "passenger2@covoit.com" et le mot de passe "password-secret-123"

  # ─── POST /api/rides/{rideId}/booking-requests ──────────────────────────────

  Rule: Demander une réservation

    Scenario: Un passager réserve un trajet disponible
      Given le conducteur a publié un trajet "Paris" → "Lyon" avec 3 sièges pour demain
      When le passager authentifié envoie POST /api/rides/{rideId}/booking-requests
      Then la réponse a le statut 201
      And la réponse contient bookingRequestId, rideId, passengerId
      And le statut de la réservation est "REQUESTED"
      And requestedAt est renseigné

    Scenario: Deux passagers réservent le même trajet
      Given le conducteur a publié un trajet avec 2 sièges
      And le passager 1 a une réservation REQUESTED sur ce trajet
      When le passager 2 envoie POST /api/rides/{rideId}/booking-requests
      Then la réponse a le statut 201
      And le passager 2 a une réservation au statut "REQUESTED"

    Scenario: Un conducteur tente de réserver son propre trajet
      Given le conducteur a publié un trajet
      When le conducteur authentifié envoie POST /api/rides/{rideId}/booking-requests sur son propre trajet
      Then la réponse a le statut 403
      And le code d'erreur est "FORBIDDEN"

    Scenario: Réserver un trajet annulé
      Given le conducteur a publié puis annulé un trajet
      When le passager tente de réserver ce trajet
      Then la réponse a le statut 409
      And le code d'erreur est "RIDE_NOT_BOOKABLE"

    Scenario: Réserver un trajet dont la date de départ est passée
      Given un trajet dont la date de départ est dans le passé (simulé)
      When le passager tente de réserver ce trajet
      Then la réponse a le statut 409
      And le code d'erreur est "RIDE_NOT_BOOKABLE"

    Scenario: Réserver un trajet sans places disponibles
      Given le conducteur a publié un trajet avec 1 siège
      And ce siège a déjà été accepté pour un autre passager
      When le passager tente de réserver ce trajet
      Then la réponse a le statut 409
      And le code d'erreur est "NO_SEATS_AVAILABLE"

    Scenario: Réserver un trajet inexistant
      Given le passager est authentifié
      When il envoie POST /api/rides/00000000-0000-0000-0000-000000000000/booking-requests
      Then la réponse a le statut 404
      And le code d'erreur est "NOT_FOUND"

    Scenario: Réserver sans authentification
      Given un trajet disponible
      When POST /api/rides/{rideId}/booking-requests est appelé sans token
      Then la réponse a le statut 401

  # ─── POST /api/driver/me/booking-requests/{id}/accept ───────────────────────

  Rule: Accepter une réservation

    Scenario: Le conducteur accepte une demande de réservation
      Given le conducteur a publié un trajet avec 3 sièges
      And le passager a envoyé une demande de réservation (statut REQUESTED)
      When le conducteur authentifié accepte cette réservation
      Then la réponse a le statut 204
      And en consultant la réservation, son statut est "ACCEPTED"
      And decidedAt est renseigné
      And les sièges disponibles du trajet sont décrémentés de 1

    Scenario: Accepter une réservation déjà acceptée
      Given une réservation au statut "ACCEPTED"
      When le conducteur tente d'accepter cette réservation à nouveau
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Accepter une réservation déjà rejetée
      Given une réservation au statut "REJECTED"
      When le conducteur tente d'accepter cette réservation
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Accepter une réservation déjà annulée
      Given une réservation au statut "CANCELED"
      When le conducteur tente d'accepter cette réservation
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Un conducteur tente d'accepter une réservation d'un trajet qui n'est pas le sien
      Given un trajet publié par le conducteur A avec une réservation REQUESTED
      When le conducteur B authentifié tente d'accepter cette réservation
      Then la réponse a le statut 403
      And le code d'erreur est "FORBIDDEN"

    Scenario: Accepter une réservation inexistante
      Given le conducteur est authentifié
      When il accepte POST /api/driver/me/booking-requests/00000000-0000-0000-0000-000000000000/accept
      Then la réponse a le statut 404
      And le code d'erreur est "NOT_FOUND"

    Scenario: Accepter sans authentification
      Given une réservation existante
      When POST /api/driver/me/booking-requests/{id}/accept est appelé sans token
      Then la réponse a le statut 401

  # ─── POST /api/driver/me/booking-requests/{id}/reject ───────────────────────

  Rule: Rejeter une réservation

    Scenario: Le conducteur rejette une demande de réservation
      Given le conducteur a publié un trajet
      And le passager a envoyé une demande de réservation (statut REQUESTED)
      When le conducteur authentifié rejette cette réservation
      Then la réponse a le statut 204
      And en consultant la réservation, son statut est "REJECTED"
      And decidedAt est renseigné
      And les sièges disponibles du trajet restent inchangés

    Scenario: Rejeter une réservation déjà rejetée
      Given une réservation au statut "REJECTED"
      When le conducteur tente de rejeter cette réservation à nouveau
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Rejeter une réservation déjà acceptée
      Given une réservation au statut "ACCEPTED"
      When le conducteur tente de rejeter cette réservation
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Un conducteur tente de rejeter une réservation d'un trajet qui n'est pas le sien
      Given un trajet publié par le conducteur A avec une réservation REQUESTED
      When le conducteur B authentifié tente de rejeter cette réservation
      Then la réponse a le statut 403
      And le code d'erreur est "FORBIDDEN"

    Scenario: Rejeter une réservation inexistante
      Given le conducteur est authentifié
      When il appelle POST /api/driver/me/booking-requests/00000000-0000-0000-0000-000000000000/reject
      Then la réponse a le statut 404
      And le code d'erreur est "NOT_FOUND"

    Scenario: Rejeter sans authentification
      Given une réservation existante
      When POST /api/driver/me/booking-requests/{id}/reject est appelé sans token
      Then la réponse a le statut 401

  # ─── POST /api/booking-requests/{id}/cancel ─────────────────────────────────

  Rule: Annuler une réservation (côté passager)

    Scenario: Un passager annule sa réservation REQUESTED
      Given le passager a une réservation au statut "REQUESTED"
      When le passager authentifié annule cette réservation
      Then la réponse a le statut 204
      And en consultant la réservation, son statut est "CANCELED"
      And canceledAt est renseigné
      And les sièges disponibles du trajet restent inchangés

    Scenario: Un passager annule sa réservation ACCEPTED — le siège est libéré
      Given le passager a une réservation au statut "ACCEPTED"
      And les sièges disponibles du trajet ont été décrémentés
      When le passager authentifié annule cette réservation
      Then la réponse a le statut 204
      And le statut est "CANCELED"
      And les sièges disponibles du trajet sont incrémentés de 1

    Scenario: Un passager tente d'annuler une réservation déjà annulée
      Given une réservation au statut "CANCELED"
      When le passager tente d'annuler à nouveau
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Un passager tente d'annuler une réservation déjà rejetée
      Given une réservation au statut "REJECTED"
      When le passager tente d'annuler cette réservation
      Then la réponse a le statut 409
      And le code d'erreur est "INVALID_STATUS_TRANSITION"

    Scenario: Un passager tente d'annuler la réservation d'un autre passager
      Given le passager 1 a une réservation REQUESTED
      When le passager 2 authentifié tente d'annuler cette réservation
      Then la réponse a le statut 403
      And le code d'erreur est "FORBIDDEN"

    Scenario: Annuler une réservation inexistante
      Given le passager est authentifié
      When il appelle POST /api/booking-requests/00000000-0000-0000-0000-000000000000/cancel
      Then la réponse a le statut 404
      And le code d'erreur est "NOT_FOUND"

    Scenario: Annuler sans authentification
      Given une réservation existante
      When POST /api/booking-requests/{id}/cancel est appelé sans token
      Then la réponse a le statut 401

  # ─── GET /api/me/bookings ────────────────────────────────────────────────────

  Rule: Consulter ses propres réservations (passager)

    Scenario: Un passager consulte ses réservations
      Given le passager a 2 réservations sur des trajets différents
      When le passager authentifié appelle GET /api/me/bookings
      Then la réponse a le statut 200
      And la liste contient exactement 2 réservations
      And toutes les réservations ont passengerId correspondant au passager

    Scenario: Un passager sans réservation reçoit une liste vide
      Given le passager n'a aucune réservation
      When le passager authentifié appelle GET /api/me/bookings
      Then la réponse a le statut 200
      And la liste est vide

    Scenario: Les réservations d'autres passagers ne sont pas visibles
      Given le passager 1 a une réservation
      And le passager 2 est authentifié
      When le passager 2 appelle GET /api/me/bookings
      Then la réservation du passager 1 n'est pas dans la liste

    Scenario: Consulter sans authentification
      Given aucune authentification
      When GET /api/me/bookings est appelé
      Then la réponse a le statut 401

  # ─── GET /api/driver/me/booking-requests ────────────────────────────────────

  Rule: Consulter les demandes de réservation reçues (conducteur)

    Scenario: Un conducteur consulte les demandes de réservation sur ses trajets
      Given le conducteur a publié 2 trajets
      And chaque trajet a 1 demande de réservation REQUESTED
      When le conducteur authentifié appelle GET /api/driver/me/booking-requests
      Then la réponse a le statut 200
      And la liste contient 2 réservations
      And toutes les réservations correspondent aux trajets du conducteur

    Scenario: Un conducteur sans demande reçoit une liste vide
      Given le conducteur a publié un trajet mais aucun passager n'a réservé
      When le conducteur appelle GET /api/driver/me/booking-requests
      Then la réponse a le statut 200
      And la liste est vide

    Scenario: Les réservations sur les trajets d'autres conducteurs ne sont pas visibles
      Given le conducteur A a un trajet avec une réservation
      And le conducteur B est authentifié
      When le conducteur B appelle GET /api/driver/me/booking-requests
      Then la réservation sur le trajet du conducteur A n'est pas dans la liste

    Scenario: Consulter sans authentification
      Given aucune authentification
      When GET /api/driver/me/booking-requests est appelé
      Then la réponse a le statut 401

  # ─── Edge cases transversaux ─────────────────────────────────────────────────

  Rule: Cohérence des données et cas limites

    Scenario: Accepter deux réservations sur un trajet à 1 siège — le second échoue
      Given le conducteur a publié un trajet avec 1 siège
      And le passager 1 et le passager 2 ont chacun une réservation REQUESTED
      When le conducteur accepte la réservation du passager 1
      Then la réponse a le statut 204
      When le conducteur tente d'accepter la réservation du passager 2
      Then la réponse a le statut 409
      And le code d'erreur est "NO_SEATS_AVAILABLE"

    Scenario: Cycle complet — demande → acceptation → annulation passager → siège libéré
      Given le conducteur a publié un trajet avec 2 sièges
      And le passager a envoyé une demande de réservation
      And le conducteur a accepté la réservation (sièges : 1 restant)
      When le passager annule sa réservation acceptée
      Then la réponse a le statut 204
      And les sièges disponibles repassent à 2

    Scenario: Token expiré — toutes les routes retournent 401
      Given un token JWT expiré
      When il appelle successivement GET /api/rides, POST /api/rides, GET /api/me
      Then toutes les réponses ont le statut 401
      And le code d'erreur est "UNAUTHENTICATED"

    Scenario: Mise à jour concurrente d'une réservation — conflict détecté
      Given une réservation REQUESTED
      When deux conducteurs tentent d'accepter cette réservation simultanément
      Then l'un d'eux reçoit 204 et l'autre reçoit 409
      And le code d'erreur du second est "CONCURRENT_UPDATE"
