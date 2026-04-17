package com.group09.ComicReader.auth.controller;

import com.group09.ComicReader.auth.dto.ChangePasswordRequest;
import com.group09.ComicReader.auth.dto.UpdateUserPreferencesRequest;
import com.group09.ComicReader.auth.dto.UpdateProfileRequest;
import com.group09.ComicReader.auth.dto.UserPreferencesResponse;
import com.group09.ComicReader.auth.dto.UserProfileResponse;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe() {
        UserEntity user = getCurrentUser();
        return ResponseEntity.ok(new UserProfileResponse(user.getEmail(), user.getFullName(), user.getAvatarUrl()));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponse> getMyPreferences() {
        UserEntity user = getCurrentUser();
        return ResponseEntity.ok(new UserPreferencesResponse(
                user.getLanguageCode(),
                user.getDateOfBirth(),
                splitCommaList(user.getPreferredGenres())
        ));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponse> updateMyPreferences(@Valid @RequestBody UpdateUserPreferencesRequest request) {
        UserEntity user = getCurrentUser();

        String languageCode = request.getLanguageCode();
        if (languageCode != null) {
            String normalized = languageCode.trim().toLowerCase();
            if (!normalized.isEmpty() && !normalized.equals("en") && !normalized.equals("vi")) {
                throw new BadRequestException("Unsupported language");
            }
            user.setLanguageCode(normalized.isEmpty() ? null : normalized);
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getPreferredGenres() != null) {
            List<String> normalized = normalizeStringList(request.getPreferredGenres());
            if (!normalized.isEmpty() && normalized.size() < 3) {
                throw new BadRequestException("Select at least 3 genres");
            }
            user.setPreferredGenres(joinCommaList(normalized));
        }

        userRepository.save(user);

        return ResponseEntity.ok(new UserPreferencesResponse(
                user.getLanguageCode(),
                user.getDateOfBirth(),
                splitCommaList(user.getPreferredGenres())
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        UserEntity user = getCurrentUser();

        String fullName = request.getFullName() == null ? "" : request.getFullName().trim();
        if (fullName.isEmpty()) {
            throw new BadRequestException("Full name is required");
        }

        user.setFullName(fullName);
        userRepository.save(user);

        return ResponseEntity.ok(new UserProfileResponse(user.getEmail(), user.getFullName(), user.getAvatarUrl()));
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateAvatar(@RequestParam("avatar") MultipartFile avatar) {
        UserEntity user = getCurrentUser();

        String avatarUrl = fileStorageService.storeUserAvatar(user.getId(), avatar);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return ResponseEntity.ok(new UserProfileResponse(user.getEmail(), user.getFullName(), user.getAvatarUrl()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserEntity user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    private UserEntity getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static List<String> splitCommaList(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (part == null) continue;
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private static String joinCommaList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return String.join(",", items);
    }

    private static List<String> normalizeStringList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String item : input) {
            if (item == null) continue;
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) unique.add(trimmed);
        }
        return new ArrayList<>(unique);
    }
}
