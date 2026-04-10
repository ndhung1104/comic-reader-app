package com.group09.ComicReader;

import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.chapter.service.ChapterPremiumPolicyService;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.comic.service.ComicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChapterPremiumPolicyIntegrationTest {

    @Autowired
    private ComicRepository comicRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private ComicService comicService;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private ChapterPremiumPolicyService chapterPremiumPolicyService;

    @Test
    void applyPolicyShouldKeepFirstChapterFreeForShortComics() {
        ComicEntity comic = createComic();
        createChapter(comic, 1);
        createChapter(comic, 2);
        createChapter(comic, 3);

        chapterPremiumPolicyService.applyLastThreePremiumPolicy(comic.getId());

        List<ChapterEntity> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comic.getId());
        assertThat(chapters).hasSize(3);
        assertChapterState(chapters.get(0), false, 0);
        assertChapterState(chapters.get(1), true, 100);
        assertChapterState(chapters.get(2), true, 100);
    }

    @Test
    void createChapterShouldApplyLastThreePremiumPolicy() {
        ComicEntity comic = createComic();
        createChapter(comic, 1);
        createChapter(comic, 2);
        createChapter(comic, 3);

        ChapterRequest request = new ChapterRequest();
        request.setChapterNumber(4);
        request.setTitle("Chapter 4");
        request.setPremium(false);
        request.setPrice(0);
        comicService.createChapter(comic.getId(), request);

        List<ChapterEntity> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comic.getId());
        assertThat(chapters).hasSize(4);
        assertChapterState(chapters.get(0), false, 0);
        assertChapterState(chapters.get(1), true, 100);
        assertChapterState(chapters.get(2), true, 100);
        assertChapterState(chapters.get(3), true, 100);
    }

    @Test
    void updateAndDeleteShouldRecalculatePremiumTail() {
        ComicEntity comic = createComic();
        ChapterEntity chapter1 = createChapter(comic, 1);
        createChapter(comic, 2);
        createChapter(comic, 3);
        ChapterEntity chapter4 = createChapter(comic, 4);

        chapterPremiumPolicyService.applyLastThreePremiumPolicy(comic.getId());

        ChapterRequest updateRequest = new ChapterRequest();
        updateRequest.setChapterNumber(10);
        updateRequest.setTitle(chapter1.getTitle());
        updateRequest.setPremium(false);
        updateRequest.setPrice(0);
        chapterService.updateChapter(chapter1.getId(), updateRequest);

        List<ChapterEntity> afterUpdate = chapterRepository.findByComicIdOrderByChapterNumberAsc(comic.getId());
        Map<Integer, ChapterEntity> byNumber = new HashMap<>();
        for (ChapterEntity chapter : afterUpdate) {
            byNumber.put(chapter.getChapterNumber(), chapter);
        }

        assertChapterState(byNumber.get(2), false, 0);
        assertChapterState(byNumber.get(3), true, 100);
        assertChapterState(byNumber.get(4), true, 100);
        assertChapterState(byNumber.get(10), true, 100);

        chapterService.deleteChapter(chapter4.getId());
        List<ChapterEntity> afterDelete = chapterRepository.findByComicIdOrderByChapterNumberAsc(comic.getId());
        assertThat(afterDelete).hasSize(3);
        assertChapterState(afterDelete.get(0), false, 0);
        assertChapterState(afterDelete.get(1), true, 100);
        assertChapterState(afterDelete.get(2), true, 100);
    }

    private ComicEntity createComic() {
        ComicEntity comic = new ComicEntity();
        comic.setTitle("Policy comic " + System.nanoTime());
        comic.setAuthor("Test Author");
        comic.setStatus("PUBLISHED");
        comic.setSynopsis("Policy test synopsis");
        return comicRepository.save(comic);
    }

    private ChapterEntity createChapter(ComicEntity comic, int chapterNumber) {
        ChapterEntity chapter = new ChapterEntity();
        chapter.setComic(comic);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle("Chapter " + chapterNumber);
        chapter.setPremium(false);
        chapter.setPrice(0);
        return chapterRepository.save(chapter);
    }

    private void assertChapterState(ChapterEntity chapter, boolean premium, int price) {
        assertThat(chapter).isNotNull();
        assertThat(chapter.isPremium()).isEqualTo(premium);
        assertThat(chapter.getPrice()).isEqualTo(price);
    }
}
