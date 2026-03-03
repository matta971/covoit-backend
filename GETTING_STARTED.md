# Covoit Backend — Guide de démarrage (QA)

## Prérequis

| Outil | Version | Lien |
|-------|---------|------|
| **Java JDK** | 25 | https://jdk.java.net/25/ |
| **Docker Desktop** | récent | https://www.docker.com/products/docker-desktop |
| **Bruno** | récent | https://www.usebruno.com/ |

> Maven n'est **pas** nécessaire, le wrapper `mvnw` est inclus dans le projet.

---

## 1. Démarrer la base de données

Ouvrir un terminal dans le dossier `covoit-backend` et lancer :

```bash
docker compose up -d
```

Vérifier que le container est actif :

```bash
docker ps
```

Vous devez voir un container `postgres:16` en statut `Up`.

---

## 2. Lancer le backend

### Windows (PowerShell)

```powershell
$env:JAVA_HOME = "C:\chemin\vers\jdk-25"   # adapter à votre installation
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
./mvnw spring-boot:run
```

### Mac / Linux (bash)

```bash
export JAVA_HOME="/chemin/vers/jdk-25"      # adapter à votre installation
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run
```

Le backend est prêt quand vous voyez dans les logs :

```
Started CovoitApplication in X.XXX seconds
```

L'API est accessible sur **http://localhost:8080**.

---

## 3. Tester avec Bruno

1. Ouvrir Bruno
2. **Open Collection** → sélectionner le dossier `covoit-backend/bruno/`
3. Sélectionner l'environnement **local** (en haut à droite dans Bruno)
4. Lancer les requêtes dans l'ordre suivant :

### Ordre recommandé

```
auth/01_register          → crée le compte conducteur
auth/02_login             → connecte le conducteur (stocke le token auto.)
auth/07_register-passenger
auth/08_login-passenger

rides/02_publish          → publie un trajet
rides/01_search           → recherche des trajets
rides/03_get-by-id

bookings/01_request       → le passager demande une réservation
bookings/02_my-bookings

driver/01_my-rides
driver/02_booking-requests
driver/03_accept-booking

cleanup/01_cancel-booking
cleanup/02_cancel-ride
cleanup/03_reject-booking
```

> Les tokens JWT sont récupérés et injectés automatiquement entre les requêtes grâce aux scripts post-response.

### Identifiants de test (environnement local)

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| Conducteur | `test@test.com` | `Password1234xxx` |
| Passager | `passager@test.com` | `Password1234xxx` |

---

## 4. Arrêter l'environnement

Arrêter le backend : `Ctrl+C` dans le terminal

Arrêter PostgreSQL :

```bash
docker compose down
```

> Les données sont **persistées** dans un volume Docker. `docker compose down -v` supprime les données.
