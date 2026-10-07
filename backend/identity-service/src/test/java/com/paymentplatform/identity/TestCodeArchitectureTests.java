package com.paymentplatform.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous du code de test du service identité.
 * Périmètre : classes de test uniquement.
 * Moyens : ArchUnit avec import restreint aux tests.
 */
@DisplayName("Code de test identité : zéro simulation, assertions AssertJ uniquement.")
class TestCodeArchitectureTests {

    private static final JavaClasses TESTS = new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages("com.paymentplatform.identity");

    @Test
    @DisplayName("Aucun test n'utilise les assertions JUnit directes.")
    void tests_utilisentAssertJUniquement() {
        noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.junit.jupiter.api.Assertions")
                .because("les assertions passent uniquement par AssertJ")
                .check(TESTS);
    }
}
