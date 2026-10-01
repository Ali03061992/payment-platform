package com.paymentplatform.notification;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fous d'architecture du service notification.
 * Périmètre : code principal (hors tests).
 * Moyens : ArchUnit sur les classes compilées.
 */
@DisplayName("Architecture notification : couches et records respectés.")
class ArchitectureTests {

    private static final JavaClasses PRINCIPAL = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.paymentplatform.notification");

    @Test
    @DisplayName("Les réponses applicatives sont des records Java.")
    void dto_sontDesRecords() {
        JavaClasses dto = PRINCIPAL.that(new com.tngtech.archunit.base.DescribedPredicate<>(
                "réponses applicatives") {
            @Override
            public boolean test(com.tngtech.archunit.core.domain.JavaClass input) {
                return input.getPackageName().contains("application.dto")
                        && input.getSimpleName().endsWith("Response");
            }
        });
        assertThat(dto).isNotEmpty();
        classes().that(new com.tngtech.archunit.base.DescribedPredicate<>(
                "réponses applicatives") {
            @Override
            public boolean test(com.tngtech.archunit.core.domain.JavaClass input) {
                return input.getPackageName().contains("application.dto")
                        && input.getSimpleName().endsWith("Response");
            }
        }).should().beRecords().check(PRINCIPAL);
    }

    @Test
    @DisplayName("Les services applicatifs ne dépendent pas de Spring Web.")
    void services_neDependentPasDeSpringWeb() {
        noClasses().that().areAnnotatedWith(Service.class)
                .should().dependOnClassesThat().resideInAPackage("org.springframework.web..")
                .because("les @Service portent la logique sans Spring Web")
                .check(PRINCIPAL);
    }

    @Test
    @DisplayName("Le service notification ne dépend d'aucun autre service métier.")
    void service_neDependPasDesAutresServices() {
        noClasses().that().resideInAPackage("com.paymentplatform.notification..")
                .should().dependOnClassesThat().resideInAPackage("com.paymentplatform.identity..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.organization..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.payment..")
                .because("les bounded contexts ne se connaissent pas en Java, uniquement via HTTP/AMQP")
                .check(PRINCIPAL);
    }
}
