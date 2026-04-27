package com.group09.ComicReader.auth.repository;

import com.group09.ComicReader.auth.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<UserEntity> findAllByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email, Pageable pageable);
    
    @org.springframework.data.jpa.repository.Query("SELECT u FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to ORDER BY u.createdAt ASC")
    java.util.List<UserEntity> findAllByDateRange(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from, @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);
}

