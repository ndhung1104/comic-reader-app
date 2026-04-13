package com.group09.ComicReader.translationjob.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.common.exception.ServiceUnavailableException;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.config.properties.TranslationWorkerProperties;
import com.group09.ComicReader.config.properties.TtsWorkerProperties;
import com.group09.ComicReader.translationjob.client.TranslationWorkerClient;
import com.group09.ComicReader.translationjob.client.TtsWorkerClient;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerAudioPage;
import com.group09.ComicReader.translationjob.client.dto.TtsWorkerSynthesizeBatchResponse;
import com.group09.ComicReader.translationjob.client.dto.WorkerJobStatusResponse;
import com.group09.ComicReader.translationjob.client.dto.WorkerOcrPageText;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobRequest;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobResponse;
import com.group09.ComicReader.translationjob.dto.CreateTranslationJobRequest;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.entity.ChapterPageTtsAudioEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobStatus;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import com.group09.ComicReader.translationjob.repository.ChapterPageTtsAudioRepository;
import com.group09.ComicReader.translationjob.repository.TranslationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationJobServiceTest {

    @Mock
    private TranslationJobRepository translationJobRepository;

    @Mock
    private ChapterPageRepository chapterPageRepository;

    @Mock
    private ChapterPageOcrTextRepository chapterPageOcrTextRepository;

    @Mock
    private ChapterPageTtsAudioRepository chapterPageTtsAudioRepository;

    @Mock
    private ChapterService chapterService;

    @Mock
    private TranslationWorkerClient translationWorkerClient;

    @Mock
    private TtsWorkerClient ttsWorkerClient;

    @Mock
    private FileStorageService fileStorageService;

    private TranslationJobService translationJobService;
    private TranslationWorkerProperties translationWorkerProperties;
    private TtsWorkerProperties ttsWorkerProperties;

    private ChapterEntity chapter;

    @BeforeEach
    void setUp() {
        translationWorkerProperties = new TranslationWorkerProperties();
        translationWorkerProperties.setEnabled(true);
        ttsWorkerProperties = new TtsWorkerProperties();
        ttsWorkerProperties.setEnabled(false);

        translationJobService = new TranslationJobService(
                translationJobRepository,
                chapterPageRepository,
                chapterPageOcrTextRepository,
                chapterPageTtsAudioRepository,
                chapterService,
                translationWorkerClient,
                ttsWorkerClient,
                translationWorkerProperties,
                ttsWorkerProperties,
                fileStorageService
        );

        ComicEntity comic = new ComicEntity();
        comic.setId(9L);

        chapter = new ChapterEntity();
        chapter.setId(5L);
        chapter.setComic(comic);

        lenient().when(translationJobRepository.save(any(TranslationJobEntity.class))).thenAnswer(invocation -> {
            TranslationJobEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(101L);
            }
            return entity;
        });
        lenient().when(chapterPageOcrTextRepository.save(any(ChapterPageOcrTextEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createJobShouldReturn503WhenWorkerDisabled() {
        translationWorkerProperties.setEnabled(false);

        CreateTranslationJobRequest request = new CreateTranslationJobRequest();
        request.setChapterId(5L);
        request.setSourceLang("ja");
        request.setTargetLang("vi");

        assertThatThrownBy(() -> translationJobService.createJob(request))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");

        verify(translationJobRepository, never()).save(any(TranslationJobEntity.class));
    }

    @Test
    void createJobShouldSubmitToWorkerAndSetExternalId() {
        when(chapterService.getChapterEntity(5L)).thenReturn(chapter);

        ChapterPageEntity page = new ChapterPageEntity();
        page.setPageNumber(1);
        page.setImageUrl("/uploads/ch1_p1.jpg");
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(5L)).thenReturn(List.of(page));

        WorkerSubmitJobResponse workerResponse = new WorkerSubmitJobResponse();
        workerResponse.setJobId("worker-1");
        workerResponse.setStatus("RUNNING");
        when(translationWorkerClient.submitJob(any())).thenReturn(workerResponse);

        CreateTranslationJobRequest request = new CreateTranslationJobRequest();
        request.setChapterId(5L);
        request.setSourceLang("ja");
        request.setTargetLang("vi");

        var response = translationJobService.createJob(request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getExternalJobId()).isEqualTo("worker-1");
        assertThat(response.getStatus()).isEqualTo(TranslationJobStatus.RUNNING);
    }

    @Test
    void createJobShouldNormalizeWorkerPageNumbersToOneBasedSequence() {
        when(chapterService.getChapterEntity(5L)).thenReturn(chapter);

        ChapterPageEntity page0 = new ChapterPageEntity();
        page0.setId(10L);
        page0.setPageNumber(0);
        page0.setImageUrl("/uploads/ch1_p0.jpg");

        ChapterPageEntity page7 = new ChapterPageEntity();
        page7.setId(11L);
        page7.setPageNumber(7);
        page7.setImageUrl("/uploads/ch1_p7.jpg");

        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(5L)).thenReturn(List.of(page0, page7));

        WorkerSubmitJobResponse workerResponse = new WorkerSubmitJobResponse();
        workerResponse.setJobId("worker-normalized");
        workerResponse.setStatus("RUNNING");

        ArgumentCaptor<WorkerSubmitJobRequest> requestCaptor = ArgumentCaptor.forClass(WorkerSubmitJobRequest.class);
        when(translationWorkerClient.submitJob(requestCaptor.capture())).thenReturn(workerResponse);

        CreateTranslationJobRequest request = new CreateTranslationJobRequest();
        request.setChapterId(5L);
        request.setSourceLang("vi");
        request.setTargetLang("en");

        translationJobService.createJob(request);

        WorkerSubmitJobRequest captured = requestCaptor.getValue();
        assertThat(captured.getPages()).hasSize(2);
        assertThat(captured.getPages().get(0).getPageNumber()).isEqualTo(1);
        assertThat(captured.getPages().get(1).getPageNumber()).isEqualTo(2);
        assertThat(captured.getPages().get(0).getImageUrl()).isEqualTo("/uploads/ch1_p0.jpg");
        assertThat(captured.getPages().get(1).getImageUrl()).isEqualTo("/uploads/ch1_p7.jpg");
    }

    @Test
    void getJobShouldPersistOcrPagesWhenSucceeded() {
        TranslationJobEntity job = new TranslationJobEntity();
        job.setId(101L);
        job.setChapter(chapter);
        job.setExternalJobId("worker-1");
        job.setStatus(TranslationJobStatus.RUNNING);
        job.setSourceLang("ja");
        job.setTargetLang("vi");

        when(translationJobRepository.findById(101L)).thenReturn(Optional.of(job));

        WorkerOcrPageText page1 = new WorkerOcrPageText();
        page1.setChapterId(5L);
        page1.setPageNumber(1);
        page1.setSourceLang("ja");
        page1.setOcrText("text page 1");

        WorkerJobStatusResponse statusResponse = new WorkerJobStatusResponse();
        statusResponse.setJobId("worker-1");
        statusResponse.setStatus("SUCCEEDED");
        statusResponse.setOcrPages(List.of(page1));

        when(translationWorkerClient.fetchJobStatus("worker-1")).thenReturn(statusResponse);
        when(chapterPageOcrTextRepository.findByChapterIdAndPageNumberAndSourceLang(5L, 1, "ja"))
                .thenReturn(Optional.empty());

        var response = translationJobService.getJob(101L);

        assertThat(response.getStatus()).isEqualTo(TranslationJobStatus.SUCCEEDED);
        assertThat(response.isOcrPersisted()).isTrue();

        ArgumentCaptor<ChapterPageOcrTextEntity> captor = ArgumentCaptor.forClass(ChapterPageOcrTextEntity.class);
        verify(chapterPageOcrTextRepository).save(captor.capture());
        assertThat(captor.getValue().getOcrText()).isEqualTo("text page 1");
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getSourceLang()).isEqualTo("ja");
    }

    @Test
    void ensureChapterOcrTextShouldCreateJobAndReturnImmediatelyWhenOcrMissing() {
        when(chapterService.getChapterEntity(5L)).thenReturn(chapter);

        ChapterPageEntity page1 = new ChapterPageEntity();
        page1.setPageNumber(1);
        page1.setImageUrl("/uploads/ch1_p1.jpg");

        ChapterPageEntity page2 = new ChapterPageEntity();
        page2.setPageNumber(2);
        page2.setImageUrl("/uploads/ch1_p2.jpg");

        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(5L)).thenReturn(List.of(page1, page2));
        when(chapterPageOcrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(5L, "ja"))
                .thenReturn(List.of(), List.of());

        when(translationJobRepository.findFirstByChapterIdAndSourceLangAndStatusInOrderByCreatedAtDesc(
                5L,
                "ja",
                List.of(TranslationJobStatus.QUEUED, TranslationJobStatus.RUNNING)
        )).thenReturn(Optional.empty());

        WorkerSubmitJobResponse workerResponse = new WorkerSubmitJobResponse();
        workerResponse.setJobId("worker-enqueue");
        workerResponse.setStatus("RUNNING");
        when(translationWorkerClient.submitJob(any())).thenReturn(workerResponse);
        when(translationJobRepository.findById(101L)).thenReturn(Optional.empty());

        List<ChapterPageOcrTextEntity> result = translationJobService.ensureChapterOcrText(5L, "ja");

        assertThat(result).isEmpty();
        verify(translationWorkerClient).submitJob(any());
        verify(translationJobRepository, never()).findTop50ByStatusInOrderByUpdatedAtAsc(anyList());
    }

    @Test
    void syncActiveJobsShouldPersistIncrementalOcrPages() {
        TranslationJobEntity job = new TranslationJobEntity();
        job.setId(101L);
        job.setChapter(chapter);
        job.setExternalJobId("worker-1");
        job.setStatus(TranslationJobStatus.RUNNING);
        job.setSourceLang("ja");
        job.setTargetLang("vi");

        when(translationJobRepository.findTop50ByStatusInOrderByUpdatedAtAsc(
                List.of(TranslationJobStatus.QUEUED, TranslationJobStatus.RUNNING)
        )).thenReturn(List.of(job));

        WorkerOcrPageText page1 = new WorkerOcrPageText();
        page1.setChapterId(5L);
        page1.setPageNumber(1);
        page1.setSourceLang("ja");
        page1.setOcrText("partial page");

        WorkerJobStatusResponse statusResponse = new WorkerJobStatusResponse();
        statusResponse.setStatus("RUNNING");
        statusResponse.setOcrPages(List.of(page1));
        when(translationWorkerClient.fetchJobStatus("worker-1")).thenReturn(statusResponse);
        when(chapterPageOcrTextRepository.findByChapterIdAndPageNumberAndSourceLang(5L, 1, "ja"))
                .thenReturn(Optional.empty());

        translationJobService.syncActiveJobs();

        verify(chapterPageOcrTextRepository).save(any(ChapterPageOcrTextEntity.class));
        verify(translationJobRepository).save(job);
    }

    @Test
    void syncActiveJobsShouldGenerateTtsForNewOcrPage() {
        ttsWorkerProperties.setEnabled(true);
        ttsWorkerProperties.setDefaultSpeed(1.0);
        ttsWorkerProperties.setVoiceJa("ja_JP-kokoro-medium");

        TranslationJobEntity job = new TranslationJobEntity();
        job.setId(101L);
        job.setChapter(chapter);
        job.setExternalJobId("worker-1");
        job.setStatus(TranslationJobStatus.RUNNING);
        job.setSourceLang("ja");
        job.setTargetLang("vi");

        when(translationJobRepository.findTop50ByStatusInOrderByUpdatedAtAsc(
                List.of(TranslationJobStatus.QUEUED, TranslationJobStatus.RUNNING)
        )).thenReturn(List.of(job));

        WorkerOcrPageText page1 = new WorkerOcrPageText();
        page1.setChapterId(5L);
        page1.setPageNumber(1);
        page1.setSourceLang("ja");
        page1.setOcrText("konnichiwa");

        WorkerJobStatusResponse statusResponse = new WorkerJobStatusResponse();
        statusResponse.setStatus("RUNNING");
        statusResponse.setOcrPages(List.of(page1));
        when(translationWorkerClient.fetchJobStatus("worker-1")).thenReturn(statusResponse);
        when(chapterPageOcrTextRepository.findByChapterIdAndPageNumberAndSourceLang(5L, 1, "ja"))
                .thenReturn(Optional.empty());
        when(chapterPageTtsAudioRepository.findByChapterIdAndPageNumberAndLangAndVoiceAndSpeed(
                5L, 1, "ja", "ja_JP-kokoro-medium", 1.0
        )).thenReturn(Optional.empty());

        TtsWorkerAudioPage audioPage = new TtsWorkerAudioPage();
        audioPage.setPageNumber(1);
        audioPage.setAudioBase64(Base64.getEncoder().encodeToString("audio".getBytes()));
        audioPage.setDurationMs(300);

        TtsWorkerSynthesizeBatchResponse ttsResponse = new TtsWorkerSynthesizeBatchResponse();
        ttsResponse.setStatus("SUCCEEDED");
        ttsResponse.setAudioPages(List.of(audioPage));
        when(ttsWorkerClient.synthesizeBatch(any())).thenReturn(ttsResponse);
        when(fileStorageService.storeChapterPageAudio(
                eq(5L), eq(1), eq("ja"), eq("ja_JP-kokoro-medium"), eq(1.0), any()))
                .thenReturn("/uploads/chapter-5/tts/ja/ja_jp-kokoro-medium-1_0/page-1.wav");
        when(chapterPageTtsAudioRepository.save(any(ChapterPageTtsAudioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        translationJobService.syncActiveJobs();

        verify(ttsWorkerClient).synthesizeBatch(any());
        verify(chapterPageTtsAudioRepository).save(any(ChapterPageTtsAudioEntity.class));
    }
}
