package com.group09.ComicReader.comment.repository;

import com.group09.ComicReader.comment.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    @EntityGraph(attributePaths = { "user" })
    List<CommentEntity> findByComicIdAndHiddenFalseOrderByCreatedAtDesc(Long comicId);

    @EntityGraph(attributePaths = { "user" })
    Page<CommentEntity> findByComicIdOrderByCreatedAtDesc(Long comicId, Pageable pageable);
}
