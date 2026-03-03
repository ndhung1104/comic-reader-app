package com.group09.ComicReader.chapter.repository;

import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterPageRepository extends JpaRepository<ChapterPageEntity, Long> {
    List<ChapterPageEntity> findByChapterIdOrderByPageNumberAsc(Long chapterId);
    int countByChapterId(Long chapterId);
}

