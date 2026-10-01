package com.paymentplatform.identity.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.event.OrganizationEvents;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de OrganizationEventConsumer.
 * Périmètre : routage des événements de désactivation vers la cascade identité.
 * Moyens : contexte SpringBootTest, base H2 réelle avec utilisateurs persistés (zéro mock).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Consommateur des événements organisation : cascade réelle contre H2 sans mock.")
class OrganizationEventConsumerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Autowired
    private OrganizationEventConsumer consumer;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void preparerUtilisateurs() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            entityManager.createNativeQuery("DELETE FROM user_roles").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM processed_events").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM outbox_events").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM audit_logs").executeUpdate();
        });

        users.save(User.create(new UserId(null), Username.of("fournisseur.admin"),
                Email.of("fournisseur.admin@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Admin", "Fournisseur", new PhoneNumber(null),
                OrganizationId.of(ORGANISATION_ID), RoleCode.SUPPLIER_ADMIN));
        users.save(User.create(new UserId(null), Username.of("fournisseur.agent"),
                Email.of("fournisseur.agent@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Agent", "Fournisseur", new PhoneNumber(null),
                OrganizationId.of(ORGANISATION_ID), RoleCode.SUPPLIER_AGENT));
        users.save(User.create(new UserId(null), Username.of("boutique.agent"),
                Email.of("boutique.agent@x.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Agent", "Boutique", new PhoneNumber(null),
                OrganizationId.of(ORGANISATION_ID), RoleCode.SHOP_AGENT));

        assertThat(objectMapper).isNotNull();
    }

    private List<User> membresOrganisation() {
        return users.findByOrganizationId(OrganizationId.of(ORGANISATION_ID));
    }

    @Test
    @DisplayName("Un événement fournisseur désactivé désactive les comptes fournisseur uniquement.")
    void onOrganizationEvent_fournisseurDesactive_desactiveComptesFournisseur() {
        String eventId = "evt-fournisseur-" + System.nanoTime();
        String payload = """
                {"eventType":"%s","eventId":"%s","organizationId":"%s"}
                """.formatted(OrganizationEvents.SupplierDisabledEvent.EVENT_TYPE, eventId, ORGANISATION_ID);

        consumer.onOrganizationEvent(payload);

        List<User> membres = membresOrganisation();
        assertThat(membres).hasSize(3);
        assertThat(membres.stream()
                .filter(u -> u.roles().contains(RoleCode.SUPPLIER_ADMIN)
                        || u.roles().contains(RoleCode.SUPPLIER_AGENT)))
                .allMatch(u -> u.status() == UserStatus.DISABLED);
        assertThat(membres.stream()
                .filter(u -> u.roles().contains(RoleCode.SHOP_AGENT)))
                .allMatch(u -> u.status() == UserStatus.ACTIVE);
        Integer lignes = jdbcClient.sql("SELECT COUNT(*) FROM processed_events WHERE event_id = :id")
                .param("id", eventId)
                .query(Integer.class)
                .single();
        assertThat(lignes).isEqualTo(1);
    }

    @Test
    @DisplayName("Un événement boutique désactivée désactive les comptes boutique uniquement.")
    void onOrganizationEvent_boutiqueDesactivee_desactiveComptesBoutique() {
        String eventId = "evt-boutique-" + System.nanoTime();
        String payload = """
                {"eventType":"%s","eventId":"%s","organizationId":"%s"}
                """.formatted(OrganizationEvents.ShopDisabledEvent.EVENT_TYPE, eventId, ORGANISATION_ID);

        consumer.onOrganizationEvent(payload);

        List<User> membres = membresOrganisation();
        assertThat(membres.stream()
                .filter(u -> u.roles().contains(RoleCode.SHOP_AGENT)))
                .allMatch(u -> u.status() == UserStatus.DISABLED);
        assertThat(membres.stream()
                .filter(u -> u.roles().contains(RoleCode.SUPPLIER_ADMIN)
                        || u.roles().contains(RoleCode.SUPPLIER_AGENT)))
                .allMatch(u -> u.status() == UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Un type d'événement inconnu ne modifie aucun compte.")
    void onOrganizationEvent_typeInconnu_neModifieRien() {
        String payload = """
                {"eventType":"organization.unknown","eventId":"evt-inconnu-%s","organizationId":"%s"}
                """.formatted(System.nanoTime(), ORGANISATION_ID);

        consumer.onOrganizationEvent(payload);

        assertThat(membresOrganisation()).allMatch(u -> u.status() == UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Un payload sans champs requis ne modifie aucun compte.")
    void onOrganizationEvent_champsManquants_neModifieRien() {
        consumer.onOrganizationEvent("""
                {"eventId":"evt-incomplet-%s"}
                """.formatted(System.nanoTime()));

        assertThat(membresOrganisation()).allMatch(u -> u.status() == UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Un payload non JSON lève une erreur d'état.")
    void onOrganizationEvent_payloadInvalide_leveErreurEtat() {
        assertThatThrownBy(() -> consumer.onOrganizationEvent("not json"))
                .isInstanceOf(IllegalStateException.class);
    }
}
