package com.paymentplatform.shared;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous du code de test du kernel partagé.
 * Périmètre : classes de test de shared-lib uniquement.
 * Moyens : ArchUnit avec import restreint aux tests, zéro framework de simulation.
 */
@DisplayName("Code de test du kernel : zéro simulation, assertions AssertJ uniquement.")
class TestCodeArchitectureTests {

    private static final JavaClasses TESTS = new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("com.paymentplatform.shared");

    @Test
    @DisplayName("Aucun test ne dépend de Mockito.")
    void tests_neDependentPasDeMockito() {
        ArchRule regle = noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.mockito..")
                .because("les tests s'exécutent contre H2 réelle, sans aucun mock");

        regle.check(TESTS);
    }

    @Test
    @DisplayName("Aucun test n'utilise les assertions JUnit directes.")
    void tests_utilisentAssertJUniquement() {
        ArchRule regle = noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.junit.jupiter.api.Assertions")
                .because("les assertions passent uniquement par AssertJ");

        regle.check(TESTS);
    }
}
