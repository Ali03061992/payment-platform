# Architecture

## Vue globale

```
                         ┌─────────────────┐
                         │  Angular 16     │  (nginx, port 8080)
                         └────────┬────────┘
                                  │ HTTPS/REST + SSE (notifications)
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │  Spring Cloud Gateway (WebMVC)
                         │  JWT + RBAC + CORS + rate-limit + correlation-id
                         └────────┬────────┘
             ┌────────────────────┼────────────────────┐
             ▼                    ▼                    ▼
      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
      │  Identity   │      │  Payment    │      │ Notification│
      │  Service    │      │  Service    │      │  Service    │
      └──────┬──────┘      └──────┬──────┘      └──────┬──────┘
             ▼                    ▼                    ▼
         identity_db          payment_db          notification_db

      ┌─────────────┐                ┌──────────────┐
      │ Organization│                │   RabbitMQ   │
      │   Service   │                │  (events)    │
      └──────┬──────┘                └──────────────┘
             ▼
         organization_db
```

## Principes

1. **Une base MySQL par microservice** (`identity_db`, `organization_db`, `payment_db`, `notification_db`). Aucun service n'accède aux tables d'un autre service.
2. **Communication synchrone** : uniquement REST via l'API Gateway (frontend → services). Un service peut appeler un autre service en REST **uniquement via le Gateway** (ex. Payment → Organization pour valider la relation boutique/fournisseur).
3. **Communication asynchrone** : RabbitMQ via **Domain Events** publiés depuis un **Outbox** transactionnel (voir [events.md](events.md)).
4. **Pas de transaction distribuée** : transactions locales + événements + idempotence.

## Décisions d'architecture (ADR)

| ADR | Décision | Justification |
|---|---|---|
| ADR-01 | Spring Cloud Gateway (variante **WebMVC**) | Stack synchrone classique (Spring Web MVC) ; starter `spring-cloud-starter-gateway-server-webmvc` du train 2025.1.x |
| ADR-02 | Pas de Service Discovery / Config Server | 4 services + gateway : le routing statique par variables d'environnement (Docker Compose) suffit ; réduction de la complexité opérationnelle. Réintroduisible si le nombre de services croît |
| ADR-03 | RabbitMQ plutôt que Kafka | Volume modeste, besoin de files simples, intégration Spring AMQP mature |
| ADR-04 | Outbox Pattern transactionnel (polling) | Garantit « paiement persisté ⇒ événement publié » sans XA ; relais par `@Scheduled` toutes les secondes, idempotent |
| ADR-05 | SSE plutôt que WebSocket | Unidirectionnel (serveur→client), suffisant pour les notifications, compatible HTTP/2 et proxies ; WebSocket gardé comme extension possible |
| ADR-06 | JWT signé HS256, clé partagée via variables d'environnement | Simple et adapté à un réseau interne ; passage RS256/Keycloak documenté comme évolution |
| ADR-07 | Chaque service re-valide le JWT et l'état du compte | Le Gateway n'est pas une frontière de confiance : un service ne fait jamais confiance aux headers entrants |

## Ports et composants Docker Compose

| Service | Port interne | Port exposé | Base |
|---|---|---|---|
| api-gateway | 8081 | 8081 | — |
| identity-service | 8082 | — | identity_db |
| organization-service | 8083 | — | organization_db |
| payment-service | 8084 | — | payment_db |
| notification-service | 8085 | — | notification_db |
| mysql | 3306 | 3307 | 4 bases |
| rabbitmq | 5672/15672 | 5673/15673 | — |
| angular (nginx) | 80 | 8080 | — |

## Dépendances inter-services

```
Payment ──REST via Gateway──▶ Organization   (validation relation + statuts)
Organization ──Event──▶ Identity           (cascade désactivation via SupplierDisabledEvent/ShopDisabledEvent)
Payment ──Event──▶ Notification            (PaymentCreated/Confirmed/Rejected/CancelledEvent)
Organization ──Event──▶ Notification       (Supplier/Shop activations & désactivations)
Identity ──Event──▶ Notification           (UserCreated/Disabled)
```