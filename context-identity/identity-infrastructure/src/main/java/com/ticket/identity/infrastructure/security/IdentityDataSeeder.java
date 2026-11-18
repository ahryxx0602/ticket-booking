package com.ticket.identity.infrastructure.security;

import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class IdentityDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityDataSeeder.class);

    private final SpringDataUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.seed.admin-password:admin_dev_password}")
    private String adminPassword;

    @org.springframework.beans.factory.annotation.Value("${app.seed.customer-password:customer_dev_password}")
    private String customerPassword;

    public IdentityDataSeeder(SpringDataUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedCustomerUser();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            UserJpaEntity admin = new UserJpaEntity();
            admin.setUsername("admin");
            admin.setEmail("admin@ticketbooking.com");
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFullName("System Administrator");
            admin.setRole("ROLE_ADMIN");
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info("[IDENTITY SEEDER] Seeded default admin user");
        }
    }

    private void seedCustomerUser() {
        if (!userRepository.existsByUsername("customer1")) {
            UserJpaEntity customer = new UserJpaEntity();
            customer.setUsername("customer1");
            customer.setEmail("customer1@ticketbooking.com");
            customer.setPasswordHash(passwordEncoder.encode(customerPassword));
            customer.setFullName("Nguyen Van Customer");
            customer.setRole("ROLE_CUSTOMER");
            customer.setStatus(UserStatus.ACTIVE);
            userRepository.save(customer);
            log.info("[IDENTITY SEEDER] Seeded default customer user");
        }
    }
}
