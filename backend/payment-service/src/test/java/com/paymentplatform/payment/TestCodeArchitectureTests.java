package com.paymentplatform.payment;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous du code de test du service paiement.
 * Périmètre : classes de test uniquement.
 * Moyens : ArchUnit avec import restreint aux tests.
 */
@DisplayName("Code de test paiement : zéro simulation, assertions AssertJ uniquement.")
class TestCodeArchitectureTests {

    private static final JavaClasses TESTS = new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("com.paymentplatform.payment");

    @Test
    @DisplayName("Aucun test ne dépend de Mockito.")
    void tests_neDependentPasDeMockito() {
        noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.mockito..")
                .because("les tests s'exécutent contre H2 réelle, sans aucun mock")
                .check(TESTS);
    }

    @Test
    @DisplayName("Aucun test n'utilise les assertions JUnit directes.")
    void tests_utilisentAssertJUniquement() {
        noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.junit.jupiter.api.Assertions")
                .because("les assertions passent uniquement par AssertJ")
                .check(TESTS);
    }
}
