package com.paymentplatform.identity.infrastructure.configuration;

import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seed du compte SYSTEM_ADMIN de démonstration (username system.admin). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = System.getenv().getOrDefault("SEED_ADMIN_USERNAME", "system.admin");
        String password = System.getenv().getOrDefault("SEED_ADMIN_PASSWORD", "Admin@123");
        if (users.existsByUsername(Username.of(username))) {
            return;
        }
        User admin = User.create(new UserId(0), Username.of(username),
                Email.of(System.getenv().getOrDefault("SEED_ADMIN_EMAIL", "system.admin@payment-platform.local")),
                PasswordHash.of(passwordEncoder.encode(password)), "System", "Admin", new PhoneNumber(null),
                (OrganizationId) null, RoleCode.SYSTEM_ADMIN);
        users.save(admin);
        log.info("Compte SYSTEM_ADMIN créé : {}", username);
    }
}