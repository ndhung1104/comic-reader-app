package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChapterPremiumPolicyService {

    private static final int PREMIUM_TAIL_COUNT = 3;
    private static final int DEFAULT_PREMIUM_PRICE = 100;

    private final ChapterRepository chapterRepository;

    public ChapterPremiumPolicyService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    @Transactional
    public void applyLastThreePremiumPolicy(Long comicId) {
        List<ChapterEntity> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId);
        int total = chapters.size();
        if (total == 0) {
            return;
        }

        int premiumCount;
        if (total <= 1) {
            premiumCount = 0;
        } else if (total <= PREMIUM_TAIL_COUNT) {
            premiumCount = total - 1;
        } else {
            premiumCount = PREMIUM_TAIL_COUNT;
        }

        int premiumStartIndex = total - premiumCount;
        LocalDateTime now = LocalDateTime.now();
        List<ChapterEntity> changedChapters = new ArrayList<>();

        for (int index = 0; index < total; index++) {
            ChapterEntity chapter = chapters.get(index);
            boolean shouldBePremium = index >= premiumStartIndex;
            int expectedPrice = shouldBePremium ? DEFAULT_PREMIUM_PRICE : 0;

            if (chapter.isPremium() != shouldBePremium || chapter.getPrice() == null || chapter.getPrice() != expectedPrice) {
                chapter.setPremium(shouldBePremium);
                chapter.setPrice(expectedPrice);
                chapter.setUpdatedAt(now);
                changedChapters.add(chapter);
            }
        }

        if (!changedChapters.isEmpty()) {
            chapterRepository.saveAll(changedChapters);
        }
    }
}
