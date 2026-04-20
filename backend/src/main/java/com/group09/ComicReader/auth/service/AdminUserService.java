package com.group09.ComicReader.auth.service;

import com.group09.ComicReader.auth.dto.UserResponse;
import com.group09.ComicReader.auth.entity.RoleEntity;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponse> getUsers(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return userRepository.findAllByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable)
                    .map(this::toUserResponse);
        }
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional
    public UserResponse banUser(Long userId) {
        UserEntity user = getUserEntity(userId);
        assertNotAdmin(user);
        user.setEnabled(false);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unbanUser(Long userId) {
        UserEntity user = getUserEntity(userId);
        user.setEnabled(true);
        return toUserResponse(userRepository.save(user));
    }

    private UserEntity getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void assertNotAdmin(UserEntity user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));
        if (isAdmin) {
            throw new BadRequestException("Cannot ban an admin user");
        }
    }

    private UserResponse toUserResponse(UserEntity entity) {
        UserResponse response = new UserResponse();
        response.setId(entity.getId());
        response.setEmail(entity.getEmail());
        response.setFullName(entity.getFullName());
        response.setEnabled(entity.isEnabled());
        response.setRoles(entity.getRoles().stream()
                .map(RoleEntity::getName)
                .toList());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
