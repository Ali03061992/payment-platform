package com.paymentplatform.identity.infrastructure.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests de InternalAuthGuardTest.
 * Perimetre : filtre/securite InternalAuthGuard.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
class InternalAuthGuardTest {

    @Autowired
    private InternalAuthGuard guard;

    @Test
    void isValid_withCorrectToken_returnsTrue() {
        assertThat(guard.isValid("test-internal-secret")).isTrue();
    }

    @Test
    void isValid_withWrongToken_returnsFalse() {
        assertThat(guard.isValid("wrong-token")).isFalse();
    }

    @Test
    void isValid_withNullToken_returnsFalse() {
        assertThat(guard.isValid(null)).isFalse();
    }

    @Test
    void isValid_withEmptyToken_returnsFalse() {
        assertThat(guard.isValid("")).isFalse();
    }
}
