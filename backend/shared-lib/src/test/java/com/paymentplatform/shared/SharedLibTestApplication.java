package com.paymentplatform.shared;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
/**
 * Tests de SharedLibTestApplication.
 * Perimetre : classe d'aide pour les tests (configuration/stub, pas de test direct).
 * Moyens : JUnit pur (AssertJ).
 */

@SpringBootApplication
@ComponentScan(basePackages = "com.paymentplatform.shared")
public class SharedLibTestApplication {
}
