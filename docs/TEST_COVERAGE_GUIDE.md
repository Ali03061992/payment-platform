# TEST_COVERAGE_GUIDE.md

Guide de lancement des tests avec rapport de coverage (JaCoCo + Karma/Jasmine).

---

## 1. Backend — Java / Maven / JaCoCo

### Prérequis

| Outil | Version requise |
|-------|----------------|
| Java | 26+ |
| Maven | via `mvnw.cmd` (wrapper inclus) |
| JaCoCo | 0.8.15 (configuré dans le parent `pom.xml`) |

### Lancer tous les tests + générer les rapports

```powershell
cd backend
.\mvnw.cmd clean test jacoco:report
```

### Lancer les tests sans régénérer les rapports

```powershell
cd backend
.\mvnw.cmd clean test
```

### Générer uniquement les rapports (après un run de tests)

```powershell
cd backend
.\mvnw.cmd jacoco:report
```

### Rapports générés

| Service | Chemin du rapport HTML | Chemin du CSV |
|---------|----------------------|---------------|
| shared-lib | `backend/shared-lib/target/site/jacoco/index.html` | `jacoco.csv` |
| identity-service | `backend/identity-service/target/site/jacoco/index.html` | `jacoco.csv` |
| organization-service | `backend/organization-service/target/site/jacoco/index.html` | `jacoco.csv` |
| payment-service | `backend/payment-service/target/site/jacoco/index.html` | `jacoco.csv` |
| notification-service | `backend/notification-service/target/site/jacoco/index.html` | `jacoco.csv` |

### Rapport agrégé (tous services)

```powershell
cd backend
.\mvnw.cmd jacoco:report -Djacoco.skip=false
```

Le rapport agrégé se trouve dans le répertoire parent :
`backend/target/site/jacoco/jacoco.csv`

### Résultats de coverage actuels (252 tests, `domain` exclu du calcul JaCoCo)

> **Note:** La configuration JaCoCo exclut la couche `domain` (`domain/model`, `domain/valueobject`, `domain/event`, `domain/exception`, `domain/engine`, `domain/security`, `domain/repository`) — seules les couches `application`, `infrastructure` et `interfaces` sont mesurées pour l'objectif 95%.

| Service | Instructions (toutes couches) | Instructions (sans `domain`) | Lignes (sans `domain`) | Objectif |
|---------|-------------------------------|------------------------------|------------------------|----------|
| shared-lib | 17.18% | 19.25% | 16.73% | 95% |
| identity-service | 68.99% | 68.26% | 71.81% | 95% |
| organization-service | 33.19% | 53.44% | 61.47% | 95% |
| payment-service | 57.88% | 53.82% | 51.57% | 95% |
| notification-service | 20.12% | 16.33% | 25.22% | 95% |

Configuration d'exclusion dans `backend/pom.xml:92-126` :
```xml
<excludes>
  <exclude>com/paymentplatform/**/domain/model/**</exclude>
  <exclude>com/paymentplatform/**/domain/valueobject/**</exclude>
  <exclude>com/paymentplatform/**/domain/event/**</exclude>
  <exclude>com/paymentplatform/**/domain/exception/**</exclude>
  <exclude>com/paymentplatform/**/domain/engine/**</exclude>
  <exclude>com/paymentplatform/**/domain/security/**</exclude>
  <exclude>com/paymentplatform/**/domain/repository/**</exclude>
</excludes>
```

### Fichiers de test existants

