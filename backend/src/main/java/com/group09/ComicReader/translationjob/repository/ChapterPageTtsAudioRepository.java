package com.group09.ComicReader.translationjob.repository;

import com.group09.ComicReader.translationjob.entity.ChapterPageTtsAudioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterPageTtsAudioRepository extends JpaRepository<ChapterPageTtsAudioEntity, Long> {

    List<ChapterPageTtsAudioEntity> findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(Long chapterId,
            String lang,
            String voice,
            Double speed);

    Optional<ChapterPageTtsAudioEntity> findByChapterIdAndPageNumberAndLangAndVoiceAndSpeed(Long chapterId,
            Integer pageNumber,
            String lang,
            String voice,
            Double speed);
}
