package com.group09.ComicReader.auth.service;

import com.group09.ComicReader.auth.dto.AuthResponse;
import com.group09.ComicReader.auth.dto.GoogleLoginRequest;
import com.group09.ComicReader.auth.dto.LoginRequest;
import com.group09.ComicReader.auth.dto.RegisterRequest;
import com.group09.ComicReader.auth.entity.RoleEntity;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.RoleRepository;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        RoleEntity userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BadRequestException("Role USER not found"));

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(userRole));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Email already exists");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, "Bearer", "USER");
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail() == null ? "" : request.getEmail().trim(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            throw new BadRequestException("Account is banned");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        String role = user.getRoles().stream().findFirst().map(RoleEntity::getName).orElse("USER");
        return new AuthResponse(token, "Bearer", role);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo userInfo = googleTokenVerifier.verify(request == null ? null : request.getIdToken());

        String email = userInfo.getEmail() == null ? "" : userInfo.getEmail().trim();
        if (email.isEmpty()) {
            throw new BadRequestException("Invalid Google token");
        }

        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user != null && !user.isEnabled()) {
            throw new BadRequestException("Account is banned");
        }

        if (user == null) {
            RoleEntity userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new BadRequestException("Role USER not found"));

            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);

            String fullName = userInfo.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                newUser.setFullName(email);
            } else {
                newUser.setFullName(fullName.trim());
            }

            // Not used for Google login, but required by our UserDetails flow.
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setRoles(Set.of(userRole));

            try {
                userRepository.save(newUser);
            } catch (DataIntegrityViolationException ex) {
                // Another request may have created the user concurrently.
            }
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtService.generateToken(userDetails);

        UserEntity ensuredUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid Google token"));
        String role = ensuredUser.getRoles().stream().findFirst().map(RoleEntity::getName).orElse("USER");
        return new AuthResponse(token, "Bearer", role);
    }
}

