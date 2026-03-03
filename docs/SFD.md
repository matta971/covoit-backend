# POC Covoiturage — Spécification Fonctionnelle Détaillée (SFD)

## 0. Informations
- **Produit** : POC "Covoiturage"
- **Objectif** : démontrer une architecture DDD + hexagonale dans un monolithe modulaire, avec un front React compatible Expo.
- **Niveau Auth** : Niveau 3 (auth réelle : inscription, login, JWT, refresh, logout)
- **Périmètre** : MVP POC (fonctionnalités essentielles, sans paiement ni messagerie temps réel)

---

## 1. Contexte & Objectifs

### 1.1 Contexte
Le POC vise à mettre en pratique :
- Des **règles métier** non triviales (capacité, transitions de statut, droits).
- Un workflow complet **passager ↔ conducteur** autour d'une demande de réservation.
- Une authentification réelle (niveau 3) avec gestion de sessions et tokens.

### 1.2 Objectifs fonctionnels
- Permettre à un conducteur de publier et gérer un trajet.
- Permettre à un passager de rechercher un trajet et demander une réservation.
- Permettre au conducteur d'accepter/refuser la demande.
- Permettre l'annulation (passager et/ou conducteur selon règles).
- Assurer la cohérence des places (jamais négatif, concurrence gérée).
- Fournir une expérience front minimaliste multi-plateforme (Expo).

### 1.3 Objectifs de démonstration (POC)
- Règles métier codées côté backend (pas seulement "dans le front").
- Gestion de concurrence (dernière place) et réponses d'erreur cohérentes.
- Découpage métier "propre" par modules.
- Traçabilité minimale (logs) + documentation API.

---

## 2. Acteurs, rôles & droits

### 2.1 Acteurs
- **Utilisateur** : personne possédant un compte (authentifiée).
- **Conducteur** : rôle contextuel (publie un trajet).
- **Passager** : rôle contextuel (réserve une place).

> Un utilisateur peut être conducteur sur un trajet et passager sur un autre.

### 2.2 Droits (règles)

**Tout utilisateur authentifié peut :**
- Rechercher des trajets
- Consulter un trajet
- Demander une réservation sur un trajet
- Voir ses demandes/réservations
- Annuler ses demandes/réservations (selon statut)

