package com.group09.ComicReader.translationjob.repository;

import com.group09.ComicReader.translationjob.entity.TranslationJobEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TranslationJobRepository extends JpaRepository<TranslationJobEntity, Long> {

    List<TranslationJobEntity> findByChapterIdOrderByCreatedAtDesc(Long chapterId);

    Optional<TranslationJobEntity> findFirstByChapterIdAndSourceLangAndStatusInOrderByCreatedAtDesc(
            Long chapterId,
            String sourceLang,
            List<TranslationJobStatus> statuses
    );

    List<TranslationJobEntity> findTop50ByStatusInOrderByUpdatedAtAsc(List<TranslationJobStatus> statuses);
}
