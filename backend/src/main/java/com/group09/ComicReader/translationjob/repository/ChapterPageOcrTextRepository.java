package com.group09.ComicReader.translationjob.repository;

import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterPageOcrTextRepository extends JpaRepository<ChapterPageOcrTextEntity, Long> {

    Optional<ChapterPageOcrTextEntity> findByChapterIdAndPageNumberAndSourceLang(Long chapterId,
            Integer pageNumber,
            String sourceLang);

    List<ChapterPageOcrTextEntity> findByChapterIdAndSourceLangOrderByPageNumberAsc(Long chapterId, String sourceLang);
}