```
shared-lib/
  src/test/java/.../JwtServiceTest.java
  src/test/java/.../AuditLogRepositoryTest.java
  src/test/java/.../OutboxEventRepositoryTest.java
  src/test/java/.../OutboxEventStoreTest.java
  src/test/java/.../EventDeduplicatorTest.java
  src/test/java/.../PermissionCatalogTest.java
  src/test/java/.../RoleCodeTest.java
  src/test/java/.../GlobalExceptionHandlerTest.java
  src/test/java/.../CorrelationIdFilterTest.java
  src/test/java/.../OutboxRelayTest.java
  src/test/java/.../AmqpTopologyTest.java
  src/test/java/.../JwtAuthenticationFilterTest.java
  src/test/java/.../SharedSecurityConfigTest.java
  src/test/java/.../JacksonConfigTest.java

identity-service/
  src/test/java/.../UserTest.java
  src/test/java/.../AuthUseCaseH2Test.java
  src/test/java/.../RegisterUseCaseH2Test.java
  src/test/java/.../PasswordSetupUseCaseH2Test.java
  src/test/java/.../AgentManagementUseCaseH2Test.java
  src/test/java/.../UserQueryUseCaseH2Test.java
  src/test/java/.../UserStatusUseCaseH2Test.java
  src/test/java/.../InternalUserCreationUseCaseH2Test.java
  src/test/java/.../OrganizationCascadeUseCaseH2Test.java
  src/test/java/.../TestOrganizationStatusPort.java
  src/test/java/.../TestMailConfiguration.java

organization-service/
  src/test/java/.../OrganizationTest.java
  src/test/java/.../CreateOrganizationUseCaseH2Test.java
  src/test/java/.../OrganizationQueryUseCaseH2Test.java
  src/test/java/.../OrganizationStatusUseCaseH2Test.java
  src/test/java/.../SupplierShopRelationUseCaseH2Test.java
  src/test/java/.../CreateOrderUseCaseH2Test.java
  src/test/java/.../ConfirmOrderUseCaseH2Test.java
  src/test/java/.../AcceptOrderUseCaseH2Test.java
  src/test/java/.../CancelOrderUseCaseH2Test.java
  src/test/java/.../RejectOrderUseCaseH2Test.java
  src/test/java/.../PrepareOrderUseCaseH2Test.java
  src/test/java/.../DeliverOrderUseCaseH2Test.java
  src/test/java/.../DeliveryRejectOrderUseCaseH2Test.java
  src/test/java/.../BalanceUseCaseH2Test.java
  src/test/java/.../StockServiceH2Test.java
  src/test/java/.../StockOptimizationServiceH2Test.java

payment-service/
  src/test/java/.../PaymentTest.java
  src/test/java/.../CreatePaymentUseCaseH2Test.java
  src/test/java/.../ConfirmPaymentUseCaseH2Test.java
  src/test/java/.../RejectPaymentUseCaseH2Test.java
  src/test/java/.../CancelPaymentUseCaseH2Test.java
  src/test/java/.../GetPaymentUseCaseH2Test.java
  src/test/java/.../ListPaymentsUseCaseH2Test.java
  src/test/java/.../SearchPaymentsUseCaseH2Test.java
  src/test/java/.../AgentPaymentsBySupplierUseCaseH2Test.java
  src/test/java/.../PaymentMapperTest.java
  src/test/java/.../CsvExportServiceTest.java

notification-service/
  src/test/java/.../NotificationTest.java
  src/test/java/.../NotificationAmqpConfigTest.java
```

---

## 2. Frontend — Angular / Karma / Jasmine

### Prérequis

| Outil | Version requise |
|-------|----------------|
| Node.js | 18+ |
| npm | 9+ |
| Angular CLI | 16.2 |

### Installer les dépendances

```powershell
cd payment-platform-ui
npm install
```

### Lancer tous les tests avec coverage

```powershell
cd payment-platform-ui
npx ng test --code-coverage --no-watch
```

### Lancer les tests en mode watch (développement)

```powershell
cd payment-platform-ui
npx ng test
```

### Lancer les tests E2E (Cypress)

```powershell
cd payment-platform-ui
npx cypress run
```

### Rapports générés

| Type | Chemin |
|------|--------|
| Coverage HTML | `payment-platform-ui/coverage/payment-platform-ui/index.html` |
| Coverage LCOV | `payment-platform-ui/coverage/payment-platform-ui/lcov.info` |
| Coverage JSON | `payment-platform-ui/coverage/coverage-summary.json` |

### Résultats de coverage actuels (798 tests)

| Métrique | Couverture |
|----------|-----------|
| Instructions | 97.38% |
| Branches | 89.27% |
| Fonctions | 97.84% |
| Lignes | 97.16% |
| **Objectif** | **85%** |

