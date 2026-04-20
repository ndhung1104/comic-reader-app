package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChapterAdPolicyService {

    private final ChapterRepository chapterRepository;

    public ChapterAdPolicyService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    public boolean isFreeChapterAdEligible(ChapterEntity chapter) {
        if (chapter == null || chapter.isPremium() || chapter.getComic() == null || chapter.getComic().getId() == null) {
            return false;
        }
        return getEligibleFreeChapterIds(chapter.getComic().getId()).contains(chapter.getId());
    }

    public Set<Long> getEligibleFreeChapterIds(Long comicId) {
        if (comicId == null) {
            return Collections.emptySet();
        }

        List<ChapterEntity> freeChapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId)
                .stream()
                .filter(chapter -> !chapter.isPremium())
                .toList();
        if (freeChapters.isEmpty()) {
            return Collections.emptySet();
        }

        int totalFree = freeChapters.size();
        int eligibleCount = Math.max(1, (int) Math.ceil(totalFree * 0.2d));
        Set<Long> eligibleIds = new LinkedHashSet<>();
        for (int index = 0; index < eligibleCount; index++) {
            int selectedIndex = (int) Math.floor((index + 0.5d) * totalFree / eligibleCount);
            selectedIndex = Math.max(0, Math.min(totalFree - 1, selectedIndex));
            eligibleIds.add(freeChapters.get(selectedIndex).getId());
        }
        return eligibleIds;
    }
}
