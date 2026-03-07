package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.ChapterPurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterPurchaseRepository extends JpaRepository<ChapterPurchaseEntity, Long> {

    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);

    List<ChapterPurchaseEntity> findByUserIdAndChapterIdIn(Long userId, List<Long> chapterIds);
}
