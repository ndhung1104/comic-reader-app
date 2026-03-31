package com.group09.ComicReader.comic.repository;

import com.group09.ComicReader.comic.entity.ComicRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComicRatingRepository extends JpaRepository<ComicRatingEntity, Long> {

    Optional<ComicRatingEntity> findByUserIdAndComicId(Long userId, Long comicId);

    @Query("SELECT COALESCE(AVG(r.score), 0) FROM ComicRatingEntity r WHERE r.comicId = :comicId")
    Double findAverageScoreByComicId(@Param("comicId") Long comicId);
}
