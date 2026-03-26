package com.group09.ComicReader.chapter.repository;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
    List<ChapterEntity> findByComicIdOrderByChapterNumberAsc(Long comicId);

    int countByComicId(Long comicId);
}
