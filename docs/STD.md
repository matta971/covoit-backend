# POC Covoiturage — Spécification Technique Détaillée (STD)

## 0. Informations & conventions
- **Backend** : Spring Boot + Spring Modulith + DDD + Hexagonal
- **Frontend** : React compatible Expo (Expo React Native + Web)
- **Base de données** : PostgreSQL
- **Auth** : JWT access token + refresh token opaque stocké (hash) en DB, rotation
- **API** : REST + OpenAPI
- **Temps** : toutes les dates en ISO-8601, timezone explicite côté serveur

---

## 1. Architecture générale

### 1.1 Vue d'ensemble
- Monolithe modulaire (un seul déploiement backend) avec modules métier isolés.
- Chaque module suit une architecture hexagonale :
    - `domain` : agrégats, invariants, événements de domaine
    - `application` : cas d'usage (use cases), orchestration, ports
    - `adapters/in` : REST controllers (ou listeners)
    - `adapters/out` : persistence, services externes, etc.

### 1.2 Modules
1. `identity` : utilisateurs, rôles
2. `auth` : register/login/logout/refresh, sessions refresh, sécurité
3. `rides` : trajets (publication, recherche, capacité, annulation)
4. `bookings` : demandes de réservation (workflow + coordination places)
5. `notifications` : écoute d'événements + notifications mock (log/inbox)

### 1.3 Dépendances autorisées (principe)
- `auth` -> `identity` (chargement user + rôles)
- `bookings` -> API exposée par `rides` (réserver/libérer une place)
- `notifications` écoute des événements publiés par `rides` et `bookings` (et éventuellement `auth`)
- `rides` et `bookings` ne dépendent pas d'implémentations internes d'autres modules (uniquement contrats/API exposés)

---

## 2. Organisation des packages (exemple)

```
com.acme.carpool
├── CarpoolApplication.java
├── identity/
│   ├── domain/
│   ├── application/
│   ├── adapters/in/rest/
│   └── adapters/out/persistence/
├── auth/
│   ├── domain/
│   ├── application/
│   ├── adapters/in/rest/
│   ├── adapters/out/persistence/
│   └── adapters/out/security/
├── rides/
│   ├── domain/
│   ├── application/
│   ├── adapters/in/rest/
│   └── adapters/out/persistence/
├── bookings/
│   ├── domain/
│   ├── application/
│   ├── adapters/in/rest/
│   └── adapters/out/persistence/
└── notifications/
    ├── application/
    ├── adapters/in/events/
    └── adapters/out/mock/
```

---

## 3. Backend — Modèle de données (PostgreSQL)

### 3.1 Tables `identity`

