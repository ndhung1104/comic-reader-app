package com.group09.ComicReader.ai.repository;

import com.group09.ComicReader.ai.entity.AiSummaryEntity;
import com.group09.ComicReader.common.entity.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiSummaryRepository extends JpaRepository<AiSummaryEntity, Long> {
    List<AiSummaryEntity> findByComicIdOrderByCreatedAtDesc(Long comicId);
    List<AiSummaryEntity> findByChapterIdOrderByCreatedAtDesc(Long chapterId);
    Optional<AiSummaryEntity> findFirstByComicIdAndChapterIdIsNullAndStatusOrderByCreatedAtDesc(Long comicId, ModerationStatus status);
    Optional<AiSummaryEntity> findFirstByChapterIdAndStatusOrderByCreatedAtDesc(Long chapterId, ModerationStatus status);
    List<AiSummaryEntity> findByStatus(ModerationStatus status);
}
