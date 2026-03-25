# Roadmap — Kafka & Event-Driven sur covoit-backend

Objectif : monter en compétence sur Kafka, la fiabilité des messages, la résilience et la montée en charge,
à travers des cas concrets sur ce projet.

---

## Vue d'ensemble

```
Niveau 1 — Kafka basics          → remplacer Spring Modulith events par Kafka
Niveau 2 — Fiabilité             → ne jamais perdre un message
Niveau 3 — Résilience            → gérer pannes et services down
Niveau 4 — Montée en charge      → scaler les consumers
Niveau 5 — Patterns avancés      → cohérence distribuée
```

---

## Niveau 1 — Kafka basics

**Objectif :** remplacer les events in-process de Spring Modulith par des topics Kafka.

### Ce qu'on implémente

- Un `KafkaProducer` dans `rides` et `bookings` qui publie les events existants
- Un `KafkaConsumer` dans `notifications` qui les consomme
- Topics : `ride.events`, `booking.events`

### Ce qu'on apprend

- Producers / consumers / topics / partitions / offsets
- `@KafkaListener` + `KafkaTemplate` avec Spring Kafka
- Sérialisation JSON des events (Jackson)
- La différence entre **at-most-once**, **at-least-once**, **exactly-once**

### Question clé

> Que se passe-t-il si le consumer `notifications` est down au moment où un `BookingAcceptedEvent`
> est publié ? Est-ce que le message est perdu ?

### Setup Docker Compose

```yaml
# À ajouter dans compose.yaml
kafka:
  image: confluentinc/cp-kafka:7.6.0
  ports:
    - "9092:9092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

zookeeper:
  image: confluentinc/cp-zookeeper:7.6.0
  ports:
    - "2181:2181"
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181

kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8090:8080"
  environment:
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

### Dépendance Maven

```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## Niveau 2 — Fiabilité : ne jamais perdre un message

### 2a — Outbox Pattern

**Problème :** si le service crashe après avoir sauvegardé en base mais avant de publier sur Kafka,
le message est perdu définitivement.

**Solution :** écrire l'event dans une table `outbox` dans la **même transaction** que l'entité métier.
Un job indépendant lit la table et publie sur Kafka.

```
Transaction atomique :
  INSERT booking_requests ...
  INSERT outbox (event_type, payload, status='PENDING')

Outbox Relay (job) :
  SELECT * FROM outbox WHERE status='PENDING'
  → publish to Kafka
  → UPDATE outbox SET status='SENT'
```

**Schéma Flyway :**

```sql
CREATE TABLE outbox (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type  VARCHAR(100) NOT NULL,
  payload     JSONB        NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  created_at  TIMESTAMP    NOT NULL DEFAULT now(),
  sent_at     TIMESTAMP
);
```

**Ce qu'on apprend :** cohérence entre base de données et broker — le problème du double write.

---

### 2b — Dead Letter Queue (DLQ)

**Problème :** le consumer échoue à traiter un message (bug, service tiers down).
Sans DLQ, le message bloque le consumer ou est perdu.

**Solution :** après N tentatives, envoyer le message dans un topic dédié pour traitement différé ou manuel.

**Topologie des topics :**

```
booking.events            → topic principal
booking.events.retry-1    → retry après 10s
booking.events.retry-2    → retry après 60s
booking.events.dlq        → messages définitivement échoués (alerte + traitement manuel)
```

