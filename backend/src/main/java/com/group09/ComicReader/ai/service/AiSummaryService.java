package com.group09.ComicReader.ai.service;

import com.group09.ComicReader.ai.dto.AiSummaryResponse;
import com.group09.ComicReader.ai.entity.AiFeature;
import com.group09.ComicReader.ai.entity.AiSummaryEntity;
import com.group09.ComicReader.ai.repository.AiSummaryRepository;
import com.group09.ComicReader.ai.service.client.AiClient;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.common.entity.ModerationStatus;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.translationjob.entity.ChapterPageOcrTextEntity;
import com.group09.ComicReader.translationjob.repository.ChapterPageOcrTextRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiSummaryService {

    private final AiSummaryRepository aiSummaryRepository;
    private final AiClient aiClient;
    private final AiUsageService aiUsageService;
    private final ComicService comicService;
    private final ChapterRepository chapterRepository;
    private final ChapterPageOcrTextRepository ocrTextRepository;
    private final UserRepository userRepository;

    public AiSummaryService(AiSummaryRepository aiSummaryRepository,
                            AiClient aiClient,
                            AiUsageService aiUsageService,
                            ComicService comicService,
                            ChapterRepository chapterRepository,
                            ChapterPageOcrTextRepository ocrTextRepository,
                            UserRepository userRepository) {
        this.aiSummaryRepository = aiSummaryRepository;
        this.aiClient = aiClient;
        this.aiUsageService = aiUsageService;
        this.comicService = comicService;
        this.chapterRepository = chapterRepository;
        this.ocrTextRepository = ocrTextRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AiSummaryResponse generateSummary(Long comicId, Long chapterId) {
        ComicEntity comic = comicService.getComicEntity(comicId);
        ChapterEntity chapter = null;
        String contentToSummarize;
        String type;

        if (chapterId != null) {
            chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new NotFoundException("Chapter not found: " + chapterId));
            List<ChapterPageOcrTextEntity> ocrTexts = ocrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "vi");
            if (ocrTexts.isEmpty()) {
                ocrTexts = ocrTextRepository.findByChapterIdAndSourceLangOrderByPageNumberAsc(chapterId, "en");
            }
            contentToSummarize = ocrTexts.stream()
                    .map(ChapterPageOcrTextEntity::getOcrText)
                    .collect(Collectors.joining("\n"));
            type = "chapter";
        } else {
            contentToSummarize = comic.getSynopsis();
            type = "comic";
        }

        if (contentToSummarize == null || contentToSummarize.isBlank()) {
            throw new com.group09.ComicReader.common.exception.BadRequestException("No content available to summarize");
        }

        AiUsageService.UsageContext usageContext = aiUsageService.beginUsage(
                AiFeature.AI_SUMMARY,
                type + "-" + (chapterId != null ? chapterId : comicId),
                contentToSummarize.length(),
                "generateSummary"
        );

        try {
            String systemPrompt = "You are a professional comic editor. Summarize the following " + type + " content in a compelling way. Return ONLY the summary text.";
            String userPrompt = "Content to summarize:\n\n" + contentToSummarize;

            String summaryText = aiClient.generate(systemPrompt, userPrompt);

            AiSummaryEntity entity = new AiSummaryEntity();
            entity.setComic(comic);
            entity.setChapter(chapter);
            entity.setContent(summaryText);
            entity.setStatus(ModerationStatus.REVIEW);
            entity.setCreator(getCurrentUser());
            entity = aiSummaryRepository.save(entity);

            aiUsageService.completeSuccess(usageContext, aiClient.getProviderKey(), aiClient.getModel(), summaryText.length(), "success");

            return toResponse(entity);
        } catch (Exception e) {
            aiUsageService.completeFailure(usageContext, aiClient.getProviderKey(), aiClient.getModel(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public AiSummaryResponse moderateSummary(Long summaryId, ModerationStatus status, String reason) {
        AiSummaryEntity entity = aiSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new NotFoundException("Summary not found: " + summaryId));
        entity.setStatus(status);
        entity.setModerationReason(reason);
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(aiSummaryRepository.save(entity));
    }

    public List<AiSummaryResponse> getHistory(Long comicId, Long chapterId) {
        List<AiSummaryEntity> entities;
        if (chapterId != null) {
            entities = aiSummaryRepository.findByChapterIdOrderByCreatedAtDesc(chapterId);
        } else {
            entities = aiSummaryRepository.findByComicIdOrderByCreatedAtDesc(comicId);
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AiSummaryResponse getLatestApproved(Long comicId, Long chapterId) {
        return aiSummaryRepository.findFirstByComicIdAndChapterIdIsNullAndStatusOrderByCreatedAtDesc(comicId, ModerationStatus.APPROVED)
                .or(() -> chapterId != null ? aiSummaryRepository.findFirstByChapterIdAndStatusOrderByCreatedAtDesc(chapterId, ModerationStatus.APPROVED) : java.util.Optional.empty())
                .map(this::toResponse)
                .orElse(null);
    }

    private AiSummaryResponse toResponse(AiSummaryEntity entity) {
        AiSummaryResponse response = new AiSummaryResponse();
        response.setId(entity.getId());
        response.setComicId(entity.getComic().getId());
        if (entity.getChapter() != null) {
            response.setChapterId(entity.getChapter().getId());
        }
        response.setContent(entity.getContent());
        response.setStatus(entity.getStatus());
        response.setModerationReason(entity.getModerationReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private UserEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
