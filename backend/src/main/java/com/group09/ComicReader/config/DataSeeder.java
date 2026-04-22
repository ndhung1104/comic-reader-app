package com.group09.ComicReader.config;

import com.group09.ComicReader.auth.entity.RoleEntity;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.RoleRepository;
import com.group09.ComicReader.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        RoleEntity adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> createRole("ADMIN"));
        RoleEntity userRole = roleRepository.findByName("USER").orElseGet(() -> createRole("USER"));
        RoleEntity creatorRole = roleRepository.findByName("CREATOR").orElseGet(() -> createRole("CREATOR"));

        if (!userRepository.existsByEmail("admin@comicreader.dev")) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@comicreader.dev");
            admin.setFullName("Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.info("Seeded admin user");
        }

        if (!userRepository.existsByEmail("user@comicreader.dev")) {
            UserEntity user = new UserEntity();
            user.setEmail("user@comicreader.dev");
            user.setFullName("Demo User");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
            log.info("Seeded demo user");
        }

        // Seed 50 users
        long currentCount = userRepository.count();
        if (currentCount < 50) {
            log.info("Current user count is {}. Seeding up to 50 test users...", currentCount);
            String encodedPassword = passwordEncoder.encode("password123");
            for (int i = 1; i <= 50; i++) {
                String email = "user" + i + "@example.com";
                if (!userRepository.existsByEmail(email)) {
                    UserEntity user = new UserEntity();
                    user.setEmail(email);
                    user.setFullName("Test User " + i);
                    user.setPassword(encodedPassword);

                    // Assign roles based on index
                    if (i <= 5) {
                        user.setRoles(Set.of(adminRole));
                    } else if (i <= 15) {
                        user.setRoles(Set.of(creatorRole));
                    } else {
                        user.setRoles(Set.of(userRole));
                    }
                    userRepository.save(user);
                }
            }
            log.info("Seeding completed.");
        }
    }

    private RoleEntity createRole(String roleName) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        return roleRepository.save(role);
    }
}

