package com.group09.ComicReader.comic.repository;

import java.util.List;
import java.util.Optional;
import com.group09.ComicReader.comic.entity.ComicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComicRepository extends JpaRepository<ComicEntity, Long> {
    Optional<ComicEntity> findBySlug(String slug);

    @Query("SELECT c FROM ComicEntity c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
           "COALESCE(LOWER(c.genres), '') LIKE LOWER(CONCAT('%', :category, '%'))")
    Page<ComicEntity> searchComics(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);

    /* Trending: sorted by view_count descending */
    @Query("SELECT c FROM ComicEntity c ORDER BY c.viewCount DESC")
    Page<ComicEntity> findTrending(Pageable pageable);

    /* Top rated: sorted by average_rating descending */
    @Query("SELECT c FROM ComicEntity c ORDER BY c.averageRating DESC")
    Page<ComicEntity> findTopRated(Pageable pageable);

    /* Related comics: share at least one genre keyword, excluding the current comic */
    @Query("SELECT DISTINCT c FROM ComicEntity c WHERE c.id <> :excludeId AND (" +
           "LOWER(c.genres) LIKE LOWER(CONCAT('%', :g0, '%'))" +
           ") ORDER BY c.averageRating DESC")
    Page<ComicEntity> findRelatedByOneGenre(@Param("excludeId") Long excludeId,
                                            @Param("g0") String genre0,
                                            Pageable pageable);

    @Query("SELECT DISTINCT c FROM ComicEntity c WHERE c.id <> :excludeId AND (" +
           "LOWER(c.genres) LIKE LOWER(CONCAT('%', :g0, '%')) OR " +
           "LOWER(c.genres) LIKE LOWER(CONCAT('%', :g1, '%'))" +
           ") ORDER BY c.averageRating DESC")
    Page<ComicEntity> findRelatedByTwoGenres(@Param("excludeId") Long excludeId,
                                             @Param("g0") String g0,
                                             @Param("g1") String g1,
                                             Pageable pageable);

    @Query("SELECT DISTINCT c FROM ComicEntity c WHERE c.id <> :excludeId AND (" +
           "LOWER(c.genres) LIKE LOWER(CONCAT('%', :g0, '%')) OR " +
           "LOWER(c.genres) LIKE LOWER(CONCAT('%', :g1, '%')) OR " +
           "LOWER(c.genres) LIKE LOWER(CONCAT('%', :g2, '%'))" +
           ") ORDER BY c.averageRating DESC")
    Page<ComicEntity> findRelatedByThreeGenres(@Param("excludeId") Long excludeId,
                                               @Param("g0") String g0,
                                               @Param("g1") String g1,
                                               @Param("g2") String g2,
                                               Pageable pageable);

    /* Increment view count */
    @Modifying
    @Query("UPDATE ComicEntity c SET c.viewCount = c.viewCount + 1 WHERE c.id = :comicId")
    void incrementViewCount(@Param("comicId") Long comicId);

    /* Update average rating */
    @Modifying
    @Query("UPDATE ComicEntity c SET c.averageRating = :avg WHERE c.id = :comicId")
    void updateAverageRating(@Param("comicId") Long comicId, @Param("avg") double avg);

    /* Increment / decrement follower count */
    @Modifying
    @Query("UPDATE ComicEntity c SET c.followerCount = c.followerCount + :delta WHERE c.id = :comicId")
    void adjustFollowerCount(@Param("comicId") Long comicId, @Param("delta") int delta);
}
