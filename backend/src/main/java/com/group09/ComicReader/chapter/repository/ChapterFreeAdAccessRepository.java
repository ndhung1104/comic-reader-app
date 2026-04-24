package com.group09.ComicReader.chapter.repository;

import com.group09.ComicReader.chapter.entity.ChapterFreeAdAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterFreeAdAccessRepository extends JpaRepository<ChapterFreeAdAccessEntity, Long> {

    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);
}
