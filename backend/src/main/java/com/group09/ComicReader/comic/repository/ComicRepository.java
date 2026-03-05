package com.group09.ComicReader.comic.repository;

import com.group09.ComicReader.comic.entity.ComicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComicRepository extends JpaRepository<ComicEntity, Long> {
    Optional<ComicEntity> findBySlug(String slug);
}
