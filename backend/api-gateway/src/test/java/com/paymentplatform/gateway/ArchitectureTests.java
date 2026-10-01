package com.paymentplatform.gateway;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous d'architecture de la passerelle.
 * Périmètre : code principal (hors tests).
 * Moyens : ArchUnit sur les classes compilées.
 */
@DisplayName("Architecture passerelle : isolation des services métier respectée.")
class ArchitectureTests {

    private static final JavaClasses PRINCIPAL = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.paymentplatform.gateway");

    @Test
    @DisplayName("La passerelle ne dépend d'aucun service métier en Java.")
    void passerelle_neDependPasDesServicesMetier() {
        noClasses().that().resideInAPackage("com.paymentplatform.gateway..")
                .should().dependOnClassesThat().resideInAPackage("com.paymentplatform.identity..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.organization..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.payment..")
                .andShould().dependOnClassesThat().resideInAPackage("com.paymentplatform.notification..")
                .because("la passerelle route en HTTP sans dépendance Java vers les services")
                .check(PRINCIPAL);
    }
}
