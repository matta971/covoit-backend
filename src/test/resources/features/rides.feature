Feature: Gestion des trajets (Rides)
  En tant que conducteur ou passager
  Je veux pouvoir publier, consulter et annuler des trajets
  Afin de faciliter le covoiturage

  Background:
    Given un conducteur enregistré avec l'email "driver@covoit.com" et le mot de passe "password-secret-123"
    And un passager enregistré avec l'email "passenger@covoit.com" et le mot de passe "password-secret-123"

  # ─── POST /api/rides ────────────────────────────────────────────────────────

  Rule: Publier un trajet

    Scenario: Publier un trajet valide
      Given le conducteur est authentifié
      When il publie un trajet avec les données suivantes :
        | from          | to    | departureTime        | totalSeats |
        | Paris         | Lyon  | demain à 09:00       | 3          |
      Then la réponse a le statut 201
      And la réponse contient un rideId non nul
      And le trajet a le statut "SCHEDULED"
      And les sièges disponibles sont 3
      And le driverId correspond à l'id du conducteur

    Scenario: Publier un trajet sans authentification
      Given aucune authentification
      When il publie un trajet vers "Lyon" avec départ demain
      Then la réponse a le statut 401

    Scenario: Publier un trajet avec un champ "from" vide
      Given le conducteur est authentifié
      When il publie un trajet avec from="" to="Lyon" departureTime=demain totalSeats=2
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Publier un trajet avec un champ "to" vide
      Given le conducteur est authentifié
      When il publie un trajet avec from="Paris" to="" departureTime=demain totalSeats=2
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Publier un trajet avec une date de départ dans le passé
      Given le conducteur est authentifié
      When il publie un trajet avec from="Paris" to="Lyon" departureTime=hier totalSeats=2
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Publier un trajet avec zéro siège
      Given le conducteur est authentifié
      When il publie un trajet avec from="Paris" to="Lyon" departureTime=demain totalSeats=0
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Publier un trajet avec un nombre de sièges négatif
      Given le conducteur est authentifié
      When il publie un trajet avec from="Paris" to="Lyon" departureTime=demain totalSeats=-1
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Publier un trajet sans body
      Given le conducteur est authentifié
      When il envoie POST /api/rides sans body
      Then la réponse a le statut 400

  # ─── GET /api/rides ─────────────────────────────────────────────────────────

  Rule: Rechercher des trajets

    Scenario: Rechercher tous les trajets sans filtre
      Given un trajet "Paris" → "Lyon" publié pour demain
      And un trajet "Bordeaux" → "Nantes" publié pour après-demain
      When le passager est authentifié et cherche GET /api/rides
      Then la réponse a le statut 200
      And la réponse contient une liste d'au moins 2 trajets

    Scenario: Rechercher des trajets avec filtre "from"
      Given un trajet "Paris" → "Lyon" publié pour demain
      And un trajet "Bordeaux" → "Nantes" publié pour après-demain
      When le passager cherche les trajets avec from="Paris"
      Then la réponse a le statut 200
      And tous les trajets retournés ont from="Paris"
      And le trajet "Bordeaux" → "Nantes" n'est pas dans les résultats

    Scenario: Rechercher des trajets avec filtre "to"
      Given un trajet "Paris" → "Lyon" publié pour demain
      When le passager cherche les trajets avec to="Lyon"
      Then la réponse a le statut 200
      And tous les trajets retournés ont to="Lyon"

    Scenario: Rechercher des trajets avec filtre "date"
      Given un trajet "Paris" → "Lyon" publié pour demain
      And un trajet "Paris" → "Lyon" publié pour dans 7 jours
      When le passager cherche les trajets avec date=demain
      Then la réponse a le statut 200
      And la liste contient uniquement les trajets du jour demain

    Scenario: Rechercher des trajets avec combinaison de filtres
      Given plusieurs trajets publiés
      When le passager cherche avec from="Paris" to="Lyon" date=demain
      Then la réponse a le statut 200
      And tous les résultats correspondent aux trois critères

    Scenario: Rechercher des trajets sans authentification
      Given aucune authentification
      When GET /api/rides est appelé
      Then la réponse a le statut 401

    Scenario: Aucun trajet ne correspond aux critères de recherche
      When le passager cherche les trajets avec from="Timbuktu"
      Then la réponse a le statut 200
      And la liste est vide

  # ─── GET /api/rides/{rideId} ────────────────────────────────────────────────

  Rule: Consulter un trajet par ID

    Scenario: Consulter un trajet existant
      Given un trajet "Paris" → "Lyon" publié avec 3 sièges pour demain
      When le passager consulte ce trajet par son rideId
      Then la réponse a le statut 200
      And la réponse contient rideId, driverId, from="Paris", to="Lyon", totalSeats=3
      And le statut du trajet est "SCHEDULED"

    Scenario: Consulter un trajet inexistant
      When le passager consulte GET /api/rides/00000000-0000-0000-0000-000000000000
      Then la réponse a le statut 404
      And le code d'erreur est "NOT_FOUND"

    Scenario: Consulter un trajet avec un UUID mal formé
      When le passager consulte GET /api/rides/not-a-uuid
      Then la réponse a le statut 400

    Scenario: Consulter un trajet sans authentification
      Given un trajet existant
      When GET /api/rides/{rideId} est appelé sans token
      Then la réponse a le statut 401

  # ─── POST /api/rides/{rideId}/cancel ────────────────────────────────────────

  Rule: Annuler un trajet

    Scenario: Le conducteur annule son propre trajet planifié
      Given le conducteur est authentifié
      And il a publié un trajet "Paris" → "Lyon" pour demain
      When il annule ce trajet
      Then la réponse a le statut 204
      And en consultant le trajet, son statut est "CANCELED"

    Scenario: Le conducteur annule un trajet avec des réservations acceptées
      Given le conducteur a publié un trajet avec 2 sièges
      And un passager a une réservation acceptée sur ce trajet
      When le conducteur annule le trajet
      Then la réponse a le statut 204
      And le siège libéré est reflété dans les sièges disponibles

    Scenario: Un passager tente d'annuler un trajet dont il n'est pas conducteur
      Given un trajet publié par le conducteur
      When le passager tente d'annuler ce trajet
      Then la réponse a le statut 403
      And le code d'erreur est "FORBIDDEN"

    Scenario: Annuler un trajet déjà annulé
      Given le conducteur a déjà annulé son trajet
      When il tente d'annuler le même trajet à nouveau
      Then la réponse a le statut 409
      And le code d'erreur est "RIDE_NOT_BOOKABLE"

    Scenario: Annuler un trajet inexistant
      Given le conducteur est authentifié
      When il tente d'annuler GET /api/rides/00000000-0000-0000-0000-000000000000/cancel
      Then la réponse a le statut 404
      And le code d'erreur est "NOT_FOUND"

    Scenario: Annuler un trajet sans authentification
      Given un trajet existant
      When POST /api/rides/{rideId}/cancel est appelé sans token
      Then la réponse a le statut 401

  # ─── GET /api/driver/me/rides ────────────────────────────────────────────────

  Rule: Consulter ses propres trajets en tant que conducteur

    Scenario: Un conducteur consulte ses trajets
      Given le conducteur est authentifié
      And il a publié 2 trajets
      When il appelle GET /api/driver/me/rides
      Then la réponse a le statut 200
      And la liste contient exactement 2 trajets
      And tous les trajets ont driverId correspondant au conducteur

    Scenario: Un conducteur sans trajet reçoit une liste vide
      Given le conducteur est authentifié et n'a publié aucun trajet
      When il appelle GET /api/driver/me/rides
      Then la réponse a le statut 200
      And la liste est vide

    Scenario: Les trajets d'autres conducteurs ne sont pas visibles
      Given le conducteur A a publié un trajet
      And le conducteur B est authentifié
      When le conducteur B appelle GET /api/driver/me/rides
      Then le trajet du conducteur A n'est pas dans la liste

    Scenario: Appeler GET /api/driver/me/rides sans authentification
      Given aucune authentification
      When GET /api/driver/me/rides est appelé
      Then la réponse a le statut 401

  # ─── GET /api/me ─────────────────────────────────────────────────────────────

  Rule: Consulter son profil

    Scenario: Un utilisateur consulte son propre profil
      Given le passager est authentifié
      When il appelle GET /api/me
      Then la réponse a le statut 200
      And la réponse contient id, email="passenger@covoit.com", displayName, roles

    Scenario: Consulter le profil sans authentification
      Given aucune authentification
      When GET /api/me est appelé
      Then la réponse a le statut 401
