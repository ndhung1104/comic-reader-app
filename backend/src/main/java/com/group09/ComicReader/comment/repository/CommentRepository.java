package com.group09.ComicReader.comment.repository;

import com.group09.ComicReader.comment.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    @EntityGraph(attributePaths = { "user", "chapter" })
    List<CommentEntity> findByComicIdAndHiddenFalseOrderByCreatedAtDesc(Long comicId);

    @EntityGraph(attributePaths = { "user", "chapter" })
    Page<CommentEntity> findByComicIdAndHiddenFalseOrderByCreatedAtDesc(Long comicId, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "chapter" })
    Page<CommentEntity> findByComicIdAndChapterIdAndHiddenFalseOrderByCreatedAtDesc(Long comicId, Long chapterId, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "chapter" })
    Page<CommentEntity> findByComicIdOrderByCreatedAtDesc(Long comicId, Pageable pageable);

        @EntityGraph(attributePaths = { "user", "chapter" })
        @Query("""
            SELECT c
            FROM CommentEntity c
            WHERE c.comic.id = :comicId
              AND c.hidden = false
              AND c.parentComment IS NULL
              AND (
                :chapterId IS NULL
                OR (c.chapter IS NOT NULL AND c.chapter.id = :chapterId)
              )
            ORDER BY c.createdAt DESC
            """)
        Page<CommentEntity> findRootCommentsPaged(@Param("comicId") Long comicId,
            @Param("chapterId") Long chapterId,
            Pageable pageable);

        @EntityGraph(attributePaths = { "user", "chapter" })
        @Query("""
            SELECT c
            FROM CommentEntity c
            WHERE c.comic.id = :comicId
              AND c.hidden = false
              AND (
                c.id IN :rootIds
                OR (c.rootComment IS NOT NULL AND c.rootComment.id IN :rootIds)
              )
              AND (
                :chapterId IS NULL
                OR (c.chapter IS NOT NULL AND c.chapter.id = :chapterId)
              )
            """)
        List<CommentEntity> findThreadCommentsForRoots(@Param("comicId") Long comicId,
            @Param("chapterId") Long chapterId,
            @Param("rootIds") List<Long> rootIds);
}
