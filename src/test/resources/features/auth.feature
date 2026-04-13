Feature: Authentification (Auth)
  En tant qu'utilisateur
  Je veux pouvoir m'inscrire, me connecter, rafraîchir mon token et me déconnecter
  Afin d'accéder aux fonctionnalités de covoiturage de façon sécurisée

  # ─── POST /api/auth/register ─────────────────────────────────────────────────

  Rule: S'inscrire

    Scenario: Inscription valide
      When un utilisateur s'inscrit avec les données suivantes :
        | email              | password            | displayName | phoneDialCode | phoneNumber |
        | alice@covoit.com   | mot-de-passe-12345  | Alice       | +33           | 0612345678  |
      Then la réponse a le statut 201
      And la réponse contient un accessToken, un refreshToken et un userId
      And expiresIn est positif

    Scenario: Inscription avec un email déjà utilisé
      Given un utilisateur avec l'email "alice@covoit.com" existe déjà
      When un second utilisateur s'inscrit avec l'email "alice@covoit.com"
      Then la réponse a le statut 409
      And le code d'erreur est "USER_ALREADY_EXISTS"

    Scenario: Inscription avec un mot de passe trop court (moins de 12 caractères)
      When un utilisateur s'inscrit avec password="court"
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Inscription sans email
      When un utilisateur s'inscrit sans champ email
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Inscription sans displayName
      When un utilisateur s'inscrit sans champ displayName
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Inscription sans numéro de téléphone
      When un utilisateur s'inscrit sans phoneDialCode ni phoneNumber
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Inscription sans body
      When POST /api/auth/register est appelé sans body
      Then la réponse a le statut 400

  # ─── POST /api/auth/login ─────────────────────────────────────────────────────

  Rule: Se connecter

    Scenario: Connexion valide
      Given un utilisateur enregistré avec l'email "bob@covoit.com" et le mot de passe "secret-password-42"
      When il se connecte avec ces identifiants
      Then la réponse a le statut 200
      And la réponse contient un accessToken, un refreshToken et le userId correct

    Scenario: Connexion avec un mot de passe incorrect
      Given un utilisateur enregistré avec l'email "bob@covoit.com"
      When il se connecte avec le mot de passe "mauvais-mot-de-passe"
      Then la réponse a le statut 401
      And le code d'erreur est "UNAUTHENTICATED"

    Scenario: Connexion avec un email inexistant
      When un utilisateur tente de se connecter avec l'email "inconnu@covoit.com"
      Then la réponse a le statut 401
      And le code d'erreur est "UNAUTHENTICATED"

    Scenario: Connexion sans email
      When POST /api/auth/login est appelé sans champ email
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

    Scenario: Connexion sans mot de passe
      When POST /api/auth/login est appelé sans champ password
      Then la réponse a le statut 400
      And le code d'erreur est "VALIDATION_ERROR"

  # ─── POST /api/auth/refresh ───────────────────────────────────────────────────

  Rule: Rafraîchir le token d'accès

    Scenario: Rafraîchissement avec un refreshToken valide
      Given un utilisateur connecté avec un refreshToken valide
      When il appelle POST /api/auth/refresh avec ce refreshToken
      Then la réponse a le statut 200
      And la réponse contient un nouvel accessToken
      And le nouvel accessToken est différent de l'ancien

    Scenario: Rafraîchissement avec un refreshToken invalide
      When POST /api/auth/refresh est appelé avec refreshToken="token-bidon"
      Then la réponse a le statut 401
      And le code d'erreur est "UNAUTHENTICATED"

    Scenario: Rafraîchissement avec un refreshToken déjà révoqué (après logout)
      Given un utilisateur s'est déconnecté (son refreshToken est révoqué)
      When il tente de rafraîchir avec ce refreshToken révoqué
      Then la réponse a le statut 401
      And le code d'erreur est "UNAUTHENTICATED"

    Scenario: Rafraîchissement sans body
      When POST /api/auth/refresh est appelé sans body
      Then la réponse a le statut 400

  # ─── POST /api/auth/logout ────────────────────────────────────────────────────

  Rule: Se déconnecter (session courante)

    Scenario: Déconnexion valide
      Given un utilisateur connecté avec un refreshToken valide
      When il appelle POST /api/auth/logout avec ce refreshToken
      Then la réponse a le statut 204
      And ce refreshToken n'est plus utilisable pour rafraîchir

    Scenario: Déconnexion sans authentification
      When POST /api/auth/logout est appelé sans accessToken
      Then la réponse a le statut 401

    Scenario: Déconnexion avec un refreshToken déjà révoqué
      Given un refreshToken déjà révoqué
      When POST /api/auth/logout est appelé avec ce refreshToken
      Then la réponse a le statut 401
      And le code d'erreur est "UNAUTHENTICATED"

  # ─── POST /api/auth/logout-all ───────────────────────────────────────────────

  Rule: Se déconnecter de toutes les sessions

    Scenario: Déconnexion de toutes les sessions
      Given un utilisateur avec 3 sessions actives (3 refreshTokens)
      When il appelle POST /api/auth/logout-all
      Then la réponse a le statut 204
      And aucun des 3 refreshTokens ne peut plus être utilisé

    Scenario: Déconnexion de toutes les sessions sans authentification
      When POST /api/auth/logout-all est appelé sans accessToken
      Then la réponse a le statut 401