### Fichiers de test (.spec.ts)

```
src/app/
  app.component.spec.ts
  services/
    auth.service.spec.ts
    login.service.spec.ts
    user.service.spec.ts
    organization.service.spec.ts
    payment.service.spec.ts
    payment-search.service.spec.ts
    order.service.spec.ts
    stock.service.spec.ts
    stock-optimization.service.spec.ts
    catalog.service.spec.ts
    supplier-agent.service.spec.ts
    notification.service.spec.ts
    balance.service.spec.ts
    toast.service.spec.ts
  admin/
    supplier-management/supplier-management.component.spec.ts
    shop-management/shop-management.component.spec.ts
    relation-management/relation-management.component.spec.ts
    organization-stats/organization-stats.component.spec.ts
    user-management/user-management.component.spec.ts
    create-user/create-user.component.spec.ts
  supplier/
    stock-management/stock-management.component.spec.ts
    stock-dashboard/stock-dashboard.component.spec.ts
    stock-optimization/stock-optimization.component.spec.ts
    add-product/add-product.component.spec.ts
    product-management/product-management.component.spec.ts
    category-management/category-management.component.spec.ts
    family-management/family-management.component.spec.ts
    order-management/order-management.component.spec.ts
    create-order/create-order.component.spec.ts
    delivery-management/delivery-management.component.spec.ts
    agent-payments/agent-payments.component.spec.ts
  shop/
    order-list/order-list.component.spec.ts
    create-order/create-order.component.spec.ts
    order-detail/order-detail.component.spec.ts
    balance-view/balance-view.component.spec.ts
  payments/
    payment-list/payment-list.component.spec.ts
    create-payment/create-payment.component.spec.ts
    payment-detail/payment-detail.component.spec.ts
    payment-stats/payment-stats.component.spec.ts
    payment-search/payment-search.component.spec.ts
  login/login.component.spec.ts
  register/register.component.spec.ts
  password-setup/password-setup.component.spec.ts
  layout/layout.component.spec.ts
  dashboard/dashboard.component.spec.ts
  sales/account-activation/account-activation.component.spec.ts
  components/
    toast/toast.component.spec.ts
    notification-banner/notification-banner.component.spec.ts
  qr-scanner/qr-scanner.component.spec.ts
  pwa-update/pwa-update.component.spec.ts
  pipes/
    status-label.pipe.spec.ts
  supplier/stock-optimization/filter-by-risk.pipe.spec.ts
  core/
    auth.guard.spec.ts
    role.guard.spec.ts
    jwt.interceptor.spec.ts
```

---

## 3. Lancer tout en une commande

### Backend + Frontend

```powershell
# Backend
cd backend
.\mvnw.cmd clean test jacoco:report
cd ..

# Frontend
cd payment-platform-ui
npm install
npx ng test --code-coverage --no-watch
cd ..
```

---

## 4. Vérifier les seuils de coverage

### Backend — Seuil JaCoCo (optionnel, dans pom.xml)

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>check</id>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.95</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Frontend — Seuil Karma (dans angular.json)

```json
"test": {
  "options": {
    "codeCoverage": true,
    "codeCoverageExclude": [
      "src/**/environment*.ts",
      "src/**/polyfills.ts",
      "src/**/main.ts"
    ]
  }
}
```

---

## 5. Résolution de problèmes

| Problème | Solution |
|----------|----------|
| `Unsupported class file major version 70` | Utiliser JaCoCo ≥ 0.8.15 (déjà configuré) |
| Tests timeout | Augmenter le timeout Maven : `.\mvnw.cmd test -Dsurefire.timeout=300` |
| H2 `MODE=MySQL` incompatible | Vérifier `application.yml` dans `src/test/resources/` |
| Coverage 0% après run | Vérifier que `jacoco:report` tourne après `test` |
| Frontend `ng test` fail | Vérifier `npm install` et `ng version` |
| Cypress fail | Vérifier que le backend tourne sur le port 8081 |
