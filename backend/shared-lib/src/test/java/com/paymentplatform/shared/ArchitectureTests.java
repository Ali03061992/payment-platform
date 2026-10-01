package com.paymentplatform.shared;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fous d'architecture du kernel partagé.
 * Périmètre : code principal de shared-lib (hors tests).
 * Moyens : ArchUnit sur les classes compilées, assertions AssertJ.
 */
@DisplayName("Architecture du kernel partagé : règles structurelles respectées.")
class ArchitectureTests {

    private static final JavaClasses PRINCIPAL = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.paymentplatform.shared");

    @Test
    @DisplayName("Les exceptions métier sont unchecked (RuntimeException).")
    void exceptionsMetier_sontUnchecked() {
        ArchRule regle = classes()
                .that().resideInAPackage("..domain.exception..")
                .should().beAssignableTo(RuntimeException.class)
                .because("les exceptions métier sont unchecked avec messages explicites");

        regle.check(PRINCIPAL);
        assertThat(PRINCIPAL.containPackage("com.paymentplatform.shared.domain.exception")).isTrue();
    }

    @Test
    @DisplayName("Les services applicatifs ne dépendent pas de Spring Web.")
    void services_neDependentPasDeSpringWeb() {
        ArchRule regle = noClasses()
                .that().areAnnotatedWith(Service.class)
                .should().dependOnClassesThat().resideInAPackage("org.springframework.web..")
                .because("les @Service contiennent la logique sans dépendre de Spring Web")
                .allowEmptyShould(true);

        regle.check(PRINCIPAL);
    }

    @Test
    @DisplayName("Le kernel partagé ne dépend d'aucun microservice métier.")
    void kernel_neDependDAucunServiceMetier() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("com.paymentplatform.shared..")
                .should().dependOnClassesThat().resideInAPackage("com.paymentplatform.identity..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.organization..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.payment..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.notification..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.gateway..")
                .because("common est l'infrastructure partagée avec zéro dépendance métier");

        regle.check(PRINCIPAL);
    }
}
