package com.group09.ComicReader.translationjob.repository;

import com.group09.ComicReader.translationjob.entity.TranslationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranslationJobRepository extends JpaRepository<TranslationJobEntity, Long> {

    List<TranslationJobEntity> findByChapterIdOrderByCreatedAtDesc(Long chapterId);
}
