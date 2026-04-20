package com.group09.ComicReader.creatorrequest.repository;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.creatorrequest.entity.CreatorRequestEntity;
import com.group09.ComicReader.creatorrequest.entity.CreatorRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreatorRequestRepository extends JpaRepository<CreatorRequestEntity, Long> {
    boolean existsByUserAndStatus(UserEntity user, CreatorRequestStatus status);

    @EntityGraph(attributePaths = {"user", "processedBy"})
    Page<CreatorRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "processedBy"})
    List<CreatorRequestEntity> findByStatusOrderByCreatedAtDesc(CreatorRequestStatus status);

    @EntityGraph(attributePaths = {"user", "processedBy"})
    Optional<CreatorRequestEntity> findTopByUserOrderByCreatedAtDesc(UserEntity user);
}
