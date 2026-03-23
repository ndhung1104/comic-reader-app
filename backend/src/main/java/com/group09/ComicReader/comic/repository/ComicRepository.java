package com.group09.ComicReader.comic.repository;

import java.util.Optional;
import com.group09.ComicReader.comic.entity.ComicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComicRepository extends JpaRepository<ComicEntity, Long> {
    Optional<ComicEntity> findBySlug(String slug);

    @Query("SELECT c FROM ComicEntity c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
           "COALESCE(LOWER(c.genres), '') LIKE LOWER(CONCAT('%', :category, '%'))")
    Page<ComicEntity> searchComics(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);
}
