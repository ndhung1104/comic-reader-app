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

        if (!userRepository.existsByEmail("admin@comicreader.dev")) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@comicreader.dev");
            admin.setFullName("Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("user@comicreader.dev")) {
            UserEntity user = new UserEntity();
            user.setEmail("user@comicreader.dev");
            user.setFullName("Demo User");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
        }
    }

    private RoleEntity createRole(String roleName) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        return roleRepository.save(role);
    }
}