#### `users`
- `id` UUID PK
- `email` VARCHAR UNIQUE NOT NULL
- `password_hash` VARCHAR NOT NULL
- `display_name` VARCHAR NOT NULL
- `phone_dial_code` VARCHAR NOT NULL (ex: +33, +687)
- `phone_number` VARCHAR NOT NULL (numéro local sans indicatif, format validé selon l'indicatif)
- `status` VARCHAR NOT NULL (ex: ACTIVE, LOCKED)
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- (option) `last_login_at` TIMESTAMP

#### `roles`
- `id` UUID PK
- `name` VARCHAR UNIQUE NOT NULL (ex: ROLE_USER)

#### `user_roles`
- `user_id` UUID FK users(id)
- `role_id` UUID FK roles(id)
- PK composite `(user_id, role_id)`

### 3.2 Tables `auth`

#### `refresh_sessions`
- `id` UUID PK
- `user_id` UUID FK users(id) NOT NULL
- `refresh_token_hash` VARCHAR NOT NULL (hash du token opaque)
- `created_at` TIMESTAMP NOT NULL
- `expires_at` TIMESTAMP NOT NULL
- `revoked_at` TIMESTAMP NULL
- `replaced_by_session_id` UUID NULL (rotation)
- `device_id` VARCHAR NULL
- `user_agent` VARCHAR NULL
- `ip` VARCHAR NULL

> Décision sécurité : stocker uniquement le **hash** du refresh token, jamais le token brut.

### 3.3 Tables `rides`

#### `rides`
- `id` UUID PK
- `driver_id` UUID NOT NULL (FK possible -> users.id)
- `from_location` VARCHAR NOT NULL
- `to_location` VARCHAR NOT NULL
- `departure_time` TIMESTAMP NOT NULL
- `total_seats` INT NOT NULL
- `available_seats` INT NOT NULL
- `status` VARCHAR NOT NULL (SCHEDULED/CANCELED)
- `version` INT NOT NULL (optimistic lock)
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

### 3.4 Tables `bookings`

#### `booking_requests`
- `id` UUID PK
- `ride_id` UUID NOT NULL (FK possible -> rides.id)
- `passenger_id` UUID NOT NULL (FK possible -> users.id)
- `status` VARCHAR NOT NULL (REQUESTED/ACCEPTED/REJECTED/CANCELED)
- `requested_at` TIMESTAMP NOT NULL
- `decided_at` TIMESTAMP NULL
- `canceled_at` TIMESTAMP NULL
- `version` INT NOT NULL (optimistic lock)
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

### 3.5 Tables `notifications` (option)

#### `notifications`
- `id` UUID PK
- `user_id` UUID NOT NULL
- `type` VARCHAR NOT NULL
- `payload_json` JSONB NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `read_at` TIMESTAMP NULL

---

## 4. Backend — DDD (agrégats, invariants, événements)

### 4.1 Agrégat `Ride`

**Invariants**
- `total_seats > 0`
- `available_seats` entre 0 et `total_seats`
- Interdiction de réserver si `status=CANCELED` ou `departure_time < now`

**Comportements**
- `publish(...)`
- `cancel(byDriverId)`
- `reserveSeat()` (si place dispo)
- `releaseSeat()` (idempotent ou strict selon choix)

**Domain events**
- `RidePublished`
- `RideCanceled`
- (option) `SeatReserved`, `SeatReleased`

### 4.2 Agrégat `BookingRequest`

**Invariants & transitions**
- `REQUESTED -> ACCEPTED | REJECTED | CANCELED`
- `ACCEPTED -> CANCELED`
- `REJECTED` terminal
- `CANCELED` terminal

**Comportements**
- `request(rideId, passengerId)`
- `accept(byDriverId)`
- `reject(byDriverId)`
- `cancel(byUserId)`

**Domain events**
- `BookingRequested`
- `BookingAccepted`
- `BookingRejected`
- `BookingCanceled`

---

## 5. Backend — Contrats inter-modules (ports)

### 5.1 Port "utilisateur courant"

Interface (port) accessible aux modules métier :
- `CurrentUserProvider.getUserId()`
- `CurrentUserProvider.getRoles()`

Implémentation dans un adapter "security" (s'appuyant sur Spring Security).

### 5.2 Port `RideCapacityPort` (exposé par `rides`, consommé par `bookings`)
- `reserveSeat(rideId, passengerId)` -> succès/erreur métier
- `releaseSeat(rideId, passengerId)` -> succès/erreur métier
- (option) `assertRideBookable(rideId)` pour validations rapides

> But : éviter que `bookings` manipule directement l'entité `Ride`.

---

## 6. Backend — Auth niveau 3 (JWT + refresh)

### 6.1 Tokens
- **Access token** : JWT (durée courte, ex 15 minutes)
- **Refresh token** : opaque, stocké hashé, durée longue (ex 30 jours)
- **Rotation** : chaque refresh produit un nouveau refresh token, l'ancien est révoqué.

### 6.2 Claims JWT recommandés
- `sub` : userId
- `roles` : liste des rôles
- `iat`, `exp`
- `iss` (issuer), `aud` (audience)

### 6.3 Endpoints Auth

#### `POST /api/auth/register`

Request:
```json
{
  "email": "...",
  "password": "...",
  "displayName": "...",
  "phoneDialCode": "+687",
  "phoneNumber": "123456"
}
```

> Validation backend : regex sur l'email, format du numéro validé selon l'indicatif fourni. Pas de vérification d'email par lien.

Response 201:
```json
{ "userId": "...", "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }
```

#### `POST /api/auth/login`

Request:
```json
{ "email": "...", "password": "..." }
```

Response 200:
```json
{ "userId": "...", "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }
```

#### `POST /api/auth/refresh`

Request:
```json
{ "refreshToken": "..." }
```

Response 200:
```json
{ "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }
```

#### `POST /api/auth/logout`

Request:
```json
{ "refreshToken": "..." }
```

Response 204

#### `POST /api/auth/logout-all`

Response 204 (utilise l'utilisateur courant via access token)

#### `GET /api/me`

Response 200:
```json
{
  "userId": "...",
  "email": "...",
  "displayName": "...",
  "phoneDialCode": "+687",
  "phoneNumber": "123456",
  "roles": ["ROLE_USER"]
}
```

### 6.4 Politique mots de passe (recommandation)
- min 12 caractères
- diversité (maj/min/chiffre/symbole) OU passphrase (à définir)
- stockage : BCrypt/Argon2 (BCrypt acceptable POC)

### 6.5 Rate limiting (POC mais utile)
- limiter login et register (ex: 5/min/ip) pour éviter brute-force
- (impl simple : bucket en mémoire) ou via reverse proxy

---

## 7. Backend — API métier (rides + bookings)

### 7.1 Rides

#### `POST /api/rides`

Auth: Bearer

Request:
```json
{ "from": "Nouméa", "to": "Dumbéa", "departureTime": "2026-03-01T08:30:00+11:00", "totalSeats": 3 }
```

Response 201:
```json
{ "rideId": "..." }
```

#### `GET /api/rides?from=&to=&date=YYYY-MM-DD`

Auth: Bearer

Response 200:
```json
[
  { "rideId": "...", "from": "...", "to": "...", "departureTime": "...", "availableSeats": 2, "status": "SCHEDULED" }
]
```

#### `GET /api/rides/{rideId}`

Auth: Bearer

Response 200:
```json
{ "rideId": "...", "driverId": "...", "from": "...", "to": "...", "departureTime": "...", "totalSeats": 3, "availableSeats": 2, "status": "SCHEDULED" }
```

#### `GET /api/driver/me/rides`

Auth: Bearer

Response 200: liste

#### `POST /api/rides/{rideId}/cancel`

Auth: Bearer

Response 204

### 7.2 Bookings

#### `POST /api/rides/{rideId}/booking-requests`

Auth: Bearer

Response 201:
```json
{ "bookingRequestId": "..." }
```

#### `GET /api/me/bookings`

Auth: Bearer

Response 200:
```json
[
  { "bookingRequestId": "...", "rideId": "...", "status": "REQUESTED", "departureTime": "...", "from": "...", "to": "..." }
]
```

#### `GET /api/driver/me/booking-requests`

Auth: Bearer

Response 200:
```json
[
  { "bookingRequestId": "...", "rideId": "...", "passengerId": "...", "status": "REQUESTED" }
]
```

#### `POST /api/booking-requests/{id}/accept`

Auth: Bearer — Response 204

#### `POST /api/booking-requests/{id}/reject`

Auth: Bearer — Response 204

#### `POST /api/booking-requests/{id}/cancel`

Auth: Bearer — Response 204

---

## 8. Erreurs & format standard

### 8.1 Format

```json
{
  "errorCode": "NO_SEATS_AVAILABLE",
  "message": "No seats available for this ride.",
  "details": { "rideId": "...", "bookingRequestId": "..." },
  "traceId": "..."
}
```

### 8.2 Codes HTTP & cas

| Code | errorCode | Description |
|------|-----------|-------------|
| 400 | `VALIDATION_ERROR` | Champ manquant, date invalide |
| 401 | `UNAUTHENTICATED` | Token absent/invalide/expiré, refresh invalide |
| 403 | `FORBIDDEN` | Ownership ride, action non permise |
| 404 | `NOT_FOUND` | Ride/booking inconnu |
| 409 | `NO_SEATS_AVAILABLE` | Plus de place disponible |
| 409 | `RIDE_NOT_BOOKABLE` | Trajet non réservable |
| 409 | `INVALID_STATUS_TRANSITION` | Transition de statut invalide |
| 409 | `CONCURRENT_UPDATE` | Conflit de concurrence |

---

## 9. Transactions & concurrence

### 9.1 Verrouillage optimiste
- `rides.version` + `booking_requests.version`
- En cas de collision sur acceptation => 409 `CONCURRENT_UPDATE` (ou `NO_SEATS_AVAILABLE` si dernière place)

### 9.2 Séquence Acceptation (recommandée)
1. Charger `BookingRequest` (doit être `REQUESTED`)
2. Vérifier ownership (driver)
3. Appeler `rides.reserveSeat(rideId, passengerId)` (transaction)
4. Marquer `BookingRequest` `ACCEPTED` (transaction)
5. Publier événements

> Selon impl, les steps 3 et 4 doivent être cohérents transactionnellement.

---

## 10. Notifications (POC)
- Listener d'événements (intra-process)
- Adapter de sortie "mock" :
  - log structuré
  - (option) insertion en table `notifications`

---

## 11. Frontend — Expo (React) compatible Web

### 11.1 Cible
- iOS/Android via Expo
- Web via Expo Web (React Native Web)

### 11.2 Stack suggérée
- Expo + TypeScript
- Routing : `expo-router`
- Data fetching/cache : TanStack Query
- State auth : Zustand (ou context)
- Storage tokens :
  - Mobile : `expo-secure-store`
  - Web : `localStorage` (POC) via abstraction

### 11.3 Gestion Auth côté front (mécanisme)
- Stocker refresh token durablement (secure store / localStorage)
- Stocker access token en mémoire + persistance optionnelle
- Interceptor :
  - si 401 sur API => call `/auth/refresh` => retry requête
  - si refresh échoue => logout + retour login

### 11.4 Écrans (routes)
- `/auth/register`
- `/auth/login`
- `/me`
- `/search`
- `/rides/[rideId]`
- `/me/bookings`
- `/driver/rides`
- `/driver/rides/[rideId]`
- `/driver/requests`

### 11.5 Composant saisie numéro de téléphone (écran inscription)
- **Sélecteur d'indicatif** : liste déroulante des indicatifs pays (ex: 🇫🇷 +33, 🇳🇨 +687…)
- **Champ numéro** : placeholder et format attendu mis à jour dynamiquement selon l'indicatif sélectionné (ex: `XX XX XX XX XX` pour +33)
- **Validation côté front** : regex ou bibliothèque (ex: `libphonenumber-js`) pour vérifier le format avant envoi
- **Validation côté back** : même logique, le backend rejette un numéro dont le format ne correspond pas à l'indicatif fourni (`400 VALIDATION_ERROR`)

### 11.6 Contrat API
- Consommation REST
- Types TS :
  - soit écrits à la main (POC)
  - soit générés depuis OpenAPI (plus fiable)

---

## 12. Observabilité & logs
- `traceId` généré par requête (filtre) et renvoyé dans erreurs
- Logs structurés des décisions importantes :
  - login success/fail (sans infos sensibles)
  - acceptation/refus/annulation
  - conflits de concurrence

---

## 13. Tests

### 13.1 Backend

**Unit tests :**
- invariants `Ride` (capacité)
- transitions `BookingRequest`

**Intégration :**
- scénario complet : publish -> request -> accept -> cancel -> seats restored
- scénario concurrence dernière place

**Sécurité :**
- endpoints protégés
- ownership vérifié

### 13.2 Front

Tests légers POC :
- service auth (refresh)
- navigation guards (si non connecté)

---

## 14. Décisions techniques arrêtées

| Sujet | Décision |
|-------|----------|
| Date/heure | Validée côté backend — ISO-8601 avec timezone explicite obligatoire |
| Réservation | 1 place par demande |
| Web storage | `localStorage` accepté (limite sécurité connue et assumée) |
| Email verification | Pas de vérification par lien — regex uniquement, côté front et back |
| Numéro de téléphone | Obligatoire à l'inscription — sélecteur d'indicatif pays + numéro validé selon l'indicatif (front + back) |
