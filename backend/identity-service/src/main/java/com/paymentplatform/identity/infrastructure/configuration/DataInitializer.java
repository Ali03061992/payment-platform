package com.paymentplatform.identity.infrastructure.configuration;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed one-shot du compte SYSTEM_ADMIN. Désactivé par défaut et exige un mot
 * de passe fourni explicitement via SEED_ADMIN_PASSWORD.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public DataInitializer(UserRepository users, PasswordEncoder passwordEncoder,
                           @Value("${app.seed.admin-username:system.admin}") String username,
                           @Value("${app.seed.admin-email:system.admin@payment-platform.local}") String email,
                           @Value("${app.seed.admin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("SEED_ADMIN_PASSWORD est requis lorsque SEED_ADMIN_ENABLED=true");
        }
        if (!users.existsByUsername(Username.of(username))) {
            User admin = User.create(new UserId(null), Username.of(username),
                    Email.of(email),
                    PasswordHash.of(passwordEncoder.encode(password)), "System", "Admin", new PhoneNumber(null),
                    (OrganizationId) null, RoleCode.SYSTEM_ADMIN);
            users.save(admin);
            log.info("Compte SYSTEM_ADMIN créé : {}", username);
        }
    }
}