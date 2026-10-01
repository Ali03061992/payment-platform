package com.paymentplatform.identity;

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
 * Garde-fous d'architecture du service identité.
 * Périmètre : code principal (hors tests).
 * Moyens : ArchUnit sur les classes compilées.
 */
@DisplayName("Architecture identité : couches et records respectés.")
class ArchitectureTests {

    private static final JavaClasses PRINCIPAL = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.paymentplatform.identity");

    @Test
    @DisplayName("Les value objects du domaine sont des records Java.")
    void valueObjects_sontDesRecords() {
        ArchRule regle = classes()
                .that().resideInAPackage("..domain.valueobject..")
                .and().areNotEnums()
                .should().beRecords()
                .because("les value objects sont des records immuables");

        regle.check(PRINCIPAL);
    }

    @Test
    @DisplayName("Les requêtes et réponses applicatives sont des records Java.")
    void dto_sontDesRecords() {
        JavaClasses dto = PRINCIPAL.that(new com.tngtech.archunit.base.DescribedPredicate<>(
                "requêtes, réponses et résumés") {
            @Override
            public boolean test(com.tngtech.archunit.core.domain.JavaClass input) {
                String nom = input.getSimpleName();
                return input.getPackageName().contains("application.dto")
                        && (nom.endsWith("Request") || nom.endsWith("Response") || nom.endsWith("Summary"));
            }
        });
        assertThat(dto).isNotEmpty();
        classes().that(new com.tngtech.archunit.base.DescribedPredicate<>(
                "requêtes, réponses et résumés") {
            @Override
            public boolean test(com.tngtech.archunit.core.domain.JavaClass input) {
                String nom = input.getSimpleName();
                return input.getPackageName().contains("application.dto")
                        && (nom.endsWith("Request") || nom.endsWith("Response") || nom.endsWith("Summary"));
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
    @DisplayName("Le service identité ne dépend d'aucun autre service métier.")
    void service_neDependPasDesAutresServices() {
        noClasses().that().resideInAPackage("com.paymentplatform.identity..")
                .should().dependOnClassesThat().resideInAPackage("com.paymentplatform.organization..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.payment..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.notification..")
                .because("les bounded contexts ne se connaissent pas en Java, uniquement via HTTP/AMQP")
                .check(PRINCIPAL);
    }
}
