package com.dozernet;

import com.dozernet.common.model.Role;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts (with correctly BCrypt-hashed passwords) so the app is
 * demonstrable immediately. Runs first (Order 1); module-specific seed data
 * runs afterwards. Idempotent: only seeds when the users table is empty.
 *
 * Demo password for every seeded account: "password123".
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Seeding demo users (password for all: {})", DEMO_PASSWORD);

        seed("Admin User", "admin@dozernet.lk", "0770000001", Role.ADMIN, true);
        seed("Chamara Perera", "customer@dozernet.lk", "0771111111", Role.CUSTOMER, true);
        seed("Nimal Fernando", "owner@dozernet.lk", "0772222222", Role.OWNER, true);
        seed("Sunil Bandara", "operator@dozernet.lk", "0773333333", Role.OPERATOR, true);
    }

    private void seed(String name, String email, String phone, Role role, boolean verified) {
        User u = new User(name, email, phone, passwordEncoder.encode(DEMO_PASSWORD), role);
        u.setVerified(verified);
        userRepository.save(u);
    }
}
