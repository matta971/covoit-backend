# Extraction du module auth vers auth-service

## Vue d'ensemble

Ce document décrit la stratégie d'extraction des modules `auth` + `identity`
du monolithe covoit-backend vers un microservice autonome.

```
Monolithe actuel                    Architecture cible
─────────────────                   ──────────────────
┌─────────────────┐                 ┌─────────────────┐   ┌─────────────────┐
│   identity      │                 │  auth-service   │   │    monolithe    │
│   auth          │    Phase 2 →    │  (identity +    │   │  (rides +       │
│   rides         │                 │   auth)         │   │   bookings +    │
│   bookings      │                 └────────┬────────┘   │   notif.)       │
│   notifications │                          │ JMS        └────────┬────────┘
└─────────────────┘                          └────────────────────►│
                                         UserRegisteredEvent       │
                                         (+ autres events)         │
```

---

## Phase 1 — ACTUELLE (branche feat/extract-auth-microservice)

### Ce qui a changé

| Composant | Changement | Pourquoi |
|-----------|-----------|----------|
| `pom.xml` | +Artemis embedded + spring-modulith-events-jms | Broker JMS sans nouveau container |
| `application.yaml` | +config Artemis + 7 queues | Transport externe pour les events |
| `RidePublishedEvent`, `RideCanceledEvent` | `@Externalized` | Traverseront la frontière rides→notifications-service |
| `BookingRequestedEvent`, `BookingAcceptedEvent`, `BookingRejectedEvent`, `BookingCanceledEvent` | `@Externalized` | Traverseront la frontière bookings→notifications-service |
| `UserRegisteredEvent` (NOUVEAU) | `@Externalized` | Prépare auth pour l'éventuelle cohérence inter-services |
| `AuthService` | publie `UserRegisteredEvent` | Source de vérité inscription |
| `TokenValidator` (NOUVEAU interface) | Sépare validation de génération | Identifie ce qui reste dans le monolithe |
| `JwtTokenProvider` | implements `TokenValidator` | Contrat explicite de la séparation |
| `JwtAuthenticationFilter` | injecte `TokenValidator` | Découplage du filtre |
| `SecurityConfig` | injecte `TokenValidator` | Découplage de la config sécurité |

### Avantages Spring Modulith observés

**1. `@Externalized` — un seul annotation**
Spring Modulith gère automatiquement :
- Sérialisation JSON de l'event
- Persistance dans `event_publication` AVANT l'envoi (garantie de cohérence)
- Envoi sur la queue JMS
- Retry automatique si Artemis est temporairement indisponible
- Les `@ApplicationModuleListener` locaux continuent de fonctionner en parallèle

**2. event_publication comme outbox**
La table `event_publication` (déjà présente via Flyway V5-V7) sert de
transactional outbox pattern : l'event est d'abord committée en base avec
la transaction métier, PUIS envoyé au broker. Aucun risque de perte
si le broker est down au moment de la publication.

**3. Frontières de modules déjà définies**
Les `package-info.java` avec `@ApplicationModule(allowedDependencies = ...)`
constituent déjà des contrats d'interface. Le travail de préparation consiste
à rendre ces frontières "traversables par le réseau", pas à les créer.

### Défis observés malgré Spring Modulith

**1. Noms de queues non typés**
La cible `@Externalized("rides.RidePublishedEvent")` est une string.
Renommer un event sans mettre à jour la cible casse le contrat sans
erreur de compilation. Mitigation : convention de nommage stricte
`{module}.{EventClassName}`.

**2. Secret HMAC partagé**
Avec HMAC-SHA256, auth-service et le monolithe devront partager
`app.jwt.secret` en configuration. C'est un couplage implicite par
configuration — risque si les deux services n'ont pas le même secret.

**3. Inversion de responsabilité pour UserRegisteredEvent**
Aujourd'hui le MONOLITHE publie `UserRegisteredEvent`.
En Phase 2, auth-SERVICE sera la source de vérité et publiera cet event.
Le monolithe devra RECEVOIR l'event au lieu de le publier.
Cette inversion nécessite un refactoring non trivial de Phase 2.

---

## Phase 2 — FUTURE : Créer auth-service

### Nouveau projet Spring Boot : covoit-auth-service

**Ce qui part dans auth-service :**
- Module `identity` complet (UserService, User, Role, UserRepository...)
- Module `auth` complet (AuthService, AuthController, MeController,
  RefreshSession, TokenIssuer/JwtTokenProvider — partie génération)
- Table `refresh_sessions` → base de données auth-service dédiée
- Table `users`, `roles`, `user_roles` → base de données auth-service dédiée

**Ce qui reste dans le monolithe :**
- `TokenValidator` + implémentation HMAC (valide les JWT reçus)
- `JwtAuthenticationFilter` (inchangé grâce à l'interface)
- `SecurityConfig` (inchangé grâce à l'interface)
- `CurrentUserProvider` + `CurrentUserProviderImpl` (lit le SecurityContext)
- Tous les modules `rides`, `bookings`, `notifications`

**Flux après extraction :**
```
1. Client → POST /auth/login → auth-service → JWT retourné
2. Client → GET /api/rides  → monolithe → JwtAuthenticationFilter valide JWT → OK
3. auth-service publie UserRegisteredEvent → queue auth.UserRegisteredEvent
4. monolithe écoute la queue → réagit si besoin
```

**Migrations Flyway :**
- auth-service hérite des migrations V1 (identity) et V2 (auth)
- Le monolithe supprime V1 et V2, garde V3 à V7

### Couplage résiduel entre auth-service et monolithe

| Type | Mécanisme | Solution |
|------|-----------|----------|
| JWT validation | Secret HMAC partagé en config | Phase 3 : RS256 |
| Données utilisateur | Monolithe stocke `driver_id`, `passenger_id` | Accepté — IDs UUID stables |
| Profil utilisateur | Monolithe appelle auth-service si besoin | API REST ou event UserUpdatedEvent |

---

## Phase 3 — OPTIONNELLE : Migrer vers RS256

Éliminer le partage de secret HMAC entre les deux services.

**auth-service :**
- Génère une paire de clés RSA au démarrage (ou depuis un KeyStore)
- Signe les JWT avec la clé privée
- Expose la clé publique via `GET /auth/.well-known/jwks.json`

**monolithe :**
- Télécharge la clé publique depuis le JWKS endpoint au démarrage
- Valide les JWT avec la clé publique uniquement
- Aucun secret partagé — séparation de sécurité complète

**Avantage :** un service compromis ne peut pas signer de nouveaux JWT.

---

## Résumé des avantages/défis Spring Modulith pour ce chantier

| Aspect | Avantage | Défi |
|--------|---------|------|
| Préparation | Frontières déjà définies via @ApplicationModule | — |
| Events | @Externalized = 1 annotation, 0 code infra | Noms de queues non typés |
| Fiabilité | event_publication = outbox intégré | Nécessite la BDD active pour publier |
| Découplage | @ApplicationModuleListener local ET JMS en parallèle | — |
| JWT | TokenValidator/TokenIssuer séparent proprement | Secret HMAC partagé en Phase 2 |
| Cohérence | UserRegisteredEvent prépare la séparation | Inversion de responsabilité en Phase 2 |
| Tests | ModularityTests vérifient les frontières | Pas de test automatique des contrats de queue |