**Un conducteur (propriétaire d'un trajet) peut :**
- Voir les demandes reçues sur ses trajets
- Accepter/refuser une demande
- Annuler son trajet

---

## 3. Périmètre fonctionnel

### 3.1 Fonctionnalités incluses

#### A) Authentification & compte
- Inscription (email + mot de passe + nom affiché + numéro de téléphone)
- Connexion (email + mot de passe)
- Déconnexion (session/device courant)
- Déconnexion "tous les devices"
- Affichage du profil "me" (inclut le numéro de téléphone)

#### B) Trajets (Conducteur)
- Publier un trajet
- Lister ses trajets
- Consulter un trajet (détails + demandes)
- Annuler un trajet

#### C) Recherche & consultation (Passager)
- Rechercher des trajets (départ, arrivée, date)
- Consulter les détails d'un trajet

#### D) Réservations (Passager ↔ Conducteur)
- Demander une réservation (1 place par demande)
- Lister ses demandes/réservations
- Lister les demandes reçues (conducteur)
- Accepter une demande (consomme 1 place)
- Refuser une demande
- Annuler une demande/réservation (libère 1 place si acceptée)

#### E) Notifications (POC)
- Notification "mock" (log / inbox optionnelle) sur :
  - Demande créée
  - Demande acceptée/refusée
  - Réservation annulée
  - Trajet annulé

### 3.2 Hors périmètre (non inclus)
- Paiement
- Chat / messagerie
- Notation/avis
- Géolocalisation + carte
- Multi-places par réservation
- Matching avancé (détours, étapes)

---

## 4. Modèle métier & statuts

### 4.1 Entités métier principales
- **Trajet (Ride)** : offre publiée par un conducteur
- **Demande de réservation (BookingRequest)** : intention du passager, décidée par le conducteur

### 4.2 Statuts

#### Trajet (`RideStatus`)

| Statut | Description |
|--------|-------------|
| `SCHEDULED` | À venir / réservable (si date future) |
| `CANCELED` | Annulé, non réservable |
| `COMPLETED` | (Optionnel) Dérivé si date passée |

#### Demande (`BookingStatus`)

| Statut | Description |
|--------|-------------|
| `REQUESTED` | Demandée, en attente |
| `ACCEPTED` | Acceptée (place consommée) |
| `REJECTED` | Refusée (terminal) |
| `CANCELED` | Annulée (terminal) |

---

## 5. Règles métier détaillées

### 5.1 Règles Trajet
1. Un trajet doit avoir : Départ, Arrivée, Date/heure, Capacité > 0.
2. Un trajet passé (date < maintenant) n'est plus réservable.
3. Un trajet annulé n'est plus réservable.
4. Seul le conducteur peut annuler son trajet.

### 5.2 Règles Demande / Réservation
1. Un passager ne peut pas demander une réservation sur son propre trajet.
2. Une demande ne peut être acceptée que si :
   - statut = `REQUESTED`
   - trajet réservable (non annulé, date future)
   - au moins 1 place disponible
3. Refus possible uniquement si statut = `REQUESTED`.
4. Annulation possible si :
   - passager : `REQUESTED` ou `ACCEPTED`
   - conducteur : (POC) autorisé au moins sur `ACCEPTED` (et optionnel sur `REQUESTED`)
5. Si une réservation `ACCEPTED` est annulée => **libère 1 place**.
6. Une demande `REJECTED` ne peut plus évoluer (POC).

### 5.3 Gestion des places (invariant)
- `availableSeats` ne doit **jamais** être négatif.
- Acceptation => `availableSeats -= 1`
- Annulation d'une acceptation => `availableSeats += 1`

### 5.4 Concurrence (cas réel)
- Si deux acceptations simultanées sur la dernière place :
  - une seule réussit,
  - l'autre doit échouer avec une erreur explicite.

### 5.5 Règles Compte / Inscription
1. L'email doit respecter un format valide (regex côté front et back). Pas de vérification par lien.
2. Le numéro de téléphone est **obligatoire** à l'inscription.
3. L'utilisateur sélectionne un indicatif pays, puis saisit le numéro local dans le format attendu pour cet indicatif.
4. Le format du numéro est validé côté front (retour immédiat) et côté back (rejet si invalide).

---

## 6. Parcours utilisateurs (User Journeys)

### 6.1 Parcours "Conducteur"
1. Se connecter
2. Publier un trajet
3. Voir ses trajets
4. Ouvrir le détail d'un trajet
5. Consulter les demandes
6. Accepter/refuser
7. (Option) Annuler le trajet

### 6.2 Parcours "Passager"
1. Se connecter
2. Rechercher un trajet
3. Ouvrir le détail
4. Demander une réservation
5. Suivre le statut dans "Mes réservations"
6. Annuler si besoin

### 6.3 Parcours "Token expiré"
1. L'utilisateur navigue
2. Access token expire
3. L'application refresh automatiquement
4. Si refresh échoue => retour login

---

## 7. Exigences UX (écrans minimum)

### 7.1 Écrans Auth
- Inscription — formulaire : email, mot de passe, nom affiché, sélecteur d'indicatif pays + champ numéro (format adapté à l'indicatif)
- Connexion
- Profil (me) — affiche email, nom, numéro de téléphone
- Déconnexion

### 7.2 Écrans Passager
- Recherche trajets (formulaire + liste résultats)
- Détail trajet
- Mes demandes/réservations

### 7.3 Écrans Conducteur
- Publier trajet
- Mes trajets
- Détail trajet + demandes reçues
- Liste globale des demandes reçues (option)

---

## 8. Exigences non fonctionnelles

### 8.1 Qualité / robustesse
- Validation des entrées
- Messages d'erreur standardisés
- Logs minimaux (corrélation requête)
- Documentation API

### 8.2 Sécurité (niveau 3)
- Mot de passe hashé (jamais stocké en clair)
- Access token court, refresh token long
- Refresh token rotatif
- Logout (révocation)

### 8.3 Tests
- Tests unitaires sur règles métier
- Tests d'intégration sur un scénario complet
- Tests "module boundary" (structure) du monolithe modulaire
