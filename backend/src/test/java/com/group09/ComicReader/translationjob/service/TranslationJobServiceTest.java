package com.group09.ComicReader.translationjob.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.translationjob.client.TranslationWorkerClient;
import com.group09.ComicReader.translationjob.client.dto.WorkerJobStatusResponse;
import com.group09.ComicReader.translationjob.client.dto.WorkerOcrPageText;
import com.group09.ComicReader.translationjob.client.dto.WorkerSubmitJobResponse;
import com.group09.ComicReader.translationjob.dto.CreateTranslationJobRequest;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobEntity;
import com.group09.ComicReader.translationjob.entity.TranslationJobStatus;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import com.group09.ComicReader.translationjob.repository.TranslationJobRepository;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private ChapterService chapterService;

    @Mock
    private TranslationWorkerClient translationWorkerClient;

    private TranslationJobService translationJobService;

    private ChapterEntity chapter;

    @BeforeEach
    void setUp() {
        translationJobService = new TranslationJobService(
                translationJobRepository,
                chapterPageRepository,
                chapterPageOcrTextRepository,
                chapterService,
                translationWorkerClient
        );

        ComicEntity comic = new ComicEntity();
        comic.setId(9L);

        chapter = new ChapterEntity();
        chapter.setId(5L);
        chapter.setComic(comic);

        when(translationJobRepository.save(any(TranslationJobEntity.class))).thenAnswer(invocation -> {
            TranslationJobEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(101L);
            }
            return entity;
        });
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
}
