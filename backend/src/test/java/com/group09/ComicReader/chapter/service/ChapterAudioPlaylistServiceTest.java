package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.ai.service.AiUsageService;
import com.group09.ComicReader.chapter.dto.ChapterAudioPlaylistRequest;
import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.common.exception.ServiceUnavailableException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private AiUsageService aiUsageService;

    private ChapterAudioPlaylistService chapterAudioPlaylistService;
    private TtsWorkerProperties ttsWorkerProperties;

    @BeforeEach
    void setUp() {
        ttsWorkerProperties = new TtsWorkerProperties();
        ttsWorkerProperties.setEnabled(true);
        ttsWorkerProperties.setDefaultLang("vi");
        ttsWorkerProperties.setDefaultVoice("vi_VN-vais1000-medium");
        ttsWorkerProperties.setVoiceVi("vi_VN-vais1000-medium");
        ttsWorkerProperties.setVoiceEn("en_US-lessac-medium");
        ttsWorkerProperties.setVoiceJa("ja_JP-kokoro-medium");
        ttsWorkerProperties.setVoiceKo("ko_KR-kss-medium");
        ttsWorkerProperties.setFallbackVoice("en_US-lessac-medium");
        ttsWorkerProperties.setDefaultSpeed(1.0);

        chapterAudioPlaylistService = new ChapterAudioPlaylistService(
                chapterService,
                chapterPageRepository,
                chapterPageOcrTextRepository,
                chapterPageTtsAudioRepository,
                translationJobService,
                ttsWorkerClient,
                ttsWorkerProperties,
                fileStorageService,
                aiUsageService
        );

        when(aiUsageService.beginUsage(any(), any(), any(), any()))
                .thenReturn(new AiUsageService.UsageContext(null, null));
    }

    @Test
    void createOrGetPlaylistShouldReturnExistingAudioWithoutCallingWorkers() {
        long chapterId = 10L;
        List<ChapterPageEntity> chapterPages = buildChapterPages(chapterId, 2);
        ChapterEntity chapter = buildChapter(chapterId, "vi");
        when(chapterService.getChapterEntity(chapterId)).thenReturn(chapter);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse(), new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);

        ChapterPageTtsAudioEntity page1 = new ChapterPageTtsAudioEntity();
        page1.setPageNumber(1);
        page1.setLang("vi");
        page1.setVoice("vi_VN-vais1000-medium");
        page1.setSpeed(1.0);
        page1.setAudioPath("/uploads/chapter-10/tts/vi/vi_vn-vais1000-medium-1_0/page-1.wav");

        ChapterPageTtsAudioEntity page2 = new ChapterPageTtsAudioEntity();
        page2.setPageNumber(2);
        page2.setLang("vi");
        page2.setVoice("vi_VN-vais1000-medium");
        page2.setSpeed(1.0);
        page2.setAudioPath("/uploads/chapter-10/tts/vi/vi_vn-vais1000-medium-1_0/page-2.wav");

        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "vi", "vi_VN-vais1000-medium", 1.0
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
        ChapterEntity chapter = buildChapter(chapterId, "vi");
        when(chapterService.getChapterEntity(chapterId)).thenReturn(chapter);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse(), new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);

        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "vi", "vi_VN-vais1000-medium", 1.0
        )).thenReturn(List.of());

        when(chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "vi"))
                .thenReturn(List.of());

        ChapterPageOcrTextEntity ocr1 = new ChapterPageOcrTextEntity();
        ocr1.setPageNumber(1);
        ocr1.setSourceLang("vi");
        ocr1.setOcrText("hello page one");

        ChapterPageOcrTextEntity ocr2 = new ChapterPageOcrTextEntity();
        ocr2.setPageNumber(2);
        ocr2.setSourceLang("vi");
        ocr2.setOcrText("hello page two");

        when(translationJobService.ensureChapterOcrText(chapterId, "vi"))
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

        when(fileStorageService.storeChapterPageAudio(eq(chapterId), eq(1), eq("vi"), eq("vi_VN-vais1000-medium"), eq(1.0), any()))
                .thenReturn("/uploads/chapter-22/tts/vi/vi_vn-vais1000-medium-1_0/page-1.wav");
        when(fileStorageService.storeChapterPageAudio(eq(chapterId), eq(2), eq("vi"), eq("vi_VN-vais1000-medium"), eq(1.0), any()))
                .thenReturn("/uploads/chapter-22/tts/vi/vi_vn-vais1000-medium-1_0/page-2.wav");

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

    @Test
    void createOrGetPlaylistShouldMapVoiceFromLanguageWhenVoiceMissing() {
        long chapterId = 35L;
        List<ChapterPageEntity> chapterPages = buildChapterPages(chapterId, 1);
        ChapterEntity chapter = buildChapter(chapterId, "ja");
        when(chapterService.getChapterEntity(chapterId)).thenReturn(chapter);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);

        ChapterAudioPlaylistRequest request = new ChapterAudioPlaylistRequest();
        request.setSourceLang("ja");

        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "ja", "ja_JP-kokoro-medium", 1.0
        )).thenReturn(List.of());

        when(chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "ja"))
                .thenReturn(List.of());

        ChapterPageOcrTextEntity ocr1 = new ChapterPageOcrTextEntity();
        ocr1.setPageNumber(1);
        ocr1.setSourceLang("ja");
        ocr1.setOcrText("konnichiwa");
        when(translationJobService.ensureChapterOcrText(chapterId, "ja"))
                .thenReturn(List.of(ocr1));

        TtsWorkerAudioPage generated1 = new TtsWorkerAudioPage();
        generated1.setPageNumber(1);
        generated1.setAudioBase64(Base64.getEncoder().encodeToString("audio-ja".getBytes()));
        generated1.setDurationMs(310);

        TtsWorkerSynthesizeBatchResponse workerResponse = new TtsWorkerSynthesizeBatchResponse();
        workerResponse.setStatus("SUCCEEDED");
        workerResponse.setAudioPages(List.of(generated1));
        when(ttsWorkerClient.synthesizeBatch(any())).thenReturn(workerResponse);

        when(fileStorageService.storeChapterPageAudio(
                eq(chapterId), eq(1), eq("ja"), eq("ja_JP-kokoro-medium"), eq(1.0), any()))
                .thenReturn("/uploads/chapter-35/tts/ja/ja_jp-kokoro-medium-1_0/page-1.wav");

        when(chapterPageTtsAudioRepository.save(any(ChapterPageTtsAudioEntity.class))).thenAnswer(invocation -> {
            ChapterPageTtsAudioEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        var response = chapterAudioPlaylistService.createOrGetPlaylist(chapterId, request);

        ArgumentCaptor<com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchRequest> captor =
                ArgumentCaptor.forClass(com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchRequest.class);
        verify(ttsWorkerClient).synthesizeBatch(captor.capture());

        assertThat(captor.getValue().getVoice()).isEqualTo("ja_JP-kokoro-medium");
        assertThat(response.getVoice()).isEqualTo("ja_JP-kokoro-medium");
        assertThat(response.getStatus()).isEqualTo("READY");
    }

    @Test
    void createOrGetPlaylistShouldReturn503WhenTtsWorkerDisabled() {
        long chapterId = 77L;
        ttsWorkerProperties.setEnabled(false);

        List<ChapterPageEntity> chapterPages = buildChapterPages(chapterId, 1);
        ChapterEntity chapter = buildChapter(chapterId, "vi");
        when(chapterService.getChapterEntity(chapterId)).thenReturn(chapter);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);
        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "vi", "vi_VN-vais1000-medium", 1.0
        )).thenReturn(List.of());
        when(chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "vi"))
                .thenReturn(List.of());

        ChapterPageOcrTextEntity ocr1 = new ChapterPageOcrTextEntity();
        ocr1.setPageNumber(1);
        ocr1.setSourceLang("vi");
        ocr1.setOcrText("hello");
        when(translationJobService.ensureChapterOcrText(chapterId, "vi"))
                .thenReturn(List.of(ocr1));

        assertThatThrownBy(() -> chapterAudioPlaylistService.createOrGetPlaylist(chapterId, new ChapterAudioPlaylistRequest()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");

        verify(ttsWorkerClient, never()).synthesizeBatch(any());
    }

    @Test
    void createOrGetPlaylistShouldReturn503WhenOcrIsStillProcessing() {
        long chapterId = 88L;
        List<ChapterPageEntity> chapterPages = buildChapterPages(chapterId, 2);
        ChapterEntity chapter = buildChapter(chapterId, "vi");
        when(chapterService.getChapterEntity(chapterId)).thenReturn(chapter);
        when(chapterService.getPages(chapterId)).thenReturn(List.of(new ChapterPageResponse(), new ChapterPageResponse()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId)).thenReturn(chapterPages);
        when(chapterPageTtsAudioRepository.findByChapterIdAndLangAndVoiceAndSpeedOrderByPageNumberAsc(
                chapterId, "vi", "vi_VN-vais1000-medium", 1.0
        )).thenReturn(List.of());
        when(chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "vi"))
                .thenReturn(List.of());

        ChapterPageOcrTextEntity ocr1 = new ChapterPageOcrTextEntity();
        ocr1.setPageNumber(1);
        ocr1.setSourceLang("vi");
        ocr1.setOcrText("partial");
        when(translationJobService.ensureChapterOcrText(chapterId, "vi"))
                .thenReturn(List.of(ocr1));

        assertThatThrownBy(() -> chapterAudioPlaylistService.createOrGetPlaylist(chapterId, new ChapterAudioPlaylistRequest()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("OCR is still processing");

        verify(ttsWorkerClient, never()).synthesizeBatch(any());
    }

    private List<ChapterPageEntity> buildChapterPages(long chapterId, int count) {
        ComicEntity comic = new ComicEntity();
        comic.setId(300L);

        ChapterEntity chapter = buildChapter(chapterId, "vi");
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

    private ChapterEntity buildChapter(long chapterId, String language) {
        ChapterEntity chapter = new ChapterEntity();
        chapter.setId(chapterId);
        chapter.setLanguage(language);
        return chapter;
    }
}
