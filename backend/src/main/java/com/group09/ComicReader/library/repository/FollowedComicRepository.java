package com.group09.ComicReader.library.repository;

import com.group09.ComicReader.library.entity.FollowedComicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowedComicRepository extends JpaRepository<FollowedComicEntity, Long> {

    boolean existsByUserIdAndComicId(Long userId, Long comicId);

    Optional<FollowedComicEntity> findByUserIdAndComicId(Long userId, Long comicId);

    List<FollowedComicEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