**Configuration Spring Kafka :**

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<?, ?> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template,
        (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition()));

    var backoff = new ExponentialBackOffWithMaxRetries(3);
    backoff.setInitialInterval(1_000);
    backoff.setMultiplier(2.0);

    return new DefaultErrorHandler(recoverer, backoff);
}
```

**Ce qu'on apprend :** retry strategies, poison pills, monitoring et alertes sur les DLQ.

---

### 2c — Idempotent Consumer

**Problème :** at-least-once delivery signifie qu'un message peut arriver **plusieurs fois**
(network retry, consumer rebalance). Sans protection, on envoie deux fois la même notification.

**Solution :** chaque event porte un `eventId` unique (UUID). Le consumer vérifie en base
s'il a déjà traité cet ID avant d'agir.

**Schéma Flyway :**

```sql
CREATE TABLE processed_events (
  event_id     UUID PRIMARY KEY,
  event_type   VARCHAR(100) NOT NULL,
  processed_at TIMESTAMP    NOT NULL DEFAULT now()
);
```

**Logique consumer :**

```java
@KafkaListener(topics = "booking.events")
public void handle(BookingEvent event) {
    if (processedEventRepository.existsById(event.eventId())) {
        return; // déjà traité, on ignore
    }
    notificationService.send(event);
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
}
```

**Ce qu'on apprend :** idempotence, at-least-once vs exactly-once, déduplication.

---

## Niveau 3 — Résilience : gérer les pannes

### 3a — Retry avec exponential backoff

**Comportement cible :**

```
Tentative 1 → échec → attendre 1s
Tentative 2 → échec → attendre 2s
Tentative 3 → échec → attendre 4s
→ envoi en DLQ
```

**Ce qu'on apprend :** distinguer les erreurs **transitoires** (réseau, timeout) des erreurs
**permanentes** (bug de désérialisation, données invalides) — les secondes ne doivent pas être retentées.

```java
var errorHandler = new DefaultErrorHandler(recoverer, backoff);
// Ne pas retenter les erreurs de désérialisation
errorHandler.addNotRetryableExceptions(JsonParseException.class, ClassCastException.class);
```

---

### 3b — Circuit Breaker sur les consumers

**Problème :** le service email/FCM est down → chaque message échoue → les retries s'accumulent
→ surcharge du broker et du service.

**Solution :** Resilience4j circuit breaker sur l'appel sortant.

```
État CLOSED    → appels normaux
État OPEN      → fail fast pendant 30s (pas d'appel au service)
État HALF-OPEN → 1 appel test, si OK → retour à CLOSED
```

**Dépendance Maven :**

```xml
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

**Ce qu'on apprend :** fail fast vs retry, protection des services downstream,
différence circuit breaker / retry.

---

### 3c — Consumer Lag Monitoring

**Problème :** le consumer est trop lent → les messages s'accumulent dans le topic
→ les notifications arrivent avec des minutes de retard.

**Métrique clé :** `consumer lag` = dernier offset produit − dernier offset commité par le consumer

**Outils :**
- **Kafka UI** (déjà dans le docker-compose ci-dessus) → visualisation en temps réel
- **Actuator + Micrometer** → exposition des métriques Kafka à Prometheus/Grafana

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

**Ce qu'on apprend :** offsets, consumer groups, détection de bottlenecks.

---

## Niveau 4 — Montée en charge

### 4a — Partitionnement et consumer groups

**Scénario :** le topic `booking.events` reçoit un fort volume, un seul consumer ne suit plus.

**Solution :**
1. Augmenter le nombre de partitions du topic (ex: 6)
2. Déployer plusieurs instances du service `notifications` dans le **même consumer group**
3. Kafka distribue les partitions automatiquement entre les instances

```
Topic booking.events (6 partitions)
│
├── Partition 0, 1 → instance notifications-1
├── Partition 2, 3 → instance notifications-2
└── Partition 4, 5 → instance notifications-3
```

**Règle fondamentale :** `nb_consumers_actifs ≤ nb_partitions`
Un consumer supplémentaire au-delà du nombre de partitions restera inactif.

**Ce qu'on apprend :** horizontal scaling, rebalancing, limites du modèle partition/consumer.

---

### 4b — Partition Key Strategy

**Problème :** les events d'un même trajet doivent être traités **dans l'ordre** :
`BookingRequested` → `BookingAccepted` → `BookingCanceled`

Si ces events vont dans des partitions différentes, l'ordre n'est pas garanti.

**Solution :** utiliser `rideId` comme clé de partition → tous les events d'un même trajet
vont dans la même partition → ordre garanti pour ce trajet.

```java
kafkaTemplate.send(
    MessageBuilder.withPayload(event)
        .setHeader(KafkaHeaders.TOPIC, "booking.events")
        .setHeader(KafkaHeaders.KEY, event.rideId().toString()) // clé de partition
        .build()
);
```

**Ce qu'on apprend :** ordering garanti dans une partition, compromis entre ordre et parallélisme,
hot partition si les clés sont mal distribuées.

---

## Niveau 5 — Patterns avancés

### 5a — Saga Pattern (Choreography)

**Use case :** réserver une place est une opération cross-modules :

```
[bookings] BookingRequested publié
     ↓
[rides]    RideCapacityReservationRequested reçu → tente de réserver le siège
     ↓ (succès)
[rides]    SeatReservedEvent publié
     ↓
[bookings] BookingAccepted
     ↓
[notifications] NotifyDriver

[rides] (échec, plus de place) → SeatReservationFailedEvent
     ↓
[bookings] BookingRejected
     ↓
[notifications] NotifyPassenger
```

Chaque module réagit aux events des autres et publie des events de compensation en cas d'échec.
Pas de transaction distribuée, pas de couplage direct entre modules.

**Ce qu'on apprend :** cohérence éventuelle, transactions distribuées sans 2PC,
events de compensation, choreography vs orchestration.

---

### 5b — Event Sourcing (bonus)

**Idée :** au lieu de stocker l'état courant d'une `BookingRequest`, stocker
**tous les events** qui ont mené à cet état et reconstruire l'état par replay.

```
Stocké en base :
  BookingRequested  { rideId, passengerId, at: T1 }
  BookingAccepted   { decidedAt: T2 }
  BookingCanceled   { canceledAt: T3, wasAccepted: true }

État reconstruit :
  bookingRequest.apply(BookingRequested)
  bookingRequest.apply(BookingAccepted)
  bookingRequest.apply(BookingCanceled)
  → status = CANCELED, wasAccepted = true
```

**Ce qu'on apprend :** event sourcing, CQRS, audit log natif, time travel debugging,
différence avec event-driven classique.

---

## Sprints suggérés

| Sprint | Objectif | Pattern appris |
|--------|----------|----------------|
| 1 | Setup Kafka + Docker, publier `BookingRequestedEvent` sur Kafka | Basics producer/consumer |
| 2 | Outbox pattern sur le module `bookings` | Double write, cohérence transactionnelle |
| 3 | DLQ + retry sur le consumer `notifications` | Retry strategy, poison pills |
| 4 | Idempotent consumer (table `processed_events`) | At-least-once, déduplication |
| 5 | Partitionnement par `rideId` + 3 instances `notifications` | Scaling, ordering |
| 6 | Circuit breaker Resilience4j sur l'envoi d'email | Résilience, fail fast |
| 7 | Saga pour la réservation de siège | Cohérence éventuelle, compensation |

---

## Ressources

- [Spring Kafka docs](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Confluent — Kafka patterns](https://developer.confluent.io/patterns/)
- [Resilience4j docs](https://resilience4j.readme.io/docs/getting-started)
- Livre : *Designing Event-Driven Systems* — Ben Stopford (gratuit chez Confluent)
- Livre : *Building Microservices* — Sam Newman (chapitres sur les sagas)
