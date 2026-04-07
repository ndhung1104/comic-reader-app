package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.dto.ChapterAudioPlaylistRequest;
import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.config.properties.TtsWorkerProperties;
import com.group09.ComicReader.translationjob.client.TtsWorkerClient;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerAudioPage;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchResponse;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.entity.ChapterPageTtsAudioEntity;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import com.group09.ComicReader.translationjob.repository.ChapterPageTtsAudioRepository;
import com.group09.ComicReader.translationjob.service.TranslationJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterAudioPlaylistServiceTest {

    @Mock
    private ChapterService chapterService;

    @Mock
    private ChapterPageRepository chapterPageRepository;

    @Mock
    private ChapterPageOcrTextRepository chapterPageOcrTextRepository;

    @Mock
    private ChapterPageTtsAudioRepository chapterPageTtsAudioRepository;

    @Mock
    private TranslationJobService translationJobService;

    @Mock
    private TtsWorkerClient ttsWorkerClient;

    @Mock
    private FileStorageService fileStorageService;

    private ChapterAudioPlaylistService chapterAudioPlaylistService;

    @BeforeEach
    void setUp() {
        TtsWorkerProperties properties = new TtsWorkerProperties();
        properties.setDefaultLang("auto");
        properties.setDefaultVoice("af_heart");
        properties.setDefaultSpeed(1.0);

        chapterAudioPlaylistService = new ChapterAudioPlaylistService(
                chapterService,
                chapterPageRepository,
                chapterPageOcrTextRepository,
                chapterPageTtsAudioRepository,
                translationJobService,
                ttsWorkerClient,
                properties,
                fileStorageService
        );
    }

    @Test
    void createOrGetPlaylistShouldReturnExistingAudioWithoutCallingWorkers() {
        long chapterId = 10L;
        List<ChapterPageEntity> chapterPages = buildChapterPages(chapterId, 2);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse(), new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);

        ChapterPageTtsAudioEntity page1 = new ChapterPageTtsAudioEntity();
        page1.setPageNumber(1);
        page1.setLang("auto");
        page1.setVoice("af_heart");
        page1.setSpeed(1.0);
        page1.setAudioPath("/uploads/chapter-10/tts/auto/af_heart-1_0/page-1.wav");

        ChapterPageTtsAudioEntity page2 = new ChapterPageTtsAudioEntity();
        page2.setPageNumber(2);
        page2.setLang("auto");
        page2.setVoice("af_heart");
        page2.setSpeed(1.0);
        page2.setAudioPath("/uploads/chapter-10/tts/auto/af_heart-1_0/page-2.wav");

        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "auto", "af_heart", 1.0
        )).thenReturn(List.of(page1, page2));

        var response = chapterAudioPlaylistService.createOrGetPlaylist(chapterId, new ChapterAudioPlaylistRequest());

        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getAudioPages()).hasSize(2);
        verify(translationJobService, never()).ensureChapterOcrText(any(), any());
        verify(ttsWorkerClient, never()).synthesizeBatch(any());
    }

    @Test
    void createOrGetPlaylistShouldGenerateAudioAfterOcrFallback() {
        long chapterId = 22L;
        List<ChapterPageEntity> chapterPages = buildChapterPages(chapterId, 2);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse(), new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);

        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "auto", "af_heart", 1.0
        )).thenReturn(List.of());

        when(chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "auto"))
                .thenReturn(List.of());

        ChapterPageOcrTextEntity ocr1 = new ChapterPageOcrTextEntity();
        ocr1.setPageNumber(1);
        ocr1.setSourceLang("auto");
        ocr1.setOcrText("hello page one");

        ChapterPageOcrTextEntity ocr2 = new ChapterPageOcrTextEntity();
        ocr2.setPageNumber(2);
        ocr2.setSourceLang("auto");
        ocr2.setOcrText("hello page two");

        when(translationJobService.ensureChapterOcrText(chapterId, "auto"))
                .thenReturn(List.of(ocr1, ocr2));

        TtsWorkerAudioPage generated1 = new TtsWorkerAudioPage();
        generated1.setPageNumber(1);
        generated1.setAudioBase64(Base64.getEncoder().encodeToString("audio-1".getBytes()));
        generated1.setDurationMs(300);

        TtsWorkerAudioPage generated2 = new TtsWorkerAudioPage();
        generated2.setPageNumber(2);
        generated2.setAudioBase64(Base64.getEncoder().encodeToString("audio-2".getBytes()));
        generated2.setDurationMs(350);

        TtsWorkerSynthesizeBatchResponse workerResponse = new TtsWorkerSynthesizeBatchResponse();
        workerResponse.setStatus("SUCCEEDED");
        workerResponse.setAudioPages(List.of(generated1, generated2));
        when(ttsWorkerClient.synthesizeBatch(any())).thenReturn(workerResponse);

        when(fileStorageService.storeChapterPageAudio(eq(chapterId), eq(1), eq("auto"), eq("af_heart"), eq(1.0), any()))
                .thenReturn("/uploads/chapter-22/tts/auto/af_heart-1_0/page-1.wav");
        when(fileStorageService.storeChapterPageAudio(eq(chapterId), eq(2), eq("auto"), eq("af_heart"), eq(1.0), any()))
                .thenReturn("/uploads/chapter-22/tts/auto/af_heart-1_0/page-2.wav");

        when(chapterPageTtsAudioRepository.save(any(ChapterPageTtsAudioEntity.class))).thenAnswer(invocation -> {
            ChapterPageTtsAudioEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId((long) entity.getPageNumber());
            }
            return entity;
        });

        var response = chapterAudioPlaylistService.createOrGetPlaylist(chapterId, new ChapterAudioPlaylistRequest());

        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getAudioPages()).hasSize(2);
        assertThat(response.getAudioPages().get(0).getAudioUrl()).contains("page-1.wav");
        assertThat(response.getAudioPages().get(1).getAudioUrl()).contains("page-2.wav");
    }

    private List<ChapterPageEntity> buildChapterPages(long chapterId, int count) {
        ComicEntity comic = new ComicEntity();
        comic.setId(300L);

        ChapterEntity chapter = new ChapterEntity();
        chapter.setId(chapterId);
        chapter.setComic(comic);

        ChapterPageEntity first = new ChapterPageEntity();
        first.setId(1L);
        first.setChapter(chapter);
        first.setPageNumber(0);
        first.setImageUrl("/uploads/a.png");

        if (count == 1) {
            return List.of(first);
        }

        ChapterPageEntity second = new ChapterPageEntity();
        second.setId(2L);
        second.setChapter(chapter);
        second.setPageNumber(9);
        second.setImageUrl("/uploads/b.png");
        return List.of(first, second);
    }
}
